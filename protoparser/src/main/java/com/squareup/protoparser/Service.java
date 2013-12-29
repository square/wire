// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    this.options = Collections.unmodifiableList(new ArrayList<Option>(options));
    this.methods = Collections.unmodifiableList(new ArrayList<Method>(methods));
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
    StringBuilder result = new StringBuilder();
    result.append(name);
    for (Option option : options) {
      result.append("\n  option: ").append(option.getName()).append('=').append(option.getValue());
    }
    for (Method method : methods) {
      result.append("\n  ").append(method);
    }
    return result.toString();
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
      this.options = Collections.unmodifiableList(new ArrayList<Option>(options));
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
      return String.format("rpc %s (%s) returns (%s) %s", name, requestType, responseType, options);
    }
  }
}
