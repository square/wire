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

import java.io.IOException;

/**
 * Reads and decodes protocol message fields.
 *
 * <p>See GOOGLE_COPYRIGHT.txt for original copyright notice.</p>
 */
final class WireInput {

  private static final String UTF_8 = "UTF-8";

  private static final String ENCOUNTERED_A_NEGATIVE_SIZE =
      "Encountered a negative size";
  private static final String INPUT_ENDED_UNEXPECTEDLY =
      "The input ended unexpectedly in the middle of a field";
  private static final String PROTOCOL_MESSAGE_CONTAINED_AN_INVALID_TAG_ZERO =
      "Protocol message contained an invalid tag (zero).";
  private static final String PROTOCOL_MESSAGE_END_GROUP_TAG_DID_NOT_MATCH_EXPECTED_TAG =
      "Protocol message end-group tag did not match expected tag.";
  private static final String ENCOUNTERED_A_MALFORMED_VARINT =
      "WireInput encountered a malformed varint.";

  /**
   * Create a new WireInput wrapping the given byte array.
   */
  public static WireInput newInstance(byte[] buf) {
    return newInstance(buf, 0, buf.length);
  }

  /**
   * Create a new WireInput wrapping the given byte array slice.
   */
  public static WireInput newInstance(byte[] buf, int offset, int count) {
    return new WireInput(buf, offset, count);
  }

  // -----------------------------------------------------------------

  /**
   * Attempt to read a field tag, returning zero if we have reached EOF.
   * Protocol message parsers use this to read tags, since a protocol message
   * may legally end wherever a tag occurs, and zero is not a valid tag number.
   */
  public int readTag() throws IOException {
    if (isAtEnd()) {
      lastTag = 0;
      return 0;
    }

    lastTag = readVarint32();
    if (lastTag == 0) {
      // If we actually read zero, that's not a valid tag.
      throw new IOException(PROTOCOL_MESSAGE_CONTAINED_AN_INVALID_TAG_ZERO);
    }
    return lastTag;
  }

  /**
   * Verifies that the last call to readTag() returned the given tag value.
   * This is used to verify that a nested group ended with the correct
   * end tag.
   *
   * @throws IOException if {@code value} does not match the last tag.
   */
  public void checkLastTagWas(int value) throws IOException {
    if (lastTag != value) {
      throw new IOException(PROTOCOL_MESSAGE_END_GROUP_TAG_DID_NOT_MATCH_EXPECTED_TAG);
    }
  }

  /** Read a {@code string} field value from the stream. */
  public String readString() throws IOException {
    int size = readVarint32();
    if (size <= (bufferSize - bufferPos) && size > 0) {
      // Fast path:  We already have the bytes in a contiguous buffer, so
      //   just copy directly from it.
      String result = new String(buffer, bufferPos, size, UTF_8);
      bufferPos += size;
      return result;
    } else {
      // Slow path:  Build a byte array first then copy it.
      return new String(readRawBytes(size), UTF_8);
    }
  }

  /** Read a {@code bytes} field value from the stream. */
  public ByteString readBytes() throws IOException {
    int size = readVarint32();
    if (size <= (bufferSize - bufferPos) && size > 0) {
      // Fast path:  We already have the bytes in a contiguous buffer, so
      //   just copy directly from it.
      byte[] result = new byte[size];
      System.arraycopy(buffer, bufferPos, result, 0, size);
      bufferPos += size;
      return ByteString.of(result);
    } else {
      // Slow path:  Build a byte array first then copy it.
      return ByteString.of(readRawBytes(size));
    }
  }

