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
import com.squareup.protoparser.EnumElement;
import com.squareup.protoparser.OptionElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WireEnum extends WireType {
  private final ProtoTypeName protoTypeName;
  private final EnumElement element;
  private final List<WireEnumConstant> constants;
  private final List<WireOption> options;

  WireEnum(ProtoTypeName protoTypeName, EnumElement element) {
    this.protoTypeName = protoTypeName;
    this.element = element;

    List<WireEnumConstant> constants = new ArrayList<WireEnumConstant>();
    for (EnumConstantElement constant : this.element.constants()) {
      constants.add(new WireEnumConstant(constant));
    }
    this.constants = Collections.unmodifiableList(constants);

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

  @Override public List<WireOption> options() {
    return options;
  }

  @Override public List<WireType> nestedTypes() {
    return Collections.emptyList(); // Enums do not allow nested type declarations.
  }

  public List<WireEnumConstant> constants() {
    return constants;
  }

  @Override void link(Linker linker) {
    for (WireEnumConstant constant : constants) {
      constant.link(linker);
    }
    for (WireOption option : options) {
      option.link(ProtoTypeName.ENUM_OPTIONS, linker);
    }
  }
}
