// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.util.ArrayList;
import java.util.List;

import static com.squareup.protoparser.ProtoFile.isValidTag;
import static java.util.Collections.unmodifiableList;

public final class MessageType implements Type {
  private final String name;
  private final String fqname;
  private final String documentation;
  private final List<Field> fields;
  private final List<Type> nestedTypes;
  private final List<Extensions> extensions;
  private final List<Option> options;

  public MessageType(String name, String fqname, String documentation, List<Field> fields,
      List<Type> nestedTypes, List<Extensions> extensions, List<Option> options) {
    if (name == null) throw new NullPointerException("name");
    if (fqname == null) throw new NullPointerException("fqname");
    if (documentation == null) throw new NullPointerException("documentation");
    if (fields == null) throw new NullPointerException("fields");
    if (nestedTypes == null) throw new NullPointerException("nestedTypes");
    if (extensions == null) throw new NullPointerException("extensions");
    if (options == null) throw new NullPointerException("options");
    this.name = name;
    this.fqname = fqname;
    this.documentation = documentation;
    this.fields = unmodifiableList(new ArrayList<Field>(fields));
    this.nestedTypes = unmodifiableList(new ArrayList<Type>(nestedTypes));
    this.extensions = unmodifiableList(new ArrayList<Extensions>(extensions));
    this.options = unmodifiableList(new ArrayList<Option>(options));
  }

  @Override public String getName() {
    return name;
  }

  @Override public String getFullyQualifiedName() {
    return fqname;
  }

  @Override public String getDocumentation() {
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

  @Override public List<Option> getOptions() {
    return options;
  }

  @Override public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof MessageType)) return false;

    MessageType that = (MessageType) other;
    return name.equals(that.name)
        && fqname.equals(that.fqname)
        && documentation.equals(that.documentation)
        && fields.equals(that.fields)
        && nestedTypes.equals(that.nestedTypes)
        && extensions.equals(that.extensions)
        && options.equals(that.options);
  }

  @Override public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + fqname.hashCode();
    result = 31 * result + documentation.hashCode();
    result = 31 * result + fields.hashCode();
    result = 31 * result + nestedTypes.hashCode();
    result = 31 * result + extensions.hashCode();
    result = 31 * result + options.hashCode();
    return result;
  }

  @Override public String toString() {
    StringBuilder result = new StringBuilder();
    result.append(name);
    for (Option option : options) {
      result.append("\n  option: ").append(option);
    }
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
    private final List<Option> options;
    private final String documentation;

    public Field(Label label, String type, String name, int tag, String documentation,
        List<Option> options) {
      if (label == null) throw new NullPointerException("label");
      if (type == null) throw new NullPointerException("type");
      if (!isValidTag(tag)) throw new IllegalArgumentException("Illegal tag value: " + tag);
      if (name == null) throw new NullPointerException("name");
      if (documentation == null) throw new NullPointerException("documentation");
      if (options == null) throw new NullPointerException("options");

      this.label = label;
      this.type = type;
      this.name = name;
      this.tag = tag;
      this.documentation = documentation;
      this.options = unmodifiableList(new ArrayList<Option>(options));
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

    public List<Option> getOptions() {
      return options;
    }

    public String getDocumentation() {
      return documentation;
    }

    public boolean isDeprecated() {
      return "true".equals(getOptionValue("deprecated"));
    }

    public boolean isPacked() {
      return "true".equals(getOptionValue("packed"));
    }

    public String getDefault() {
      return (String) getOptionValue("default");
    }

    private Object getOptionValue(String name) {
      for (Option option : options) {
        if (option.getName().equals(name)) {
          return option.getValue();
        }
      }
      return null;
    }

    @Override public boolean equals(Object other) {
      if (this == other) return true;
      if (!(other instanceof Field)) return false;

      Field that = (Field) other;
      return label.equals(that.label)
          && type.equals(that.type)
          && name.equals(that.name)
          && tag == that.tag
          && options.equals(that.options)
          && documentation.equals(that.documentation);
    }

    @Override public int hashCode() {
      int result = label.hashCode();
      result = 31 * result + type.hashCode();
      result = 31 * result + name.hashCode();
      result = 31 * result + tag;
      result = 31 * result + options.hashCode();
      result = 31 * result + documentation.hashCode();
      return result;
    }

    @Override public String toString() {
      return String.format("%s %s %s = %d %s", label, type, name, tag, options);
    }
  }
}
