// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public final class ExtendDeclaration {
  private final String name;
  private final String fqname;
  private final String documentation;
  private final List<MessageType.Field> fields;

  public ExtendDeclaration(String name, String fqname, String documentation,
      List<MessageType.Field> fields) {
    this.name = name;
    this.fqname = fqname;
    this.documentation = documentation;
    this.fields = unmodifiableList(new ArrayList<MessageType.Field>(fields));
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

  public List<MessageType.Field> getFields() {
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
    StringBuilder result = new StringBuilder();
    result.append(String.format("extend %s %s %s", name, fqname, documentation));
    for (MessageType.Field field : fields) {
      result.append("\n  ").append(field);
    }
    return result.toString();
  }
}
