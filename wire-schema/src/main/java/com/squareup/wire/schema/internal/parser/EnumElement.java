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
public abstract class EnumElement implements TypeElement {
  static Builder builder(Location location) {
    return new AutoValue_EnumElement.Builder()
        .location(location)
        .documentation("")
        .constants(ImmutableList.<EnumConstantElement>of())
        .options(ImmutableList.<OptionElement>of());
  }

  @Override public abstract Location location();
  @Override public abstract String name();
  @Override public abstract String documentation();
  @Override public abstract ImmutableList<OptionElement> options();
  @Override public final ImmutableList<TypeElement> nestedTypes() {
    return ImmutableList.of(); // Enums do not allow nested type declarations.
  }

  public abstract ImmutableList<EnumConstantElement> constants();

  @AutoValue.Builder
  interface  Builder {
    Builder location(Location location);
    Builder name(String name);
    Builder documentation(String documentation);
    Builder constants(ImmutableList<EnumConstantElement> constants);
    Builder options(ImmutableList<OptionElement> options);
    EnumElement build();
  }
}
