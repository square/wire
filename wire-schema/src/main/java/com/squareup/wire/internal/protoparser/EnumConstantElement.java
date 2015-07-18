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
package com.squareup.wire.internal.protoparser;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.squareup.wire.schema.Location;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.squareup.wire.internal.protoparser.OptionElement.formatOptionList;
import static com.squareup.wire.internal.protoparser.Utils.appendDocumentation;

/** An enum constant. */
@AutoValue
public abstract class EnumConstantElement {
  public static Builder builder(Location location) {
    return new Builder(location);
  }

  EnumConstantElement() {
  }

  public abstract Location location();
  public abstract String name();
  public abstract int tag();
  public abstract String documentation();
  public abstract List<OptionElement> options();

  public final String toSchema() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    builder.append(name())
        .append(" = ")
        .append(tag());
    if (!options().isEmpty()) {
      builder.append(" [\n");
      formatOptionList(builder, options());
      builder.append(']');
    }
    return builder.append(";\n").toString();
  }

  public static final class Builder {
    private final Location location;
    private String name;
    private Integer tag;
    private String documentation = "";
    private final List<OptionElement> options = new ArrayList<>();

    private Builder(Location location) {
      this.location = checkNotNull(location, "location");
    }

    public Builder name(String name) {
      this.name = checkNotNull(name, "name");
      return this;
    }

    public Builder tag(int tag) {
      this.tag = tag;
      return this;
    }

    public Builder documentation(String documentation) {
      this.documentation = checkNotNull(documentation, "documentation");
      return this;
    }

    public Builder addOption(OptionElement option) {
      options.add(checkNotNull(option, "option"));
      return this;
    }

    public EnumConstantElement build() {
      checkNotNull(name, "name");
      checkNotNull(tag, "tag");

      return new AutoValue_EnumConstantElement(location, name, tag, documentation,
          ImmutableList.copyOf(options));
    }
  }
}
