// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Service {
  private final String name;
  private final String fqname;
  private final String documentation;
  private final List<Method> methods;

  public Service(String name, String fqname, String documentation, List<Method> methods) {
    if (name == null) throw new NullPointerException("name");
    if (fqname == null) throw new NullPointerException("fqname");
    if (documentation == null) throw new NullPointerException("documentation");
    if (methods == null) throw new NullPointerException("methods");
    this.name = name;
    this.fqname = fqname;
    this.documentation = documentation;
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
        && methods.equals(that.methods);
  }

  @Override public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + fqname.hashCode();
    result = 31 * result + documentation.hashCode();
    result = 31 * result + methods.hashCode();
    return result;
  }

  @Override public String toString() {
    StringBuilder result = new StringBuilder();
    result.append(name);
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
    private final Map<String, Object> options;

    public Method(String name, String documentation, String requestType, String responseType,
        Map<String, Object> options) {
      if (name == null) throw new NullPointerException("name");
      if (documentation == null) throw new NullPointerException("documentation");
      if (requestType == null) throw new NullPointerException("requestType");
      if (responseType == null) throw new NullPointerException("responseType");
      if (options == null) throw new NullPointerException("options");
      this.name = name;
      this.documentation = documentation;
      this.requestType = requestType;
      this.responseType = responseType;
      this.options = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(options));
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

    public Map<String, Object> getOptions() {
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