  /**
   * Read a raw varint from the stream.  If larger than 32 bits, discard the
   * upper bits.
   */
  public int readVarint32() throws IOException {
    byte tmp = readRawByte();
    if (tmp >= 0) {
      return tmp;
    }
    int result = tmp & 0x7f;
    if ((tmp = readRawByte()) >= 0) {
      result |= tmp << 7;
    } else {
      result |= (tmp & 0x7f) << 7;
      if ((tmp = readRawByte()) >= 0) {
        result |= tmp << 14;
      } else {
        result |= (tmp & 0x7f) << 14;
        if ((tmp = readRawByte()) >= 0) {
          result |= tmp << 21;
        } else {
          result |= (tmp & 0x7f) << 21;
          result |= (tmp = readRawByte()) << 28;
          if (tmp < 0) {
            // Discard upper 32 bits.
            for (int i = 0; i < 5; i++) {
              if (readRawByte() >= 0) {
                return result;
              }
            }
            throw new IOException(ENCOUNTERED_A_MALFORMED_VARINT);
          }
        }
      }
    }
    return result;
  }

  /** Read a raw Varint from the stream. */
  public long readVarint64() throws IOException {
    int shift = 0;
    long result = 0;
    while (shift < 64) {
      byte b = readRawByte();
      result |= (long) (b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        return result;
      }
      shift += 7;
    }
    throw new IOException(ENCOUNTERED_A_MALFORMED_VARINT);
  }

  /** Read a 32-bit little-endian integer from the stream. */
  public int readFixed32() throws IOException {
    byte b1 = readRawByte();
    byte b2 = readRawByte();
    byte b3 = readRawByte();
    byte b4 = readRawByte();
    return   (b1 & 0xff)
          | ((b2 & 0xff) <<  8)
          | ((b3 & 0xff) << 16)
          | ((b4 & 0xff) << 24);
  }

  /** Read a 64-bit little-endian integer from the stream. */
  public long readFixed64() throws IOException {
    byte b1 = readRawByte();
    byte b2 = readRawByte();
    byte b3 = readRawByte();
    byte b4 = readRawByte();
    byte b5 = readRawByte();
    byte b6 = readRawByte();
    byte b7 = readRawByte();
    byte b8 = readRawByte();
    return    ((long) b1 & 0xff)
           | (((long) b2 & 0xff) <<  8)
           | (((long) b3 & 0xff) << 16)
           | (((long) b4 & 0xff) << 24)
           | (((long) b5 & 0xff) << 32)
           | (((long) b6 & 0xff) << 40)
           | (((long) b7 & 0xff) << 48)
           | (((long) b8 & 0xff) << 56);
  }

  /**
   * Decode a ZigZag-encoded 32-bit value.  ZigZag encodes signed integers
   * into values that can be efficiently encoded with varint.  (Otherwise,
   * negative values must be sign-extended to 64 bits to be varint encoded,
   * thus always taking 10 bytes on the wire.)
   *
   * @param n An unsigned 32-bit integer, stored in a signed int because
   *          Java has no explicit unsigned support.
   * @return A signed 32-bit integer.
   */
  public static int decodeZigZag32(int n) {
    return (n >>> 1) ^ -(n & 1);
  }

  /**
   * Decode a ZigZag-encoded 64-bit value.  ZigZag encodes signed integers
   * into values that can be efficiently encoded with varint.  (Otherwise,
   * negative values must be sign-extended to 64 bits to be varint encoded,
   * thus always taking 10 bytes on the wire.)
   *
   * @param n An unsigned 64-bit integer, stored in a signed int because
   *          Java has no explicit unsigned support.
   * @return A signed 64-bit integer.
   */
  public static long decodeZigZag64(long n) {
    return (n >>> 1) ^ -(n & 1);
  }

  // -----------------------------------------------------------------

  private final byte[] buffer;
  private final int bufferStart;
  private int bufferSize;
  private int bufferSizeAfterLimit;
  private int bufferPos;
  private int lastTag;

  /** The absolute position of the end of the current message. */
  private int currentLimit = Integer.MAX_VALUE;

  public static final int RECURSION_LIMIT = 64;
  public int recursionDepth;

