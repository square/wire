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

final class RuntimeMessageAdapter<M extends Message<M>, B extends Builder<M, B>>
    extends WireAdapter<M> {
  static <M extends Message<M>, B extends Builder<M, B>> RuntimeMessageAdapter<M, B> create(
      Wire wire, Class<M> messageType) {
    Class<Builder<M, B>> builderType = getBuilderType(messageType);
    Constructor<Builder<M, B>> builderCopyConstructor =
        getBuilderCopyConstructor(builderType, messageType);
    Map<Integer, FieldBinding<M, B>> fieldBindings
        = new LinkedHashMap<Integer, FieldBinding<M, B>>();

    // Create tag bindings for fields annotated with '@ProtoField'
    for (Field messageField : messageType.getDeclaredFields()) {
      ProtoField protoField = messageField.getAnnotation(ProtoField.class);
      if (protoField != null) {
        WireAdapter<?> singleAdapter = singleAdapter(wire, messageField, protoField);
        fieldBindings.put(protoField.tag(), new FieldBinding<M, B>(
            protoField, singleAdapter, messageField, builderType));
      }
    }

    Map<Integer, RegisteredExtension> extensions = Collections.emptyMap();
    return new RuntimeMessageAdapter<M, B>(wire, messageType, builderType, builderCopyConstructor,
        Collections.unmodifiableMap(fieldBindings), extensions);
  }

  private static WireAdapter<?> singleAdapter(Wire wire, Field messageField,
      ProtoField protoField) {
    if (protoField.type() == Message.Datatype.ENUM) {
      return wire.enumAdapter(getEnumType(protoField, messageField));
    }
    if (protoField.type() == Message.Datatype.MESSAGE) {
      return wire.adapter(getMessageType(protoField, messageField));
    }
    return WireAdapter.get(wire, protoField.type(), null, null);
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

  private final Wire wire;
  private final Class<M> messageType;
  private final Class<Builder<M, B>> builderType;
  private final Constructor<Builder<M, B>> builderCopyConstructor;
  private final Map<Integer, FieldBinding<M, B>> fieldBindings;
  private final Map<Integer, RegisteredExtension> extensions;

  RuntimeMessageAdapter(Wire wire, Class<M> messageType, Class<Builder<M, B>> builderType,
      Constructor<Builder<M, B>> builderCopyConstructor,
      Map<Integer, FieldBinding<M, B>> fieldBindings,
      Map<Integer, RegisteredExtension> extensions) {
    super(FieldEncoding.LENGTH_DELIMITED, messageType);
    this.wire = wire;
    this.messageType = messageType;
    this.builderType = builderType;
    this.builderCopyConstructor = builderCopyConstructor;
    this.fieldBindings = fieldBindings;
    this.extensions = extensions;
  }

  @Override public RuntimeMessageAdapter<M, B> withExtensions(ExtensionRegistry extensionRegistry) {
    Map<Integer, RegisteredExtension> extensions =
        new LinkedHashMap<Integer, RegisteredExtension>(this.extensions);

    for (Extension<?, ?> extension : extensionRegistry.extensions(messageType)) {
      WireAdapter<?> singleAdapter = WireAdapter.get(wire, extension.getDatatype(),
          extension.getMessageType(), extension.getEnumType());
      extensions.put(extension.getTag(), new RegisteredExtension(extension, singleAdapter));
    }

    return new RuntimeMessageAdapter<M, B>(wire, messageType, builderType, builderCopyConstructor,
        fieldBindings, Collections.unmodifiableMap(extensions));
  }

  Map<Integer, FieldBinding<M, B>> fieldBindings() {
    return fieldBindings;
  }

  Map<Integer, RegisteredExtension> extensions() {
    return extensions;
  }

  B newBuilder() {
    try {
      return (B) builderType.newInstance();
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    } catch (InstantiationException e) {
      throw new AssertionError(e);
    }
  }

  Builder<M, B> newBuilder(M value) {
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
  private static <M extends Message<M>, B extends Builder<M, B>> Class<Builder<M, B>>
  getBuilderType(Class<M> messageType) {
    try {
      return (Class<Builder<M, B>>) Class.forName(messageType.getName() + "$Builder");
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("No builder class found for message type "
          + messageType.getName());
    }
  }

  private static <M extends Message<M>, B extends Builder<M, B>> Constructor<Builder<M, B>>
  getBuilderCopyConstructor(Class<Builder<M, B>> builderType, Class<M> messageType) {
    try {
      return builderType.getConstructor(messageType);
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }

  // Writing

  @Override public int encodedSize(M message) {
    int cachedSerializedSize = message.cachedSerializedSize;
    if (cachedSerializedSize != 0) return cachedSerializedSize;

    int size = 0;
    for (FieldBinding<M, B> fieldBinding : fieldBindings.values()) {
      Object value = fieldBinding.get(message);
      if (value == null) continue;
      size += ((WireAdapter<Object>) fieldBinding.adapter).encodedSize(fieldBinding.tag, value);
    }

    size += message.getUnknownFieldsSerializedSize();
    message.cachedSerializedSize = size;
    return size;
  }

  @Override public void encode(ProtoWriter writer, M message) throws IOException {
    for (FieldBinding<M, B> fieldBinding : fieldBindings.values()) {
      Object value = fieldBinding.get(message);
      if (value == null) continue;
      ((WireAdapter<Object>) fieldBinding.adapter).encodeTagged(writer, fieldBinding.tag, value);
    }
    message.writeUnknownFieldMap(writer);
  }

  @Override public M redact(M message) {
    Builder<M, B> builder = newBuilder(message);
    for (FieldBinding<M, B> fieldBinding : fieldBindings.values()) {
      if (!fieldBinding.redacted && fieldBinding.datatype != Message.Datatype.MESSAGE) continue;
      if (fieldBinding.redacted && fieldBinding.label == Message.Label.REQUIRED) {
        throw new IllegalArgumentException(String.format(
            "Field %s.%s is REQUIRED and cannot be redacted.",
            javaType.getName(), fieldBinding.name));
      }
      Object builderValue = fieldBinding.getFromBuilder(builder);
      if (builderValue != null) {
        Object redactedValue = ((WireAdapter<Object>) fieldBinding.adapter).redact(builderValue);
        fieldBinding.set(builder, redactedValue);
      }
    }
    builder.tagMap = builder.tagMap != null
        ? builder.tagMap.redact()
        : null;
    return builder.build();
  }

  @Override public boolean equals(Object o) {
    return o instanceof RuntimeMessageAdapter
        && ((RuntimeMessageAdapter) o).messageType == messageType;
  }

  @Override public int hashCode() {
    return messageType.hashCode();
  }

  /**
   * Returns a human-readable version of the given {@link Message}.
   */
  public String toString(M message) {
    StringBuilder sb = new StringBuilder();
    sb.append(messageType.getSimpleName());
    sb.append('{');
    boolean seenValue = false;
    for (FieldBinding<M, B> fieldBinding : fieldBindings.values()) {
      Object value = fieldBinding.get(message);
      if (value == null) continue;
      if (seenValue) sb.append(", ");
      sb.append(fieldBinding.name)
          .append('=')
          .append(fieldBinding.redacted ? "██" : value);
      seenValue = true;
    }
    if (message.tagMap != null) {
      for (Extension<?, ?> extension : message.tagMap.extensions(true)) {
        if (seenValue) sb.append(", ");
        if (!extension.isUnknown()) {
          sb.append(extension.getName());
        } else {
          sb.append(extension.getTag());
        }
        sb.append('=').append(message.tagMap.get(extension));
        seenValue = true;
      }
    }
    sb.append('}');
    return sb.toString();
  }

  // Reading

  @Override public M decode(ProtoReader reader) throws IOException {
    B builder = newBuilder();
    Storage storage = new Storage();

    long token = reader.beginMessage();
    for (int tag; (tag = reader.nextTag()) != -1;) {
      FieldBinding<M, B> fieldBinding = fieldBindings.get(tag);
      if (fieldBinding != null) {
        try {
          Object value = fieldBinding.singleAdapter.decode(reader);
          if (fieldBinding.label.isRepeated()) {
            storage.add(tag, value);
          } else {
            fieldBinding.set(builder, value);
          }
        } catch (RuntimeEnumAdapter.EnumConstantNotFoundException e) {
          // An unknown Enum value was encountered, store it as an unknown field
          builder.addVarint(tag, e.value);
        }
        continue;
      }

      RegisteredExtension registeredExtension = extensions.get(tag);
      if (registeredExtension != null) {
        try {
          Object value = registeredExtension.adapter.decode(reader);
          builder.ensureUnknownFieldMap().add(registeredExtension.extension, value);
        } catch (RuntimeEnumAdapter.EnumConstantNotFoundException e) {
          // An unknown Enum value was encountered, store it as an unknown field
          builder.addVarint(tag, e.value);
        }
        continue;
      }

      FieldEncoding fieldEncoding = reader.peekFieldEncoding();
      Object value = fieldEncoding.rawWireAdapter().decode(reader);
      builder.ensureUnknownFieldMap().add(tag, fieldEncoding, value);
    }
    reader.endMessage(token);

    // Set repeated fields
    for (int storedTag : storage.getTags()) {
      FieldBinding<M, B> fieldBinding = fieldBindings.get(storedTag);
      List<Object> value = storage.get(storedTag);
      fieldBinding.set(builder, value);
    }
    return builder.build();
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
