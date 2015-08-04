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

  @Override public int getSerializedSize(M message) {
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

  @Override void write(M message, ProtoWriter output) throws IOException {
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
      case INT32: return ProtoWriter.int32Size((Integer) value);
      case INT64: case UINT64: return ProtoWriter.varint64Size((Long) value);
      case UINT32: return ProtoWriter.varint32Size((Integer) value);
      case SINT32: return ProtoWriter.varint32Size(ProtoWriter.zigZag32((Integer) value));
      case SINT64: return ProtoWriter.varint64Size(ProtoWriter.zigZag64((Long) value));
      case BOOL: return 1;
      case ENUM: return getEnumSize((ProtoEnum) value);
      case STRING:
        int utf8Length = utf8Length((String) value);
        return ProtoWriter.varint32Size(utf8Length) + utf8Length;
      case BYTES:
        int length = ((ByteString) value).size();
        return ProtoWriter.varint32Size(length) + length;
      case MESSAGE: return getMessageSize((Message) value);
      case FIXED32: case SFIXED32: case FLOAT:
        return WireType.FIXED_32_SIZE;
      case FIXED64: case SFIXED64: case DOUBLE:
        return WireType.FIXED_64_SIZE;
      default: throw new RuntimeException();
    }
  }

  private int utf8Length(String s) {
    int count = 0;
    for (int i = 0, length = s.length(); i < length; i++) {
      char ch = s.charAt(i);
      if (ch <= 0x7F) {
        count++;
      } else if (ch <= 0x7FF) {
        count += 2;
      } else if (Character.isHighSurrogate(ch)) {
        count += 4;
        ++i;
      } else {
        count += 3;
      }
    }
    return count;
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
      size = message.cachedSerializedSize = adapter.getSerializedSize(message);
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
      case INT32: output.writeSignedVarint32((Integer) value); break;
      case INT64: case UINT64: output.writeVarint64((Long) value); break;
      case UINT32: output.writeVarint32((Integer) value); break;
      case SINT32: output.writeVarint32(ProtoWriter.zigZag32((Integer) value)); break;
      case SINT64: output.writeVarint64(ProtoWriter.zigZag64((Long) value)); break;
      case BOOL: output.writeRawByte((Boolean) value ? 1 : 0); break;
      case ENUM: writeEnum((ProtoEnum) value, output); break;
      case STRING:
        ByteString bytes = ByteString.encodeUtf8((String) value);
        output.writeVarint32(bytes.size());
        output.writeRawBytes(bytes);
        break;
      case BYTES:
        ByteString byteString = (ByteString) value;
        output.writeVarint32(byteString.size());
        output.writeRawBytes(byteString);
        break;
      case MESSAGE: writeMessage((Message) value, output); break;
      case FIXED32: case SFIXED32: output.writeFixed32((Integer) value); break;
      case FIXED64: case SFIXED64: output.writeFixed64((Long) value); break;
      case FLOAT: output.writeFixed32(Float.floatToIntBits((Float) value)); break;
      case DOUBLE: output.writeFixed64(Double.doubleToLongBits((Double) value)); break;
      default: throw new RuntimeException();
    }
  }

  @SuppressWarnings("unchecked")
  private <MM extends Message> void writeMessage(MM message, ProtoWriter output)
      throws IOException {
    MessageAdapter<MM> adapter = wire.messageAdapter((Class<MM>) message.getClass());
    int size = message.cachedSerializedSize;
    if (size == -1) {
      size = message.cachedSerializedSize = adapter.getSerializedSize(message);
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

  @Override M read(ProtoReader input) throws IOException {
    Builder<M> builder = newBuilder();
    Storage storage = new Storage();

    while (true) {
      int tagAndType = input.readTag();
      int tag = tagAndType >> WireType.TAG_TYPE_BITS;
      if (tag == 0) break;
      WireType wireType = WireType.valueOf(tagAndType);

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
          readUnknownField(builder, input, tag, wireType);
          continue;
        }
        datatype = extension.getDatatype();
        label = extension.getLabel();
      }

      Object value;
      if (label.isPacked() && wireType == WireType.LENGTH_DELIMITED) {
        // Decode packed format
        int length = input.readVarint32();
        long start = input.getPosition();
        int oldLimit = input.pushLimit(length);
        while (input.getPosition() < start + length) {
          value = readValue(input, tag, datatype);
          if (datatype == Datatype.ENUM && value instanceof Integer) {
            // An unknown Enum value was encountered, store it as an unknown field
            builder.addVarint(tag, (Integer) value);
          } else {
            storage.add(tag, value);
          }
        }
        input.popLimit(oldLimit);
        if (input.getPosition() != start + length) {
          throw new ProtocolException("Packed data had wrong length!");
        }
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
      case INT32: case UINT32: return input.readVarint32();
      case INT64: case UINT64: return input.readVarint64();
      case SINT32: return ProtoReader.decodeZigZag32(input.readVarint32());
      case SINT64: return ProtoReader.decodeZigZag64(input.readVarint64());
      case BOOL: return input.readVarint32() != 0;
      case ENUM:
        EnumAdapter<? extends ProtoEnum> adapter = getEnumAdapter(tag);
        int value = input.readVarint32();
        try {
          return adapter.fromInt(value);
        } catch (IllegalArgumentException e) {
          // Return the raw value as an Integer
          return value;
        }
      case STRING: return input.readString();
      case BYTES: return input.readBytes();
      case MESSAGE: return readMessage(input, tag);
      case FIXED32: case SFIXED32: return input.readFixed32();
      case FIXED64: case SFIXED64: return input.readFixed64();
      case FLOAT: return Float.intBitsToFloat(input.readFixed32());
      case DOUBLE: return Double.longBitsToDouble(input.readFixed64());
      default: throw new RuntimeException();
    }
  }

  private Message readMessage(ProtoReader input, int tag) throws IOException {
    final int length = input.readVarint32();
    if (input.recursionDepth >= ProtoReader.RECURSION_LIMIT) {
      throw new IOException("Wire recursion limit exceeded");
    }
    final int oldLimit = input.pushLimit(length);
    ++input.recursionDepth;
    MessageAdapter<? extends Message> adapter = getMessageAdapter(tag);
    Message message = adapter.read(input);
    input.checkLastTagWas(0);
    --input.recursionDepth;
    input.popLimit(oldLimit);
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

  private void readUnknownField(Builder builder, ProtoReader input, int tag, WireType type)
      throws IOException {
    switch (type) {
      case VARINT:
        builder.ensureUnknownFieldMap().addVarint(tag, input.readVarint64());
        break;
      case FIXED32:
        builder.ensureUnknownFieldMap().addFixed32(tag, input.readFixed32());
        break;
      case FIXED64:
        builder.ensureUnknownFieldMap().addFixed64(tag, input.readFixed64());
        break;
      case LENGTH_DELIMITED:
        builder.ensureUnknownFieldMap().addLengthDelimited(tag, input.readBytes());
        break;
      /* Skip any groups found in the input */
      case START_GROUP:
        input.skipGroup();
        break;
      case END_GROUP:
        break;
      default: throw new RuntimeException("Unsupported wire type: " + type);
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
