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

import com.squareup.protoparser.FieldElement;
import com.squareup.protoparser.OptionElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WireField {
  private final String packageName;
  private final FieldElement element;
  private final List<WireOption> options;
  private ProtoTypeName type;

  WireField(String packageName, FieldElement element) {
    this.packageName = packageName;
    this.element = element;

    List<WireOption> options = new ArrayList<WireOption>();
    for (OptionElement option : element.options()) {
      options.add(new WireOption(packageName, option));
    }
    this.options = Collections.unmodifiableList(options);
  }

  public String packageName() {
    return packageName;
  }

  public FieldElement.Label label() {
    return element.label();
  }

  public ProtoTypeName type() {
    return type;
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

  public List<WireOption> options() {
    return options;
  }

  public boolean isDeprecated() {
    return element.isDeprecated();
  }

  public boolean isPacked() {
    return element.isPacked();
  }

  public WireOption getDefault() {
    return Util.findOption(options, "default");
  }

  void link(Linker linker) {
    type = linker.resolveType(packageName, element.type());
    for (WireOption option : options) {
      option.link(ProtoTypeName.FIELD_OPTIONS, linker);
    }
  }
}
