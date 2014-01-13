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

import java.io.IOException;

/**
 * Encodes and writes protocol message fields.
 */
final class WireOutput {

  public static final int FIXED_32_SIZE = 4;
  public static final int FIXED_64_SIZE = 8;
  public static final int TAG_TYPE_BITS = 3;
  public static final int TAG_TYPE_MASK = (1 << TAG_TYPE_BITS) - 1;
  public static final int WIRETYPE_VARINT           = 0;
  public static final int WIRETYPE_FIXED64          = 1;
  public static final int WIRETYPE_LENGTH_DELIMITED = 2;
  public static final int WIRETYPE_START_GROUP      = 3;
  public static final int WIRETYPE_END_GROUP        = 4;
  public static final int WIRETYPE_FIXED32          = 5;

  private final byte[] buffer;
  private final int limit;
  private int position;

  private WireOutput(byte[] buffer, int offset, int length) {
    this.buffer = buffer;
    position = offset;
    limit = offset + length;
  }

  /**
   * Create a new {@code CodedOutputStream} that writes directly to the given
   * byte array.  If more bytes are written than fit in the array, an
   * {@link IOException} will be thrown.  Writing directly to a flat
   * array is faster than writing to an {@code OutputStream}.
   */
  public static WireOutput newInstance(byte[] flatArray) {
    return newInstance(flatArray, 0, flatArray.length);
  }

  /**
   * Create a new {@code CodedOutputStream} that writes directly to the given
   * byte array slice.  If more bytes are written than fit in the slice, an
   * {@link IOException} will be thrown.  Writing directly to a flat
   * array is faster than writing to an {@code OutputStream}.
   */
  public static WireOutput newInstance(byte[] flatArray, int offset, int length) {
    return new WireOutput(flatArray, offset, length);
  }

  /** Makes a tag value given a field number and wire type. */
  public static int makeTag(int fieldNumber, int wireType) {
    return (fieldNumber << TAG_TYPE_BITS) | wireType;
  }

  /** Compute the number of bytes that would be needed to encode a tag. */
  public static int tagSize(int tag) {
    return varint32Size(makeTag(tag, 0));
  }

  /**
   * Compute the number of bytes that would be needed to encode an
   * {@code int32} field without a tag.
   */
  public static int int32Size(int value) {
    if (value >= 0) {
      return varint32Size(value);
    } else {
      // Must sign-extend.
      return 10;
    }
  }

  /**
   * Compute the number of bytes that would be needed to encode a varint.
   * {@code value} is treated as unsigned, so it won't be sign-extended if
   * negative.
   */
  public static int varint32Size(int value) {
    if ((value & (0xffffffff <<  7)) == 0) return 1;
    if ((value & (0xffffffff << 14)) == 0) return 2;
    if ((value & (0xffffffff << 21)) == 0) return 3;
    if ((value & (0xffffffff << 28)) == 0) return 4;
    return 5;
  }

  /** Compute the number of bytes that would be needed to encode a varint. */
  public static int varint64Size(long value) {
    if ((value & (0xffffffffffffffffL <<  7)) == 0) return 1;
    if ((value & (0xffffffffffffffffL << 14)) == 0) return 2;
    if ((value & (0xffffffffffffffffL << 21)) == 0) return 3;
    if ((value & (0xffffffffffffffffL << 28)) == 0) return 4;
    if ((value & (0xffffffffffffffffL << 35)) == 0) return 5;
    if ((value & (0xffffffffffffffffL << 42)) == 0) return 6;
    if ((value & (0xffffffffffffffffL << 49)) == 0) return 7;
    if ((value & (0xffffffffffffffffL << 56)) == 0) return 8;
    if ((value & (0xffffffffffffffffL << 63)) == 0) return 9;
    return 10;
  }

  /** Write a single byte. */
  public void writeRawByte(byte value) throws IOException {
    if (position == limit) {
      // We're writing to a single buffer.
      throw new IOException("Out of space: position=" + position + ", limit=" + limit);
    }
    buffer[position++] = value;
  }

  /** Write a single byte, represented by an integer value. */
  public void writeRawByte(int value) throws IOException {
    writeRawByte((byte) value);
  }

