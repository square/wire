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
   * Constant indicating the protocol buffer 'int32' datatype.
   */
  public static final int INT32 = 1;

  /**
   * Constant indicating the protocol buffer 'int64' datatype.
   */
  public static final int INT64 = 2;

  /**
   * Constant indicating the protocol buffer 'uint32' datatype.
   */
  public static final int UINT32 = 3;

  /**
   * Constant indicating the protocol buffer 'unit64' datatype.
   */
  public static final int UINT64 = 4;

  /**
   * Constant indicating the protocol buffer 'sint32' datatype.
   */
  public static final int SINT32 = 5;

  /**
   * Constant indicating the protocol buffer 'sint64' datatype.
   */
  public static final int SINT64 = 6;

  /**
   * Constant indicating the protocol buffer 'bool' datatype.
   */
  public static final int BOOL = 7;

  /**
   * Constant indicating the protocol buffer 'enum' datatype.
   */
  public static final int ENUM = 8;

  /**
   * Constant indicating the protocol buffer 'string' datatype.
   */
  public static final int STRING = 9;

  /**
   * Constant indicating the protocol buffer 'bytes' datatype.
   */
  public static final int BYTES = 10;

  /**
   * Constant indicating the protocol buffer 'message' datatype.
   */
  public static final int MESSAGE = 11;

  /**
   * Constant indicating the protocol buffer 'fixed32' datatype.
   */
  public static final int FIXED32 = 12;

  /**
   * Constant indicating the protocol buffer 'sfixed32' datatype.
   */
  public static final int SFIXED32 = 13;

  /**
   * Constant indicating the protocol buffer 'fixed64' datatype.
   */
  public static final int FIXED64 = 14;

  /**
   * Constant indicating the protocol buffer 'sfixed64' datatype.
   */
  public static final int SFIXED64 = 15;

  /**
   * Constant indicating the protocol buffer 'float' datatype.
   */
  public static final int FLOAT = 16;

  /**
   * Constant indicating the protocol buffer 'double' datatype.
   */
  public static final int DOUBLE = 17;

  /**
   * Constant indicating the protocol buffer 'required' label.
   */
  public static final int REQUIRED = 32;

  /**
   * Constant indicating the protocol buffer 'optional' label.
   */
  public static final int OPTIONAL = 64;

  /**
   * Constant indicating the protocol buffer 'repeated' label.
   */
  public static final int REPEATED = 128;

  /**
   * Constant indicating the protocol buffer '[packed = true]' extension.
   */
  public static final int PACKED = 256;

  static final int TYPE_MASK = 0x1f;
  static final int LABEL_MASK = 0xe0;
  static final int PACKED_MASK = 0x100;

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
