// Copyright 2013 Square, Inc.
package com.squareup.proto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A single {@code .proto} file. */
public final class ProtoFile {
  private final String fileName;
  private final String packageName;
  private final List<String> dependencies;
  private final List<Type> types;
  private final Map<String, Object> options;

  ProtoFile(String fileName, String packageName, List<String> dependencies,
      List<Type> types, Map<String, Object> options) {
    if (fileName == null) throw new NullPointerException("fileName");
    if (dependencies == null) throw new NullPointerException("dependencies");
    if (types == null) throw new NullPointerException("types");
    if (options == null) throw new NullPointerException("options");

    this.fileName = fileName;
    this.packageName = packageName;
    this.dependencies = Collections.unmodifiableList(new ArrayList<String>(dependencies));
    this.types = Collections.unmodifiableList(new ArrayList<Type>(types));
    this.options = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(options));
  }

  /** Returns the Java package, or the default package if no Java package is specified. */
  public String getJavaPackage() {
    Object javaPackage = options.get("java_package");
    return javaPackage != null ? (String) javaPackage : packageName;
  }

  public String getFileName() {
    return fileName;
  }

  public String getPackageName() {
    return packageName;
  }

  public List<String> getDependencies() {
    return dependencies;
  }

  public List<Type> getTypes() {
    return types;
  }

  public Map<String, Object> getOptions() {
    return options;
  }

  @Override public boolean equals(Object other) {
    if (other instanceof ProtoFile) {
      ProtoFile that = (ProtoFile) other;
      return eq(fileName, that.fileName)
          && eq(packageName, that.packageName)
          && eq(dependencies, that.dependencies)
          && eq(types, that.types)
          && eq(options, that.options);
    }
    return false;
  }

  private static boolean eq(Object a, Object b) {
    return a == b || a != null && a.equals(b);
  }

  @Override public int hashCode() {
    return fileName.hashCode();
  }

  @Override public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("fileName: ").append(fileName).append('\n');
    result.append("packageName: ").append(packageName).append('\n');
    for (Map.Entry<String, Object> option : options.entrySet()) {
      result.append("option ").append(option.getKey()).append(" = ").append(option.getValue())
          .append('\n');
    }
    for (String dependency : dependencies) {
      result.append("import ").append(dependency).append('\n');
    }
    for (Type type : types) {
      result.append(type).append('\n');
    }
    return result.toString();
  }
}
