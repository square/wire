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

final class UnknownFieldMap {

  abstract static class Value {
    final int tag;

    public Value(int tag) {
      this.tag = tag;
    }

    /** The size of the tag and value when serialized. */
    abstract int getSerializedSize();
    abstract void write(int tag, WireOutput output) throws IOException;
  }

  static final class VarintValue extends Value {
    final Long value;

    public VarintValue(int tag, Long value) {
      super(tag);
      this.value = value;
    }

    @Override int getSerializedSize() {
      return WireOutput.varintTagSize(tag) + WireOutput.varint64Size(value);
    }

    @Override void write(int tag, WireOutput output) throws IOException {
      output.writeTag(tag, WireType.VARINT);
      output.writeVarint64(value);
    }
  }

  static final class Fixed32Value extends Value {
    final Integer value;

    Fixed32Value(int tag, Integer value) {
      super(tag);
      this.value = value;
    }

    @Override int getSerializedSize() {
      return WireOutput.varintTagSize(tag) + WireType.FIXED_32_SIZE;
    }

    @Override void write(int tag, WireOutput output) throws IOException {
      output.writeTag(tag, WireType.FIXED32);
      output.writeFixed32(value);
    }
  }

  static final class Fixed64Value extends Value {
    final Long value;

    Fixed64Value(int tag, Long value) {
      super(tag);
      this.value = value;
    }

    @Override int getSerializedSize() {
      return WireOutput.varintTagSize(tag) + WireType.FIXED_64_SIZE;
    }

    @Override void write(int tag, WireOutput output) throws IOException {
      output.writeTag(tag, WireType.FIXED64);
      output.writeFixed64(value);
    }
  }

  static final class LengthDelimitedValue extends Value {
    final ByteString value;

    LengthDelimitedValue(int tag, ByteString value) {
      super(tag);
      this.value = value;
    }

    @Override int getSerializedSize() {
      return WireOutput.varintTagSize(tag) + WireOutput.varint32Size(value.size()) + value.size();
    }

    @Override void write(int tag, WireOutput output) throws IOException {
      output.writeTag(tag, WireType.LENGTH_DELIMITED);
      output.writeVarint32(value.size());
      output.writeRawBytes(value);
    }
  }

  final Map<Integer, List<Value>> fieldMap;

  UnknownFieldMap() {
    this(null);
  }

  UnknownFieldMap(UnknownFieldMap other) {
    if (other != null && other.fieldMap != null) {
      fieldMap = new TreeMap<Integer, List<Value>>(other.fieldMap);
    } else {
      fieldMap = new TreeMap<Integer, List<Value>>();
    }
  }

  void addVarint(int tag, Long value) throws IOException {
    addElement(tag, new VarintValue(tag, value));
  }

  void addFixed32(int tag, Integer value) throws IOException {
    addElement(tag, new Fixed32Value(tag, value));
  }

  void addFixed64(int tag, Long value) throws IOException {
    addElement(tag, new Fixed64Value(tag, value));
  }

  void addLengthDelimited(int tag, ByteString value) throws IOException {
    addElement(tag, new LengthDelimitedValue(tag, value));
  }

  /**
   * @throws IOException if the added element's type doesn't match the types of the other elements
   *     with the same tag.
   */
  private void addElement(int tag, Value value) throws IOException {
    List<Value> values = fieldMap.get(tag);
    if (values == null) {
      values = new ArrayList<Value>();
      fieldMap.put(tag, values);
    } else if (values.get(0).getClass() != value.getClass()) {
      throw new IOException(String.format("Wire type %s differs from previous type %s for tag %s",
          value.getClass().getSimpleName(), values.get(0).getClass().getSimpleName(), tag));
    }
    values.add(value);
  }

  int getSerializedSize() {
    int size = 0;
    for (Map.Entry<Integer, List<Value>> entry : fieldMap.entrySet()) {
      for (Value value : entry.getValue()) {
        size += value.getSerializedSize();
      }
    }
    return size;
  }

  void write(WireOutput output) throws IOException {
    for (Map.Entry<Integer, List<Value>> entry : fieldMap.entrySet()) {
      int tag = entry.getKey();
      for (Value value : entry.getValue()) {
        value.write(tag, output);
      }
    }
  }
}
