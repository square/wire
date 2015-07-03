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

import com.squareup.protoparser.EnumConstantElement;

public final class WireEnumConstant {
  private final String packageName;
  private final EnumConstantElement element;
  private final Options options;

  WireEnumConstant(String packageName, EnumConstantElement element) {
    this.packageName = packageName;
    this.element = element;
    this.options = new Options(
        ProtoTypeName.ENUM_VALUE_OPTIONS, packageName, element.options());
  }

  public String packageName() {
    return packageName;
  }

  public String name() {
    return element.name();
  }

  public int tag() {
    return element.tag();
  }

  public String documentation() {
    return element.documentation();
  }

  public Options options() {
    return options;
  }

  void linkOptions(Linker linker) {
    options.link(linker);
  }
}
