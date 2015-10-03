/*
 * Copyright 2013 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import okio.Buffer;
import okio.ByteString;

import static com.squareup.wire.FieldEncoding.FIXED32;
import static com.squareup.wire.FieldEncoding.FIXED64;
import static com.squareup.wire.FieldEncoding.LENGTH_DELIMITED;
import static com.squareup.wire.FieldEncoding.VARINT;
import static com.squareup.wire.WireField.Label;
import static java.util.Collections.unmodifiableMap;

class MessageTypeAdapter<M extends Message<M, B>, B extends Message.Builder<M, B>>
    extends TypeAdapter<M> {

  private final Class<M> messageType;

  enum UnknownFieldType {
    VARINT, FIXED32, FIXED64, LENGTH_DELIMITED;

    public static UnknownFieldType of(String name) {
      if ("varint".equals(name)) return VARINT;
      if ("fixed32".equals(name)) return FIXED32;
      if ("fixed64".equals(name)) return FIXED64;
      if ("length-delimited".equals(name)) return LENGTH_DELIMITED;
      throw new IllegalArgumentException("Unknown type " + name);
    }
  }

  // 2^64, used to convert sint64 values >= 2^63 to unsigned decimal form
  private static final BigInteger POWER_64 = new BigInteger("18446744073709551616");

  private final Gson gson;
  private final RuntimeMessageAdapter<M, B> messageAdapter;
  private final Map<String, FieldBinding<M, B>> fieldBindings;

  @SuppressWarnings("unchecked")
  public MessageTypeAdapter(Gson gson, TypeToken<M> type) {
    this.gson = gson;
    this.messageType = (Class<M>) type.getRawType();
    this.messageAdapter = RuntimeMessageAdapter.create(messageType);

    Map<String, FieldBinding<M, B>> fieldBindings = new LinkedHashMap<>();
    for (FieldBinding<M, B> binding : messageAdapter.fieldBindings().values()) {
      fieldBindings.put(binding.name, binding);
    }
    this.fieldBindings = unmodifiableMap(fieldBindings);
  }

  @SuppressWarnings("unchecked")
  @Override public void write(JsonWriter out, M message) throws IOException {
    if (message == null) {
      out.nullValue();
      return;
    }

    out.beginObject();
    for (FieldBinding<M, B> tagBinding : messageAdapter.fieldBindings().values()) {
      Object value = tagBinding.get(message);
      if (value == null) {
        continue;
      }
      out.name(tagBinding.name);
      emitJson(out, value, tagBinding.singleAdapter(), tagBinding.label);
    }

    if (message.unknownFields.size() > 0) {
      ProtoReader reader = new ProtoReader(new Buffer().write(message.unknownFields));
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        FieldEncoding fieldEncoding = reader.peekFieldEncoding();
        Object value = fieldEncoding.rawProtoAdapter().decode(reader);
        out.name(Integer.toString(tag));
        out.beginArray();
        if (fieldEncoding == FieldEncoding.VARINT) {
          out.value("varint");
          out.value((Long) value);
        } else if (fieldEncoding == FieldEncoding.FIXED32) {
          out.value("fixed32");
          out.value((Integer) value);
        } else if (fieldEncoding == FieldEncoding.FIXED64) {
          out.value("fixed64");
          out.value((Long) value);
        } else if (fieldEncoding == FieldEncoding.LENGTH_DELIMITED) {
          out.value("length-delimited");
          out.value(((ByteString) value).base64());
        } else {
          throw new AssertionError("Unknown field encoding " + fieldEncoding);
        }
        out.endArray();
      }
      reader.endMessage(token);
    }

    out.endObject();
  }

  @SuppressWarnings("unchecked")
  private void emitJson(JsonWriter out, Object value, ProtoAdapter<?> adapter, Label label)
      throws IOException {
    if (adapter == ProtoAdapter.UINT64) {
      if (label.isRepeated()) {
        List<Long> longs = (List<Long>) value;
        out.beginArray();
        for (int i = 0, count = longs.size(); i < count; i++) {
          emitUint64(longs.get(i), out);
        }
        out.endArray();
      } else {
        emitUint64((Long) value, out);
      }
    } else {
      gson.toJson(value, value.getClass(), out);
    }
  }

  private void emitUint64(Long value, JsonWriter out) throws IOException {
    if (value < 0) {
      BigInteger unsigned = POWER_64.add(BigInteger.valueOf(value));
      out.value(unsigned);
    } else {
      out.value(value);
    }
  }

  @SuppressWarnings("unchecked")
  @Override public M read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }

    B builder = messageAdapter.newBuilder();
    in.beginObject();

    while (in.peek() == JsonToken.NAME) {
      String name = in.nextName();

      FieldBinding<M, B> fieldBinding = fieldBindings.get(name);
      if (fieldBinding != null) {
        Object value = parseValue(fieldBinding.label, singleType(fieldBinding), parse(in));
        fieldBinding.set(builder, value);
        continue;
      }

      parseUnknownField(in, builder, Integer.parseInt(name));
    }

    in.endObject();
    return builder.build();
  }

  private JsonElement parse(JsonReader in) {
    return gson.fromJson(in, JsonElement.class);
  }

  private Object parseValue(Label label, Type valueType, JsonElement valueElement) {
    if (label.isRepeated()) {
      List<Object> valueList = new ArrayList<>();
      for (JsonElement element : valueElement.getAsJsonArray()) {
        valueList.add(readJson(valueType, element));
      }
      return valueList;
    } else {
      return readJson(valueType, valueElement);
    }
  }

  private void parseUnknownField(JsonReader in, B builder, int tag) throws IOException {
    in.beginArray();
    UnknownFieldType type = UnknownFieldType.of(in.nextString());
    while (in.peek() != JsonToken.END_ARRAY) {
      switch (type) {
        case VARINT:
          long varint = in.nextLong();
          builder.addUnknownField(tag, VARINT, varint);
          break;
        case FIXED32:
          int fixed32 = in.nextInt();
          builder.addUnknownField(tag, FIXED32, fixed32);
          break;
        case FIXED64:
          long fixed64 = in.nextLong();
          builder.addUnknownField(tag, FIXED64, fixed64);
          break;
        case LENGTH_DELIMITED:
          ByteString byteString = ByteString.decodeBase64(in.nextString());
          builder.addUnknownField(tag, LENGTH_DELIMITED, byteString);
          break;
        default:
          throw new AssertionError("Unknown field type " + type);
      }
    }
    in.endArray();
  }

  private Object readJson(Type valueType, JsonElement element) {
    return gson.fromJson(element, valueType);
  }

  private Type singleType(FieldBinding<M, B> tagBinding) {
    return tagBinding.singleAdapter().javaType;
  }
}
