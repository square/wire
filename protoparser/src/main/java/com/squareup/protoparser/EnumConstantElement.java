// Copyright 2014 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.List;

import static com.squareup.protoparser.OptionElement.formatOptionList;
import static com.squareup.protoparser.Utils.appendDocumentation;
import static com.squareup.protoparser.Utils.checkNotNull;
import static com.squareup.protoparser.Utils.immutableCopyOf;

/** An enum constant. */
@AutoValue
public abstract class EnumConstantElement {
  public static final int UNKNOWN_TAG = -1;

  /** Used to represent enums constants where we just know the name. */
  static EnumConstantElement anonymous(String name) {
    return builder().name(name).tag(UNKNOWN_TAG).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  EnumConstantElement() {
  }

  public abstract String name();
  public abstract int tag();
  public abstract String documentation();
  public abstract List<OptionElement> options();

  @Override public final String toString() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    builder.append(name())
        .append(" = ")
        .append(tag());
    if (!options().isEmpty()) {
      builder.append(" [\n");
      formatOptionList(builder, options());
      builder.append(']');
    }
    return builder.append(";\n").toString();
  }

  public static final class Builder {
    private String name;
    private Integer tag;
    private String documentation = "";
    private final List<OptionElement> options = new ArrayList<>();

    private Builder() {
    }

    public Builder name(String name) {
      this.name = checkNotNull(name, "name");
      return this;
    }

    public Builder tag(int tag) {
      this.tag = tag;
      return this;
    }

    public Builder documentation(String documentation) {
      this.documentation = checkNotNull(documentation, "documentation");
      return this;
    }

    public Builder addOption(OptionElement option) {
      options.add(checkNotNull(option, "option"));
      return this;
    }

    public EnumConstantElement build() {
      return new AutoValue_EnumConstantElement(name, tag, documentation, immutableCopyOf(options));
    }
  }
}
