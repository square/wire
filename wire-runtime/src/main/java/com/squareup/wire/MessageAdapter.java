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
import java.util.Collections;
import java.util.HashMap;
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
class MessageAdapter<M extends Message> {

  private final Wire wire;
  private final Class<M> messageType;
  private final Class<Builder<M>> builderType;
  private M defaultInstance;

  private final List<Integer> tags = new ArrayList<Integer>();
  private final Map<Integer, Integer> typeMap = new HashMap<Integer, Integer>();
  private final Map<Integer, Class<? extends Message>> messageTypeMap =
      new HashMap<Integer, Class<? extends Message>>();
  private final Map<Integer, Class<? extends Enum>> enumTypeMap =
      new HashMap<Integer, Class<? extends Enum>>();
  private final Map<Integer, Field> fieldMap = new HashMap<Integer, Field>();
  private final Map<Integer, Method> builderMethodMap = new HashMap<Integer, Method>();

  /** Cache information about the Message class and its mapping to proto wire format. */
  @SuppressWarnings("unchecked") MessageAdapter(Wire wire, Class<M> messageType) {
    this.wire = wire;
    this.messageType = messageType;
    try {
      this.builderType = (Class<Message.Builder<M>>) Class.forName(messageType.getName()
          + "$Builder");
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("No builder class found for message type "
          + messageType.getName());
    }

    for (Field field : messageType.getDeclaredFields()) {
      // Process fields annotated with '@ProtoField'
      if (field.isAnnotationPresent(ProtoField.class)) {
        ProtoField annotation = field.getAnnotation(ProtoField.class);
        int tag = annotation.tag();

        tags.add(tag);
        fieldMap.put(tag, field);
        typeMap.put(tag, annotation.label().value() | annotation.type().value());

        // Record setter methods on the builder class
        try {
          Method method = builderType.getMethod(field.getName(), field.getType());
          builderMethodMap.put(tag, method);
        } catch (NoSuchMethodException e) {
          throw new IllegalArgumentException("No builder method "
              + builderType.getName() + "." + field.getName() + "(" + field.getType() + ")");
        }

        // Record type for tags that store a Message
        Class<Message> fieldAsMessage = getMessageType(field);
        if (fieldAsMessage != null) {
          messageTypeMap.put(tag, fieldAsMessage);
          continue;
        }

        // Record type for tags that store an Enum
        Class<Enum> fieldAsEnum = getEnumType(field);
        if (fieldAsEnum != null) {
          enumTypeMap.put(tag, fieldAsEnum);
        }
      }
    }

    // Sort tags so we can process them in order
    Collections.sort(tags);
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

  // Writing

  /**
   * Returns the serialized size of a given message, in bytes.
   */
  int getSerializedSize(M message) {
    int size = 0;
    for (int tag : tags) {
      Field field = fieldMap.get(tag);
      if (field == null) {
        throw new IllegalArgumentException();
      }
      Object value;
      try {
        value = field.get(message);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
      if (value == null) {
        continue;
      }
      int typeFlags = typeMap.get(tag);
      Datatype datatype = Datatype.valueOf(typeFlags);
      Label label = Label.valueOf(typeFlags);

      if (label.isRepeated()) {
        if (label.isPacked()) {
          int packedLength = 0;
          for (Object o : (List<?>) value) {
            packedLength += getSerializedSizeNoTag(o, datatype);
          }
          // tag + length + value + value + ...
          size += WireOutput.varint32Size(
              WireOutput.makeTag(tag, WireOutput.WIRETYPE_LENGTH_DELIMITED));
          size += WireOutput.varint32Size(packedLength);
          size += packedLength;
        } else {
          for (Object o : (List<?>) value) {
            size += getSerializedSize(tag, o, datatype);
          }
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
    size += message.unknownFieldMap.getSerializedSize();
    return size;
  }

  private <T extends ExtendableMessage<?>> int getExtensionsSerializedSize(
      ExtensionMap<T> map) {
    int size = 0;
    for (Extension<T, ?> extension : map.getExtensions()) {
      Object value = map.get(extension);
      int tag = extension.getTag();
      Datatype datatype = extension.getDatatype();
      Label label = extension.getLabel();
      if (label.isRepeated()) {
        if (label.isPacked()) {
          int packedLength = 0;
          for (Object o : (List<?>) value) {
            packedLength += getSerializedSizeNoTag(o, datatype);
          }
          size += WireOutput.varint32Size(
              WireOutput.makeTag(tag, WireOutput.WIRETYPE_LENGTH_DELIMITED));
          size += WireOutput.varint32Size(packedLength);
          size += packedLength;
        } else {
          for (Object o : (List<?>) value) {
            size += getSerializedSize(tag, o, datatype);
          }
        }
      } else {
        size += getSerializedSize(tag, value, datatype);
      }
    }
    return size;
  }

  /** Uses reflection to write {@code message} to {@code output} in serialized form. */
  void write(M message, WireOutput output) throws IOException {
    for (int tag : tags) {
      Field field = fieldMap.get(tag);
      if (field == null) {
        throw new IllegalArgumentException();
      }
      Object value;
      try {
        value = field.get(message);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
      if (value == null) {
        continue;
      }
      int typeFlags = typeMap.get(tag);
      Datatype datatype = Datatype.valueOf(typeFlags);
      Label label = Label.valueOf(typeFlags);

      if (label.isRepeated()) {
        if (label.isPacked()) {
          int packedLength = 0;
          for (Object o : (List<?>) value) {
            packedLength += getSerializedSizeNoTag(o, datatype);
          }
          output.writeTag(tag, 2);
          output.writeVarint32(packedLength);
          for (Object o : (List<?>) value) {
            writeValueNoTag(output, o, datatype);
          }
        } else {
          for (Object o : (List<?>) value) {
            writeValue(output, tag, o, datatype);
          }
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
    message.unknownFieldMap.write(output);
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
          int packedLength = 0;
          for (Object o : (List<?>) value) {
            packedLength += getSerializedSizeNoTag(o, datatype);
          }
          output.writeTag(tag, 2);
          output.writeVarint32(packedLength);
          for (Object o : (List<?>) value) {
            writeValueNoTag(output, o, datatype);
          }
        } else {
          for (Object o : (List<?>) value) {
            writeValue(output, tag, o, datatype);
          }
        }
      } else {
        writeValue(output, tag, value, datatype);
      }
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
    for (int tag : tags) {
      Field field = fieldMap.get(tag);
      if (field == null) {
        throw new AssertionError();
      }
      Object value;
      try {
        value = field.get(message);
      } catch (IllegalAccessException e) {
        throw new AssertionError(e);
      }
      if (value == null) {
        continue;
      }
      sb.append(sep);
      sep = ",";
      sb.append(field.getName());
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
        return WireOutput.FIXED_32_SIZE;
      case FIXED64: case SFIXED64: case DOUBLE:
        return WireOutput.FIXED_64_SIZE;
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
    int wiretype = wiretype(datatype);
    output.writeTag(tag, wiretype);
    writeValueNoTag(output, value, datatype);
  }

  private int wiretype(Datatype datatype) {
    switch (datatype) {
      case INT32: case INT64: case UINT32: case UINT64:
      case SINT32: case SINT64: case BOOL: case ENUM:
        return WireOutput.WIRETYPE_VARINT;
      case STRING: case BYTES: case MESSAGE:
        return WireOutput.WIRETYPE_LENGTH_DELIMITED;
      case FIXED32: case SFIXED32: case FLOAT:
        return WireOutput.WIRETYPE_FIXED32;
      case FIXED64: case SFIXED64: case DOUBLE:
        return WireOutput.WIRETYPE_FIXED64;
      default:
        throw new IllegalArgumentException("datatype=" + datatype);
    }
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
        int tag = tagAndType >> WireOutput.TAG_TYPE_BITS;
        int wireType = tagAndType & WireOutput.TAG_TYPE_MASK;
        if (tag == 0) {
          // Set repeated fields
          for (int storedTag : storage.getTags()) {
            if (typeMap.containsKey(storedTag)) {
              set(builder, storedTag, storage.get(storedTag));
            } else {
              setExtension((ExtendableBuilder<?>) builder, getExtension(storedTag),
                  storage.get(storedTag));
            }
          }
          return builder.build();
        }

        Datatype datatype;
        Label label;
        if (typeMap.containsKey(tag)) {
          int typeFlags = typeMap.get(tag);
          datatype = Datatype.valueOf(typeFlags);
          label = Label.valueOf(typeFlags);
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

        if (label.isPacked() && wireType == 2) {
          // Decode packed format
          int length = input.readVarint32();
          int start = input.getPosition();
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
            set(builder, tag, value);
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
    Class<Message> messageClass = (Class<Message>) messageTypeMap.get(tag);
    if (messageClass == null) {
      Extension<ExtendableMessage<?>, ?> extension = getExtension(tag);
      if (extension != null) {
        messageClass = (Class<Message>) extension.getMessageType();
      }
    }
    return messageClass;
  }

  private void readUnknownField(Builder builder, WireInput input,
      int tag, int type) throws IOException {
    switch (type) {
      case WireOutput.WIRETYPE_VARINT:
        builder.addVarint(tag, input.readVarint64());
        break;
      case WireOutput.WIRETYPE_FIXED64:
        builder.addFixed64(tag, input.readFixed64());
        break;
      case WireOutput.WIRETYPE_LENGTH_DELIMITED:
        int length = input.readVarint32();
        builder.addLengthDelimited(tag, ByteString.of(input.readRawBytes(length)));
        break;
      case WireOutput.WIRETYPE_START_GROUP:
        int groupLength = input.readVarint32();
        builder.addGroup(tag, ByteString.of(input.readRawBytes(groupLength)));
        break;
      case WireOutput.WIRETYPE_END_GROUP:
        break;
      case WireOutput.WIRETYPE_FIXED32:
        builder.addFixed32(tag, input.readFixed32());
        break;
      default: throw new RuntimeException();
    }
  }

  private static class Storage {
    private final Map<Integer, List<Object>> map = new HashMap<Integer, List<Object>>();

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

  private void set(Builder builder, int tag, Object value) {
    try {
      builderMethodMap.get(tag).invoke(builder, value);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  // TODO - improve type safety
  @SuppressWarnings("unchecked")
  private void setExtension(ExtendableMessage.ExtendableBuilder builder, Extension<?, ?> extension,
      Object value) {
    builder.setExtension(extension, value);
  }

  private Class<? extends Enum> getEnumClass(int tag) {
    Class<? extends Enum> enumType = enumTypeMap.get(tag);
    if (enumType == null) {
      Extension<ExtendableMessage<?>, ?> extension = getExtension(tag);
      if (extension != null) {
        enumType = extension.getEnumType();
      }
    }
    return enumType;
  }
}
