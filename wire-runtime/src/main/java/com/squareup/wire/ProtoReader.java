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
import java.net.ProtocolException;
import okio.BufferedSource;
import okio.ByteString;

/**
 * Reads and decodes protocol message fields.
 */
public final class ProtoReader {
  /** The standard number of levels of message nesting to allow. */
  private static final int RECURSION_LIMIT = 65;

  private static final int FIELD_ENCODING_MASK = 0x7;
  static final int TAG_FIELD_ENCODING_BITS = 3;

  /** Read states. These constants correspond to field encodings where both exist. */
  private static final int STATE_VARINT = 0;
  private static final int STATE_FIXED64 = 1;
  private static final int STATE_LENGTH_DELIMITED = 2;
  private static final int STATE_START_GROUP = 3;
  private static final int STATE_END_GROUP = 4;
  private static final int STATE_FIXED32 = 5;
  private static final int STATE_TAG = 6; // Note: not a field encoding.
  private static final int STATE_PACKED_TAG = 7; // Note: not a field encoding.

  private final BufferedSource source;

  /** The current position in the input source, starting at 0 and increasing monotonically. */
  private long pos = 0;
  /** The absolute position of the end of the current message. */
  private long limit = Long.MAX_VALUE;
  /** The current number of levels of message nesting. */
  private int recursionDepth;
  /** How to interpret the next read call. */
  private int state = STATE_LENGTH_DELIMITED;
  /** The most recently read tag. Used to make packed values look like regular values. */
  private int tag = -1;
  /** Limit once we complete the current length-delimited value. */
  private long pushedLimit = -1;
  /** The encoding of the next value to be read. */
  private FieldEncoding nextFieldEncoding;

  public ProtoReader(BufferedSource source) {
    this.source = source;
  }

  /**
   * Begin a nested message. A call to this method will restrict the reader so that {@link
   * #nextTag()} returns -1 when the message is complete. An accompanying call to {@link
   * #endMessage(long)} must then occur with the opaque token returned from this method.
   */
  public long beginMessage() throws IOException {
    if (state != STATE_LENGTH_DELIMITED) {
      throw new IllegalStateException("Unexpected call to beginMessage()");
    }
    if (++recursionDepth > RECURSION_LIMIT) {
      throw new IOException("Wire recursion limit exceeded");
    }
    // Give the pushed limit to the caller to hold. The value is returned in endMessage() where we
    // resume using it as our limit.
    long token = pushedLimit;
    pushedLimit = -1L;
    state = STATE_TAG;
    return token;
  }

  /**
   * End a length-delimited nested message. Calls to this method must be symmetric with calls to
   * {@link #beginMessage()}.
   *
   * @param token value returned from the corresponding call to {@link #beginMessage()}.
   */
  public void endMessage(long token) throws IOException {
    if (state != STATE_TAG) {
      throw new IllegalStateException("Unexpected call to endMessage()");
    }
    if (--recursionDepth < 0 || pushedLimit != -1L) {
      throw new IllegalStateException("No corresponding call to beginMessage()");
    }
    if (pos != limit && recursionDepth != 0) {
      throw new IOException("Expected to end at " + limit + " but was " + pos);
    }
    limit = token;
  }

  /**
   * Reads and returns the next tag of the message, or -1 if there are no further tags. Use {@link
   * #peekFieldEncoding()} after calling this method to query its encoding. This silently skips
   * groups.
   */
  public int nextTag() throws IOException {
    if (state == STATE_PACKED_TAG) {
      state = STATE_LENGTH_DELIMITED;
      return tag;
    } else if (state != STATE_TAG) {
      throw new IllegalStateException("Unexpected call to nextTag()");
    }

    while (pos < limit && !source.exhausted()) {
      int tagAndFieldEncoding = internalReadVarint32();
      if (tagAndFieldEncoding == 0) throw new ProtocolException("Unexpected tag 0");

      tag = tagAndFieldEncoding >> TAG_FIELD_ENCODING_BITS;
      int groupOrFieldEncoding = tagAndFieldEncoding & FIELD_ENCODING_MASK;
      switch (groupOrFieldEncoding) {
        case STATE_START_GROUP:
          skipGroup(tag);
          continue;

        case STATE_END_GROUP:
          throw new ProtocolException("Unexpected end group");

        case STATE_LENGTH_DELIMITED:
          nextFieldEncoding = FieldEncoding.LENGTH_DELIMITED;
          state = STATE_LENGTH_DELIMITED;
          int length = internalReadVarint32();
          if (length < 0) throw new ProtocolException("Negative length: " + length);
          if (pushedLimit != -1) throw new IllegalStateException();
          // Push the current limit, and set a new limit to the length of this value.
          pushedLimit = limit;
          limit = pos + length;
          if (limit > pushedLimit) throw new EOFException();
          return tag;

        case STATE_VARINT:
          nextFieldEncoding = FieldEncoding.VARINT;
          state = STATE_VARINT;
          return tag;

        case STATE_FIXED64:
          nextFieldEncoding = FieldEncoding.FIXED64;
          state = STATE_FIXED64;
          return tag;

        case STATE_FIXED32:
          nextFieldEncoding = FieldEncoding.FIXED32;
          state = STATE_FIXED32;
          return tag;

        default:
          throw new ProtocolException("Unexpected field encoding: " + groupOrFieldEncoding);
      }
    }
    return -1;
  }

  /**
   * Returns the encoding of the next field value. {@link #nextTag()} must be called before
   * this method.
   */
  public FieldEncoding peekFieldEncoding() {
    return nextFieldEncoding;
  }

