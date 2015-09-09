/*
 * Copyright 2015 Square Inc.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import okio.Buffer;

import static com.squareup.wire.ProtoWriter.varint32Size;

/**
 * A collection of tagged, typed, values. The tag and type may be known or unknown.
 * <ul>
 *   <li><strong>Known values</strong> have a name and application-layer type that are specified
 *       when a value is inserted. The tag and type are inserted using a user-provided {@link
 *       Extension} instance that describes the value. Types of known values include messages,
 *       enums, uint32s, sint32s, doubles, int64s, fixed64s strings, and bools.
 *   <li><strong>Unknown values</strong> know just their tag and field encoding. This occurs when
 *       a message field or extension field is unknown when the value is inserted. Types of unknown
 *       values are limited to the four field encodings: uint64, fixed32, fixed64 and bytes.
 * </ul>
 *
 * <p>It’s possible for us to have an unknown value and for the application to request it as a
 * known type. In this case we will encode the unknown value back to bytes, and then decode it
 * into the expected type. For example, we may convert a uint64 value '3' into the enum 'WEST'.
 *
 * <p>Lists are flattened, which means that tags are not-unique in the tags array, and that lookups
 * will create lists when necessary.
 *
 * <p>Tags are stored in the order that they were added. Values that are semantically equal may be
 * unequal according to this class because this implementation doesn't implement a strong order-
 * independent equals. This is unlikely to be a problem in practice because encoders typically use a
 * predictable encoding order.
 */
final class TagMap {
  private int size = 0;

  /** Known and unknown extensions. Defined only for [0..size). */
  private Extension<?, ?>[] extensions;

  /** Parallel to extensions, this array contains single-element values. */
  private Object[] values = new Object[8];

  public TagMap() {
    this.extensions = new Extension<?, ?>[8];
    this.values = new Object[8];
  }

  public TagMap(TagMap copyFrom) {
    this.size = copyFrom.size;
    this.extensions = copyFrom.extensions.clone();
    this.values = copyFrom.values.clone();
  }

  public <T> void add(int tag, FieldEncoding fieldEncoding, T value) {
    add(Extension.unknown(Message.class, tag, fieldEncoding), value);
  }

  public void add(Extension<?, ?> extension, Object value) {
    if (size == extensions.length) {
      int newCapacity = extensions.length * 2;
      Extension<?, ?>[] newExtensions = new Extension<?, ?>[newCapacity];
      Object[] newValues = new Object[newCapacity];
      System.arraycopy(extensions, 0, newExtensions, 0, size);
      System.arraycopy(values, 0, newValues, 0, size);
      extensions = newExtensions;
      values = newValues;
    }
    extensions[size] = extension;
    values[size] = value;
    size++;
  }

  /** Remove all elements in this map tagged {@code tag}. */
  public void removeAll(int tag) {
    for (int i = 0; i < size; i++) {
      int runStart = i;

      // When we encounter a value to remove, attempt to remove an entire run of values that share a
      // tag. This optimization is particularly useful because lists are flattened.
      while (i < size && extensions[i].getTag() == tag) i++;

      // Shift elements after the deleted run to the left.
      if (i > runStart) {
        System.arraycopy(extensions, i, extensions, runStart, size - i);
        System.arraycopy(values, i, values, runStart, size - i);
        Arrays.fill(extensions, size - (i - runStart), size, null);
        Arrays.fill(values, size - (i - runStart), size, null);
        size -= (i - runStart);
        i = runStart;
      }
    }
  }

  int size() {
    return size;
  }

  public int encodedSize() {
    int result = 0;
    for (int i = 0; i < size;) {
      Extension<?, ?> extension = extensions[i];
      WireAdapter<Object> adapter = adapter(extension);

      if (extension.getLabel().isPacked()) {
        int runSize = 0;
        int runEnd = runEnd(i);
        for (int j = i; j < runEnd; j++) {
          runSize += adapter.encodedSize(values[j]);
        }
        result += ProtoWriter.tagSize(extension.getTag());
        result += varint32Size(runSize);
        result += runSize;
        i = runEnd;
      } else {
        result += adapter.encodedSize(extension.getTag(), values[i]);
        i++;
      }
    }
    return result;
  }

