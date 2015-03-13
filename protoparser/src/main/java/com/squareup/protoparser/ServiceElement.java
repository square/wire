// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.List;

import static com.squareup.protoparser.Utils.appendDocumentation;
import static com.squareup.protoparser.Utils.appendIndented;
import static com.squareup.protoparser.Utils.checkNotNull;
import static com.squareup.protoparser.Utils.immutableCopyOf;

@AutoValue
public abstract class ServiceElement {
  public static Builder builder() {
    return new Builder();
  }

  public abstract String name();
  public abstract String qualifiedName();
  public abstract String documentation();
  public abstract List<OptionElement> options();
  public abstract List<RpcElement> rpcs();

  ServiceElement() {
  }

  @Override public final String toString() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    builder.append("service ")
        .append(name())
        .append(" {");
    if (!options().isEmpty()) {
      builder.append('\n');
      for (OptionElement option : options()) {
        appendIndented(builder, option.toDeclaration());
      }
    }
    if (!rpcs().isEmpty()) {
      builder.append('\n');
      for (RpcElement rpc : rpcs()) {
        appendIndented(builder, rpc.toString());
      }
    }
    return builder.append("}\n").toString();
  }

  public static final class Builder {
    private String name;
    private String qualifiedName;
    private String documentation = "";
    private final List<OptionElement> options = new ArrayList<>();
    private final List<RpcElement> rpcs = new ArrayList<>();

    private Builder() {
    }

    public Builder name(String name) {
      this.name = checkNotNull(name, "name");
      if (qualifiedName == null) {
        qualifiedName = name;
      }
      return this;
    }

    public Builder qualifiedName(String qualifiedName) {
      this.qualifiedName = checkNotNull(qualifiedName, "qualifiedName");
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

    public Builder addRpc(RpcElement rpc) {
      rpcs.add(checkNotNull(rpc, "rpc"));
      return this;
    }

    public ServiceElement build() {
      checkNotNull(name, "name");
      checkNotNull(qualifiedName, "qualifiedName");

      return new AutoValue_ServiceElement(name, qualifiedName, documentation,
          immutableCopyOf(options), immutableCopyOf(rpcs));
    }
  }
}
