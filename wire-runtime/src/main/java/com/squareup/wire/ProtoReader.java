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
  private static final String ENCOUNTERED_A_NEGATIVE_SIZE =
      "Encountered a negative size";
  private static final String INPUT_ENDED_UNEXPECTEDLY =
      "The input ended unexpectedly in the middle of a field";
  private static final String PROTOCOL_MESSAGE_TAG_ZERO =
      "Protocol message contained an invalid tag (zero).";
  private static final String PROTOCOL_MESSAGE_END_GROUP_TAG_DID_NOT_MATCH_EXPECTED_TAG =
      "Protocol message end-group tag did not match expected tag.";
  public static final String PROTOCOL_MESSAGE_GROUP_IS_TRUNCATED =
      "Protocol message group is truncated.";
  private static final String ENCOUNTERED_A_MALFORMED_VARINT =
      "WireInput encountered a malformed varint.";
  /** The standard number of levels of message nesting to allow. */
  private static final int RECURSION_LIMIT = 64;

  static final int FIELD_ENCODING_MASK = 0x7;
  static final int TAG_FIELD_ENCODING_BITS = 3;
  private static final int START_GROUP = 3;
  private static final int END_GROUP = 4;

  private final BufferedSource source;

  /** The current position in the input source, starting at 0 and increasing monotonically. */
  private long pos = 0;
  /** The absolute position of the end of the current message. */
  private long limit = Long.MAX_VALUE;
  /** The current number of levels of message nesting. */
  private int recursionDepth;
  /** The encoding of the next value to be read. */
  private FieldEncoding nextFieldEncoding;

  public ProtoReader(BufferedSource source) {
    this.source = source;
  }

  /**
   * True if there is more data to process in the current scope. This method's return value is
   * influenced by calls to {@link #beginLengthDelimited()}.
   */
  public boolean hasNext() throws IOException {
    return pos < limit && !source.exhausted();
  }

  /**
   * Begin a length-delimited section of the current message by reading the length prefix. A call
   * to this method will restrict the reader so that {@link #hasNext()} returns false when the
   * section is complete. An accompanying call to {@link #endLengthDelimited(long)} must then occur
   * with the opaque token returned from this method.
   */
  public long beginLengthDelimited() throws IOException {
    if (++recursionDepth > RECURSION_LIMIT) {
      throw new IOException("Wire recursion limit exceeded");
    }
    int length = readVarint32();
    if (length < 0) {
      throw new ProtocolException(ENCOUNTERED_A_NEGATIVE_SIZE);
    }
    long newLimit = pos + length;
    long oldLimit = limit;
    if (newLimit > oldLimit) {
      throw new EOFException(INPUT_ENDED_UNEXPECTEDLY);
    }
    limit = newLimit;
    // Give the old limit to the caller to hold. The value is returned in endLengthDelimited where
    // we resume using it as our limit.
    return oldLimit;
  }

  /**
   * End a length-delimited section of the current message. Calls to this method must be symmetric
   * with calls to {@link #beginLengthDelimited()}.
   *
   * @param token value returned from the corresponding call to {@link #beginLengthDelimited()}.
   */
  public void endLengthDelimited(long token) throws IOException {
    if (pos != limit) {
      throw new IOException("Expected to end at " + limit + " but was " + pos);
    }
    if (--recursionDepth < 0) {
      throw new IllegalStateException("No corresponding call to beginLengthDelimited()");
    }
    limit = token;
  }

  /**
   * Reads and returns the next tag of the message, or -1 if there are no further tags. Use {@link
   * #peekFieldEncoding()} after calling this method to query its encoding. This silently skips
   * groups.
   */
  public int nextTag() throws IOException {
    while (hasNext()) {
      int tagAndFieldEncoding = readVarint32();
      if (tagAndFieldEncoding == 0) throw new ProtocolException(PROTOCOL_MESSAGE_TAG_ZERO);

      int tag = tagAndFieldEncoding >> TAG_FIELD_ENCODING_BITS;
      int groupOrFieldEncoding = tagAndFieldEncoding & FIELD_ENCODING_MASK;
      switch (groupOrFieldEncoding) {
        case START_GROUP:
          skipGroup(tag);
          continue;
        case END_GROUP:
          continue; // TODO(jwilson): crash if we get an end group without a preceding start group.
        default:
          nextFieldEncoding = FieldEncoding.get(groupOrFieldEncoding);
          return tag;
      }
    }
    return -1;
  }

  /**
   * Returns the encoding of the next field value. {@link #nextTag()} must be called before
   * this method.
   */
  public FieldEncoding peekFieldEncoding() throws IOException {
    return nextFieldEncoding;
  }

  /** Skips a section of the input delimited by START_GROUP/END_GROUP type markers. */
  void skipGroup(int expectedEndTag) throws IOException {
    while (hasNext()) {
      int tagAndFieldEncoding = readVarint32();
      if (tagAndFieldEncoding == 0) throw new ProtocolException(PROTOCOL_MESSAGE_TAG_ZERO);
      int tag = tagAndFieldEncoding >> TAG_FIELD_ENCODING_BITS;
      int groupOrFieldEncoding = tagAndFieldEncoding & FIELD_ENCODING_MASK;
      switch (groupOrFieldEncoding) {
        case START_GROUP:
          skipGroup(tag); // Nested group.
          break;
        case END_GROUP:
          if (tag == expectedEndTag) return; // Success!
          throw new ProtocolException(PROTOCOL_MESSAGE_END_GROUP_TAG_DID_NOT_MATCH_EXPECTED_TAG);
        default:
          FieldEncoding fieldEncoding = FieldEncoding.get(groupOrFieldEncoding);
          fieldEncoding.rawTypeAdapter().read(this);
      }
    }
    throw new ProtocolException(PROTOCOL_MESSAGE_GROUP_IS_TRUNCATED);
  }

  int readByte() throws IOException {
    source.require(1); // Throws EOFException if insufficient bytes are available.
    pos++;
    return source.readByte() & 0xff;
  }

  /**
   * Reads a {@code bytes} field value from the stream. The length is read from the
   * stream prior to the actual data.
   */
  ByteString readBytes() throws IOException {
    int count = readVarint32();
    source.require(count); // Throws EOFException if insufficient bytes are available.
    pos += count;
    return source.readByteString(count);
  }

  /** Reads a {@code string} field value from the stream. */
  String readString() throws IOException {
    int count = readVarint32();
    source.require(count); // Throws EOFException if insufficient bytes are available.
    pos += count;
    return source.readUtf8(count);
  }

  /**
   * Reads a raw varint from the stream.  If larger than 32 bits, discard the
   * upper bits.
   */
  int readVarint32() throws IOException {
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
            throw new ProtocolException(ENCOUNTERED_A_MALFORMED_VARINT);
          }
        }
      }
    }
    return result;
  }

  /** Reads a raw varint up to 64 bits in length from the stream. */
  long readVarint64() throws IOException {
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
    throw new ProtocolException(ENCOUNTERED_A_MALFORMED_VARINT);
  }

  /** Reads a 32-bit little-endian integer from the stream. */
  int readFixed32() throws IOException {
    source.require(4); // Throws EOFException if insufficient bytes are available.
    pos += 4;
    return source.readIntLe();
  }

  /** Reads a 64-bit little-endian integer from the stream. */
  long readFixed64() throws IOException {
    source.require(8); // Throws EOFException if insufficient bytes are available.
    pos += 8;
    return source.readLongLe();
  }
}
