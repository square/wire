package com.squareup.wire;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

final class RuntimeFieldBinding {
  static RuntimeFieldBinding create(Wire wire, ProtoField protoField, Field messageField,
      Class<?> builderType) {
    Message.Datatype datatype = protoField.type();
    Message.Label label = protoField.label();

    Class<? extends ProtoEnum> enumType = null;
    Class<? extends Message> messageType = null;
    if (datatype == Message.Datatype.ENUM) {
      enumType = getEnumType(messageField);
    } else if (datatype == Message.Datatype.MESSAGE) {
      messageType = getMessageType(messageField);
    }
    TypeAdapter<?> singleAdapter =
        RuntimeMessageAdapter.typeAdapter(wire, datatype, messageType, enumType);
    TypeAdapter<?> adapter;
    if (!label.isRepeated()) {
      adapter = singleAdapter;
    } else if (label.isPacked()) {
      adapter = TypeAdapter.createPacked(singleAdapter);
    } else {
      adapter = TypeAdapter.createRepeated(singleAdapter);
    }

    int tag = protoField.tag();
    boolean redacted = protoField.redacted();
    String name = messageField.getName();
    Field builderField = getBuilderField(builderType, name);
    Method builderMethod = getBuilderMethod(builderType, name, messageField.getType());
    return new RuntimeFieldBinding(tag, name, datatype, label, redacted, enumType, messageType,
        (TypeAdapter<Object>) singleAdapter, (TypeAdapter<Object>) adapter, messageField,
        builderField, builderMethod);
  }

  @SuppressWarnings("unchecked")
  private static Class<? extends Message> getMessageType(Field field) {
    Class<?> fieldType = field.getType();
    if (List.class.isAssignableFrom(fieldType)) {
      return field.getAnnotation(ProtoField.class).messageType();
    }
    return (Class<Message>) fieldType;
  }

  @SuppressWarnings("unchecked")
  private static Class<? extends ProtoEnum> getEnumType(Field field) {
    Class<?> fieldType = field.getType();
    if (List.class.isAssignableFrom(fieldType)) {
      return field.getAnnotation(ProtoField.class).enumType();
    }
    return (Class<ProtoEnum>) fieldType;
  }

  private static Field getBuilderField(Class<?> builderType, String name) {
    try {
      return builderType.getField(name);
    } catch (NoSuchFieldException e) {
      throw new AssertionError("No builder field " + builderType.getName() + "." + name);
    }
  }

  private static Method getBuilderMethod(Class<?> builderType, String name, Class<?> type) {
    try {
      return builderType.getMethod(name, type);
    } catch (NoSuchMethodException e) {
      throw new AssertionError("No builder method " + builderType.getName() + "." + name
          + "(" + type.getName() + ")");
    }
  }

  final int tag;
  final String name;
  final Message.Datatype datatype;
  final Message.Label label;
  final Class<? extends ProtoEnum> enumType;
  final Class<? extends Message> messageType;
  final boolean redacted;
  final TypeAdapter<Object> singleAdapter;
  final TypeAdapter<Object> adapter;

  private final Field messageField;
  private final Field builderField;
  private final Method builderMethod;

  private RuntimeFieldBinding(int tag, String name, Message.Datatype datatype,
      Message.Label label, boolean redacted, Class<? extends ProtoEnum> enumType,
      Class<? extends Message> messageType, TypeAdapter<Object> singleAdapter,
      TypeAdapter<Object> adapter, Field messageField, Field builderField, Method builderMethod) {
    this.tag = tag;
    this.name = name;
    this.datatype = datatype;
    this.label = label;
    this.redacted = redacted;
    this.enumType = enumType;
    this.messageType = messageType;
    this.singleAdapter = singleAdapter;
    this.adapter = adapter;
    this.messageField = messageField;
    this.builderField = builderField;
    this.builderMethod = builderMethod;
  }

  int serializedSize(Object message) {
    Object value = getValue(message);
    if (value == null) {
      return 0;
    }
    return adapter.serializedSize(tag, value);
  }

  void write(Object message, ProtoWriter writer) throws IOException {
    Object value = getValue(message);
    if (value != null) {
      writer.write(tag, value, adapter);
    }
  }

  boolean addToString(Object message, StringBuilder builder, boolean seenValue) {
    Object value = getValue(message);
    if (value == null) {
      return false;
    }
    if (seenValue) {
      builder.append(", ");
    }
    builder.append(name);
    builder.append("=");
    builder.append(redacted ? "██" : value);
    return true;
  }

  Object getValue(Object message) {
    try {
      return messageField.get(message);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  void setBuilderField(Object builder, Object value) {
    try {
      builderField.set(builder, value);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  public void redactBuilderField(Message.Builder<?> builder) {
    if (!redacted && datatype != Message.Datatype.MESSAGE) {
      return;
    }
    if (redacted && label == Message.Label.REQUIRED) {
      throw new IllegalArgumentException(
          String.format("Field %s.%s is REQUIRED and cannot be redacted.",
              messageField.getDeclaringClass().getName(), messageField.getName()));
    }

    Object builderValue = getBuilderValue(builder);
    if (builderValue != null) {
      Object redactedValue = adapter.redact(builderValue);
      setBuilderField(builder, redactedValue);
    }
  }

  private Object getBuilderValue(Object builder) {
    try {
      return builderField.get(builder);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  void setBuilderMethod(Object builder, Object value) {
    try {
      builderMethod.invoke(builder, value);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    } catch (InvocationTargetException e) {
      throw new AssertionError(e);
    }
  }
}
