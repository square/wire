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
import com.squareup.wire.schema.internal.parser.FieldElement;
import java.util.Collection;

public final class Field {
  private final String packageName;
  private final FieldElement element;
  private final boolean extension;
  private final Options options;
  private ProtoType type;

  Field(String packageName, FieldElement element, Options options, boolean extension) {
    this.packageName = packageName;
    this.element = element;
    this.extension = extension;
    this.options = options;
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

  public ProtoType type() {
    return type;
  }

  public String name() {
    return element.name();
  }

  /**
   * Returns this field's name, prefixed with its package name. Uniquely identifies extension
   * fields, such as in options.
   */
  public String qualifiedName() {
    return packageName != null
        ? packageName + '.' + element.name()
        : element.name();
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

  public String getDefault() {
    return element.defaultValue();
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

  void validate(Linker linker) {
    linker = linker.withContext(this);
    if (isPacked() && !isPackable(linker, type)) {
      linker.addError("packed=true not permitted on %s", type);
    }
    if (extension && isRequired()) {
      linker.addError("extension fields cannot be required", type);
    }
    linker.validateImport(location(), type);
  }

  private boolean isPackable(Linker linker, ProtoType type) {
    return !type.equals(ProtoType.STRING)
        && !type.equals(ProtoType.BYTES)
        && !(linker.get(type) instanceof MessageType);
  }

  public boolean isExtension() {
    return extension;
  }

  Field retainAll(MarkSet markSet) {
    if (!markSet.contains(type)) return null;

    Field result = new Field(packageName, element, options.retainAll(markSet), extension);
    result.type = type;
    return result;
  }

  static ImmutableList<Field> retainAll(
      MarkSet markSet, ProtoType enclosingType, Collection<Field> fields) {
    ImmutableList.Builder<Field> result = ImmutableList.builder();
    for (Field field : fields) {
      Field retainedField = field.retainAll(markSet);
      if (retainedField != null && markSet.contains(enclosingType, field.name())) {
        result.add(retainedField);
      }
    }
    return result.build();
  }

  public enum Label {
    OPTIONAL, REQUIRED, REPEATED,
    /** Indicates the field is a member of a {@code oneof} block. */
    ONE_OF
  }
}
