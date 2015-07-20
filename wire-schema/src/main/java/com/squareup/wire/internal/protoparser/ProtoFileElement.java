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
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoFile;
import java.util.Collection;

/** A single {@code .proto} file. */
@AutoValue
public abstract class ProtoFileElement {
  public static Builder builder(Location location) {
    return new AutoValue_ProtoFileElement.Builder()
        .location(location)
        .dependencies(ImmutableList.<String>of())
        .publicDependencies(ImmutableList.<String>of())
        .types(ImmutableList.<TypeElement>of())
        .services(ImmutableList.<ServiceElement>of())
        .extendDeclarations(ImmutableList.<ExtendElement>of())
        .options(ImmutableList.<OptionElement>of());
  }

  public abstract Location location();
  @Nullable public abstract String packageName();
  @Nullable public abstract ProtoFile.Syntax syntax();
  public abstract ImmutableList<String> dependencies();
  public abstract ImmutableList<String> publicDependencies();
  public abstract ImmutableList<TypeElement> types();
  public abstract ImmutableList<ServiceElement> services();
  public abstract ImmutableList<ExtendElement> extendDeclarations();
  public abstract ImmutableList<OptionElement> options();

  public final String toSchema() {
    StringBuilder builder = new StringBuilder();
    builder.append("// ").append(location()).append('\n');
    if (packageName() != null) {
      builder.append("package ").append(packageName()).append(";\n");
    }
    if (syntax() != null) {
      builder.append("syntax \"").append(syntax()).append("\";\n");
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
    if (!types().isEmpty()) {
      builder.append('\n');
      for (TypeElement typeElement : types()) {
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

  @AutoValue.Builder
  public interface Builder {
    Builder location(Location location);
    Builder packageName(@Nullable String packageName);
    Builder syntax(@Nullable ProtoFile.Syntax syntax);
    Builder dependencies(ImmutableList<String> dependencies);
    Builder publicDependencies(ImmutableList<String> dependencies);
    Builder types(ImmutableList<TypeElement> types);
    Builder services(ImmutableList<ServiceElement> services);
    Builder extendDeclarations(ImmutableList<ExtendElement> extendDeclarations);
    Builder options(Collection<OptionElement> options);
    ProtoFileElement build();
  }
}
