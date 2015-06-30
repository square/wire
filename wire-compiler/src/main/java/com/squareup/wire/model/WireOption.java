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
package com.squareup.wire.model;

import com.squareup.protoparser.OptionElement;
import java.util.List;

public final class WireOption {
  private final String packageName;
  private final OptionElement element;
  private List<WireField> fieldPath;

  public WireOption(String packageName, OptionElement element) {
    this.packageName = packageName;
    this.element = element;
  }

  public String packageName() {
    return packageName;
  }

  public String name() {
    return element.name();
  }

  public List<WireField> fieldPath() {
    return fieldPath;
  }

  public OptionElement.Kind kind() {
    return element.kind();
  }

  public Object value() {
    return element.value();
  }

  public boolean isParenthesized() {
    return element.isParenthesized();
  }

  void link(ProtoTypeName optionType, Linker linker) {
    fieldPath = linker.fieldPath(packageName, optionType, element.name());
  }
}
