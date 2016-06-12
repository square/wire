/*
 * Copyright (C) 2016 Square, Inc.
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

@AutoValue
abstract class GroupElement {
  static Builder builder() {
    return new AutoValue_GroupElement.Builder()
        .documentation("")
        .fields(ImmutableList.<FieldElement>of());
  }

  @Nullable public abstract Field.Label label();
  public abstract String name();
  public abstract int tag();
  public abstract String documentation();
  public abstract ImmutableList<FieldElement> fields();

  @AutoValue.Builder
  interface Builder {
    Builder label(Field.Label label);
    Builder name(String name);
    Builder tag(int value);
    Builder documentation(String documentation);
    Builder fields(ImmutableList<FieldElement> fields);
    GroupElement build();
  }
}
