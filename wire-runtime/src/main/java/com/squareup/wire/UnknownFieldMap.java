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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import okio.ByteString;

// TODO re-use Extension2 and ExtensionMap to accomplish this.
final class UnknownFieldMap {
  static final class Value<E> {
    final int tag;
    final String name;
    final E value;
    final TypeAdapter<E> adapter;

    public Value(int tag, String name, E value, TypeAdapter<E> adapter) {
      this.tag = tag;
      this.name = name;
      this.value = value;
      this.adapter = adapter;
    }

    int serializedSize() {
      return adapter.serializedSize(value);
    }

    void write(ProtoWriter writer) throws IOException {
      writer.value(tag, value, adapter);
    }
  }

  final Map<Integer, List<Value<?>>> fieldMap;

  UnknownFieldMap() {
    this(null);
  }

  UnknownFieldMap(UnknownFieldMap other) {
    if (other != null && other.fieldMap != null) {
      fieldMap = new TreeMap<Integer, List<Value<?>>>(other.fieldMap);
    } else {
      fieldMap = new TreeMap<Integer, List<Value<?>>>();
    }
  }

  void addVarint(int tag, Long value) throws IOException {
    addElement(tag, new Value<Long>(tag, "varint", value, TypeAdapter.INT64));
  }

  void addFixed32(int tag, Integer value) throws IOException {
    addElement(tag, new Value<Integer>(tag, "fixed32", value, TypeAdapter.FIXED32));
  }

  void addFixed64(int tag, Long value) throws IOException {
    addElement(tag, new Value<Long>(tag, "fixed64", value, TypeAdapter.FIXED64));
  }

  void addLengthDelimited(int tag, ByteString value) throws IOException {
    addElement(tag, new Value<ByteString>(tag, "length-delimited", value, TypeAdapter.BYTES));
  }

  /**
   * @throws IOException if the added element's type doesn't match the types of the other elements
   * with the same tag.
   */
  private void addElement(int tag, Value<?> value) throws IOException {
    List<Value<?>> values = fieldMap.get(tag);
    if (values == null) {
      values = new ArrayList<Value<?>>();
      fieldMap.put(tag, values);
    } else if (values.get(0).adapter != value.adapter) {
      throw new IOException(
          String.format("Wire type %s differs from previous type %s for tag %s", value.name,
              values.get(0).name, tag));
    }
    values.add(value);
  }

  int serializedSize() {
    int size = 0;
    for (Map.Entry<Integer, List<Value<?>>> entry : fieldMap.entrySet()) {
      int tagSize = ProtoWriter.tagSize(entry.getKey());
      List<Value<?>> value = entry.getValue();
      for (int i = 0, count = value.size(); i < count; i++) {
        size += tagSize + value.get(i).serializedSize();
      }
    }
    return size;
  }

  void write(ProtoWriter writer) throws IOException {
    for (Map.Entry<Integer, List<Value<?>>> entry : fieldMap.entrySet()) {
      List<Value<?>> value = entry.getValue();
      for (int i = 0, count = value.size(); i < count; i++) {
        value.get(i).write(writer);
      }
    }
  }

  void read(int tag, ProtoReader reader) throws IOException {
    Value<?> value;
    switch (reader.lastTagType()) {
      case TypeAdapter.TYPE_VARINT:
        value = readValue(tag, "varint", TypeAdapter.INT64, reader);
        break;
      case TypeAdapter.TYPE_FIXED32:
        value = readValue(tag, "fixed32", TypeAdapter.FIXED32, reader);
        break;
      case TypeAdapter.TYPE_FIXED64:
        value = readValue(tag, "fixed64", TypeAdapter.FIXED64, reader);
        break;
      case TypeAdapter.TYPE_LEN_DELIMITED:
        value = readValue(tag, "length-delimited", TypeAdapter.BYTES, reader);
        break;

      case TypeAdapter.TYPE_START_GROUP:
        reader.skipGroup();
        return;
      case TypeAdapter.TYPE_END_GROUP:
        return;

      default:
        throw new IOException("Unknown tag type " + reader.lastTagType());
    }

    addElement(tag, value);
  }

  static <E> Value<E> readValue(int tag, String name, TypeAdapter<E> adapter, ProtoReader reader)
      throws IOException {
    return new Value<E>(tag, name, adapter.read(reader), adapter);
  }
}
