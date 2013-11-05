// Copyright 2013 Square, Inc.
package com.squareup.wire;

import com.squareup.protoparser.MessageType;

import static com.squareup.protoparser.MessageType.Field;

final class FieldInfo {

  final String name;
  final MessageType.Label label;

  FieldInfo(String name, MessageType.Label label) {
    this.name = name;
    this.label = label;
  }

  public static boolean isOptional(Field field) {
    return field.getLabel() == MessageType.Label.OPTIONAL;
  }

  public static boolean isPacked(Field field, boolean isEnum) {
    return field.isPacked() && (isEnum || isPackableScalar(field));
  }

  private static boolean isPackableScalar(Field field) {
    String type = field.getType();
    return TypeInfo.isScalar(type) && !"string".equals(type) && !"bytes".equals(type);
  }

  public static boolean isRepeated(Field field) {
    return field.getLabel() == MessageType.Label.REPEATED;
  }

  public static boolean isRequired(Field field) {
    return field.getLabel() == MessageType.Label.REQUIRED;
  }

  public boolean isRepeated() {
    return label == MessageType.Label.REPEATED;
  }
}
