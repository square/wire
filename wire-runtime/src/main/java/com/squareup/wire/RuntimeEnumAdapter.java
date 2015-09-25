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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Converts values of an enum to and from integers.
 */
final class RuntimeEnumAdapter<E extends WireEnum> extends ProtoAdapter<E> {
  private final Class<E> type;
  private final Method fromValueMethod;

  RuntimeEnumAdapter(Class<E> type) {
    super(FieldEncoding.VARINT, type);
    this.type = type;

    try {
      fromValueMethod = type.getDeclaredMethod("fromValue", int.class);
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }

  @Override public int encodedSize(E value) {
    return ProtoWriter.varint32Size(value.getValue());
  }

  @Override public void encode(ProtoWriter writer, E value) throws IOException {
    writer.writeVarint32(value.getValue());
  }

  @Override public E decode(ProtoReader reader) throws IOException {
    int value = reader.readVarint32();
    E constant;
    try {
      //noinspection unchecked
      constant = (E) fromValueMethod.invoke(null, value);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new AssertionError(e);
    }
    if (constant == null) {
      throw new EnumConstantNotFoundException(value, type);
    }
    return constant;
  }

  @Override public boolean equals(Object o) {
    return o instanceof RuntimeEnumAdapter
        && ((RuntimeEnumAdapter) o).type == type;
  }

  @Override public int hashCode() {
    return type.hashCode();
  }

  static final class EnumConstantNotFoundException extends IllegalArgumentException {
    final long value;

    EnumConstantNotFoundException(int value, Class<?> type) {
      super("Unknown enum tag " + value + " for " + type.getCanonicalName());
      this.value = value;
    }
  }
}
