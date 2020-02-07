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
package com.squareup.wire

import okio.BufferedSink
import okio.ByteString

/** A protocol buffer message. */
actual abstract class Message<M : Message<M, B>, B : Message.Builder<M, B>>
protected actual constructor(
  /** The [ProtoAdapter] for encoding and decoding messages of this type. */
  actual val adapter: ProtoAdapter<M>,
  /**
   * Returns a byte string containing the proto encoding of this message's unknown fields. Returns
   * an empty byte string if this message has no unknown fields.
   */
  actual val unknownFields: ByteString
) {
  /** If non-zero, the hash code of this message. Accessed by generated code. */
  @JsName("cachedHashCode") protected actual var hashCode = 0

  /**
   * Returns a new builder initialized with the data in this message.
   */
  actual abstract fun newBuilder(): B

  /** Encode this message and write it to `stream`. */
  actual fun encode(sink: BufferedSink) {
    adapter.encode(sink, this as M)
  }

  /** Encode this message as a `byte[]`. */
  actual fun encode(): ByteArray = adapter.encode(this as M)

  /** Encode this message as a `ByteString`. */
  actual fun encodeByteString(): ByteString {
    return adapter.encodeByteString(this as M)
  }

  /**
   * Superclass for protocol buffer message builders.
   */
  actual abstract class Builder<M : Message<M, B>, B : Builder<M, B>>
}
