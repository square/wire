// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** An enumerated type declaration. */
public final class EnumType implements Type {
  private final String name;
  private final String fqname;
  private final String documentation;
  private final List<Value> values;

  public EnumType(String name, String fqname, String documentation, List<Value> values) {
    if (name == null) throw new NullPointerException("name");
    if (fqname == null) throw new NullPointerException("fqname");
    if (documentation == null) throw new NullPointerException("documentation");
    if (values == null) throw new NullPointerException("values");
    this.name = name;
    this.fqname = fqname;
    this.documentation = documentation;
    this.values = Collections.unmodifiableList(new ArrayList<Value>(values));
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

  public List<Value> getValues() {
    return values;
  }

  @Override public List<Type> getNestedTypes() {
    return Collections.emptyList();
  }

  @Override public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof EnumType)) return false;

    EnumType that = (EnumType) other;
    return name.equals(that.name) //
        && fqname.equals(that.fqname) //
        && documentation.equals(that.documentation) //
        && values.equals(that.values);
  }

  @Override public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + fqname.hashCode();
    result = 31 * result + documentation.hashCode();
    result = 31 * result + values.hashCode();
    return result;
  }

  @Override public String toString() {
    StringBuilder result = new StringBuilder();
    result.append(name);
    for (Value value : values) {
      result.append("\n  ").append(value);
    }
    return result.toString();
  }

  /** An enum constant. */
  public static final class Value {
    private final String name;
    private final int tag;
    private final String documentation;
    private final List<Option> options;

    public Value(String name, int tag, String documentation, List<Option> options) {
      if (name == null) throw new NullPointerException("name");
      if (documentation == null) throw new NullPointerException("documentation");
      if (options == null) throw new NullPointerException("options");
      this.name = name;
      this.tag = tag;
      this.documentation = documentation;
      this.options = Collections.unmodifiableList(new ArrayList<Option>(options));
    }

    public String getName() {
      return name;
    }

    public int getTag() {
      return tag;
    }

    public String getDocumentation() {
      return documentation;
    }

    public List<Option> getOptions() {
      return options;
    }

    @Override public boolean equals(Object other) {
      if (this == other) return true;
      if (!(other instanceof Value)) return false;

      Value that = (Value) other;
      return name.equals(that.name) //
          && tag == that.tag //
          && documentation.equals(that.documentation) //
          && options.equals(that.options);
    }

    @Override public int hashCode() {
      int result = name.hashCode();
      result = 31 * result + tag;
      result = 31 * result + documentation.hashCode();
      result = 31 * result + options.hashCode();
      return result;
    }

    @Override public String toString() {
      StringBuilder sb = new StringBuilder(String.format("%s = %d", name, tag));
      for (Option option : options) {
        sb.append("\n    option: ").append(option);
      }
      return sb.toString();
    }
  }
}
