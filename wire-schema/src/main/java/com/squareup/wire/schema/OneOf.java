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
import com.squareup.wire.schema.internal.parser.OneOfElement;

public final class OneOf {
  private final String name;
  private final String documentation;
  private final ImmutableList<Field> fields;

  private OneOf(String name, String documentation, ImmutableList<Field> fields) {
    this.name = name;
    this.documentation = documentation;
    this.fields = fields;
  }

  public String name() {
    return name;
  }

  public String documentation() {
    return documentation;
  }

  public ImmutableList<Field> fields() {
    return fields;
  }

  void link(Linker linker) {
    for (Field field : fields) {
      field.link(linker);
    }
  }

  void linkOptions(Linker linker) {
    for (Field field : fields) {
      field.linkOptions(linker);
    }
  }

  OneOf retainAll(Schema schema, MarkSet markSet, ProtoType enclosingType) {
    ImmutableList<Field> retainedFields = Field.retainAll(schema, markSet, enclosingType, fields);
    if (retainedFields.isEmpty()) return null;
    return new OneOf(name, documentation, retainedFields);
  }

  static ImmutableList<OneOf> fromElements(String packageName,
      ImmutableList<OneOfElement> elements, boolean extension) {
    ImmutableList.Builder<OneOf> oneOfs = ImmutableList.builder();
    for (OneOfElement oneOf : elements) {
      if (!oneOf.groups().isEmpty()) {
        throw new IllegalStateException("'group' is not supported");
      }
      oneOfs.add(new OneOf(oneOf.name(), oneOf.documentation(),
          Field.fromElements(packageName, oneOf.fields(), extension)));
    }
    return oneOfs.build();
  }

  static ImmutableList<OneOfElement> toElements(ImmutableList<OneOf> oneOfs) {
    ImmutableList.Builder<OneOfElement> elements = new ImmutableList.Builder<>();
    for (OneOf oneOf : oneOfs) {
      elements.add(OneOfElement.builder()
          .documentation(oneOf.documentation)
          .name(oneOf.name)
          .fields(Field.toElements(oneOf.fields))
          .build());
    }
    return elements.build();
  }
}
