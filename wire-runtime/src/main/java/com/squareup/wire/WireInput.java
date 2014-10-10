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
package com.squareup.wire;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import okio.Buffer;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;
import okio.Source;

/**
 * Reads and decodes protocol message fields.
 */
final class WireInput {

  private static final Charset UTF_8 = Charset.forName("UTF-8");

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
    return new WireInput(new Buffer().write(buf));
  }

  /**
   * Create a new WireInput wrapping the given byte array slice.
   */
  public static WireInput newInstance(byte[] buf, int offset, int count) {
    return new WireInput(new Buffer().write(buf, offset, count));
  }

  public static WireInput newInstance(InputStream source) {
    return new WireInput(Okio.buffer(Okio.source(source)));
  }

  public static WireInput newInstance(Source source) {
    return new WireInput(Okio.buffer(source));
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

  /** Reads a {@code string} field value from the stream. */
  public String readString() throws IOException {
    int count = readVarint32();
    pos += count;
    return source.readString(count, UTF_8);
  }

  /**
   * Reads a {@code bytes} field value from the stream. The length is read from the
   * stream prior to the actual data.
   */
  public ByteString readBytes() throws IOException {
    int count = readVarint32();
    return readBytes(count);
  }

  /** Reads a ByteString from the stream with a given size in bytes. */
  public ByteString readBytes(int count) throws IOException {
    pos += count;
    source.require(count); // Throws EOFException if insufficient bytes are available.
    return source.readByteString(count);
  }

  /**
   * Reads a raw varint from the stream.  If larger than 32 bits, discard the
   * upper bits.
   */
  public int readVarint32() throws IOException {
    pos++;
    byte tmp = source.readByte();
    if (tmp >= 0) {
      return tmp;
    }
    int result = tmp & 0x7f;
    pos++;
    if ((tmp = source.readByte()) >= 0) {
      result |= tmp << 7;
    } else {
      result |= (tmp & 0x7f) << 7;
      pos++;
      if ((tmp = source.readByte()) >= 0) {
        result |= tmp << 14;
      } else {
        result |= (tmp & 0x7f) << 14;
        pos++;
        if ((tmp = source.readByte()) >= 0) {
          result |= tmp << 21;
        } else {
          result |= (tmp & 0x7f) << 21;
          pos++;
          result |= (tmp = source.readByte()) << 28;
          if (tmp < 0) {
            // Discard upper 32 bits.
            for (int i = 0; i < 5; i++) {
              pos++;
              if (source.readByte() >= 0) {
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

  /** Reads a raw varint up to 64 bits in length from the stream. */
  public long readVarint64() throws IOException {
    int shift = 0;
    long result = 0;
    while (shift < 64) {
      pos++;
      byte b = source.readByte();
      result |= (long) (b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        return result;
      }
      shift += 7;
    }
    throw new IOException(ENCOUNTERED_A_MALFORMED_VARINT);
  }

  /** Reads a 32-bit little-endian integer from the stream. */
  public int readFixed32() throws IOException {
    pos += 4;
    return source.readIntLe();
  }

  /** Reads a 64-bit little-endian integer from the stream. */
  public long readFixed64() throws IOException {
    pos += 8;
    return source.readLongLe();
  }

  /**
   * Decodes a ZigZag-encoded 32-bit value.  ZigZag encodes signed integers
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
   * Decodes a ZigZag-encoded 64-bit value.  ZigZag encodes signed integers
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

  /** The Okio input source. */
  private final BufferedSource source;

  /**
   * The current position in the input source, starting at 0 and increasing monotonically.
   */
  private int pos = 0;

  /** The absolute position of the end of the current message. */
  private int currentLimit = Integer.MAX_VALUE;

  /** The standard number of levels of message nesting to allow. */
  public static final int RECURSION_LIMIT = 64;

  /** The current number of levels of message nesting. */
  public int recursionDepth;

  /** The last tag that was read. */
  private int lastTag;

  private WireInput(BufferedSource source) {
    this.source = source;
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
    byteLimit += pos;
    int oldLimit = currentLimit;
    if (byteLimit > oldLimit) {
      throw new EOFException(INPUT_ENDED_UNEXPECTEDLY);
    }
    currentLimit = byteLimit;
    return oldLimit;
  }

  /**
   * Discards the current limit, returning to the previous limit.
   *
   * @param oldLimit The old limit, as returned by {@code pushLimit}.
   */
  public void popLimit(int oldLimit) {
    currentLimit = oldLimit;
  }

  /**
   * Returns true if the stream has reached the end of the input.  This is the
   * case if either the end of the underlying input source has been reached or
   * if the stream has reached a limit created using {@link #pushLimit(int)}.
   */
  private boolean isAtEnd() throws IOException {
    // Treat the end of the current message (as specified by the message length field in the
    // protocol buffer wire encoding) as though it were end-of-stream.
    if (getPosition() == currentLimit) {
      return true;
    }
    return source.exhausted();
  }

  /**
   * Returns the current source position in bytes.
   */
  public long getPosition() {
    return pos;
  }

  /** Skips a section of the input delimited by START_GROUP/END_GROUP type markers. */
  public void skipGroup() throws IOException {
    while (true) {
      int tag = readTag();
      if (tag == 0 || skipField(tag)) {
        return;
      }
    }
  }

  // Returns true when END_GROUP tag found
  private boolean skipField(int tag) throws IOException {
    switch (WireType.valueOf(tag)) {
      case VARINT: readVarint64(); return false;
      case FIXED32: readFixed32(); return false;
      case FIXED64: readFixed64(); return false;
      case LENGTH_DELIMITED: skip(readVarint32()); return false;
      case START_GROUP:
        skipGroup();
        checkLastTagWas((tag & ~0x7) | WireType.END_GROUP.value());
        return false;
      case END_GROUP:
        return true;
      default:
        throw new AssertionError();
    }
  }

  // Skips count bytes of input.
  private void skip(long count) throws IOException {
    pos += count;
    source.skip(count);
  }
}
