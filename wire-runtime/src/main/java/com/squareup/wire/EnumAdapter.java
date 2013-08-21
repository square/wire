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

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts values of an enum to and from integers using {@link ProtoEnum}
 * annotations.
 */
final class EnumAdapter<E extends Enum> {
  private final Map<Integer, E> fromInt = new LinkedHashMap<Integer, E>();
  private final Map<E, Integer> toInt = new LinkedHashMap<E, Integer>();

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
