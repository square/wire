// Copyright 2014 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;
import java.util.List;

import static com.squareup.protoparser.Utils.appendDocumentation;
import static com.squareup.protoparser.Utils.appendIndented;
import static com.squareup.protoparser.Utils.immutableCopyOf;

@AutoValue
public abstract class OneOfElement {
  public static OneOfElement create(String name, String documentation, List<FieldElement> fields) {
    return new AutoValue_OneOfElement(name, documentation, immutableCopyOf(fields, "fields"));
  }

  OneOfElement() {
  }

  public abstract String name();
  public abstract String documentation();
  public abstract List<FieldElement> fields();

  @Override public final String toString() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    builder.append("oneof ").append(name()).append(" {");
    if (!fields().isEmpty()) {
      builder.append('\n');
      for (FieldElement field : fields()) {
        appendIndented(builder, field.toString());
      }
    }
    return builder.append("}\n").toString();
  }
}
