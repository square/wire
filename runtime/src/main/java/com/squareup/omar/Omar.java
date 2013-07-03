// Copyright 2013 Square, Inc.
package com.squareup.omar;

import com.google.protobuf.nano.CodedInputByteBufferNano;
import com.google.protobuf.nano.CodedOutputByteBufferNano;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.squareup.omar.Message.ExtendableMessage.Extension;

public final class Omar {

  // Hidden instance that can perform work that does not require knowledge of extensions.
  private static final Omar instance = new Omar();

  /**
   * Constant indicating the protocol buffer 'int32' datatype.
   */
  public static final int INT32 = 1;

  /**
   * Constant indicating the protocol buffer 'int64' datatype.
   */
  public static final int INT64 = 2;

  /**
   * Constant indicating the protocol buffer 'uint32' datatype.
   */
  public static final int UINT32 = 3;

  /**
   * Constant indicating the protocol buffer 'unit64' datatype.
   */
  public static final int UINT64 = 4;

  /**
   * Constant indicating the protocol buffer 'sint32' datatype.
   */
  public static final int SINT32 = 5;

  /**
   * Constant indicating the protocol buffer 'sint64' datatype.
   */
  public static final int SINT64 = 6;

  /**
   * Constant indicating the protocol buffer 'bool' datatype.
   */
  public static final int BOOL = 7;

  /**
   * Constant indicating the protocol buffer 'enum' datatype.
   */
  public static final int ENUM = 8;

  /**
   * Constant indicating the protocol buffer 'string' datatype.
   */
  public static final int STRING = 9;

  /**
   * Constant indicating the protocol buffer 'bytes' datatype.
   */
  public static final int BYTES = 10;

  /**
   * Constant indicating the protocol buffer 'message' datatype.
   */
  public static final int MESSAGE = 11;

  /**
   * Constant indicating the protocol buffer 'packed' datatype.
   */
  public static final int PACKED = 12;

  /**
   * Constant indicating the protocol buffer 'fixed32' datatype.
   */
  public static final int FIXED32 = 13;

  /**
   * Constant indicating the protocol buffer 'sfixed32' datatype.
   */
  public static final int SFIXED32 = 14;

  /**
   * Constant indicating the protocol buffer 'fixed64' datatype.
   */
  public static final int FIXED64 = 15;

  /**
   * Constant indicating the protocol buffer 'sfixed64' datatype.
   */
  public static final int SFIXED64 = 16;

  /**
   * Constant indicating the protocol buffer 'float' datatype.
   */
  public static final int FLOAT = 17;

  /**
   * Constant indicating the protocol buffer 'double' datatype.
   */
  public static final int DOUBLE = 18;

  /**
   * Constant indicating the protocol buffer 'required' label.
   */
  public static final int REQUIRED = 32;

  /**
   * Constant indicating the protocol buffer 'optional' label.
   */
  public static final int OPTIONAL = 64;

  /**
   * Constant indicating the protocol buffer 'repeated' label.
   */
  public static final int REPEATED = 128;

  static final int TYPE_MASK = 0x1f;
  static final int LABEL_MASK = 0xe0;

  private final Map<Class<? extends Message>, ProtoAdapter<? extends Message>> messageAdapters =
      new HashMap<Class<? extends Message>, ProtoAdapter<? extends Message>>();
  private final Map<Class<? extends Enum>, ProtoEnumAdapter<? extends Enum>> enumAdapters =
      new HashMap<Class<? extends Enum>, ProtoEnumAdapter<? extends Enum>>();

  // Visible to ProtoAdapter
  final ExtensionRegistry registry;

  /**
   * Register all {@link Extension} objects defined as static fields on the given classes.
   *
   * @param extensionClasses an array of zero or more classes to search
   */
  public Omar(Class<?>... extensionClasses) {
    this(Arrays.asList(extensionClasses));
  }

