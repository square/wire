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
import java.util.Collections;
import java.util.List;

/**
 * Superclass for protocol buffer messages.
 */
public abstract class Message {

  private static final UnknownFieldMap EMPTY_UNKNOWN_FIELD_MAP = new UnknownFieldMap();

  // Hidden Wire instance that can perform work that does not require knowledge of extensions.
  private static final Wire WIRE = new Wire();

  /**
   * A protocol buffer data type.
   */
  public enum Datatype {
    INT32(1), INT64(2), UINT32(3), UINT64(4), SINT32(5),
    SINT64(6), BOOL(7), ENUM(8), STRING(9), BYTES(10),
    MESSAGE(11), FIXED32(12), SFIXED32(13), FIXED64(14),
    SFIXED64(15), FLOAT(16), DOUBLE(17);

    private static final int TYPE_MASK = 0x1f;

    public static Datatype valueOf(int value) {
      switch (value & TYPE_MASK) {
        case 1: return INT32;
        case 2: return INT64;
        case 3: return UINT32;
        case 4: return UINT64;
        case 5: return SINT32;
        case 6: return SINT64;
        case 7: return BOOL;
        case 8: return ENUM;
        case 9: return STRING;
        case 10: return BYTES;
        case 11: return MESSAGE;
        case 12: return FIXED32;
        case 13: return SFIXED32;
        case 14: return FIXED64;
        case 15: return SFIXED64;
        case 16: return FLOAT;
        case 17: return DOUBLE;
        default: throw new IllegalArgumentException("value = " + value);
      }
    }

    private final int value;

    private Datatype(int value) {
      this.value = value;
    }

    public int value() {
      return value;
    }
  }

  /**
   * A protocol buffer label. We treat "packed" as a label of its own that implies "repeated."
   */
  public enum Label {
    REQUIRED(32), OPTIONAL(64), REPEATED(128), PACKED(256);

    private static final int LABEL_MASK = 0x1e0;

    public static Label valueOf(int value) {
      switch (value & LABEL_MASK) {
        case 32: return REQUIRED;
        case 64: return OPTIONAL;
        case 128: return REPEATED;
        case 256: return PACKED;
        default: throw new IllegalArgumentException("value = " + value);
      }
    }

    private final int value;

    private Label(int value) {
      this.value = value;
    }

    public int value() {
      return value;
    }

    public boolean isRepeated() {
      return this == REPEATED || this == PACKED;
    }

    public boolean isPacked() {
      return this == PACKED;
    }
  }

  /** Use EMPTY_UNKNOWN_FIELD_MAP until a field is added. */
  transient UnknownFieldMap unknownFieldMap = EMPTY_UNKNOWN_FIELD_MAP;

  /** If non-zero, the hash code of this message. */
  protected transient int hashCode = 0;

  /** If >= 0, the serialized size of this message. */
  private transient int cachedSerializedSize = -1;

  /**
   * Constructs a Message, initialized with any unknown field data stored in the given
   * {@code Builder}.
   */
  protected Message(Builder builder) {
    if (builder.unknownFieldMap != EMPTY_UNKNOWN_FIELD_MAP) {
      unknownFieldMap = new UnknownFieldMap(builder.unknownFieldMap);
    }
  }

  /**
   * Utility method to return a copy of a given List. Used by generated code.
   */
  protected static <T> List<T> copyOf(List<T> source) {
    return source == null ? null : new ArrayList<T>(source);
  }

  /**
   * Utility method to return an unmodifiable copy of a given List. Used by generated code.
   */
  protected static <T> List<T> unmodifiableCopyOf(List<T> source) {
    return source == null ? null : Collections.unmodifiableList(new ArrayList<T>(source));
  }

  /**
   * Returns an instance of the given message class, with all fields unset.
   *
   * @param messageClass the class of the desired {@link Message}
   * @param <M> the Message type
   * @return an instance of the desired Message class
   */
  public static <M extends Message> M getDefaultInstance(Class<M> messageClass) {
    return WIRE.messageAdapter(messageClass).getDefaultInstance();
  }

  /**
   * Returns the integer value tagged associated with the given enum instance.
   * If the enum value is not annotated with a {@link ProtoEnum} annotation, an exception
   * will be thrown.
   *
   * @param <E> the enum class type
   */
  @SuppressWarnings("unchecked")
  public static <E extends Enum> int intFromEnum(E value) {
    EnumAdapter<E> adapter = WIRE.enumAdapter((Class<E>) value.getClass());
    return adapter.toInt(value);
  }

  /**
   * Returns the enumerated value tagged with the given integer value for the
   * given enum class. If no enum value in the given class is annotated with a {@link ProtoEnum}
   * annotation having the given value, null is returned.
   *
   * @param <E> the enum class type
   */
  public static <E extends Enum> E enumFromInt(Class<E> enumClass, int value) {
    EnumAdapter<E> adapter = WIRE.enumAdapter(enumClass);
    return adapter.fromInt(value);
  }

  @SuppressWarnings("unchecked")
  public byte[] toByteArray() {
    return WIRE.messageAdapter((Class<Message>) getClass()).toByteArray(this);
  }

  public void writeTo(byte[] output) {
    writeTo(output, 0, output.length);
  }

  public void writeTo(byte[] output, int offset, int count) {
    write(WireOutput.newInstance(output, offset, count));
  }

  @SuppressWarnings("unchecked")
  private void write(WireOutput output) {
    MessageAdapter<Message> adapter = WIRE.messageAdapter((Class<Message>) getClass());
    try {
      adapter.write(this, output);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public int getSerializedSize() {
    if (cachedSerializedSize < 0) {
      cachedSerializedSize =
          WIRE.messageAdapter((Class<Message>) getClass()).getSerializedSize(this);
    }
    return cachedSerializedSize;
  }

  protected boolean equals(Object a, Object b) {
    return a == b || (a != null && a.equals(b));
  }

  @SuppressWarnings("unchecked")
  @Override public String toString() {
    return WIRE.messageAdapter((Class<Message>) getClass()).toString(this);
  }

  /**
   * Superclass for protocol buffer message builders.
   */
  public abstract static class Builder<T extends Message> {

    // Avoid call to accessor method for private field
    private static final UnknownFieldMap EMPTY_UNKNOWN_FIELD_MAP = Message.EMPTY_UNKNOWN_FIELD_MAP;

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
     * Throws an exception if a required field has not been set.
     */
    public void checkRequiredFields() {
      WIRE.builderAdapter(getClass()).checkRequiredFields(this);
    }

    /**
     * Returns an immutable {@link com.squareup.wire.Message} based on the fields that have been set
     * in this builder.
     */
    public abstract T build();
  }
}
