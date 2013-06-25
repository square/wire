// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    this.fields = Collections.unmodifiableList(new ArrayList<MessageType.Field>(fields));
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
    if (other instanceof ExtendDeclaration) {
      ExtendDeclaration that = (ExtendDeclaration) other;
      return name.equals(that.name)
          && documentation.equals(that.documentation)
          && fields.equals(that.fields);
    }
    return false;
  }

  @Override public int hashCode() {
    return name.hashCode();
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