  /**
   * Register all {@link Extension} objects defined as static fields on the given classes.
   *
   * @param extensionClasses a list of zero or more classes to search
   */
  public Omar(List<Class<?>> extensionClasses) {
    this.registry = new ExtensionRegistry();
    for (Class<?> extensionClass : extensionClasses) {
      for (Field field : extensionClass.getDeclaredFields()) {
        if (field.getType().equals(Extension.class)) {
          try {
            Extension extension = (Extension) field.get(null);
            registry.add(extension);
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
  }

  /**
   * Returns a {@link ProtoAdapter} for the given message type, using the given
   * {@link ExtensionRegistry}.
   *
   * @param messageType the {@link Message} class
   */
  public <M extends Message> ProtoAdapter<M> messageAdapter(Class <M> messageType) {
    ProtoAdapter<?> adapter = messageAdapters.get(messageType);
    if (adapter == null) {
      adapter = new ProtoAdapter<M>(this, messageType);
      messageAdapters.put(messageType, adapter);
    }
    return (ProtoAdapter<M>) adapter;
  }

  /**
   * Returns an {@link ProtoEnumAdapter} for the given enum class.
   *
   * @param enumClass the enum class
   */
  public <E extends Enum> ProtoEnumAdapter<E> enumAdapter(Class<E> enumClass) {
    ProtoEnumAdapter<?> adapter = enumAdapters.get(enumClass);
    if (adapter == null) {
      adapter = new ProtoEnumAdapter<E>(enumClass);
      enumAdapters.put(enumClass, adapter);
    }
    return (ProtoEnumAdapter<E>) adapter;
  }

  <E extends Enum> E enumFromIntInternal(Class<E> enumClass, int value) {
    ProtoEnumAdapter<E> adapter = enumAdapter(enumClass);
    return adapter.fromInt(value);
  }

  /**
   * Returns the enumerated value tagged with the given integer value for the
   * given enum class. The enum values must be annotated with the {@link ProtoEnum}
   * annotation. If no value in the given enumeration
   *
   * @param enumClass the enum class
   * @param value the integer value
   * @param <E> the enum class constant
   * @return a value from the given enum, or null
   */
  public static <E extends Enum> E enumFromInt(Class<E> enumClass, int value) {
    return instance.enumFromIntInternal(enumClass, value);
  }

  private <E extends Enum> int intFromEnumInternal(E value) {
    ProtoEnumAdapter<E> adapter = enumAdapter((Class <E>) value.getClass());
    return adapter.toInt(value);
  }

  /**
   * Returns the integer value tagged associated with the given enum instance.
   * The enum values must be annotated with the {@link ProtoEnum}
   * annotation.
   *
   * @param value the integer value
   * @param <E> the enum class constant
   * @return the associated integer value
   */
  public static <E extends Enum> int intFromEnum(E value) {
    return instance.intFromEnumInternal(value);
  }

  /**
   * Parse a given range of bytes into a {@link Message} of the given message class.
   *
   * @param messageClass the class of the outermost {@link Message}
   * @param bytes an array of bytes
   * @param off the starting offset within the array
   * @param len the number of bytes to use
   * @param <M> the outermost {@link Message} class
   * @return an instance of the desired message class
   * @throws IOException if parsing fails
   */
  public <M extends Message> M parseFrom(Class<M> messageClass,
      byte[] bytes, int off, int len) throws IOException {
    ProtoAdapter<M> adapter = messageAdapter(messageClass);
    return adapter.read(CodedInputByteBufferNano.newInstance(bytes, off, len));
  }

  /**
   * Parse an entire byte array into a {@link Message} of the given message class.
   * Equivalent to {@code parseFrom(messageClass, bytes, 0, bytes.length, registry)}.
   *
   * @param messageClass the class of the outermost {@link Message}
   * @param bytes an array of bytes
   * @param <M> the outermost {@link Message} class
   * @return an instance of the desired message class
   * @throws IOException if parsing fails
   */
  public <M extends Message> M parseFrom(Class<M> messageClass, byte[] bytes) throws IOException {
    ProtoAdapter<M> adapter = messageAdapter(messageClass);
    return adapter.read(CodedInputByteBufferNano.newInstance(bytes));
  }

  private <M extends Message> int getSerializedSizeHelper(M message) {
    return messageAdapter((Class<M>) message.getClass()).getSerializedSize(message);
  }

  /**
   * Returns the serialized size of a given message, in bytes.
   */
  public static <M extends Message> int getSerializedSize(M message) {
    return instance.getSerializedSizeHelper(message);
  }

  private <M extends Message> byte[] toByteArrayHelper(M message) {
    return messageAdapter((Class<M>) message.getClass()).toByteArray(message);
  }

  /**
   * Serializes a given {@link Message} and returns the result as a byte array.
   *
   * @param message an instance of {@link Message}}
   * @param <M> the outermost {@link Message} class
   * @return an array of bytes in protocol buffer format
   */
  public static <M extends Message> byte[] toByteArray(M message) {
    return instance.toByteArrayHelper(message);
  }

  /**
   * Serializes a given {@link Message} and writes the result into a given byte array.
   *
   * @param message an instance of {@link Message}}
   * @param <M> the outermost {@link Message} class
   */
  public static <M extends Message> void writeTo(M message, byte[] output, int off, int len) {
    instance.writeToHelper(message, output, off, len);
  }

  private <M extends Message> void writeToHelper(M message, byte[] output, int off, int len) {
    ProtoAdapter<M> adapter = messageAdapter((Class<M>) message.getClass());
    try {
      adapter.write(message, CodedOutputByteBufferNano.newInstance(output, off, len));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  ///**
  // * Returns an instance of the given message class, with all fields unset.
  // *
  // * @param messageClass the class of the desired {@link Message}
  // * @param <M> the Message type
  // * @return an instance of the desired Message class
  // */
  //public <M extends Message> M getDefaultInstance(Class<M> messageClass) {
  //  return messageAdapter(messageClass).getDefaultInstance();
  //}

  /**
   * Returns an instance of the given message class, with all fields unset.
   *
   * @param messageClass the class of the desired {@link Message}
   * @param <Type> the Message type
   * @return an instance of the desired Message class
   */
  public static <Type extends Message> Type getDefaultInstance(Class<Type> messageClass) {
    return instance.messageAdapter(messageClass).getDefaultInstance();
  }

  /**
   * Utility to return a default value when a protobuf value is null.
   * For example,
   *
   * <pre>
   * MyProto myProto = ...
   * MyField field = get(myProto.f, MyProto.f_default);
   * </pre>
   *
   * will attempt to retrieve the value of the field 'f' defined by MyProto.
   * If the field is null (i.e., unset), <code>get</code> will return its
   * second argument, which in this case is the default value for the field
   * 'f'.
   *
   * @param value the value to return if non-null
   * @param defaultValue the value to return if value is null
   * @param <M> the value type
   * @return one of value or defaultValue
   */
  public static <M> M get(M value, M defaultValue) {
    return value != null ? value : defaultValue;
  }

  /**
   * Utility method to check two possibly null values for equality.
   */
  public static boolean equals(Object a, Object b) {
    return (a == b) || (a != null && a.equals(b));
  }

  /**
   * Utility method to return a copy of a given List.
   */
  public static <T> List<T> copyOf(List<T> source) {
    return source == null ? null : new ArrayList<T>(source);
  }

  /**
   * Utility method to return a copy of a given List.
   */
  public static <T> List<T> unmodifiableCopyOf(List<T> source) {
    return source == null ? null : Collections.unmodifiableList(new ArrayList<T>(source));
  }
}
