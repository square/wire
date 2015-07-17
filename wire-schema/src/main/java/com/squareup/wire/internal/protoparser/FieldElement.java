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
import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.squareup.wire.internal.protoparser.ProtoFileElement.isValidTag;
import static com.squareup.wire.internal.protoparser.Utils.appendDocumentation;
import static com.squareup.wire.internal.protoparser.Utils.appendIndented;

@AutoValue
public abstract class FieldElement {
  public static Builder builder(Location location) {
    return new Builder(location);
  }

  FieldElement() {
  }

  public abstract Location location();
  public abstract Label label();
  /**
   * Returns the type of this field. May be a message type name, an enum type
   * name, or a <a href="https://developers.google.com/protocol-buffers/docs/proto#scalar">
   * scalar value type</a> like {@code int64} or {@code bytes}.
   */
  public abstract String type();
  public abstract String name();
  public abstract int tag();
  public abstract String documentation();
  public abstract List<OptionElement> options();

  /** Returns true when the {@code deprecated} option is present and set to true. */
  public final boolean isDeprecated() {
    OptionElement deprecatedOption = OptionElement.findByName(options(), "deprecated");
    return deprecatedOption != null && "true".equals(deprecatedOption.value());
  }

  /** Returns true when the {@code packed} option is present and set to true. */
  public final boolean isPacked() {
    OptionElement packedOption = OptionElement.findByName(options(), "packed");
    return packedOption != null && "true".equals(packedOption.value());
  }

  /** Returns the {@code default} option value or {@code null}. */
  public final OptionElement getDefault() {
    OptionElement defaultOption = OptionElement.findByName(options(), "default");
    return defaultOption != null ? defaultOption : null;
  }

  public final String toSchema() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    if (label() != Label.ONE_OF) {
      builder.append(label().name().toLowerCase(Locale.US)).append(' ');
    }
    builder.append(type())
        .append(' ')
        .append(name())
        .append(" = ")
        .append(tag());
    if (!options().isEmpty()) {
      builder.append(" [\n");
      for (OptionElement option : options()) {
        appendIndented(builder, option.toSchema());
      }
      builder.append(']');
    }
    return builder.append(";\n").toString();
  }

  public enum Label {
    OPTIONAL, REQUIRED, REPEATED,
    /** Indicates the field is a member of a {@code oneof} block. */
    ONE_OF
  }

  public static final class Builder {
    private final Location location;
    private Label label;
    private String type;
    private String name;
    private Integer tag;
    private String documentation = "";
    private final List<OptionElement> options = new ArrayList<>();

    private Builder(Location location) {
      this.location = checkNotNull(location, "location");
    }

    public Builder label(Label label) {
      this.label = checkNotNull(label, "label");
      return this;
    }

    public Builder type(String type) {
      this.type = checkNotNull(type, "type");
      return this;
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

    public Builder addOptions(Collection<OptionElement> options) {
      for (OptionElement option : checkNotNull(options, "options")) {
        addOption(option);
      }
      return this;
    }

    public FieldElement build() {
      checkNotNull(label, "label");
      checkNotNull(type, "type");
      checkNotNull(name, "name");
      checkNotNull(tag, "tag");

      checkArgument(isValidTag(tag), "Illegal tag value: %s", tag);

      return new AutoValue_FieldElement(location, label, type, name, tag, documentation,
          ImmutableList.copyOf(options));
    }
  }
}
