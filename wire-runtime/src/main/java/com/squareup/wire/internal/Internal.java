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
package com.squareup.wire.internal;

import com.squareup.wire.ProtoAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Methods for generated code use only. Not subject to public API rules. */
public final class Internal {
  private Internal() {
  }

  public static <T> List<T> newMutableList() {
    return new MutableOnWriteList<>(Collections.<T>emptyList());
  }

  public static <T> List<T> copyOf(String name, List<T> list) {
    if (list == null) throw new NullPointerException(name + " == null");
    if (list == Collections.emptyList() || list instanceof ImmutableList) {
      return new MutableOnWriteList<>(list);
    }
    return new ArrayList<>(list);
  }

  public static <T> List<T> immutableCopyOf(String name, List<T> list) {
    if (list == null) throw new NullPointerException(name + " == null");
    if (list instanceof MutableOnWriteList) {
      list = ((MutableOnWriteList<T>) list).mutableList;
    }
    if (list == Collections.emptyList() || list instanceof ImmutableList) {
      return list;
    }
    ImmutableList<T> result = new ImmutableList<>(list);
    // Check after the list has been copied to defend against races.
    if (result.contains(null)) {
      throw new IllegalArgumentException(name + ".contains(null)");
    }
    return result;
  }

  public static <T> void redactElements(List<T> list, ProtoAdapter<T> adapter) {
    for (int i = 0, count = list.size(); i < count; i++) {
      list.set(i, adapter.redact(list.get(i)));
    }
  }

  public static boolean equals(Object a, Object b) {
    return a == b || (a != null && a.equals(b));
  }

  /**
   * Create an exception for missing required fields.
   *
   * @param args Alternating field value and field name pairs.
   */
  public static IllegalStateException missingRequiredFields(Object... args) {
    StringBuilder sb = new StringBuilder();
    String plural = "";
    for (int i = 0, size = args.length; i < size; i += 2) {
      if (args[i] == null) {
        if (sb.length() > 0) {
          plural = "s"; // Found more than one missing field
        }
        sb.append("\n  ");
        sb.append(args[i + 1]);
      }
    }
    throw new IllegalStateException("Required field" + plural + " not set:" + sb);
  }

  /** Throw {@link NullPointerException} if {@code list} or one of its items are null. */
  public static void checkElementsNotNull(List<?> list) {
    if (list == null) throw new NullPointerException("list == null");
    for (int i = 0, size = list.size(); i < size; i++) {
      Object element = list.get(i);
      if (element == null) {
        throw new NullPointerException("Element at index " + i + " is null");
      }
    }
  }

  /** Returns the number of non-null values in {@code a, b}. */
  public static int countNonNull(Object a, Object b) {
    return (a != null ? 1 : 0)
        + (b != null ? 1 : 0);
  }

  /** Returns the number of non-null values in {@code a, b, c}. */
  public static int countNonNull(Object a, Object b, Object c) {
    return (a != null ? 1 : 0)
        + (b != null ? 1 : 0)
        + (c != null ? 1 : 0);
  }

  /** Returns the number of non-null values in {@code a, b, c, d, rest}. */
  public static int countNonNull(Object a, Object b, Object c, Object d, Object... rest) {
    int result = 0;
    if (a != null) result++;
    if (b != null) result++;
    if (c != null) result++;
    if (d != null) result++;
    for (Object o : rest) {
      if (o != null) result++;
    }
    return result;
  }
}
