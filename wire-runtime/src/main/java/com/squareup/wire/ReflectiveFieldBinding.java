package com.squareup.wire;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

final class ReflectiveFieldBinding {
  static ReflectiveFieldBinding create(Wire wire, ProtoField protoField, Field messageField,
      Class<?> builderType) {
    int tag = protoField.tag();
    Message.Datatype datatype = protoField.type();
    Message.Label label = protoField.label();
    boolean redacted = protoField.redacted();
    Class<? extends ProtoEnum> enumType = null;
    Class<? extends Message> messageType = null;
    if (datatype == Message.Datatype.ENUM) {
      enumType = getEnumType(messageField);
    } else if (datatype == Message.Datatype.MESSAGE) {
      messageType = getMessageType(messageField);
    }
    TypeAdapter<Object> adapter =
        (TypeAdapter<Object>) ReflectiveMessageAdapter.typeAdapter(wire, datatype, messageType,
            enumType);
    String name = messageField.getName();
    Field builderField = getBuilderField(builderType, name);
    Method builderMethod = getBuilderMethod(builderType, name, messageField.getType());
    return new ReflectiveFieldBinding(tag, name, datatype, label, redacted, enumType, messageType,
        adapter, messageField, builderField, builderMethod);
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
  final TypeAdapter<Object> adapter;

  private final Field messageField;
  private final Field builderField;
  private final Method builderMethod;

  private ReflectiveFieldBinding(int tag, String name, Message.Datatype datatype,
      Message.Label label, boolean redacted, Class<? extends ProtoEnum> enumType,
      Class<? extends Message> messageType, TypeAdapter<Object> adapter, Field messageField,
      Field builderField, Method builderMethod) {
    this.tag = tag;
    this.name = name;
    this.datatype = datatype;
    this.label = label;
    this.redacted = redacted;
    this.enumType = enumType;
    this.messageType = messageType;
    this.adapter = adapter;
    this.messageField = messageField;
    this.builderField = builderField;
    this.builderMethod = builderMethod;
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
