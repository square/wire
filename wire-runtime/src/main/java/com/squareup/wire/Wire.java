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
 * Main class for Wire mobile protocol buffers.
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

  private final Map<Class<? extends Message>, MessageAdapter<? extends Message>> messageAdapters =
      new HashMap<Class<? extends Message>, MessageAdapter<? extends Message>>();
  private final Map<Class<? extends Message.Builder>,
      BuilderAdapter<? extends Message.Builder>> builderAdapters =
          new HashMap<Class<? extends Message.Builder>,
              BuilderAdapter<? extends Message.Builder>>();
  private final Map<Class<? extends Enum>, EnumAdapter<? extends Enum>> enumAdapters =
      new HashMap<Class<? extends Enum>, EnumAdapter<? extends Enum>>();

  // Visible to MessageAdapter
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
   * Returns a {@link MessageAdapter} for the given message type.
   *
   * @param messageType the {@link Message} class
   */
  @SuppressWarnings("unchecked")
  public synchronized <M extends Message> MessageAdapter<M> messageAdapter(Class<M> messageType) {
    MessageAdapter<M> adapter = (MessageAdapter<M>) messageAdapters.get(messageType);
    if (adapter == null) {
      adapter = new MessageAdapter<M>(this, messageType);
      messageAdapters.put(messageType, adapter);
    }
    return adapter;
  }

  /**
   * Returns a {@link BuilderAdapter} for the given message type.
   *
   * @param builderType the {@link Message.Builder} class
   */
  @SuppressWarnings("unchecked")
  public synchronized <B extends Message.Builder> BuilderAdapter<B>
      builderAdapter(Class<B> builderType) {
    BuilderAdapter<B> adapter = (BuilderAdapter<B>) builderAdapters.get(builderType);
    if (adapter == null) {
      adapter = new BuilderAdapter<B>(builderType);
      builderAdapters.put(builderType, adapter);
    }
    return adapter;
  }

  /**
   * Returns an {@link EnumAdapter} for the given enum class.
   *
   * @param enumClass the enum class
   */
  @SuppressWarnings("unchecked")
  public synchronized <E extends Enum> EnumAdapter<E> enumAdapter(Class<E> enumClass) {
    EnumAdapter<E> adapter = (EnumAdapter<E>) enumAdapters.get(enumClass);
    if (adapter == null) {
      adapter = new EnumAdapter<E>(enumClass);
      enumAdapters.put(enumClass, adapter);
    }
    return adapter;
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
    MessageAdapter<M> adapter = messageAdapter(messageClass);
    return adapter.read(WireInput.newInstance(bytes, off, len));
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
    MessageAdapter<M> adapter = messageAdapter(messageClass);
    return adapter.read(WireInput.newInstance(bytes));
  }

  /**
   * Returns the serialized size of a given {@link Message}, in bytes.
   */
  @SuppressWarnings("unchecked")
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
  @SuppressWarnings("unchecked")
  public <M extends Message> byte[] toByteArray(M message) {
    return messageAdapter((Class<M>) message.getClass()).toByteArray(message);
  }

  /**
   * Serializes a given {@link Message} and writes the result into a given byte array.
   *
   * @param message an instance of {@link Message}}
   * @param <M> the outermost {@link Message} class
   */
  @SuppressWarnings("unchecked")
  public <M extends Message> void writeTo(M message, byte[] output, int off, int len) {
    MessageAdapter<M> adapter = messageAdapter((Class<M>) message.getClass());
    try {
      adapter.write(message, WireOutput.newInstance(output, off, len));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns an instance of the given message class, with all fields unset.
   *
   * @param messageClass the class of the desired {@link Message}
   * @param <M> the Message type
   * @return an instance of the desired Message class
   */
  public static <M extends Message> M getDefaultInstance(Class<M> messageClass) {
    return INSTANCE.messageAdapter(messageClass).getDefaultInstance();
  }

  /**
   * Returns a String representation of the given {@code Message}.
   */
  @SuppressWarnings("unchecked")
  public static <M extends Message> String toString(M message) {
    return INSTANCE.messageAdapter((Class<M>) message.getClass()).toString(message);
  }

  /**
   * Throws an {@link IllegalStateException} if a required field of the given
   * {@link Message.Builder} is not set.
   */
  public static <B extends Message.Builder> void checkRequiredFields(B builder) {
    INSTANCE.builderAdapter(builder.getClass()).checkRequiredFields(builder);
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

  @SuppressWarnings("unchecked")
  private <E extends Enum> int intFromEnumHelper(E value) {
    EnumAdapter<E> adapter = enumAdapter((Class<E>) value.getClass());
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
    EnumAdapter<E> adapter = enumAdapter(enumClass);
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
   * @param <T> the value type
   * @return one of value or defaultValue
   */
  public static <T> T get(T value, T defaultValue) {
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
}
