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

  abstract static class FieldValue {
    private final int tag;
    private final WireType wireType;

    public FieldValue(int tag, WireType wireType) {
      this.tag = tag;
      this.wireType = wireType;
    }

    public static VarintFieldValue varint(int tag, Long value) {
      return new VarintFieldValue(tag, value);
    }

    public static Fixed32FieldValue fixed32(int tag, Integer value) {
      return new Fixed32FieldValue(tag, value);
    }

    public static Fixed64FieldValue fixed64(int tag, Long value) {
      return new Fixed64FieldValue(tag, value);
    }

    public static LengthDelimitedFieldValue lengthDelimited(int tag, ByteString value) {
      return new LengthDelimitedFieldValue(tag, value);
    }

    public abstract int getSerializedSize();

    public abstract void write(int tag, WireOutput output) throws IOException;

    public int getTag() {
      return tag;
    }

    public WireType getWireType() {
      return wireType;
    }

    public Integer getAsInteger() {
      throw new IllegalStateException();
    }

    public Long getAsLong() {
      throw new IllegalStateException();
    }

    public ByteString getAsBytes() {
      throw new IllegalStateException();
    }
  }

  static final class VarintFieldValue extends FieldValue {
    private final Long value;

    public VarintFieldValue(int tag, Long value) {
      super(tag, WireType.VARINT);
      this.value = value;
    }

    @Override public int getSerializedSize() {
      return WireOutput.varint64Size(value);
    }

    @Override public void write(int tag, WireOutput output) throws IOException {
      output.writeTag(tag, WireType.VARINT);
      output.writeVarint64(value);
    }

    @Override public Long getAsLong() {
      return value;
    }
  }

  static final class Fixed32FieldValue extends FieldValue {
    private final Integer value;

    public Fixed32FieldValue(int tag, Integer value) {
      super(tag, WireType.FIXED32);
      this.value = value;
    }

    @Override public int getSerializedSize() {
      return WireType.FIXED_32_SIZE;
    }

    @Override public void write(int tag, WireOutput output) throws IOException {
      output.writeTag(tag, WireType.FIXED32);
      output.writeFixed32(value);
    }

    @Override public Integer getAsInteger() {
      return value;
    }
  }

  static final class Fixed64FieldValue extends FieldValue {
    private final Long value;

    public Fixed64FieldValue(int tag, Long value) {
      super(tag, WireType.FIXED64);
      this.value = value;
    }

    @Override public int getSerializedSize() {
      return WireType.FIXED_64_SIZE;
    }

    @Override public void write(int tag, WireOutput output) throws IOException {
      output.writeTag(tag, WireType.FIXED64);
      output.writeFixed64(value);
    }

    @Override public Long getAsLong() {
      return value;
    }
  }

  static final class LengthDelimitedFieldValue extends FieldValue {
    private final ByteString value;

    public LengthDelimitedFieldValue(int tag, ByteString value) {
      super(tag, WireType.LENGTH_DELIMITED);
      this.value = value;
    }

    @Override public int getSerializedSize() {
      return WireOutput.varint32Size(value.size()) + value.size();
    }

    @Override public void write(int tag, WireOutput output) throws IOException {
      output.writeTag(tag, WireType.LENGTH_DELIMITED);
      output.writeVarint32(value.size());
      output.writeRawBytes(value.toByteArray());
    }

    @Override public ByteString getAsBytes() {
      return value;
    }
  }

  Map<Integer, List<FieldValue>> fieldMap;

  UnknownFieldMap() {
  }

  UnknownFieldMap(UnknownFieldMap other) {
    if (other.fieldMap != null) {
      ensureFieldMap().putAll(other.fieldMap);
    }
  }

  void addVarint(int tag, Long value) throws IOException {
    addElement(ensureFieldMap(), tag, value, WireType.VARINT);
  }

  void addFixed32(int tag, Integer value) throws IOException {
    addElement(ensureFieldMap(), tag, value, WireType.FIXED32);
  }

  void addFixed64(int tag, Long value) throws IOException {
    addElement(ensureFieldMap(), tag, value, WireType.FIXED64);
  }

  void addLengthDelimited(int tag, ByteString value) throws IOException {
    addElement(ensureFieldMap(), tag, value, WireType.LENGTH_DELIMITED);
  }

  private Map<Integer, List<FieldValue>> ensureFieldMap() {
    if (fieldMap == null) {
      fieldMap = new TreeMap<Integer, List<FieldValue>>();
    }
    return fieldMap;
  }

  /**
   * @throws IOException if the added element's type doesn't match the types of the other elements
   *     with the same tag.
   */
  private <T> void addElement(Map<Integer, List<FieldValue>> map, int tag, T value,
      WireType wireType) throws IOException {
    List<FieldValue> values = map.get(tag);
    if (values == null) {
      values = new ArrayList<FieldValue>();
      map.put(tag, values);
    }
    FieldValue fieldValue;
    switch (wireType) {
      case VARINT: fieldValue = FieldValue.varint(tag, (Long) value); break;
      case FIXED32: fieldValue = FieldValue.fixed32(tag, (Integer) value); break;
      case FIXED64: fieldValue = FieldValue.fixed64(tag, (Long) value); break;
      case LENGTH_DELIMITED:
        fieldValue = FieldValue.lengthDelimited(tag, (ByteString) value);
        break;
      default:
        throw new IllegalArgumentException("Unsupported wireType = " + wireType);
    }
    if (values.size() > 0 && values.get(0).getWireType() != fieldValue.getWireType()) {
      throw new IOException(String.format(
          "Wire type %s differs from previous type %s for tag %s", fieldValue.getWireType(),
          values.get(0).getWireType(), tag));
    }
    values.add(fieldValue);
  }

  int getSerializedSize() {
    int size = 0;
    if (fieldMap != null) {
      for (Map.Entry<Integer, List<FieldValue>> entry : fieldMap.entrySet()) {
        size += WireOutput.varintTagSize(entry.getKey());
        for (FieldValue value : entry.getValue()) {
          size += value.getSerializedSize();
        }
      }
    }
    return size;
  }

  void write(WireOutput output) throws IOException {
    if (fieldMap != null) {
      for (Map.Entry<Integer, List<FieldValue>> entry : fieldMap.entrySet()) {
        int tag = entry.getKey();
        for (FieldValue value : entry.getValue()) {
          value.write(tag, output);
        }
      }
    }
  }
}
