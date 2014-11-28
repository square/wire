// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;
import java.util.List;

import static com.squareup.protoparser.Utils.appendDocumentation;
import static com.squareup.protoparser.Utils.appendIndented;
import static com.squareup.protoparser.Utils.immutableCopyOf;

@AutoValue
public abstract class ServiceElement {
  public static ServiceElement create(String name, String qualifiedName, String documentation,
      List<OptionElement> options, List<MethodElement> methods) {
    return new AutoValue_ServiceElement(name, qualifiedName, documentation,
        immutableCopyOf(options, "options"), immutableCopyOf(methods, "methods"));
  }

  public abstract String name();
  public abstract String qualifiedName();
  public abstract String documentation();
  public abstract List<OptionElement> options();
  public abstract List<MethodElement> methods();

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
    if (!methods().isEmpty()) {
      builder.append('\n');
      for (MethodElement method : methods()) {
        appendIndented(builder, method.toString());
      }
    }
    return builder.append("}\n").toString();
  }

  @AutoValue
  public abstract static class MethodElement {
    public static MethodElement create(String name, String documentation, String requestType,
        String responseType, List<OptionElement> options) {
      return new AutoValue_ServiceElement_MethodElement(name, documentation, requestType,
          responseType, immutableCopyOf(options, "options"));
    }

    MethodElement() {
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
}
