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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Superclass for protocol buffer messages.
 */
public abstract class Message {

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

    public static final Comparator<Datatype> ORDER_BY_NAME = new Comparator<Datatype>() {
      @Override public int compare(Datatype o1, Datatype o2) {
        return o1.name().compareTo(o2.name());
      }
    };

    private static final Map<String, Datatype> TYPES_BY_NAME =
        new LinkedHashMap<String, Datatype>();
    static {
      TYPES_BY_NAME.put("int32", INT32);
      TYPES_BY_NAME.put("int64", INT64);
      TYPES_BY_NAME.put("uint32", UINT32);
      TYPES_BY_NAME.put("uint64", UINT64);
      TYPES_BY_NAME.put("sint32", SINT32);
      TYPES_BY_NAME.put("sint64", SINT64);
      TYPES_BY_NAME.put("bool", BOOL);
      TYPES_BY_NAME.put("enum", ENUM);
      TYPES_BY_NAME.put("string", STRING);
      TYPES_BY_NAME.put("bytes", BYTES);
      TYPES_BY_NAME.put("message", MESSAGE);
      TYPES_BY_NAME.put("fixed32", FIXED32);
      TYPES_BY_NAME.put("sfixed32", SFIXED32);
      TYPES_BY_NAME.put("fixed64", FIXED64);
      TYPES_BY_NAME.put("sfixed64", SFIXED64);
      TYPES_BY_NAME.put("float", FLOAT);
      TYPES_BY_NAME.put("double", DOUBLE);
    }

    private final int value;

    private Datatype(int value) {
      this.value = value;
    }

    public int value() {
      return value;
    }

    public WireType wireType() {
      switch (this) {
        case INT32: case INT64: case UINT32: case UINT64:
        case SINT32: case SINT64: case BOOL: case ENUM:
          return WireType.VARINT;
        case FIXED32: case SFIXED32: case FLOAT:
          return WireType.FIXED32;
        case FIXED64: case SFIXED64: case DOUBLE:
          return WireType.FIXED64;
        case STRING: case BYTES: case MESSAGE:
          return WireType.LENGTH_DELIMITED;
        default:
          throw new AssertionError("No wiretype for datatype " + this);
      }
    }

    public static Datatype of(String typeString) {
      return TYPES_BY_NAME.get(typeString);
    }
  }

  /**
   * A protocol buffer label. We treat "packed" as a label of its own that implies "repeated."
   */
  public enum Label {
    REQUIRED(32), OPTIONAL(64), REPEATED(128), PACKED(256);

    public static final Comparator<Label> ORDER_BY_NAME = new Comparator<Label>() {
      @Override public int compare(Label o1, Label o2) {
        return o1.name().compareTo(o2.name());
      }
    };

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

  /** Set to null until a field is added. */
  private transient UnknownFieldMap unknownFields;

  /** If >= 0, the serialized size of this message. */
  private transient int cachedSerializedSize = -1;

  /** If non-zero, the hash code of this message. Accessed by generated code. */
  protected transient int hashCode = 0;

  /**
   * Constructs a Message, initialized with any unknown field data stored in the given
   * {@code Builder}.
   */
  protected Message(Builder builder) {
    if (builder.unknownFieldMap != null) {
      unknownFields = new UnknownFieldMap(builder.unknownFieldMap);
    }
  }

  Collection<List<UnknownFieldMap.FieldValue>> unknownFields() {
    return unknownFields == null ? Collections.<List<UnknownFieldMap.FieldValue>>emptySet()
        : unknownFields.fieldMap.values();
  }

  /**
   * Utility method to return a mutable copy of a given List. Used by generated code.
   */
  protected static <T> List<T> copyOf(List<T> source) {
    return source == null ? null : new ArrayList<T>(source);
  }

  /**
   * Utility method to return an immutable copy of a given List. Used by generated code.
   */
  protected static <T> List<T> immutableCopyOf(List<T> source) {
    return source == null ? null : Collections.unmodifiableList(new ArrayList<T>(source));
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

  public void writeUnknownFieldMap(WireOutput output) throws IOException {
    if (unknownFields != null) {
      unknownFields.write(output);
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

  public int getUnknownFieldsSerializedSize() {
    return unknownFields == null ? 0 : unknownFields.getSerializedSize();
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

    UnknownFieldMap unknownFieldMap;

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
      if (message != null && message.unknownFields != null) {
        this.unknownFieldMap = new UnknownFieldMap(message.unknownFields);
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

    private UnknownFieldMap ensureUnknownFieldMap() {
      if (unknownFieldMap == null) {
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
