/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.wire.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Util {
  private Util() {
  }

  public static <T> T checkNotNull(T value, String name) {
    if (value == null) {
      throw new NullPointerException(name + " == null");
    }
    return value;
  }

  public static void checkState(boolean assertion) {
    if (!assertion) {
      throw new IllegalStateException();
    }
  }

  public static <T> List<T> concatenate(List<T> a, T b) {
    List<T> result = new ArrayList<T>();
    result.addAll(a);
    result.add(b);
    return result;
  }

  public static WireOption findOption(List<WireOption> options, String name) {
    checkNotNull(options, "options");
    checkNotNull(name, "name");

    WireOption found = null;
    for (WireOption option : options) {
      if (option.name().equals(name)) {
        if (found != null) {
          throw new IllegalStateException("Multiple options match name: " + name);
        }
        found = option;
      }
    }
    return found;
  }

  public static boolean equal(Object a, Object b) {
    return a == b || (a != null && a.equals(b));
  }

  /** Returns an immutable copy of {@code list}. */
  public static <T> List<T> immutableList(Collection<T> list) {
    return Collections.unmodifiableList(new ArrayList<T>(list));
  }

  public static <K, V> Map<K, V> immutableMap(Map<K, V> map) {
    return Collections.unmodifiableMap(new LinkedHashMap<K, V>(map));
  }
}
