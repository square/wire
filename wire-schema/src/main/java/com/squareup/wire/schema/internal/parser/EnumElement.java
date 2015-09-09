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

import static com.squareup.wire.schema.internal.Util.appendDocumentation;
import static com.squareup.wire.schema.internal.Util.appendIndented;

@AutoValue
public abstract class EnumElement implements TypeElement {
  public static Builder builder(Location location) {
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

  @Override public final String toSchema() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    builder.append("enum ")
        .append(name())
        .append(" {");
    if (!options().isEmpty()) {
      builder.append('\n');
      for (OptionElement option : options()) {
        appendIndented(builder, option.toSchemaDeclaration());
      }
    }
    if (!constants().isEmpty()) {
      builder.append('\n');
      for (EnumConstantElement constant : constants()) {
        appendIndented(builder, constant.toSchema());
      }
    }
    return builder.append("}\n").toString();
  }

  @AutoValue.Builder
  public interface  Builder {
    Builder location(Location location);
    Builder name(String name);
    Builder documentation(String documentation);
    Builder constants(ImmutableList<EnumConstantElement> constants);
    Builder options(ImmutableList<OptionElement> options);
    EnumElement build();
  }
}
