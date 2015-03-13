// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import static com.squareup.protoparser.Utils.checkNotNull;
import static com.squareup.protoparser.Utils.immutableCopyOf;

/** A single {@code .proto} file. */
@AutoValue
public abstract class ProtoFile {
  static final int MIN_TAG_VALUE = 1;
  static final int MAX_TAG_VALUE = (1 << 29) - 1; // 536,870,911
  private static final int RESERVED_TAG_VALUE_START = 19000;
  private static final int RESERVED_TAG_VALUE_END = 19999;

  /** Syntax version. */
  public enum Syntax {
    PROTO_2("proto2"),
    PROTO_3("proto3");

    private final String name;

    Syntax(String name) {
      this.name = name;
    }
  }

  /** True if the supplied value is in the valid tag range and not reserved. */
  static boolean isValidTag(int value) {
    return (value >= MIN_TAG_VALUE && value < RESERVED_TAG_VALUE_START)
        || (value > RESERVED_TAG_VALUE_END && value <= MAX_TAG_VALUE);
  }

  public static Builder builder(String filePath) {
    return new Builder(checkNotNull(filePath, "filePath"));
  }

  ProtoFile() {
  }

  public abstract String filePath();
  @Nullable public abstract String packageName();
  @Nullable public abstract Syntax syntax();
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
    if (syntax() != null) {
      builder.append("syntax \"").append(syntax().name).append("\";\n");
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

  public static final class Builder {
    private final String filePath;
    private String packageName;
    private Syntax syntax;
    private final List<String> dependencies = new ArrayList<>();
    private final List<String> publicDependencies = new ArrayList<>();
    private final List<TypeElement> types = new ArrayList<>();
    private final List<ServiceElement> services = new ArrayList<>();
    private final List<ExtendElement> extendDeclarations = new ArrayList<>();
    private final List<OptionElement> options = new ArrayList<>();

    Builder(String filePath) {
      this.filePath = filePath;
    }

    public Builder packageName(String packageName) {
      this.packageName = checkNotNull(packageName, "packageName");
      return this;
    }

    public Builder syntax(Syntax syntax) {
      this.syntax = checkNotNull(syntax, "syntax");
      return this;
    }

    public Builder addDependency(String dependency) {
      dependencies.add(checkNotNull(dependency, "dependency"));
      return this;
    }

    public Builder addPublicDependency(String dependency) {
      publicDependencies.add(checkNotNull(dependency, "dependency"));
      return this;
    }

    public Builder addType(TypeElement type) {
      types.add(checkNotNull(type, "type"));
      return this;
    }

    public Builder addService(ServiceElement service) {
      services.add(checkNotNull(service, "service"));
      return this;
    }

    public Builder addExtendDeclaration(ExtendElement extend) {
      extendDeclarations.add(checkNotNull(extend, "extend"));
      return this;
    }

    public Builder addOption(OptionElement option) {
      options.add(checkNotNull(option, "option"));
      return this;
    }

    public ProtoFile build() {
      return new AutoValue_ProtoFile(filePath, packageName, syntax, immutableCopyOf(dependencies),
          immutableCopyOf(publicDependencies), immutableCopyOf(types), immutableCopyOf(services),
          immutableCopyOf(extendDeclarations), immutableCopyOf(options));
    }
  }
}
