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

import com.squareup.wire.schema.Options.Companion.FIELD_OPTIONS
import com.squareup.wire.schema.Options.Companion.GOOGLE_PROTOBUF_OPTION_TYPES
import com.squareup.wire.schema.internal.parser.FieldElement
import com.squareup.wire.schema.internal.parser.OptionElement.Companion.PACKED_OPTION_ELEMENT
import kotlin.jvm.JvmStatic

class Field private constructor(
  val packageName: String?,

  val location: Location,

  /** May be null for proto3 fields. */
  val label: Label?,

  val name: String,

  val documentation: String,

  val tag: Int,

  val default: String?,

  private val elementType: String,

  val options: Options,

  val isExtension: Boolean
) {
  // Null until this field is linked.
  var type: ProtoType? = null
    private set

  // Null until this field is linked.
  private var deprecated: Any? = null

  // Null until this field is linked.
  private var packed: Any? = null

  // Null until this field is linked.
  var isRedacted: Boolean = false
    private set

  val isRepeated: Boolean
    get() = label == Label.REPEATED

  val isOptional: Boolean
    get() = label == Label.OPTIONAL

  val isRequired: Boolean
    get() = label == Label.REQUIRED

  /**
   * Returns this field's name, prefixed with its package name. Uniquely identifies extension
   * fields, such as in options.
   */
  val qualifiedName: String
    get() {
      return when {
        packageName != null -> "$packageName.$name"
        else -> name
      }
    }

  val isDeprecated: Boolean
    get() = "true" == deprecated

  val isPacked: Boolean
    get() = "true" == packed

  private fun isPackable(linker: Linker, type: ProtoType): Boolean {
    return type != ProtoType.STRING &&
        type != ProtoType.BYTES &&
        linker.get(type) !is MessageType
  }

  fun link(linker: Linker) {
    type = linker.withContext(this).resolveType(elementType)
  }

  fun linkOptions(linker: Linker, syntaxRules: SyntaxRules) {
    val linker = linker.withContext(this)
    options.link(linker)
    deprecated = options.get(DEPRECATED)
    packed = options.get(PACKED)
        ?: if (syntaxRules.isPackedByDefault(type!!, label)) PACKED_OPTION_ELEMENT.value else null
    // We allow any package name to be used as long as it ends with '.redacted'.
    isRedacted = options.optionMatches(".*\\.redacted", "true")
  }

  fun validate(linker: Linker, syntaxRules: SyntaxRules) {
    val linker = linker.withContext(this)
    if (isPacked && !isPackable(linker, type!!)) {
      linker.addError("packed=true not permitted on $type")
    }
    if (isExtension && isRequired) {
      linker.addError("extension fields cannot be required")
    }
    if (default != null && !syntaxRules.allowUserDefinedDefaultValue()) {
      linker.addError("user-defined default values are not permitted [proto3]")
    }
    linker.validateImport(location, type!!)
  }

  fun retainAll(schema: Schema, markSet: MarkSet, enclosingType: ProtoType): Field? {
    if (!isUsedAsOption(schema, markSet, enclosingType)) {
      // If the type is null this field was never linked. Prune it.
      // TODO(jwilson): perform this transformation in the Linker.
      val type = type ?: return null

      // For map types only the value can participate in pruning as the key will always be scalar.
      if (type.isMap && type.valueType!! !in markSet) return null

      if (!markSet.contains(type)) return null
    }

    val memberName = if (isExtension) qualifiedName else name
    val protoMember = ProtoMember.get(enclosingType, memberName)

    if (!markSet.contains(protoMember) &&
        !(GOOGLE_PROTOBUF_OPTION_TYPES.contains(enclosingType) && !isExtension)) return null

    return withOptions(options.retainAll(schema, markSet))
  }

  /** Returns a copy of this whose options is [options].  */
  private fun withOptions(options: Options): Field {
    val result = Field(
        packageName = packageName,
        location = location,
        label = label,
        name = name,
        documentation = documentation,
        tag = tag,
        default = default,
        elementType = elementType,
        options = options,
        isExtension = isExtension
    )
    result.type = type
    result.deprecated = deprecated
    result.packed = packed
    result.isRedacted = isRedacted
    return result
  }

  private fun isUsedAsOption(schema: Schema, markSet: MarkSet, enclosingType: ProtoType): Boolean {
    for (protoFile in schema.protoFiles) {
      if (protoFile.types.any { isUsedAsOption(markSet, enclosingType, it) }) return true
      if (protoFile.services.any { isUsedAsOption(markSet, enclosingType, it) }) return true
    }
    return false
  }

  private fun isUsedAsOption(
    markSet: MarkSet,
    enclosingType: ProtoType,
    service: Service
  ): Boolean {
    if (service.type() !in markSet) return false

    val protoMember = ProtoMember.get(enclosingType, if (isExtension) qualifiedName else name)
    if (service.options().assignsMember(protoMember)) return true
    if (service.rpcs().any { it.options.assignsMember(protoMember) }) return true

    return false
  }

  private fun isUsedAsOption(markSet: MarkSet, enclosingType: ProtoType, type: Type): Boolean {
    if (type.type !in markSet) return false

    val protoMember = ProtoMember.get(enclosingType, if (isExtension) qualifiedName else name)

    when (type) {
      is MessageType -> {
        if (type.options.assignsMember(protoMember)) return true
        if (type.fields().any { it.options.assignsMember(protoMember) }) return true
      }
      is EnumType -> {
        if (type.options.assignsMember(protoMember)) return true
        if (type.constants.any { it.options.assignsMember(protoMember) }) return true
      }
    }

    if (type.nestedTypes.any { isUsedAsOption(markSet, enclosingType, it) }) return true

    return false
  }

  override fun toString() = name

  enum class Label {
    OPTIONAL,
    REQUIRED,
    REPEATED,
    /** Indicates the field is a member of a `oneof` block.  */
    ONE_OF
  }

  companion object {
    internal val DEPRECATED = ProtoMember.get(FIELD_OPTIONS, "deprecated")
    internal val PACKED = ProtoMember.get(FIELD_OPTIONS, "packed")

    @JvmStatic
    fun fromElements(
      packageName: String?,
      fieldElements: List<FieldElement>,
      extension: Boolean
    ) = fieldElements.map {
      Field(
          packageName = packageName,
          location = it.location,
          label = it.label,
          name = it.name,
          documentation = it.documentation,
          tag = it.tag,
          default = it.defaultValue,
          elementType = it.type,
          options = Options(FIELD_OPTIONS, it.options),
          isExtension = extension
      )
    }

    @JvmStatic
    fun toElements(fields: List<Field>) = fields.map {
      FieldElement(
          location = it.location,
          label = it.label,
          type = it.elementType,
          name = it.name,
          defaultValue = it.default,
          tag = it.tag,
          documentation = it.documentation,
          options = it.options.elements
      )
    }

    @JvmStatic
    fun retainLinked(fields: List<Field>) = fields
        .filter { it.type != null } // If the type is non-null, then the field has been linked.
        .map { it.withOptions(it.options.retainLinked()) }

    @JvmStatic
    fun retainAll(
      schema: Schema,
      markSet: MarkSet,
      enclosingType: ProtoType,
      fields: Collection<Field>
    ) = fields.mapNotNull { it.retainAll(schema, markSet, enclosingType) }
  }
}
