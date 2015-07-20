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
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.Location;
import java.util.Locale;

import static com.squareup.wire.internal.Util.appendDocumentation;
import static com.squareup.wire.internal.Util.appendIndented;

@AutoValue
public abstract class FieldElement {
  public static Builder builder(Location location) {
    return new AutoValue_FieldElement.Builder()
        .documentation("")
        .options(ImmutableList.<OptionElement>of())
        .location(location);
  }

  public abstract Location location();
  public abstract Field.Label label();
  /**
   * Returns the type of this field. May be a message type name, an enum type
   * name, or a <a href="https://developers.google.com/protocol-buffers/docs/proto#scalar">
   * scalar value type</a> like {@code int64} or {@code bytes}.
   */
  public abstract String type();
  public abstract String name();
  public abstract int tag();
  public abstract String documentation();
  public abstract ImmutableList<OptionElement> options();

  public final String toSchema() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    if (label() != Field.Label.ONE_OF) {
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

  @AutoValue.Builder
  public abstract static class Builder {
    abstract Builder location(Location location);
    abstract Builder label(Field.Label label);
    abstract Builder type(String type);
    abstract Builder name(String name);
    abstract Builder tag(int tag);
    abstract Builder documentation(String documentation);
    abstract Builder options(ImmutableList<OptionElement> options);
    abstract FieldElement build();
  }
}
