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
package com.squareup.wire.schema;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.squareup.wire.schema.internal.parser.ReservedElement;

final class Reserved {
  private final Location location;
  private final String documentation;
  private final ImmutableList<Object> values;

  private Reserved(Location location, String documentation, ImmutableList<Object> values) {
    this.location = location;
    this.documentation = documentation;
    this.values = values;
  }

  public Location location() {
    return location;
  }

  public String documentation() {
    return documentation;
  }

  public ImmutableList<Object> values() {
    return values;
  }

  public boolean matchesTag(int tag) {
    for (Object value : values) {
      if (value instanceof Integer && tag == (Integer) value) {
        return true;
      }
      if (value instanceof Range && ((Range<Integer>) value).contains(tag)) {
        return true;
      }
    }
    return false;
  }

  public boolean matchesName(String name) {
    for (Object value : values) {
      if (value instanceof String && name.equals(value)) {
        return true;
      }
    }
    return false;
  }

  static ImmutableList<Reserved> fromElements(ImmutableList<ReservedElement> reserveds) {
    ImmutableList.Builder<Reserved> builder = ImmutableList.builder();
    for (ReservedElement reserved : reserveds) {
      builder.add(new Reserved(reserved.location(), reserved.documentation(), reserved.values()));
    }
    return builder.build();
  }

  static ImmutableList<ReservedElement> toElements(ImmutableList<Reserved> reserveds) {
    ImmutableList.Builder<ReservedElement> builder = ImmutableList.builder();
    for (Reserved reserved : reserveds) {
      builder.add(
          ReservedElement.create(reserved.location(), reserved.documentation(), reserved.values()));
    }
    return builder.build();
  }
}
