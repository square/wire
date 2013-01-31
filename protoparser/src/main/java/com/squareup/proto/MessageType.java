// Copyright 2013 Square, Inc.
package com.squareup.proto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MessageType implements Type {
  private final String name;
  private final String documentation;
  private final List<Field> fields;
  private final List<Type> nestedTypes;

  MessageType(String name, String documentation, List<Field> fields, List<Type> nestedTypes) {
    if (name == null) throw new NullPointerException("name");
    if (documentation == null) throw new NullPointerException("documentation");
    if (fields == null) throw new NullPointerException("fields");
    if (nestedTypes == null) throw new NullPointerException("nestedTypes");
    this.name = name;
    this.documentation = documentation;
    this.fields = Collections.unmodifiableList(new ArrayList<Field>(fields));
    this.nestedTypes = Collections.unmodifiableList(new ArrayList<Type>(nestedTypes));
  }

  @Override public String getName() {
    return name;
  }

  public String getDocumentation() {
    return documentation;
  }

  public List<Field> getFields() {
    return fields;
  }

  @Override public List<Type> getNestedTypes() {
    return nestedTypes;
  }

  @Override public boolean equals(Object other) {
    if (other instanceof MessageType) {
      MessageType that = (MessageType) other;
      return name.equals(that.name)
          && documentation.equals(that.documentation)
          && fields.equals(that.fields)
          && nestedTypes.equals(that.nestedTypes);
    }
    return false;
  }

  @Override public int hashCode() {
    return name.hashCode();
  }

  @Override public String toString() {
    StringBuilder result = new StringBuilder();
    result.append(name);
    for (Field field : fields) {
      result.append("\n  ").append(field);
    }
    for (Type type : nestedTypes) {
      result.append(type).append("\n");
    }
    return result.toString();
  }

  enum Label {
    OPTIONAL, REQUIRED, REPEATED
  }

  public static final class Field {
    private final Label label;
    private final String type;
    private final String name;
    private final int tag;
    private final Map<String, Object> extensions;
    private final String documentation;

    Field(Label label, String type, String name, int tag, String documentation,
        Map<String, Object> extensions) {
      if (label == null) throw new NullPointerException("label");
      if (type == null) throw new NullPointerException("type");
      if (name == null) throw new NullPointerException("name");
      if (documentation == null) throw new NullPointerException("documentation");
      if (extensions == null) throw new NullPointerException("extensions");

      this.label = label;
      this.type = type;
      this.name = name;
      this.tag = tag;
      this.documentation = documentation;
      this.extensions = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(extensions));
    }

    public Label getLabel() {
      return label;
    }

    public String getType() {
      return type;
    }

    public String getName() {
      return name;
    }

    public int getTag() {
      return tag;
    }

    public Map<String, Object> getExtensions() {
      return extensions;
    }

    public String getDocumentation() {
      return documentation;
    }

    public boolean isDeprecated() {
      return "true".equals(extensions.get("deprecated"));
    }

    public String getDefault() {
      return (String) extensions.get("default");
    }

    @Override public boolean equals(Object other) {
      if (other instanceof Field) {
        Field that = (Field) other;
        return label.equals(that.label)
            && type.equals(that.type)
            && name.equals(that.name)
            && tag == that.tag
            && extensions.equals(that.extensions)
            && documentation.equals(that.documentation);
      }
      return false;
    }

    @Override public int hashCode() {
      return name.hashCode() + (37 * type.hashCode());
    }

    @Override public String toString() {
      return String.format("%s %s %s = %d %s", label, type, name, tag, extensions);
    }
  }
}
