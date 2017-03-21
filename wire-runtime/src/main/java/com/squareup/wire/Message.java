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
import java.io.OutputStream;
import java.io.Serializable;
import okio.Buffer;
import okio.BufferedSink;
import okio.ByteString;

/** A protocol buffer message. */
public abstract class Message<M extends Message<M, B>, B extends Message.Builder<M, B>>
    implements Serializable {
  private static final long serialVersionUID = 0L;

  private final transient ProtoAdapter<M> adapter;

  /** Unknown fields, proto-encoded. We permit null to support magic deserialization. */
  private final transient ByteString unknownFields;

  /** If not {@code 0} then the serialized size of this message. */
  transient int cachedSerializedSize = 0;

  /** If non-zero, the hash code of this message. Accessed by generated code. */
  protected transient int hashCode = 0;

  protected Message(ProtoAdapter<M> adapter, ByteString unknownFields) {
    if (adapter == null) throw new NullPointerException("adapter == null");
    if (unknownFields == null) throw new NullPointerException("unknownFields == null");
    this.adapter = adapter;
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

  @Override public String toString() {
    //noinspection unchecked
    return adapter.toString((M) this);
  }

  protected final Object writeReplace() throws ObjectStreamException {
    //noinspection unchecked
    return new MessageSerializedForm(encode(), getClass());
  }

  /** The {@link ProtoAdapter} for encoding and decoding messages of this type. */
  public final ProtoAdapter<M> adapter() {
    return adapter;
  }

  /** Encode this message and write it to {@code stream}. */
  public final void encode(BufferedSink sink) throws IOException {
    //noinspection unchecked
    adapter.encode(sink, (M) this);
  }

  /** Encode this message as a {@code byte[]}. */
  public final byte[] encode() {
    //noinspection unchecked
    return adapter.encode((M) this);
  }

  /** Encode this message and write it to {@code stream}. */
  public final void encode(OutputStream stream) throws IOException {
    //noinspection unchecked
    adapter.encode(stream, (M) this);
  }

  /**
   * Superclass for protocol buffer message builders.
   */
  public abstract static class Builder<M extends Message<M, B>, B extends Builder<M, B>> {
    /**
     * Caches unknown fields as a {@link ByteString} when {@link #buildUnknownFields()} is called.
     * When the caller adds an additional unknown field after that, it will be written to the new
     * {@link #unknownFieldsBuffer} to ensure that all unknown fields are retained between calls to
     * {@link #buildUnknownFields()}.
     */
    transient ByteString unknownFieldsByteString = ByteString.EMPTY;
    /**
     * {@link Buffer} of the message's unknown fields that is lazily instantiated between calls to
     * {@link #buildUnknownFields()}. It's automatically cleared in {@link #buildUnknownFields()},
     * and can also be manually cleared by calling {@link #clearUnknownFields()}.
     */
    transient Buffer unknownFieldsBuffer;
    transient ProtoWriter unknownFieldsWriter;

    protected Builder() {
    }

    public final Builder<M, B> addUnknownFields(ByteString unknownFields) {
      if (unknownFields.size() > 0) {
        prepareForNewUnknownFields();
        try {
          unknownFieldsWriter.writeBytes(unknownFields);
        } catch (IOException e) {
          throw new AssertionError();
        }
      }
      return this;
    }

    public final Builder<M, B> addUnknownField(int tag, FieldEncoding fieldEncoding, Object value) {
      prepareForNewUnknownFields();
      try {
        ProtoAdapter<Object> protoAdapter = (ProtoAdapter<Object>) fieldEncoding.rawProtoAdapter();
        protoAdapter.encodeWithTag(unknownFieldsWriter, tag, value);
      } catch (IOException e) {
        throw new AssertionError();
      }
      return this;
    }

    public final Builder<M, B> clearUnknownFields() {
      unknownFieldsByteString = ByteString.EMPTY;
      if (unknownFieldsBuffer != null) {
        unknownFieldsBuffer.clear();
        unknownFieldsBuffer = null;
      }
      unknownFieldsWriter = null;
      return this;
    }

    /**
     * Returns a byte string with this message's unknown fields. Returns an empty byte string if
     * this message has no unknown fields.
     */
    public final ByteString buildUnknownFields() {
      if (unknownFieldsBuffer != null) {
        // Reads and caches the unknown fields from the buffer.
        unknownFieldsByteString = unknownFieldsBuffer.readByteString();
        unknownFieldsBuffer = null;
        unknownFieldsWriter = null;
      }
      return unknownFieldsByteString;
    }

    /** Returns an immutable {@link Message} based on the fields that set in this builder. */
    public abstract M build();

    private void prepareForNewUnknownFields() {
      if (unknownFieldsBuffer == null) {
        unknownFieldsBuffer = new Buffer();
        unknownFieldsWriter = new ProtoWriter(unknownFieldsBuffer);
        try {
          // Writes the cached unknown fields to the buffer.
          unknownFieldsWriter.writeBytes(unknownFieldsByteString);
        } catch (IOException e) {
          throw new AssertionError();
        }
        unknownFieldsByteString = ByteString.EMPTY;
      }
    }
  }
}
