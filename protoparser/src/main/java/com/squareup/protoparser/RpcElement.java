// Copyright 2014 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;
import java.util.List;

import static com.squareup.protoparser.Utils.appendDocumentation;
import static com.squareup.protoparser.Utils.appendIndented;
import static com.squareup.protoparser.Utils.immutableCopyOf;

@AutoValue
public abstract class RpcElement {
  public static RpcElement create(String name, String documentation, String requestType,
      String responseType, List<OptionElement> options) {
    return new AutoValue_RpcElement(name, documentation, requestType, responseType,
        immutableCopyOf(options, "options"));
  }

  RpcElement() {
  }

  public abstract String name();
  public abstract String documentation();
  public abstract String requestType();
  public abstract String responseType();
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
}
