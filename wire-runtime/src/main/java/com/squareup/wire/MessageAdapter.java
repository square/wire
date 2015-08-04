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

public abstract class MessageAdapter<M extends Message> extends TypeAdapter<M> {
  protected MessageAdapter() {
    super(WireType.LENGTH_DELIMITED);
  }

  /**
   * Returns a copy of {@code value} with all redacted fields set to null or an empty list.
   * This operation is recursive: nested messages are themselves redacted in the returned object.
   */
  public abstract M redact(M value);

  /** Returns a human-readable version of the given {@code value}. */
  abstract String toString(M value);

  /** Encode {@code value} as a {@code byte[]}. */
  public final byte[] writeBytes(M value) {
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
  public final void writeStream(M value, OutputStream stream) throws IOException {
    checkNotNull(value, "value == null");
    checkNotNull(stream, "stream == null");
    BufferedSink buffer = Okio.buffer(Okio.sink(stream));
    write(value, buffer);
    buffer.emit();
  }

  /** Encode {@code value} and write it to {@code stream}. */
  public final void write(M value, BufferedSink sink) throws IOException {
    checkNotNull(value, "value == null");
    checkNotNull(sink, "sink == null");
    write(value, new ProtoWriter(sink));
  }

  /** Read an encoded message from {@code source}. */
  public final M read(BufferedSource source) throws IOException {
    checkNotNull(source, "source == null");
    return read(new ProtoReader(source));
  }

  /** Read an encoded message from {@code bytes}. */
  public final M readBytes(byte[] bytes) throws IOException {
    checkNotNull(bytes, "bytes == null");
    return read(new Buffer().write(bytes));
  }

  /** Read an encoded message from {@code stream}. */
  public final M readStream(InputStream stream) throws IOException {
    checkNotNull(stream, "stream == null");
    return read(Okio.buffer(Okio.source(stream)));
  }
}
