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
 * <p>Lists are flattened, which means that tags are not-unique in the tags array. Lookups will
 * create lists when necessary.
 *
 * <p>Tags are stored in the order that they were added. Values that are semantically equal may be
 * unequal according to this class because this implementation doesn't implement a strong order-
 * independent equals. This is unlikely to be a problem in practice because encoders typically use a
 * predictable encoding order.
 *
 * <p>Instances of this class are immutable.
 */
public final class TagMap {
  /**
   * Alternating extensions and values. Extensions are both known and unknown extensions. Values
   * are single elements. Extensions with multiple elements occur multiple times in this array.
   */
  private final Object[] array;

  private TagMap(Builder builder) {
    this.array = new Object[builder.limit];
    System.arraycopy(builder.array, 0, array, 0, builder.limit);
  }

  /**
   * Returns the number of values in this tag map. The returned value counts each element in a
   * repeated tag. For example, a tag map containing {@code {a = 1, a = 2, b = true}} returns 3.
   */
  public int size() {
    return array.length / 2;
  }

  /** Returns the number of bytes required to encode this tag map. */
  public int encodedSize() {
    int result = 0;
    for (int i = 0; i < array.length;) {
      Extension<?, ?> extension = (Extension<?, ?>) array[i];
      ProtoAdapter<Object> adapter = adapter(extension);

      if (extension.getLabel().isPacked()) {
        int runSize = 0;
        int runEnd = runEnd(i);
        for (int j = i; j < runEnd; j += 2) {
          runSize += adapter.encodedSize(array[j + 1]);
        }
        result += ProtoWriter.tagSize(extension.getTag());
        result += varint32Size(runSize);
        result += runSize;
        i = runEnd;
      } else {
        result += adapter.encodedSize(extension.getTag(), array[i + 1]);
        i += 2;
      }
    }
    return result;
  }

  /** Writes tags and values to {@code writer}. */
  public void encode(ProtoWriter writer) throws IOException {
    for (int i = 0; i < array.length;) {
      Extension<?, ?> extension = (Extension<?, ?>) array[i];
      ProtoAdapter<Object> adapter = adapter(extension);
      if (extension.getLabel().isPacked()) {
        int runEnd = runEnd(i);
        int runSize = 0;
        for (int j = i; j < runEnd; j += 2) {
          runSize += adapter.encodedSize(array[j + 1]);
        }
        writer.writeTag(extension.getTag(), FieldEncoding.LENGTH_DELIMITED);
        writer.writeVarint32(runSize);
        for (int j = i; j < runEnd; j += 2) {
          adapter.encode(writer, array[j + 1]);
        }
        i = runEnd;
      } else {
        adapter.encodeTagged(writer, extension.getTag(), array[i + 1]);
        i += 2;
      }
    }
  }

  /**
   * Returns the first index after {@code runStart} that has a different tag or protoType. This is
   * useful to write a run of packed values with a single tag.
   */
  private int runEnd(int runStart) {
    Extension<?, ?> extension = (Extension<?, ?>) array[runStart];
    int runEnd = runStart + 2;
    while (runEnd < array.length
        && ((Extension<?, ?>) array[runEnd]).getTag() == extension.getTag()
        && ((Extension<?, ?>) array[runEnd]).getAdapter().equals(extension.getAdapter())) {
      runEnd += 2;
    }
    return runEnd;
  }

  /**
   * Returns this map's value for {@code extension}, or null if it has no such value. If {@code
   * extension} is repeated, this returns an empty list for no elements (instead of null).
   */
  public Object get(Extension<?, ?> extension) {
    return get(array, array.length, extension);
  }

  static Object get(Object[] array, int limit, Extension<?, ?> targetExtension) {
    List<Object> list = new ArrayList<>();

    for (int i = 0; i < limit; i += 2) {
      Extension<?, ?> sourceExtension = (Extension<?, ?>) array[i];
      if (sourceExtension.getTag() == targetExtension.getTag()) {
        transcode(list, sourceExtension, array[i + 1], targetExtension);
      }
    }

    if (targetExtension.getLabel().isRepeated()) {
      return Collections.unmodifiableList(list);
    } else if (list.isEmpty()) {
      return null;
    } else if (list.size() == 1) {
      return list.get(0);
    } else {
      throw new IllegalArgumentException(
          "found multiple values for non-repeated extension " + targetExtension);
    }
  }

