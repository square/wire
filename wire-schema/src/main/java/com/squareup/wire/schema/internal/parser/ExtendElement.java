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
public abstract class ExtendElement {
  public static Builder builder(Location location) {
    return new AutoValue_ExtendElement.Builder()
        .documentation("")
        .fields(ImmutableList.<FieldElement>of())
        .location(location);
  }

  public abstract Location location();
  public abstract String name();
  public abstract String documentation();
  public abstract ImmutableList<FieldElement> fields();

  public final String toSchema() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    builder.append("extend ")
        .append(name())
        .append(" {");
    if (!fields().isEmpty()) {
      builder.append('\n');
      for (FieldElement field : fields()) {
        appendIndented(builder, field.toSchema());
      }
    }
    return builder.append("}\n").toString();
  }

  @AutoValue.Builder
  public interface Builder {
    Builder location(Location location);
    Builder name(String name);
    Builder documentation(String documentation);
    Builder fields(ImmutableList<FieldElement> fields);
    ExtendElement build();
  }
}
