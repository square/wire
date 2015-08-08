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

final class ReflectiveMessageAdapter<M extends Message> extends MessageAdapter<M> {
  // Unicode character "Full Block" (U+2588)
  private static final String FULL_BLOCK = "█";
  // The string to use when redacting fields from toString.
  private static final String REDACTED = FULL_BLOCK + FULL_BLOCK;

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
      case MESSAGE: return wire.adapter(messageType);
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
    int size = 0;
    for (ReflectiveFieldBinding fieldBinding : fieldBindingMap.values) {
      Object value = fieldBinding.getValue(message);
      if (value == null) {
        continue;
      }
      int tag = fieldBinding.tag;
      TypeAdapter<Object> adapter = fieldBinding.adapter;

      Label label = fieldBinding.label;
      if (label.isRepeated()) {
        if (label.isPacked()) {
          size += getPackedSize((List<?>) value, tag, adapter);
        } else {
          size += getRepeatedSize((List<?>) value, tag, adapter);
        }
      } else {
        size += getSerializedSize(tag, value, adapter);
      }
    }

    if (message instanceof ExtendableMessage) {
      ExtendableMessage extendableMessage = (ExtendableMessage) message;
      if (extendableMessage.extensionMap != null) {
        size += getExtensionsSerializedSize(extendableMessage.extensionMap);
      }
    }
    size += message.getUnknownFieldsSerializedSize();
    return size;
  }

  private <T extends ExtendableMessage<T>> int getExtensionsSerializedSize(ExtensionMap<T> map) {
    int size = 0;
    for (int i = 0, count = map.size(); i < count; i++) {
      Extension<T, ?> extension = map.getExtension(i);
      TypeAdapter<Object> adapter = (TypeAdapter<Object>) typeAdapter(wire, extension.getDatatype(),
          extension.getMessageType(), extension.getEnumType());
      Object value = map.getExtensionValue(i);
      int tag = extension.getTag();
      Label label = extension.getLabel();
      if (label.isRepeated()) {
        if (label.isPacked()) {
          size += getPackedSize((List<?>) value, tag, adapter);
        } else {
          size += getRepeatedSize((List<?>) value, tag, adapter);
        }
      } else {
        size += getSerializedSize(tag, value, adapter);
      }
    }
    return size;
  }

  private int getRepeatedSize(List<?> value, int tag, TypeAdapter<Object> adapter) {
    int size = 0;
    for (int i = 0, count = value.size(); i < count; i++) {
      size += getSerializedSize(tag, value.get(i), adapter);
    }
    return size;
  }

  private int getPackedSize(List<?> value, int tag, TypeAdapter<Object> adapter) {
    int packedLength = 0;
    for (int i = 0, count = value.size(); i < count; i++) {
      packedLength += adapter.serializedSize(value.get(i));
    }
    // tag + length + value + value + ...
    int size = ProtoWriter.varint32Size(ProtoWriter.makeTag(tag, WireType.LENGTH_DELIMITED));
    size += ProtoWriter.varint32Size(packedLength);
    size += packedLength;
    return size;
  }

  @Override public void write(M message, ProtoWriter output) throws IOException {
    for (ReflectiveFieldBinding fieldBinding : fieldBindingMap.values) {
      Object value = fieldBinding.getValue(message);
      if (value == null) {
        continue;
      }
      int tag = fieldBinding.tag;
      Datatype datatype = fieldBinding.datatype;
      TypeAdapter<Object> adapter = fieldBinding.adapter;

      Label label = fieldBinding.label;
      if (label.isRepeated()) {
        if (label.isPacked()) {
          writePacked(output, (List<?>) value, tag, adapter);
        } else {
          writeRepeated(output, (List<?>) value, tag, datatype, adapter);
        }
      } else {
        writeValue(output, tag, value, datatype, adapter);
      }
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
      TypeAdapter<Object> adapter = (TypeAdapter<Object>) typeAdapter(wire, extension.getDatatype(),
          extension.getMessageType(), extension.getEnumType());
      Object value = extensionMap.getExtensionValue(i);
      int tag = extension.getTag();
      Datatype datatype = extension.getDatatype();
      Label label = extension.getLabel();
      if (label.isRepeated()) {
        if (label.isPacked()) {
          writePacked(output, (List<?>) value, tag, adapter);
        } else {
          writeRepeated(output, (List<?>) value, tag, datatype, adapter);
        }
      } else {
        writeValue(output, tag, value, datatype, adapter);
      }
    }
  }

  private void writeRepeated(ProtoWriter output, List<?> value, int tag, Datatype datatype,
      TypeAdapter<Object> adapter)
      throws IOException {
    for (int i = 0, count = value.size(); i < count; i++) {
      writeValue(output, tag, value.get(i), datatype, adapter);
    }
  }

  private void writePacked(ProtoWriter output, List<?> value, int tag, TypeAdapter<Object> adapter)
      throws IOException {
    int packedLength = 0;
    for (int i = 0, count = value.size(); i < count; i++) {
      packedLength += adapter.serializedSize(value.get(i));
    }
    output.writeTag(tag, WireType.LENGTH_DELIMITED);
    output.writeVarint32(packedLength);
    for (int i = 0, count = value.size(); i < count; i++) {
      adapter.write(value.get(i), output);
    }
  }

  @Override public M redact(M value) {
    return Redactor.get(messageType).redact(value);
  }

  /**
   * Returns a human-readable version of the given {@link Message}.
   */
  String toString(M message) {
    StringBuilder sb = new StringBuilder();
    sb.append(messageType.getSimpleName());
    sb.append("{");

    String sep = "";
    for (ReflectiveFieldBinding fieldBinding : fieldBindingMap.values) {
      Object value = fieldBinding.getValue(message);
      if (value == null) {
        continue;
      }
      sb.append(sep);
      sep = ", ";
      sb.append(fieldBinding.name);
      sb.append("=");
      sb.append(fieldBinding.redacted ? REDACTED : value);
    }
    if (message instanceof ExtendableMessage<?>) {
      ExtendableMessage<?> extendableMessage = (ExtendableMessage<?>) message;
      sb.append(sep);
      sb.append("{extensions=");
      sb.append(extendableMessage.extensionsToString());
      sb.append("}");
    }
    sb.append("}");
    return sb.toString();
  }

  /**
   * Returns the serialized size in bytes of the given tag and value.
   */
  private int getSerializedSize(int tag, Object value, TypeAdapter<Object> adapter) {
    int size = adapter.serializedSize(value);
    if (adapter.type == WireType.LENGTH_DELIMITED) {
      size += varint32Size(size);
    }
    return ProtoWriter.tagSize(tag) + size;
  }

  private void writeValue(ProtoWriter output, int tag, Object value, Datatype datatype,
      TypeAdapter<Object> adapter) throws IOException {
    output.writeTag(tag, datatype.wireType());
    output.write(adapter, value);
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
        adapter = fieldBinding.adapter;
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
        // Message adapters do not read their length prefix. TODO this is a temporary hack.
        long token = adapter instanceof MessageAdapter<?> ? input.beginLengthDelimited() : -1;
        try {
          value = input.value(adapter);
        } catch (EnumAdapter.EnumConstantNotFoundException e) {
          // An unknown Enum value was encountered, store it as an unknown field
          builder.addVarint(tag, e.value);
          continue;
        }
        if (token != -1) {
          input.endLengthDelimited(token);
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
