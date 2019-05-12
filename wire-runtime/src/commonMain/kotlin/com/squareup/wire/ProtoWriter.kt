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

// This class is derived from the CodedOutputByteBuffer, and WireFormatNano classes in Google's
// "Nano" Protocol Buffer implementation. The original copyright notice, list of conditions, and
// disclaimer for those classes is as follows:

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

import com.squareup.wire.internal.Throws
import okio.BufferedSink
import okio.ByteString
import okio.IOException

/**
 * Utilities for encoding and writing protocol message fields.
 */
class ProtoWriter(private val sink: BufferedSink) {

  @Throws(IOException::class)
  fun writeBytes(value: ByteString) {
    sink.write(value)
  }

  @Throws(IOException::class)
  fun writeString(value: String) {
    sink.writeUtf8(value)
  }

  /** Encode and write a tag.  */
  @Throws(IOException::class)
  fun writeTag(fieldNumber: Int, fieldEncoding: FieldEncoding) {
    writeVarint32(makeTag(fieldNumber, fieldEncoding))
  }

  /** Write an `int32` field to the stream.  */
  @Throws(IOException::class)
  internal fun writeSignedVarint32(value: Int) {
    if (value >= 0) {
      writeVarint32(value)
    } else {
      // Must sign-extend.
      writeVarint64(value.toLong())
    }
  }

  /**
   * Encode and write a varint. `value` is treated as unsigned, so it won't be sign-extended
   * if negative.
   */
  @Throws(IOException::class)
  fun writeVarint32(value: Int) {
    var value = value
    while (value and 0x7f.inv() != 0) {
      sink.writeByte((value and 0x7f) or 0x80)
      value = value ushr 7
    }
    sink.writeByte(value)
  }

  /** Encode and write a varint.  */
  @Throws(IOException::class)
  fun writeVarint64(value: Long) {
    var value = value
    while (value and 0x7fL.inv() != 0L) {
      sink.writeByte((value.toInt() and 0x7f) or 0x80)
      value = value ushr 7
    }
    sink.writeByte(value.toInt())
  }

  /** Write a little-endian 32-bit integer.  */
  @Throws(IOException::class)
  fun writeFixed32(value: Int) {
    sink.writeIntLe(value)
  }

  /** Write a little-endian 64-bit integer.  */
  @Throws(IOException::class)
  fun writeFixed64(value: Long) {
    sink.writeLongLe(value)
  }

  companion object {

    /** Makes a tag value given a field number and wire type. */
    private fun makeTag(fieldNumber: Int, fieldEncoding: FieldEncoding): Int {
      return (fieldNumber shl ProtoReader.TAG_FIELD_ENCODING_BITS) or fieldEncoding.value
    }

    /** Compute the number of bytes that would be needed to encode a tag. */
    internal fun tagSize(tag: Int): Int = varint32Size(makeTag(tag, FieldEncoding.VARINT))

    /**
     * Computes the number of bytes that would be needed to encode a signed variable-length integer
     * of up to 32 bits.
     */
    internal fun int32Size(value: Int): Int {
      return if (value >= 0) {
        varint32Size(value)
      } else {
        // Must sign-extend.
        10
      }
    }

    /**
     * Compute the number of bytes that would be needed to encode a varint. `value` is treated
     * as unsigned, so it won't be sign-extended if negative.
     */
    internal fun varint32Size(value: Int): Int {
      if (value and (-0x1 shl 7) == 0) return 1
      if (value and (-0x1 shl 14) == 0) return 2
      if (value and (-0x1 shl 21) == 0) return 3
      return if (value and (-0x1 shl 28) == 0) 4 else 5
    }

    /** Compute the number of bytes that would be needed to encode a varint. */
    internal fun varint64Size(value: Long): Int {
      if (value and (-0x1L shl 7) == 0L) return 1
      if (value and (-0x1L shl 14) == 0L) return 2
      if (value and (-0x1L shl 21) == 0L) return 3
      if (value and (-0x1L shl 28) == 0L) return 4
      if (value and (-0x1L shl 35) == 0L) return 5
      if (value and (-0x1L shl 42) == 0L) return 6
      if (value and (-0x1L shl 49) == 0L) return 7
      if (value and (-0x1L shl 56) == 0L) return 8
      return if (value and (-0x1L shl 63) == 0L) 9 else 10
    }

    /**
     * Encode a ZigZag-encoded 32-bit value. ZigZag encodes signed integers into values that can be
     * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
     * to be varint encoded, thus always taking 10 bytes on the wire.)
     *
     * @param n A signed 32-bit integer.
     * @return An unsigned 32-bit integer, stored in a signed int because Java has no explicit
     * unsigned support.
     */
    internal fun encodeZigZag32(n: Int): Int {
      // Note: the right-shift must be arithmetic
      return (n shl 1) xor (n shr 31)
    }

    /**
     * Decodes a ZigZag-encoded 32-bit value. ZigZag encodes signed integers into values that can be
     * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
     * to be varint encoded, thus always taking 10 bytes on the wire.)
     *
     * @param n An unsigned 32-bit integer, stored in a signed int because Java has no explicit
     * unsigned support.
     * @return A signed 32-bit integer.
     */
    internal fun decodeZigZag32(n: Int): Int = (n.ushr(1)) xor -(n and 1)

    /**
     * Encode a ZigZag-encoded 64-bit value. ZigZag encodes signed integers into values that can be
     * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
     * to be varint encoded, thus always taking 10 bytes on the wire.)
     *
     * @param n A signed 64-bit integer.
     * @return An unsigned 64-bit integer, stored in a signed int because Java has no explicit
     * unsigned support.
     */
    internal fun encodeZigZag64(n: Long): Long {
      // Note:  the right-shift must be arithmetic
      return (n shl 1) xor (n shr 63)
    }

    /**
     * Decodes a ZigZag-encoded 64-bit value. ZigZag encodes signed integers into values that can be
     * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
     * to be varint encoded, thus always taking 10 bytes on the wire.)
     *
     * @param n An unsigned 64-bit integer, stored in a signed int because Java has no explicit
     * unsigned support.
     * @return A signed 64-bit integer.
     */
    internal fun decodeZigZag64(n: Long): Long = (n.ushr(1)) xor -(n and 1)
  }
}
