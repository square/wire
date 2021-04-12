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

import com.squareup.wire.Syntax
import com.squareup.wire.schema.Options.Companion.ENUM_OPTIONS
import com.squareup.wire.schema.internal.parser.EnumElement
import kotlin.jvm.JvmStatic

data class EnumType(
  override val type: ProtoType,
  override val location: Location,
  override val documentation: String,
  val name: String,
  val constants: List<EnumConstant>,
  override val options: Options,
  override val syntax: Syntax
) : Type() {
  private var allowAlias: Any? = null

  private var deprecated: Any? = null

  override val nestedTypes: List<Type>
    get() = emptyList() // Enums do not allow nested type declarations.

  fun allowAlias() = "true" == allowAlias

  val isDeprecated: Boolean
    get() = "true" == deprecated

  /** Returns the constant named `name`, or null if this enum has no such constant.  */
  fun constant(name: String) = constants.find { it.name == name }

  /** Returns the constant tagged `tag`, or null if this enum has no such constant.  */
  fun constant(tag: Int) = constants.find { it.tag == tag }

  override fun linkMembers(linker: Linker) {}

  override fun linkOptions(linker: Linker, syntaxRules: SyntaxRules, validate: Boolean) {
    val linker = linker.withContext(this)
    options.link(linker, location, validate = validate)
    for (constant in constants) {
      constant.linkOptions(linker, validate)
    }
    allowAlias = options.get(ALLOW_ALIAS)
    deprecated = options.get(DEPRECATED)
  }

  override fun validate(linker: Linker, syntaxRules: SyntaxRules) {
    val linker = linker.withContext(this)

    if ("true" != allowAlias) {
      validateTagUniqueness(linker)
    }
    validateTagNameAmbiguity("true" == allowAlias, linker)
    syntaxRules.validateEnumConstants(constants, linker.errors)
  }

  private fun validateTagNameAmbiguity(allowAlias: Boolean, linker: Linker) {
    val nameToConstants: Map<String, List<EnumConstant>> =
        constants.groupBy {
          buildString(it.name.length) {
            for (char in it.name) {
              if (char in 'A'..'Z') append(char - ('A' - 'a'))
              else append(char)
            }
          }
        }

    for ((_, constants) in nameToConstants) {
      if (constants.size > 1) {
        if (allowAlias && constants.groupBy { it.tag }.size == 1) continue

        val error = buildString {
          append("Ambiguous constant names (if you are using allow_alias, use the same value for " +
              "these constants):")
          constants.forEach { it ->
            append("\n  ${it.name}:${it.tag} (${it.location})")
          }
        }
        linker.errors += error
      }
    }
  }

  private fun validateTagUniqueness(linker: Linker) {
    val tagToConstants = linkedMapOf<Int, MutableList<EnumConstant>>()
    constants.forEach {
      tagToConstants
          .getOrPut(it.tag) { mutableListOf() }
          .add(it)
    }

    for ((tag, constants) in tagToConstants) {
      if (constants.size > 1) {
        val error = buildString {
          append("multiple enum constants share tag $tag:")
          constants.forEachIndexed { index, it ->
            append("\n  ${index + 1}. ${it.name} (${it.location})")
          }
        }
        linker.errors += error
      }
    }
  }

  override fun retainAll(schema: Schema, markSet: MarkSet): Type? {
    // If this type is not retained, prune it.
    if (!markSet.contains(type)) return null

    val retainedConstants = constants
        .filter { markSet.contains(ProtoMember.get(type, it.name)) }
        .map { it.retainAll(schema, markSet) }

    val result = EnumType(
        type = type,
        location = location,
        documentation = documentation,
        name = name,
        constants = retainedConstants,
        options = options.retainAll(schema, markSet),
        syntax = syntax
    )
    result.allowAlias = allowAlias
    return result
  }

  override fun retainLinked(linkedTypes: Set<ProtoType>): Type? {
    if (!linkedTypes.contains(type)) {
      return null
    }

    val retainedConstants = constants.map { it.retainLinked() }

    return EnumType(
        type = type,
        location = location,
        documentation = documentation,
        name = name,
        constants = retainedConstants,
        options = options.retainLinked(),
        syntax = syntax
    )
  }

  fun toElement(): EnumElement {
    return EnumElement(
        location = location,
        name = name,
        documentation = documentation,
        options = options.elements,
        constants = EnumConstant.toElements(constants)
    )
  }

  companion object {
    internal val ALLOW_ALIAS = ProtoMember.get(ENUM_OPTIONS, "allow_alias")
    internal val DEPRECATED = ProtoMember.get(ENUM_OPTIONS, "deprecated")

    @JvmStatic
    fun fromElement(
      protoType: ProtoType,
      enumElement: EnumElement,
      syntax: Syntax
    ): EnumType {
      return EnumType(
          type = protoType,
          location = enumElement.location,
          documentation = enumElement.documentation,
          name = enumElement.name,
          constants = EnumConstant.fromElements(enumElement.constants),
          options = Options(ENUM_OPTIONS, enumElement.options),
          syntax = syntax
      )
    }
  }
}
