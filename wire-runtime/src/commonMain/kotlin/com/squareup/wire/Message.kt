/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire

import okio.Buffer
import okio.BufferedSink
import okio.ByteString

/** A protocol buffer message. */
expect abstract class Message<M : Message<M, B>, B : Message.Builder<M, B>> protected constructor(
  adapter: ProtoAdapter<M>,
  unknownFields: ByteString,
) {
  /** If non-zero, the hash code of this message. Accessed by generated code. */
  protected var hashCode: Int

  /**
   * Returns a byte string containing the proto encoding of this message's unknown fields. Returns
   * an empty byte string if this message has no unknown fields.
   */
  open val unknownFields: ByteString

  /**
   * Returns a new builder initialized with the data in this message.
   */
  abstract fun newBuilder(): B

  /** The [ProtoAdapter] for encoding and decoding messages of this type. */
  val adapter: ProtoAdapter<M>

  /** Encode this message and write it to `stream`. */
  fun encode(sink: BufferedSink)

  /** Encode this message as a `byte[]`. */
  fun encode(): ByteArray

  /** Encode this message as a `ByteString`. */
  fun encodeByteString(): ByteString

  /**
   * Superclass for protocol buffer message builders.
   */
  abstract class Builder<M : Message<M, B>, B : Builder<M, B>> protected constructor() {
    /**
     * Caches unknown fields as a [ByteString] when [buildUnknownFields] is called.
     * When the caller adds an additional unknown field after that, it will be written to the new
     * [unknownFieldsBuffer] to ensure that all unknown fields are retained between calls to
     * [buildUnknownFields].
     */
    internal var unknownFieldsByteString: ByteString

    /**
     * [Buffer] of the message's unknown fields that is lazily instantiated between calls to
     * [buildUnknownFields]. It's automatically cleared in [buildUnknownFields], and can also be
     * manually cleared by calling [clearUnknownFields].
     */
    internal var unknownFieldsBuffer: Buffer?
    internal var unknownFieldsWriter: ProtoWriter?

    fun addUnknownFields(unknownFields: ByteString): Builder<M, B>

    fun addUnknownField(
      tag: Int,
      fieldEncoding: FieldEncoding,
      value: Any?,
    ): Builder<M, B>

    fun clearUnknownFields(): Builder<M, B>

    /**
     * Returns a byte string with this message's unknown fields. Returns an empty byte string if
     * this message has no unknown fields.
     */
    fun buildUnknownFields(): ByteString

    /** Returns an immutable [Message] based on the fields that set in this builder. */
    abstract fun build(): M
  }
}