  public void encode(ProtoWriter output) throws IOException {
    for (int i = 0; i < size;) {
      Extension<?, ?> extension = extensions[i];
      WireAdapter<Object> adapter = adapter(extension);
      if (extension.getLabel().isPacked()) {
        int runEnd = runEnd(i);
        int runSize = 0;
        for (int j = i; j < runEnd; j++) {
          runSize += adapter.encodedSize(values[j]);
        }
        output.writeTag(extension.getTag(), FieldEncoding.LENGTH_DELIMITED);
        output.writeVarint32(runSize);
        for (int j = i; j < runEnd; j++) {
          adapter.encode(output, values[j]);
        }
        i = runEnd;
      } else {
        adapter.encodeTagged(output, extension.getTag(), values[i]);
        i++;
      }
    }
  }

  /**
   * Returns the first index after {@code runStart} that has a different tag or datatype. This is
   * useful to write a run of packed values with a single tag.
   */
  private int runEnd(int runStart) {
    Extension<?, ?> extension = extensions[runStart];
    int runEnd = runStart + 1;
    while (runEnd < size
        && extensions[runEnd].getTag() == extension.getTag()
        && extensions[runEnd].getType().equals(extension.getType())) {
      runEnd++;
    }
    return runEnd;
  }

  public Object get(Extension<?, ?> targetExtension) {
    List<Object> list = new ArrayList<>();

    for (int i = 0; i < size; i++) {
      Extension<?, ?> sourceExtension = extensions[i];
      if (sourceExtension.getTag() == targetExtension.getTag()) {
        transcode(list, sourceExtension, values[i], targetExtension);
      }
    }

    if (targetExtension.getLabel().isRepeated()) {
      return list;
    } else if (list.isEmpty()) {
      return null;
    } else if (list.size() == 1) {
      return list.get(0);
    } else {
      throw new IllegalArgumentException(
          "found multiple values for non-repeated extension " + targetExtension);
    }
  }

  private void transcode(List<Object> list, Extension<?, ?> sourceExtension,
      Object value, Extension<?, ?> targetExtension) {
    // If the adapter we're expecting has already been applied, we're done.
    if (sourceExtension.getType().equals(targetExtension.getType())) {
      list.add(value);
      return;
    }

    try {
      // Encode one source value to buffer.
      Buffer buffer = new Buffer();
      adapter(sourceExtension).encodeTagged(new ProtoWriter(buffer), 1, value);

      // Read zero or more target values from buffer.
      ProtoReader reader = new ProtoReader(buffer);
      long token = reader.beginMessage();
      while (reader.nextTag() != -1) {
        try {
          list.add(adapter(targetExtension).decode(reader));
        } catch (RuntimeEnumAdapter.EnumConstantNotFoundException e) {
          list.add(e.value);
        }
      }
      reader.endMessage(token);
    } catch (IOException e) {
      throw new IllegalStateException("failed to transcode " + value + " to "
          + targetExtension, e);
    }
  }

  /** Returns a set of the unique, known extensions in use by this map. */
  public Set<Extension<?, ?>> extensions(boolean includeUnknown) {
    Set<Extension<?, ?>> result = new LinkedHashSet<>();
    for (int i = 0; i < size; i++) {
      if (includeUnknown || !extensions[i].isUnknown()) result.add(extensions[i]);
    }
    return Collections.unmodifiableSet(result);
  }

  /** Returns a copy of this tag map with redacted and unknown fields removed. */
  public TagMap redact() {
    TagMap result = new TagMap();
    for (int i = 0; i < size; i++) {
      if (extensions[i].isUnknown()) continue;

      Object redactedValue = adapter(extensions[i]).redact(values[i]);
      if (redactedValue == null) continue; // This value was redacted completely.

      result.add(extensions[i], redactedValue);
    }
    return result;
  }

  /** Returns the first value tagged {@code tag}, or null if there is no such value. */
  Object firstWithTag(int tag) {
    for (int i = 0; i < size; i++) {
      if (extensions[i].getTag() != tag) continue;
      return values[i];
    }
    return null;
  }

  @SuppressWarnings("unchecked") // Caller beware! Assumes the extension and value match at runtime.
  private WireAdapter<Object> adapter(Extension<?, ?> extension) {
    return (WireAdapter<Object>) WireAdapter.get(Message.WIRE, extension.getType(),
        extension.getMessageType(), extension.getEnumType());
  }

  @Override public boolean equals(Object o) {
    return o instanceof TagMap
        && Arrays.equals(((TagMap) o).extensions, extensions)
        && Arrays.equals(((TagMap) o).values, values);
  }

  @Override public int hashCode() {
    return Arrays.hashCode(extensions) + 37 * Arrays.hashCode(values);
  }
}
