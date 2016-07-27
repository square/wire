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
package com.squareup.wire.java.internal;

import com.google.auto.value.AutoValue;
import com.squareup.wire.schema.Location;

import static com.squareup.wire.schema.internal.Util.appendDocumentation;

/**
 * Configures how Wire will generate code for a specific type. This configuration belongs in a
 * {@code build.wire} file that is in the same directory as the configured type.
 */
@AutoValue
public abstract class TypeConfigElement {
  public static Builder builder(Location location) {
    return new AutoValue_TypeConfigElement.Builder()
        .location(location)
        .documentation("");
  }

  public abstract Location location();
  public abstract String type();
  public abstract String documentation();
  public abstract String target();
  public abstract String adapter();

  public final String toSchema() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    builder.append("type ").append(type()).append(" {\n");
    builder.append("  target ").append(target()).append(" using ").append(adapter()).append("\n");
    builder.append("}\n");
    return builder.toString();
  }

  @AutoValue.Builder
  public interface Builder {
    Builder location(Location location);
    Builder type(String type);
    Builder documentation(String documentation);
    Builder target(String target);
    Builder adapter(String adapter);
    TypeConfigElement build();
  }
}
