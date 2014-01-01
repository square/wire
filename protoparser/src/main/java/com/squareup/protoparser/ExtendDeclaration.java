// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.util.ArrayList;
import java.util.List;

import static com.squareup.protoparser.MessageType.Field;
import static com.squareup.protoparser.Utils.appendDocumentation;
import static com.squareup.protoparser.Utils.appendIndented;
import static java.util.Collections.unmodifiableList;

public final class ExtendDeclaration {
  private final String name;
  private final String fqname;
  private final String documentation;
  private final List<Field> fields;

  public ExtendDeclaration(String name, String fqname, String documentation, List<Field> fields) {
    Field.validateTagUniqueness(fqname, fields);

    this.name = name;
    this.fqname = fqname;
    this.documentation = documentation;
    this.fields = unmodifiableList(new ArrayList<Field>(fields));
  }

  public String getName() {
    return name;
  }

  public String getFullyQualifiedName() {
    return fqname;
  }

  public String getDocumentation() {
    return documentation;
  }

  public List<Field> getFields() {
    return fields;
  }

  @Override public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof ExtendDeclaration)) return false;

    ExtendDeclaration that = (ExtendDeclaration) other;
    return name.equals(that.name)
        && fqname.equals(that.fqname)
        && documentation.equals(that.documentation)
        && fields.equals(that.fields);
  }

  @Override public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + fqname.hashCode();
    result = 31 * result + documentation.hashCode();
    result = 31 * result + fields.hashCode();
    return result;
  }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation);
    builder.append("extend ")
        .append(name)
        .append(" {");
    if (!fields.isEmpty()) {
      builder.append('\n');
      for (Field field : fields) {
        appendIndented(builder, field.toString());
      }
    }
    return builder.append("}\n").toString();
  }
}
