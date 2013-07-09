// Copyright 2013 Square, Inc.
package com.squareup.omar;

import com.google.protobuf.nano.CodedInputByteBufferNano;
import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
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

import static com.squareup.omar.Message.ExtendableMessage.Extension;

/**
 * An adapter than can perform I/O on a given Message type.
 *
 * @param <M> the Message class handled by this adapter.
 */
public class ProtoAdapter<M extends Message> {
  private final Omar omar;
  private final Class<M> messageType;
  private final Class<Message.Builder<M>> builderType;
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
  ProtoAdapter(Omar omar, Class <M> messageType) {
    this.omar = omar;
    this.messageType = messageType;
    try {
      this.builderType =
          (Class<Message.Builder<M>>) Class.forName(messageType.getName() + "$Builder");
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("No builder class found for message type " +
          messageType.getName());
    }

    for (Field field : messageType.getDeclaredFields()) {
      // Process fields annotated with '@ProtoField'
      if (field.isAnnotationPresent(ProtoField.class)) {
        ProtoField annotation = field.getAnnotation(ProtoField.class);
        int tag = annotation.tag();

        tags.add(tag);
        fieldMap.put(tag, field);
        typeMap.put(tag, annotation.label() | annotation.type() |
            (annotation.packed() ? Omar.PACKED : 0));

        // Record setter methods on the builder class
        try {
          Method method = builderType.getMethod(field.getName(), field.getType());
          builderMethodMap.put(tag, method);
        } catch (NoSuchMethodException e) {
          throw new IllegalArgumentException("No builder method " +
              builderType.getName() + "." + field.getName() + "(" + field.getType() + ")");
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
      int type = typeFlags & Omar.TYPE_MASK;

      if ((typeFlags & Omar.LABEL_MASK) == Omar.REPEATED) {
        boolean packed = (typeFlags & Omar.PACKED_MASK) == Omar.PACKED;
        if (packed) {
          int len = 0;
          for (Object o : (List<?>) value) {
            len += getSerializedSizeNoTag(o, type);
          }
          // tag + length + value + value + ...
          size += CodedOutputByteBufferNano.computeRawVarint32Size((tag << 3) | 2);
          size += CodedOutputByteBufferNano.computeRawVarint32Size(len);
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

    if (instance instanceof Message.ExtendableMessage) {
      Message.ExtendableMessage extendableMessage = (Message.ExtendableMessage) instance;
      try {
        Field extensionMap = extendableMessage.getClass().getField("extensionMap");
        Map<Extension<?, ?>, ?> map = (Map<Extension<?, ?>, ?>) extensionMap.get(instance);
        for (Map.Entry<Extension<?, ?>, ?> entry: map.entrySet()) {
          Extension<?, ?> extension = entry.getKey();
          Object value = entry.getValue();
          int tag = extension.getTag();
          int type = extension.getType();
          if (extension.getLabel() == Omar.REPEATED) {
            if (extension.getPacked()) {
              int len = 0;
              for (Object o : (List<?>) value) {
                len += getSerializedSizeNoTag(o, type);
              }
              size += CodedOutputByteBufferNano.computeRawVarint32Size((tag << 3) | 2);
              size += CodedOutputByteBufferNano.computeRawVarint32Size(len);
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
      } catch (NoSuchFieldException e) {
        throw new RuntimeException("Can't access extensionMap in " + instance);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Can't access extensionMap in " + instance);
      }
    }

    return size;
  }

  /** Uses reflection to write {@code instance} to {@code out}. */
  public void write(M instance, CodedOutputByteBufferNano output) throws IOException {
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
      int type = typeFlags & Omar.TYPE_MASK;

      if ((typeFlags & Omar.LABEL_MASK) == Omar.REPEATED) {
        boolean packed = (typeFlags & Omar.PACKED_MASK) == Omar.PACKED;
        if (packed) {
          int len = 0;
          for (Object o : (List<?>) value) {
            len += getSerializedSizeNoTag(o, type);
          }
          output.writeTag(tag, 2);
          output.writeRawVarint32(len);
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

    if (instance instanceof Message.ExtendableMessage) {
      Message.ExtendableMessage extendableMessage = (Message.ExtendableMessage) instance;
      try {
        Field extensionMap = extendableMessage.getClass().getField("extensionMap");
        Map<Extension<?, ?>, ?> map = (Map<Extension<?, ?>, ?>) extensionMap.get(instance);
        for (Map.Entry<Extension<?, ?>, ?> entry: map.entrySet()) {
           Extension<?, ?> extension = entry.getKey();
           Object value = entry.getValue();
          int tag = extension.getTag();
          int type = extension.getType();
          if (extension.getLabel() == Omar.REPEATED) {
            if (extension.getPacked()) {
              int len = 0;
              for (Object o : (List<?>) value) {
                len += getSerializedSizeNoTag(o, type);
              }
              output.writeTag(tag, 2);
              output.writeRawVarint32(len);
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
      } catch (NoSuchFieldException e) {
        throw new RuntimeException("Can't access extensionMap in " + instance);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Can't access extensionMap in " + instance);
      }
    }
  }

  /**
   * Serializes a given {@link Message} instance and returns the results as a byte array.
   */
  public byte[] toByteArray(M message) {
    byte[] result = new byte[getSerializedSize(message)];
    try {
      write(message, CodedOutputByteBufferNano.newInstance(result));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  private int getSerializedSize(int tag, Object value, int type) {
    switch (type) {
      case Omar.INT32: return CodedOutputByteBufferNano.computeInt32Size(tag, (Integer) value);
      case Omar.INT64: return CodedOutputByteBufferNano.computeInt64Size(tag, (Long) value);
      case Omar.UINT32: return CodedOutputByteBufferNano.computeUInt32Size(tag, (Integer) value);
      case Omar.UINT64: return CodedOutputByteBufferNano.computeUInt64Size(tag, (Long) value);
      case Omar.SINT32: return CodedOutputByteBufferNano.computeSInt32Size(tag, (Integer) value);
      case Omar.SINT64: return CodedOutputByteBufferNano.computeSInt64Size(tag, (Long) value);
      case Omar.BOOL: return CodedOutputByteBufferNano.computeBoolSize(tag, (Boolean) value);
      case Omar.ENUM: return getEnumSize((Enum) value, tag);
      case Omar.STRING: return CodedOutputByteBufferNano.computeStringSize(tag, (String) value);
      case Omar.BYTES: return CodedOutputByteBufferNano.computeBytesSize(tag, (byte[]) value);
      case Omar.MESSAGE: return getMessageSize((Message) value, tag);
      case Omar.FIXED32: return CodedOutputByteBufferNano.computeFixed32Size(tag, (Integer) value);
      case Omar.SFIXED32: return CodedOutputByteBufferNano.computeSFixed32Size(tag,  (Integer) value);
      case Omar.FIXED64: return CodedOutputByteBufferNano.computeFixed64Size(tag, (Long) value);
      case Omar.SFIXED64: return CodedOutputByteBufferNano.computeSFixed64Size(tag, (Long) value);
      case Omar.FLOAT: return CodedOutputByteBufferNano.computeFloatSize(tag, (Float) value);
      case Omar.DOUBLE: return CodedOutputByteBufferNano.computeDoubleSize(tag, (Double) value);
      default: throw new RuntimeException();
    }
  }

  private <E extends Enum> int getEnumSize(E value, int tag) {
    ProtoEnumAdapter<E> adapter = (ProtoEnumAdapter<E>) omar.enumAdapter(value.getClass());
    return CodedOutputByteBufferNano.computeEnumSize(tag, adapter.toInt(value));
  }

  private <M extends Message> int getMessageSize(M message, int tag) {
    ProtoAdapter<M> adapter = omar.messageAdapter((Class<M>) message.getClass());
    int messageSize = adapter.getSerializedSize(message);
    int size = CodedOutputByteBufferNano.computeTagSize(tag);
    size += CodedOutputByteBufferNano.computeRawVarint32Size(messageSize) + messageSize;
    return size;
  }

  private int getSerializedSizeNoTag(Object value, int type) {
    switch (type) {
      case Omar.INT32: return CodedOutputByteBufferNano.computeInt32SizeNoTag((Integer) value);
      case Omar.INT64: return CodedOutputByteBufferNano.computeInt64SizeNoTag((Long) value);
      case Omar.UINT32: return CodedOutputByteBufferNano.computeUInt32SizeNoTag((Integer) value);
      case Omar.UINT64: return CodedOutputByteBufferNano.computeUInt64SizeNoTag((Long) value);
      case Omar.SINT32: return CodedOutputByteBufferNano.computeSInt32SizeNoTag((Integer) value);
      case Omar.SINT64: return CodedOutputByteBufferNano.computeSInt64SizeNoTag((Long) value);
      case Omar.BOOL: return CodedOutputByteBufferNano.computeBoolSizeNoTag((Boolean) value);
      case Omar.ENUM: return getEnumSizeNoTag((Enum) value);
      case Omar.STRING: return CodedOutputByteBufferNano.computeStringSizeNoTag((String) value);
      case Omar.BYTES: return CodedOutputByteBufferNano.computeBytesSizeNoTag((byte[]) value);
      case Omar.MESSAGE: return getMessageSizeNoTag((Message) value);
      case Omar.FIXED32: return CodedOutputByteBufferNano.computeFixed32SizeNoTag((Integer) value);
      case Omar.SFIXED32: return CodedOutputByteBufferNano.computeSFixed32SizeNoTag((Integer) value);
      case Omar.FIXED64: return CodedOutputByteBufferNano.computeFixed64SizeNoTag((Long) value);
      case Omar.SFIXED64: return CodedOutputByteBufferNano.computeSFixed64SizeNoTag((Long) value);
      case Omar.FLOAT: return CodedOutputByteBufferNano.computeFloatSizeNoTag((Float) value);
      case Omar.DOUBLE: return CodedOutputByteBufferNano.computeDoubleSizeNoTag((Double) value);
      default: throw new RuntimeException();
    }
  }

  private <E extends Enum> int getEnumSizeNoTag(E value) {
    ProtoEnumAdapter<E> adapter = (ProtoEnumAdapter<E>) omar.enumAdapter(value.getClass());
    return CodedOutputByteBufferNano.computeEnumSizeNoTag(adapter.toInt(value));
  }

  private <M extends Message> int getMessageSizeNoTag(M message) {
    ProtoAdapter<M> adapter = omar.messageAdapter((Class<M>) message.getClass());
    return adapter.getSerializedSize(message);
  }

  private void writeValue(CodedOutputByteBufferNano output, int tag, Object value, int type)
    throws IOException {
    switch (type) {
      case Omar.INT32: output.writeInt32(tag, (Integer) value); break;
      case Omar.INT64: output.writeInt64(tag, (Long) value); break;
      case Omar.UINT32: output.writeUInt32(tag, (Integer) value); break;
      case Omar.UINT64: output.writeUInt64(tag, (Long) value); break;
      case Omar.SINT32: output.writeSInt32(tag, (Integer) value); break;
      case Omar.SINT64: output.writeSInt64(tag, (Long) value); break;
      case Omar.BOOL: output.writeBool(tag, (Boolean) value); break;
      case Omar.ENUM: writeEnum((Enum) value, tag, output); break;
      case Omar.STRING: output.writeString(tag, (String) value); break;
      case Omar.BYTES: output.writeBytes(tag, (byte[]) value); break;
      case Omar.MESSAGE: writeMessage((Message) value, tag, output); break;
      case Omar.FIXED32: output.writeFixed32(tag, (Integer) value); break;
      case Omar.SFIXED32: output.writeSFixed32(tag, (Integer) value); break;
      case Omar.FIXED64: output.writeFixed64(tag, (Long) value); break;
      case Omar.SFIXED64: output.writeSFixed64(tag, (Long) value); break;
      case Omar.FLOAT: output.writeFloat(tag, (Float) value); break;
      case Omar.DOUBLE: output.writeDouble(tag, (Double) value); break;
      default: throw new RuntimeException();
    }
  }

  private <E extends Enum> void writeEnum(E value, int tag,
      CodedOutputByteBufferNano output) throws IOException {
    ProtoEnumAdapter<E> adapter = (ProtoEnumAdapter<E>) omar.enumAdapter(value.getClass());
    output.writeEnum(tag, adapter.toInt(value));
  }

  private <M extends Message> void writeMessage(M message, int tag,
      CodedOutputByteBufferNano output) throws IOException {
    ProtoAdapter<M> adapter = omar.messageAdapter((Class<M>) message.getClass());
    output.writeTag(tag, 2); // 2 = WireFormatNano.WIRETYPE_LENGTH_DELIMITED
    output.writeRawVarint32(adapter.getSerializedSize(message));
    adapter.write(message, output);
  }

  private void writeValueNoTag(CodedOutputByteBufferNano output, Object value, int type)
      throws IOException {
    switch (type) {
      case Omar.INT32: output.writeInt32NoTag((Integer) value); break;
      case Omar.INT64: output.writeInt64NoTag((Long) value); break;
      case Omar.UINT32: output.writeUInt32NoTag((Integer) value); break;
      case Omar.UINT64: output.writeUInt64NoTag((Long) value); break;
      case Omar.SINT32: output.writeSInt32NoTag((Integer) value); break;
      case Omar.SINT64: output.writeSInt64NoTag((Long) value); break;
      case Omar.BOOL: output.writeBoolNoTag((Boolean) value); break;
      case Omar.ENUM: writeEnumNoTag((Enum) value, output); break;
      case Omar.STRING: output.writeStringNoTag((String) value); break;
      case Omar.BYTES: output.writeBytesNoTag((byte[]) value); break;
      case Omar.MESSAGE: writeMessageNoTag((Message) value, output); break;
      case Omar.FIXED32: output.writeFixed32NoTag((Integer) value); break;
      case Omar.SFIXED32: output.writeSFixed32NoTag((Integer) value); break;
      case Omar.FIXED64: output.writeFixed64NoTag((Long) value); break;
      case Omar.SFIXED64: output.writeSFixed64NoTag((Long) value); break;
      case Omar.FLOAT: output.writeFloatNoTag((Float) value); break;
      case Omar.DOUBLE: output.writeDoubleNoTag((Double) value); break;
      default: throw new RuntimeException();
    }
  }

  private <E extends Enum> void writeEnumNoTag(E value, CodedOutputByteBufferNano output)
      throws IOException {
    ProtoEnumAdapter<E> adapter = (ProtoEnumAdapter<E>) omar.enumAdapter(value.getClass());
    output.writeEnumNoTag(adapter.toInt(value));
  }

  private <M extends Message> void writeMessageNoTag(M message, CodedOutputByteBufferNano output)
      throws IOException {
    ProtoAdapter<M> adapter = omar.messageAdapter((Class<M>) message.getClass());
    output.writeRawVarint32(adapter.getSerializedSize(message));
    adapter.write(message, output);
  }

  // Reading

  /** Uses reflection to read an instance from {@code input}. */
  public M read(CodedInputByteBufferNano input) throws IOException {
    try {
      Message.Builder<M> builder = builderType.newInstance();
      Storage storage = new Storage();

      while (true) {
        Extension<?, ?> extension = null;
        int tagAndType = input.readTag();
        int tag = tagAndType >> 3;
        int wireType = tagAndType & 0x7;
        if (tag == 0) {
          // Set repeated fields
          for (int storedTag : storage.getTags()) {
            if (typeMap.containsKey(storedTag)) {
              set(builder, storedTag, storage.get(storedTag));
            } else {
              setExtension(builder, getExtension(storedTag), storage.get(storedTag));
            }
          }
          return builder.build();
        }

        int type;
        int label;
        boolean packed;
        if (typeMap.containsKey(tag)) {
          type = typeMap.get(tag);
          label = type & Omar.LABEL_MASK;
          packed = (type & Omar.PACKED_MASK) == Omar.PACKED;
          type &= Omar.TYPE_MASK;
        } else {
          extension = getExtension(tag);
          if (extension == null) {
            readUnknownField(input, tagAndType & 0x7);
            continue;
          }
          type = extension.getType();
          label = extension.getLabel();
          packed = extension.getPacked();
        }
        Object value;

        if (label == Omar.REPEATED && packed && wireType == 2) {
          // Decode packed format
          int length = input.readRawVarint32();
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
          if (label == Omar.REPEATED) {
            storage.add(tag, value);
          } else if (extension != null) {
            setExtension(builder, extension, value);
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

  private Object readValue(CodedInputByteBufferNano input, int tag, int type) throws IOException {
    Object value;
    switch (type) {
      case Omar.INT32: value = input.readInt32(); break;
      case Omar.INT64: value = input.readInt64(); break;
      case Omar.UINT32: value = input.readUInt32(); break;
      case Omar.UINT64: value = input.readUInt64(); break;
      case Omar.SINT32: value = input.readSInt32(); break;
      case Omar.SINT64: value = input.readSInt64(); break;
      case Omar.BOOL: value = input.readBool(); break;
      case Omar.ENUM: value = omar.enumFromInt(getEnumClass(tag), input.readEnum()); break;
      case Omar.STRING: value = input.readString(); break;
      case Omar.BYTES: value = input.readBytes(); break;
      case Omar.MESSAGE: value = readMessage(input, tag); break;
      case Omar.FIXED32: value = input.readFixed32(); break;
      case Omar.SFIXED32: value = input.readSFixed32(); break;
      case Omar.FIXED64: value = input.readFixed64(); break;
      case Omar.SFIXED64: value = input.readSFixed64(); break;
      case Omar.FLOAT: value = input.readFloat(); break;
      case Omar.DOUBLE: value = input.readDouble(); break;
      default: throw new RuntimeException();
    }
    return value;
  }

  private Message readMessage(CodedInputByteBufferNano input, int tag) throws IOException {
    // inlined from CodedInputByteBufferNano.readMessage()
    final int length = input.readRawVarint32();
    if (input.recursionDepth >= input.recursionLimit) {
      throw new InvalidProtocolBufferNanoException("recursion limit exceeded");
    }
    final int oldLimit = input.pushLimit(length);
    ++input.recursionDepth;
    ProtoAdapter<? extends Message> adapter = omar.messageAdapter(getMessageClass(tag));
    Message message = adapter.read(input);
    input.checkLastTagWas(0);
    --input.recursionDepth;
    input.popLimit(oldLimit);
    return message;
  }

  private Class<Message> getMessageClass(int tag) {
    Class<Message> messageClass = (Class<Message>) messageTypeMap.get(tag);
    if (messageClass == null) {
      Extension<Message.ExtendableMessage, ?> extension = getExtension(tag);
      if (extension != null) {
        messageClass = (Class<Message>) extension.getMessageType();
      }
    }
    return messageClass;
  }

  // Just skip unknown fields for now
  // TODO - store unknown field values somewhere
  private void readUnknownField(CodedInputByteBufferNano input, int type)
      throws IOException {
    switch (type) {
      case 0: input.readRawVarint64(); break;
      case 1: input.readFixed64(); break;
      case 2: case 3:
        int length = input.readInt32();
        input.readRawBytes(length);
        break;
      case 4: break;
      case 5: input.readFixed32();
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

  private Extension<Message.ExtendableMessage, ?> getExtension(int tag) {
    ExtensionRegistry registry = omar.registry;
    return registry == null ? null :
        registry.getExtension((Class<Message.ExtendableMessage>) messageType, tag);
  }

  private void set(Message.Builder builder, int tag, Object value) {
    try {
      builderMethodMap.get(tag).invoke(builder, value);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private void setExtension(Message.Builder builder, Extension<?, ?> extension, Object value) {
    try {
      Method setExtension = builder.getClass().getMethod("setExtension", Extension.class,
          Object.class);
      setExtension.invoke(builder, extension, value);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private Class<? extends Enum> getEnumClass(int tag) {
    Class<? extends Enum> enumType = enumTypeMap.get(tag);
    if (enumType == null) {
      Extension<Message.ExtendableMessage, ?> extension = getExtension(tag);
      if (extension != null) {
        enumType = extension.getEnumType();
      }
    }
    return enumType;
  }
}
