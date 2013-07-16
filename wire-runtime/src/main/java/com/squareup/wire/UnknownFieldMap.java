// Copyright 2013 Square, Inc.
package com.squareup.wire;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

final class UnknownFieldMap {

  static final SortedSet<Integer> EMPTY_SORTED_SET =
      Collections.unmodifiableSortedSet(new TreeSet<Integer>());

  // The performance gain for avoiding eager creating of these maps is worth the inconvenience
  // of having to use null checks throughout this class.
  private Map<Integer, List<Long>> varintMap;
  private Map<Integer, List<Integer>> fixed32Map;
  private Map<Integer, List<Long>> fixed64Map;
  private Map<Integer, List<ByteString>> lengthDelimitedMap;
  private Map<Integer, List<ByteString>> groupMap;

  UnknownFieldMap() {
  }

  UnknownFieldMap(UnknownFieldMap other) {
    if (other.varintMap != null) {
      ensureVarintMap();
      varintMap.putAll(other.varintMap);
    }
    if (other.fixed32Map != null) {
      ensureFixed32Map();
      fixed32Map.putAll(other.fixed32Map);
    }
    if (other.fixed64Map != null) {
      ensureFixed64Map();
      fixed64Map.putAll(other.fixed64Map);
    }
    if (other.lengthDelimitedMap != null) {
      ensureLengthDelimitedMap();
      lengthDelimitedMap.putAll(other.lengthDelimitedMap);
    }
    if (other.groupMap != null) {
      ensureGroupMap();
      groupMap.putAll(other.groupMap);
    }
  }

  private synchronized void ensureVarintMap() {
    if (varintMap == null) {
      varintMap = new HashMap<Integer, List<Long>>();
    }
  }

  private synchronized void ensureFixed32Map() {
    if (fixed32Map == null) {
      fixed32Map = new HashMap<Integer, List<Integer>>();
    }
  }

  private synchronized void ensureFixed64Map() {
    if (fixed64Map == null) {
      fixed64Map = new HashMap<Integer, List<Long>>();
    }
  }

  private synchronized void ensureLengthDelimitedMap() {
    if (lengthDelimitedMap == null) {
      lengthDelimitedMap = new HashMap<Integer, List<ByteString>>();
    }
  }

  private synchronized void ensureGroupMap() {
    if (groupMap == null) {
      groupMap = new HashMap<Integer, List<ByteString>>();
    }
  }

  void addVarint(int tag, long value) {
    ensureVarintMap();
    addElement(varintMap, tag, value);
  }

  void addFixed32(int tag, int value) {
    ensureFixed32Map();
    addElement(fixed32Map, tag, value);
  }

  void addFixed64(int tag, long value) {
    ensureFixed64Map();
    addElement(fixed64Map, tag, value);
  }

  void addLengthDelimited(int tag, ByteString value) {
    ensureLengthDelimitedMap();
    addElement(lengthDelimitedMap, tag, value);
  }

  void addGroup(int tag, ByteString value) {
    ensureGroupMap();
    addElement(groupMap, tag, value);
  }

  private <T> void addElement(Map<Integer, List<T>> map, int tag, T value) {
    List<T> values = map.get(tag);
    if (values == null) {
      values = new ArrayList<T>();
      map.put(tag, values);
    }
    values.add(value);
  }

  SortedSet<Integer> getVarintTags() {
    return wrapKeySet(varintMap);
  }

  List<Long> getVarintFields(int tag) {
    return wrap(varintMap.get(tag));
  }

  SortedSet<Integer> getFixed32Tags() {
    return wrapKeySet(fixed32Map);
  }

  List<Integer> getFixed32Fields(int tag) {
    return wrap(fixed32Map.get(tag));
  }

  SortedSet<Integer> getFixed64Tags() {
    return wrapKeySet(fixed64Map);
  }

  List<Long> getFixed64Fields(int tag) {
    return wrap(fixed64Map.get(tag));
  }

  SortedSet<Integer> getLengthDelimitedTags() {
    return wrapKeySet(lengthDelimitedMap);
  }

  List<ByteString> getLengthDelimitedFields(int tag) {
    return wrap(lengthDelimitedMap.get(tag));
  }

  SortedSet<Integer> getGroupTags() {
    return wrapKeySet(groupMap);
  }

  List<ByteString> getGroupFields(int tag) {
    return wrap(groupMap.get(tag));
  }

  private SortedSet<Integer> wrapKeySet(Map<Integer, ?> map) {
    if (map == null || map.isEmpty()) {
      return EMPTY_SORTED_SET;
    }
    return Collections.unmodifiableSortedSet(new TreeSet<Integer>(map.keySet()));
  }

  private <T> List<T> wrap(List<T> list) {
    if (list == null) {
      return Collections.emptyList();
    }
    return new ArrayList<T>(list);
  }

  int getSerializedSize() {
    int size = varintSize();
    size += fixed32Size();
    size += fixed64Size();
    size += lengthDelimitedSize();
    size += groupSize();
    return size;
  }

  private int varintSize() {
    if (varintMap == null) {
      return 0;
    }
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
    if (fixed32Map == null) {
      return 0;
    }
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
    if (fixed64Map == null) {
      return 0;
    }
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
    if (map == null) {
      return 0;
    }
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
    writeVarint(output);
    writeFixed32(output);
    writeFixed64(output);
    writeLengthDelimited(output);
    writeGroup(output);
  }

  private void writeVarint(CodedOutputByteBufferNano output) throws IOException {
    if (varintMap == null) {
      return;
    }
    for (Map.Entry<Integer, List<Long>> entry : varintMap.entrySet()) {
      int tag = entry.getKey();
      for (Long value : entry.getValue()) {
        output.writeInt64(tag, value);
      }
    }
  }

  private void writeFixed32(CodedOutputByteBufferNano output) throws IOException {
    if (fixed32Map == null) {
      return;
    }
    for (Map.Entry<Integer, List<Integer>> entry : fixed32Map.entrySet()) {
      int tag = entry.getKey();
      for (Integer value : entry.getValue()) {
        output.writeFixed32(tag, value);
      }
    }
  }

  private void writeFixed64(CodedOutputByteBufferNano output) throws IOException {
    if (fixed64Map == null) {
      return;
    }
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
    if (map == null) {
      return;
    }
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
