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

import com.squareup.wire.WireType;
import com.squareup.wire.internal.protoparser.FieldElement;

public final class Field {
  private final String packageName;
  private final FieldElement element;
  private final Options options;
  private WireType type;

  Field(String packageName, FieldElement element) {
    this.packageName = packageName;
    this.element = element;
    this.options = new Options(Options.FIELD_OPTIONS, element.options());
  }

  public Location location() {
    return element.location();
  }

  public String packageName() {
    return packageName;
  }

  public Label label() {
    return element.label();
  }

  public boolean isRepeated() {
    return label() == Label.REPEATED;
  }

  public boolean isOptional() {
    return label() == Label.OPTIONAL;
  }

  public boolean isRequired() {
    return label() == Label.REQUIRED;
  }

  public WireType type() {
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

  public Options options() {
    return options;
  }

  public boolean isDeprecated() {
    return "true".equals(options().get("deprecated"));
  }

  public boolean isPacked() {
    return "true".equals(options().get("packed"));
  }

  public Object getDefault() {
    return options.get("default");
  }

  void link(Linker linker) {
    linker = linker.withContext(this);
    type = linker.resolveType(element.type());
  }

  void linkOptions(Linker linker) {
    linker = linker.withContext(this);
    options.link(linker);
  }

  @Override public String toString() {
    return name();
  }

  void validate(Linker linker, boolean extension) {
    linker = linker.withContext(this);
    if (isPacked() && !isPackable(linker, type)) {
      linker.addError("packed=true not permitted on %s", type);
    }
    if (extension && isRequired()) {
      linker.addError("extension fields cannot be required", type);
    }
    linker.validateImport(location(), type);
  }

  private boolean isPackable(Linker linker, WireType type) {
    return !type.equals(WireType.STRING)
        && !type.equals(WireType.BYTES)
        && !(linker.get(type) instanceof MessageType);
  }

  public enum Label {
    OPTIONAL, REQUIRED, REPEATED,
    /** Indicates the field is a member of a {@code oneof} block. */
    ONE_OF
  }
}
