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
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;

import static com.squareup.wire.Message.Builder;

final class RuntimeMessageAdapter<M extends Message> extends MessageAdapter<M> {
  private final Wire wire;
  private final Class<M> messageType;
  private final Class<Builder<M>> builderType;
  private final Constructor<Builder<M>> builderCopyConstructor;
  private final Map<Integer, TagBinding<M, Builder<M>>> tagBindings;

  /** Cache information about the Message class and its mapping to proto wire format. */
  RuntimeMessageAdapter(Wire wire, Class<M> messageType) {
    this.wire = wire;
    this.messageType = messageType;
    this.builderType = getBuilderType(messageType);
    this.builderCopyConstructor = getBuilderCopyConstructor(builderType, messageType);

    Map<Integer, TagBinding<M, Builder<M>>> tagBindings
        = new LinkedHashMap<Integer, TagBinding<M, Builder<M>>>();

    // Create tag bindings for fields annotated with '@ProtoField'
    for (Field messageField : messageType.getDeclaredFields()) {
      ProtoField protoField = messageField.getAnnotation(ProtoField.class);
      if (protoField != null) {
        TypeAdapter<?> singleAdapter = singleTypeAdapter(wire, messageField, protoField);
        Class<?> singleType = singleAdapter.javaType;
        FieldTagBinding<M> tagBinding = new FieldTagBinding<M>(
            protoField, singleAdapter, singleType, messageField, builderType);
        tagBindings.put(tagBinding.tag, tagBinding);
      }
    }

    // Create tag bindings for registered extensions.
    for (Extension<?, ?> extension : wire.getExtensions(messageType)) {
      TypeAdapter<?> singleAdapter = TypeAdapter.get(wire, extension.getDatatype(),
          extension.getMessageType(), extension.getEnumType());
      tagBindings.put(extension.getTag(), new ExtensionTagBinding<M>(extension, singleAdapter));
    }

    this.tagBindings = Collections.unmodifiableMap(tagBindings);
  }

  @Override public Class<M> messageType() {
    return messageType;
  }

  private TypeAdapter<?> singleTypeAdapter(Wire wire, Field messageField, ProtoField protoField) {
    if (protoField.type() == Message.Datatype.ENUM) {
      return wire.enumAdapter(getEnumType(protoField, messageField));
    }
    if (protoField.type() == Message.Datatype.MESSAGE) {
      return TypeAdapter.forMessage(wire.adapter(getMessageType(protoField, messageField)));
    }
    return TypeAdapter.get(wire, protoField.type(), null, null);
  }

  @SuppressWarnings("unchecked")
  private static Class<? extends Message> getMessageType(ProtoField protoField, Field field) {
    Class<?> fieldType = field.getType();
    if (List.class.isAssignableFrom(fieldType)) {
      return protoField.messageType();
    } else {
      return (Class<Message>) fieldType;
    }
  }

  @SuppressWarnings("unchecked")
  private static Class<? extends ProtoEnum> getEnumType(ProtoField protoField, Field field) {
    Class<?> fieldType = field.getType();
    if (List.class.isAssignableFrom(fieldType)) {
      return protoField.enumType();
    } else {
      return (Class<ProtoEnum>) fieldType;
    }
  }

  Map<Integer, TagBinding<M, Builder<M>>> tagBindings() {
    return tagBindings;
  }

  Builder<M> newBuilder() {
    try {
      return builderType.newInstance();
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    } catch (InstantiationException e) {
      throw new AssertionError(e);
    }
  }

