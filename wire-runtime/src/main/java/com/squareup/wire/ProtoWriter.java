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
import okio.BufferedSink;
import okio.ByteString;

/**
 * Utilities for encoding and writing protocol message fields.
 */
public final class ProtoWriter {

  /** Makes a tag value given a field number and wire type. */
  private static int makeTag(int fieldNumber, FieldEncoding fieldEncoding) {
    return (fieldNumber << ProtoReader.TAG_FIELD_ENCODING_BITS) | fieldEncoding.value;
  }

  /** Compute the number of bytes that would be needed to encode a tag. */
  static int tagSize(int tag) {
    return varint32Size(makeTag(tag, FieldEncoding.VARINT));
  }

  static int utf8Length(String s) {
    int result = 0;
    for (int i = 0, length = s.length(); i < length; i++) {
      char c = s.charAt(i);
      if (c < 0x80) {
        result++;
      } else if (c < 0x800) {
        result += 2;
      } else if (c < 0xd800 || c > 0xdfff) {
        result += 3;
      } else if (c <= 0xdbff && i + 1 < length
          && s.charAt(i + 1) >= 0xdc00 && s.charAt(i + 1) <= 0xdfff) {
        // A UTF-16 high surrogate followed by UTF-16 low surrogate yields 4 UTF-8 bytes.
        result += 4;
        i++;
      } else {
        // An unexpected surrogate yields a '?' character.
        result++;
      }
    }
    return result;
  }

  /**
   * Computes the number of bytes that would be needed to encode a signed variable-length integer
   * of up to 32 bits.
   */
  static int int32Size(int value) {
    if (value >= 0) {
      return varint32Size(value);
    } else {
      // Must sign-extend.
      return 10;
    }
  }

  /**
   * Compute the number of bytes that would be needed to encode a varint. {@code value} is treated
   * as unsigned, so it won't be sign-extended if negative.
   */
  static int varint32Size(int value) {
    if ((value & (0xffffffff <<  7)) == 0) return 1;
    if ((value & (0xffffffff << 14)) == 0) return 2;
    if ((value & (0xffffffff << 21)) == 0) return 3;
    if ((value & (0xffffffff << 28)) == 0) return 4;
    return 5;
  }

  /** Compute the number of bytes that would be needed to encode a varint. */
  static int varint64Size(long value) {
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

  /**
   * Encode a ZigZag-encoded 32-bit value. ZigZag encodes signed integers into values that can be
   * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
   * to be varint encoded, thus always taking 10 bytes on the wire.)
   *
   * @param n A signed 32-bit integer.
   * @return An unsigned 32-bit integer, stored in a signed int because Java has no explicit
   * unsigned support.
   */
  static int encodeZigZag32(int n) {
    // Note:  the right-shift must be arithmetic
    return (n << 1) ^ (n >> 31);
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
  static int decodeZigZag32(int n) {
    return (n >>> 1) ^ -(n & 1);
  }

  /**
   * Encode a ZigZag-encoded 64-bit value. ZigZag encodes signed integers into values that can be
   * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
   * to be varint encoded, thus always taking 10 bytes on the wire.)
   *
   * @param n A signed 64-bit integer.
   * @return An unsigned 64-bit integer, stored in a signed int because Java has no explicit
   * unsigned support.
   */
  static long encodeZigZag64(long n) {
    // Note:  the right-shift must be arithmetic
    return (n << 1) ^ (n >> 63);
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
  static long decodeZigZag64(long n) {
    return (n >>> 1) ^ -(n & 1);
  }

  private final BufferedSink sink;

  public ProtoWriter(BufferedSink sink) {
    this.sink = sink;
  }

  public void writeBytes(ByteString value) throws IOException {
    sink.write(value);
  }

  public void writeString(String value) throws IOException {
    sink.writeUtf8(value);
  }

  /** Encode and write a tag. */
  public void writeTag(int fieldNumber, FieldEncoding fieldEncoding) throws IOException {
    writeVarint32(makeTag(fieldNumber, fieldEncoding));
  }

  /** Write an {@code int32} field to the stream. */
  void writeSignedVarint32(int value) throws IOException {
    if (value >= 0) {
      writeVarint32(value);
    } else {
      // Must sign-extend.
      writeVarint64(value);
    }
  }

  /**
   * Encode and write a varint. {@code value} is treated as unsigned, so it won't be sign-extended
   * if negative.
   */
  public void writeVarint32(int value) throws IOException {
    while ((value & ~0x7f) != 0) {
      sink.writeByte((value & 0x7f) | 0x80);
      value >>>= 7;
    }
    sink.writeByte(value);
  }

  /** Encode and write a varint. */
  public void writeVarint64(long value) throws IOException {
    while ((value & ~0x7fL) != 0) {
      sink.writeByte(((int) value & 0x7f) | 0x80);
      value >>>= 7;
    }
    sink.writeByte((int) value);
  }

  /** Write a little-endian 32-bit integer. */
  public void writeFixed32(int value) throws IOException {
    sink.writeIntLe(value);
  }

  /** Write a little-endian 64-bit integer. */
  public void writeFixed64(long value) throws IOException {
    sink.writeLongLe(value);
  }
}
