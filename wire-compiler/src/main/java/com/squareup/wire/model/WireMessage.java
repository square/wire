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

import com.google.common.collect.ImmutableList;
import com.squareup.protoparser.ExtensionsElement;
import com.squareup.protoparser.MessageElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class WireMessage extends WireType {
  private final ProtoTypeName protoTypeName;
  private final MessageElement element;
  private final List<WireField> fields;
  private final List<WireOneOf> oneOfs;
  private final List<WireType> nestedTypes;
  private final Options options;

  public WireMessage(ProtoTypeName protoTypeName, MessageElement element,
      List<WireField> fields, List<WireOneOf> oneOfs,
      List<WireType> nestedTypes, Options options) {
    this.protoTypeName = protoTypeName;
    this.element = element;
    this.fields = ImmutableList.copyOf(fields);
    this.oneOfs = ImmutableList.copyOf(oneOfs);
    this.nestedTypes = ImmutableList.copyOf(nestedTypes);
    this.options = options;
  }

  @Override public ProtoTypeName protoTypeName() {
    return protoTypeName;
  }

  @Override public String documentation() {
    return element.documentation();
  }

  @Override public List<WireType> nestedTypes() {
    return nestedTypes;
  }

  @Override public Options options() {
    return options;
  }

  public List<WireField> fields() {
    return fields;
  }

  public boolean hasRequiredFields() {
    for (WireField field : fieldsAndOneOfFields()) {
      if (field.isRequired()) return true;
    }
    return false;
  }

  public List<WireField> fieldsAndOneOfFields() {
    ImmutableList.Builder<WireField> result = ImmutableList.builder();
    result.addAll(fields);
    for (WireOneOf oneOf : oneOfs) {
      result.addAll(oneOf.fields());
    }
    return result.build();
  }

  /** Returns the field named {@code name}, or null if this type has no such field. */
  public WireField field(String name) {
    for (WireField field : fields) {
      if (field.name().equals(name)) {
        return field;
      }
    }
    return null;
  }

  public List<WireOneOf> oneOfs() {
    return oneOfs;
  }

  public List<ExtensionsElement> extensions() {
    return element.extensions();
  }

  void link(Linker linker) {
    linker = linker.withMessage(this);
    for (WireField field : fields) {
      field.link(linker);
    }
    for (WireOneOf oneOf : oneOfs) {
      oneOf.link(linker);
    }
    for (WireType type : nestedTypes) {
      type.link(linker);
    }
  }

  void linkOptions(Linker linker) {
    linker = linker.withMessage(this);
    for (WireType type : nestedTypes) {
      type.linkOptions(linker);
    }
    for (WireField field : fields) {
      field.linkOptions(linker);
    }
    for (WireOneOf oneOf : oneOfs) {
      oneOf.linkOptions(linker);
    }
    options.link(linker);
  }

  @Override WireType retainAll(Set<String> identifiers) {
    List<WireType> retainedNestedTypes = new ArrayList<WireType>();
    for (WireType nestedType : nestedTypes) {
      WireType retainedNestedType = nestedType.retainAll(identifiers);
      if (retainedNestedType != null) {
        retainedNestedTypes.add(retainedNestedType);
      }
    }

    // If this type is retained, or any of its nested types are retained, keep it.
    if (identifiers.contains(protoTypeName.toString()) || !retainedNestedTypes.isEmpty()) {
      return new WireMessage(protoTypeName, element, fields, oneOfs, retainedNestedTypes, options);
    }

    // This type isn't needed.
    return null;
  }
}
