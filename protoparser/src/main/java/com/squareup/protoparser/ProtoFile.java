// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

/** A single {@code .proto} file. */
public final class ProtoFile {
  private final String fileName;
  private final String packageName;
  private final List<String> dependencies;
  private final List<String> publicDependencies;
  private final List<Type> types;
  private final List<Service> services;
  private final Map<String, Object> options;
  private final List<ExtendDeclaration> extendDeclarations;

  ProtoFile(String fileName, String packageName, List<String> dependencies,
      List<String> publicDependencies, List<Type> types, List<Service> services,
      Map<String, Object> options, List<ExtendDeclaration> extendDeclarations) {
    if (fileName == null) throw new NullPointerException("fileName");
    if (dependencies == null) throw new NullPointerException("dependencies");
    if (publicDependencies == null) throw new NullPointerException("publicDependencies");
    if (types == null) throw new NullPointerException("types");
    if (services == null) throw new NullPointerException("services");
    if (options == null) throw new NullPointerException("options");
    if (extendDeclarations == null) throw new NullPointerException("extendDeclarations");

    this.fileName = fileName;
    this.packageName = packageName;
    this.dependencies = unmodifiableList(new ArrayList<String>(dependencies));
    this.publicDependencies = unmodifiableList(new ArrayList<String>(publicDependencies));
    this.types = unmodifiableList(new ArrayList<Type>(types));
    this.services = unmodifiableList(new ArrayList<Service>(services));
    this.options = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(options));
    this.extendDeclarations =
        unmodifiableList(new ArrayList<ExtendDeclaration>(extendDeclarations));
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

  public List<String> getPublicDependencies() {
    return publicDependencies;
  }

  public List<Type> getTypes() {
    return types;
  }

  public List<Service> getServices() {
    return services;
  }

  public Map<String, Object> getOptions() {
    return options;
  }

  public List<ExtendDeclaration> getExtendDeclarations() {
    return extendDeclarations;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ProtoFile)) return false;

    ProtoFile that = (ProtoFile) o;
    return dependencies.equals(that.dependencies)
        && extendDeclarations.equals(that.extendDeclarations)
        && fileName.equals(that.fileName)
        && options.equals(that.options)
        && packageName == null ? that.packageName == null : packageName.equals(that.packageName)
        && publicDependencies.equals(that.publicDependencies)
        && services.equals(that.services)
        && types.equals(that.types);
  }

  @Override public int hashCode() {
    int result = fileName.hashCode();
    result = 31 * result + (packageName != null ? packageName.hashCode() : 0);
    result = 31 * result + dependencies.hashCode();
    result = 31 * result + publicDependencies.hashCode();
    result = 31 * result + types.hashCode();
    result = 31 * result + services.hashCode();
    result = 31 * result + options.hashCode();
    result = 31 * result + extendDeclarations.hashCode();
    return result;
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
    for (Service service : services) {
      result.append(service).append('\n');
    }
    for (ExtendDeclaration extendDeclaration : extendDeclarations) {
      result.append(extendDeclaration).append('\n');
    }
    return result.toString();
  }
}
