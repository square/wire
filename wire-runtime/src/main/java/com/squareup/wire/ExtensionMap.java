// Copyright 2013 Square, Inc.
package com.squareup.wire;

import java.util.Map;
import java.util.Set;
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
   * Returns a {@link Set} of {@link Extension}s in this map.
   */
  public Set<Extension<T, ?>> getExtensions() {
    return map.keySet();
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

  /**
   * Removes any previous value associated with the given {@link Extension}.
   */
  public void clear(Extension<T, ?> extension) {
    map.remove(extension);
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
