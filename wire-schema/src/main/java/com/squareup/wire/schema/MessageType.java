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
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public final class MessageType extends Type {
  private final Name name;
  private final MessageElement element;
  private final ImmutableList<Field> fields;
  private final ImmutableList<OneOf> oneOfs;
  private final ImmutableList<Type> nestedTypes;
  private final ImmutableList<Extensions> extensionsList;
  private final Options options;

  MessageType(Name name, MessageElement element,
      ImmutableList<Field> fields, ImmutableList<OneOf> oneOfs,
      ImmutableList<Type> nestedTypes, ImmutableList<Extensions> extensionsList, Options options) {
    this.name = name;
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

  @Override public Name name() {
    return name;
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

  public ImmutableList<OneOf> oneOfs() {
    return oneOfs;
  }

  public ImmutableList<Extensions> extensions() {
    return extensionsList;
  }

  void validate(Linker linker) {
    linker = linker.withContext(this);
    linker.validateTags(fieldsAndOneOfFields());
    linker.validateEnumConstantNameUniqueness(nestedTypes);
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

  @Override Type retainAll(Set<String> identifiers) {
    ImmutableList.Builder<Type> retainedNestedTypesBuilder = ImmutableList.builder();
    for (Type nestedType : nestedTypes) {
      Type retainedNestedType = nestedType.retainAll(identifiers);
      if (retainedNestedType != null) {
        retainedNestedTypesBuilder.add(retainedNestedType);
      }
    }

    // If this type is retained, or any of its nested types are retained, keep it.
    ImmutableList<Type> retainedNestedTypes = retainedNestedTypesBuilder.build();
    if (identifiers.contains(name.toString()) || !retainedNestedTypes.isEmpty()) {
      return new MessageType(name, element, fields, oneOfs, retainedNestedTypes, extensionsList,
          options);
    }

    // This type isn't needed.
    return null;
  }
}
