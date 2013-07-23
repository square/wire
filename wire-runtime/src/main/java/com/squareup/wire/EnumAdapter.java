// Copyright 2013 Square, Inc.
package com.squareup.wire;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * An adapter than can convert values of a given Enum to and from integers, based on
 * {@link ProtoEnum} annotations.
 *
 * @param <E> the Enum class handled by this adapter.
 */
class EnumAdapter<E extends Enum> {
  private final Map<Integer, E> fromInt = new HashMap<Integer, E>();
  private final Map<E, Integer> toInt = new HashMap<E, Integer>();

  EnumAdapter(Class<E> type) {
    // Record values for each constant annotated with '@ProtoEnum'.
    for (E value : type.getEnumConstants()) {
      try {
        Field f = type.getField(value.name());
        if (f.isAnnotationPresent(ProtoEnum.class)) {
          ProtoEnum annotation = f.getAnnotation(ProtoEnum.class);
          int tag = annotation.value();
          fromInt.put(tag, value);
          toInt.put(value, tag);
        }
      } catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public int toInt(E e) {
    return toInt.get(e);
  }

  public E fromInt(int value) {
    return fromInt.get(value);
  }
}
