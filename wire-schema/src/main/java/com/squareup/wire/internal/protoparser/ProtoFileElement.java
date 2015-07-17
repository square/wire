/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.internal.protoparser;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.squareup.wire.internal.protoparser.Utils.Nullable;
import com.squareup.wire.schema.Location;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/** A single {@code .proto} file. */
@AutoValue
public abstract class ProtoFileElement {
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

  public static Builder builder(String path) {
    return builder(Location.get(path));
  }

  public static Builder builder(Location location) {
    return new Builder(location);
  }

  ProtoFileElement() {
  }

  public abstract Location location();
  @Nullable public abstract String packageName();
  @Nullable public abstract Syntax syntax();
  public abstract List<String> dependencies();
  public abstract List<String> publicDependencies();
  public abstract List<TypeElement> typeElements();
  public abstract List<ServiceElement> services();
  public abstract List<ExtendElement> extendDeclarations();
  public abstract List<OptionElement> options();

  public final String toSchema() {
    StringBuilder builder = new StringBuilder();
    builder.append("// ").append(location()).append('\n');
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
        builder.append(option.toSchemaDeclaration());
      }
    }
    if (!typeElements().isEmpty()) {
      builder.append('\n');
      for (TypeElement typeElement : typeElements()) {
        builder.append(typeElement.toSchema());
      }
    }
    if (!extendDeclarations().isEmpty()) {
      builder.append('\n');
      for (ExtendElement extendDeclaration : extendDeclarations()) {
        builder.append(extendDeclaration.toSchema());
      }
    }
    if (!services().isEmpty()) {
      builder.append('\n');
      for (ServiceElement service : services()) {
        builder.append(service.toSchema());
      }
    }
    return builder.toString();
  }

  public static final class Builder {
    private final Location location;
    private String packageName;
    private Syntax syntax;
    private final List<String> dependencies = new ArrayList<>();
    private final List<String> publicDependencies = new ArrayList<>();
    private final List<TypeElement> types = new ArrayList<>();
    private final List<ServiceElement> services = new ArrayList<>();
    private final List<ExtendElement> extendDeclarations = new ArrayList<>();
    private final List<OptionElement> options = new ArrayList<>();

    Builder(Location location) {
      this.location = checkNotNull(location, "location");
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

    public Builder addDependencies(Collection<String> dependencies) {
      for (String dependency : checkNotNull(dependencies, "dependencies")) {
        addDependency(dependency);
      }
      return this;
    }

    public Builder addPublicDependency(String dependency) {
      publicDependencies.add(checkNotNull(dependency, "dependency"));
      return this;
    }

    public Builder addPublicDependencies(Collection<String> dependencies) {
      for (String dependency : checkNotNull(dependencies, "dependencies")) {
        addPublicDependency(dependency);
      }
      return this;
    }

    public Builder addType(TypeElement type) {
      types.add(checkNotNull(type, "type"));
      return this;
    }

    public Builder addTypes(Collection<TypeElement> types) {
      for (TypeElement type : checkNotNull(types, "types")) {
        addType(type);
      }
      return this;
    }

    public Builder addService(ServiceElement service) {
      services.add(checkNotNull(service, "service"));
      return this;
    }

    public Builder addServices(Collection<ServiceElement> services) {
      for (ServiceElement service : checkNotNull(services, "services")) {
        addService(service);
      }
      return this;
    }

    public Builder addExtendDeclaration(ExtendElement extend) {
      extendDeclarations.add(checkNotNull(extend, "extend"));
      return this;
    }

    public Builder addExtendDeclarations(Collection<ExtendElement> extendDeclarations) {
      for (ExtendElement extendDeclaration : checkNotNull(extendDeclarations,
          "extendDeclarations")) {
        addExtendDeclaration(extendDeclaration);
      }
      return this;
    }

    public Builder addOption(OptionElement option) {
      options.add(checkNotNull(option, "option"));
      return this;
    }

    public Builder addOptions(Collection<OptionElement> options) {
      for (OptionElement option : checkNotNull(options, "options")) {
        addOption(option);
      }
      return this;
    }

    public ProtoFileElement build() {
      return new AutoValue_ProtoFileElement(location, packageName, syntax,
          ImmutableList.copyOf(dependencies),
          ImmutableList.copyOf(publicDependencies), ImmutableList.copyOf(types),
          ImmutableList.copyOf(services),
          ImmutableList.copyOf(extendDeclarations), ImmutableList.copyOf(options));
    }
  }
}
