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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Maps {@link Extension} keys to their values.
 *
 * @param <T> the type of the containing {@link ExtendableMessage}
 */
final class ExtensionMap<T extends ExtendableMessage<?>> {

  private final Map<Extension<T, ?>, Object> map = new TreeMap<Extension<T, ?>, Object>();

  /** Constructs an empty ExtensionMap. */
  public ExtensionMap() {
  }

  /** Constructs an ExtensionMap that is a copy of an existing ExtensionMap. */
  public ExtensionMap(ExtensionMap<T> other) {
    map.putAll(other.map);
  }

  /**
   * Returns a {@link List} of {@link Extension}s in this map in tag order.
   */
  public List<Extension<T, ?>> getExtensions() {
    return Collections.unmodifiableList(new ArrayList<Extension<T, ?>>(map.keySet()));
  }

  /**
   * Returns the value associated with the given {@link Extension}, or null.
   *
   * @param <E> the (boxed) Java data type of the {@link Extension} value
   * @return a value of type E, or null
   */
  @SuppressWarnings("unchecked")
  public <E> E get(Extension<T, E> extension) {
    return (E) map.get(extension);
  }

  /**
   * Associates a value with the given {@link Extension}.
   *
   * @param value a non-null value of type E
   * @param <E> the (boxed) Java data type of the {@link Extension} value
   */
  public <E> void put(Extension<T, E> extension, E value) {
    map.put(extension, value);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ExtensionMap && map.equals(((ExtensionMap) other).map);
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    String sep = "";
    for (Map.Entry<? extends Extension<?, ?>, Object> entry : map.entrySet()) {
      sb.append(sep);
      sb.append(entry.getKey().getTag());
      sb.append("=");
      sb.append(entry.getValue());
      sep = ",";
    }
    sb.append("}");
    return sb.toString();
  }
}
