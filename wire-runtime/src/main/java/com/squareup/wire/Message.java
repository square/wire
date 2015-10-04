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
import java.util.Collections;
import java.util.List;
import okio.Buffer;
import okio.ByteString;

/** A protocol buffer message. */
public abstract class Message<M extends Message<M, B>, B extends Message.Builder<M, B>>
    implements Serializable {
  private static final long serialVersionUID = 0L;

  /** Unknown fields, proto-encoded. We permit null to support magic deserialization. */
  private final transient ByteString unknownFields;

  /** If not {@code 0} then the serialized size of this message. */
  transient int cachedSerializedSize = 0;

  /** If non-zero, the hash code of this message. Accessed by generated code. */
  protected transient int hashCode = 0;

  protected Message(ByteString unknownFields) {
    if (unknownFields == null) {
      throw new NullPointerException("unknownFields == null");
    }
    this.unknownFields = unknownFields;
  }

  /**
   * Returns a byte string containing the proto encoding of this message's unknown fields. Returns
   * an empty byte string if this message has no unknown fields.
   */
  public final ByteString unknownFields() {
    ByteString result = this.unknownFields;
    return result != null ? result : ByteString.EMPTY;
  }

  /**
   * Returns a new builder initialized with the data in this message.
   */
  public abstract Builder<M, B> newBuilder();

  /** Returns this message with any unknown fields removed. */
  public final M withoutUnknownFields() {
    return newBuilder().clearUnknownFields().build();
  }

  @SuppressWarnings("unchecked")
  @Override public String toString() {
    return ProtoAdapter.get((Class<Message>) getClass()).toString(this);
  }

  protected final Object writeReplace() throws ObjectStreamException {
    return new MessageSerializedForm(this, getClass());
  }

  /**
   * Superclass for protocol buffer message builders.
   */
  public abstract static class Builder<T extends Message<T, B>, B extends Builder<T, B>> {
    // Lazily-instantiated buffer and writer of this message's unknown fields.
    Buffer unknownFieldsBuffer;
    ProtoWriter unknownFieldsWriter;

    /**
     * Constructs a Builder with no unknown field data.
     */
    public Builder() {
    }

    public Builder<T, B> addUnknownFields(ByteString unknownFields) {
      if (unknownFields.size() > 0) {
        if (unknownFieldsWriter == null) {
          unknownFieldsBuffer = new Buffer();
          unknownFieldsWriter = new ProtoWriter(unknownFieldsBuffer);
        }
        try {
          unknownFieldsWriter.writeBytes(unknownFields);
        } catch (IOException e) {
          throw new AssertionError();
        }
      }
      return this;
    }

    public Builder<T, B> addUnknownField(int tag, FieldEncoding fieldEncoding, Object value) {
      if (unknownFieldsWriter == null) {
        unknownFieldsBuffer = new Buffer();
        unknownFieldsWriter = new ProtoWriter(unknownFieldsBuffer);
      }
      try {
        ProtoAdapter<Object> protoAdapter = (ProtoAdapter<Object>) fieldEncoding.rawProtoAdapter();
        protoAdapter.encodeTagged(unknownFieldsWriter, tag, value);
      } catch (IOException e) {
        throw new AssertionError();
      }
      return this;
    }

    public Builder<T, B> clearUnknownFields() {
      unknownFieldsWriter = null;
      unknownFieldsBuffer = null;
      return this;
    }

    /**
     * Returns a byte string with this message's unknown fields. Returns an empty byte string if
     * this message has no unknown fields.
     */
    public ByteString buildUnknownFields() {
      return unknownFieldsBuffer != null
          ? unknownFieldsBuffer.clone().readByteString()
          : ByteString.EMPTY;
    }

    /** Returns an immutable {@link Message} based on the fields that set in this builder. */
    public abstract T build();
  }

  /** <b>For generated code only.</b> */
  protected static <T> List<T> newMutableList() {
    return new MutableOnWriteList<>(Collections.<T>emptyList());
  }

  /** <b>For generated code only.</b> Utility method to return a mutable copy of {@code list}. */
  protected static <T> List<T> copyOf(List<T> list) {
    if (list == null) throw new NullPointerException("list == null");
    if (list == Collections.emptyList() || list instanceof ImmutableList) {
      return new MutableOnWriteList<>(list);
    }
    return new ArrayList<>(list);
  }

  /** <b>For generated code only.</b> Utility method to return an immutable copy of {@code list}. */
  protected static <T> List<T> immutableCopyOf(List<T> list) {
    if (list == null) throw new NullPointerException("list == null");
    if (list instanceof MutableOnWriteList) {
      list = ((MutableOnWriteList<T>) list).mutableList;
    }
    if (list == Collections.emptyList() || list instanceof ImmutableList) {
      return list;
    }
    return new ImmutableList<>(list);
  }

  /** <b>For generated code only.</b> */
  protected static <T> void redactElements(List<T> list, ProtoAdapter<T> adapter) {
    for (int i = 0, count = list.size(); i < count; i++) {
      list.set(i, adapter.redact(list.get(i)));
    }
  }

  /** <b>For generated code only.</b> */
  protected static boolean equals(Object a, Object b) {
    return a == b || (a != null && a.equals(b));
  }

  /**
   * <b>For generated code only.</b> Create an exception for missing required fields.
   *
   * @param args Alternating field value and field name pairs.
   */
  protected static IllegalStateException missingRequiredFields(Object... args) {
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
   * <b>For generated code only.</b> Throw {@link NullPointerException} if {@code list} or one of
   * its items are null.
   */
  protected static void checkElementsNotNull(List<?> list) {
    if (list == null) throw new NullPointerException("list == null");
    for (int i = 0, size = list.size(); i < size; i++) {
      Object element = list.get(i);
      if (element == null) {
        throw new NullPointerException("Element at index " + i + " is null");
      }
    }
  }
}
