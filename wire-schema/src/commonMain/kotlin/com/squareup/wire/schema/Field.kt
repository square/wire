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

import com.squareup.wire.schema.Options.Companion.FIELD_OPTIONS
import com.squareup.wire.schema.Options.Companion.GOOGLE_PROTOBUF_OPTION_TYPES
import com.squareup.wire.schema.internal.parser.FieldElement
import com.squareup.wire.schema.internal.parser.OptionElement.Companion.PACKED_OPTION_ELEMENT
import kotlin.jvm.JvmStatic

data class Field(
  /**
   * The namespaces in which the field is defined. For top-level extensions in a
   * file that has no package declaration, this may be empty. For normal fields
   * and extensions nested inside a message, the first entry will always be the
   * package name, which might be the empty string if defined in a file that has
   * no package declaration. Subsequent entries will be the names of enclosing
   * messages, outer-most to inner-most.
   */
  val namespaces: List<String>,

  val location: Location,

  /** May be null for proto3 fields, one-of's, or maps. */
  val label: Label?,

  val name: String,

  val documentation: String,

  val tag: Int,

  val default: String?,

  private var elementType: String,

  val options: Options,

  val isExtension: Boolean,

  val isOneOf: Boolean,

  val declaredJsonName: String?,
) {
  // Null until this field is linked.
  var type: ProtoType? = null
    private set

  // Null until this field is linked.
  private var deprecated: Any? = null

  // False until this field is linked.
  var isRedacted: Boolean = false
    private set

  val isRepeated: Boolean
    get() = label == Label.REPEATED

  val isRequired: Boolean
    get() = encodeMode == EncodeMode.REQUIRED

  // Null until this field is linked.
  var encodeMode: EncodeMode? = null
    private set

  /**
   * Returns this field's name, prefixed with its namespaces. Uniquely identifies extension
   * fields, such as in options.
   */
  val qualifiedName: String
    get() {
      val prefix = namespaces.joinToString(".").removePrefix(".")
      return when {
        prefix.isNotEmpty() -> "$prefix.$name"
        else -> name
      }
    }

  /**
   * Returns the package in which this field is defined. If the file that
   * defined this field has no package declaration, returns the empty string.
   */
  val packageName: String
    get() {
      return when {
        namespaces.isNotEmpty() -> namespaces[0]
        else -> ""
      }
    }

  val isDeprecated: Boolean
    get() = "true" == deprecated

  val isPacked: Boolean
    get() = encodeMode == EncodeMode.PACKED

  val useArray: Boolean
    get() = options.get(WIRE_USE_ARRAY) == "true"

  // Null until this field is linked.
  var jsonName: String? = null
    private set

  private fun isPackable(linker: Linker, type: ProtoType): Boolean {
    return type != ProtoType.STRING &&
      type != ProtoType.BYTES &&
      linker.get(type) !is MessageType
  }

  fun link(linker: Linker) {
    type = linker.withContext(this).resolveType(elementType)
    if (type == ProtoType.BYTES && elementType != "bytes") {
      // The type has been opaqued, we update its proto definition as well.
      elementType = "bytes"
    }
  }

  fun linkOptions(linker: Linker, syntaxRules: SyntaxRules, validate: Boolean) {
    @Suppress("NAME_SHADOWING")
    val linker = linker.withContext(this)
    options.link(linker, location, validate)
    deprecated = options.get(DEPRECATED)
    val packed = options.get(PACKED)
      ?: if (syntaxRules.isPackedByDefault(type!!, label)) PACKED_OPTION_ELEMENT.value else null
    // We allow any package name to be used as long as it ends with '.redacted'.
    isRedacted = options.optionMatches(".*\\.redacted", "true")

    encodeMode =
      syntaxRules.getEncodeMode(type!!, label, isPacked = packed == "true", isOneOf = isOneOf)
    jsonName = syntaxRules.jsonName(name, declaredJsonName)
  }

  fun validate(linker: Linker, syntaxRules: SyntaxRules) {
    @Suppress("NAME_SHADOWING")
    val linker = linker.withContext(this)
    if (isPacked && !isPackable(linker, type!!)) {
      linker.errors += "packed=true not permitted on $type"
    }
    if (useArray && !isPacked) {
      linker.errors += "wire.use_array=true only permitted on packed fields"
    }
    if (useArray && type?.isScalar != true) {
      linker.errors += "wire.use_array=true only permitted on scalar fields"
    }
    if (isExtension) {
      if (isRequired) {
        linker.errors += "extension fields cannot be required"
      }
      if (type!!.isMap) {
        linker.errors += "extension fields cannot be a map"
      }
    }
    syntaxRules.validateDefaultValue(default != null, linker.errors)
    if (type!!.isMap) {
      val valueType = linker.get(type!!.valueType!!)
      if (valueType is EnumType && valueType.constants[0].tag != 0) {
        linker.errors += "enum value in map must define 0 as the first value"
      }
    }
    linker.validateImportForType(location, type!!)
  }

  fun retainAll(schema: Schema, markSet: MarkSet, enclosingType: ProtoType): Field? {
    // TODO(jwilson): perform this transformation in the Linker.
    val type = type ?: return null

    // For map types only the value can participate in pruning as the key will always be scalar.
    if (type.isMap && type.valueType!! !in markSet) return null

    if (!markSet.contains(type)) return null

    val memberName = if (isExtension) qualifiedName else name
    val protoMember = ProtoMember.get(enclosingType, memberName)

    if (!markSet.contains(protoMember) &&
      !(GOOGLE_PROTOBUF_OPTION_TYPES.contains(enclosingType) && !isExtension)
    ) {
      return null
    }

    return withOptions(options.retainAll(schema, markSet))
  }

  /** Returns a copy of this whose options is [options].  */
  private fun withOptions(options: Options): Field {
    val result = Field(
      namespaces = namespaces,
      location = location,
      label = label,
      name = name,
      documentation = documentation,
      tag = tag,
      default = default,
      elementType = elementType,
      options = options,
      isExtension = isExtension,
      isOneOf = isOneOf,
      declaredJsonName = declaredJsonName,
    )
    result.type = type
    result.deprecated = deprecated
    result.encodeMode = encodeMode
    result.isRedacted = isRedacted
    result.jsonName = jsonName
    return result
  }

  override fun toString() = name

  enum class Label {
    OPTIONAL,
    REQUIRED,
    REPEATED,

    /** Indicates the field is a member of a `oneof` block.  */
    ONE_OF,
  }

  enum class EncodeMode {
    /** Optional, or OneOf from proto2. */
    NULL_IF_ABSENT,

    /** Required from proto2. */
    REQUIRED,

    /** Non-repeated fields in proto3. Identity can be `0`, `false`, `""`, or `null`. */
    OMIT_IDENTITY,

    /** List. */
    REPEATED,

    /** Packed encoded list. */
    PACKED,

    /** Map. */
    MAP,
  }

  companion object {
    internal val DEPRECATED = ProtoMember.get(FIELD_OPTIONS, "deprecated")
    internal val PACKED = ProtoMember.get(FIELD_OPTIONS, "packed")
    internal val WIRE_USE_ARRAY = ProtoMember.get(FIELD_OPTIONS, "wire.use_array")

    @JvmStatic
    fun fromElements(
      namespaces: List<String>,
      fieldElements: List<FieldElement>,
      extension: Boolean,
      oneOf: Boolean,
    ) = fieldElements.map {
      Field(
        namespaces = namespaces,
        location = it.location,
        label = it.label,
        name = it.name,
        documentation = it.documentation,
        tag = it.tag,
        default = it.defaultValue,
        elementType = it.type,
        options = Options(FIELD_OPTIONS, it.options),
        isExtension = extension,
        isOneOf = oneOf,
        declaredJsonName = it.jsonName,
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
        jsonName = it.declaredJsonName,
        tag = it.tag,
        documentation = it.documentation,
        options = it.options.elements,
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
      fields: Collection<Field>,
    ) = fields.mapNotNull { it.retainAll(schema, markSet, enclosingType) }
  }
}
