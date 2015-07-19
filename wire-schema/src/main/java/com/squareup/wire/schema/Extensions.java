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

import com.squareup.wire.internal.Util;
import com.squareup.wire.internal.protoparser.ExtensionsElement;

public final class Extensions {
  private final ExtensionsElement element;

  Extensions(ExtensionsElement element) {
    this.element = element;
  }

  public Location location() {
    return element.location();
  }

  public String documentation() {
    return element.documentation();
  }

  public int start() {
    return element.start();
  }

  public int end() {
    return element.end();
  }

  void validate(Linker linker) {
    if (!Util.isValidTag(start()) || !Util.isValidTag(end())) {
      linker.withContext(this).addError("tags are out of range: %s to %s", start(), end());
    }
  }
}
