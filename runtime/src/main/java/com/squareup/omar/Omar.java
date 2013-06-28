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

public class Omar {

  public static final int TYPE_MASK = 0x1f;
  public static final int INT32 = 1;
  public static final int INT64 = 2;
  public static final int UINT32 = 3;
  public static final int UINT64 = 4;
  public static final int SINT32 = 5;
  public static final int SINT64 = 6;
  public static final int BOOL = 7;
  public static final int ENUM = 8;
  public static final int STRING = 9;
  public static final int BYTES = 10;
  public static final int MESSAGE = 11;
  public static final int PACKED = 12;
  public static final int FIXED32 = 13;
  public static final int SFIXED32 = 14;
  public static final int FIXED64 = 15;
  public static final int SFIXED64 = 16;
  public static final int FLOAT = 17;
  public static final int DOUBLE = 18;
  public static final int LABEL_MASK = 0xe0;
  public static final int REQUIRED = 32;
  public static final int OPTIONAL = 64;
  public static final int REPEATED = 128;

  private static Map<Class<? extends Message>, ProtoAdapter<? extends Message>> messageAdapters =
      new HashMap<Class<? extends Message>, ProtoAdapter<? extends Message>>();
  private static Map<Class<? extends Enum>, ProtoEnumAdapter<? extends Enum>> enumAdapters =
      new HashMap<Class<? extends Enum>, ProtoEnumAdapter<? extends Enum>>();

  public static <M extends Message> ProtoAdapter<M> messageAdapter(Class<M> type,
      ExtensionRegistry registry) {
    ProtoAdapter<?> adapter = messageAdapters.get(type);
    if (adapter == null || adapter.getExtensionRegistry() != registry) {
      adapter = new ProtoAdapter<M>(type, registry);
      messageAdapters.put(type, adapter);
    }
    return (ProtoAdapter<M>) adapter;
  }

  public static <M extends Message> ProtoAdapter<M> messageAdapter(Class<M> type) {
    return messageAdapter(type, null);
  }

  public static <E extends Enum> ProtoEnumAdapter<E> enumAdapter(Class<E> type) {
    ProtoEnumAdapter<?> adapter = enumAdapters.get(type);
    if (adapter == null) {
      adapter = new ProtoEnumAdapter<E>(type);
      enumAdapters.put(type, adapter);
    }
    return (ProtoEnumAdapter<E>) adapter;
  }

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

  public static <M extends Message> M getDefaultInstance(Class<M> messageClass) {
    return messageAdapter(messageClass).getDefaultInstance();
  }

  public static <M> M get(M value, M defaultValue) {
    return value != null ? value : defaultValue;
  }
}
