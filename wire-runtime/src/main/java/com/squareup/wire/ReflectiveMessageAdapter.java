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
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.ProtocolException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;

import static com.squareup.wire.ExtendableMessage.ExtendableBuilder;
import static com.squareup.wire.Message.Builder;
import static com.squareup.wire.Message.Datatype;
import static com.squareup.wire.Message.Label;
import static com.squareup.wire.TypeAdapter.BYTES;
import static com.squareup.wire.TypeAdapter.FIXED32;
import static com.squareup.wire.TypeAdapter.FIXED64;
import static com.squareup.wire.TypeAdapter.UINT64;

final class ReflectiveMessageAdapter<M extends Message> extends MessageAdapter<M> {
  private final Wire wire;
  private final Class<M> messageType;
  private final Class<Builder<M>> builderType;
  private final TagMap<ReflectiveFieldBinding> fieldBindingMap;

  /** Cache information about the Message class and its mapping to proto wire format. */
  ReflectiveMessageAdapter(Wire wire, Class<M> messageType) {
    this.wire = wire;
    this.messageType = messageType;
    this.builderType = getBuilderType(messageType);

    Map<Integer, ReflectiveFieldBinding> map = new LinkedHashMap<Integer, ReflectiveFieldBinding>();
    for (Field messageField : messageType.getDeclaredFields()) {
      // Process fields annotated with '@ProtoField'
      ProtoField protoField = messageField.getAnnotation(ProtoField.class);
      if (protoField != null) {
        int tag = protoField.tag();
        map.put(tag, ReflectiveFieldBinding.create(wire, protoField, messageField, builderType));
      }
    }
    fieldBindingMap = TagMap.of(map);
  }

