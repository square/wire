// Copyright 2013 Square, Inc.
package com.squareup.wire;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * Superclass for protocol buffer messages.
 */
public abstract class Message {

  UnknownFieldMap unknownFieldMap;

  protected Message(Builder builder) {
    if (builder.unknownFieldMap != null) {
      unknownFieldMap = new UnknownFieldMap(builder.unknownFieldMap);
    }
  }

  /**
   * Returns the set of tags associated with unknown varint fields in tag order.
   */
  public synchronized SortedSet<Integer> getUnknownVarintTags() {
    if (unknownFieldMap == null) {
      return UnknownFieldMap.EMPTY_SORTED_SET;
    }
    return unknownFieldMap.getVarintTags();
  }

  /**
   * Returns a {@link List} of all varint values associated with the given tag in the order
   * in which they appeared in the input stream.
   */
  public synchronized List<Long> getUnknownVarintFields(int tag) {
    if (unknownFieldMap == null) {
      return Collections.emptyList();
    }
    return unknownFieldMap.getVarintFields(tag);
  }

  /**
   * Returns the set of tags associated with unknown {@code fixed32} fields in tag order.
   */
  public SortedSet<Integer> getUnknownFixed32Tags() {
    if (unknownFieldMap == null) {
      return UnknownFieldMap.EMPTY_SORTED_SET;
    }
    return unknownFieldMap.getFixed32Tags();
  }

  /**
   * Returns a {@link List} of all {@code fixed32} values associated with the given tag, or null
   * if no fixed32 values are present for that tag.
   */
  public List<Integer> getUnknownFixed32Fields(int tag) {
    if (unknownFieldMap == null) {
      return Collections.emptyList();
    }
    return unknownFieldMap.getFixed32Fields(tag);
  }

  /**
   * Returns the set of tags associated with unknown {@code fixed64} fields in tag order.
   */
  public SortedSet<Integer> getUnknownFixed64Tags() {
    if (unknownFieldMap == null) {
      return UnknownFieldMap.EMPTY_SORTED_SET;
    }
    return unknownFieldMap.getFixed64Tags();
  }

  /**
   * Returns a {@link List} of all {@code fixed64} values associated with the given tag, or null
   * if no fixed64 values are present for that tag.
   */
  public List<Long> getUnknownFixed64Fields(int tag) {
    if (unknownFieldMap == null) {
      return Collections.emptyList();
    }
    return unknownFieldMap.getFixed64Fields(tag);
  }

  /**
   * Returns the set of tags associated with unknown length delimited fields in tag order.
   */
  public Set<Integer> getUnknownLengthDelimitedTags() {
    if (unknownFieldMap == null) {
      return UnknownFieldMap.EMPTY_SORTED_SET;
    }
    return unknownFieldMap.getLengthDelimitedTags();
  }

  /**
   * Returns a {@link List} of all length delimited values associated with the given tag, or null
   * if no length delimited values are present for that tag.
   */
  public List<ByteString> getUnknownLengthDelimitedFields(int tag) {
    if (unknownFieldMap == null) {
      return Collections.emptyList();
    }
    return unknownFieldMap.getLengthDelimitedFields(tag);
  }

  /**
   * Returns the set of tags associated with unknown group fields.
   */
  public Set<Integer> getUnknownGroupTags() {
    if (unknownFieldMap == null) {
      return UnknownFieldMap.EMPTY_SORTED_SET;
    }
    return unknownFieldMap.getGroupTags();
  }

  /**
   * Returns a {@link List} of all group values associated with the given tag, or null
   * if no group values are present for that tag.
   */
  public List<ByteString> getUnknownGroup(int tag) {
    if (unknownFieldMap == null) {
      return Collections.emptyList();
    }
    return unknownFieldMap.getGroupFields(tag);
  }

  /**
   * Superclass for protocol buffer message builders.
   */
  public abstract static class Builder<T extends Message> {

    UnknownFieldMap unknownFieldMap;

    public Builder() {
    }

    public Builder(Message message) {
      if (message != null && message.unknownFieldMap != null) {
        this.unknownFieldMap = new UnknownFieldMap(message.unknownFieldMap);
      }
    }

    /**
     * Adds a {@code varint} value to the unknown field set with the given tag number.
     */
    public void addVarint(int tag, long value) {
      ensureUnknownFieldMap();
      unknownFieldMap.addVarint(tag, value);
    }

    /**
     * Adds a {@code fixed32} value to the unknown field set with the given tag number.
     */
    public void addFixed32(int tag, int value) {
      ensureUnknownFieldMap();
      unknownFieldMap.addFixed32(tag, value);
    }

    /**
     * Adds a {@code fixed64} value to the unknown field set with the given tag number.
     */
    public void addFixed64(int tag, long value) {
      ensureUnknownFieldMap();
      unknownFieldMap.addFixed64(tag, value);
    }

    /**
     * Adds a length delimited value to the unknown field set with the given tag number.
     */
    public void addLengthDelimited(int tag, ByteString value) {
      ensureUnknownFieldMap();
      unknownFieldMap.addLengthDelimited(tag, value);
    }

    /**
     * Adds a group value to the unknown field set with the given tag number.
     */
    public void addGroup(int tag, ByteString value) {
      ensureUnknownFieldMap();
      unknownFieldMap.addGroup(tag, value);
    }

    private void ensureUnknownFieldMap() {
      if (unknownFieldMap == null) {
        unknownFieldMap = new UnknownFieldMap();
      }
    }

    /**
     * Returns true if all required fields have been set. The default implementation returns
     * true.
     */
    public boolean isInitialized() {
      return true;
    }

    /**
     * Returns an immutable {@link com.squareup.wire.Message} based on the fields that have been set
     * in this builder.
     */
    public abstract T build();
  }
}
