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

import static com.google.common.base.Preconditions.checkArgument;
import static com.squareup.wire.internal.protoparser.ProtoFileElement.isValidTag;
import static com.squareup.wire.internal.protoparser.Utils.appendDocumentation;

@AutoValue
public abstract class ExtensionsElement {
  public static ExtensionsElement create(int start, int end) {
    return create(start, end, "");
  }

  public static ExtensionsElement create(int start, int end, String documentation) {
    checkArgument(isValidTag(start), "Invalid start value: %s", start);
    checkArgument(isValidTag(end), "Invalid end value: %s", end);

    return new AutoValue_ExtensionsElement(documentation, start, end);
  }

  ExtensionsElement() {
  }

  public abstract String documentation();
  public abstract int start();
  public abstract int end();

  public final String toSchema() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    builder.append("extensions ")
        .append(start());
    if (start() != end()) {
      builder.append(" to ");
      if (end() < ProtoFileElement.MAX_TAG_VALUE) {
        builder.append(end());
      } else {
        builder.append("max");
      }
    }
    return builder.append(";\n").toString();
  }
}
