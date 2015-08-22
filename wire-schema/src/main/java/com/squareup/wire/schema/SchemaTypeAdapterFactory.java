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
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.TypeAdapter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates type adapters to read and write protocol buffer data from a schema model. This doesn't
 * require an intermediate code gen step.
 */
final class SchemaTypeAdapterFactory {
  final Schema schema;
  final Map<Type.Name, TypeAdapter<?>> adapterMap = new LinkedHashMap<>();

  public SchemaTypeAdapterFactory(Schema schema) {
    this.schema = schema;

    adapterMap.put(Type.Name.BOOL, TypeAdapter.BOOL);
    adapterMap.put(Type.Name.BYTES, TypeAdapter.BYTES);
    adapterMap.put(Type.Name.DOUBLE, TypeAdapter.DOUBLE);
    adapterMap.put(Type.Name.FLOAT, TypeAdapter.FLOAT);
    adapterMap.put(Type.Name.FIXED32, TypeAdapter.FIXED32);
    adapterMap.put(Type.Name.FIXED64, TypeAdapter.FIXED64);
    adapterMap.put(Type.Name.INT32, TypeAdapter.INT32);
    adapterMap.put(Type.Name.INT64, TypeAdapter.INT64);
    adapterMap.put(Type.Name.SFIXED32, TypeAdapter.SFIXED32);
    adapterMap.put(Type.Name.SFIXED64, TypeAdapter.SFIXED64);
    adapterMap.put(Type.Name.SINT32, TypeAdapter.SINT32);
    adapterMap.put(Type.Name.SINT64, TypeAdapter.SINT64);
    adapterMap.put(Type.Name.STRING, TypeAdapter.STRING);
    adapterMap.put(Type.Name.UINT32, TypeAdapter.UINT32);
    adapterMap.put(Type.Name.UINT64, TypeAdapter.UINT64);
  }

  public TypeAdapter<Map<String, Object>> get(Type.Name typeName) {
    MessageType type = (MessageType) schema.getType(typeName);
    SchemaMessageAdapter messageAdapter = new SchemaMessageAdapter();
    for (Field field : type.fields()) {
      FieldAdapter fieldAdapter = new FieldAdapter(
          field.name(), field.tag(), field.isRepeated(), getTypeAdapter(field.type()));
      messageAdapter.fieldsByName.put(field.name(), fieldAdapter);
      messageAdapter.fieldsByTag.put(field.tag(), fieldAdapter);
    }
    return messageAdapter;
  }

  private synchronized TypeAdapter<?> getTypeAdapter(Type.Name typeName) {
    TypeAdapter<?> result = adapterMap.get(typeName);

    if (result == null) {
      Type type = schema.getType(typeName);
      if (type instanceof EnumType) {
        result = new SchemaEnumAdapter((EnumType) type);

      } else if (type instanceof MessageType) {
        // TODO(swankjesse): re-entrant calls.
        result = get(typeName);

      } else {
        throw new IllegalArgumentException("unexpected type: " + typeName);
      }

      adapterMap.put(typeName, result);
    }

    return result;
  }

  static final class SchemaEnumAdapter extends TypeAdapter<Object> {
    final EnumType enumType;

    public SchemaEnumAdapter(EnumType enumType) {
      super(FieldEncoding.VARINT, Object.class);
      this.enumType = enumType;
    }

    @Override public int encodedSize(Object value) {
      throw new UnsupportedOperationException();
    }

    @Override public void write(ProtoWriter writer, Object value) throws IOException {
      if (value instanceof String) {
        EnumConstant constant = enumType.constant((String) value);
        writer.writeVarint32(constant.tag());
      } else if (value instanceof Integer) {
        writer.writeVarint32((Integer) value);
      } else {
        throw new IllegalArgumentException("unexpected " + enumType.name() + ": " + value);
      }
    }

    @Override public Object read(ProtoReader reader) throws IOException {
      Integer value = TypeAdapter.UINT32.read(reader);
      EnumConstant constant = enumType.constant(value);
      return constant != null ? constant.name() : value;
    }
  }

  static final class SchemaMessageAdapter extends TypeAdapter<Map<String, Object>> {
    final Map<Integer, FieldAdapter> fieldsByTag = new LinkedHashMap<>();
    final Map<String, FieldAdapter> fieldsByName = new LinkedHashMap<>();

    public SchemaMessageAdapter() {
      super(FieldEncoding.LENGTH_DELIMITED, Map.class);
    }

    @Override public Map<String, Object> redact(Map<String, Object> message) {
      throw new UnsupportedOperationException();
    }

    @Override public int encodedSize(Map<String, Object> value) {
      int size = 0;
      for (Map.Entry<String, Object> entry : value.entrySet()) {
        FieldAdapter fieldAdapter = fieldsByName.get(entry.getKey());
        if (fieldAdapter == null) continue; // Ignore unknown values!

        TypeAdapter<Object> typeAdapter = (TypeAdapter<Object>) fieldAdapter.typeAdapter;
        if (fieldAdapter.repeated) {
          for (Object o : (List<?>) entry.getValue()) {
            size += typeAdapter.serializedSize(fieldAdapter.tag, o);
          }
        } else {
          size += typeAdapter.serializedSize(fieldAdapter.tag, entry.getValue());
        }
      }
      return size;
    }

    @Override public void write(ProtoWriter writer, Map<String, Object> value) throws IOException {
      for (Map.Entry<String, Object> entry : value.entrySet()) {
        FieldAdapter fieldAdapter = fieldsByName.get(entry.getKey());
        if (fieldAdapter == null) continue; // Ignore unknown values!

        TypeAdapter<Object> typeAdapter = (TypeAdapter<Object>) fieldAdapter.typeAdapter;
        if (fieldAdapter.repeated) {
          for (Object o : (List<?>) entry.getValue()) {
            typeAdapter.writeTagged(writer, fieldAdapter.tag, o);
          }
        } else {
          typeAdapter.writeTagged(writer, fieldAdapter.tag, entry.getValue());
        }
      }
    }

    @Override public Map<String, Object> read(ProtoReader reader) throws IOException {
      Map<String, Object> result = new LinkedHashMap<>();

      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        FieldAdapter fieldAdapter = fieldsByTag.get(tag);
        if (fieldAdapter == null) {
          fieldAdapter = new FieldAdapter(
              Integer.toString(tag), tag, true, reader.peekFieldEncoding().rawTypeAdapter());
        }

        Object value = fieldAdapter.typeAdapter.read(reader);
        if (fieldAdapter.repeated) {
          List<Object> values = (List<Object>) result.get(fieldAdapter.name);
          if (values == null) {
            values = new ArrayList<>();
            result.put(fieldAdapter.name, values);
          }
          values.add(value);
        } else {
          result.put(fieldAdapter.name, value);
        }
      }
      reader.endMessage(token);
      return result;
    }

    @Override public String toString(Map<String, Object> value) {
      throw new UnsupportedOperationException();
    }
  }

  static class FieldAdapter {
    final String name;
    final int tag;
    final boolean repeated;
    final TypeAdapter<?> typeAdapter;

    public FieldAdapter(String name, int tag, boolean repeated, TypeAdapter<?> typeAdapter) {
      this.name = name;
      this.tag = tag;
      this.repeated = repeated;
      this.typeAdapter = typeAdapter;
    }
  }
}
