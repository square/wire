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
import com.squareup.wire.UnknownFieldMap.Fixed32Value;
import com.squareup.wire.UnknownFieldMap.Fixed64Value;
import com.squareup.wire.UnknownFieldMap.LengthDelimitedValue;
import com.squareup.wire.UnknownFieldMap.VarintValue;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import okio.ByteString;

import static com.squareup.wire.Message.Datatype;
import static com.squareup.wire.Message.Label;

class MessageTypeAdapter<M extends Message> extends TypeAdapter<M> {

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

  private final Wire wire;
  private final Gson gson;
  private final Class<M> type;

  @SuppressWarnings("unchecked")
  public MessageTypeAdapter(Wire wire, Gson gson, TypeToken<M> type) {
    this.wire = wire;
    this.gson = gson;
    this.type = (Class<M>) type.getRawType();
  }

  @SuppressWarnings("unchecked")
  @Override public void write(JsonWriter out, M message) throws IOException {
    if (message == null) {
      out.nullValue();
      return;
    }

    MessageAdapter<M> messageAdapter = wire.messageAdapter((Class<M>) message.getClass());
    out.beginObject();
    for (MessageAdapter.FieldInfo fieldInfo : messageAdapter.getFields()) {
      Object value = messageAdapter.getFieldValue(message, fieldInfo);
      if (value == null) {
        continue;
      }
      out.name(fieldInfo.name);
      emitJson(out, value, fieldInfo.datatype, fieldInfo.label);
    }

    if (message instanceof ExtendableMessage<?>) {
      emitExtensions((ExtendableMessage<?>) message, out);
    }

    Collection<List<UnknownFieldMap.Value>> unknownFields = message.unknownFields();
    if (unknownFields != null) {
      for (List<UnknownFieldMap.Value> fieldList : unknownFields) {
        int tag = fieldList.get(0).tag;
        out.name("" + tag);
        out.beginArray();
        for (int i = 0, count = fieldList.size(); i < count; i++) {
          UnknownFieldMap.Value unknownField = fieldList.get(i);
          if (unknownField instanceof VarintValue) {
            if (i == 0) out.value("varint");
            out.value(((VarintValue) unknownField).value);
          } else if (unknownField instanceof Fixed32Value) {
            if (i == 0) out.value("fixed32");
            out.value(((Fixed32Value) unknownField).value);
          } else if (unknownField instanceof Fixed64Value) {
            if (i == 0) out.value("fixed64");
            out.value(((Fixed64Value) unknownField).value);
          } else if (unknownField instanceof LengthDelimitedValue) {
            if (i == 0) out.value("length-delimited");
            out.value(((LengthDelimitedValue) unknownField).value.base64());
          } else {
            throw new AssertionError("Unknown wire type " + unknownField.getClass());
          }
        }
        out.endArray();
      }
    }

    out.endObject();
  }

  @SuppressWarnings("unchecked")
  private <M extends ExtendableMessage<M>, E> void emitExtensions(ExtendableMessage<M> message,
      JsonWriter out) throws IOException {
    if (message.extensionMap == null) return;
    for (int i = 0, count = message.extensionMap.size(); i < count; i++) {
      Extension<M, E> extension = (Extension<M, E>) message.extensionMap.getExtension(i);
      E value = (E) message.extensionMap.getExtensionValue(i);
      emitExtension(extension, value, out);
    }
  }

  private <M extends ExtendableMessage<M>, E> void emitExtension(Extension<M, E> extension,
      E value, JsonWriter out) throws IOException {
    out.name(extension.getName());
    emitJson(out, value, extension.getDatatype(), extension.getLabel());
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

    MessageAdapter<M> messageAdapter = wire.messageAdapter(type);
    Message.Builder<M> builder = messageAdapter.newBuilder();
    in.beginObject();

    while (in.peek() == JsonToken.NAME) {
      String name = in.nextName();
      MessageAdapter.FieldInfo fieldInfo = messageAdapter.getField(name);
      if (fieldInfo == null) {
        Extension<?, ?> extension = messageAdapter.getExtension(name);
        if (extension == null) {
          parseUnknownField(in, builder, Integer.parseInt(name));
        } else {
          Type valueType = getType(extension);
          Object value = parseValue(extension.getLabel(), valueType, parse(in));
          ((ExtendableMessage.ExtendableBuilder) builder).setExtension(extension, value);
        }
      } else {
        Type valueType = getType(fieldInfo);
        Object value = parseValue(fieldInfo.label, valueType, parse(in));
        // Use the builder setter method to ensure proper 'oneof' behavior.
        messageAdapter.setBuilderMethod(builder, fieldInfo, value);
      }
    }

    in.endObject();
    return builder.build();
  }

  private JsonElement parse(JsonReader in) {
    return gson.fromJson(in, JsonElement.class);
  }

  private Type getType(MessageAdapter.FieldInfo fieldInfo) {
    Type valueType;
    if (fieldInfo.datatype == Datatype.ENUM) {
      valueType = fieldInfo.enumType;
    } else if (fieldInfo.datatype == Datatype.MESSAGE) {
      valueType = fieldInfo.messageType;
    } else {
      valueType = javaType(fieldInfo.datatype);
    }
    return valueType;
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

  private Type getType(Extension<?, ?> extension) {
    Datatype datatype = extension.getDatatype();
    if (datatype == Datatype.ENUM) {
      return extension.getEnumType();
    } else if (datatype == Datatype.MESSAGE) {
      return extension.getMessageType();
    } else {
      return javaType(datatype);
    }
  }

  private void parseUnknownField(JsonReader in, Message.Builder<M> builder, int tag)
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

  // Returns the Type used to store a given primitive scalar type.
  @SuppressWarnings("unchecked")
  private Type javaType(Datatype datatype) {
    switch (datatype) {
      case INT32:case UINT32:case SINT32:case FIXED32:case SFIXED32:
        return int.class;
      case INT64:case UINT64:case SINT64:case FIXED64:case SFIXED64:
        return long.class;
      case BOOL:
        return boolean.class;
      case FLOAT:
        return float.class;
      case DOUBLE:
        return double.class;
      case STRING:
        return String.class;
      case BYTES:
        return ByteString.class;
      default:
        throw new AssertionError("Unknown datatype: " + datatype);
    }
  }
}
