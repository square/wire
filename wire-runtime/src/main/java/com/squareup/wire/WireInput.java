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
    return new WireInput(buf, 0, buf.length);
  }

  /**
   * Create a new WireInput wrapping the given byte array slice.
   */
  public static WireInput newInstance(byte[] buf, int offset, int count) {
    return new WireInput(buf, offset, count);
  }

  public static WireInput newInstance(InputStream input) {
    return new WireInput(input);
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
    int count = readVarint32();
    // Optimization: avoid an extra copy if the needed bytes are present in the buffer
    if (bytesRemaining() >= count) {
      String result = new String(buffer, pos, count, UTF_8);
      pos += count;
      return result;
    }
    return new String(readRawBytes(count), UTF_8);
  }

  /** Returns the number of unconsumed bytes in the current buffer. */
  private int bytesRemaining() {
    return limit - pos;
  }

  /**
   * Reads a {@code bytes} field value from the stream. The length is read from the
   * stream prior to the actual data.
   */
  public ByteString readBytes() throws IOException {
    int count = readVarint32();
    return readBytes(count);
  }

  /** Reads a {@code bytes} field value from the stream with a known length. */
  public ByteString readBytes(int count) throws IOException {
    // Optimization: avoid an extra copy if the needed bytes are present in the buffer
    if (bytesRemaining() >= count) {
      ByteString result = ByteString.of(buffer, pos, count);
      pos += count;
      return result;
    }
    return ByteString.of(readRawBytes(count));
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

  private static final int BUFFER_SIZE = 1024;

  private final InputStream input;

  /**
   * A buffer containing a subset of bytes from the input, running from position
   * {@code bufferOffset} to {@code bufferOffset + limit}.
   */
  private final byte[] buffer;

  /**
   * The input position associated with index 0 of {@code buffer}. This will be negative if the
   * input begins at a non-zero offset within the buffer.
   */
  private long bufferOffset = 0L;

  /**
   * The current position in the input buffer, starting at 0 and increasing monotonically.
   */
  private int pos = 0;

  /**
   * The number of readable bytes in the buffer. Indices 0 through limit - 1 contain
   * readable input bytes.
   */
  private int limit;

  /**
   * True if the end of the input has already been read into {@code buffer}.
   */
  private boolean inputStreamAtEof;

  private int lastTag;

  /** The absolute position of the end of the current message. */
  private int currentLimit = Integer.MAX_VALUE;

  public static final int RECURSION_LIMIT = 64;
  public int recursionDepth;

  private WireInput(InputStream input) {
    // Read the input stream as needed
    this.input = input;
    this.buffer = new byte[BUFFER_SIZE];
  }

  private WireInput(byte[] buffer, int start, int count) {
    this.input = null;

    // Keep a reference to the supplied buffer
    this.buffer = buffer;
    this.bufferOffset = -start;
    this.pos = start;
    this.limit = start + count;
    this.inputStreamAtEof = true;
  }

  /**
   * Refills the buffer if all bytes have been consumed. If unconsumed bytes
   * are present, or end-of-stream has been reached, does nothing.
   *
   * This method does not perform any buffer compaction. Consumers may call this method at
   * any time, but no new data will be read until all available bytes have been consumed.
   *
   * The {@code bytesRequested} parameter is a hint as to how many bytes to
   * request from the input stream when new data is required.
   * {@link InputStream#read(byte[], int, int)} will be called
   * repeatedly in an attempt to obtain the desired number of bytes. However,
   * we will never ask for more than {@link #BUFFER_SIZE} bytes.
   *
   * When end-of-stream is reached, the {@link #inputStreamAtEof} flag will be set.
   */
  private void refillBuffer(int bytesRequested) throws IOException {
    // If there is still data in the buffer, do nothing.
    // If there is no more data in the input stream, do nothing.
    if (pos < limit || inputStreamAtEof) {
      return;
    }

    bufferOffset += pos;
    pos = 0;
    int offset = 0;

    // Try to read at least bytesRequested bytes, but not more than BUFFER_SIZE.
    bytesRequested = Math.min(bytesRequested, BUFFER_SIZE);
    while (offset < bytesRequested) {
      int bytesRead = input.read(buffer, offset, BUFFER_SIZE - offset);
      if (bytesRead == -1) {
        // We reached end-of-stream
        limit = offset;
        inputStreamAtEof = true;
        return;
      }
      offset += bytesRead;
    }

    // There are still more bytes in the stream to read when these are consumed
    limit = offset;
    inputStreamAtEof = false;
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
    byteLimit += bufferOffset + pos;
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
  public boolean isAtEnd() throws IOException {
    // Treat the end of the current message (as specified by the message length field in the
    // protocol buffer wire encoding) as though it were end-of-stream.
    if (getPosition() == currentLimit) {
      return true;
    }
    refillBuffer(1);
    return bytesRemaining() == 0 && inputStreamAtEof;
  }

  /**
   * Get current position in buffer relative to beginning offset.
   */
  public long getPosition() {
    return bufferOffset + pos;
  }

  /**
   * Read one byte from the input.
   *
   * @throws IOException The end of the stream or the current limit was reached.
   */
  public byte readRawByte() throws IOException {
    refillBuffer(1);
    if (bytesRemaining() == 0) {
      throw new EOFException(INPUT_ENDED_UNEXPECTEDLY);
    }
    return buffer[pos++];
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

    byte[] bytes = new byte[size];
    int offset = 0;
    while (offset < size) {
      refillBuffer(size - offset);
      if (bytesRemaining() == 0) {
        throw new EOFException(INPUT_ENDED_UNEXPECTEDLY);
      }
      int count = Math.min(size - offset, bytesRemaining());
      System.arraycopy(buffer, pos, bytes, offset, count);
      pos += count;
      offset += count;
    }
    return bytes;
  }

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
      case LENGTH_DELIMITED: readRawBytes(readVarint32()); return false;
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
}
