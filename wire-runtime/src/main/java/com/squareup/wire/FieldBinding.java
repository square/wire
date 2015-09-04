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

import com.squareup.wire.Message.Builder;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Read, write, and describe a tag within a message. This class knows how to assign fields to a
 * builder object, and how to extract values from a message object.
 */
final class FieldBinding<M extends Message<M>, B extends Message.Builder<M, B>> {
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

  public final Message.Label label;
  public final String name;
  public final int tag;
  public final Message.Datatype datatype;
  public final boolean redacted;
  public final WireAdapter<?> singleAdapter;
  public final WireAdapter<?> adapter;

  private final Field messageField;
  private final Field builderField;
  private final Method builderMethod;

  FieldBinding(ProtoField protoField, WireAdapter<?> singleAdapter,
      Field messageField, Class<Builder<M, B>> builderType) {
    this.label = protoField.label();
    this.name = messageField.getName();
    this.tag = protoField.tag();
    this.datatype = protoField.type();
    this.redacted = protoField.redacted();
    this.singleAdapter = singleAdapter;
    this.adapter = singleAdapter.withLabel(label);
    this.messageField = messageField;
    this.builderField = getBuilderField(builderType, name);
    this.builderMethod = getBuilderMethod(builderType, name, messageField.getType());
  }

  void set(Builder<M, B> builder, Object value) {
    try {
      if (label.isOneOf()) {
        // In order to maintain the 'oneof' invariant, call the builder setter method rather
        // than setting the builder field directly.
        builderMethod.invoke(builder, value);
      } else {
        builderField.set(builder, value);
      }
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    } catch (InvocationTargetException e) {
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

  Object getFromBuilder(Builder<M, B> builder) {
    try {
      return builderField.get(builder);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }
}
