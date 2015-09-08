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
import com.squareup.wire.WireAdapter;
import com.squareup.wire.WireType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates type adapters to read and write protocol buffer data from a schema model. This doesn't
 * require an intermediate code gen step.
 */
final class SchemaWireAdapterFactory {
  final Schema schema;
  final boolean includeUnknown;
  final Map<WireType, WireAdapter<?>> adapterMap = new LinkedHashMap<>();

  public SchemaWireAdapterFactory(Schema schema, boolean includeUnknown) {
    this.schema = schema;
    this.includeUnknown = includeUnknown;

    adapterMap.put(WireType.BOOL, WireAdapter.BOOL);
    adapterMap.put(WireType.BYTES, WireAdapter.BYTES);
    adapterMap.put(WireType.DOUBLE, WireAdapter.DOUBLE);
    adapterMap.put(WireType.FLOAT, WireAdapter.FLOAT);
    adapterMap.put(WireType.FIXED32, WireAdapter.FIXED32);
    adapterMap.put(WireType.FIXED64, WireAdapter.FIXED64);
    adapterMap.put(WireType.INT32, WireAdapter.INT32);
    adapterMap.put(WireType.INT64, WireAdapter.INT64);
    adapterMap.put(WireType.SFIXED32, WireAdapter.SFIXED32);
    adapterMap.put(WireType.SFIXED64, WireAdapter.SFIXED64);
    adapterMap.put(WireType.SINT32, WireAdapter.SINT32);
    adapterMap.put(WireType.SINT64, WireAdapter.SINT64);
    adapterMap.put(WireType.STRING, WireAdapter.STRING);
    adapterMap.put(WireType.UINT32, WireAdapter.UINT32);
    adapterMap.put(WireType.UINT64, WireAdapter.UINT64);
  }

  public WireAdapter<Object> get(WireType wireType) {
    WireAdapter<?> result = adapterMap.get(wireType);
    if (result != null) {
      return (WireAdapter<Object>) result;
    }

    Type type = schema.getType(wireType);
    if (type == null) {
      throw new IllegalArgumentException("unknown type: " + wireType);
    }

    if (type instanceof EnumType) {
      EnumAdapter enumAdapter = new EnumAdapter((EnumType) type);
      adapterMap.put(wireType, enumAdapter);
      return enumAdapter;
    }

    if (type instanceof MessageType) {
      MessageAdapter messageAdapter = new MessageAdapter(includeUnknown);
      // Put the adapter in the map early to mitigate the recursive calls to get() made below.
      adapterMap.put(wireType, messageAdapter);

      for (com.squareup.wire.schema.Field field : ((MessageType) type).fields()) {
        Field fieldAdapter = new Field(
            field.name(), field.tag(), field.isRepeated(), get(field.type()));
        messageAdapter.fieldsByName.put(field.name(), fieldAdapter);
        messageAdapter.fieldsByTag.put(field.tag(), fieldAdapter);
      }
      return (WireAdapter) messageAdapter;
    }

    throw new IllegalArgumentException("unexpected type: " + wireType);
  }

  static final class EnumAdapter extends WireAdapter<Object> {
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
        throw new IllegalArgumentException("unexpected " + enumType.name() + ": " + value);
      }
    }

    @Override public Object decode(ProtoReader reader) throws IOException {
      Integer value = WireAdapter.UINT32.decode(reader);
      EnumConstant constant = enumType.constant(value);
      return constant != null ? constant.name() : value;
    }
  }

  static final class MessageAdapter extends WireAdapter<Map<String, Object>> {
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

        WireAdapter<Object> wireAdapter = (WireAdapter<Object>) field.wireAdapter;
        if (field.repeated) {
          for (Object o : (List<?>) entry.getValue()) {
            size += wireAdapter.encodedSize(field.tag, o);
          }
        } else {
          size += wireAdapter.encodedSize(field.tag, entry.getValue());
        }
      }
      return size;
    }

    @Override public void encode(ProtoWriter writer, Map<String, Object> value) throws IOException {
      for (Map.Entry<String, Object> entry : value.entrySet()) {
        Field field = fieldsByName.get(entry.getKey());
        if (field == null) continue; // Ignore unknown values!

        WireAdapter<Object> wireAdapter = (WireAdapter<Object>) field.wireAdapter;
        if (field.repeated) {
          for (Object o : (List<?>) entry.getValue()) {
            wireAdapter.encodeTagged(writer, field.tag, o);
          }
        } else {
          wireAdapter.encodeTagged(writer, field.tag, entry.getValue());
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
            field = new Field(name, tag, true, reader.peekFieldEncoding().rawWireAdapter());
          } else {
            reader.skip();
            continue;
          }
        }

        Object value = field.wireAdapter.decode(reader);
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
    final WireAdapter<?> wireAdapter;

    public Field(String name, int tag, boolean repeated, WireAdapter<?> wireAdapter) {
      this.name = name;
      this.tag = tag;
      this.repeated = repeated;
      this.wireAdapter = wireAdapter;
    }
  }
}
