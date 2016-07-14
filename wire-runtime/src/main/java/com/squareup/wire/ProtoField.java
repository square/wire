/*
 * Copyright (C) 2016 Square, Inc.
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

/** A field that can be decided from or encoded to a protocol buffer message. */
public final class ProtoField<E> {
  public final int tag;
  public final ProtoAdapter<E> protoAdapter;
  public E defaultValue;

  public ProtoField(int tag, ProtoAdapter<E> protoAdapter, E defaultValue) {
    this.tag = tag;
    this.protoAdapter = protoAdapter;
    this.defaultValue = defaultValue;
  }

  /** Returns the encoded size of {@code value}, or 0 if it is null. */
  public int encodedSize(E value) {
    return value != null ? protoAdapter.encodedSizeWithTag(tag, value) : 0;
  }

  /**
   * Encodes {@code value} with this field's tag to writer. This will not encode anything bytes if
   * {@code value} is null.
   */
  public void encode(ProtoWriter writer, E value) throws IOException {
    if (value != null) protoAdapter.encodeWithTag(writer, tag, value);
  }
}