  /** Write an array of bytes. */
  public void writeRawBytes(byte[] value) throws IOException {
    writeRawBytes(value, 0, value.length);
  }

  /** Write part of an array of bytes. */
  public void writeRawBytes(byte[] value, int offset, int length) throws IOException {
    if (limit - position >= length) {
      // We have room in the current buffer.
      System.arraycopy(value, offset, buffer, position, length);
      position += length;
    } else {
      // We're writing to a single buffer.
      throw new IOException("Out of space: position=" + position + ", limit=" + limit);
    }
  }

  /** Encode and write a tag. */
  public void writeTag(int fieldNumber, int wireType) throws IOException {
    writeVarint32(makeTag(fieldNumber, wireType));
  }

  /** Write an {@code int32} field to the stream. */
  public void writeSignedVarint32(int value) throws IOException {
    if (value >= 0) {
      writeVarint32(value);
    } else {
      // Must sign-extend.
      writeVarint64(value);
    }
  }

  /**
   * Encode and write a varint.  {@code value} is treated as
   * unsigned, so it won't be sign-extended if negative.
   */
  public void writeVarint32(int value) throws IOException {
    while (true) {
      if ((value & ~0x7F) == 0) {
        writeRawByte(value);
        return;
      } else {
        writeRawByte((value & 0x7F) | 0x80);
        value >>>= 7;
      }
    }
  }

  /** Encode and write a varint. */
  public void writeVarint64(long value) throws IOException {
    while (true) {
      if ((value & ~0x7FL) == 0) {
        writeRawByte((int) value);
        return;
      } else {
        writeRawByte(((int) value & 0x7F) | 0x80);
        value >>>= 7;
      }
    }
  }

  /** Write a little-endian 32-bit integer. */
  public void writeFixed32(int value) throws IOException {
    // CHECKSTYLE.OFF: ParenPad
    writeRawByte((value      ) & 0xFF);
    writeRawByte((value >>  8) & 0xFF);
    writeRawByte((value >> 16) & 0xFF);
    writeRawByte((value >> 24) & 0xFF);
    // CHECKSTYLE.ON: ParenPad
  }

  /** Write a little-endian 64-bit integer. */
  public void writeFixed64(long value) throws IOException {
    // CHECKSTYLE.OFF: ParenPad
    writeRawByte((int) (value      ) & 0xFF);
    writeRawByte((int) (value >>  8) & 0xFF);
    writeRawByte((int) (value >> 16) & 0xFF);
    writeRawByte((int) (value >> 24) & 0xFF);
    writeRawByte((int) (value >> 32) & 0xFF);
    writeRawByte((int) (value >> 40) & 0xFF);
    writeRawByte((int) (value >> 48) & 0xFF);
    writeRawByte((int) (value >> 56) & 0xFF);
    // CHECKSTYLE.ON: ParenPad
  }

  /**
   * Encode a ZigZag-encoded 32-bit value.  ZigZag encodes signed integers
   * into values that can be efficiently encoded with varint.  (Otherwise,
   * negative values must be sign-extended to 64 bits to be varint encoded,
   * thus always taking 10 bytes on the wire.)
   *
   * @param n A signed 32-bit integer.
   * @return An unsigned 32-bit integer, stored in a signed int because
   *         Java has no explicit unsigned support.
   */
  public static int zigZag32(int n) {
    // Note:  the right-shift must be arithmetic
    return (n << 1) ^ (n >> 31);
  }

  /**
   * Encode a ZigZag-encoded 64-bit value.  ZigZag encodes signed integers
   * into values that can be efficiently encoded with varint.  (Otherwise,
   * negative values must be sign-extended to 64 bits to be varint encoded,
   * thus always taking 10 bytes on the wire.)
   *
   * @param n A signed 64-bit integer.
   * @return An unsigned 64-bit integer, stored in a signed int because
   *         Java has no explicit unsigned support.
   */
  public static long zigZag64(long n) {
    // Note:  the right-shift must be arithmetic
    return (n << 1) ^ (n >> 63);
  }
}