  TagMap<ReflectiveFieldBinding> getFieldBindingMap() {
    return fieldBindingMap;
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

  @SuppressWarnings("unchecked")
  private Class<Builder<M>> getBuilderType(Class<M> messageType) {
    try {
      return (Class<Builder<M>>) Class.forName(messageType.getName() + "$Builder");
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("No builder class found for message type "
          + messageType.getName());
    }
  }

  static TypeAdapter<?> fieldTypeAdapter(Wire wire, Datatype datatype, Label label,
      Class<? extends Message> messageType, Class<? extends ProtoEnum> enumType) {
    TypeAdapter<?> adapter = typeAdapter(wire, datatype, messageType, enumType);
    if (!label.isRepeated()) {
      return adapter;
    }
    if (label.isPacked()) {
      return TypeAdapter.createPacked(adapter);
    }
    return TypeAdapter.createRepeated(adapter);
  }

  static TypeAdapter<?> typeAdapter(Wire wire, Datatype datatype,
      Class<? extends Message> messageType, Class<? extends ProtoEnum> enumType) {
    switch (datatype) {
      case BOOL: return TypeAdapter.BOOL;
      case BYTES: return TypeAdapter.BYTES;
      case DOUBLE: return TypeAdapter.DOUBLE;
      case ENUM: return wire.enumAdapter(enumType);
      case FIXED32: return TypeAdapter.FIXED32;
      case FIXED64: return TypeAdapter.FIXED64;
      case FLOAT: return TypeAdapter.FLOAT;
      case INT32: return TypeAdapter.INT32;
      case INT64: return TypeAdapter.INT64;
      case MESSAGE: return TypeAdapter.forMessage(wire.adapter(messageType));
      case SFIXED32: return TypeAdapter.SFIXED32;
      case SFIXED64: return TypeAdapter.SFIXED64;
      case SINT32: return TypeAdapter.SINT32;
      case SINT64: return TypeAdapter.SINT64;
      case STRING: return TypeAdapter.STRING;
      case UINT32: return TypeAdapter.UINT32;
      case UINT64: return TypeAdapter.UINT64;
      default: throw new AssertionError("Unknown data type " + datatype);
    }
  }

  // Writing

  @Override public int serializedSize(M message) {
    int cachedSerializedSize = message.cachedSerializedSize;
    if (cachedSerializedSize != -1) {
      return cachedSerializedSize;
    }

    int size = 0;
    for (ReflectiveFieldBinding fieldBinding : fieldBindingMap.values) {
      size += fieldBinding.serializedSize(message);
    }

    if (message instanceof ExtendableMessage) {
      ExtendableMessage extendableMessage = (ExtendableMessage) message;
      if (extendableMessage.extensionMap != null) {
        size += getExtensionsSerializedSize(extendableMessage.extensionMap);
      }
    }
    size += message.getUnknownFieldsSerializedSize();

    message.cachedSerializedSize = size;
    return size;
  }

  private <T extends ExtendableMessage<T>> int getExtensionsSerializedSize(ExtensionMap<T> map) {
    int size = 0;
    for (int i = 0, count = map.size(); i < count; i++) {
      Extension<T, ?> extension = map.getExtension(i);
      TypeAdapter<Object> adapter =
          (TypeAdapter<Object>) fieldTypeAdapter(wire, extension.getDatatype(),
              extension.getLabel(), extension.getMessageType(), extension.getEnumType());
      Object value = map.getExtensionValue(i);
      int tag = extension.getTag();
      size += adapter.serializedSize(tag, value);
    }
    return size;
  }

  @Override public void write(M message, ProtoWriter output) throws IOException {
    for (ReflectiveFieldBinding fieldBinding : fieldBindingMap.values) {
      fieldBinding.write(message, output);
    }

    if (message instanceof ExtendableMessage) {
      ExtendableMessage extendableMessage = (ExtendableMessage) message;
      if (extendableMessage.extensionMap != null) {
        writeExtensions(output, extendableMessage.extensionMap);
      }
    }
    message.writeUnknownFieldMap(output);
  }

  private <T extends ExtendableMessage<T>> void writeExtensions(ProtoWriter output,
      ExtensionMap<T> extensionMap) throws IOException {
    for (int i = 0, count = extensionMap.size(); i < count; i++) {
      Extension<T, ?> extension = extensionMap.getExtension(i);
      TypeAdapter<Object> adapter =
          (TypeAdapter<Object>) fieldTypeAdapter(wire, extension.getDatatype(),
              extension.getLabel(), extension.getMessageType(), extension.getEnumType());
      Object value = extensionMap.getExtensionValue(i);
      int tag = extension.getTag();
      output.write(tag, value, adapter);
    }
  }

  @Override public M redact(M value) {
    return Redactor.get(messageType).redact(value);
  }

  /**
   * Returns a human-readable version of the given {@link Message}.
   */
  public String toString(M message) {
    StringBuilder sb = new StringBuilder();
    sb.append(messageType.getSimpleName());
    sb.append("{");

    boolean seenValue = false;
    for (ReflectiveFieldBinding fieldBinding : fieldBindingMap.values) {
      seenValue |= fieldBinding.addToString(message, sb, seenValue);
    }
    if (message instanceof ExtendableMessage<?>) {
      if (seenValue) {
        sb.append(", ");
      }
      ExtendableMessage<?> extendableMessage = (ExtendableMessage<?>) message;
      sb.append("{extensions=");
      sb.append(extendableMessage.extensionsToString());
      sb.append("}");
    }
    sb.append("}");
    return sb.toString();
  }

  // Reading

  @Override public M read(ProtoReader input) throws IOException {
    Builder<M> builder = newBuilder();
    Storage storage = new Storage();

    while (input.hasNext()) {
      int tag = input.readTag();

      Extension<?, ?> extension = null;
      Label label;
      TypeAdapter<Object> adapter;
      ReflectiveFieldBinding fieldBinding = fieldBindingMap.get(tag);
      if (fieldBinding != null) {
        label = fieldBinding.label;
        adapter = fieldBinding.singleAdapter;
      } else {
        extension = getExtension(tag);
        if (extension == null) {
          readUnknownField(builder, input, tag);
          continue;
        }
        label = extension.getLabel();
        adapter = (TypeAdapter<Object>) typeAdapter(wire, extension.getDatatype(),
            extension.getMessageType(), extension.getEnumType());
      }

      if (label.isPacked() && input.peekType() == WireType.LENGTH_DELIMITED) {
        // Decode packed format
        long token = input.beginLengthDelimited();
        while (input.hasNext()) {
          try {
            Object value = input.value(adapter);
            storage.add(tag, value);
          } catch (EnumAdapter.EnumConstantNotFoundException e) {
            // An unknown Enum value was encountered, store it as an unknown field
            builder.addVarint(tag, e.value);
          }
        }
        input.endLengthDelimited(token);
      } else {
        // Read a single value
        Object value;
        try {
          value = input.value(adapter);
        } catch (EnumAdapter.EnumConstantNotFoundException e) {
          // An unknown Enum value was encountered, store it as an unknown field
          builder.addVarint(tag, e.value);
          continue;
        }
        if (label.isRepeated()) {
          storage.add(tag, value != null ? value : Collections.emptyList());
        } else if (extension != null) {
          setExtension((ExtendableBuilder<?, ?>) builder, extension, value);
        } else if (label.isOneOf()) {
          // In order to maintain the 'oneof' invariant, call the builder setter method rather
          // than setting the builder field directly.
          fieldBinding.setBuilderMethod(builder, value);
        } else {
          fieldBinding.setBuilderField(builder, value);
        }
      }
    }

    // Set repeated fields
    for (int storedTag : storage.getTags()) {
      ReflectiveFieldBinding fieldBinding = fieldBindingMap.get(storedTag);
      List<Object> value = storage.get(storedTag);

      if (fieldBinding != null) {
        fieldBinding.setBuilderField(builder, value);
      } else {
        setExtension((ExtendableBuilder<?, ?>) builder, getExtension(storedTag), value);
      }
    }
    return builder.build();
  }

  private void readUnknownField(Builder builder, ProtoReader input, int tag)
      throws IOException {
    WireType type = input.peekType();
    switch (type) {
      case VARINT:
        builder.ensureUnknownFieldMap().add(tag, input.readVarint64(), UINT64);
        break;
      case FIXED32:
        builder.ensureUnknownFieldMap().add(tag, input.readFixed32(), FIXED32);
        break;
      case FIXED64:
        builder.ensureUnknownFieldMap().add(tag, input.readFixed64(), FIXED64);
        break;
      case LENGTH_DELIMITED:
        builder.ensureUnknownFieldMap().add(tag, input.readBytes(), BYTES);
        break;
      /* Skip any groups found in the input */
      case START_GROUP:
        input.skipGroup();
        break;
      case END_GROUP:
        break;
      default: throw new ProtocolException("Unknown wire type: " + type);
    }
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

  @SuppressWarnings("unchecked")
  private Extension<?, ?> getExtension(int tag) {
    ExtensionRegistry registry = wire.registry;
    return registry == null
        ? null : registry.getExtension((Class<ExtendableMessage>) messageType, tag);
  }

  @SuppressWarnings("unchecked")
  Extension<?, ?> getExtension(String name) {
    ExtensionRegistry registry = wire.registry;
    return registry == null
        ? null : registry.getExtension((Class<ExtendableMessage>) messageType, name);
  }

  @SuppressWarnings("unchecked")
  private void setExtension(ExtendableMessage.ExtendableBuilder builder, Extension<?, ?> extension,
      Object value) {
    builder.setExtension(extension, value);
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
  }
}
