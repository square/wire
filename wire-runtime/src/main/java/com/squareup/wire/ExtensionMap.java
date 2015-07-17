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
import java.util.Arrays;

/** Maps {@link Extension} keys to their values. */
final class ExtensionMap<T extends ExtendableMessage<T>> {

  private static final int INITIAL_SIZE = 1;

  // Entries [0..(size - 1)] contain keys, entries [size..(2 * size - 1)] contain values.
  // This saves 12 bytes per instance over having separate arrays for keys and values.
  private Object[] data;
  private int size;

  public <E> ExtensionMap(Extension<T, ?, E> extension, E value) {
    data = new Object[2 * INITIAL_SIZE];
    data[0] = extension;
    data[1] = value;
    size = 1;
  }

  public ExtensionMap(ExtensionMap<T> other) {
    data = other.data.clone();
    size = other.size;
  }

  int serializedSize() {
    int size = 0;
    for (int i = 0; i < this.size; i++) {
      //noinspection unchecked
      Extension<T, ?, Object> extension = (Extension<T, ?, Object>) data[i];
      Object value = data[this.size + i];
      size += extension.serializedSize(value);
    }
    return size;
  }

  void write(ProtoWriter writer) throws IOException {
    for (int i = 0; i < size; i++) {
      //noinspection unchecked
      Extension<T, ?, Object> extension = (Extension<T, ?, Object>) data[i];
      Object value = data[size + i];
      extension.write(value, writer);
    }
  }

  public int size() {
    return size;
  }

  @SuppressWarnings("unchecked")
  public Extension<T, ?, Object> getExtension(int index) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException("" + index);
    }
    return (Extension<T, ?, Object>) data[index];
  }

  public Object getExtensionValue(int index) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException("" + index);
    }
    return data[size + index];
  }

  /**
   * Returns the value associated with the given {@link Extension}, or null.
   *
   * @param <E> the (boxed) Java data type of the {@link Extension} value
   * @return a value of type E, or null
   */
  @SuppressWarnings("unchecked")
  public <E> E get(Extension<T, ?, E> extension) {
    int index = Arrays.binarySearch(data, 0, size, extension);
    return index < 0 ? null : (E) data[size + index];
  }

  /**
   * Associates a value with the given {@link Extension}.
   *
   * @param value a non-null value of type E
   * @param <E> the (boxed) Java data type of the {@link Extension} value
   */
  public <E> void put(Extension<T, ?, E> extension, E value) {
    int index = Arrays.binarySearch(data, 0, size, extension);
    if (index >= 0) {
      data[size + index] = value;
    } else {
      insert(extension, value, -(index + 1));
    }
  }

  private <E> void insert(Extension<T, ?, E> key, E value, int insertionPoint) {
    // Grow the array and copy over the initial segment if necessary.
    Object[] dest = data;
    if (data.length < 2 * (size + 1)) {
      dest = new Object[2 * data.length];
      System.arraycopy(data, 0, dest, 0, insertionPoint);
    }

    // Make room for the new key and value.
    if (insertionPoint < size) {
      // Insert within the existing key section:
      //
      // K K K K K V V V V V       K = key, V = value
      // | | | | | | |  \ \ \
      // | |  \ \ \ \ \  \ \ \
      // K K * K K K V V * V V V   * = inserted key or value

      // Slide the rightmost values over by 2 slots.
      System.arraycopy(data, size + insertionPoint, dest, size + insertionPoint + 2,
          size - insertionPoint);
      // Slide the middle section containing the rightmost keys and leftmost values over by 1 slot.
      System.arraycopy(data, insertionPoint, dest, insertionPoint + 1, size);
    } else {
      // Insert immediately after the existing key section:
      //
      // K K K K K V V V V V
      // | | | | |  \ \ \ \ \
      // | | | | |   \ \ \ \ \
      // K K K K K * V V V V V *

      // Slide all values over by 1 slot.
      System.arraycopy(data, size, dest, size + 1, size);
    }

    size++;
    data = dest;

    data[insertionPoint] = key;
    data[size + insertionPoint] = value;
  }

  @Override public boolean equals(Object o) {
    if (!(o instanceof ExtensionMap<?>)) {
      return false;
    }
    ExtensionMap<?> other = (ExtensionMap<?>) o;
    if (size != other.size) {
      return false;
    }
    for (int i = 0; i < 2 * size; i++) {
      if (!data[i].equals(other.data[i])) {
        return false;
      }
    }
    return true;
  }

  @Override public int hashCode() {
    int result = 0;
    for (int i = 0; i < 2 * size; i++) {
      result = result * 37 + data[i].hashCode();
    }
    return result;
  }

  @Override public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    String sep = "";
    for (int i = 0; i < size; i++) {
      sb.append(sep);
      sb.append(((Extension<?, ?, ?>) data[i]).tag);
      sb.append("=");
      sb.append(data[size + i]);
      sep = ", ";
    }
    sb.append("}");
    return sb.toString();
  }
}
