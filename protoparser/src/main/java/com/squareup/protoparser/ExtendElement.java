// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;
import java.util.List;

import static com.squareup.protoparser.MessageElement.Field;
import static com.squareup.protoparser.MessageElement.validateFieldTagUniqueness;
import static com.squareup.protoparser.Utils.appendDocumentation;
import static com.squareup.protoparser.Utils.appendIndented;
import static com.squareup.protoparser.Utils.immutableCopyOf;

@AutoValue
public abstract class ExtendElement {
  public static ExtendElement create(String name, String qualifiedName, String documentation,
      List<Field> fields) {
    validateFieldTagUniqueness(qualifiedName, fields);
    return new AutoValue_ExtendElement(name, qualifiedName, documentation,
        immutableCopyOf(fields, "fields"));
  }

  ExtendElement() {
  }

  public abstract String name();
  public abstract String qualifiedName();
  public abstract String documentation();
  public abstract List<Field> fields();

  @Override public final String toString() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    builder.append("extend ")
        .append(name())
        .append(" {");
    if (!fields().isEmpty()) {
      builder.append('\n');
      for (Field field : fields()) {
        appendIndented(builder, field.toString());
      }
    }
    return builder.append("}\n").toString();
  }
}
