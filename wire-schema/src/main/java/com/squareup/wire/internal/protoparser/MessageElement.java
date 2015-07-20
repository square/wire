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

import static com.squareup.wire.internal.Util.appendDocumentation;
import static com.squareup.wire.internal.Util.appendIndented;

@AutoValue
public abstract class MessageElement implements TypeElement {
  public static Builder builder(Location location) {
    return new AutoValue_MessageElement.Builder()
        .location(location)
        .documentation("")
        .fields(ImmutableList.<FieldElement>of())
        .oneOfs(ImmutableList.<OneOfElement>of())
        .nestedTypes(ImmutableList.<TypeElement>of())
        .extensions(ImmutableList.<ExtensionsElement>of())
        .options(ImmutableList.<OptionElement>of());
  }

  @Override public abstract Location location();
  @Override public abstract String name();
  @Override public abstract String documentation();
  public abstract ImmutableList<FieldElement> fields();
  public abstract ImmutableList<OneOfElement> oneOfs();
  @Override public abstract ImmutableList<TypeElement> nestedTypes();
  public abstract ImmutableList<ExtensionsElement> extensions();
  @Override public abstract ImmutableList<OptionElement> options();

  @Override public final String toSchema() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    builder.append("message ")
        .append(name())
        .append(" {");
    if (!options().isEmpty()) {
      builder.append('\n');
      for (OptionElement option : options()) {
        appendIndented(builder, option.toSchemaDeclaration());
      }
    }
    if (!fields().isEmpty()) {
      builder.append('\n');
      for (FieldElement field : fields()) {
        appendIndented(builder, field.toSchema());
      }
    }
    if (!oneOfs().isEmpty()) {
      builder.append('\n');
      for (OneOfElement oneOf : oneOfs()) {
        appendIndented(builder, oneOf.toSchema());
      }
    }
    if (!extensions().isEmpty()) {
      builder.append('\n');
      for (ExtensionsElement extension : extensions()) {
        appendIndented(builder, extension.toSchema());
      }
    }
    if (!nestedTypes().isEmpty()) {
      builder.append('\n');
      for (TypeElement type : nestedTypes()) {
        appendIndented(builder, type.toSchema());
      }
    }
    return builder.append("}\n").toString();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder location(Location location);
    public abstract Builder name(String name);
    public abstract Builder documentation(String documentation);
    public abstract Builder fields(ImmutableList<FieldElement> fields);
    public abstract Builder oneOfs(ImmutableList<OneOfElement> oneOfs);
    public abstract Builder nestedTypes(ImmutableList<TypeElement> types);
    public abstract Builder extensions(ImmutableList<ExtensionsElement> extensions);
    public abstract Builder options(ImmutableList<OptionElement> options);
    public abstract MessageElement build();
  }
}
