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

final class RuntimeMessageAdapter<M extends Message> extends WireAdapter<M> {
  static <M extends Message> RuntimeMessageAdapter<M> create(Wire wire, Class<M> messageType) {
    Class<Builder<M>> builderType = getBuilderType(messageType);
    Constructor<Builder<M>> builderCopyConstructor =
        getBuilderCopyConstructor(builderType, messageType);
    Map<Integer, TagBinding<M, Builder<M>>> tagBindings
        = new LinkedHashMap<Integer, TagBinding<M, Builder<M>>>();

    // Create tag bindings for fields annotated with '@ProtoField'
    for (Field messageField : messageType.getDeclaredFields()) {
      ProtoField protoField = messageField.getAnnotation(ProtoField.class);
      if (protoField != null) {
        WireAdapter<?> singleAdapter = singleAdapter(wire, messageField, protoField);
        Class<?> singleType = singleAdapter.javaType;
        FieldTagBinding<M> tagBinding = new FieldTagBinding<M>(
            protoField, singleAdapter, singleType, messageField, builderType);
        tagBindings.put(tagBinding.tag, tagBinding);
      }
    }

    return new RuntimeMessageAdapter<M>(wire, messageType, builderType, builderCopyConstructor,
        Collections.unmodifiableMap(tagBindings));
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
  private final Class<Builder<M>> builderType;
  private final Constructor<Builder<M>> builderCopyConstructor;
  private final Map<Integer, TagBinding<M, Builder<M>>> tagBindings;

  RuntimeMessageAdapter(Wire wire, Class<M> messageType, Class<Builder<M>> builderType,
      Constructor<Builder<M>> builderCopyConstructor,
      Map<Integer, TagBinding<M, Builder<M>>> tagBindings) {
    super(FieldEncoding.LENGTH_DELIMITED, messageType);
    this.wire = wire;
    this.messageType = messageType;
    this.builderType = builderType;
    this.builderCopyConstructor = builderCopyConstructor;
    this.tagBindings = tagBindings;
  }

  @Override public RuntimeMessageAdapter<M> withExtensions(ExtensionRegistry extensionRegistry) {
    Map<Integer, TagBinding<M, Builder<M>>> tagBindings =
        new LinkedHashMap<Integer, TagBinding<M, Builder<M>>>(this.tagBindings);

    for (Extension<?, ?> extension : extensionRegistry.extensions(messageType)) {
      WireAdapter<?> singleAdapter = WireAdapter.get(wire, extension.getDatatype(),
          extension.getMessageType(), extension.getEnumType());
      tagBindings.put(extension.getTag(), new ExtensionTagBinding<M>(extension, singleAdapter));
    }

    return new RuntimeMessageAdapter<M>(wire, messageType, builderType, builderCopyConstructor,
        Collections.unmodifiableMap(tagBindings));
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

  @Override public int encodedSize(M message) {
    int cachedSerializedSize = message.cachedSerializedSize;
    if (cachedSerializedSize != 0) return cachedSerializedSize;

    int size = 0;
    for (TagBinding<M, Builder<M>> tagBinding : tagBindings.values()) {
      Object value = tagBinding.get(message);
      if (value == null) continue;
      if (tagBinding instanceof FieldTagBinding) {
        size += ((WireAdapter<Object>) tagBinding.adapter).encodedSize(tagBinding.tag, value);
      }
    }

    size += message.getUnknownFieldsSerializedSize();
    message.cachedSerializedSize = size;
    return size;
  }

  @Override public void encode(ProtoWriter writer, M message) throws IOException {
    for (TagBinding<M, Builder<M>> tagBinding : tagBindings.values()) {
      Object value = tagBinding.get(message);
      if (value == null) continue;
      if (tagBinding instanceof FieldTagBinding) {
        ((WireAdapter<Object>) tagBinding.adapter).encodeTagged(writer, tagBinding.tag, value);
      }
    }
    message.writeUnknownFieldMap(writer);
  }

  @Override public M redact(M message) {
    Builder<M> builder = newBuilder(message);
    for (TagBinding<M, Builder<M>> tagBinding : tagBindings.values()) {
      if (!tagBinding.redacted && tagBinding.datatype != Message.Datatype.MESSAGE) continue;
      if (tagBinding.redacted && tagBinding.label == Message.Label.REQUIRED) {
        throw new IllegalArgumentException(String.format(
            "Field %s.%s is REQUIRED and cannot be redacted.",
            javaType.getName(), tagBinding.name));
      }
      Object builderValue = tagBinding.getFromBuilder(builder);
      if (builderValue != null) {
        Object redactedValue = ((WireAdapter<Object>) tagBinding.adapter).redact(builderValue);
        tagBinding.set(builder, redactedValue);
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
    for (TagBinding<M, Builder<M>> tagBinding : tagBindings.values()) {
      if (!(tagBinding instanceof FieldTagBinding)) continue;
      Object value = tagBinding.get(message);
      if (value == null) continue;
      if (seenValue) sb.append(", ");
      sb.append(tagBinding.name)
          .append('=')
          .append(tagBinding.redacted ? "██" : value);
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
    Builder<M> builder = newBuilder();
    Storage storage = new Storage();

    long token = reader.beginMessage();
    for (int tag; (tag = reader.nextTag()) != -1;) {
      TagBinding<M, Builder<M>> tagBinding = tagBindings.get(tag);
      if (tagBinding == null) {
        FieldEncoding fieldEncoding = reader.peekFieldEncoding();
        Object value = fieldEncoding.rawWireAdapter().decode(reader);
        builder.ensureUnknownFieldMap().add(tag, fieldEncoding, value);
        continue;
      }

      try {
        Object value = tagBinding.singleAdapter.decode(reader);
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
    reader.endMessage(token);

    // Set repeated fields
    for (int storedTag : storage.getTags()) {
      TagBinding<M, Builder<M>> tagBinding = tagBindings.get(storedTag);
      List<Object> value = storage.get(storedTag);
      tagBinding.set(builder, value);
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
