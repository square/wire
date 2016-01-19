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
import com.squareup.wire.schema.internal.parser.EnumConstantElement;

public final class EnumConstant {
  private final Location location;
  private final String name;
  private final int tag;
  private final String documentation;
  private final Options options;

  private EnumConstant(Location location, String name, int tag, String documentation,
      Options options) {
    this.location = location;
    this.name = name;
    this.tag = tag;
    this.documentation = documentation;
    this.options = options;
  }

  public Location location() {
    return location;
  }

  public String name() {
    return name;
  }

  public int tag() {
    return tag;
  }

  public String documentation() {
    return documentation;
  }

  public Options options() {
    return options;
  }

  static EnumConstant fromElement(EnumConstantElement element) {
    return new EnumConstant(element.location(), element.name(), element.tag(),
        element.documentation(), new Options(Options.ENUM_VALUE_OPTIONS, element.options()));
  }

  EnumConstantElement toElement() {
    return EnumConstantElement.builder(location)
        .documentation(documentation)
        .name(name)
        .tag(tag)
        .options(options.toElements())
        .build();
  }

  void linkOptions(Linker linker) {
    options.link(linker);
  }

  EnumConstant retainAll(Schema schema, MarkSet markSet) {
    return new EnumConstant(location, name, tag, documentation, options.retainAll(schema, markSet));
  }

  static ImmutableList<EnumConstant> fromElements(ImmutableList<EnumConstantElement> elements) {
    ImmutableList.Builder<EnumConstant> constants = ImmutableList.builder();
    for (EnumConstantElement element : elements) {
      constants.add(fromElement(element));
    }
    return constants.build();
  }

  static ImmutableList<EnumConstantElement> toElements(ImmutableList<EnumConstant> constants) {
    ImmutableList.Builder<EnumConstantElement> elements = new ImmutableList.Builder<>();
    for (EnumConstant constant : constants) {
      elements.add(constant.toElement());
    }
    return elements.build();
  }
}
