// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.util.ArrayList;
import java.util.List;

import static com.squareup.protoparser.Utils.appendDocumentation;
import static com.squareup.protoparser.Utils.appendIndented;
import static java.util.Collections.unmodifiableList;

public final class Service {
  private final String name;
  private final String fqname;
  private final String documentation;
  private final List<Option> options;
  private final List<Method> methods;

  public Service(String name, String fqname, String documentation, List<Option> options,
      List<Method> methods) {
    if (name == null) throw new NullPointerException("name");
    if (fqname == null) throw new NullPointerException("fqname");
    if (documentation == null) throw new NullPointerException("documentation");
    if (options == null) throw new NullPointerException("options");
    if (methods == null) throw new NullPointerException("methods");
    this.name = name;
    this.fqname = fqname;
    this.documentation = documentation;
    this.options = unmodifiableList(new ArrayList<Option>(options));
    this.methods = unmodifiableList(new ArrayList<Method>(methods));
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

  public List<Option> getOptions() {
    return options;
  }

  public List<Method> getMethods() {
    return methods;
  }

  @Override public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof Service)) return false;

    Service that = (Service) other;
    return name.equals(that.name)
        && fqname.equals(that.fqname)
        && documentation.equals(that.documentation)
        && options.equals(that.options)
        && methods.equals(that.methods);
  }

  @Override public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + fqname.hashCode();
    result = 31 * result + documentation.hashCode();
    result = 31 * result + options.hashCode();
    result = 31 * result + methods.hashCode();
    return result;
  }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation);
    builder.append("service ")
        .append(name)
        .append(" {");
    if (!options.isEmpty()) {
      builder.append('\n');
      for (Option option : options) {
        appendIndented(builder, option.toDeclaration());
      }
    }
    if (!methods.isEmpty()) {
      builder.append('\n');
      for (Method method : methods) {
        appendIndented(builder, method.toString());
      }
    }
    return builder.append("}\n").toString();
  }

  public static final class Method {
    private final String name;
    private final String documentation;
    private final String requestType;
    private final String responseType;
    private final List<Option> options;

    public Method(String name, String documentation, String requestType, String responseType,
        List<Option> options) {
      if (name == null) throw new NullPointerException("name");
      if (documentation == null) throw new NullPointerException("documentation");
      if (requestType == null) throw new NullPointerException("requestType");
      if (responseType == null) throw new NullPointerException("responseType");
      if (options == null) throw new NullPointerException("options");
      this.name = name;
      this.documentation = documentation;
      this.requestType = requestType;
      this.responseType = responseType;
      this.options = unmodifiableList(new ArrayList<Option>(options));
    }

    public String getName() {
      return name;
    }

    public String getDocumentation() {
      return documentation;
    }

    public String getRequestType() {
      return requestType;
    }

    public String getResponseType() {
      return responseType;
    }

    public List<Option> getOptions() {
      return options;
    }

    @Override public boolean equals(Object other) {
      if (this == other) return true;
      if (!(other instanceof Method)) return false;

      Method that = (Method) other;
      return name.equals(that.name)
          && documentation.equals(that.documentation)
          && requestType.equals(that.requestType)
          && responseType.equals(that.responseType)
          && options.equals(that.options);
    }

    @Override public int hashCode() {
      int result = name.hashCode();
      result = 31 * result + documentation.hashCode();
      result = 31 * result + requestType.hashCode();
      result = 31 * result + responseType.hashCode();
      result = 31 * result + options.hashCode();
      return result;
    }

    @Override public String toString() {
      StringBuilder builder = new StringBuilder();
      appendDocumentation(builder, documentation);
      builder.append("rpc ")
          .append(name)
          .append(" (")
          .append(requestType)
          .append(") returns (")
          .append(responseType)
          .append(')');
      if (!options.isEmpty()) {
        builder.append(" {\n");
        for (Option option : options) {
          appendIndented(builder, option.toDeclaration());
        }
        builder.append("}");
      }
      return builder.append(";\n").toString();
    }
  }
}