  private WireInput(byte[] buffer, int offset, int count) {
    this.buffer = buffer;
    bufferStart = offset;
    bufferSize = offset + count;
    bufferPos = offset;
  }

  /**
   * Sets {@code currentLimit} to (current position) + {@code byteLimit}.  This
   * is called when descending into a length-delimited embedded message.
   *
   * @return the old limit.
   */
  public int pushLimit(int byteLimit) throws IOException {
    if (byteLimit < 0) {
      throw new IOException(ENCOUNTERED_A_NEGATIVE_SIZE);
    }
    byteLimit += bufferPos;
    int oldLimit = currentLimit;
    if (byteLimit > oldLimit) {
      throw new IOException(INPUT_ENDED_UNEXPECTEDLY);
    }
    currentLimit = byteLimit;
    recomputeBufferSizeAfterLimit();
    return oldLimit;
  }

  private void recomputeBufferSizeAfterLimit() {
    bufferSize += bufferSizeAfterLimit;
    int bufferEnd = bufferSize;
    if (bufferEnd > currentLimit) {
      // Limit is in current buffer.
      bufferSizeAfterLimit = bufferEnd - currentLimit;
      bufferSize -= bufferSizeAfterLimit;
    } else {
      bufferSizeAfterLimit = 0;
    }
  }

  /**
   * Discards the current limit, returning to the previous limit.
   *
   * @param oldLimit The old limit, as returned by {@code pushLimit}.
   */
  public void popLimit(int oldLimit) {
    currentLimit = oldLimit;
    recomputeBufferSizeAfterLimit();
  }

  /**
   * Returns true if the stream has reached the end of the input.  This is the
   * case if either the end of the underlying input source has been reached or
   * if the stream has reached a limit created using {@link #pushLimit(int)}.
   */
  public boolean isAtEnd() {
    return bufferPos == bufferSize;
  }

  /**
   * Get current position in buffer relative to beginning offset.
   */
  public int getPosition() {
    return bufferPos - bufferStart;
  }

  /**
   * Read one byte from the input.
   *
   * @throws IOException The end of the stream or the current limit was reached.
   */
  public byte readRawByte() throws IOException {
    if (bufferPos == bufferSize) {
      throw new IOException(INPUT_ENDED_UNEXPECTEDLY);
    }
    return buffer[bufferPos++];
  }

  /**
   * Read a fixed size of bytes from the input.
   *
   * @throws IOException The end of the stream or the current limit was reached.
   */
  public byte[] readRawBytes(int size) throws IOException {
    if (size < 0) {
      throw new IOException(ENCOUNTERED_A_NEGATIVE_SIZE);
    }

    if (bufferPos + size > currentLimit) {
      bufferPos = currentLimit;
      throw new IOException(INPUT_ENDED_UNEXPECTEDLY);
    }

    if (size <= bufferSize - bufferPos) {
      // We have all the bytes we need already.
      byte[] bytes = new byte[size];
      System.arraycopy(buffer, bufferPos, bytes, 0, size);
      bufferPos += size;
      return bytes;
    } else {
      throw new IOException(INPUT_ENDED_UNEXPECTEDLY);
    }
  }

  public void skipGroup() throws IOException {
    while (true) {
      int tag = readTag();
      if (tag == 0 || !skipField(tag)) {
        return;
      }
    }
  }

  private boolean skipField(int tag) throws IOException {
    switch (WireType.valueOf(tag)) {
      case VARINT: readVarint64(); return true;
      case FIXED32: readFixed32(); return true;
      case FIXED64: readFixed64(); return true;
      case LENGTH_DELIMITED: readRawBytes(readVarint32()); return true;
      case START_GROUP:
        skipGroup();
        checkLastTagWas((tag & ~0x7) | WireType.END_GROUP.value());
        return true;
      case END_GROUP:
        return false;
      default:
        throw new AssertionError();
    }
  }
}
