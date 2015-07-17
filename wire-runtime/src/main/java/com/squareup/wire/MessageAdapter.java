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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import static com.squareup.wire.Preconditions.checkNotNull;

public final class MessageAdapter<M extends Message> {
  // Unicode character "Full Block" (U+2588)
  private static final String FULL_BLOCK = "█";

  private final ExtensionRegistry extensionRegistry;
  private final TypeAdapter<M> adapter;

  /** Cache information about the Message class and its mapping to proto wire format. */
  MessageAdapter(ExtensionRegistry extensionRegistry, Class<M> messageType) {
    this.extensionRegistry = extensionRegistry;
    try {
      //noinspection unchecked
      this.adapter = (TypeAdapter<M>) messageType.getField("ADAPTER").get(null);
    } catch (NoSuchFieldException e) {
      throw new IllegalStateException(e);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Encode {@code value} as a {@code byte[]}. */
  public byte[] writeBytes(M value) {
    checkNotNull(value, "value == null");
    Buffer buffer = new Buffer();
    try {
      write(value, buffer);
    } catch (IOException e) {
      throw new AssertionError(e); // No I/O writing to Buffer.
    }
    return buffer.readByteArray();
  }

  /** Encode {@code value} and write it to {@code stream}. */
  public void writeStream(M value, OutputStream stream) throws IOException {
    checkNotNull(value, "value == null");
    checkNotNull(stream, "stream == null");
    BufferedSink buffer = Okio.buffer(Okio.sink(stream));
    write(value, buffer);
    buffer.emit();
  }

  /** Encode {@code value} and write it to {@code stream}. */
  public void write(M value, BufferedSink sink) throws IOException {
    checkNotNull(value, "value == null");
    checkNotNull(sink, "sink == null");
    adapter.write(value, new ProtoWriter(sink));
  }

  /** Read an encoded message from {@code bytes}. */
  public M readBytes(byte[] bytes) throws IOException {
    checkNotNull(bytes, "bytes == null");
    return read(new Buffer().write(bytes));
  }

  /** Read an encoded message from {@code stream}. */
  public M readStream(InputStream stream) throws IOException {
    checkNotNull(stream, "stream == null");
    return read(Okio.buffer(Okio.source(stream)));
  }

  /** Read an encoded message from {@code source}. */
  public M read(BufferedSource source) throws IOException {
    checkNotNull(source, "source == null");
    return adapter.read(new ProtoReader(source, extensionRegistry));
  }
}
