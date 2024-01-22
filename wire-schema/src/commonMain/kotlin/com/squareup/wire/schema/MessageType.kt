/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema

import com.squareup.wire.Syntax
import com.squareup.wire.schema.Extend.Companion.fromElements
import com.squareup.wire.schema.Extend.Companion.toElements
import com.squareup.wire.schema.Extensions.Companion.fromElements
import com.squareup.wire.schema.Extensions.Companion.toElements
import com.squareup.wire.schema.Field.Companion.fromElements
import com.squareup.wire.schema.Field.Companion.retainAll
import com.squareup.wire.schema.Field.Companion.retainLinked
import com.squareup.wire.schema.Field.Companion.toElements
import com.squareup.wire.schema.OneOf.Companion.fromElements
import com.squareup.wire.schema.OneOf.Companion.toElements
import com.squareup.wire.schema.Options.Companion.MESSAGE_OPTIONS
import com.squareup.wire.schema.Reserved.Companion.fromElements
import com.squareup.wire.schema.Reserved.Companion.toElements
import com.squareup.wire.schema.internal.parser.MessageElement
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic

data class MessageType(
  override val type: ProtoType,
  override val location: Location,
  override val documentation: String,
  override val name: String,
  val declaredFields: List<Field>,
  val extensionFields: MutableList<Field>,
  val oneOfs: List<OneOf>,
  override val nestedTypes: List<Type>,
  override val nestedExtendList: List<Extend>,
  val extensionsList: List<Extensions>,
  private val reserveds: List<Reserved>,
  override val options: Options,
  override val syntax: Syntax,
) : Type() {
  private var deprecated: Any? = null

  val isDeprecated: Boolean
    get() = "true" == deprecated

  @get:JvmName("fields")
  val fields get() = declaredFields + extensionFields

  val requiredFields: List<Field>
    get() = fieldsAndOneOfFields.filter { it.isRequired }

  val fieldsAndOneOfFields: List<Field>
    get() = declaredFields + extensionFields + oneOfs.flatMap { it.fields }

  /** Returns the field named [name], or null if this type has no such field. */
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
   * Returns the field with the qualified name [qualifiedName], or null if this type has no
   * such field.
   */
  fun extensionField(qualifiedName: String): Field? =
    extensionFields.firstOrNull { it.qualifiedName == qualifiedName }

  /** Returns the oneOf named [name], or null if this type has no such oneOf. */
  fun oneOf(name: String): OneOf? =
    oneOfs.firstOrNull { it.name == name }

  /** Returns the field tagged [tag], or null if this type has no such field.  */
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
    @Suppress("NAME_SHADOWING")
    val linker = linker.withContext(this)
    for (field in declaredFields) {
      field.link(linker)
    }
    for (oneOf in oneOfs) {
      oneOf.link(linker)
    }
  }

  override fun linkOptions(linker: Linker, syntaxRules: SyntaxRules, validate: Boolean) {
    @Suppress("NAME_SHADOWING")
    val linker = linker.withContext(this)
    for (nestedType in nestedTypes) {
      nestedType.linkOptions(linker, syntaxRules, validate)
    }
    for (field in declaredFields) {
      field.linkOptions(linker, syntaxRules, validate)
    }
    for (oneOf in oneOfs) {
      oneOf.linkOptions(linker, syntaxRules, validate)
    }
    options.link(linker, location, validate)

    deprecated = options.get(DEPRECATED)
  }

  override fun validate(linker: Linker, syntaxRules: SyntaxRules) {
    @Suppress("NAME_SHADOWING")
    val linker = linker.withContext(this)
    linker.validateFields(fieldsAndOneOfFields, reserveds, syntaxRules)
    linker.validateEnumConstantNameUniqueness(nestedTypes)
    for (field in fieldsAndOneOfFields) {
      field.validate(linker, syntaxRules)
    }
    for (nestedType in nestedTypes) {
      nestedType.validate(linker, syntaxRules)
    }
    for (extensions in extensionsList) {
      extensions.validate(linker)
    }
  }

  override fun retainAll(
    schema: Schema,
    markSet: MarkSet,
  ): Type? {
    val retainedNestedTypes = nestedTypes.mapNotNull { it.retainAll(schema, markSet) }
    val retainedNestedExtends = nestedExtendList.mapNotNull { it.retainAll(schema, markSet) }
    if (!markSet.contains(type) && !Options.GOOGLE_PROTOBUF_OPTION_TYPES.contains(type)) {
      return when {
        // This type is not retained, and none of its nested types are retained, prune it.
        retainedNestedTypes.isEmpty() && retainedNestedExtends.isEmpty() -> null
        // This type is not retained but retained nested types, replace it with an enclosing type.
        else -> EnclosingType(location, type, name, documentation, retainedNestedTypes, retainedNestedExtends, syntax)
      }
    }

    val retainedOneOfs = oneOfs.mapNotNull { it.retainAll(schema, markSet, type) }

    val result = MessageType(
      type = type,
      location = location,
      documentation = documentation,
      name = name,
      declaredFields = retainAll(schema, markSet, type, declaredFields),
      extensionFields = retainAll(schema, markSet, type, extensionFields).toMutableList(),
      oneOfs = retainedOneOfs,
      nestedTypes = retainedNestedTypes,
      nestedExtendList = retainedNestedExtends,
      extensionsList = extensionsList,
      reserveds = reserveds,
      options = options.retainAll(schema, markSet),
      syntax = syntax,
    )
    result.deprecated = deprecated
    return result
  }

  override fun retainLinked(linkedTypes: Set<ProtoType>, linkedFields: Set<Field>): Type? {
    val retainedNestedTypes = nestedTypes.mapNotNull { it.retainLinked(linkedTypes, linkedFields) }
    val retainedNestedExtends = nestedExtendList.mapNotNull { it.retainLinked(linkedFields) }

    if (!linkedTypes.contains(type)) {
      return when {
        // This type is not retained, and none of its nested types are retained, prune it.
        retainedNestedTypes.isEmpty() && retainedNestedExtends.isEmpty() -> null
        // This type is not retained but retained nested types, replace it with an enclosing type.
        else -> EnclosingType(location, type, name, documentation, retainedNestedTypes, retainedNestedExtends, syntax)
      }
    }

    // We're retaining this type. Retain its fields and oneofs.
    val retainedOneOfs = oneOfs.mapNotNull { it.retainLinked() }

    return MessageType(
      type = type,
      location = location,
      documentation = documentation,
      name = name,
      declaredFields = retainLinked(declaredFields),
      extensionFields = retainLinked(extensionFields).toMutableList(),
      oneOfs = retainedOneOfs,
      nestedTypes = retainedNestedTypes,
      nestedExtendList = retainedNestedExtends,
      extensionsList = emptyList(),
      reserveds = emptyList(),
      options = options.retainLinked(),
      syntax = syntax,
    )
  }

  fun toElement(): MessageElement {
    return MessageElement(
      location = location,
      name = name,
      documentation = documentation,
      nestedTypes = toElements(nestedTypes),
      extendDeclarations = toElements(nestedExtendList),
      options = options.elements,
      reserveds = toElements(reserveds),
      fields = toElements(declaredFields),
      oneOfs = toElements(oneOfs),
      extensions = toElements(extensionsList),
      groups = emptyList(),
    )
  }

  companion object {
    internal val DEPRECATED = ProtoMember.get(MESSAGE_OPTIONS, "deprecated")

    @JvmStatic fun fromElement(
      namespaces: List<String>,
      protoType: ProtoType,
      messageElement: MessageElement,
      syntax: Syntax,
    ): MessageType {
      check(messageElement.groups.isEmpty()) {
        "${messageElement.groups[0].location}: 'group' is not supported"
      }
      // Namespaces for all child elements include this message's name.
      val childNamespaces = when {
        namespaces.isEmpty() -> listOf("", messageElement.name) // first element must be package name
        else -> namespaces.plus(messageElement.name)
      }
      val nestedTypes =
        messageElement.nestedTypes.map { get(childNamespaces, protoType.nestedType(it.name), it, syntax) }
      val nestedExtends = fromElements(childNamespaces, messageElement.extendDeclarations)

      return MessageType(
        type = protoType,
        location = messageElement.location,
        documentation = messageElement.documentation,
        name = messageElement.name,
        declaredFields =
        fromElements(childNamespaces, messageElement.fields, extension = false, oneOf = false),
        extensionFields = mutableListOf(), // Extension fields are populated during linking.
        oneOfs = fromElements(childNamespaces, messageElement.oneOfs),
        nestedTypes = nestedTypes,
        nestedExtendList = nestedExtends,
        extensionsList = fromElements(messageElement.extensions),
        reserveds = fromElements(messageElement.reserveds),
        options = Options(MESSAGE_OPTIONS, messageElement.options),
        syntax = syntax,
      )
    }
  }
}
