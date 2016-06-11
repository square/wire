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
import com.squareup.wire.schema.internal.parser.MessageElement;
import com.squareup.wire.schema.internal.parser.TypeElement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public final class MessageType extends Type {
  private final ProtoType protoType;
  private final Location location;
  private final String documentation;
  private final String name;
  private final ImmutableList<Field> declaredFields;
  private final List<Field> extensionFields;
  private final ImmutableList<OneOf> oneOfs;
  private final ImmutableList<Type> nestedTypes;
  private final ImmutableList<Extensions> extensionsList;
  private final ImmutableList<Reserved> reserveds;
  private final Options options;

  private MessageType(ProtoType protoType, Location location, String documentation, String name,
      ImmutableList<Field> declaredFields, List<Field> extensionFields, ImmutableList<OneOf> oneOfs,
      ImmutableList<Type> nestedTypes, ImmutableList<Extensions> extensionsList,
      ImmutableList<Reserved> reserveds, Options options) {
    this.protoType = protoType;
    this.location = location;
    this.documentation = documentation;
    this.name = name;
    this.declaredFields = declaredFields;
    this.extensionFields = extensionFields;
    this.oneOfs = oneOfs;
    this.nestedTypes = nestedTypes;
    this.extensionsList = extensionsList;
    this.reserveds = reserveds;
    this.options = checkNotNull(options);
  }

  @Override public Location location() {
    return location;
  }

  @Override public ProtoType type() {
    return protoType;
  }

  @Override public String documentation() {
    return documentation;
  }

  @Override public ImmutableList<Type> nestedTypes() {
    return nestedTypes;
  }

  @Override public Options options() {
    return options;
  }

  public ImmutableList<Field> fields() {
    return ImmutableList.<Field>builder()
        .addAll(declaredFields)
        .addAll(extensionFields)
        .build();
  }

  public ImmutableList<Field> extensionFields() {
    return ImmutableList.copyOf(extensionFields);
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
    result.addAll(declaredFields);
    result.addAll(extensionFields);
    for (OneOf oneOf : oneOfs) {
      result.addAll(oneOf.fields());
    }
    return result.build();
  }

  /** Returns the field named {@code name}, or null if this type has no such field. */
  public Field field(String name) {
    for (Field field : declaredFields) {
      if (field.name().equals(name)) {
        return field;
      }
    }
    for (OneOf oneOf : oneOfs) {
      for (Field field : oneOf.fields()) {
        if (field.name().equals(name)) {
          return field;
        }
      }
    }
    return null;
  }

  /**
   * Returns the field with the qualified name {@code qualifiedName}, or null if this type has no
   * such field.
   */
  public Field extensionField(String qualifiedName) {
    for (Field field : extensionFields) {
      if (field.qualifiedName().equals(qualifiedName)) {
        return field;
      }
    }
    return null;
  }

  /** Returns the field tagged {@code tag}, or null if this type has no such field. */
  public Field field(int tag) {
    for (Field field : declaredFields) {
      if (field.tag() == tag) {
        return field;
      }
    }
    for (Field field : extensionFields) {
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

  Map<String, Field> extensionFieldsMap() {
    // TODO(jwilson): simplify this to just resolve field values directly.
    Map<String, Field> extensionsForType = new LinkedHashMap<>();
    for (Field field : extensionFields) {
      extensionsForType.put(field.qualifiedName(), field);
    }
    return extensionsForType;
  }

  void addExtensionFields(ImmutableList<Field> fields) {
    extensionFields.addAll(fields);
  }

  void link(Linker linker) {
    linker = linker.withContext(this);
    for (Field field : declaredFields) {
      field.link(linker);
    }
    for (Field field : extensionFields) {
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
    for (Field field : declaredFields) {
      field.linkOptions(linker);
    }
    for (Field field : extensionFields) {
      field.linkOptions(linker);
    }
    for (OneOf oneOf : oneOfs) {
      oneOf.linkOptions(linker);
    }
    options.link(linker);
  }

  void validate(Linker linker) {
    linker = linker.withContext(this);
    linker.validateFields(fieldsAndOneOfFields(), reserveds);
    linker.validateEnumConstantNameUniqueness(nestedTypes);
    for (Field field : fieldsAndOneOfFields()) {
      field.validate(linker);
    }
    for (Type type : nestedTypes) {
      type.validate(linker);
    }
    for (Extensions extensions : extensionsList) {
      extensions.validate(linker);
    }
  }

  @Override Type retainAll(Schema schema, MarkSet markSet) {
    ImmutableList.Builder<Type> retainedNestedTypesBuilder = ImmutableList.builder();
    for (Type nestedType : nestedTypes) {
      Type retainedNestedType = nestedType.retainAll(schema, markSet);
      if (retainedNestedType != null) {
        retainedNestedTypesBuilder.add(retainedNestedType);
      }
    }

    ImmutableList<Type> retainedNestedTypes = retainedNestedTypesBuilder.build();
    if (!markSet.contains(protoType)) {
      // If this type is not retained, and none of its nested types are retained, prune it.
      if (retainedNestedTypes.isEmpty()) {
        return null;
      }
      // If this type is not retained but retained nested types, replace it with an enclosing type.
      return new EnclosingType(location, protoType, documentation, retainedNestedTypes);
    }

    ImmutableList.Builder<OneOf> retainedOneOfsBuilder = ImmutableList.builder();
    for (OneOf oneOf : oneOfs) {
      OneOf retainedOneOf = oneOf.retainAll(schema, markSet, protoType);
      if (retainedOneOf != null) {
        retainedOneOfsBuilder.add(retainedOneOf);
      }
    }
    ImmutableList<OneOf> retainedOneOfs = retainedOneOfsBuilder.build();

    return new MessageType(protoType, location, documentation, name,
        Field.retainAll(schema, markSet, protoType, declaredFields),
        Field.retainAll(schema, markSet, protoType, extensionFields), retainedOneOfs,
        retainedNestedTypes, extensionsList, reserveds, options.retainAll(schema, markSet));
  }

  static MessageType fromElement(String packageName, ProtoType protoType,
      MessageElement messageElement) {
    if (!messageElement.groups().isEmpty()) {
      throw new IllegalStateException("'group' is not supported");
    }

    ImmutableList<Field> declaredFields =
        Field.fromElements(packageName, messageElement.fields(), false);

    // Extension fields be populated during linking.
    List<Field> extensionFields = new ArrayList<>();

    ImmutableList<OneOf> oneOfs = OneOf.fromElements(packageName, messageElement.oneOfs(), false);

    ImmutableList.Builder<Type> nestedTypes = ImmutableList.builder();
    for (TypeElement nestedType : messageElement.nestedTypes()) {
      nestedTypes.add(Type.get(packageName, protoType.nestedType(nestedType.name()), nestedType));
    }

    ImmutableList<Extensions> extensionsList =
        Extensions.fromElements(messageElement.extensions());

    ImmutableList<Reserved> reserveds = Reserved.fromElements(messageElement.reserveds());

    Options options = new Options(Options.MESSAGE_OPTIONS, messageElement.options());

    return new MessageType(protoType, messageElement.location(), messageElement.documentation(),
        messageElement.name(), declaredFields, extensionFields, oneOfs, nestedTypes.build(),
        extensionsList, reserveds, options);
  }

  MessageElement toElement() {
    return MessageElement.builder(location)
        .documentation(documentation)
        .name(name)
        .options(options.toElements())
        .fields(Field.toElements(declaredFields))
        .nestedTypes(Type.toElements(nestedTypes))
        .oneOfs(OneOf.toElements(oneOfs))
        .extensions(Extensions.toElements(extensionsList))
        .reserveds(Reserved.toElements(reserveds))
        .build();
  }
}
