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
import com.squareup.wire.schema.internal.parser.ExtendElement;
import java.util.List;

public final class Extend {
  private final Location location;
  private final String documentation;
  private final String name;
  private final List<Field> fields;
  private ProtoType protoType;

  private Extend(Location location, String documentation, String name,
      List<Field> fields) {
    this.location = location;
    this.documentation = documentation;
    this.name = name;
    this.fields = fields;
  }

  static ImmutableList<Extend> fromElements(String packageName,
      List<ExtendElement> extendElements) {
    ImmutableList.Builder<Extend> extendBuilder = new ImmutableList.Builder<>();
    for (ExtendElement extendElement : extendElements) {
      extendBuilder.add(new Extend(extendElement.getLocation(), extendElement.getDocumentation(),
          extendElement.getName(),
          Field.fromElements(packageName, extendElement.getFields(), true)));
    }
    return extendBuilder.build();
  }

  static ImmutableList<ExtendElement> toElements(List<Extend> extendList) {
    ImmutableList.Builder<ExtendElement> elements = new ImmutableList.Builder<>();
    for (Extend extend : extendList) {
      elements.add(new ExtendElement(
          extend.location,
          extend.name,
          extend.documentation,
          Field.toElements(extend.fields)));
    }
    return elements.build();
  }

  public Location location() {
    return location;
  }

  public ProtoType type() {
    return protoType;
  }

  public String documentation() {
    return documentation;
  }

  public List<Field> fields() {
    return fields;
  }

  void link(Linker linker) {
    linker = linker.withContext(this);
    protoType = linker.resolveMessageType(name);
    Type type = linker.get(protoType);
    if (type != null) {
      ((MessageType) type).addExtensionFields(fields);
    }
  }

  void validate(Linker linker) {
    linker = linker.withContext(this);
    linker.validateImport(location(), type());
  }

  public Extend retainAll(Schema schema, MarkSet markSet) {
    ImmutableList<Field> retainedFields = Field.retainAll(schema, markSet, protoType, fields);
    if (retainedFields.isEmpty()) return null;
    else return new Extend(location, documentation, name, retainedFields);
  }
}
