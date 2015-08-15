/*
 * Copyright 2015 Square Inc.
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
 * Read, write, and describe a tag within a message. This class knows how to assign fields to a
 * builder object, and how to extract values from a message object.
 */
abstract class TagBinding<M, B> {
  public final int tag;
  public final String name;
  public final Message.Datatype datatype;
  public final Message.Label label;
  public final boolean redacted;
  public final TypeAdapter<?> singleAdapter;
  public final TypeAdapter<?> adapter;
  public final Class<?> singleType;

  public TagBinding(Message.Label label, Message.Datatype datatype, String name, int tag,
      boolean redacted, TypeAdapter<?> singleAdapter, Class<?> singleType) {
    this.tag = tag;
    this.name = name;
    this.datatype = datatype;
    this.label = label;
    this.redacted = redacted;
    this.singleAdapter = singleAdapter;
    this.singleType = singleType;
    this.adapter = singleAdapter.withLabel(label);
  }

  abstract void set(B builder, Object value);

  abstract Object get(M message);

  final int serializedSize(M message) {
    Object value = get(message);
    return value != null
        ? ((TypeAdapter<Object>) adapter).serializedSize(tag, value)
        : 0;
  }

  final void write(M message, ProtoWriter protoWriter) throws IOException {
    Object value = get(message);
    if (value != null) {
      protoWriter.write(tag, value, (TypeAdapter<Object>) adapter);
    }
  }

  abstract void redactBuilderField(B builder);

  final boolean addToString(M message, StringBuilder sb, boolean seenValue) {
    Object value = get(message);
    if (value == null) {
      return false;
    }
    if (seenValue) {
      sb.append(", ");
    }
    sb.append(name);
    sb.append("=");
    sb.append(redacted ? "██" : value);
    return true;
  }
}
