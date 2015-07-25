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

// Parts of this class are derived from the CodedOutputByteBuffer and WireFormatNano classes in
// Google's "Nano" Protocol Buffer implementation. The original copyright notice, list of
// conditions, and disclaimer for those classes is as follows:
//
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
import java.util.List;
import okio.BufferedSink;
import okio.ByteString;

import static com.squareup.wire.TypeAdapter.TYPE_LEN_DELIMITED;

/**
 * Utilities for encoding and writing protocol message fields.
 */
public final class ProtoWriter {

  /** Makes a tag value given a field number and wire type. */
  static int makeTag(int tag, int type) {
    return (tag << WireType.TAG_TYPE_BITS) | type;
  }

  /** Compute the number of bytes that would be needed to encode a tag. */
  static int tagSize(int tag) {
    return TypeAdapter.varint32Size(tag << WireType.TAG_TYPE_BITS);
  }

  private final BufferedSink sink;

  final Message.Visitor visitor = new Message.Visitor() {
    @Override
    public <T> void value(int tag, String name, T value, TypeAdapter<T> adapter, boolean redacted) {
      try {
        ProtoWriter.this.value(tag, value, adapter);
      } catch (IOException e) {
        throw new WriteIOException(e);
      }
    }

    @Override public <T> void repeated(int tag, String name, List<T> list, TypeAdapter<T> adapter,
        boolean redacted) {
      try {
        ProtoWriter.this.repeated(tag, list, adapter);
      } catch (IOException e) {
        throw new WriteIOException(e);
      }
    }

    @Override public <T> void packed(int tag, String name, List<T> list, TypeAdapter<T> adapter,
        boolean redacted) {
      try {
        ProtoWriter.this.packed(tag, list, adapter);
      } catch (IOException e) {
        throw new WriteIOException(e);
      }
    }

    @Override public void unknowns(Message<?> message) {
      UnknownFieldMap unknownFields = message.unknownFields;
      if (unknownFields != null) {
        try {
          unknownFields.write(ProtoWriter.this);
        } catch (IOException e) {
          throw new WriteIOException(e);
        }
      }
    }

    @Override public void extensions(ExtendableMessage<?> message) {
      ExtensionMap<?> extensionMap = message.extensionMap;
      if (extensionMap != null) {
        try {
          extensionMap.write(ProtoWriter.this);
        } catch (IOException e) {
          throw new WriteIOException(e);
        }
      }
    }
  };

  public ProtoWriter(BufferedSink sink) {
    this.sink = sink;
  }

  public <E> void value(int tag, E value, TypeAdapter<E> adapter) throws IOException {
    if (value == null) return;
    writeVarint32(adapter.makeTag(tag));
    if (adapter.type == TypeAdapter.TYPE_LEN_DELIMITED) {
      writeVarint32(adapter.serializedSize(value));
    }
    adapter.write(value, this);
  }

  public <E> void repeated(int tag, List<E> value, TypeAdapter<E> adapter)
      throws IOException {
    if (value.isEmpty()) return;
    boolean isLengthDelimited = adapter.type == TypeAdapter.TYPE_LEN_DELIMITED;
    int encodedTag = adapter.makeTag(tag);
    for (int i = 0, count = value.size(); i < count; i++) {
      writeVarint32(encodedTag);
      E item = value.get(i);
      if (isLengthDelimited) {
        writeVarint32(adapter.serializedSize(item));
      }
      adapter.write(item, this);
    }
  }

  public <E> void packed(int tag, List<E> value, TypeAdapter<E> adapter)
      throws IOException {
    if (value.isEmpty()) return;
    writeVarint32(makeTag(tag, TYPE_LEN_DELIMITED));
    int size = 0;
    for (int i = 0, count = value.size(); i < count; i++) {
      size += adapter.serializedSize(value.get(i));
    }
    writeVarint32(size);
    for (int i = 0, count = value.size(); i < count; i++) {
      adapter.write(value.get(i), this);
    }
  }

  void writeRawByte(int value) throws IOException {
    sink.writeByte(value);
  }

  void writeRawBytes(ByteString value) throws IOException {
    sink.write(value);
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
  void writeVarint32(int value) throws IOException {
    while ((value & ~0x7F) != 0) {
      sink.writeByte((value & 0x7F) | 0x80);
      value >>>= 7;
    }
    sink.writeByte(value);
  }

  /** Encode and write a varint. */
  void writeVarint64(long value) throws IOException {
    while ((value & ~0x7FL) != 0) {
      sink.writeByte(((int) value & 0x7F) | 0x80);
      value >>>= 7;
    }
    sink.writeByte((int) value);
  }

  /** Write a little-endian 32-bit integer. */
  void writeFixed32(int value) throws IOException {
    sink.writeIntLe(value);
  }

  /** Write a little-endian 64-bit integer. */
  void writeFixed64(long value) throws IOException {
    sink.writeLongLe(value);
  }
}
