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
        typeMap.put(tag, annotation.label() | annotation.type()
            | (annotation.packed() ? Message.PACKED : 0));

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
      int type = typeFlags & Message.TYPE_MASK;

      if ((typeFlags & Message.LABEL_MASK) == Message.REPEATED) {
        if (isPacked(typeFlags)) {
          int packedLength = 0;
          for (Object o : (List<?>) value) {
            packedLength += getSerializedSizeNoTag(o, type);
          }
          // tag + length + value + value + ...
          size += WireOutput.varint32Size(
              WireOutput.makeTag(tag, WireOutput.WIRETYPE_LENGTH_DELIMITED));
          size += WireOutput.varint32Size(packedLength);
          size += packedLength;
        } else {
          for (Object o : (List<?>) value) {
            size += getSerializedSize(tag, o, type);
          }
        }
      } else {
        size += getSerializedSize(tag, value, type);
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

  private boolean isPacked(int typeFlags) {
    return (typeFlags & Message.PACKED_MASK) == Message.PACKED;
  }

  private <T extends ExtendableMessage<?>> int getExtensionsSerializedSize(
      ExtensionMap<T> map) {
    int size = 0;
    for (Extension<T, ?> extension : map.getExtensions()) {
      Object value = map.get(extension);
      int tag = extension.getTag();
      int type = extension.getType();
      if (extension.getLabel() == Message.REPEATED) {
        if (extension.getPacked()) {
          int packedLength = 0;
          for (Object o : (List<?>) value) {
            packedLength += getSerializedSizeNoTag(o, type);
          }
          size += WireOutput.varint32Size(
              WireOutput.makeTag(tag, WireOutput.WIRETYPE_LENGTH_DELIMITED));
          size += WireOutput.varint32Size(packedLength);
          size += packedLength;
        } else {
          for (Object o : (List<?>) value) {
            size += getSerializedSize(tag, o, type);
          }
        }
      } else {
        size += getSerializedSize(tag, value, type);
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
      int type = typeFlags & Message.TYPE_MASK;

      if ((typeFlags & Message.LABEL_MASK) == Message.REPEATED) {
        if (isPacked(typeFlags)) {
          int packedLength = 0;
          for (Object o : (List<?>) value) {
            packedLength += getSerializedSizeNoTag(o, type);
          }
          output.writeTag(tag, 2);
          output.writeVarint32(packedLength);
          for (Object o : (List<?>) value) {
            writeValueNoTag(output, o, type);
          }
        } else {
          for (Object o : (List<?>) value) {
            writeValue(output, tag, o, type);
          }
        }
      } else {
        writeValue(output, tag, value, type);
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
      int type = extension.getType();
      if (extension.getLabel() == Message.REPEATED) {
        if (extension.getPacked()) {
          int packedLength = 0;
          for (Object o : (List<?>) value) {
            packedLength += getSerializedSizeNoTag(o, type);
          }
          output.writeTag(tag, 2);
          output.writeVarint32(packedLength);
          for (Object o : (List<?>) value) {
            writeValueNoTag(output, o, type);
          }
        } else {
          for (Object o : (List<?>) value) {
            writeValue(output, tag, o, type);
          }
        }
      } else {
        writeValue(output, tag, value, type);
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
  private int getSerializedSize(int tag, Object value, int type) {
    return WireOutput.tagSize(tag) + getSerializedSizeNoTag(value, type);
  }

  /**
   * Returns the serialized size in bytes of the given value without any prepended tag or length,
   * e.g., as it would be written as part of a 'packed' repeated field.
   */
  private int getSerializedSizeNoTag(Object value, int type) {
    switch (type) {
      case Message.INT32: return WireOutput.int32Size((Integer) value);
      case Message.INT64: case Message.UINT64: return WireOutput.varint64Size((Long) value);
      case Message.UINT32: return WireOutput.varint32Size((Integer) value);
      case Message.SINT32: return WireOutput.varint32Size(WireOutput.zigZag32((Integer) value));
      case Message.SINT64: return WireOutput.varint64Size(WireOutput.zigZag64((Long) value));
      case Message.BOOL: return 1;
      case Message.ENUM: return getEnumSize((Enum) value);
      case Message.STRING:
        int utf8Length = utf8Length((String) value);
        return WireOutput.varint32Size(utf8Length) + utf8Length;
      case Message.BYTES:
        int length = ((ByteString) value).size();
        return WireOutput.varint32Size(length) + length;
      case Message.MESSAGE: return getMessageSize((Message) value);
      case Message.FIXED32: case Message.SFIXED32: case Message.FLOAT:
        return WireOutput.FIXED_32_SIZE;
      case Message.FIXED64: case Message.SFIXED64: case Message.DOUBLE:
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

  private void writeValue(WireOutput output, int tag, Object value, int type)
    throws IOException {
    int wiretype = wiretype(type);
    output.writeTag(tag, wiretype);
    writeValueNoTag(output, value, type);
  }

  private int wiretype(int type) {
    switch (type) {
      case Message.INT32: case Message.INT64: case Message.UINT32: case Message.UINT64:
      case Message.SINT32: case Message.SINT64: case Message.BOOL: case Message.ENUM:
        return WireOutput.WIRETYPE_VARINT;
      case Message.STRING: case Message.BYTES: case Message.MESSAGE:
        return WireOutput.WIRETYPE_LENGTH_DELIMITED;
      case Message.FIXED32: case Message.SFIXED32: case Message.FLOAT:
        return WireOutput.WIRETYPE_FIXED32;
      case Message.FIXED64: case Message.SFIXED64: case Message.DOUBLE:
        return WireOutput.WIRETYPE_FIXED64;
      default:
        throw new IllegalArgumentException("type=" + type);
    }
  }

  /**
   * Writes a value with no tag.
   */
  private void writeValueNoTag(WireOutput output, Object value, int type) throws IOException {
    switch (type) {
      case Message.INT32: output.writeSignedVarint32((Integer) value); break;
      case Message.INT64: case Message.UINT64: output.writeVarint64((Long) value); break;
      case Message.UINT32: output.writeVarint32((Integer) value); break;
      case Message.SINT32: output.writeVarint32(WireOutput.zigZag32((Integer) value)); break;
      case Message.SINT64: output.writeVarint64(WireOutput.zigZag64((Long) value)); break;
      case Message.BOOL: output.writeRawByte((Boolean) value ? 1 : 0); break;
      case Message.ENUM: writeEnum((Enum) value, output); break;
      case Message.STRING:
        final byte[] bytes = ((String) value).getBytes("UTF-8");
        output.writeVarint32(bytes.length);
        output.writeRawBytes(bytes);
        break;
      case Message.BYTES:
        ByteString byteString = (ByteString) value;
        output.writeVarint32(byteString.size());
        output.writeRawBytes(byteString.toByteArray());
        break;
      case Message.MESSAGE: writeMessage((Message) value, output); break;
      case Message.FIXED32: case Message.SFIXED32: output.writeFixed32((Integer) value); break;
      case Message.FIXED64: case Message.SFIXED64: output.writeFixed64((Long) value); break;
      case Message.FLOAT: output.writeFixed32(Float.floatToIntBits((Float) value)); break;
      case Message.DOUBLE: output.writeFixed64(Double.doubleToLongBits((Double) value)); break;
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

        int type;
        int label;
        boolean packed;
        if (typeMap.containsKey(tag)) {
          type = typeMap.get(tag);
          label = type & Message.LABEL_MASK;
          packed = (type & Message.PACKED_MASK) == Message.PACKED;
          type &= Message.TYPE_MASK;
        } else {
          extension = getExtension(tag);
          if (extension == null) {
            readUnknownField(builder, input, tag, wireType);
            continue;
          }
          type = extension.getType();
          label = extension.getLabel();
          packed = extension.getPacked();
        }
        Object value;

        if (label == Message.REPEATED && packed && wireType == 2) {
          // Decode packed format
          int length = input.readVarint32();
          int start = input.getPosition();
          int oldLimit = input.pushLimit(length);
          while (input.getPosition() < start + length) {
            value = readValue(input, tag, type);
            storage.add(tag, value);
          }
          input.popLimit(oldLimit);
          if (input.getPosition() != start + length) {
            throw new IOException("Packed data had wrong length!");
          }
        } else {
          // Read a single value
          value = readValue(input, tag, type);
          if (label == Message.REPEATED) {
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

  private Object readValue(WireInput input, int tag, int type) throws IOException {
    switch (type) {
      case Message.INT32: case Message.UINT32: return input.readVarint32();
      case Message.INT64: case Message.UINT64: return input.readVarint64();
      case Message.SINT32: return WireInput.decodeZigZag32(input.readVarint32());
      case Message.SINT64: return WireInput.decodeZigZag64(input.readVarint64());
      case Message.BOOL: return input.readVarint32() != 0;
      case Message.ENUM:
        EnumAdapter<? extends Enum> adapter = wire.enumAdapter(getEnumClass(tag));
        return adapter.fromInt(input.readVarint32());
      case Message.STRING: return input.readString();
      case Message.BYTES: return input.readBytes();
      case Message.MESSAGE: return readMessage(input, tag);
      case Message.FIXED32: case Message.SFIXED32: return input.readFixed32();
      case Message.FIXED64: case Message.SFIXED64: return input.readFixed64();
      case Message.FLOAT: return Float.intBitsToFloat(input.readFixed32());
      case Message.DOUBLE: return Double.longBitsToDouble(input.readFixed64());
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
      Extension<ExtendableMessage, ?> extension = getExtension(tag);
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
  private Extension<ExtendableMessage, ?> getExtension(int tag) {
    ExtensionRegistry registry = wire.registry;
    return registry == null
        ? null : registry.getExtension((Class<ExtendableMessage>) messageType, tag);
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
      Extension<ExtendableMessage, ?> extension = getExtension(tag);
      if (extension != null) {
        enumType = extension.getEnumType();
      }
    }
    return enumType;
  }
}
