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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * An immutable sequence of bytes.
 *
 * <p><strong>Full disclosure:</strong> this class provides untrusted input and
 * output streams with raw access to the underlying byte array. A hostile
 * stream implementation could keep a reference to the mutable byte string,
 * violating the immutable guarantee of this class. For this reason a byte
 * string's immutability guarantee cannot be relied upon for security in applets
 * and other environments that run both trusted and untrusted code in the same
 * process.
 */
public final class ByteString {
  private final byte[] data;
  private transient int hashCode; // Lazily computed; 0 if unknown.

  /**
   * A singleton empty {@code ByteString}.
   */
  public static final ByteString EMPTY = ByteString.of();

  /** Returns a new byte string containing the bytes of {@code data}. */
  public static ByteString of(byte... data) {
    return new ByteString(data.clone());
  }

  /**
   * Returns a new byte string containing the bytes of {@code data}, interpreted
   * as Base64.
   */
  public static ByteString of(String data) {
    return new ByteString(Stringer.decode(data));
  }

  /**
   * Reads {@code count} bytes from {@code in} and returns the result.
   *
   * @throws EOFException if {@code in} has fewer than {@code count} bytes to
   *     read.
   */
  public static ByteString read(InputStream in, int count) throws IOException {
    byte[] result = new byte[count];
    for (int c = 0; c < count;) {
      int read = in.read(result, c, count - c);
      if (read == -1) throw new EOFException("Expected " + count + "; received " + c);
      c += read;
    }
    return new ByteString(result);
  }

  private ByteString(byte[] data) {
    this.data = data; // Trusted internal constructor doesn't clone data.
  }

  public byte byteAt(int index) {
    return data[index];
  }

  /**
   * Returns the number of bytes in this ByteString.
   */
  public int size() {
    return data.length;
  }

  /**
   * Returns a byte array containing a copy of the bytes in this {@code ByteString}.
   */
  public byte[] toByteArray() {
    return data.clone();
  }

  /** Writes the contents of this byte string to {@code out}. */
  public void write(OutputStream out) throws IOException {
    out.write(data);
  }

  /** Writes a subsequence of this byte string to {@code out}. */
  public void write(OutputStream out, int offset, int count) throws IOException {
    out.write(data, offset, count);
  }

  @Override public boolean equals(Object o) {
    return o == this
        || o instanceof ByteString && Arrays.equals(((ByteString) o).data, data);
  }

  @Override public int hashCode() {
    int result = hashCode;
    return result != 0 ? result : (hashCode = Arrays.hashCode(data));
  }

  /**
   * Returns a string containing the contents of this ByteString in Base64 format.
   */
  @Override public String toString() {
    return Stringer.encode(data);
  }
}
