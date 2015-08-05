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
import okio.ByteString;

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
        map.put(tag, ReflectiveFieldBinding.create(protoField, messageField, builderType));
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

  // Writing

  @Override public int serializedSize(M message) {
    int size = 0;
    for (ReflectiveFieldBinding fieldBinding : fieldBindingMap.values) {
      Object value = fieldBinding.getValue(message);
      if (value == null) {
        continue;
      }
      int tag = fieldBinding.tag;
      Datatype datatype = fieldBinding.datatype;
      Label label = fieldBinding.label;

      if (label.isRepeated()) {
        if (label.isPacked()) {
          size += getPackedSize((List<?>) value, tag, datatype);
        } else {
          size += getRepeatedSize((List<?>) value, tag, datatype);
        }
      } else {
        size += getSerializedSize(tag, value, datatype);
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
      Object value = map.getExtensionValue(i);
      int tag = extension.getTag();
      Datatype datatype = extension.getDatatype();
      Label label = extension.getLabel();
      if (label.isRepeated()) {
        if (label.isPacked()) {
          size += getPackedSize((List<?>) value, tag, datatype);
        } else {
          size += getRepeatedSize((List<?>) value, tag, datatype);
        }
      } else {
        size += getSerializedSize(tag, value, datatype);
      }
    }
    return size;
  }

  private int getRepeatedSize(List<?> value, int tag, Datatype datatype) {
    int size = 0;
    for (int i = 0, count = value.size(); i < count; i++) {
      size += getSerializedSize(tag, value.get(i), datatype);
    }
    return size;
  }

  private int getPackedSize(List<?> value, int tag, Datatype datatype) {
    int packedLength = 0;
    for (int i = 0, count = value.size(); i < count; i++) {
      packedLength += getSerializedSizeNoTag(value.get(i), datatype);
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
      Label label = fieldBinding.label;

      if (label.isRepeated()) {
        if (label.isPacked()) {
          writePacked(output, (List<?>) value, tag, datatype);
        } else {
          writeRepeated(output, (List<?>) value, tag, datatype);
        }
      } else {
        writeValue(output, tag, value, datatype);
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
      Object value = extensionMap.getExtensionValue(i);
      int tag = extension.getTag();
      Datatype datatype = extension.getDatatype();
      Label label = extension.getLabel();
      if (label.isRepeated()) {
        if (label.isPacked()) {
          writePacked(output, (List<?>) value, tag, datatype);
        } else {
          writeRepeated(output, (List<?>) value, tag, datatype);
        }
      } else {
        writeValue(output, tag, value, datatype);
      }
    }
  }

  private void writeRepeated(ProtoWriter output, List<?> value, int tag, Datatype datatype)
      throws IOException {
    for (int i = 0, count = value.size(); i < count; i++) {
      writeValue(output, tag, value.get(i), datatype);
    }
  }

  private void writePacked(ProtoWriter output, List<?> value, int tag, Datatype datatype)
      throws IOException {
    int packedLength = 0;
    for (int i = 0, count = value.size(); i < count; i++) {
      packedLength += getSerializedSizeNoTag(value.get(i), datatype);
    }
    output.writeTag(tag, WireType.LENGTH_DELIMITED);
    output.writeVarint32(packedLength);
    for (int i = 0, count = value.size(); i < count; i++) {
      writeValueNoTag(output, value.get(i), datatype);
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
  private int getSerializedSize(int tag, Object value, Datatype datatype) {
    return ProtoWriter.tagSize(tag) + getSerializedSizeNoTag(value, datatype);
  }

  /**
   * Returns the serialized size in bytes of the given value without any prepended tag or length,
   * e.g., as it would be written as part of a 'packed' repeated field.
   */
  private int getSerializedSizeNoTag(Object value, Datatype datatype) {
    switch (datatype) {
      case INT32: return TypeAdapter.INT32.serializedSize((Integer) value);
      case INT64: return TypeAdapter.INT64.serializedSize((Long) value);
      case UINT32: return TypeAdapter.UINT32.serializedSize((Integer) value);
      case UINT64: return TypeAdapter.UINT64.serializedSize((Long) value);
      case SINT32: return TypeAdapter.SINT32.serializedSize((Integer) value);
      case SINT64: return TypeAdapter.SINT64.serializedSize((Long) value);
      case BOOL: return TypeAdapter.BOOL.serializedSize((Boolean) value);
      case STRING:
        int stringSize = TypeAdapter.STRING.serializedSize((String) value);
        return varint32Size(stringSize) + stringSize;
      case BYTES:
        int bytesSize = TypeAdapter.BYTES.serializedSize((ByteString) value);
        return varint32Size(bytesSize) + bytesSize;
      case FIXED32: return TypeAdapter.FIXED32.serializedSize((Integer) value);
      case FIXED64: return TypeAdapter.FIXED64.serializedSize((Long) value);
      case SFIXED32: return TypeAdapter.SFIXED32.serializedSize((Integer) value);
      case SFIXED64: return TypeAdapter.SFIXED64.serializedSize((Long) value);
      case FLOAT: return TypeAdapter.FLOAT.serializedSize((Float) value);
      case DOUBLE: return TypeAdapter.DOUBLE.serializedSize((Double) value);
      case MESSAGE: return getMessageSize((Message) value);
      case ENUM: return getEnumSize((ProtoEnum) value);
      default: throw new AssertionError("Unknown data type " + datatype);
    }
  }

  @SuppressWarnings("unchecked")
  private <E extends ProtoEnum> int getEnumSize(E value) {
    return ProtoWriter.varint32Size(value.getValue());
  }

  @SuppressWarnings("unchecked")
  private <MM extends Message> int getMessageSize(MM message) {
    int size = message.cachedSerializedSize;
    if (size == -1) {
      MessageAdapter<MM> adapter = wire.messageAdapter((Class<MM>) message.getClass());
      size = message.cachedSerializedSize = adapter.serializedSize(message);
    }
    return ProtoWriter.varint32Size(size) + size;
  }

  private void writeValue(ProtoWriter output, int tag, Object value, Datatype datatype)
      throws IOException {
    output.writeTag(tag, datatype.wireType());
    writeValueNoTag(output, value, datatype);
  }

  /**
   * Writes a value with no tag.
   */
  private void writeValueNoTag(ProtoWriter output, Object value, Datatype datatype)
      throws IOException {
    switch (datatype) {
      case INT32: output.write(TypeAdapter.INT32, (Integer) value); break;
      case INT64: output.write(TypeAdapter.INT64, (Long) value); break;
      case UINT32: output.write(TypeAdapter.UINT32, (Integer) value); break;
      case UINT64: output.write(TypeAdapter.UINT64, (Long) value); break;
      case SINT32: output.write(TypeAdapter.SINT32, (Integer) value); break;
      case SINT64: output.write(TypeAdapter.SINT64, (Long) value); break;
      case BOOL: output.write(TypeAdapter.BOOL, (Boolean) value); break;
      case STRING: output.write(TypeAdapter.STRING, (String) value); break;
      case BYTES: output.write(TypeAdapter.BYTES, (ByteString) value); break;
      case FIXED32: output.write(TypeAdapter.FIXED32, (Integer) value); break;
      case FIXED64: output.write(TypeAdapter.FIXED64, (Long) value); break;
      case SFIXED32: output.write(TypeAdapter.SFIXED32, (Integer) value); break;
      case SFIXED64: output.write(TypeAdapter.SFIXED64, (Long) value); break;
      case FLOAT: output.write(TypeAdapter.FLOAT, (Float) value); break;
      case DOUBLE: output.write(TypeAdapter.DOUBLE, (Double) value); break;
      case MESSAGE: writeMessage((Message) value, output); break;
      case ENUM: writeEnum((ProtoEnum) value, output); break;
      default: throw new AssertionError("Unknown data type " + datatype);
    }
  }

  @SuppressWarnings("unchecked")
  private <MM extends Message> void writeMessage(MM message, ProtoWriter output)
      throws IOException {
    MessageAdapter<MM> adapter = wire.messageAdapter((Class<MM>) message.getClass());
    int size = message.cachedSerializedSize;
    if (size == -1) {
      size = message.cachedSerializedSize = adapter.serializedSize(message);
    }
    output.writeVarint32(size);
    adapter.write(message, output);
  }

  @SuppressWarnings("unchecked")
  private <E extends ProtoEnum> void writeEnum(E value, ProtoWriter output)
      throws IOException {
    output.writeVarint32(value.getValue());
  }

  // Reading

  @Override public M read(ProtoReader input) throws IOException {
    Builder<M> builder = newBuilder();
    Storage storage = new Storage();

    while (input.hasNext()) {
      int tag = input.readTag();

      Extension<?, ?> extension = null;
      Datatype datatype;
      Label label;
      ReflectiveFieldBinding fieldBinding = fieldBindingMap.get(tag);
      if (fieldBinding != null) {
        datatype = fieldBinding.datatype;
        label = fieldBinding.label;
      } else {
        extension = getExtension(tag);
        if (extension == null) {
          readUnknownField(builder, input, tag);
          continue;
        }
        datatype = extension.getDatatype();
        label = extension.getLabel();
      }

      Object value;
      if (label.isPacked() && input.peekType() == WireType.LENGTH_DELIMITED) {
        // Decode packed format
        long token = input.beginLengthDelimited();
        while (input.hasNext()) {
          value = readValue(input, tag, datatype);
          if (datatype == Datatype.ENUM && value instanceof Integer) {
            // An unknown Enum value was encountered, store it as an unknown field
            builder.addVarint(tag, (Integer) value);
          } else {
            storage.add(tag, value);
          }
        }
        input.endLengthDelimited(token);
      } else {
        // Read a single value
        value = readValue(input, tag, datatype);
        if (datatype == Datatype.ENUM && value instanceof Integer) {
          // An unknown Enum value was encountered, store it as an unknown field
          builder.addVarint(tag, (Integer) value);
        } else {
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

  private Object readValue(ProtoReader input, int tag, Datatype datatype) throws IOException {
    switch (datatype) {
      case INT32: return TypeAdapter.INT32.read(input);
      case INT64: return TypeAdapter.INT64.read(input);
      case UINT32: return TypeAdapter.UINT32.read(input);
      case UINT64: return TypeAdapter.UINT64.read(input);
      case SINT32: return TypeAdapter.SINT32.read(input);
      case SINT64: return TypeAdapter.SINT64.read(input);
      case BOOL: return TypeAdapter.BOOL.read(input);
      case STRING: return TypeAdapter.STRING.read(input);
      case BYTES: return TypeAdapter.BYTES.read(input);
      case FIXED32: return TypeAdapter.FIXED32.read(input);
      case FIXED64: return TypeAdapter.FIXED64.read(input);
      case SFIXED32: return TypeAdapter.SFIXED32.read(input);
      case SFIXED64: return TypeAdapter.SFIXED64.read(input);
      case FLOAT: return TypeAdapter.FLOAT.read(input);
      case DOUBLE: return TypeAdapter.DOUBLE.read(input);
      case MESSAGE: return readMessage(input, tag);
      case ENUM:
        EnumAdapter<? extends ProtoEnum> adapter = getEnumAdapter(tag);
        int value = input.readVarint32();
        try {
          return adapter.fromInt(value);
        } catch (IllegalArgumentException e) {
          return value; // Return the raw value as an Integer
        }
      default: throw new AssertionError("Unknown data type " + datatype);
    }
  }

  private Message readMessage(ProtoReader input, int tag) throws IOException {
    long token = input.beginLengthDelimited();
    Message message = getMessageAdapter(tag).read(input);
    input.endLengthDelimited(token);
    return message;
  }

  private MessageAdapter<? extends Message> getMessageAdapter(int tag) {
    ReflectiveFieldBinding fieldBinding = fieldBindingMap.get(tag);
    if (fieldBinding != null && fieldBinding.messageAdapter != null) {
      return fieldBinding.messageAdapter;
    }
    MessageAdapter<? extends Message> result = wire.messageAdapter(getMessageClass(tag));
    if (fieldBinding != null) {
      fieldBinding.messageAdapter = result;
    }
    return result;
  }

  private EnumAdapter<? extends ProtoEnum> getEnumAdapter(int tag) {
    ReflectiveFieldBinding fieldBinding = fieldBindingMap.get(tag);
    if (fieldBinding != null && fieldBinding.enumAdapter != null) {
      return fieldBinding.enumAdapter;
    }
    EnumAdapter<? extends ProtoEnum> result = wire.enumAdapter(getEnumClass(tag));
    if (fieldBinding != null) {
      fieldBinding.enumAdapter = result;
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private Class<Message> getMessageClass(int tag) {
    ReflectiveFieldBinding fieldBinding = fieldBindingMap.get(tag);
    Class<Message> messageClass = fieldBinding == null
        ? null : (Class<Message>) fieldBinding.messageType;
    if (messageClass == null) {
      Extension<?, ?> extension = getExtension(tag);
      if (extension != null) {
        messageClass = (Class<Message>) extension.getMessageType();
      }
    }
    return messageClass;
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

  private Class<? extends ProtoEnum> getEnumClass(int tag) {
    ReflectiveFieldBinding fieldBinding = fieldBindingMap.get(tag);
    Class<? extends ProtoEnum> enumType = fieldBinding == null ? null : fieldBinding.enumType;
    if (enumType == null) {
      Extension<?, ?> extension = getExtension(tag);
      if (extension != null) {
        enumType = extension.getEnumType();
      }
    }
    return enumType;
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