  /**
   * Skips the current field's value. This is only safe to call immediately following a call to
   * {@link #nextTag()}.
   */
  public void skip() throws IOException {
    switch (state) {
      case STATE_LENGTH_DELIMITED:
        long byteCount = beforeLengthDelimitedScalar();
        source.skip(byteCount);
        break;
      case STATE_VARINT:
        readVarint64();
        break;
      case STATE_FIXED64:
        readFixed64();
        break;
      case STATE_FIXED32:
        readFixed32();
        break;
      default:
        throw new IllegalStateException("Unexpected call to skip()");
    }
  }

  /** Skips a section of the input delimited by START_GROUP/END_GROUP type markers. */
  private void skipGroup(int expectedEndTag) throws IOException {
    while (pos < limit && !source.exhausted()) {
      int tagAndFieldEncoding = internalReadVarint32();
      if (tagAndFieldEncoding == 0) throw new ProtocolException("Unexpected tag 0");
      int tag = tagAndFieldEncoding >> TAG_FIELD_ENCODING_BITS;
      int groupOrFieldEncoding = tagAndFieldEncoding & FIELD_ENCODING_MASK;
      switch (groupOrFieldEncoding) {
        case STATE_START_GROUP:
          skipGroup(tag); // Nested group.
          break;
        case STATE_END_GROUP:
          if (tag == expectedEndTag) return; // Success!
          throw new ProtocolException("Unexpected end group");
        case STATE_LENGTH_DELIMITED:
          int length = internalReadVarint32();
          pos += length;
          source.skip(length);
          break;
        case STATE_VARINT:
          state = STATE_VARINT;
          readVarint64();
          break;
        case STATE_FIXED64:
          state = STATE_FIXED64;
          readFixed64();
          break;
        case STATE_FIXED32:
          state = STATE_FIXED32;
          readFixed32();
          break;
        default:
          throw new ProtocolException("Unexpected field encoding: " + groupOrFieldEncoding);
      }
    }
    throw new EOFException();
  }

  /**
   * Reads a {@code bytes} field value from the stream. The length is read from the
   * stream prior to the actual data.
   */
  public ByteString readBytes() throws IOException {
    long byteCount = beforeLengthDelimitedScalar();
    return source.readByteString(byteCount);
  }

  /** Reads a {@code string} field value from the stream. */
  public String readString() throws IOException {
    long byteCount = beforeLengthDelimitedScalar();
    return source.readUtf8(byteCount);
  }

  /**
   * Reads a raw varint from the stream.  If larger than 32 bits, discard the
   * upper bits.
   */
  public int readVarint32() throws IOException {
    if (state != STATE_VARINT && state != STATE_LENGTH_DELIMITED) {
      throw new ProtocolException("Expected VARINT or LENGTH_DELIMITED but was " + state);
    }
    int result = internalReadVarint32();
    afterPackableScalar(STATE_VARINT);
    return result;
  }

  private int internalReadVarint32() throws IOException {
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
            throw new ProtocolException("Malformed VARINT");
          }
        }
      }
    }
    return result;
  }

  /** Reads a raw varint up to 64 bits in length from the stream. */
  public long readVarint64() throws IOException {
    if (state != STATE_VARINT && state != STATE_LENGTH_DELIMITED) {
      throw new ProtocolException("Expected VARINT or LENGTH_DELIMITED but was " + state);
    }
    int shift = 0;
    long result = 0;
    while (shift < 64) {
      pos++;
      byte b = source.readByte();
      result |= (long) (b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        afterPackableScalar(STATE_VARINT);
        return result;
      }
      shift += 7;
    }
    throw new ProtocolException("WireInput encountered a malformed varint");
  }

  /** Reads a 32-bit little-endian integer from the stream. */
  public int readFixed32() throws IOException {
    if (state != STATE_FIXED32 && state != STATE_LENGTH_DELIMITED) {
      throw new ProtocolException("Expected FIXED32 or LENGTH_DELIMITED but was " + state);
    }
    source.require(4); // Throws EOFException if insufficient bytes are available.
    pos += 4;
    int result = source.readIntLe();
    afterPackableScalar(STATE_FIXED32);
    return result;
  }

  /** Reads a 64-bit little-endian integer from the stream. */
  public long readFixed64() throws IOException {
    if (state != STATE_FIXED64 && state != STATE_LENGTH_DELIMITED) {
      throw new ProtocolException("Expected FIXED64 or LENGTH_DELIMITED but was " + state);
    }
    source.require(8); // Throws EOFException if insufficient bytes are available.
    pos += 8;
    long result = source.readLongLe();
    afterPackableScalar(STATE_FIXED64);
    return result;
  }

  private void afterPackableScalar(int fieldEncoding) throws IOException {
    if (state == fieldEncoding) {
      state = STATE_TAG;
    } else {
      if (pos > limit) {
        throw new IOException("Expected to end at " + limit + " but was " + pos);
      } else if (pos == limit) {
        // We've completed a sequence of packed values. Pop the limit.
        limit = pushedLimit;
        pushedLimit = -1;
        state = STATE_TAG;
      } else {
        state = STATE_PACKED_TAG;
      }
    }
  }

  private long beforeLengthDelimitedScalar() throws IOException {
    if (state != STATE_LENGTH_DELIMITED) {
      throw new ProtocolException("Expected LENGTH_DELIMITED but was " + state);
    }
    long byteCount = limit - pos;
    source.require(byteCount); // Throws EOFException if insufficient bytes are available.
    state = STATE_TAG;
    // We've completed a length-delimited scalar. Pop the limit.
    pos = limit;
    limit = pushedLimit;
    pushedLimit = -1;
    return byteCount;
  }
}
