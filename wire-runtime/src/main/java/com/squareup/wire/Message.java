// Copyright 2013 Square, Inc.
package com.squareup.wire;

/**
 * Superclass for protocol buffer messages.
 */
public abstract class Message {

  private static final UnknownFieldMap EMPTY_UNKNOWN_FIELD_MAP = new UnknownFieldMap();

  /** Use EMPTY_UNKNOWN_FIELD_MAP until a field is added. */
  transient UnknownFieldMap unknownFieldMap = EMPTY_UNKNOWN_FIELD_MAP;

  /** If non-zero, the hash code of this message. */
  protected transient int hashCode = 0;
  /** If >= 0, the serialized size of this message. */
  transient int cachedSerializedSize = -1;

  /**
   * Constructs a Message, initialized with any unknown field data stored in the given
   * {@code Builder}.
   */
  protected Message(Builder builder) {
    if (builder.unknownFieldMap != EMPTY_UNKNOWN_FIELD_MAP) {
      unknownFieldMap = new UnknownFieldMap(builder.unknownFieldMap);
    }
  }

  @Override public String toString() {
    return Wire.toString(this);
  }

  /**
   * Superclass for protocol buffer message builders.
   */
  public abstract static class Builder<T extends Message> {

    UnknownFieldMap unknownFieldMap = EMPTY_UNKNOWN_FIELD_MAP;

    /**
     * Constructs a Builder with no unknown field data.
     */
    public Builder() {
    }

    /**
     * Constructs a Builder with unknown field data initialized to a copy of any unknown
     * field data in the given {@link Message}.
     */
    public Builder(Message message) {
      if (message != null && !message.unknownFieldMap.isEmpty()) {
        this.unknownFieldMap = new UnknownFieldMap(message.unknownFieldMap);
      }
    }

    /**
     * Adds a {@code varint} value to the unknown field set with the given tag number.
     */
    public void addVarint(int tag, long value) {
      ensureUnknownFieldMap().addVarint(tag, value);
    }

    /**
     * Adds a {@code fixed32} value to the unknown field set with the given tag number.
     */
    public void addFixed32(int tag, int value) {
      ensureUnknownFieldMap().addFixed32(tag, value);
    }

    /**
     * Adds a {@code fixed64} value to the unknown field set with the given tag number.
     */
    public void addFixed64(int tag, long value) {
      ensureUnknownFieldMap().addFixed64(tag, value);
    }

    /**
     * Adds a length delimited value to the unknown field set with the given tag number.
     */
    public void addLengthDelimited(int tag, ByteString value) {
      ensureUnknownFieldMap().addLengthDelimited(tag, value);
    }

    /**
     * Adds a group value to the unknown field set with the given tag number.
     */
    public void addGroup(int tag, ByteString value) {
      ensureUnknownFieldMap().addGroup(tag, value);
    }

    private UnknownFieldMap ensureUnknownFieldMap() {
      if (unknownFieldMap == EMPTY_UNKNOWN_FIELD_MAP) {
        unknownFieldMap = new UnknownFieldMap();
      }
      return unknownFieldMap;
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
