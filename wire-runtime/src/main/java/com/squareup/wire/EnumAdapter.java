/*
 * Copyright 2016 Square Inc.
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

/**
 * An abstract {@link ProtoAdapter} that converts values of an enum to and from integers.
 */
public abstract class EnumAdapter<E extends WireEnum> extends ProtoAdapter<E> {
  protected EnumAdapter(Class<E> type) {
    super(FieldEncoding.VARINT, type);
  }

  @Override public final int encodedSize(E value) {
    return ProtoWriter.varint32Size(value.getValue());
  }

  @Override public final void encode(ProtoWriter writer, E value) throws IOException {
    writer.writeVarint32(value.getValue());
  }

  @Override public final E decode(ProtoReader reader) throws IOException {
    int value = reader.readVarint32();
    E constant = fromValue(value);
    if (constant == null) {
      throw new EnumConstantNotFoundException(value, javaType);
    }
    return constant;
  }

  /**
   * Converts an integer to an enum.
   * Returns null if there is no corresponding enum.
   */
  protected abstract E fromValue(int value);
}
