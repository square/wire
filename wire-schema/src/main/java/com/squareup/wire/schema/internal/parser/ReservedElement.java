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
package com.squareup.wire.schema.internal.parser;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.squareup.wire.schema.Location;

@AutoValue
public abstract class ReservedElement {
  static ReservedElement create(Location location, String documentation,
      ImmutableList<Object> values) {
    return new AutoValue_ReservedElement(location, documentation, values);
  }

  public abstract Location location();
  public abstract String documentation();
  /** A {@link String} name or {@link Integer} or {@link Range Range&lt;Integer>} tag. */
  public abstract ImmutableList<Object> values();
}
