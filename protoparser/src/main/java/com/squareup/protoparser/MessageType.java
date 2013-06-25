// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MessageType implements Type {
  private final String name;
  private final String fqname;
  private final String documentation;
  private final List<Field> fields;
  private final List<Type> nestedTypes;
  private final List<Extensions> extensions;

  MessageType(String name, String fqname, String documentation, List<Field> fields,
      List<Type> nestedTypes, List<Extensions> extensions) {
    if (name == null) throw new NullPointerException("name");
    if (fqname == null) throw new NullPointerException("fqname");
    if (documentation == null) throw new NullPointerException("documentation");
    if (fields == null) throw new NullPointerException("fields");
    if (nestedTypes == null) throw new NullPointerException("nestedTypes");
    if (extensions == null) throw new NullPointerException("extensions");
    this.name = name;
    this.fqname = fqname;
    this.documentation = documentation;
    this.fields = Collections.unmodifiableList(new ArrayList<Field>(fields));
    this.nestedTypes = Collections.unmodifiableList(new ArrayList<Type>(nestedTypes));
    this.extensions = Collections.unmodifiableList(new ArrayList<Extensions>(extensions));
  }

  @Override public String getName() {
    return name;
  }

  @Override public String getFullyQualifiedName() {
    return fqname;
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

  public List<Extensions> getExtensions() {
    return extensions;
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

  public enum Label {
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

    /**
     * Returns the type of this field. May be a message type name, an enum type
     * name, or a <a href="https://developers.google.com/protocol-buffers/docs/proto#scalar">
     * scalar value type</a> like {@code int64} or {@code bytes}.
     */
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
