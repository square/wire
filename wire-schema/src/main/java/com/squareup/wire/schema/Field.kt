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
import com.squareup.wire.schema.Options.Companion.FIELD_OPTIONS
import com.squareup.wire.schema.internal.parser.FieldElement

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

  fun linkOptions(linker: Linker) {
    val linker = linker.withContext(this)
    options.link(linker)
    deprecated = options.get(DEPRECATED)
    packed = options.get(PACKED)
    // We allow any package name to be used as long as it ends with '.redacted'.
    isRedacted = options.optionMatches(".*\\.redacted", "true")
  }

  fun validate(linker: Linker) {
    val linker = linker.withContext(this)
    if (isPacked && !isPackable(linker, type!!)) {
      linker.addError("packed=true not permitted on %s", type!!)
    }
    if (isExtension && isRequired) {
      linker.addError("extension fields cannot be required", type!!)
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

      val memberName = if (isExtension) qualifiedName else name
      val protoMember = ProtoMember.get(enclosingType, memberName)
      if (!markSet.contains(protoMember)) return null
    }

    return withOptions(options.retainAll(schema, markSet))
  }

  /** Returns a copy of this whose options is `options`.  */
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
    for (protoFile in schema.getProtoFiles()) {
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

    val protoMember = ProtoMember.get(enclosingType, qualifiedName)
    if (service.options().assignsMember(protoMember)) return true
    if (service.rpcs().any { it.options.assignsMember(protoMember) }) return true

    return false
  }

  private fun isUsedAsOption(markSet: MarkSet, enclosingType: ProtoType, type: Type): Boolean {
    if (type.type!! !in markSet) return false

    val protoMember = ProtoMember.get(enclosingType, qualifiedName)

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
    ): ImmutableList<Field> {
      val fields = ImmutableList.builder<Field>()
      for (element in fieldElements) {
        fields.add(Field(
            packageName = packageName,
            location = element.location,
            label = element.label,
            name = element.name,
            documentation = element.documentation,
            tag = element.tag,
            default = element.defaultValue,
            elementType = element.type,
            options = Options(FIELD_OPTIONS, element.options),
            isExtension = extension
        ))
      }
      return fields.build()
    }

    @JvmStatic
    fun toElements(fields: List<Field>): ImmutableList<FieldElement> {
      val elements = ImmutableList.Builder<FieldElement>()
      for (field in fields) {
        elements.add(FieldElement(
            location = field.location,
            label = field.label,
            type = field.elementType,
            name = field.name,
            defaultValue = field.default,
            tag = field.tag,
            documentation = field.documentation,
            options = field.options.elements
        ))
      }
      return elements.build()
    }

    @JvmStatic
    fun retainLinked(fields: List<Field>): ImmutableList<Field> {
      val result = ImmutableList.builder<Field>()
      for (field in fields) {
        // If the type is non-null, then the field has been linked.
        if (field.type != null) {
          result.add(field.withOptions(field.options.retainLinked()))
        }
      }
      return result.build()
    }

    @JvmStatic
    fun retainAll(
      schema: Schema, markSet: MarkSet, enclosingType: ProtoType, fields: Collection<Field>
    ): ImmutableList<Field> {
      val result = ImmutableList.builder<Field>()
      for (field in fields) {
        val retainedField = field.retainAll(schema, markSet, enclosingType)
        if (retainedField != null) {
          result.add(retainedField)
        }
      }
      return result.build()
    }
  }
}
