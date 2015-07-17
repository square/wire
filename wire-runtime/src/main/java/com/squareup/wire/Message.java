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
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import okio.ByteString;

/**
 * Superclass for protocol buffer messages.
 */
public abstract class Message implements Serializable {
  private static final long serialVersionUID = 0L;

  // Hidden Wire instance that can perform work that does not require knowledge of extensions.
  static final Wire WIRE = new Wire();

  /**
   * A protocol buffer data type.
   */
  public enum Datatype {
    INT32(1), INT64(2), UINT32(3), UINT64(4), SINT32(5),
    SINT64(6), BOOL(7), ENUM(8), STRING(9), BYTES(10),
    MESSAGE(11), FIXED32(12), SFIXED32(13), FIXED64(14),
    SFIXED64(15), FLOAT(16), DOUBLE(17);

    private final int value;

    Datatype(int value) {
      this.value = value;
    }

    public int value() {
      return value;
    }

    WireType wireType() {
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
  }

  /**
   * A protocol buffer label. We treat "packed" as a label of its own that implies "repeated."
   */
  public enum Label {
    REQUIRED(32), OPTIONAL(64), REPEATED(128), PACKED(256), ONE_OF(512);

    private final int value;

    Label(int value) {
      this.value = value;
    }

    public int value() {
      return value;
    }

    boolean isRepeated() {
      return this == REPEATED || this == PACKED;
    }

    boolean isPacked() {
      return this == PACKED;
    }

    boolean isOneOf() {
      return this == ONE_OF;
    }
  }

  /** Set to null until a field is added. */
  private transient UnknownFieldMap unknownFields;

  /** If not {@code -1} then the serialized size of this message. */
  transient int cachedSerializedSize = -1;

  /** If non-zero, the hash code of this message. Accessed by generated code. */
  protected transient int hashCode = 0;

  protected Message() {
  }

  /**
   * Initializes any unknown field data to that stored in the given {@code Builder}.
   */
  protected void setBuilder(Builder builder) {
    if (builder.unknownFieldMap != null) {
      unknownFields = new UnknownFieldMap(builder.unknownFieldMap);
    }
  }

  // Increase visibility for testing
  protected Collection<List<UnknownFieldMap.Value>> unknownFields() {
    return unknownFields == null ? Collections.<List<UnknownFieldMap.Value>>emptySet()
        : unknownFields.fieldMap.values();
  }

  /** Utility method to return a mutable copy of a given List. Used by generated code. */
  protected static <T> List<T> copyOf(List<T> list) {
    if (list == null) {
      throw new NullPointerException("list == null");
    }
    if (list == Collections.emptyList()) {
      return list;
    }
    return new ArrayList<T>(list);
  }

  /** Utility method to return an immutable copy of a given List. Used by generated code. */
  protected static <T> List<T> immutableCopyOf(List<T> list) {
    if (list == null) {
      throw new NullPointerException("list == null");
    }
    if (list == Collections.emptyList()) {
      return list;
    } else if (list instanceof MessageAdapter.ImmutableList) {
      return list;
    }
    return Collections.unmodifiableList(new ArrayList<T>(list));
  }

  /**
   * Returns the enumerated value tagged with the given integer value for the
   * given enum class. If no enum value in the given class is initialized
   * with the given integer tag value, an exception will be thrown.
   *
   * @param <E> the enum class type
   */
  public static <E extends Enum & ProtoEnum> E enumFromInt(Class<E> enumClass, int value) {
    EnumAdapter<E> adapter = WIRE.enumAdapter(enumClass);
    return adapter.fromInt(value);
  }

  void writeUnknownFieldMap(WireOutput output) throws IOException {
    if (unknownFields != null) {
      unknownFields.write(output);
    }
  }

  int getUnknownFieldsSerializedSize() {
    return unknownFields == null ? 0 : unknownFields.getSerializedSize();
  }

  protected static boolean equals(Object a, Object b) {
    return a == b || (a != null && a.equals(b));
  }

  @SuppressWarnings("unchecked")
  @Override public String toString() {
    return WIRE.messageAdapter((Class<Message>) getClass()).toString(this);
  }

  private Object writeReplace() throws ObjectStreamException {
    return new MessageSerializedForm(this, getClass());
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
      try {
        ensureUnknownFieldMap().addVarint(tag, value);
      } catch (IOException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }

    /**
     * Adds a {@code fixed32} value to the unknown field set with the given tag number.
     */
    public void addFixed32(int tag, int value) {
      try {
        ensureUnknownFieldMap().addFixed32(tag, value);
      } catch (IOException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }

    /**
     * Adds a {@code fixed64} value to the unknown field set with the given tag number.
     */
    public void addFixed64(int tag, long value) {
      try {
        ensureUnknownFieldMap().addFixed64(tag, value);
      } catch (IOException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }

    /**
     * Adds a length delimited value to the unknown field set with the given tag number.
     */
    public void addLengthDelimited(int tag, ByteString value) {
      try {
        ensureUnknownFieldMap().addLengthDelimited(tag, value);
      } catch (IOException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }

    UnknownFieldMap ensureUnknownFieldMap() {
      if (unknownFieldMap == null) {
        unknownFieldMap = new UnknownFieldMap();
      }
      return unknownFieldMap;
    }

    /**
     * Create an exception for missing required fields.
     *
     * @param args Alternating field value and field name pairs.
     */
    protected IllegalStateException missingRequiredFields(Object... args) {
      StringBuilder sb = new StringBuilder();
      String plural = "";
      for (int i = 0, size = args.length; i < size; i += 2) {
        if (args[i] == null) {
          if (sb.length() > 0) {
            plural = "s"; // Found more than one missing field
          }
          sb.append("\n  ");
          sb.append(args[i + 1]);
        }
      }
      throw new IllegalStateException("Required field" + plural + " not set:" + sb);
    }

    /**
     * If {@code list} is null it will be replaced with {@link Collections#emptyList()}.
     * Otherwise look for null items and throw {@link NullPointerException} if one is found.
     */
    protected static <T> List<T> canonicalizeList(List<T> list) {
      if (list == null) {
        throw new NullPointerException("list == null");
      }
      for (int i = 0, size = list.size(); i < size; i++) {
        T element = list.get(i);
        if (element == null) {
          throw new NullPointerException("Element at index " + i + " is null");
        }
      }
      return list;
    }

    /**
     * Returns an immutable {@link com.squareup.wire.Message} based on the fields that have been set
     * in this builder.
     */
    public abstract T build();
  }
}
