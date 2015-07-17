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
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.squareup.wire.internal.protoparser.Utils.appendDocumentation;
import static com.squareup.wire.internal.protoparser.Utils.appendIndented;

@AutoValue
public abstract class RpcElement {
  public static Builder builder(Location location) {
    return new Builder(location);
  }

  RpcElement() {
  }

  public abstract Location location();
  public abstract String name();
  public abstract String documentation();
  public abstract String requestType();
  public abstract String responseType();
  public abstract List<OptionElement> options();

  public final String toSchema() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    builder.append("rpc ")
        .append(name())
        .append(" (")
        .append(requestType())
        .append(") returns (")
        .append(responseType())
        .append(')');
    if (!options().isEmpty()) {
      builder.append(" {\n");
      for (OptionElement option : options()) {
        appendIndented(builder, option.toSchemaDeclaration());
      }
      builder.append("}");
    }
    return builder.append(";\n").toString();
  }

  public static final class Builder {
    private final Location location;
    private String name;
    private String documentation = "";
    private String requestType;
    private String responseType;
    private final List<OptionElement> options = new ArrayList<>();

    private Builder(Location location) {
      this.location = checkNotNull(location, "location");
    }

    public Builder name(String name) {
      this.name = checkNotNull(name, "name");
      return this;
    }

    public Builder documentation(String documentation) {
      this.documentation = checkNotNull(documentation, "documentation");
      return this;
    }

    public Builder requestType(String requestType) {
      this.requestType = checkNotNull(requestType, "requestType");
      return this;
    }

    public Builder responseType(String responseType) {
      this.responseType = checkNotNull(responseType, "responseType");
      return this;
    }

    public Builder addOption(OptionElement option) {
      options.add(checkNotNull(option, "option"));
      return this;
    }

    public Builder addOptions(Collection<OptionElement> options) {
      for (OptionElement option : checkNotNull(options, "options")) {
        addOption(option);
      }
      return this;
    }

    public RpcElement build() {
      checkNotNull(name, "name");
      checkNotNull(requestType, "requestType");
      checkNotNull(responseType, "responseType");

      return new AutoValue_RpcElement(location, name, documentation, requestType, responseType,
          ImmutableList.copyOf(options));
    }
  }
}
