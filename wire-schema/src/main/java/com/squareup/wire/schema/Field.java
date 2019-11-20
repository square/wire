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
import java.util.List;

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

  static ImmutableList<Field> fromElements(String packageName, List<FieldElement> fieldElements,
      boolean extension) {
    ImmutableList.Builder<Field> fields = ImmutableList.builder();
    for (FieldElement field : fieldElements) {
      fields.add(new Field(packageName, field.getLocation(), field.getLabel(), field.getName(),
          field.getDocumentation(), field.getTag(), field.getDefaultValue(), field.getType(),
          new Options(Options.FIELD_OPTIONS, field.getOptions()), extension));
    }
    return fields.build();
  }

  static ImmutableList<FieldElement> toElements(List<Field> fields) {
    ImmutableList.Builder<FieldElement> elements = new ImmutableList.Builder<>();
    for (Field field : fields) {
      elements.add(new FieldElement(
          field.location,
          field.label,
          field.elementType,
          field.name,
          field.defaultValue,
          field.tag,
          field.documentation,
          field.options.toElements()
      ));
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

  Field retainAll(Schema schema, MarkSet markSet, ProtoType enclosingType) {
    if (!isUsedAsOption(schema, markSet, enclosingType)) {
      // For map types only the value can participate in pruning as the key will always be scalar.
      if (type.isMap() && !markSet.contains(type.valueType())) return null;

      if (!markSet.contains(type)) return null;

      ProtoMember protoMember = ProtoMember.get(
          enclosingType,
          isExtension() ? this.qualifiedName() : this.name()
      );
      if (!markSet.contains(protoMember)) return null;
    }

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
      Field retainedField = field.retainAll(schema, markSet, enclosingType);
      if (retainedField != null) {
        result.add(retainedField);
      }
    }
    return result.build();
  }

  private boolean isUsedAsOption(Schema schema, MarkSet markSet, ProtoType enclosingType) {
    for (ProtoFile protoFile : schema.protoFiles()) {
      for (Type type : protoFile.types()) {
        if (isUsedAsOption(markSet, enclosingType, type)) return true;
      }
    }
    return false;
  }

  private boolean isUsedAsOption(MarkSet markSet, ProtoType enclosingType, Type type) {
    if (!markSet.contains(type.type())) return false;

    ProtoMember protoMember = ProtoMember.get(enclosingType, this.qualifiedName());
    if (type instanceof MessageType) {
      if (type.options().map().containsKey(protoMember)) {
        return true;
      }
      for (Field messageField : ((MessageType) type).fields()) {
        if (messageField.options().map().containsKey(protoMember)) {
          return true;
        }
      }
    } else if (type instanceof EnumType) {
      if (type.options().map().containsKey(protoMember)) {
        return true;
      }
      for (EnumConstant constant : ((EnumType) type).constants()) {
        if (constant.getOptions().map().containsKey(protoMember)) {
          return true;
        }
      }
    }
    for (Type nestedType : type.nestedTypes()) {
      if (isUsedAsOption(markSet, enclosingType, nestedType)) return true;
    }
    return false;
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
