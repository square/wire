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
 * This collection keeps a set of tags, values for those tags, and encoders for those values.
 * Each value is decoded either with a <strong>known adapter</strong> or with a <strong>raw
 * adapter</strong>. For example, if an extension is known at the time of decoding, the proper
 * decoded value model is decoded and stored in this map. On the other hand if the extension is
 * unknown, only a byte string is stored in this map. In either case a lookup for with extension key
 * will return the value model, decoding it from the byte string if necessary. This uses the wire
 * adapters from {@link FieldEncoding#rawWireAdapter()} for unknown types.
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

  /** Tags. Defined only for [0..size). */
  private int[] tags = new int[8];

  /** Parallel to tags, this array contains single-element adapters. */
  private WireAdapter<?>[] adapters = new WireAdapter<?>[8];

  /** Parallel to tags, this array contains single-element values. */
  private Object[] values = new Object[8];

  public <T> void add(int tag, WireAdapter<T> singleAdapter, T value) {
    if (size == tags.length) {
      int newCapacity = tags.length * 2;
      int[] newTags = new int[newCapacity];
      WireAdapter<?>[] newAdapters = new WireAdapter<?>[newCapacity];
      Object[] newValues = new Object[newCapacity];
      System.arraycopy(tags, 0, newTags, 0, size);
      System.arraycopy(adapters, 0, newAdapters, 0, size);
      System.arraycopy(values, 0, newValues, 0, size);
      tags = newTags;
      adapters = newAdapters;
      values = newValues;
    }
    tags[size] = tag;
    adapters[size] = singleAdapter;
    values[size] = value;
    size++;
  }

  public int encodedSize() {
    int result = 0;
    for (int i = 0; i < size; i++) {
      WireAdapter<Object> adapter = (WireAdapter<Object>) adapters[i];
      result += adapter.encodedSize(tags[i], values[i]);
    }
    return result;
  }

  public void encode(ProtoWriter output) throws IOException {
    for (int i = 0; i < size; i++) {
      WireAdapter<Object> adapter = (WireAdapter<Object>) adapters[i];
      adapter.encodeTagged(output, tags[i], values[i]);
    }
  }

  public Object get(Extension<?, ?> extension) {
    List<Object> list = extension.getLabel().isRepeated()
        ? new ArrayList<Object>()
        : null;

    for (int i = 0; i < size; i++) {
      if (tags[i] != extension.getTag()) continue;
      Object value = decode(extension, (WireAdapter<Object>) adapters[i], values[i]);
      if (list == null) return value;
      list.add(value);
    }

    return list;
  }

  private <T> Object decode(Extension<?, ?> extension, WireAdapter<T> valueAdapter, T value) {
    WireAdapter<?> resultAdapter = WireAdapter.get(Message.WIRE, extension.getDatatype(),
        extension.getMessageType(), extension.getEnumType());

    // If the adapter we're expecting has already been applied, we're done.
    if (valueAdapter.equals(resultAdapter)) return value;

    // We need to encode the value (like a ByteString) so we can decode it as the requested type.
    try {
      Buffer buffer = new Buffer();
      valueAdapter.encodeTagged(new ProtoWriter(buffer), 1, value);
      ProtoReader reader = new ProtoReader(buffer);
      reader.beginMessage();
      reader.nextTag();
      return resultAdapter.decode(reader);
    } catch (RuntimeEnumAdapter.EnumConstantNotFoundException e) {
      return e.value;
    } catch (IOException e) {
      throw new IllegalStateException("failed to decode extension " + extension, e);
    }
  }

  @Override public boolean equals(Object o) {
    return o instanceof NewTagMap
        && Arrays.equals(((NewTagMap) o).tags, tags)
        && Arrays.equals(((NewTagMap) o).values, values);
  }

  @Override public int hashCode() {
    return Arrays.hashCode(tags) + 37 * Arrays.hashCode(values);
  }
}
