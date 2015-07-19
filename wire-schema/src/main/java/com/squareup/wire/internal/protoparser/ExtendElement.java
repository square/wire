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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.squareup.wire.internal.Util.appendDocumentation;
import static com.squareup.wire.internal.Util.appendIndented;

@AutoValue
public abstract class ExtendElement {
  public static Builder builder(Location location) {
    return new Builder(location);
  }

  ExtendElement() {
  }

  public abstract Location location();
  public abstract String name();
  public abstract String qualifiedName();
  public abstract String documentation();
  public abstract List<FieldElement> fields();

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

  public static final class Builder {
    private final Location location;
    private String name;
    private String qualifiedName;
    private String documentation = "";
    private final List<FieldElement> fields = new ArrayList<>();

    private Builder(Location location) {
      this.location = checkNotNull(location, "location");
    }

    public Builder name(String name) {
      this.name = checkNotNull(name, "name");
      if (qualifiedName == null) {
        qualifiedName = name;
      }
      return this;
    }

    public Builder qualifiedName(String qualifiedName) {
      this.qualifiedName = checkNotNull(qualifiedName, "qualifiedName");
      return this;
    }

    public Builder documentation(String documentation) {
      this.documentation = checkNotNull(documentation, "documentation");
      return this;
    }

    public Builder addField(FieldElement field) {
      fields.add(checkNotNull(field, "field"));
      return this;
    }

    public Builder addFields(Collection<FieldElement> fields) {
      for (FieldElement field : checkNotNull(fields, "fields")) {
        addField(field);
      }
      return this;
    }

    public ExtendElement build() {
      checkNotNull(name, "name");
      checkNotNull(qualifiedName, "qualifiedName");

      return new AutoValue_ExtendElement(location, name, qualifiedName, documentation,
          ImmutableList.copyOf(fields));
    }
  }
}
