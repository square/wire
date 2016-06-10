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
package com.squareup.wire.schema.internal.parser;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoFile;
import java.util.Collection;

/** A single {@code .proto} file. */
@AutoValue
public abstract class ProtoFileElement {
  static Builder builder(Location location) {
    return new AutoValue_ProtoFileElement.Builder()
        .location(location)
        .imports(ImmutableList.<String>of())
        .publicImports(ImmutableList.<String>of())
        .types(ImmutableList.<TypeElement>of())
        .services(ImmutableList.<ServiceElement>of())
        .extendDeclarations(ImmutableList.<ExtendElement>of())
        .options(ImmutableList.<OptionElement>of());
  }

  public abstract Location location();
  @Nullable public abstract String packageName();
  @Nullable public abstract ProtoFile.Syntax syntax();
  public abstract ImmutableList<String> imports();
  public abstract ImmutableList<String> publicImports();
  public abstract ImmutableList<TypeElement> types();
  public abstract ImmutableList<ServiceElement> services();
  public abstract ImmutableList<ExtendElement> extendDeclarations();
  public abstract ImmutableList<OptionElement> options();

  @AutoValue.Builder
  interface Builder {
    Builder location(Location location);
    Builder packageName(@Nullable String packageName);
    Builder syntax(@Nullable ProtoFile.Syntax syntax);
    Builder imports(ImmutableList<String> imports);
    Builder publicImports(ImmutableList<String> publicImports);
    Builder types(ImmutableList<TypeElement> types);
    Builder services(ImmutableList<ServiceElement> services);
    Builder extendDeclarations(ImmutableList<ExtendElement> extendDeclarations);
    Builder options(Collection<OptionElement> options);
    ProtoFileElement build();
  }
}
