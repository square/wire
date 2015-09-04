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
import com.squareup.wire.internal.protoparser.MessageElement;
import java.util.NavigableSet;

import static com.google.common.base.Preconditions.checkNotNull;

public final class MessageType extends Type {
  private final WireType wireType;
  private final MessageElement element;
  private final ImmutableList<Field> fields;
  private final ImmutableList<OneOf> oneOfs;
  private final ImmutableList<Type> nestedTypes;
  private final ImmutableList<Extensions> extensionsList;
  private final Options options;

  MessageType(WireType wireType, MessageElement element,
      ImmutableList<Field> fields, ImmutableList<OneOf> oneOfs,
      ImmutableList<Type> nestedTypes, ImmutableList<Extensions> extensionsList, Options options) {
    this.wireType = wireType;
    this.element = element;
    this.fields = fields;
    this.oneOfs = oneOfs;
    this.nestedTypes = nestedTypes;
    this.extensionsList = extensionsList;
    this.options = checkNotNull(options);
  }

  @Override public Location location() {
    return element.location();
  }

  @Override public WireType name() {
    return wireType;
  }

  @Override public String documentation() {
    return element.documentation();
  }

  @Override public ImmutableList<Type> nestedTypes() {
    return nestedTypes;
  }

  @Override public Options options() {
    return options;
  }

  public ImmutableList<Field> fields() {
    return fields;
  }

  public ImmutableList<Field> getRequiredFields() {
    ImmutableList.Builder<Field> required = ImmutableList.builder();
    for (Field field : fieldsAndOneOfFields()) {
      if (field.isRequired()) {
        required.add(field);
      }
    }
    return required.build();
  }

  public ImmutableList<Field> fieldsAndOneOfFields() {
    ImmutableList.Builder<Field> result = ImmutableList.builder();
    result.addAll(fields);
    for (OneOf oneOf : oneOfs) {
      result.addAll(oneOf.fields());
    }
    return result.build();
  }

  /** Returns the field named {@code name}, or null if this type has no such field. */
  public Field field(String name) {
    for (Field field : fields) {
      if (field.name().equals(name)) {
        return field;
      }
    }
    return null;
  }

  /** Returns the field tagged {@code tag}, or null if this type has no such field. */
  public Field field(int tag) {
    for (Field field : fields) {
      if (field.tag() == tag) {
        return field;
      }
    }
    return null;
  }

  public ImmutableList<OneOf> oneOfs() {
    return oneOfs;
  }

  public ImmutableList<Extensions> extensions() {
    return extensionsList;
  }

  void validate(Linker linker) {
    linker = linker.withContext(this);
    linker.validateTags(fieldsAndOneOfFields(), linker.extensions(name()));
    linker.validateEnumConstantNameUniqueness(nestedTypes);
    for (Field field : fieldsAndOneOfFields()) {
      field.validate(linker, false);
    }
    for (Type type : nestedTypes) {
      type.validate(linker);
    }
    for (Extensions extensions : extensionsList) {
      extensions.validate(linker);
    }
  }

  void link(Linker linker) {
    linker = linker.withContext(this);
    for (Field field : fields) {
      field.link(linker);
    }
    for (OneOf oneOf : oneOfs) {
      oneOf.link(linker);
    }
    for (Type type : nestedTypes) {
      type.link(linker);
    }
  }

  void linkOptions(Linker linker) {
    linker = linker.withContext(this);
    for (Type type : nestedTypes) {
      type.linkOptions(linker);
    }
    for (Field field : fields) {
      field.linkOptions(linker);
    }
    for (OneOf oneOf : oneOfs) {
      oneOf.linkOptions(linker);
    }
    options.link(linker);
  }

  @Override Type retainAll(NavigableSet<String> identifiers) {
    ImmutableList.Builder<Type> retainedNestedTypesBuilder = ImmutableList.builder();
    for (Type nestedType : nestedTypes) {
      Type retainedNestedType = nestedType.retainAll(identifiers);
      if (retainedNestedType != null) {
        retainedNestedTypesBuilder.add(retainedNestedType);
      }
    }

    String typeName = wireType.toString();

    // If this type is not retained, and none of its nested types are retained, prune it.
    ImmutableList<Type> retainedNestedTypes = retainedNestedTypesBuilder.build();
    if (!identifiers.contains(typeName) && retainedNestedTypes.isEmpty()) {
      return null;
    }

    // If any of our fields are specifically retained, retain only that set.
    ImmutableList<Field> retainedFields = fields;
    if (Pruner.hasMarkedMember(identifiers, wireType)) {
      ImmutableList.Builder<Field> retainedFieldsBuilder = ImmutableList.builder();
      for (Field field : fields) {
        if (identifiers.contains(typeName + '#' + field.name())) {
          retainedFieldsBuilder.add(field);
        }
      }
      retainedFields = retainedFieldsBuilder.build();
    }

    return new MessageType(wireType, element, retainedFields, oneOfs, retainedNestedTypes,
        extensionsList, options);
  }
}
