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

@AutoValue
public abstract class OptionElement {
  public enum Kind {
    STRING,
    BOOLEAN,
    NUMBER,
    ENUM,
    MAP,
    LIST,
    OPTION
  }

  static OptionElement create(String name, Kind kind, Object value) {
    return create(name, kind, value, false);
  }

  static OptionElement create(String name, Kind kind, Object value, boolean isParenthesized) {
    return new AutoValue_OptionElement(name, kind, value, isParenthesized);
  }

  public abstract String name();
  public abstract Kind kind();
  public abstract Object value();
  public abstract boolean isParenthesized();
}
