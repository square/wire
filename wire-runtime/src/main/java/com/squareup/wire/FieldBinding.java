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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Read, write, and describe a tag within a message. This class knows how to assign fields to a
 * builder object, and how to extract values from a message object.
 */
final class FieldBinding<M extends Message<M, B>, B extends Message.Builder<M, B>> {
  private static Field getBuilderField(Class<?> builderType, String name) {
    try {
      return builderType.getField(name);
    } catch (NoSuchFieldException e) {
      throw new AssertionError("No builder field " + builderType.getName() + "." + name);
    }
  }

  private static Method getBuilderMethod(Class<?> builderType, String name, Class<?> type) {
    try {
      return builderType.getMethod(name, type);
    } catch (NoSuchMethodException e) {
      throw new AssertionError("No builder method " + builderType.getName() + "." + name
          + "(" + type.getName() + ")");
    }
  }

  public final WireField.Label label;
  public final String name;
  public final int tag;
  public final String adapterString;
  public final boolean redacted;
  private final Field messageField;
  private final Field builderField;
  private final Method builderMethod;

  // Delegate adapters are created lazily; otherwise we could stack overflow!
  private ProtoAdapter<?> singleAdapter;
  private ProtoAdapter<Object> adapter;

  FieldBinding(WireField wireField, Field messageField, Class<B> builderType) {
    this.label = wireField.label();
    this.name = messageField.getName();
    this.tag = wireField.tag();
    this.adapterString = wireField.adapter();
    this.redacted = wireField.redacted();
    this.messageField = messageField;
    this.builderField = getBuilderField(builderType, name);
    this.builderMethod = getBuilderMethod(builderType, name, messageField.getType());
  }

  ProtoAdapter<?> singleAdapter() {
    ProtoAdapter<?> result = singleAdapter;
    return result != null ? result : (singleAdapter = ProtoAdapter.get(adapterString));
  }

  ProtoAdapter<Object> adapter() {
    ProtoAdapter<Object> result = adapter;
    return result != null
        ? result
        : (adapter = (ProtoAdapter<Object>) singleAdapter().withLabel(label));
  }

  /** Accept a single value, independent of whether this value is single or repeated. */
  void value(B builder, Object value) {
    if (label.isRepeated()) {
      try {
        List<Object> list = (List<Object>) builderField.get(builder);
        list.add(value);
      } catch (IllegalAccessException e) {
        throw new AssertionError(e);
      }
    } else {
      set(builder, value);
    }
  }

  /** Assign a single value for required/optional fields, or a list for repeated/packed fields. */
  void set(B builder, Object value) {
    try {
      if (label.isOneOf()) {
        // In order to maintain the 'oneof' invariant, call the builder setter method rather
        // than setting the builder field directly.
        builderMethod.invoke(builder, value);
      } else {
        builderField.set(builder, value);
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new AssertionError(e);
    }
  }

  Object get(M message) {
    try {
      return messageField.get(message);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  Object getFromBuilder(B builder) {
    try {
      return builderField.get(builder);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }
}
