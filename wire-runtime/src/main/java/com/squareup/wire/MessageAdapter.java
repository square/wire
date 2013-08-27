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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.squareup.wire.ExtendableMessage.ExtendableBuilder;
import static com.squareup.wire.Message.Builder;
import static com.squareup.wire.Message.Datatype;
import static com.squareup.wire.Message.Label;

/**
 * An adapter than can perform I/O on a given Message type.
 *
 * @param <M> the Message class handled by this adapter.
 */
final class MessageAdapter<M extends Message> {

  public static final class FieldInfo {
    final int tag;
    final String name;
    final Datatype datatype;
    final Label label;
    final Class<? extends Enum> enumType;
    final Class<? extends Message> messageType;

    private final Field messageField;
    private final Method builderMethod;

    @SuppressWarnings("unchecked")
    private FieldInfo(int tag, String name, Datatype datatype, Label label,
        Class<?> enumOrMessageType, Field messageField, Method builderMethod) {
      this.tag = tag;
      this.name = name;
      this.datatype = datatype;
      this.label = label;
      if (datatype == Datatype.ENUM) {
        this.enumType = (Class<? extends Enum>) enumOrMessageType;
        this.messageType = null;
      } else if (datatype == Datatype.MESSAGE) {
        this.messageType = (Class<? extends Message>) enumOrMessageType;
        this.enumType = null;
      } else {
        this.enumType = null;
        this.messageType = null;
      }

      // private fields
      this.messageField = messageField;
      this.builderMethod = builderMethod;
    }
  }

