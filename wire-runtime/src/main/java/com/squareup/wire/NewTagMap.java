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
import java.util.List;
import okio.Buffer;

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
final class NewTagMap {
  private int size = 0;

  /** Known and unknown extensions. Defined only for [0..size). */
  private Extension<?, ?>[] extensions = new Extension<?, ?>[8];

  /** Parallel to tags, this array contains single-element values. */
  private Object[] values = new Object[8];

  public <T> void add(int tag, FieldEncoding fieldEncoding, T value) {
    add(Extension.unknown(ExtendableMessage.class, tag, fieldEncoding), value);
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

  public int encodedSize() {
    int result = 0;
    for (int i = 0; i < size; i++) {
      result += adapter(extensions[i]).encodedSize(extensions[i].getTag(), values[i]);
    }
    return result;
  }

  public void encode(ProtoWriter output) throws IOException {
    for (int i = 0; i < size; i++) {
      adapter(extensions[i]).encodeTagged(output, extensions[i].getTag(), values[i]);
    }
  }

  public Object get(Extension<?, ?> extension) {
    List<Object> list = extension.getLabel().isRepeated()
        ? new ArrayList<Object>()
        : null;

    for (int i = 0; i < size; i++) {
      if (extensions[i].getTag() != extension.getTag()) continue;
      Object value = decode(extension, extensions[i], values[i]);
      if (list == null) return value;
      list.add(value);
    }

    return list;
  }

  private Object decode(Extension<?, ?> target, Extension<?, ?> source, Object value) {
    // If the adapter we're expecting has already been applied, we're done.
    if (source.getDatatype() == target.getDatatype()) return value;

    // We need to encode the value (like a ByteString) so we can decode it as the requested type.
    try {
      Buffer buffer = new Buffer();
      adapter(source).encodeTagged(new ProtoWriter(buffer), 1, value);
      ProtoReader reader = new ProtoReader(buffer);
      reader.beginMessage();
      reader.nextTag();
      return adapter(target).decode(reader);
    } catch (RuntimeEnumAdapter.EnumConstantNotFoundException e) {
      return e.value;
    } catch (IOException e) {
      throw new IllegalStateException("failed to decode target " + target, e);
    }
  }

  @SuppressWarnings("unchecked") // Caller beware! Assumes the extension and value match at runtime.
  private WireAdapter<Object> adapter(Extension<?, ?> extension) {
    return (WireAdapter<Object>) WireAdapter.get(Message.WIRE, extension.getDatatype(),
        extension.getMessageType(), extension.getEnumType());
  }

  @Override public boolean equals(Object o) {
    return o instanceof NewTagMap
        && Arrays.equals(((NewTagMap) o).extensions, extensions)
        && Arrays.equals(((NewTagMap) o).values, values);
  }

  @Override public int hashCode() {
    return Arrays.hashCode(extensions) + 37 * Arrays.hashCode(values);
  }
}
