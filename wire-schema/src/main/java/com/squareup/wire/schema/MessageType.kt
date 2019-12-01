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
package com.squareup.wire.schema

import com.google.common.collect.ImmutableList
import com.squareup.wire.schema.Extensions.Companion.fromElements
import com.squareup.wire.schema.Extensions.Companion.toElements
import com.squareup.wire.schema.Field.Companion.fromElements
import com.squareup.wire.schema.Field.Companion.retainAll
import com.squareup.wire.schema.Field.Companion.retainLinked
import com.squareup.wire.schema.Field.Companion.toElements
import com.squareup.wire.schema.OneOf.Companion.fromElements
import com.squareup.wire.schema.OneOf.Companion.toElements
import com.squareup.wire.schema.Reserved.Companion.fromElements
import com.squareup.wire.schema.Reserved.Companion.toElements
import com.squareup.wire.schema.internal.parser.MessageElement

class MessageType private constructor(
  override val type: ProtoType,
  override val location: Location,
  override val documentation: String,
  private val name: String,
  val declaredFields: ImmutableList<Field>,
  val extensionFields: MutableList<Field>,
  val oneOfs: ImmutableList<OneOf>,
  override val nestedTypes: ImmutableList<Type>,
  private val extensionsList: List<Extensions>,
  private val reserveds: List<Reserved>,
  override val options: Options
) : Type() {
  fun fields(): ImmutableList<Field> {
    return ImmutableList.builder<Field>()
        .addAll(declaredFields)
        .addAll(extensionFields)
        .build()
  }

  val requiredFields: ImmutableList<Field>
    get() {
      val required = ImmutableList.builder<Field>()
      for (field in fieldsAndOneOfFields) {
        if (field.isRequired) {
          required.add(field)
        }
      }
      return required.build()
    }

  val fieldsAndOneOfFields: ImmutableList<Field>
    get() {
      val result = ImmutableList.builder<Field>()
          .addAll(declaredFields)
          .addAll(extensionFields)
      for (oneOf in oneOfs) {
        result.addAll(oneOf.fields)
      }
      return result.build()
    }

  /** Returns the field named `name`, or null if this type has no such field. */
  fun field(name: String): Field? {
    for (field in declaredFields) {
      if (field.name == name) {
        return field
      }
    }
    for (oneOf in oneOfs) {
      for (field in oneOf.fields) {
        if (field.name == name) {
          return field
        }
      }
    }
    return null
  }

  /**
   * Returns the field with the qualified name `qualifiedName`, or null if this type has no
   * such field.
   */
  fun extensionField(qualifiedName: String): Field? =
      extensionFields.firstOrNull { it.qualifiedName == qualifiedName }

  /** Returns the field tagged `tag`, or null if this type has no such field.  */
  fun field(tag: Int): Field? {
    for (field in declaredFields) {
      if (field.tag == tag) {
        return field
      }
    }
    for (field in extensionFields) {
      if (field.tag == tag) {
        return field
      }
    }
    return null
  }

  fun extensionFieldsMap(): Map<String, Field> {
    // TODO(jwilson): simplify this to just resolve field values directly.
    val extensionsForType = mutableMapOf<String, Field>()
    for (field in extensionFields) {
      extensionsForType[field.qualifiedName] = field
    }
    return extensionsForType
  }

  fun addExtensionFields(fields: List<Field>) {
    extensionFields.addAll(fields)
  }

  override fun linkMembers(linker: Linker) {
    val linker = linker.withContext(this)
    for (field in declaredFields) {
      field.link(linker)
    }
    for (field in extensionFields) {
      field.link(linker)
    }
    for (oneOf in oneOfs) {
      oneOf.link(linker)
    }
  }

  override fun linkOptions(linker: Linker) {
    val linker = linker.withContext(this)
    for (nestedType in nestedTypes) {
      nestedType.linkOptions(linker)
    }
    for (field in declaredFields) {
      field.linkOptions(linker)
    }
    for (field in extensionFields) {
      field.linkOptions(linker)
    }
    for (oneOf in oneOfs) {
      oneOf.linkOptions(linker)
    }
    options.link(linker)
  }

  override fun validate(linker: Linker) {
    val linker = linker.withContext(this)
    linker.validateFields(fieldsAndOneOfFields, reserveds)
    linker.validateEnumConstantNameUniqueness(nestedTypes)
    for (field in fieldsAndOneOfFields) {
      field.validate(linker)
    }
    for (nestedType in nestedTypes) {
      nestedType.validate(linker)
    }
    for (extensions in extensionsList) {
      extensions.validate(linker)
    }
  }

  override fun retainAll(
    schema: Schema,
    markSet: MarkSet
  ): Type? {
    val retainedNestedTypesBuilder = ImmutableList.builder<Type>()
    for (nestedType in nestedTypes) {
      val retainedNestedType = nestedType.retainAll(schema, markSet)
      if (retainedNestedType != null) {
        retainedNestedTypesBuilder.add(retainedNestedType)
      }
    }
    val retainedNestedTypes = retainedNestedTypesBuilder.build()
    if (!markSet.contains(type)) {
      return when {
        // This type is not retained, and none of its nested types are retained, prune it.
        retainedNestedTypes.isEmpty() -> null
        // This type is not retained but retained nested types, replace it with an enclosing type.
        else -> EnclosingType(location, type, documentation, retainedNestedTypes)
      }
    }
    val retainedOneOfsBuilder = ImmutableList.builder<OneOf>()
    for (oneOf in oneOfs) {
      val retainedOneOf = oneOf.retainAll(schema, markSet, type)
      if (retainedOneOf != null) {
        retainedOneOfsBuilder.add(retainedOneOf)
      }
    }
    val retainedOneOfs = retainedOneOfsBuilder.build()
    return MessageType(
        type = type,
        location = location,
        documentation = documentation,
        name = name,
        declaredFields = retainAll(schema, markSet, type, declaredFields),
        extensionFields = retainAll(schema, markSet, type, extensionFields),
        oneOfs = retainedOneOfs,
        nestedTypes = retainedNestedTypes,
        extensionsList = extensionsList,
        reserveds = reserveds,
        options = options.retainAll(schema, markSet)
    )
  }

  override fun retainLinked(linkedTypes: Set<ProtoType>): Type? {
    val retainedNestedTypesBuilder = ImmutableList.builder<Type>()
    for (nestedType in nestedTypes) {
      val retainedNestedType = nestedType.retainLinked(linkedTypes)
      if (retainedNestedType != null) {
        retainedNestedTypesBuilder.add(retainedNestedType)
      }
    }
    val retainedNestedTypes = retainedNestedTypesBuilder.build()
    if (!linkedTypes.contains(type)) {
      return when {
        // This type is not retained, and none of its nested types are retained, prune it.
        retainedNestedTypes.isEmpty() -> null
        // This type is not retained but retained nested types, replace it with an enclosing type.
        else -> EnclosingType(location, type, documentation, retainedNestedTypes)
      }
    }
    // We're retaining this type. Retain its fields and oneofs.
    val retainedOneOfsBuilder = ImmutableList.builder<OneOf>()
    for (oneOf in oneOfs) {
      val retainedOneOf = oneOf.retainLinked()
      if (retainedOneOf != null) {
        retainedOneOfsBuilder.add(retainedOneOf)
      }
    }
    return MessageType(
        type = type,
        location = location,
        documentation = documentation,
        name = name,
        declaredFields = retainLinked(declaredFields),
        extensionFields = retainLinked(extensionFields),
        oneOfs = retainedOneOfsBuilder.build(),
        nestedTypes = retainedNestedTypes,
        extensionsList = ImmutableList.of(),
        reserveds = ImmutableList.of(),
        options = options.retainLinked()
    )
  }

  fun toElement(): MessageElement {
    return MessageElement(
        location = location,
        name = name,
        documentation = documentation,
        nestedTypes = toElements(nestedTypes),
        options = options.elements,
        reserveds = toElements(reserveds),
        fields = toElements(declaredFields),
        oneOfs = toElements(oneOfs),
        extensions = toElements(extensionsList),
        groups = emptyList()
    )
  }

  companion object {
    @JvmStatic fun fromElement(
      packageName: String?,
      protoType: ProtoType,
      messageElement: MessageElement
    ): MessageType {
      check(messageElement.groups.isEmpty()) {
        "${messageElement.groups[0].location}: 'group' is not supported"
      }
      val nestedTypes = ImmutableList.builder<Type>()
      for (nestedType in messageElement.nestedTypes) {
        nestedTypes.add(get(packageName, protoType.nestedType(nestedType.name), nestedType))
      }
      return MessageType(
          type = protoType,
          location = messageElement.location,
          documentation = messageElement.documentation,
          name = messageElement.name,
          declaredFields = fromElements(packageName, messageElement.fields, false),
          extensionFields = mutableListOf(), // Extension fields are populated during linking.
          oneOfs = fromElements(packageName, messageElement.oneOfs, false),
          nestedTypes = nestedTypes.build(),
          extensionsList = fromElements(messageElement.extensions),
          reserveds = fromElements(messageElement.reserveds),
          options = Options(Options.MESSAGE_OPTIONS, messageElement.options)
      )
    }
  }
}