  Builder<M> newBuilder(M value) {
    try {
      return builderCopyConstructor.newInstance(value);
    } catch (InvocationTargetException e) {
      throw new AssertionError(e);
    } catch (InstantiationException e) {
      throw new AssertionError(e);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static <M extends Message> Class<Builder<M>> getBuilderType(Class<M> messageType) {
    try {
      return (Class<Builder<M>>) Class.forName(messageType.getName() + "$Builder");
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("No builder class found for message type "
          + messageType.getName());
    }
  }

  private static <M extends Message> Constructor<Builder<M>> getBuilderCopyConstructor(
      Class<Builder<M>> builderType, Class<M> messageType) {
    try {
      return builderType.getConstructor(messageType);
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }

  // Writing

  @Override public int serializedSize(M message) {
    int cachedSerializedSize = message.cachedSerializedSize;
    if (cachedSerializedSize != -1) {
      return cachedSerializedSize;
    }

    int size = 0;
    for (TagBinding<M, Builder<M>> tagBinding : tagBindingsForMessage(message).values()) {
      Object value = tagBinding.get(message);
      if (value == null) continue;
      size += ((TypeAdapter<Object>) tagBinding.adapter).serializedSize(tagBinding.tag, value);
    }

    size += message.getUnknownFieldsSerializedSize();
    message.cachedSerializedSize = size;
    return size;
  }

  /**
   * Returns the tag bindings for this adapter, plus any extensions known by {@code message} but
   * unknown to this adapter. If all extensions are registered this is returns the same map;
   * otherwise the returned map may include more tags.
   */
  Map<Integer, TagBinding<M, Builder<M>>> tagBindingsForMessage(M message) {
    Map<Integer, TagBinding<M, Builder<M>>> result = tagBindings;
    if (!(message instanceof ExtendableMessage)) return result;

    ExtensionMap map = ((ExtendableMessage) message).extensionMap;
    if (map == null) return result;

    for (int i = 0, count = map.size(); i < count; i++) {
      Extension<?, ?> extension = map.getExtension(i);
      if (tagBindings.containsKey(extension.getTag())) continue;

      // Make a mutable copy before we mutate result.
      if (result == tagBindings) {
        result = new LinkedHashMap<Integer, TagBinding<M, Builder<M>>>(tagBindings);
      }

      TypeAdapter<?> singleAdapter = TypeAdapter.get(wire, extension.getDatatype(),
          extension.getMessageType(), extension.getEnumType());
      result.put(extension.getTag(), new ExtensionTagBinding<M>(extension, singleAdapter));
    }

    return result;
  }

  @Override public void write(M message, ProtoWriter writer) throws IOException {
    for (TagBinding<M, Builder<M>> tagBinding : tagBindingsForMessage(message).values()) {
      Object value = tagBinding.get(message);
      if (value == null) continue;
      ((TypeAdapter<Object>) tagBinding.adapter).writeTagged(writer, tagBinding.tag, value);
    }
    message.writeUnknownFieldMap(writer);
  }

  @Override public M redact(M message) {
    Builder<M> builder = newBuilder(message);
    for (TagBinding<M, Builder<M>> tagBinding : tagBindingsForMessage(message).values()) {
      if (!tagBinding.redacted && tagBinding.datatype != Message.Datatype.MESSAGE) continue;
      if (tagBinding.redacted && tagBinding.label == Message.Label.REQUIRED) {
        throw new IllegalArgumentException(String.format(
            "Field %s.%s is REQUIRED and cannot be redacted.",
            messageType().getName(), tagBinding.name));
      }
      Object builderValue = tagBinding.getFromBuilder(builder);
      if (builderValue != null) {
        Object redactedValue = ((TypeAdapter<Object>) tagBinding.adapter).redact(builderValue);
        tagBinding.set(builder, redactedValue);
      }
    }
    return builder.build();
  }

  /**
   * Returns a human-readable version of the given {@link Message}.
   */
  public String toString(M message) {
    StringBuilder sb = new StringBuilder();
    sb.append(messageType.getSimpleName());
    sb.append('{');
    boolean seenValue = false;
    for (TagBinding<M, Builder<M>> tagBinding : tagBindingsForMessage(message).values()) {
      Object value = tagBinding.get(message);
      if (value == null) continue;
      if (seenValue) sb.append(", ");
      sb.append(tagBinding.name);
      sb.append('=');
      sb.append(tagBinding.redacted ? "██" : value);
      seenValue = true;
    }
    sb.append('}');
    return sb.toString();
  }

  // Reading

  @Override public M read(ProtoReader input) throws IOException {
    Builder<M> builder = newBuilder();
    Storage storage = new Storage();

    for (int tag; (tag = input.nextTag()) != -1;) {
      TagBinding<M, Builder<M>> tagBinding = tagBindings.get(tag);
      if (tagBinding == null) {
        TypeAdapter<?> typeAdapter = input.peekFieldEncoding().rawTypeAdapter();
        readUnknownField(builder, input, tag, typeAdapter);
        continue;
      }

      try {
        Object value = tagBinding.singleAdapter.read(input);
        if (tagBinding.label.isRepeated()) {
          storage.add(tag, value);
        } else {
          tagBinding.set(builder, value);
        }
      } catch (RuntimeEnumAdapter.EnumConstantNotFoundException e) {
        // An unknown Enum value was encountered, store it as an unknown field
        builder.addVarint(tag, e.value);
      }
    }

    // Set repeated fields
    for (int storedTag : storage.getTags()) {
      TagBinding<M, Builder<M>> tagBinding = tagBindings.get(storedTag);
      List<Object> value = storage.get(storedTag);
      tagBinding.set(builder, value);
    }
    return builder.build();
  }

  private <T> void readUnknownField(
      Builder builder, ProtoReader input, int tag, TypeAdapter<T> typeAdapter) throws IOException {
    builder.ensureUnknownFieldMap().add(tag, typeAdapter.read(input), typeAdapter);
  }

  private static class Storage {
    private Map<Integer, ImmutableList<Object>> map;

    void add(int tag, Object value) {
      ImmutableList<Object> list = map == null ? null : map.get(tag);
      if (list == null) {
        list = new ImmutableList<Object>();
        if (map == null) {
          map = new LinkedHashMap<Integer, ImmutableList<Object>>();
        }
        map.put(tag, list);
      }
      list.list.add(value);
    }

    Set<Integer> getTags() {
      if (map == null) return Collections.emptySet();
      return map.keySet();
    }

    List<Object> get(int tag) {
      return map == null ? null : map.get(tag);
    }
  }

  /**
   * An immutable implementation of List that allows Wire messages to avoid the need to make copies.
   */
  static class ImmutableList<T> extends AbstractList<T>
      implements Cloneable, RandomAccess, Serializable {

    private final List<T> list = new ArrayList<T>();

    @Override public Object clone() {
      return this;
    }

    @Override public int size() {
      return list.size();
    }

    @Override public T get(int i) {
      return list.get(i);
    }

    private Object writeReplace() throws ObjectStreamException {
      return Collections.unmodifiableList(list);
    }
  }
}
