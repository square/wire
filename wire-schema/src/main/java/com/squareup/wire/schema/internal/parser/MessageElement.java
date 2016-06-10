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

@AutoValue
public abstract class MessageElement implements TypeElement {
  static Builder builder(Location location) {
    return new AutoValue_MessageElement.Builder()
        .location(location)
        .documentation("")
        .fields(ImmutableList.<FieldElement>of())
        .oneOfs(ImmutableList.<OneOfElement>of())
        .nestedTypes(ImmutableList.<TypeElement>of())
        .extensions(ImmutableList.<ExtensionsElement>of())
        .options(ImmutableList.<OptionElement>of())
        .reserveds(ImmutableList.<ReservedElement>of())
        .groups(ImmutableList.<GroupElement>of());
  }

  @Override public abstract Location location();
  @Override public abstract String name();
  @Override public abstract String documentation();
  @Override public abstract ImmutableList<TypeElement> nestedTypes();
  @Override public abstract ImmutableList<OptionElement> options();
  public abstract ImmutableList<ReservedElement> reserveds();
  public abstract ImmutableList<FieldElement> fields();
  public abstract ImmutableList<OneOfElement> oneOfs();
  public abstract ImmutableList<ExtensionsElement> extensions();
  public abstract ImmutableList<GroupElement> groups();

  @AutoValue.Builder
  interface Builder {
    Builder location(Location location);
    Builder name(String name);
    Builder documentation(String documentation);
    Builder fields(ImmutableList<FieldElement> fields);
    Builder oneOfs(ImmutableList<OneOfElement> oneOfs);
    Builder nestedTypes(ImmutableList<TypeElement> types);
    Builder extensions(ImmutableList<ExtensionsElement> extensions);
    Builder options(ImmutableList<OptionElement> options);
    Builder reserveds(ImmutableList<ReservedElement> reserveds);
    Builder groups(ImmutableList<GroupElement> groups);
    MessageElement build();
  }
}
