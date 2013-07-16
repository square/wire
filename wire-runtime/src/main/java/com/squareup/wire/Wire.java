// Copyright 2013 Square, Inc.
package com.squareup.wire;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main class for "Wire" mobile protocol buffers.
 */
public final class Wire {

  // Hidden instance that can perform work that does not require knowledge of extensions.
  private static final Wire INSTANCE = new Wire();

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
   * Constant indicating the protocol buffer 'fixed32' datatype.
   */
  public static final int FIXED32 = 12;

  /**
   * Constant indicating the protocol buffer 'sfixed32' datatype.
   */
  public static final int SFIXED32 = 13;

  /**
   * Constant indicating the protocol buffer 'fixed64' datatype.
   */
  public static final int FIXED64 = 14;

  /**
   * Constant indicating the protocol buffer 'sfixed64' datatype.
   */
  public static final int SFIXED64 = 15;

  /**
   * Constant indicating the protocol buffer 'float' datatype.
   */
  public static final int FLOAT = 16;

  /**
   * Constant indicating the protocol buffer 'double' datatype.
   */
  public static final int DOUBLE = 17;

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

  /**
   * Constant indicating the protocol buffer '[packed = true]' extension.
   */
  public static final int PACKED = 256;

  static final int TYPE_MASK = 0x1f;
  static final int LABEL_MASK = 0xe0;
  static final int PACKED_MASK = 0x100;
  static final int BYTE_MASK = 0xff;

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
  public Wire(Class<?>... extensionClasses) {
    this(Arrays.asList(extensionClasses));
  }

  /**
   * Register all {@link Extension} objects defined as static fields on the given classes.
   *
   * @param extensionClasses a list of zero or more classes to search
   */
  public Wire(List<Class<?>> extensionClasses) {
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
   * Returns a {@link ProtoAdapter} for the given message type.
   *
   * @param messageType the {@link Message} class
   */
  public synchronized <M extends Message> ProtoAdapter<M> messageAdapter(Class<M> messageType) {
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
  public synchronized <E extends Enum> ProtoEnumAdapter<E> enumAdapter(Class<E> enumClass) {
    ProtoEnumAdapter<?> adapter = enumAdapters.get(enumClass);
    if (adapter == null) {
      adapter = new ProtoEnumAdapter<E>(enumClass);
      enumAdapters.put(enumClass, adapter);
    }
    return (ProtoEnumAdapter<E>) adapter;
  }

  /**
   * Parses a given range of bytes into a {@link Message} of the given message class.
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
   * Equivalent to {@code parseFrom(messageClass, bytes, 0, bytes.length)}.
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

  /**
   * Returns the serialized size of a given {@link Message}, in bytes.
   */
  public <M extends Message> int getSerializedSize(M message) {
    return messageAdapter((Class<M>) message.getClass()).getSerializedSize(message);
  }

  /**
   * Serializes a given {@link Message} and returns the result as a byte array.
   *
   * @param message an instance of {@link Message}
   * @param <M> the outermost {@link Message} class
   * @return an array of bytes in protocol buffer format
   */
  public <M extends Message> byte[] toByteArray(M message) {
    return messageAdapter((Class<M>) message.getClass()).toByteArray(message);
  }

  /**
   * Serializes a given {@link Message} and writes the result into a given byte array.
   *
   * @param message an instance of {@link Message}}
   * @param <M> the outermost {@link Message} class
   */
  public <M extends Message> void writeTo(M message, byte[] output, int off, int len) {
    ProtoAdapter<M> adapter = messageAdapter((Class<M>) message.getClass());
    try {
      adapter.write(message, CodedOutputByteBufferNano.newInstance(output, off, len));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns an instance of the given message class, with all fields unset.
   *
   * @param messageClass the class of the desired {@link Message}
   * @param <Type> the Message type
   * @return an instance of the desired Message class
   */
  public static <Type extends Message> Type getDefaultInstance(Class<Type> messageClass) {
    return INSTANCE.messageAdapter(messageClass).getDefaultInstance();
  }

  /**
   * Returns the integer value tagged associated with the given enum instance.
   * If the enum value is not annotated with a {@link ProtoEnum} annotation, an exception
   * will be thrown.
   *
   * @param value the integer value
   * @param <E> the enum class constant
   * @return the associated integer value
   */
  public static <E extends Enum> int intFromEnum(E value) {
    return INSTANCE.intFromEnumHelper(value);
  }

  private <E extends Enum> int intFromEnumHelper(E value) {
    ProtoEnumAdapter<E> adapter = enumAdapter((Class<E>) value.getClass());
    return adapter.toInt(value);
  }

  /**
   * Returns the enumerated value tagged with the given integer value for the
   * given enum class. If no enum value in the given class is annotated with a {@link ProtoEnum}
   * annotation having the given value, null is returned.
   *
   * @param enumClass the enum class
   * @param value the integer value
   * @param <E> the enum class constant
   * @return a value from the given enum, or null
   */
  public static <E extends Enum> E enumFromInt(Class<E> enumClass, int value) {
    return INSTANCE.enumFromIntHelper(enumClass, value);
  }

  private <E extends Enum> E enumFromIntHelper(Class<E> enumClass, int value) {
    ProtoEnumAdapter<E> adapter = enumAdapter(enumClass);
    return adapter.fromInt(value);
  }

  /**
   * Utility to return a default value when a protobuf value is null.
   * For example,
   *
   * <pre>
   * MyProto myProto = ...
   * MyField field = Wire.get(myProto.f, MyProto.f_default);
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
   * Utility method to check two values for equality. Nulls compare as equal.
   * Lists and byte arrays are compared element-by-element.
   * Used by generated code.
   */
  public static boolean equals(Object a, Object b) {
    if (a == b) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    if (a instanceof List && b instanceof List) {
      List<?> aList = (List<?>) a;
      List<?> bList = (List<?>) b;
      int size = aList.size();
      if (size != bList.size()) {
        return false;
      }
      for (int i = 0; i < size; i++) {
        if (!equals(aList.get(i), bList.get(i))) {
          return false;
        }
      }
      return true;
    }
    if (a instanceof byte[] && b instanceof byte[]) {
      return Arrays.equals((byte[]) a, (byte[]) b);
    }
    return a.equals(b);
  }

  /**
   * Utility method to return a copy of a given List.
   * Used by generated code.
   */
  public static <T> List<T> copyOf(List<T> source) {
    return source == null ? null : new ArrayList<T>(source);
  }

  /**
   * Utility method to return an unmodifiable copy of a given List.
   * Used by generated code.
   */
  public static <T> List<T> unmodifiableCopyOf(List<T> source) {
    return source == null ? null : Collections.unmodifiableList(new ArrayList<T>(source));
  }

  /**
   * Formats an extension map as a string, e.g., {@code {key=value, key=value}}.
   * Used by generated code.
   */
  public static String toString(Map<? extends Extension<?, ?>, Object> extensionMap) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    String sep = "";
    for (Map.Entry<? extends Extension<?, ?>, Object> entry : extensionMap.entrySet()) {
      sb.append(sep);
      sb.append(entry.getKey().getTag());
      sb.append("=");
      sb.append(entry.getValue());
      sep = ",";
    }
    sb.append("}");
    return sb.toString();
  }
}
