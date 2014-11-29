// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

import static com.squareup.protoparser.Utils.immutableCopyOf;

/** A single {@code .proto} file. */
@AutoValue
public abstract class ProtoFile {
  static final int MIN_TAG_VALUE = 1;
  static final int MAX_TAG_VALUE = (1 << 29) - 1; // 536,870,911
  private static final int RESERVED_TAG_VALUE_START = 19000;
  private static final int RESERVED_TAG_VALUE_END = 19999;

  /** True if the supplied value is in the valid tag range and not reserved. */
  static boolean isValidTag(int value) {
    return (value >= MIN_TAG_VALUE && value < RESERVED_TAG_VALUE_START)
        || (value > RESERVED_TAG_VALUE_END && value <= MAX_TAG_VALUE);
  }

  public static ProtoFile create(String filePath, String packageName, List<String> dependencies,
      List<String> publicDependencies, List<TypeElement> typeElements,
      List<ServiceElement> services, List<ExtendElement> extendDeclarations,
      List<OptionElement> options) {
    return new AutoValue_ProtoFile(filePath, packageName,
        immutableCopyOf(dependencies, "dependencies"),
        immutableCopyOf(publicDependencies, "publicDependencies"),
        immutableCopyOf(typeElements, "typeElements"), immutableCopyOf(services, "services"),
        immutableCopyOf(extendDeclarations, "extendDeclarations"),
        immutableCopyOf(options, "options"));
  }

  ProtoFile() {
  }

  public abstract String filePath();
  @Nullable public abstract String packageName();
  public abstract List<String> dependencies();
  public abstract List<String> publicDependencies();
  public abstract List<TypeElement> typeElements();
  public abstract List<ServiceElement> services();
  public abstract List<ExtendElement> extendDeclarations();
  public abstract List<OptionElement> options();

  @Override public final String toString() {
    StringBuilder builder = new StringBuilder();
    if (!filePath().isEmpty()) {
      builder.append("// ").append(filePath()).append('\n');
    }
    if (packageName() != null) {
      builder.append("package ").append(packageName()).append(";\n");
    }
    if (!dependencies().isEmpty() || !publicDependencies().isEmpty()) {
      builder.append('\n');
      for (String dependency : dependencies()) {
        builder.append("import \"").append(dependency).append("\";\n");
      }
      for (String publicDependency : publicDependencies()) {
        builder.append("import public \"").append(publicDependency).append("\";\n");
      }
    }
    if (!options().isEmpty()) {
      builder.append('\n');
      for (OptionElement option : options()) {
        builder.append(option.toDeclaration());
      }
    }
    if (!typeElements().isEmpty()) {
      builder.append('\n');
      for (TypeElement typeElement : typeElements()) {
        builder.append(typeElement);
      }
    }
    if (!extendDeclarations().isEmpty()) {
      builder.append('\n');
      for (ExtendElement extendDeclaration : extendDeclarations()) {
        builder.append(extendDeclaration);
      }
    }
    if (!services().isEmpty()) {
      builder.append('\n');
      for (ServiceElement service : services()) {
        builder.append(service);
      }
    }
    return builder.toString();
  }
}
