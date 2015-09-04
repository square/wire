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
import okio.ByteString;

import static com.squareup.wire.Message.Datatype;
import static com.squareup.wire.Message.Label;
import static java.util.Collections.unmodifiableMap;

class MessageTypeAdapter<M extends Message<M>, B extends Message.Builder<M, B>>
    extends TypeAdapter<M> {

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
  private final Map<String, RegisteredExtension> extensions;

  @SuppressWarnings("unchecked")
  public MessageTypeAdapter(Wire wire, Gson gson, TypeToken<M> type) {
    this.gson = gson;
    this.messageAdapter = wire.messageAdapter((Class<M>) type.getRawType());

    Map<String, FieldBinding<M, B>> fieldBindings = new LinkedHashMap<String, FieldBinding<M, B>>();
    for (FieldBinding<M, B> binding : messageAdapter.fieldBindings().values()) {
      fieldBindings.put(binding.name, binding);
    }
    this.fieldBindings = unmodifiableMap(fieldBindings);

    Map<String, RegisteredExtension> extensions = new LinkedHashMap<String, RegisteredExtension>();
    for (RegisteredExtension extension : messageAdapter.extensions().values()) {
      extensions.put(extension.name, extension);
    }
    this.extensions = extensions;
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
      emitJson(out, value, tagBinding.datatype, tagBinding.label);
    }

    TagMap tagMap = message.tagMap();
    if (tagMap != null) {
      for (Extension<?, ?> extension : tagMap.extensions(true)) {
        if (extension.isUnknown()) {
          List<?> values = (List<?>) tagMap.get(extension);
          if (values.isEmpty()) continue;

          out.name(Integer.toString(extension.getTag()));
          out.beginArray();
          switch (extension.getDatatype()) {
            case UINT64:
              out.value("varint");
              for (Object o : values) {
                out.value((Long) o);
              }
              break;
            case FIXED32:
              out.value("fixed32");
              for (Object o : values) {
                out.value((Integer) o);
              }
              break;
            case FIXED64:
              out.value("fixed64");
              for (Object o : values) {
                out.value((Long) o);
              }
              break;
            case BYTES:
              out.value("length-delimited");
              for (Object o : values) {
                out.value(((ByteString) o).base64());
              }
              break;
            default:
              throw new AssertionError("Unknown wire type " + extension.getDatatype());
          }
          out.endArray();
        } else {
          Object value = tagMap.get(extension);
          out.name(extension.getName());
          emitJson(out, value, extension.getDatatype(), extension.getLabel());
        }
      }
    }

    out.endObject();
  }

  @SuppressWarnings("unchecked")
  private void emitJson(JsonWriter out, Object value, Datatype datatype, Label label)
      throws IOException {
    if (datatype == Datatype.UINT64) {
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

    Message.Builder<M, B> builder = messageAdapter.newBuilder();
    in.beginObject();

    while (in.peek() == JsonToken.NAME) {
      String name = in.nextName();

      FieldBinding<M, B> fieldBinding = fieldBindings.get(name);
      if (fieldBinding != null) {
        Object value = parseValue(fieldBinding.label, singleType(fieldBinding), parse(in));
        fieldBinding.set(builder, value);
        continue;
      }

      RegisteredExtension registeredExtension = extensions.get(name);
      if (registeredExtension != null) {
        Object value = parseValue(registeredExtension.extension.getLabel(),
            registeredExtension.adapter.javaType, parse(in));
        builder.setExtension((Extension) registeredExtension.extension, value);
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
      List<Object> valueList = new ArrayList<Object>();
      for (JsonElement element : valueElement.getAsJsonArray()) {
        valueList.add(readJson(valueType, element));
      }
      return valueList;
    } else {
      return readJson(valueType, valueElement);
    }
  }

  private void parseUnknownField(JsonReader in, Message.Builder<M, B> builder, int tag)
      throws IOException {
    in.beginArray();
    UnknownFieldType type = UnknownFieldType.of(in.nextString());
    while (in.peek() != JsonToken.END_ARRAY) {
      switch (type) {
        case VARINT:
          builder.addVarint(tag, in.nextInt());
          break;
        case FIXED32:
          builder.addFixed32(tag, in.nextInt());
          break;
        case FIXED64:
          builder.addFixed64(tag, in.nextInt());
          break;
        case LENGTH_DELIMITED:
          builder.addLengthDelimited(tag, ByteString.decodeBase64(in.nextString()));
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
    return tagBinding.singleAdapter.javaType;
  }
}
