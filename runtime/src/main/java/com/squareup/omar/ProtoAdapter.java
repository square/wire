// Copyright 2013 Square, Inc.
package com.squareup.omar;

import com.google.protobuf.nano.CodedInputByteBufferNano;
import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
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
  private final Class<M> messageType;
  private final Class<Message.Builder<M>> builderType;
  private M defaultInstance;

  private final List<Integer> tags = new ArrayList<Integer>();
  private final Map<Integer, Integer> typeMap = new HashMap<Integer, Integer>();
  private final Map<Integer, Class<? extends Message>> messageTypeMap =
      new HashMap<Integer, Class<? extends Message>>();
  private final Map<Integer, Class<? extends Enum>> enumTypeMap =
      new HashMap<Integer, Class<? extends Enum>>();
  private final Map<Integer, Object> defaultValueMap = new HashMap<Integer, Object>();
  private final Map<Integer, Field> fieldMap = new HashMap<Integer, Field>();
  private final Map<Integer, Method> builderMethodMap = new HashMap<Integer, Method>();
  private final ExtensionRegistry extensionRegistry;

  private static <M extends Message> Object parseDefaultValue(Class<? extends Enum> enumType,
      int type, String s) {
     switch (type) {
       case Omar.BOOL:
         return Boolean.getBoolean(s);
       case Omar.INT32: case Omar.SINT32: case Omar.UINT32: case Omar.FIXED32: case Omar.SFIXED32:
         return new BigInteger(s).intValue();
       case Omar.INT64: case Omar.SINT64: case Omar.UINT64: case Omar.FIXED64: case Omar.SFIXED64:
         if (s.endsWith("l") || s.endsWith("L")) {
           s = s.substring(0, s.length() - 1);
         }
         return new BigInteger(s).longValue();
       case Omar.FLOAT:
         if (s.endsWith("f") || s.endsWith("F")) {
           s = s.substring(0, s.length() - 1);
         }
         return new BigDecimal(s).floatValue();
       case Omar.DOUBLE:
         if (s.endsWith("d") || s.endsWith("D")) {
           s = s.substring(0, s.length() - 1);
         }
         return new BigDecimal(s).doubleValue();
       case Omar.ENUM:
         int index = s.lastIndexOf('.');
         String enumName = s.substring(index + 1);
         return Enum.valueOf(enumType, enumName);
       case Omar.STRING:
         return s;
     }
     throw new RuntimeException("Not yet implemented: " + type);
  }

  ProtoAdapter(Class <M> messageType, ExtensionRegistry extensionRegistry) {
    this.messageType = messageType;
    try {
      this.builderType = (Class<Message.Builder<M>>) Class.forName(messageType.getName() + "$Builder");
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
        typeMap.put(tag, annotation.label() | annotation.type());
        // Record type for tags that store a Message.
        if (Message.class.isAssignableFrom(field.getType())) {
          messageTypeMap.put(tag, (Class<Message>) field.getType());
        } else if (List.class.isAssignableFrom(field.getType())) {
            // If the field is repeated, the @ProtoField annotation must specify messageType
            // since the actual type within the List has been erased.
            if (annotation.messageType() != ProtoField.NotAMessage.class) {
              messageTypeMap.put(tag, annotation.messageType());
            }
        }

        Class<? extends Enum> enumType = null;

        // Record type for tags that store an Enum
        if (Enum.class.isAssignableFrom(field.getType())) {
          enumType = (Class<? extends Enum>) field.getType();
          enumTypeMap.put(tag, enumType);
        }
        // Record default values
        if (annotation.defaultValue().length() > 0) {
          defaultValueMap.put(tag, parseDefaultValue(enumType, annotation.type(),
              annotation.defaultValue()));
        }

        // Record setter methods on the builder class
        try {
          Method method = builderType.getMethod(field.getName(), field.getType());
          builderMethodMap.put(tag, method);
        } catch (NoSuchMethodException e) {
          throw new IllegalArgumentException("No builder method " +
              builderType.getName() + "." + field.getName() + "(" + field.getType() + ")");
        }
      }
    }

    // Sort tags so we can process them in order
    Collections.sort(tags);

    this.extensionRegistry = extensionRegistry;
  }

  /**
   * Returns an instance of the message type of this {@link ProtoAdapter} with all fields unset.
   */
  public M getDefaultInstance() {
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

  /**
   * Returns the {@link ExtensionRegistry} for this {@link ProtoAdapter}.
   */
  public ExtensionRegistry getExtensionRegistry() {
    return extensionRegistry;
  }

  private <M extends Message> int getMessageSize(M message, int tag) {
    ProtoAdapter<M> adapter = Omar.messageAdapter((Class<M>) message.getClass(), extensionRegistry);
    int messageSize = adapter.getSerializedSize(message);
    int size = CodedOutputByteBufferNano.computeTagSize(tag);
    size += CodedOutputByteBufferNano.computeRawVarint32Size(messageSize) + messageSize;
    return size;
  }

  private <M extends Message> int getRepeatedMessageSize(List<M> messages, int tag) {
    if (messages.size() == 0) {
      return 0;
    }
    ProtoAdapter<M> adapter = Omar.messageAdapter((Class<M>) messages.get(0).getClass(),
        extensionRegistry);
    int size = 0;
    for (M message : messages) {
      int messageSize = adapter.getSerializedSize(message);
      size += CodedOutputByteBufferNano.computeTagSize(tag);
      size += CodedOutputByteBufferNano.computeRawVarint32Size(messageSize) + messageSize;
    }
    return size;
  }

  private Class<? extends Enum> getEnumClass(int tag) {
    Class<? extends Enum> enumType = enumTypeMap.get(tag);
    if (enumType == null) {
      enumType = extensionRegistry.getExtension((Class<Message.ExtendableMessage>) messageType,
          tag).getEnumType();
    }
    return enumType;
  }

  private <E extends Enum> int getEnumSize(E value, int tag) {
    ProtoEnumAdapter<E> adapter = (ProtoEnumAdapter<E>) Omar.enumAdapter(value.getClass());
    return CodedOutputByteBufferNano.computeEnumSize(tag, adapter.toInt(value));
  }

  private <E extends Enum> int getRepeatedEnumSize(List<E> enums, int tag) {
    if (enums.size() == 0) {
      return 0;
    }
    Class<? extends Enum> enumClass = enums.get(0).getClass();
    ProtoEnumAdapter<E> adapter = (ProtoEnumAdapter<E>) Omar.enumAdapter(enumClass);
    int size = 0;
    for (E e : enums) {
      size += CodedOutputByteBufferNano.computeEnumSize(tag, adapter.toInt(e));
    }
    return size;
  }

  private <M extends Message> void writeMessage(M message, int tag,
      CodedOutputByteBufferNano output) throws IOException {
    ProtoAdapter<M> adapter = Omar.messageAdapter((Class<M>) message.getClass(), extensionRegistry);
    output.writeTag(tag, 2); // 2 = WireFormatNano.WIRETYPE_LENGTH_DELIMITED
    output.writeRawVarint32(adapter.getSerializedSize(message));
    adapter.write(message, output);
  }

  private <M extends Message> void writeRepeatedMessage(List<M> messages,
      int tag, CodedOutputByteBufferNano output) throws IOException {
    if (messages.size() == 0) {
      return;
    }
    ProtoAdapter<M> adapter = Omar.messageAdapter((Class<M>) messages.get(0).getClass(),
        extensionRegistry);
    for (M message : messages) {
      output.writeTag(tag, 2); // 2 = WireFormatNano.WIRETYPE_LENGTH_DELIMITED
      output.writeRawVarint32(adapter.getSerializedSize(message));
      adapter.write(message, output);
    }
  }

  private <E extends Enum> void writeEnum(E value, int tag,
      CodedOutputByteBufferNano output) throws IOException {
    ProtoEnumAdapter<E> adapter = (ProtoEnumAdapter<E>) Omar.enumAdapter(value.getClass());
    output.writeEnum(tag, adapter.toInt(value));
  }

  private <E extends Enum> void writeRepeatedEnum(List<E> enums, int tag,
      CodedOutputByteBufferNano output) throws IOException {
    if (enums.size() == 0) {
      return;
    }
    Class<? extends Enum> enumClass = enums.get(0).getClass();
    ProtoEnumAdapter<E> adapter = (ProtoEnumAdapter<E>) Omar.enumAdapter(enumClass);
    for (E e : enums) {
      output.writeEnum(tag, adapter.toInt(e));
    }
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
      Method setExtension = builder.getClass().getMethod("setExtension", Extension.class, Object.class);
      setExtension.invoke(builder, extension, (Object) value);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

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
      int type = typeMap.get(tag);
      int label = type & Omar.LABEL_MASK;
      type &= Omar.TYPE_MASK;

      if (label == Omar.REPEATED && type != Omar.MESSAGE && type != Omar.ENUM) {
        for (Object o : (List<?>) value) {
          size += getSerializedSize(tag, o, type, label);
        }
      } else {
        size += getSerializedSize(tag, value, type, label);
      }
    }

    if (instance instanceof Message.ExtendableMessage) {
      Message.ExtendableMessage extendableMessage = (Message.ExtendableMessage) instance;
      try {
        Field extensionMap = extendableMessage.getClass().getField("extensionMap");
        Map<Extension<?, ?>, ?> map = (Map<Extension<?, ?>, ?>) extensionMap.get(instance);
        for (Map.Entry<Extension<?, ?>, ?> entry: map.entrySet()) {
          Extension<?, ?> extension = entry.getKey();
          Object value = (Object) entry.getValue();
          int tag = extension.getTag();
          int type = extension.getType();
          int label = extension.getLabel();
          if (label == Omar.REPEATED && type != Omar.MESSAGE && type != Omar.ENUM) {
            for (Object o : (List<?>) value) {
              size += getSerializedSize(tag, o, type, label);
            }
          } else {
            size += getSerializedSize(tag, value, type, label);
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

  private Class<Message> getMessageClass(int tag) {
    Class<Message> messageClass = (Class<Message>) messageTypeMap.get(tag);
    if (messageClass == null && extensionRegistry != null) {
      messageClass = (Class<Message>) extensionRegistry.getExtension(
          (Class<Message.ExtendableMessage>) messageType, tag).getMessageType();
    }
    return messageClass;
  }

  private int getSerializedSize(int tag, Object value, int type, int label) {
    switch (type) {
      case Omar.INT32: return CodedOutputByteBufferNano.computeInt32Size(tag, (Integer) value);
      case Omar.INT64: return CodedOutputByteBufferNano.computeInt64Size(tag, (Long) value);
      case Omar.UINT32: return CodedOutputByteBufferNano.computeUInt32Size(tag, (Integer) value);
      case Omar.UINT64: return CodedOutputByteBufferNano.computeUInt64Size(tag, (Long) value);
      case Omar.SINT32: return CodedOutputByteBufferNano.computeSInt32Size(tag, (Integer) value);
      case Omar.SINT64: return CodedOutputByteBufferNano.computeSInt64Size(tag, (Long) value);
      case Omar.BOOL: return CodedOutputByteBufferNano.computeBoolSize(tag, (Boolean) value);
      case Omar.ENUM:
        if (label == Omar.REPEATED) {
          return getRepeatedEnumSize((List<Enum>) value, tag);
        } else {
          return getEnumSize((Enum) value, tag);
        }
      case Omar.STRING: return CodedOutputByteBufferNano.computeStringSize(tag, (String) value);
      case Omar.BYTES: return CodedOutputByteBufferNano.computeBytesSize(tag, (byte[]) value);
      case Omar.MESSAGE:
        if (label == Omar.REPEATED) {
          return getRepeatedMessageSize((List<Message>) value, tag);
        } else {
          return getMessageSize((Message) value, tag);
        }
      case Omar.PACKED: throw new IllegalArgumentException("not yet supported");
      case Omar.FIXED32: return CodedOutputByteBufferNano.computeFixed32Size(tag, (Integer) value);
      case Omar.SFIXED32: return CodedOutputByteBufferNano.computeSFixed32Size(tag, (Integer) value);
      case Omar.FIXED64: return CodedOutputByteBufferNano.computeFixed64Size(tag, (Long) value);
      case Omar.SFIXED64: return CodedOutputByteBufferNano.computeSFixed64Size(tag, (Long) value);
      case Omar.FLOAT: return CodedOutputByteBufferNano.computeFloatSize(tag, (Float) value);
      case Omar.DOUBLE: return CodedOutputByteBufferNano.computeDoubleSize(tag, (Double) value);
      default: throw new RuntimeException();
    }
  }

  private void writeValue(CodedOutputByteBufferNano output, int tag, Object value, int type,
      int label) throws IOException {
    switch (type) {
      case Omar.INT32: output.writeInt32(tag, (Integer) value); break;
      case Omar.INT64: output.writeInt64(tag, (Long) value); break;
      case Omar.UINT32: output.writeUInt32(tag, (Integer) value); break;
      case Omar.UINT64: output.writeUInt64(tag, (Long) value); break;
      case Omar.SINT32: output.writeSInt32(tag, (Integer) value); break;
      case Omar.SINT64: output.writeSInt64(tag, (Long) value); break;
      case Omar.BOOL: output.writeBool(tag, (Boolean) value); break;
      case Omar.ENUM:
        if (label == Omar.REPEATED) {
          writeRepeatedEnum((List<Enum>) value, tag, output);
        } else {
          writeEnum((Enum) value, tag, output);
        }
        break;
      case Omar.STRING: output.writeString(tag, (String) value); break;
      case Omar.BYTES: output.writeBytes(tag, (byte[]) value); break;
      case Omar.MESSAGE:
        if (label == Omar.REPEATED) {
          writeRepeatedMessage((List<Message>) value, tag, output);
        } else {
          writeMessage((Message) value, tag, output);
        }
        break;
      case Omar.PACKED: output.writeInt32(tag, (Integer) value); break;
      case Omar.FIXED32: output.writeFixed32(tag, (Integer) value); break;
      case Omar.SFIXED32: output.writeSFixed32(tag, (Integer) value); break;
      case Omar.FIXED64: output.writeFixed64(tag, (Long) value); break;
      case Omar.SFIXED64: output.writeSFixed64(tag, (Long) value); break;
      case Omar.FLOAT: output.writeFloat(tag, (Float) value); break;
      case Omar.DOUBLE: output.writeDouble(tag, (Double) value); break;
      default: throw new RuntimeException();
    }
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
      int type = typeMap.get(tag);
      int label = type & Omar.LABEL_MASK;
      type &= Omar.TYPE_MASK;

      if (label == Omar.REPEATED && type != Omar.MESSAGE && type != Omar.ENUM) {
        for (Object o : (List<?>) value) {
          writeValue(output, tag, o, type, label);
        }
      } else {
        writeValue(output, tag, value, type, label);
      }
    }

    if (instance instanceof Message.ExtendableMessage) {
      Message.ExtendableMessage extendableMessage = (Message.ExtendableMessage) instance;
      try {
        Field extensionMap = extendableMessage.getClass().getField("extensionMap");
        Map<Extension<?, ?>, ?> map = (Map<Extension<?, ?>, ?>) extensionMap.get(instance);
        for (Map.Entry<Extension<?, ?>, ?> entry: map.entrySet()) {
           Extension<?, ?> extension = entry.getKey();
           Object value = (Object) entry.getValue();
          int tag = extension.getTag();
          int type = extension.getType();
          int label = extension.getLabel();
          if (label == Omar.REPEATED && type != Omar.MESSAGE && type != Omar.ENUM) {
             for (Object o : (List<?>) value) {
               writeValue(output, tag, o, type, label);
             }
           } else {
             writeValue(output, tag, value, type, label);
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

  private Message readMessage(CodedInputByteBufferNano input, int tag) throws IOException {
    // inlined from CodedInputByteBufferNano.readMessage()
    final int length = input.readRawVarint32();
    if (input.recursionDepth >= input.recursionLimit) {
      throw new InvalidProtocolBufferNanoException("recursion limit exceeded");
    }
    final int oldLimit = input.pushLimit(length);
    ++input.recursionDepth;
    ProtoAdapter<? extends Message> adapter =
        Omar.messageAdapter(getMessageClass(tag), extensionRegistry);
    Message message = adapter.read(input);
    input.checkLastTagWas(0);
    --input.recursionDepth;
    input.popLimit(oldLimit);
    return message;
  }

  private void readUnknownField(CodedInputByteBufferNano input, int tag, int type)
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

  /** Uses reflection to read an instance from {@code in}. */
  public M read(CodedInputByteBufferNano input) throws IOException {
    try {
      Message.Builder<M> builder = builderType.newInstance();
      Storage storage = new Storage();

      while (true) {
        Extension<?, ?> extension = null;
        int tagAndType = input.readTag();
        int tag = tagAndType >> 3;
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
        if (typeMap.containsKey(tag)) {
          type = typeMap.get(tag);
          label = type & Omar.LABEL_MASK;
          type &= Omar.TYPE_MASK;
        } else {
          extension = getExtension(tag);
          if (extension == null) {
            readUnknownField(input, tag, tagAndType & 0x7);
            continue;
          }
          type = extension.getType();
          label = extension.getLabel();
        }
        Object value;
        switch (type) {
          case Omar.INT32: value = input.readInt32(); break;
          case Omar.INT64: value = input.readInt64(); break;
          case Omar.UINT32: value = input.readUInt32(); break;
          case Omar.UINT64: value = input.readUInt64(); break;
          case Omar.SINT32: value = input.readSInt32(); break;
          case Omar.SINT64: value = input.readSInt64(); break;
          case Omar.BOOL: value = input.readBool(); break;
          case Omar.ENUM: value = Omar.enumFromInt(getEnumClass(tag), input.readEnum()); break;
          case Omar.STRING: value = input.readString(); break;
          case Omar.BYTES: value = input.readBytes(); break;
          case Omar.MESSAGE: value = readMessage(input, tag); break;
          case Omar.PACKED: throw new RuntimeException("Not supported yet");
          case Omar.FIXED32: value = input.readFixed32(); break;
          case Omar.SFIXED32: value = input.readSFixed32(); break;
          case Omar.FIXED64: value = input.readFixed64(); break;
          case Omar.SFIXED64: value = input.readSFixed64(); break;
          case Omar.FLOAT: value = input.readFloat(); break;
          case Omar.DOUBLE: value = input.readDouble(); break;
          default: throw new RuntimeException();
        }

        if (label == Omar.REPEATED) {
          storage.add(tag, value);
        } else if (extension != null) {
          setExtension(builder, extension, value);
        } else {
          set(builder, tag, value);
        }
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  private Extension<Message.ExtendableMessage, ?> getExtension(int tag) {
    return extensionRegistry == null ? null :
        extensionRegistry.getExtension((Class <Message.ExtendableMessage>) messageType, tag);
  }
}
