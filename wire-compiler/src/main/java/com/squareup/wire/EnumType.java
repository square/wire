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
package com.squareup.wire;

import com.google.common.collect.ImmutableList;
import com.squareup.protoparser.EnumElement;
import java.util.Set;

public final class EnumType extends Type {
  private final Name name;
  private final EnumElement element;
  private final ImmutableList<EnumConstant> constants;
  private final Options options;

  EnumType(Name name, EnumElement element,
      ImmutableList<EnumConstant> constants, Options options) {
    this.name = name;
    this.element = element;
    this.constants = constants;
    this.options = options;
  }

  @Override public Name name() {
    return name;
  }

  @Override public String documentation() {
    return element.documentation();
  }

  @Override public Options options() {
    return options;
  }

  @Override public ImmutableList<Type> nestedTypes() {
    return ImmutableList.of(); // Enums do not allow nested type declarations.
  }

  public ImmutableList<EnumConstant> constants() {
    return constants;
  }

  @Override void link(Linker linker) {
  }

  @Override void linkOptions(Linker linker) {
    options.link(linker);
    for (EnumConstant constant : constants) {
      constant.linkOptions(linker);
    }
  }

  @Override Type retainAll(Set<String> identifiers) {
    return identifiers.contains(name.toString()) ? this : null;
  }
}
