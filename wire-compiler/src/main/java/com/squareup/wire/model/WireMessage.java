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

import com.squareup.protoparser.ExtensionsElement;
import com.squareup.protoparser.FieldElement;
import com.squareup.protoparser.MessageElement;
import com.squareup.protoparser.OneOfElement;
import com.squareup.protoparser.OptionElement;
import com.squareup.protoparser.TypeElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WireMessage extends WireType {
  private final ProtoTypeName protoTypeName;
  private final MessageElement element;
  private final List<WireField> fields;
  private final List<WireOneOf> oneOfs;
  private final List<WireType> nestedTypes;
  private final List<WireOption> options;

  WireMessage(ProtoTypeName protoTypeName, MessageElement element) {
    this.protoTypeName = protoTypeName;
    this.element = element;

    List<WireField> fields = new ArrayList<WireField>();
    for (FieldElement field : element.fields()) {
      fields.add(new WireField(field));
    }
    this.fields = Collections.unmodifiableList(fields);

    List<WireOneOf> oneOfs = new ArrayList<WireOneOf>();
    for (OneOfElement oneOf : element.oneOfs()) {
      oneOfs.add(new WireOneOf(oneOf));
    }
    this.oneOfs = Collections.unmodifiableList(oneOfs);

    List<WireType> nestedTypes = new ArrayList<WireType>();
    for (TypeElement type : element.nestedElements()) {
      nestedTypes.add(WireType.get(protoTypeName.nestedType(type.name()), type));
    }
    this.nestedTypes = Collections.unmodifiableList(nestedTypes);

    List<WireOption> options = new ArrayList<WireOption>();
    for (OptionElement option : element.options()) {
      options.add(new WireOption(option));
    }
    this.options = Collections.unmodifiableList(options);
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

  @Override public List<WireOption> options() {
    return options;
  }

  public List<WireField> fields() {
    return fields;
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
}
