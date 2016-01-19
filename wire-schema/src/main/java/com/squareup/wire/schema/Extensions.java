/*
 * Copyright (C) 2015 Square, Inc.
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
import com.squareup.wire.schema.internal.Util;
import com.squareup.wire.schema.internal.parser.ExtensionsElement;

final class Extensions {
  private final Location location;
  private final String documentation;
  private final int start;
  private final int end;

  private Extensions(Location location, String documentation, int start, int end) {
    this.location = location;
    this.documentation = documentation;
    this.start = start;
    this.end = end;
  }

  public Location location() {
    return location;
  }

  public String documentation() {
    return documentation;
  }

  public int start() {
    return start;
  }

  public int end() {
    return end;
  }

  static ImmutableList<Extensions> fromElements(ImmutableList<ExtensionsElement> elements) {
    ImmutableList.Builder<Extensions> extensions = ImmutableList.builder();
    for (ExtensionsElement element : elements) {
      extensions.add(new Extensions(element.location(), element.documentation(),
          element.start(), element.end()));
    }
    return extensions.build();
  }

  static ImmutableList<ExtensionsElement> toElements(ImmutableList<Extensions> extensions) {
    ImmutableList.Builder<ExtensionsElement> elements = new ImmutableList.Builder<>();
    for (Extensions extension : extensions) {
      elements.add(ExtensionsElement.create(extension.location, extension.start, extension.end,
          extension.documentation));
    }
    return elements.build();
  }

  void validate(Linker linker) {
    if (!Util.isValidTag(start()) || !Util.isValidTag(end())) {
      linker.withContext(this).addError("tags are out of range: %s to %s", start(), end());
    }
  }
}