  /**
   * Returns an instance of the message type of this {@link MessageAdapter} with all fields unset.
   */
  public synchronized M getDefaultInstance() {
    if (defaultInstance == null) {
      try {
        defaultInstance = builderType.newInstance().build();
      } catch (InstantiationException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    return defaultInstance;
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

  Collection<FieldInfo> getFields() {
    return fieldInfoMap.values();
  }

  FieldInfo getField(String name) {
    Integer key = tagMap.get(name);
    return key == null ? null : fieldInfoMap.get(key);
  }

  Object getFieldValue(M message, FieldInfo fieldInfo) {
    if (fieldInfo.messageField == null) {
      throw new AssertionError("Field is not of type \"Message\"");
    }
    try {
      return fieldInfo.messageField.get(message);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  public void setBuilderField(Builder<M> builder, int tag, Object value) {
    try {
      fieldInfoMap.get(tag).builderMethod.invoke(builder, value);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    } catch (InvocationTargetException e) {
      throw new AssertionError(e);
    }
  }

  private final Wire wire;
  private final Class<M> messageType;
  private final Class<Builder<M>> builderType;
  private final Map<String, Integer> tagMap = new LinkedHashMap<String, Integer>();
  private final Map<Integer, FieldInfo> fieldInfoMap =
      new LinkedHashMap<Integer, FieldInfo>();

  private M defaultInstance;

  /** Cache information about the Message class and its mapping to proto wire format. */
  MessageAdapter(Wire wire, Class<M> messageType) {
    this.wire = wire;
    this.messageType = messageType;
    this.builderType = getBuilderType(messageType);

    for (Field messageField : messageType.getDeclaredFields()) {
      // Process fields annotated with '@ProtoField'
      ProtoField annotation = messageField.getAnnotation(ProtoField.class);
      if (annotation != null) {
        int tag = annotation.tag();

        String name = messageField.getName();
        tagMap.put(name, tag);
        Class<?> enumOrMessageType = null;
        Datatype datatype = annotation.type();
        if (datatype == Datatype.ENUM) {
          enumOrMessageType = getEnumType(messageField);
        } else if (datatype == Datatype.MESSAGE) {
          enumOrMessageType = getMessageType(messageField);
        }
        fieldInfoMap.put(tag, new FieldInfo(tag, name, datatype, annotation.label(),
            enumOrMessageType, messageField, getBuilderMethod(name, messageField.getType())));
      }
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

  private Method getBuilderMethod(String name, Class<?> type) {
    try {
      return builderType.getMethod(name, type);
    } catch (NoSuchMethodException e) {
      throw new AssertionError("No builder method "
          + builderType.getName() + "." + name + "(" + type.getName() + ")");
    }
  }

  @SuppressWarnings("unchecked")
  private Class<Message> getMessageType(Field field) {
    Class<?> fieldType = field.getType();
    if (Message.class.isAssignableFrom(fieldType)) {
      return (Class<Message>) fieldType;
    } else if (List.class.isAssignableFrom(fieldType)) {
      // Retrieve the declare element type of the list
      Type type = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
      if (type instanceof Class<?> && Message.class.isAssignableFrom((Class<?>) type)) {
        return (Class<Message>) type;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private Class<Enum> getEnumType(Field field) {
    Class<?> fieldType = field.getType();
    if (Enum.class.isAssignableFrom(fieldType)) {
      return (Class<Enum>) fieldType;
    } else if (List.class.isAssignableFrom(fieldType)) {
      // Retrieve the declare element type of the list
      Type type = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
      if (type instanceof Class<?> && Enum.class.isAssignableFrom((Class<?>) type)) {
        return (Class<Enum>) type;
      }
    }
    return null;
  }

  // Writing

  /**
   * Returns the serialized size of a given message, in bytes.
   */
  int getSerializedSize(M message) {
    int size = 0;
    for (FieldInfo fieldInfo : getFields()) {
      Object value = getFieldValue(message, fieldInfo);
      if (value == null) {
        continue;
      }
      int tag = fieldInfo.tag;
      Datatype datatype = fieldInfo.datatype;
      Label label = fieldInfo.label;

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

  private <T extends ExtendableMessage<?>> int getExtensionsSerializedSize(ExtensionMap<T> map) {
    int size = 0;
    for (Extension<T, ?> extension : map.getExtensions()) {
      Object value = map.get(extension);
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
    for (Object o : value) {
      size += getSerializedSize(tag, o, datatype);
    }
    return size;
  }

  private int getPackedSize(List<?> value, int tag, Datatype datatype) {
    int packedLength = 0;
    for (Object o : value) {
      packedLength += getSerializedSizeNoTag(o, datatype);
    }
    // tag + length + value + value + ...
    int size = WireOutput.varint32Size(WireOutput.makeTag(tag, WireType.LENGTH_DELIMITED));
    size += WireOutput.varint32Size(packedLength);
    size += packedLength;
    return size;
  }

  /** Uses reflection to write {@code message} to {@code output} in serialized form. */
  void write(M message, WireOutput output) throws IOException {
    for (FieldInfo fieldInfo : getFields()) {
      Object value = getFieldValue(message, fieldInfo);
      if (value == null) {
        continue;
      }
      int tag = fieldInfo.tag;
      Datatype datatype = fieldInfo.datatype;
      Label label = fieldInfo.label;

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

  private <T extends ExtendableMessage<?>> void writeExtensions(WireOutput output,
      ExtensionMap<T> extensionMap) throws IOException {
    for (Extension<T, ?> extension: extensionMap.getExtensions()) {
      Object value = extensionMap.get(extension);
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

  private void writeRepeated(WireOutput output, List<?> value, int tag, Datatype datatype)
      throws IOException {
    for (Object o : value) {
      writeValue(output, tag, o, datatype);
    }
  }

  private void writePacked(WireOutput output, List<?> value, int tag, Datatype datatype)
      throws IOException {
    int packedLength = 0;
    for (Object o : value) {
      packedLength += getSerializedSizeNoTag(o, datatype);
    }
    output.writeTag(tag, WireType.LENGTH_DELIMITED);
    output.writeVarint32(packedLength);
    for (Object o : value) {
      writeValueNoTag(output, o, datatype);
    }
  }

  /**
   * Serializes a given {@link Message} and returns the results as a byte array.
   */
  byte[] toByteArray(M message) {
    byte[] result = new byte[getSerializedSize(message)];
    try {
      write(message, WireOutput.newInstance(result));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  /**
   * Returns a human-readable version of the given {@link Message}.
   */
  String toString(M message) {
    StringBuilder sb = new StringBuilder();
    sb.append(messageType.getSimpleName());
    sb.append("{");

    String sep = "";
    for (FieldInfo fieldInfo : getFields()) {
      Object value = getFieldValue(message, fieldInfo);
      if (value == null) {
        continue;
      }
      sb.append(sep);
      sep = ", ";
      sb.append(fieldInfo.name);
      sb.append("=");
      sb.append(value);
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
    return WireOutput.tagSize(tag) + getSerializedSizeNoTag(value, datatype);
  }

  /**
   * Returns the serialized size in bytes of the given value without any prepended tag or length,
   * e.g., as it would be written as part of a 'packed' repeated field.
   */
  private int getSerializedSizeNoTag(Object value, Datatype datatype) {
    switch (datatype) {
      case INT32: return WireOutput.int32Size((Integer) value);
      case INT64: case UINT64: return WireOutput.varint64Size((Long) value);
      case UINT32: return WireOutput.varint32Size((Integer) value);
      case SINT32: return WireOutput.varint32Size(WireOutput.zigZag32((Integer) value));
      case SINT64: return WireOutput.varint64Size(WireOutput.zigZag64((Long) value));
      case BOOL: return 1;
      case ENUM: return getEnumSize((Enum) value);
      case STRING:
        int utf8Length = utf8Length((String) value);
        return WireOutput.varint32Size(utf8Length) + utf8Length;
      case BYTES:
        int length = ((ByteString) value).size();
        return WireOutput.varint32Size(length) + length;
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
  private <E extends Enum> int getEnumSize(E value) {
    EnumAdapter<E> adapter = (EnumAdapter<E>) wire.enumAdapter(value.getClass());
    return WireOutput.varint32Size(adapter.toInt(value));
  }

  @SuppressWarnings("unchecked")
  private <M extends Message> int getMessageSize(M message) {
    int messageSize = message.getSerializedSize();
    return WireOutput.varint32Size(messageSize) + messageSize;
  }

  private void writeValue(WireOutput output, int tag, Object value, Datatype datatype)
    throws IOException {
    output.writeTag(tag, datatype.wireType());
    writeValueNoTag(output, value, datatype);
  }

  /**
   * Writes a value with no tag.
   */
  private void writeValueNoTag(WireOutput output, Object value, Datatype datatype)
      throws IOException {
    switch (datatype) {
      case INT32: output.writeSignedVarint32((Integer) value); break;
      case INT64: case UINT64: output.writeVarint64((Long) value); break;
      case UINT32: output.writeVarint32((Integer) value); break;
      case SINT32: output.writeVarint32(WireOutput.zigZag32((Integer) value)); break;
      case SINT64: output.writeVarint64(WireOutput.zigZag64((Long) value)); break;
      case BOOL: output.writeRawByte((Boolean) value ? 1 : 0); break;
      case ENUM: writeEnum((Enum) value, output); break;
      case STRING:
        final byte[] bytes = ((String) value).getBytes("UTF-8");
        output.writeVarint32(bytes.length);
        output.writeRawBytes(bytes);
        break;
      case BYTES:
        ByteString byteString = (ByteString) value;
        output.writeVarint32(byteString.size());
        output.writeRawBytes(byteString.toByteArray());
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
  private <M extends Message> void writeMessage(M message, WireOutput output) throws IOException {
    output.writeVarint32(message.getSerializedSize());
    MessageAdapter<M> adapter = wire.messageAdapter((Class<M>) message.getClass());
    adapter.write(message, output);
  }

  @SuppressWarnings("unchecked")
  private <E extends Enum> void writeEnum(E value, WireOutput output)
      throws IOException {
    EnumAdapter<E> adapter = (EnumAdapter<E>) wire.enumAdapter(value.getClass());
    output.writeVarint32(adapter.toInt(value));
  }

  // Reading

  /** Uses reflection to read an instance from {@code input}. */
  M read(WireInput input) throws IOException {
    try {
      Builder<M> builder = builderType.newInstance();
      Storage storage = new Storage();

      while (true) {
        Extension<?, ?> extension = null;
        int tagAndType = input.readTag();
        int tag = tagAndType >> WireType.TAG_TYPE_BITS;
        WireType wireType = WireType.valueOf(tagAndType);
        if (tag == 0) {
          // Set repeated fields
          for (int storedTag : storage.getTags()) {
            FieldInfo fieldInfo = fieldInfoMap.get(storedTag);
            if (fieldInfo != null) {
              setBuilderField(builder, storedTag, storage.get(storedTag));
            } else {
              setExtension((ExtendableBuilder<?>) builder, getExtension(storedTag),
                  storage.get(storedTag));
            }
          }
          return builder.build();
        }

        Datatype datatype;
        Label label;
        FieldInfo fieldInfo = fieldInfoMap.get(tag);
        if (fieldInfo != null) {
          datatype = fieldInfo.datatype;
          label = fieldInfo.label;
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
            storage.add(tag, value);
          }
          input.popLimit(oldLimit);
          if (input.getPosition() != start + length) {
            throw new IOException("Packed data had wrong length!");
          }
        } else {
          // Read a single value
          value = readValue(input, tag, datatype);
          if (label.isRepeated()) {
            storage.add(tag, value);
          } else if (extension != null) {
            setExtension((ExtendableBuilder<?>) builder, extension, value);
          } else {
            setBuilderField(builder, tag, value);
          }
        }
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  private Object readValue(WireInput input, int tag, Datatype datatype) throws IOException {
    switch (datatype) {
      case INT32: case UINT32: return input.readVarint32();
      case INT64: case UINT64: return input.readVarint64();
      case SINT32: return WireInput.decodeZigZag32(input.readVarint32());
      case SINT64: return WireInput.decodeZigZag64(input.readVarint64());
      case BOOL: return input.readVarint32() != 0;
      case ENUM:
        EnumAdapter<? extends Enum> adapter = wire.enumAdapter(getEnumClass(tag));
        return adapter.fromInt(input.readVarint32());
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

  private Message readMessage(WireInput input, int tag) throws IOException {
    final int length = input.readVarint32();
    if (input.recursionDepth >= WireInput.RECURSION_LIMIT) {
      throw new IOException("Wire recursion limit exceeded");
    }
    final int oldLimit = input.pushLimit(length);
    ++input.recursionDepth;
    MessageAdapter<? extends Message> adapter = wire.messageAdapter(getMessageClass(tag));
    Message message = adapter.read(input);
    input.checkLastTagWas(0);
    --input.recursionDepth;
    input.popLimit(oldLimit);
    return message;
  }

  @SuppressWarnings("unchecked")
  private Class<Message> getMessageClass(int tag) {
    FieldInfo fieldInfo = fieldInfoMap.get(tag);
    Class<Message> messageClass = fieldInfo == null
        ? null : (Class<Message>) fieldInfo.messageType;
    if (messageClass == null) {
      Extension<ExtendableMessage<?>, ?> extension = getExtension(tag);
      if (extension != null) {
        messageClass = (Class<Message>) extension.getMessageType();
      }
    }
    return messageClass;
  }

  private void readUnknownField(Builder builder, WireInput input, int tag, WireType type)
      throws IOException {
    switch (type) {
      case VARINT:
        builder.addVarint(tag, input.readVarint64());
        break;
      case FIXED32:
        builder.addFixed32(tag, input.readFixed32());
        break;
      case FIXED64:
        builder.addFixed64(tag, input.readFixed64());
        break;
      case LENGTH_DELIMITED:
        int length = input.readVarint32();
        builder.addLengthDelimited(tag, input.readBytes(length));
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
    private final Map<Integer, List<Object>> map = new LinkedHashMap<Integer, List<Object>>();

    void add(int tag, Object value) {
      List<Object> list = map.get(tag);
      if (list == null) {
        list = new ArrayList<Object>();
        map.put(tag, list);
      }
      list.add(value);
    }

    Set<Integer> getTags() {
      return map.keySet();
    }

    List<Object> get(int tag) {
      return map.get(tag);
    }
  }

  @SuppressWarnings("unchecked")
  private Extension<ExtendableMessage<?>, ?> getExtension(int tag) {
    ExtensionRegistry registry = wire.registry;
    return registry == null
        ? null : registry.getExtension((Class<ExtendableMessage<?>>) messageType, tag);
  }

  @SuppressWarnings("unchecked")
  Extension<ExtendableMessage<?>, ?> getExtension(String name) {
    ExtensionRegistry registry = wire.registry;
    return registry == null
        ? null : registry.getExtension((Class<ExtendableMessage<?>>) messageType, name);
  }

  @SuppressWarnings("unchecked")
  private void setExtension(ExtendableMessage.ExtendableBuilder builder, Extension<?, ?> extension,
      Object value) {
    builder.setExtension(extension, value);
  }

  private Class<? extends Enum> getEnumClass(int tag) {
    FieldInfo fieldInfo = fieldInfoMap.get(tag);
    Class<? extends Enum> enumType = fieldInfo == null ? null : fieldInfo.enumType;
    if (enumType == null) {
      Extension<ExtendableMessage<?>, ?> extension = getExtension(tag);
      if (extension != null) {
        enumType = extension.getEnumType();
      }
    }
    return enumType;
  }
}
