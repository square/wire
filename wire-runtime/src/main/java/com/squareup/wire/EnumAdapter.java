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

import java.util.Arrays;
import java.util.Comparator;

/**
 * Converts values of an enum to and from integers.
 */
final class EnumAdapter<E extends ProtoEnum> {
  private static final Comparator<ProtoEnum> COMPARATOR = new Comparator<ProtoEnum>() {
    @Override public int compare(ProtoEnum o1, ProtoEnum o2) {
      return o1.getValue() - o2.getValue();
    }
  };

  private final Class<E> type;

  private final int[] values;
  private final E[] constants;
  private final boolean isDense;

  EnumAdapter(Class<E> type) {
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

  public int toInt(E e) {
    return e.getValue();
  }

  public E fromInt(int value) {
    int index = isDense ? value - 1 : Arrays.binarySearch(values, value);
    try {
      return constants[index];
    } catch (IndexOutOfBoundsException e) {
      throw new IllegalArgumentException(
          "Unknown enum tag " + value + " for " + type.getCanonicalName());
    }
  }
}
