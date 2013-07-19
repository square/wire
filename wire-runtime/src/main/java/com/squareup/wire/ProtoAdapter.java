// Copyright 2013 Square, Inc.
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
public class ProtoAdapter<M extends Message> {

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
  @SuppressWarnings("unchecked")
  ProtoAdapter(Wire wire, Class<M> messageType) {
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
            | (annotation.packed() ? Wire.PACKED : 0));

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
   * Returns an instance of the message type of this {@link ProtoAdapter} with all fields unset.
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
  public int getSerializedSize(M instance) {
    if (instance.cachedSerializedSize >= 0) {
      return instance.cachedSerializedSize;
    }
    int size = 0;
    for (int tag : tags) {
      Field field = fieldMap.get(tag);
      if (field == null) {
        throw new IllegalArgumentException();
      }
      Object value;
      try {
        value = field.get(instance);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
      if (value == null) {
        continue;
      }
      int typeFlags = typeMap.get(tag);
      int type = typeFlags & Wire.TYPE_MASK;

      if ((typeFlags & Wire.LABEL_MASK) == Wire.REPEATED) {
        boolean packed = (typeFlags & Wire.PACKED_MASK) == Wire.PACKED;
        if (packed) {
          int len = 0;
          for (Object o : (List<?>) value) {
            len += getSerializedSizeNoTag(o, type);
          }
          // tag + length + value + value + ...
          size += WireOutput.varint32Size(
              WireOutput.makeTag(tag, WireOutput.WIRETYPE_LENGTH_DELIMITED));
          size += WireOutput.varint32Size(len);
          size += len;
        } else {
          for (Object o : (List<?>) value) {
            size += getSerializedSize(tag, o, type);
          }
        }
      } else {
        size += getSerializedSize(tag, value, type);
      }
    }

    if (instance instanceof ExtendableMessage) {
      ExtendableMessage message = (ExtendableMessage) instance;
      if (message.extensionMap != null) {
        size += getExtensionsSerializedSize(message.extensionMap);
      }
    }
    size += instance.unknownFieldMap.getSerializedSize();
    instance.cachedSerializedSize = size;
    return size;
  }

  private <T extends ExtendableMessage<?>> int getExtensionsSerializedSize(
      ExtensionMap<T> map) {
    int size = 0;
    for (Extension<T, ?> extension : map.getExtensions()) {
      Object value = map.get(extension);
      int tag = extension.getTag();
      int type = extension.getType();
      if (extension.getLabel() == Wire.REPEATED) {
        if (extension.getPacked()) {
          int len = 0;
          for (Object o : (List<?>) value) {
            len += getSerializedSizeNoTag(o, type);
          }
          size += WireOutput.varint32Size(
              WireOutput.makeTag(tag, WireOutput.WIRETYPE_LENGTH_DELIMITED));
          size += WireOutput.varint32Size(len);
          size += len;
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

  /** Uses reflection to write {@code instance} to {@code output} in serialized form. */
  public void write(M instance, WireOutput output) throws IOException {
    for (int tag : tags) {
      Field field = fieldMap.get(tag);
      if (field == null) {
        throw new IllegalArgumentException();
      }
      Object value;
      try {
        value = field.get(instance);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
      if (value == null) {
        continue;
      }
      int typeFlags = typeMap.get(tag);
      int type = typeFlags & Wire.TYPE_MASK;

      if ((typeFlags & Wire.LABEL_MASK) == Wire.REPEATED) {
        boolean packed = (typeFlags & Wire.PACKED_MASK) == Wire.PACKED;
        if (packed) {
          int len = 0;
          for (Object o : (List<?>) value) {
            len += getSerializedSizeNoTag(o, type);
          }
          output.writeTag(tag, 2);
          output.writeVarint32(len);
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

    if (instance instanceof ExtendableMessage) {
      ExtendableMessage message = (ExtendableMessage) instance;
      if (message.extensionMap != null) {
        writeExtensions(output, message.extensionMap);
      }
    }
    instance.unknownFieldMap.write(output);
  }

  private <T extends ExtendableMessage<?>> void writeExtensions(WireOutput output,
      ExtensionMap<T> extensionMap) throws IOException {
    for (Extension<T, ?> extension: extensionMap.getExtensions()) {
      Object value = extensionMap.get(extension);
      int tag = extension.getTag();
      int type = extension.getType();
      if (extension.getLabel() == Wire.REPEATED) {
        if (extension.getPacked()) {
          int len = 0;
          for (Object o : (List<?>) value) {
            len += getSerializedSizeNoTag(o, type);
          }
          output.writeTag(tag, 2);
          output.writeVarint32(len);
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
   * Serializes a given {@link Message} instance and returns the results as a byte array.
   */
  public byte[] toByteArray(M message) {
    byte[] result = new byte[getSerializedSize(message)];
    try {
      write(message, WireOutput.newInstance(result));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return result;
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
      case Wire.INT32: return WireOutput.int32Size((Integer) value);
      case Wire.INT64: case Wire.UINT64: return WireOutput.varint64Size((Long) value);
      case Wire.UINT32: return WireOutput.varint32Size((Integer) value);
      case Wire.SINT32: return WireOutput.varint32Size(WireOutput.zigZag32((Integer) value));
      case Wire.SINT64: return WireOutput.varint64Size(WireOutput.zigZag64((Long) value));
      case Wire.BOOL: return 1;
      case Wire.ENUM: return getEnumSize((Enum) value);
      case Wire.STRING:
        int utf8Length = utf8Length((String) value);
        return WireOutput.varint32Size(utf8Length) + utf8Length;
      case Wire.BYTES:
        int length = ((ByteString) value).size();
        return WireOutput.varint32Size(length) + length;
      case Wire.MESSAGE: return getMessageSize((Message) value);
      case Wire.FIXED32: case Wire.SFIXED32: case Wire.FLOAT: return WireOutput.FIXED_32_SIZE;
      case Wire.FIXED64: case Wire.SFIXED64: case Wire.DOUBLE: return WireOutput.FIXED_64_SIZE;
      default: throw new RuntimeException();
    }
  }

  private int utf8Length(String s) {
    int count = 0;
    for (int i = 0, len = s.length(); i < len; i++) {
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
    ProtoEnumAdapter<E> adapter = (ProtoEnumAdapter<E>) wire.enumAdapter(value.getClass());
    return WireOutput.varint32Size(adapter.toInt(value));
  }

  @SuppressWarnings("unchecked")
  private <M extends Message> int getMessageSize(M message) {
    ProtoAdapter<M> adapter = wire.messageAdapter((Class<M>) message.getClass());
    int messageSize = adapter.getSerializedSize(message);
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
      case Wire.INT32: case Wire.INT64: case Wire.UINT32: case Wire.UINT64:
      case Wire.SINT32: case Wire.SINT64: case Wire.BOOL: case Wire.ENUM:
        return WireOutput.WIRETYPE_VARINT;
      case Wire.STRING: case Wire.BYTES: case Wire.MESSAGE:
        return WireOutput.WIRETYPE_LENGTH_DELIMITED;
      case Wire.FIXED32: case Wire.SFIXED32: case Wire.FLOAT:
        return WireOutput.WIRETYPE_FIXED32;
      case Wire.FIXED64: case Wire.SFIXED64: case Wire.DOUBLE:
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
      case Wire.INT32: output.writeSignedVarint32((Integer) value); break;
      case Wire.INT64: case Wire.UINT64: output.writeVarint64((Long) value); break;
      case Wire.UINT32: output.writeVarint32((Integer) value); break;
      case Wire.SINT32: output.writeVarint32(WireOutput.zigZag32((Integer) value)); break;
      case Wire.SINT64: output.writeVarint64(WireOutput.zigZag64((Long) value)); break;
      case Wire.BOOL: output.writeRawByte((Boolean) value ? 1 : 0); break;
      case Wire.ENUM: writeEnum((Enum) value, output); break;
      case Wire.STRING:
        final byte[] bytes = ((String) value).getBytes("UTF-8");
        output.writeVarint32(bytes.length);
        output.writeRawBytes(bytes);
        break;
      case Wire.BYTES:
        ByteString byteString = (ByteString) value;
        output.writeVarint32(byteString.size());
        output.writeRawBytes(byteString.toByteArray());
        break;
      case Wire.MESSAGE: writeMessage((Message) value, output); break;
      case Wire.FIXED32: case Wire.SFIXED32: output.writeFixed32((Integer) value); break;
      case Wire.FIXED64: case Wire.SFIXED64: output.writeFixed64((Long) value); break;
      case Wire.FLOAT: output.writeFixed32(Float.floatToIntBits((Float) value)); break;
      case Wire.DOUBLE: output.writeFixed64(Double.doubleToLongBits((Double) value)); break;
      default: throw new RuntimeException();
    }
  }

  @SuppressWarnings("unchecked")
  private <M extends Message> void writeMessage(M message, WireOutput output) throws IOException {
    ProtoAdapter<M> adapter = wire.messageAdapter((Class<M>) message.getClass());
    output.writeVarint32(adapter.getSerializedSize(message));
    adapter.write(message, output);
  }

  @SuppressWarnings("unchecked")
  private <E extends Enum> void writeEnum(E value, WireOutput output)
      throws IOException {
    ProtoEnumAdapter<E> adapter = (ProtoEnumAdapter<E>) wire.enumAdapter(value.getClass());
    output.writeVarint32(adapter.toInt(value));
  }

  // Reading

  /** Uses reflection to read an instance from {@code input}. */
  public M read(WireInput input) throws IOException {
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
          label = type & Wire.LABEL_MASK;
          packed = (type & Wire.PACKED_MASK) == Wire.PACKED;
          type &= Wire.TYPE_MASK;
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

        if (label == Wire.REPEATED && packed && wireType == 2) {
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
          if (label == Wire.REPEATED) {
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
      case Wire.INT32: case Wire.UINT32: return input.readVarint32();
      case Wire.INT64: case Wire.UINT64: return input.readVarint64();
      case Wire.SINT32: return WireInput.decodeZigZag32(input.readVarint32());
      case Wire.SINT64: return WireInput.decodeZigZag64(input.readVarint64());
      case Wire.BOOL: return input.readVarint32() != 0;
      case Wire.ENUM: return Wire.enumFromInt(getEnumClass(tag), input.readVarint32());
      case Wire.STRING: return input.readString();
      case Wire.BYTES: return input.readBytes();
      case Wire.MESSAGE: return readMessage(input, tag);
      case Wire.FIXED32: case Wire.SFIXED32: return input.readFixed32();
      case Wire.FIXED64: case Wire.SFIXED64: return input.readFixed64();
      case Wire.FLOAT: return Float.intBitsToFloat(input.readFixed32());
      case Wire.DOUBLE: return Double.longBitsToDouble(input.readFixed64());
      default: throw new RuntimeException();
    }
  }

  private Message readMessage(WireInput input, int tag) throws IOException {
    final int length = input.readVarint32();
    if (input.recursionDepth >= input.recursionLimit) {
      throw new IOException("Wire recursion limit exceeded");
    }
    final int oldLimit = input.pushLimit(length);
    ++input.recursionDepth;
    ProtoAdapter<? extends Message> adapter = wire.messageAdapter(getMessageClass(tag));
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

    public void add(int tag, Object value) {
      List<Object> list = map.get(tag);
      if (list == null) {
        list = new ArrayList<Object>();
        map.put(tag, list);
      }
      list.add(value);
    }

    public Set<Integer> getTags() {
      return map.keySet();
    }

    public List<Object> get(int tag) {
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
