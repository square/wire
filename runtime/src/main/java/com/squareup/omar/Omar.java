// Copyright 2013 Square, Inc.
package com.squareup.omar;

import com.google.protobuf.nano.CodedInputByteBufferNano;
import com.google.protobuf.nano.CodedOutputByteBufferNano;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Omar {

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

  private Omar() {}

  private static Map<Class<? extends Message>, ProtoAdapter<? extends Message>> messageAdapters =
      new HashMap<Class<? extends Message>, ProtoAdapter<? extends Message>>();
  private static Map<Class<? extends Enum>, ProtoEnumAdapter<? extends Enum>> enumAdapters =
      new HashMap<Class<? extends Enum>, ProtoEnumAdapter<? extends Enum>>();

  /**
   * Returns a {@link ProtoAdapter} for the given message type, using the given
   * {@link ExtensionRegistry}.
   */
  public static <M extends Message> ProtoAdapter<M> messageAdapter(Class<M> messageType,
      ExtensionRegistry registry) {
    ProtoAdapter<?> adapter = messageAdapters.get(messageType);
    if (adapter == null || adapter.getExtensionRegistry() != registry) {
      adapter = new ProtoAdapter<M>(messageType, registry);
      messageAdapters.put(messageType, adapter);
    }
    return (ProtoAdapter<M>) adapter;
  }

  /**
   * Returns a {@link ProtoAdapter} for the given message type, with no
   * {@link ExtensionRegistry}.
   */
  public static <M extends Message> ProtoAdapter<M> messageAdapter(Class<M> type) {
    return messageAdapter(type, null);
  }

  /**
   * Returns an {@link EnumAdapter} for the given enum type.
   */
  public static <E extends Enum> ProtoEnumAdapter<E> enumAdapter(Class<E> type) {
    ProtoEnumAdapter<?> adapter = enumAdapters.get(type);
    if (adapter == null) {
      adapter = new ProtoEnumAdapter<E>(type);
      enumAdapters.put(type, adapter);
    }
    return (ProtoEnumAdapter<E>) adapter;
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
    ProtoEnumAdapter<E> adapter = enumAdapter(enumClass);
    return adapter.fromInt(value);
  }

  public static <E extends Enum> int intFromEnum(E value) {
    ProtoEnumAdapter<E> adapter = enumAdapter((Class <E>) value.getClass());
    return adapter.toInt(value);
  }

  public static boolean equals(Object a, Object b) {
    return (a == b) || (a != null && a.equals(b));
  }

  public static <T> List<T> copy(List<T> source) {
    return source == null ? null : new ArrayList<T>(source);
  }

  public static <T> List<T> wrap(List<T> source) {
    return source == null ? null : Collections.unmodifiableList(new ArrayList<T>(source));
  }

  public static <M extends Message> M parseFrom(Class<M> messageClass,
      byte[] bytes, int off, int len,
      ExtensionRegistry extensionRegistry) throws IOException {
    ProtoAdapter<M> adapter = messageAdapter(messageClass, extensionRegistry);
    return adapter.read(CodedInputByteBufferNano.newInstance(bytes, off, len));
  }

  public static <M extends Message> M parseFrom(Class<M> messageClass, byte[] bytes,
      ExtensionRegistry extensionRegistry) throws IOException {
    ProtoAdapter<M> adapter = messageAdapter(messageClass, extensionRegistry);
    return adapter.read(CodedInputByteBufferNano.newInstance(bytes));
  }

  public static <M extends Message> byte[] toByteArray(M message) {
    return messageAdapter((Class<M>) message.getClass()).toByteArray(message);
  }

  public static <M extends Message> void writeTo(M message, byte[] output, int off, int len) {
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
   * @param <M> the Message type
   * @return an instance of the desired Message class
   */
  public static <M extends Message> M getDefaultInstance(Class<M> messageClass) {
    return messageAdapter(messageClass).getDefaultInstance();
  }

  /**
   * Utility to return a default value when a protobuf value is null.
   * For example, <pre>
   * MyProto myProro = ...
   * MyField field = get(myProto.f, MyProto.f_default;
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
}
