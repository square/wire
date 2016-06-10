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

import static com.squareup.wire.schema.Options.FIELD_OPTIONS;

public final class Field {
  static final ProtoMember DEPRECATED = ProtoMember.get(FIELD_OPTIONS, "deprecated");
  static final ProtoMember PACKED = ProtoMember.get(FIELD_OPTIONS, "packed");

  private final String packageName;
  private final Location location;
  private final Label label;
  private final String name;
  private final String documentation;
  private final int tag;
  private final String defaultValue;
  private final String elementType;
  private final boolean extension;
  private final Options options;
  private ProtoType type;
  private Object deprecated;
  private Object packed;
  private boolean redacted;

  private Field(String packageName, Location location, Label label, String name,
      String documentation, int tag, String defaultValue, String elementType, Options options,
      boolean extension) {
    this.packageName = packageName;
    this.location = location;
    this.label = label;
    this.name = name;
    this.documentation = documentation;
    this.tag = tag;
    this.defaultValue = defaultValue;
    this.elementType = elementType;
    this.extension = extension;
    this.options = options;
  }

  static ImmutableList<Field> fromElements(String packageName,
      ImmutableList<FieldElement> fieldElements, boolean extension) {
    ImmutableList.Builder<Field> fields = ImmutableList.builder();
    for (FieldElement field : fieldElements) {
      fields.add(new Field(packageName, field.location(), field.label(), field.name(),
          field.documentation(), field.tag(), field.defaultValue(), field.type(),
          new Options(Options.FIELD_OPTIONS, field.options()), extension));
    }
    return fields.build();
  }

  static ImmutableList<FieldElement> toElements(ImmutableList<Field> fields) {
    ImmutableList.Builder<FieldElement> elements = new ImmutableList.Builder<>();
    for (Field field : fields) {
      elements.add(FieldElement.builder(field.location)
          .label(field.label)
          .name(field.name)
          .documentation(field.documentation)
          .tag(field.tag)
          .defaultValue(field.defaultValue)
          .options(field.options.toElements())
          .type(field.elementType)
          .build());
    }
    return elements.build();
  }

  public Location location() {
    return location;
  }

  public String packageName() {
    return packageName;
  }

  public Label label() {
    return label;
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
    return name;
  }

  /**
   * Returns this field's name, prefixed with its package name. Uniquely identifies extension
   * fields, such as in options.
   */
  public String qualifiedName() {
    return packageName != null
        ? packageName + '.' + name
        : name;
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

  public boolean isDeprecated() {
    return "true".equals(deprecated);
  }

  public boolean isPacked() {
    return "true".equals(packed);
  }

  public boolean isRedacted() {
    return redacted;
  }

  public String getDefault() {
    return defaultValue;
  }

  private boolean isPackable(Linker linker, ProtoType type) {
    return !type.equals(ProtoType.STRING)
        && !type.equals(ProtoType.BYTES)
        && !(linker.get(type) instanceof MessageType);
  }

  public boolean isExtension() {
    return extension;
  }

  void link(Linker linker) {
    linker = linker.withContext(this);
    type = linker.resolveType(elementType);
  }

  void linkOptions(Linker linker) {
    linker = linker.withContext(this);
    options.link(linker);
    deprecated = options().get(DEPRECATED);
    packed = options().get(PACKED);
    // We allow any package name to be used as long as it ends with '.redacted'.
    redacted = options().optionMatches(".*\\.redacted", "true");
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

  Field retainAll(Schema schema, MarkSet markSet) {
    // For map types only the value can participate in pruning as the key will always be scalar.
    if (type.isMap() && !markSet.contains(type.valueType())) return null;

    if (!markSet.contains(type)) return null;

    Field result = new Field(packageName, location, label, name, documentation, tag, defaultValue,
        elementType, options.retainAll(schema, markSet), extension);
    result.type = type;
    result.deprecated = deprecated;
    result.packed = packed;
    result.redacted = redacted;
    return result;
  }

  static ImmutableList<Field> retainAll(
      Schema schema, MarkSet markSet, ProtoType enclosingType, Collection<Field> fields) {
    ImmutableList.Builder<Field> result = ImmutableList.builder();
    for (Field field : fields) {
      Field retainedField = field.retainAll(schema, markSet);
      if (retainedField != null && markSet.contains(ProtoMember.get(enclosingType, field.name()))) {
        result.add(retainedField);
      }
    }
    return result.build();
  }

  @Override public String toString() {
    return name();
  }

  public enum Label {
    OPTIONAL, REQUIRED, REPEATED,
    /** Indicates the field is a member of a {@code oneof} block. */
    ONE_OF
  }
}
