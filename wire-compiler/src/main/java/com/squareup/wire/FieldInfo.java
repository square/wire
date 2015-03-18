// Copyright 2013 Square, Inc.
package com.squareup.wire;

import com.squareup.protoparser.FieldElement;

final class FieldInfo {
  final String name;
  final FieldElement.Label label;

  FieldInfo(String name, FieldElement.Label label) {
    this.name = name;
    this.label = label;
  }

  public static boolean isOptional(FieldElement field) {
    return field.label() == FieldElement.Label.OPTIONAL;
  }

  public static boolean isPacked(FieldElement field, boolean isEnum) {
    return field.isPacked() && (isEnum || isPackableScalar(field));
  }

  private static boolean isPackableScalar(FieldElement field) {
    String type = field.type().toString();
    return TypeInfo.isScalar(type) && !"string".equals(type) && !"bytes".equals(type);
  }

  public static boolean isRepeated(FieldElement field) {
    return field.label() == FieldElement.Label.REPEATED;
  }

  public static boolean isRequired(FieldElement field) {
    return field.label() == FieldElement.Label.REQUIRED;
  }

  public boolean isRepeated() {
    return label == FieldElement.Label.REPEATED;
  }
}
