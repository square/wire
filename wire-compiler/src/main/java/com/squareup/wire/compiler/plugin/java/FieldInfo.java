package com.squareup.wire.compiler.plugin.java;

import com.squareup.protoparser.MessageType;

final class FieldInfo {

  final String name;
  final MessageType.Label label;

  FieldInfo(String name, MessageType.Label label) {
    this.name = name;
    this.label = label;
  }

  public static boolean isOptional(MessageType.Field field) {
    return field.getLabel() == MessageType.Label.OPTIONAL;
  }

  public static boolean isPacked(MessageType.Field field, boolean isEnum) {
    return field.isPacked() && (isEnum || isPackableScalar(field));
  }

  private static boolean isPackableScalar(MessageType.Field field) {
    String type = field.getType();
    return TypeInfo.isScalar(type) && !"string".equals(type) && !"bytes".equals(type);
  }

  public static boolean isRepeated(MessageType.Field field) {
    return field.getLabel() == MessageType.Label.REPEATED;
  }

  public static boolean isRequired(MessageType.Field field) {
    return field.getLabel() == MessageType.Label.REQUIRED;
  }

  public boolean isRepeated() {
    return label == MessageType.Label.REPEATED;
  }
}