  static void transcode(List<Object> list, Extension<?, ?> sourceExtension,
      Object value, Extension<?, ?> targetExtension) {
    // If the adapter we're expecting has already been applied, we're done.
    if (sourceExtension.getAdapter().equals(targetExtension.getAdapter())) {
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

  private static ProtoAdapter<Object> adapter(Extension<?, ?> sourceExtension) {
    return (ProtoAdapter<Object>) sourceExtension.getAdapter();
  }

  /**
   * Returns a set of the unique, known extensions in use by this map.
   *
   * @param includeUnknown true to include {@link Extension#isUnknown() unknown extensions}.
   */
  public Set<Extension<?, ?>> extensions(boolean includeUnknown) {
    Set<Extension<?, ?>> result = new LinkedHashSet<>();
    for (int i = 0; i < array.length; i += 2) {
      Extension<?, ?> extension = (Extension<?, ?>) array[i];
      if (includeUnknown || !extension.isUnknown()) {
        result.add(extension);
      }
    }
    return Collections.unmodifiableSet(result);
  }

  @Override public boolean equals(Object o) {
    return o instanceof TagMap
        && Arrays.equals(((TagMap) o).array, array);
  }

  @Override public int hashCode() {
    return Arrays.hashCode(array);
  }

  public static final class Builder {
    static final int INITIAL_CAPACITY = 8;

    private Object[] array;
    private int limit = 0;

    public Builder() {
      array = new Object[INITIAL_CAPACITY];
    }

    public Builder(TagMap tagMap) {
      this.array = tagMap.array.clone();
      this.limit = tagMap.array.length;
    }

    public Builder add(Extension<?, ?> extension, Object value) {
      if (extension == null) throw new NullPointerException("extension == null");
      if (value == null) throw new NullPointerException("value == null");
      if (limit == array.length) {
        int newLimit = Math.max(limit * 2, INITIAL_CAPACITY);
        Object[] newArray = new Object[newLimit];
        System.arraycopy(array, 0, newArray, 0, limit);
        array = newArray;
      }
      array[limit++] = extension;
      array[limit++] = value;
      return this;
    }

    public Object get(Extension<?, ?> extension) {
      return TagMap.get(array, limit, extension);
    }

    /** Remove all elements in this map tagged {@code tag}. */
    public Builder removeAll(int tag) {
      for (int i = 0; i < limit; i += 2) {
        int runStart = i;

        // When we encounter a value to remove, attempt to remove an entire run of values that share
        // a tag. This optimization is particularly useful because lists are flattened.
        while (i < limit && ((Extension<?, ?>) array[i]).getTag() == tag) i += 2;

        // Shift elements after the deleted run to the left.
        if (i > runStart) {
          System.arraycopy(array, i, array, runStart, limit - i);
          Arrays.fill(array, limit - (i - runStart), limit, null);
          limit -= (i - runStart);
          i = runStart;
        }
      }
      return this;
    }

    /** Removes all redacted fields from this builder. */
    public Builder redact() {
      Object[] oldArray = array.clone();
      int oldLimit = limit;

      Arrays.fill(array, 0, limit, null);
      limit = 0;

      for (int i = 0; i < oldLimit; i += 2) {
        Extension<?, ?> extension = (Extension<?, ?>) oldArray[i];
        if (extension.isUnknown()) continue;

        Object redactedValue = adapter(extension).redact(oldArray[i + 1]);
        if (redactedValue == null) continue; // This value was redacted completely.

        array[limit++] = extension;
        array[limit++] = redactedValue;
      }
      return this;
    }

    public TagMap build() {
      return new TagMap(this);
    }
  }
}
