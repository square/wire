// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

/** A single {@code .proto} file. */
public final class ProtoFile {
  public static final int MIN_TAG_VALUE = 1;
  public static final int MAX_TAG_VALUE = (1 << 29) - 1; // 536,870,911
  private static final int RESERVED_TAG_VALUE_START = 19000;
  private static final int RESERVED_TAG_VALUE_END = 19999;

  /** True if the supplied value is in the valid tag range and not reserved. */
  public static boolean isValidTag(int value) {
    return (value >= MIN_TAG_VALUE && value < RESERVED_TAG_VALUE_START)
        || (value > RESERVED_TAG_VALUE_END && value <= MAX_TAG_VALUE);
  }

  private final String fileName;
  private final String packageName;
  private final List<String> dependencies;
  private final List<String> publicDependencies;
  private final List<TypeElement> typeElements;
  private final List<ServiceElement> services;
  private final List<OptionElement> options;
  private final List<ExtendElement> extendDeclarations;

  public ProtoFile(String fileName, String packageName, List<String> dependencies,
      List<String> publicDependencies, List<TypeElement> typeElements,
      List<ServiceElement> services, List<OptionElement> options,
      List<ExtendElement> extendDeclarations) {
    if (fileName == null) throw new NullPointerException("fileName");
    if (dependencies == null) throw new NullPointerException("dependencies");
    if (publicDependencies == null) throw new NullPointerException("publicDependencies");
    if (typeElements == null) throw new NullPointerException("typeElements");
    if (services == null) throw new NullPointerException("services");
    if (options == null) throw new NullPointerException("options");
    if (extendDeclarations == null) throw new NullPointerException("extendDeclarations");

    this.fileName = fileName;
    this.packageName = packageName;
    this.dependencies = unmodifiableList(new ArrayList<String>(dependencies));
    this.publicDependencies = unmodifiableList(new ArrayList<String>(publicDependencies));
    this.typeElements = unmodifiableList(new ArrayList<TypeElement>(typeElements));
    this.services = unmodifiableList(new ArrayList<ServiceElement>(services));
    this.options = unmodifiableList(new ArrayList<OptionElement>(options));
    this.extendDeclarations =
        unmodifiableList(new ArrayList<ExtendElement>(extendDeclarations));
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

  public List<TypeElement> getTypeElements() {
    return typeElements;
  }

  public List<ServiceElement> getServices() {
    return services;
  }

  public List<OptionElement> getOptions() {
    return options;
  }

  public List<ExtendElement> getExtendDeclarations() {
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
        && (packageName == null ? that.packageName == null : packageName.equals(that.packageName))
        && publicDependencies.equals(that.publicDependencies)
        && services.equals(that.services)
        && typeElements.equals(that.typeElements);
  }

  @Override public int hashCode() {
    int result = fileName.hashCode();
    result = 31 * result + (packageName != null ? packageName.hashCode() : 0);
    result = 31 * result + dependencies.hashCode();
    result = 31 * result + publicDependencies.hashCode();
    result = 31 * result + typeElements.hashCode();
    result = 31 * result + services.hashCode();
    result = 31 * result + options.hashCode();
    result = 31 * result + extendDeclarations.hashCode();
    return result;
  }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    if (!fileName.isEmpty()) {
      builder.append("// ").append(fileName).append('\n');
    }
    if (packageName != null) {
      builder.append("package ").append(packageName).append(";\n");
    }
    if (!dependencies.isEmpty() || !publicDependencies.isEmpty()) {
      builder.append('\n');
      for (String dependency : dependencies) {
        builder.append("import \"").append(dependency).append("\";\n");
      }
      for (String publicDependency : publicDependencies) {
        builder.append("import public \"").append(publicDependency).append("\";\n");
      }
    }
    if (!options.isEmpty()) {
      builder.append('\n');
      for (OptionElement option : options) {
        builder.append(option.toDeclaration());
      }
    }
    if (!typeElements.isEmpty()) {
      builder.append('\n');
      for (TypeElement typeElement : typeElements) {
        builder.append(typeElement);
      }
    }
    if (!extendDeclarations.isEmpty()) {
      builder.append('\n');
      for (ExtendElement extendDeclaration : extendDeclarations) {
        builder.append(extendDeclaration);
      }
    }
    if (!services.isEmpty()) {
      builder.append('\n');
      for (ServiceElement service : services) {
        builder.append(service);
      }
    }
    return builder.toString();
  }
}
