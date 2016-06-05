/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.wire.schema;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates type adapters to read and write protocol buffer data from a schema model. This doesn't
 * require an intermediate code gen step.
 */
final class SchemaProtoAdapterFactory {
  final Schema schema;
  final boolean includeUnknown;
  final Map<ProtoType, ProtoAdapter<?>> adapterMap = new LinkedHashMap<>();

  public SchemaProtoAdapterFactory(Schema schema, boolean includeUnknown) {
    this.schema = schema;
    this.includeUnknown = includeUnknown;

    adapterMap.put(ProtoType.BOOL, ProtoAdapter.BOOL);
    adapterMap.put(ProtoType.BYTES, ProtoAdapter.BYTES);
    adapterMap.put(ProtoType.DOUBLE, ProtoAdapter.DOUBLE);
    adapterMap.put(ProtoType.FLOAT, ProtoAdapter.FLOAT);
    adapterMap.put(ProtoType.FIXED32, ProtoAdapter.FIXED32);
    adapterMap.put(ProtoType.FIXED64, ProtoAdapter.FIXED64);
    adapterMap.put(ProtoType.INT32, ProtoAdapter.INT32);
    adapterMap.put(ProtoType.INT64, ProtoAdapter.INT64);
    adapterMap.put(ProtoType.SFIXED32, ProtoAdapter.SFIXED32);
    adapterMap.put(ProtoType.SFIXED64, ProtoAdapter.SFIXED64);
    adapterMap.put(ProtoType.SINT32, ProtoAdapter.SINT32);
    adapterMap.put(ProtoType.SINT64, ProtoAdapter.SINT64);
    adapterMap.put(ProtoType.STRING, ProtoAdapter.STRING);
    adapterMap.put(ProtoType.UINT32, ProtoAdapter.UINT32);
    adapterMap.put(ProtoType.UINT64, ProtoAdapter.UINT64);
  }

  public ProtoAdapter<Object> get(ProtoType protoType) {
    if (protoType.isMap()) throw new UnsupportedOperationException("map types not supported");

    ProtoAdapter<?> result = adapterMap.get(protoType);
    if (result != null) {
      return (ProtoAdapter<Object>) result;
    }

    Type type = schema.getType(protoType);
    if (type == null) {
      throw new IllegalArgumentException("unknown type: " + protoType);
    }

    if (type instanceof EnumType) {
      EnumAdapter enumAdapter = new EnumAdapter((EnumType) type);
      adapterMap.put(protoType, enumAdapter);
      return enumAdapter;
    }

    if (type instanceof MessageType) {
      MessageAdapter messageAdapter = new MessageAdapter(includeUnknown);
      // Put the adapter in the map early to mitigate the recursive calls to get() made below.
      adapterMap.put(protoType, messageAdapter);

      for (com.squareup.wire.schema.Field field : ((MessageType) type).fields()) {
        Field fieldAdapter = new Field(
            field.name(), field.tag(), field.isRepeated(), get(field.type()));
        messageAdapter.fieldsByName.put(field.name(), fieldAdapter);
        messageAdapter.fieldsByTag.put(field.tag(), fieldAdapter);
      }
      return (ProtoAdapter) messageAdapter;
    }

    throw new IllegalArgumentException("unexpected type: " + protoType);
  }

  static final class EnumAdapter extends ProtoAdapter<Object> {
    final EnumType enumType;

    public EnumAdapter(EnumType enumType) {
      super(FieldEncoding.VARINT, Object.class);
      this.enumType = enumType;
    }

    @Override public int encodedSize(Object value) {
      throw new UnsupportedOperationException();
    }

    @Override public void encode(ProtoWriter writer, Object value) throws IOException {
      if (value instanceof String) {
        EnumConstant constant = enumType.constant((String) value);
        writer.writeVarint32(constant.tag());
      } else if (value instanceof Integer) {
        writer.writeVarint32((Integer) value);
      } else {
        throw new IllegalArgumentException("unexpected " + enumType.type() + ": " + value);
      }
    }

    @Override public Object decode(ProtoReader reader) throws IOException {
      Integer value = ProtoAdapter.UINT32.decode(reader);
      EnumConstant constant = enumType.constant(value);
      return constant != null ? constant.name() : value;
    }
  }

  static final class MessageAdapter extends ProtoAdapter<Map<String, Object>> {
    final Map<Integer, Field> fieldsByTag = new LinkedHashMap<>();
    final Map<String, Field> fieldsByName = new LinkedHashMap<>();
    final boolean includeUnknown;

    public MessageAdapter(boolean includeUnknown) {
      super(FieldEncoding.LENGTH_DELIMITED, Map.class);
      this.includeUnknown = includeUnknown;
    }

    @Override public Map<String, Object> redact(Map<String, Object> message) {
      throw new UnsupportedOperationException();
    }

    @Override public int encodedSize(Map<String, Object> value) {
      int size = 0;
      for (Map.Entry<String, Object> entry : value.entrySet()) {
        Field field = fieldsByName.get(entry.getKey());
        if (field == null) continue; // Ignore unknown values!

        ProtoAdapter<Object> protoAdapter = (ProtoAdapter<Object>) field.protoAdapter;
        if (field.repeated) {
          for (Object o : (List<?>) entry.getValue()) {
            size += protoAdapter.encodedSizeWithTag(field.tag, o);
          }
        } else {
          size += protoAdapter.encodedSizeWithTag(field.tag, entry.getValue());
        }
      }
      return size;
    }

    @Override public void encode(ProtoWriter writer, Map<String, Object> value) throws IOException {
      for (Map.Entry<String, Object> entry : value.entrySet()) {
        Field field = fieldsByName.get(entry.getKey());
        if (field == null) continue; // Ignore unknown values!

        ProtoAdapter<Object> protoAdapter = (ProtoAdapter<Object>) field.protoAdapter;
        if (field.repeated) {
          for (Object o : (List<?>) entry.getValue()) {
            protoAdapter.encodeWithTag(writer, field.tag, o);
          }
        } else {
          protoAdapter.encodeWithTag(writer, field.tag, entry.getValue());
        }
      }
    }

    @Override public Map<String, Object> decode(ProtoReader reader) throws IOException {
      Map<String, Object> result = new LinkedHashMap<>();

      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        Field field = fieldsByTag.get(tag);
        if (field == null) {
          if (includeUnknown) {
            String name = Integer.toString(tag);
            field = new Field(name, tag, true, reader.peekFieldEncoding().rawProtoAdapter());
          } else {
            reader.skip();
            continue;
          }
        }

        Object value = field.protoAdapter.decode(reader);
        if (field.repeated) {
          List<Object> values = (List<Object>) result.get(field.name);
          if (values == null) {
            values = new ArrayList<>();
            result.put(field.name, values);
          }
          values.add(value);
        } else {
          result.put(field.name, value);
        }
      }
      reader.endMessage(token);
      return result;
    }

    @Override public String toString(Map<String, Object> value) {
      throw new UnsupportedOperationException();
    }
  }

  static class Field {
    final String name;
    final int tag;
    final boolean repeated;
    final ProtoAdapter<?> protoAdapter;

    public Field(String name, int tag, boolean repeated, ProtoAdapter<?> protoAdapter) {
      this.name = name;
      this.tag = tag;
      this.repeated = repeated;
      this.protoAdapter = protoAdapter;
    }
  }
}
