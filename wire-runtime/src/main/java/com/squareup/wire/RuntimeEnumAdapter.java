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
import java.util.Comparator;

/**
 * Converts values of an enum to and from integers.
 */
final class RuntimeEnumAdapter<E extends WireEnum> extends ProtoAdapter<E> {
  private static final Comparator<WireEnum> COMPARATOR = new Comparator<WireEnum>() {
    @Override public int compare(WireEnum o1, WireEnum o2) {
      return o1.getValue() - o2.getValue();
    }
  };

  private final Class<E> type;

  private final int[] values;
  private final E[] constants;
  private final boolean isDense;

  RuntimeEnumAdapter(Class<E> type) {
    super(FieldEncoding.VARINT, type);
    this.type = type;

    constants = type.getEnumConstants();
    Arrays.sort(constants, COMPARATOR);

    int length = constants.length;
    if (constants[0].getValue() == 1 && constants[length - 1].getValue() == length) {
      // Values completely fill the range from 1..length
      isDense = true;
      values = null;
    } else {
      isDense = false;
      values = new int[length];
      for (int i = 0; i < length; i++) {
        values[i] = constants[i].getValue();
      }
    }
  }

  public E fromInt(int value) {
    int index = isDense ? value - 1 : Arrays.binarySearch(values, value);
    try {
      return constants[index];
    } catch (IndexOutOfBoundsException e) {
      throw new EnumConstantNotFoundException(value, type);
    }
  }

  @Override public int encodedSize(E value) {
    return ProtoWriter.varint32Size(value.getValue());
  }

  @Override public void encode(ProtoWriter writer, E value) throws IOException {
    writer.writeVarint32(value.getValue());
  }

  @Override public E decode(ProtoReader reader) throws IOException {
    return fromInt(reader.readVarint32());
  }

  @Override public boolean equals(Object o) {
    return o instanceof RuntimeEnumAdapter
        && ((RuntimeEnumAdapter) o).type == type;
  }

  @Override public int hashCode() {
    return type.hashCode();
  }

  static final class EnumConstantNotFoundException extends IllegalArgumentException {
    final int value;

    EnumConstantNotFoundException(int value, Class<?> type) {
      super("Unknown enum tag " + value + " for " + type.getCanonicalName());
      this.value = value;
    }
  }
}
