/*
 * Copyright (C) 2014 Square, Inc.
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
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.Location;

@AutoValue
public abstract class FieldElement {
  static Builder builder(Location location) {
    return new AutoValue_FieldElement.Builder()
        .documentation("")
        .options(ImmutableList.<OptionElement>of())
        .location(location);
  }

  public abstract Location location();
  @Nullable public abstract Field.Label label();
  public abstract String type();
  public abstract String name();
  @Nullable public abstract String defaultValue();
  public abstract int tag();
  public abstract String documentation();
  public abstract ImmutableList<OptionElement> options();

  @AutoValue.Builder
  interface Builder {
    Builder location(Location location);
    Builder label(@Nullable Field.Label label);
    Builder type(String type);
    Builder name(String name);
    Builder defaultValue(@Nullable String value);
    Builder tag(int tag);
    Builder documentation(String documentation);
    Builder options(ImmutableList<OptionElement> options);
    FieldElement build();
  }
}
