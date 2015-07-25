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
import java.util.List;
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
  private static final String PROTOCOL_MESSAGE_CONTAINED_AN_INVALID_TAG_ZERO =
      "Protocol message contained an invalid tag (zero).";
  private static final String PROTOCOL_MESSAGE_END_GROUP_TAG_DID_NOT_MATCH_EXPECTED_TAG =
      "Protocol message end-group tag did not match expected tag.";
  private static final String ENCOUNTERED_A_MALFORMED_VARINT =
      "WireInput encountered a malformed varint.";
  private static final int RECURSION_LIMIT = 64;

  private final BufferedSource source;
  private final ExtensionRegistry extensionRegistry;

  /** The current position in the source, starting at 0 and increasing monotonically. */
  private long pos = 0;
  /** The absolute position of the end of the current message. */
  private long currentLimit = Long.MAX_VALUE;
  /** The current number of levels of message nesting. */
  public int recursionDepth;
  /** The last tag that was read. */
  private int lastTag;

  public ProtoReader(BufferedSource source) {
    this(source, null);
  }

  // TODO ctor for extension registration

  ProtoReader(BufferedSource source, ExtensionRegistry extensionRegistry) {
    this.source = source;
    this.extensionRegistry = extensionRegistry;
  }

  public <E> E value(TypeAdapter<E> adapter) throws IOException {
    return adapter.read(this);
  }

  public <E> List<E> repeated(List<E> existing, TypeAdapter<E> adapter) throws IOException {
    return Message.repeated(existing, adapter.read(this));
  }

  public <E> List<E> packed(List<E> existing, TypeAdapter<E> adapter) throws IOException {
    long cursor = beginLengthDelimited();
    while (hasNext()) {
      existing = Message.repeated(existing, adapter.read(this));
    }
    endLengthDelimited(cursor);
    return existing;
  }

  public <T extends ExtendableMessage<T>> Extension<T, ?, ?> getExtension(Class<T> type, int tag) {
    return extensionRegistry == null ? null : extensionRegistry.getExtension(type, tag);
  }

  public long beginLengthDelimited() throws IOException {
    if (++recursionDepth > RECURSION_LIMIT) {
      throw new IOException("Wire recursion limit exceeded");
    }
    int length = readVarint32();
    if (length < 0) {
      throw new IOException(ENCOUNTERED_A_NEGATIVE_SIZE);
    }
    long newLimit = length + pos;
    long oldLimit = currentLimit;
    if (length > oldLimit) {
      throw new EOFException(INPUT_ENDED_UNEXPECTEDLY);
    }
    currentLimit = newLimit;
    return oldLimit;
  }

  public boolean hasNext() throws IOException {
    return pos < currentLimit && !source.exhausted();
  }

  public void endLengthDelimited(long cursor) throws IOException {
    if (pos != currentLimit) {
      throw new IOException("Expected to end at " + currentLimit + " but was " + pos);
    }
    currentLimit = cursor;
    --recursionDepth;
  }

  /**
   * Attempt to read a field tag, returning zero if we have reached EOF.
   * Protocol message parsers use this to read tags, since a protocol message
   * may legally end wherever a tag occurs, and zero is not a valid tag number.
   */
  public int nextTag() throws IOException {
    lastTag = readVarint32();
    if (lastTag == 0) {
      throw new IOException(PROTOCOL_MESSAGE_CONTAINED_AN_INVALID_TAG_ZERO);
    }
    return lastTag >> WireType.TAG_TYPE_BITS;
  }

  int lastTagType() {
    return lastTag & WireType.TAG_TYPE_MASK;
  }

  /**
   * Verifies that the last call to nextTag() returned the given tag value.
   * This is used to verify that a nested group ended with the correct
   * end tag.
   *
   * @throws IOException if {@code value} does not match the last tag.
   */
  void checkLastTagWas(int value) throws IOException {
    if (lastTag != value) {
      throw new IOException(PROTOCOL_MESSAGE_END_GROUP_TAG_DID_NOT_MATCH_EXPECTED_TAG);
    }
  }

  /** Reads a {@code string} field value from the stream. */
  String readString() throws IOException {
    int count = readVarint32();
    pos += count;
    return source.readUtf8(count);
  }

  /**
   * Reads a {@code bytes} field value from the stream. The length is read from the
   * stream prior to the actual data.
   */
  ByteString readBytes() throws IOException {
    int count = readVarint32();
    source.require(count); // Throws EOFException on insufficient bytes.
    pos += count;
    return source.readByteString(count);
  }

  int readRawByte() throws IOException {
    pos++;
    return source.readByte() & 0xFF;
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
            throw new IOException(ENCOUNTERED_A_MALFORMED_VARINT);
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
    throw new IOException(ENCOUNTERED_A_MALFORMED_VARINT);
  }

  /** Reads a 32-bit little-endian integer from the stream. */
  int readFixed32() throws IOException {
    pos += 4;
    return source.readIntLe();
  }

  /** Reads a 64-bit little-endian integer from the stream. */
  long readFixed64() throws IOException {
    pos += 8;
    return source.readLongLe();
  }

  // -----------------------------------------------------------------

  /** Skips a section of the input delimited by START_GROUP/END_GROUP type markers. */
  void skipGroup() throws IOException {
    //noinspection StatementWithEmptyBody
    while (skipField(nextTag()));
  }

  /** Returns true when END_GROUP tag found. */
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

  private void skip(long count) throws IOException {
    pos += count;
    source.skip(count);
  }
}
