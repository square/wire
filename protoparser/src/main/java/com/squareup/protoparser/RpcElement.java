// Copyright 2014 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;
import com.squareup.protoparser.DataType.NamedType;
import java.util.ArrayList;
import java.util.List;

import static com.squareup.protoparser.Utils.appendDocumentation;
import static com.squareup.protoparser.Utils.appendIndented;
import static com.squareup.protoparser.Utils.checkNotNull;
import static com.squareup.protoparser.Utils.immutableCopyOf;

@AutoValue
public abstract class RpcElement {
  public static Builder builder() {
    return new Builder();
  }

  RpcElement() {
  }

  public abstract String name();
  public abstract String documentation();
  public abstract NamedType requestType();
  public abstract NamedType responseType();
  public abstract List<OptionElement> options();

  @Override public final String toString() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    builder.append("rpc ")
        .append(name())
        .append(" (")
        .append(requestType())
        .append(") returns (")
        .append(responseType())
        .append(')');
    if (!options().isEmpty()) {
      builder.append(" {\n");
      for (OptionElement option : options()) {
        appendIndented(builder, option.toDeclaration());
      }
      builder.append("}");
    }
    return builder.append(";\n").toString();
  }

  public static final class Builder {
    private String name;
    private String documentation = "";
    private NamedType requestType;
    private NamedType responseType;
    private final List<OptionElement> options = new ArrayList<>();

    private Builder() {
    }

    public Builder name(String name) {
      this.name = checkNotNull(name, "name");
      return this;
    }

    public Builder documentation(String documentation) {
      this.documentation = checkNotNull(documentation, "documentation");
      return this;
    }

    public Builder requestType(NamedType requestType) {
      this.requestType = checkNotNull(requestType, "requestType");
      return this;
    }

    public Builder responseType(NamedType responseType) {
      this.responseType = checkNotNull(responseType, "responseType");
      return this;
    }

    public Builder addOption(OptionElement option) {
      options.add(checkNotNull(option, "option"));
      return this;
    }

    public RpcElement build() {
      checkNotNull(name, "name");
      checkNotNull(requestType, "requestType");
      checkNotNull(responseType, "responseType");

      return new AutoValue_RpcElement(name, documentation, requestType, responseType,
          immutableCopyOf(options));
    }
  }
}
