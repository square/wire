// This class is derived from the CodedInputByteBuffer class in Google's "Nano" Protocol Buffer
// implementation. The original copyright notice, list of conditions, and disclaimer for those
// classes is as follows:

// Protocol Buffers - Google's data interchange format
// Copyright 2013 Google Inc.  All rights reserved.
// http://code.google.com/p/protobuf/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
// * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
package com.squareup.wire

import kotlin.Throws
import kotlin.jvm.JvmName
import okio.ByteString
import okio.IOException

/**
 * Reads and decodes protocol message fields using an `Int` as a cursor.
 *
 * This is an alternative to [ProtoReader], which uses `Long` as a cursor. It originates as an
 * optimization for Kotlin/JS, where `Long` cursors are prohibitively expensive. It doesn't subclass
 * [ProtoReader] because [nextTag] and [forEachTag] must each return the appropriate cursor type.
 */
interface ProtoReader32 {
  /** Returns a [ProtoReader] that reads the same data as this using a different type. */
  fun asProtoReader(): ProtoReader

  /**
   * Begin a nested message. A call to this method will restrict the reader so that [nextTag]
   * returns -1 when the message is complete. An accompanying call to [endMessage] must then occur
   * with the opaque token returned from this method.
   */
  @Throws(IOException::class)
  fun beginMessage(): Int

  /**
   * End a length-delimited nested message. Calls to this method must be symmetric with calls to
   * [beginMessage].
   *
   * @param token value returned from the corresponding call to [beginMessage].
   */
  @Throws(IOException::class)
  fun endMessageAndGetUnknownFields(token: Int): ByteString

  /** Reads and returns the length of the next message in a length-delimited stream. */
  @Throws(IOException::class)
  fun nextLengthDelimited(): Int

  /**
   * Reads and returns the next tag of the message, or -1 if there are no further tags. Use
   * [peekFieldEncoding] after calling this method to query its encoding. This silently skips
   * groups.
   */
  @Throws(IOException::class)
  fun nextTag(): Int

  /**
   * Returns the encoding of the next field value. [nextTag] must be called before this method.
   */
  fun peekFieldEncoding(): FieldEncoding?

  /**
   * Skips the current field's value. This is only safe to call immediately following a call to
   * [nextTag].
   */
  @Throws(IOException::class)
  fun skip()

  /**
   * Reads a `bytes` field value from the stream. The length is read from the stream prior to the
   * actual data.
   */
  @Throws(IOException::class)
  fun readBytes(): ByteString

  /**
   * Prepares to read a value and returns true if the read should proceed. If there's nothing to
   * read (because a packed value has length 0), this will clear the reader state.
   */
  @Throws(IOException::class)
  fun beforePossiblyPackedScalar(): Boolean

  /** Reads a `string` field value from the stream. */
  @Throws(IOException::class)
  fun readString(): String

  /**
   * Reads a raw varint from the stream. If larger than 32 bits, discard the upper bits.
   */
  @Throws(IOException::class)
  fun readVarint32(): Int

  /** Reads a raw varint up to 64 bits in length from the stream.  */
  @Throws(IOException::class)
  fun readVarint64(): Long

  /** Reads a 32-bit little-endian integer from the stream.  */
  @Throws(IOException::class)
  fun readFixed32(): Int

  /** Reads a 64-bit little-endian integer from the stream.  */
  @Throws(IOException::class)
  fun readFixed64(): Long

  /**
   * Read an unknown field and store temporarily. Once the entire message is read, call
   * [endMessageAndGetUnknownFields] to retrieve unknown fields.
   */
  fun readUnknownField(tag: Int)

  /**
   * Store an already read field temporarily. Once the entire message is read, call
   * [endMessageAndGetUnknownFields] to retrieve unknown fields.
   */
  fun addUnknownField(
    tag: Int,
    fieldEncoding: FieldEncoding,
    value: Any?,
  )

  /**
   * Returns the min length of the next field in bytes. Some encodings have a fixed length, while
   * others have a variable length. LENGTH_DELIMITED fields have a known variable length, while
   * VARINT fields could be as small as a single byte.
   */
  fun nextFieldMinLengthInBytes(): Int
}

/** Reads each tag, handles it, and returns a byte string with the unknown fields. */
@JvmName("-forEachTag") // hide from Java
inline fun ProtoReader32.forEachTag(tagHandler: (Int) -> Any): ByteString {
  val token = beginMessage()
  while (true) {
    val tag = nextTag()
    if (tag == -1) break
    tagHandler(tag)
  }
  return endMessageAndGetUnknownFields(token)
}

fun ProtoReader32(
  source: ByteString,
  pos: Int = 0,
  limit: Int = source.size,
): ProtoReader32 = ByteArrayProtoReader32(
  source = source.toByteArray(),
  pos = pos,
  limit = limit,
)

fun ProtoReader32(
  source: ByteArray,
  pos: Int = 0,
  limit: Int = source.size,
): ProtoReader32 = ByteArrayProtoReader32(
  source = source,
  pos = pos,
  limit = limit,
)
