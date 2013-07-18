// Copyright 2013 Square, Inc.
package com.squareup.wire;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class UnknownFieldMap {

  private static final Map<Integer, List<Long>> EMPTY_VARINT_MAP = Collections.emptyMap();
  private static final Map<Integer, List<Integer>> EMPTY_FIXED32_MAP = Collections.emptyMap();
  private static final Map<Integer, List<Long>> EMPTY_FIXED64_MAP = Collections.emptyMap();
  private static final Map<Integer, List<ByteString>> EMPTY_LENGTH_DELIMITED_MAP =
      Collections.emptyMap();
  private static final Map<Integer, List<ByteString>> EMPTY_GROUP_MAP = Collections.emptyMap();

  private Map<Integer, List<Long>> varintMap = EMPTY_VARINT_MAP;
  private Map<Integer, List<Integer>> fixed32Map = EMPTY_FIXED32_MAP;
  private Map<Integer, List<Long>> fixed64Map = EMPTY_FIXED64_MAP;
  private Map<Integer, List<ByteString>> lengthDelimitedMap = EMPTY_LENGTH_DELIMITED_MAP;
  private Map<Integer, List<ByteString>> groupMap = EMPTY_GROUP_MAP;

  private boolean isEmpty = true;

  UnknownFieldMap() {
  }

  UnknownFieldMap(UnknownFieldMap other) {
    if (!other.varintMap.isEmpty()) {
      ensureVarintMap().putAll(other.varintMap);
      isEmpty = false;
    }
    if (!other.fixed32Map.isEmpty()) {
      ensureFixed32Map().putAll(other.fixed32Map);
      isEmpty = false;
    }
    if (!other.fixed64Map.isEmpty()) {
      ensureFixed64Map().putAll(other.fixed64Map);
      isEmpty = false;
    }
    if (!other.lengthDelimitedMap.isEmpty()) {
      ensureLengthDelimitedMap().putAll(other.lengthDelimitedMap);
      isEmpty = false;
    }
    if (!other.groupMap.isEmpty()) {
      ensureGroupMap().putAll(other.groupMap);
      isEmpty = false;
    }
  }

  boolean isEmpty() {
    return isEmpty;
  }

  void addVarint(int tag, long value) {
    addElement(ensureVarintMap(), tag, value);
  }

  void addFixed32(int tag, int value) {
    addElement(ensureFixed32Map(), tag, value);
  }

  void addFixed64(int tag, long value) {
    addElement(ensureFixed64Map(), tag, value);
  }

  void addLengthDelimited(int tag, ByteString value) {
    addElement(ensureLengthDelimitedMap(), tag, value);
  }

  void addGroup(int tag, ByteString value) {
    addElement(ensureGroupMap(), tag, value);
  }

  private Map<Integer, List<Long>> ensureVarintMap() {
    if (varintMap == EMPTY_VARINT_MAP) {
      varintMap = new HashMap<Integer, List<Long>>();
    }
    return varintMap;
  }

  private Map<Integer, List<Integer>> ensureFixed32Map() {
    if (fixed32Map == EMPTY_FIXED32_MAP) {
      fixed32Map = new HashMap<Integer, List<Integer>>();
    }
    return fixed32Map;
  }

  private Map<Integer, List<Long>> ensureFixed64Map() {
    if (fixed64Map == EMPTY_FIXED64_MAP) {
      fixed64Map = new HashMap<Integer, List<Long>>();
    }
    return fixed64Map;
  }

  private Map<Integer, List<ByteString>> ensureLengthDelimitedMap() {
    if (lengthDelimitedMap == EMPTY_LENGTH_DELIMITED_MAP) {
      lengthDelimitedMap = new HashMap<Integer, List<ByteString>>();
    }
    return lengthDelimitedMap;
  }

  private Map<Integer, List<ByteString>> ensureGroupMap() {
    if (groupMap == EMPTY_GROUP_MAP) {
      groupMap = new HashMap<Integer, List<ByteString>>();
    }
    return groupMap;
  }

  private <T> void addElement(Map<Integer, List<T>> map, int tag, T value) {
    List<T> values = map.get(tag);
    if (values == null) {
      values = new ArrayList<T>();
      map.put(tag, values);
    }
    values.add(value);
    isEmpty = false;
  }

  int getSerializedSize() {
    if (isEmpty()) {
      return 0;
    }
    int size = varintSize();
    size += fixed32Size();
    size += fixed64Size();
    size += lengthDelimitedSize();
    size += groupSize();
    return size;
  }

  private int varintSize() {
    int size = 0;
    for (Map.Entry<Integer, List<Long>> entry : varintMap.entrySet()) {
      int tag = entry.getKey();
      for (Long value : entry.getValue()) {
        size += CodedOutputByteBufferNano.computeInt64Size(tag, value);
      }
    }
    return size;
  }

  private int fixed32Size() {
    int size = 0;
    for (Map.Entry<Integer, List<Integer>> entry : fixed32Map.entrySet()) {
      int tag = entry.getKey();
      for (Integer value : entry.getValue()) {
        size += CodedOutputByteBufferNano.computeFixed32Size(tag, value);
      }
    }
    return size;
  }

  private int fixed64Size() {
    int size = 0;
    for (Map.Entry<Integer, List<Long>> entry : fixed64Map.entrySet()) {
      int tag = entry.getKey();
      for (Long value : entry.getValue()) {
        size += CodedOutputByteBufferNano.computeFixed64Size(tag, value);
      }
    }
    return size;
  }

  private int lengthDelimitedSize() {
    return lengthDelimitedOrGroupSize(lengthDelimitedMap);
  }

  private int groupSize() {
    return lengthDelimitedOrGroupSize(groupMap);
  }

  private int lengthDelimitedOrGroupSize(Map<Integer, List<ByteString>> map) {
    int size = 0;
    for (Map.Entry<Integer, List<ByteString>> entry : map.entrySet()) {
      Integer tag = entry.getKey();
      for (ByteString data : entry.getValue()) {
        size += CodedOutputByteBufferNano.computeTagSize(tag);
        size += CodedOutputByteBufferNano.computeRawVarint32Size(data.size());
        size += data.size();
      }
    }
    return size;
  }

  void write(CodedOutputByteBufferNano output) throws IOException {
    if (isEmpty()) {
      return;
    }
    writeVarint(output);
    writeFixed32(output);
    writeFixed64(output);
    writeLengthDelimited(output);
    writeGroup(output);
  }

  private void writeVarint(CodedOutputByteBufferNano output) throws IOException {
    for (Map.Entry<Integer, List<Long>> entry : varintMap.entrySet()) {
      int tag = entry.getKey();
      for (Long value : entry.getValue()) {
        output.writeInt64(tag, value);
      }
    }
  }

  private void writeFixed32(CodedOutputByteBufferNano output) throws IOException {
    for (Map.Entry<Integer, List<Integer>> entry : fixed32Map.entrySet()) {
      int tag = entry.getKey();
      for (Integer value : entry.getValue()) {
        output.writeFixed32(tag, value);
      }
    }
  }

  private void writeFixed64(CodedOutputByteBufferNano output) throws IOException {
    for (Map.Entry<Integer, List<Long>> entry : fixed64Map.entrySet()) {
      int tag = entry.getKey();
      for (Long value : entry.getValue()) {
        output.writeFixed64(tag, value);
      }
    }
  }

  private void writeLengthDelimited(CodedOutputByteBufferNano output) throws IOException {
    writeLengthDelimitedOrGroup(output, lengthDelimitedMap,
        WireFormatNano.WIRETYPE_LENGTH_DELIMITED);
  }

  private void writeGroup(CodedOutputByteBufferNano output) throws IOException {
    writeLengthDelimitedOrGroup(output, groupMap, WireFormatNano.WIRETYPE_START_GROUP);
  }

  private void writeLengthDelimitedOrGroup(CodedOutputByteBufferNano output,
      Map<Integer, List<ByteString>> map, int wiretype) throws IOException {
    for (Map.Entry<Integer, List<ByteString>> entry : map.entrySet()) {
      for (ByteString data : entry.getValue()) {
        Integer tag = entry.getKey();
        output.writeTag(tag, wiretype);
        output.writeRawVarint32(data.size());
        output.writeRawBytes(data.toByteArray());
      }
    }
  }
}
