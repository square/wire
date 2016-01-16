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

import com.squareup.wire.internal.Internal;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import okio.Buffer;
import okio.BufferedSink;
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
    return ProtoAdapter.get(this).toString(this);
  }

  protected final Object writeReplace() throws ObjectStreamException {
    return new MessageSerializedForm(this, getClass());
  }

  /** Encode this message and write it to {@code stream}. */
  public final void encode(BufferedSink sink) throws IOException {
    ProtoAdapter.get(this).encode(sink, this);
  }

  /** Encode this message as a {@code byte[]}. */
  public final byte[] encode() {
    return ProtoAdapter.get(this).encode(this);
  }

  /** Encode this message and write it to {@code stream}. */
  public final void encode(OutputStream stream) throws IOException {
    ProtoAdapter.get(this).encode(stream, this);
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
        protoAdapter.encodeWithTag(unknownFieldsWriter, tag, value);
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

  /** For generated code only. */
  @Deprecated // TODO remove for 2.1.
  public static <T> List<T> newMutableList() {
    return Internal.newMutableList();
  }
  /** For generated code only. */
  @Deprecated // TODO remove for 2.1.
  public static <T> List<T> copyOf(String name, List<T> list) {
    return Internal.copyOf(name, list);
  }
  /** For generated code only. */
  @Deprecated // TODO remove for 2.1.
  public static <T> List<T> immutableCopyOf(String name, List<T> list) {
    return Internal.immutableCopyOf(name, list);
  }
  /** For generated code only. */
  @Deprecated // TODO remove for 2.1.
  public static <T> void redactElements(List<T> list, ProtoAdapter<T> adapter) {
    Internal.redactElements(list, adapter);
  }
  /** For generated code only. */
  @Deprecated // TODO remove for 2.1.
  public static boolean equals(Object a, Object b) {
    return Internal.equals(a, b);
  }
  /** For generated code only. */
  @Deprecated // TODO remove for 2.1.
  public static IllegalStateException missingRequiredFields(Object... args) {
    return Internal.missingRequiredFields(args);
  }
  /** For generated code only. */
  @Deprecated // TODO remove for 2.1.
  public static void checkElementsNotNull(List<?> list) {
    Internal.checkElementsNotNull(list);
  }
  /** For generated code only. */
  @Deprecated // TODO remove for 2.1.
  public static int countNotNull(Object a, Object b) {
    return Internal.countNonNull(a, b);
  }
  /** For generated code only. */
  @Deprecated // TODO remove for 2.1.
  public static int countNotNull(Object a, Object b, Object c) {
    return Internal.countNonNull(a, b, c);
  }
  /** For generated code only. */
  @Deprecated // TODO remove for 2.1.
  public static int countNotNull(Object a, Object b, Object c, Object d, Object... rest) {
    return Internal.countNonNull(a, b, c, d, rest);
  }
}
