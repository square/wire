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

import com.squareup.wire.schema.Options.ENUM_OPTIONS
import com.squareup.wire.schema.internal.parser.EnumElement

class EnumType private constructor(
  private val protoType: ProtoType,
  private val location: Location,
  private val documentation: String,
  private val name: String,
  private val constants: List<EnumConstant>,
  private val options: Options
) : Type() {
  private var allowAlias: Any? = null

  // TODO(jrodbx): Konvert to overridden vals, once Type is konverted
  override fun location() = location

  override fun type() = protoType
  override fun documentation() = documentation
  override fun options() = options
  override fun nestedTypes() = emptyList<Type>() // Enums do not allow nested type declarations.

  fun allowAlias() = "true" == allowAlias

  /** Returns the constant named `name`, or null if this enum has no such constant.  */
  fun constant(name: String) = constants.find { it.name == name }

  /** Returns the constant tagged `tag`, or null if this enum has no such constant.  */
  fun constant(tag: Int) = constants.find { it.tag == tag }

  fun constants() = constants

  internal override fun linkMembers(linker: Linker) {}

  internal override fun linkOptions(linker: Linker) {
    options.link(linker)
    for (constant in constants) {
      constant.linkOptions(linker)
    }
    allowAlias = options.get(ALLOW_ALIAS)
  }

  internal override fun validate(linker: Linker) {
    var linker = linker
    linker = linker.withContext(this)

    if ("true" != allowAlias) {
      validateTagUniqueness(linker)
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
        linker.addError("%s", error)
      }
    }
  }

  internal override fun retainAll(
    schema: Schema,
    markSet: MarkSet
  ): Type? {
    // If this type is not retained, prune it.
    if (!markSet.contains(protoType)) return null

    val retainedConstants = constants
        .filter { markSet.contains(ProtoMember.get(protoType, it.name)) }
        .map { it.retainAll(schema, markSet) }

    val result = EnumType(
        protoType, location, documentation, name,
        retainedConstants,
        options.retainAll(schema, markSet)
    )
    result.allowAlias = allowAlias
    return result
  }

  override fun retainLinked(linkedTypes: Set<ProtoType>): Type? {
    if (!linkedTypes.contains(type())) {
      return null
    }

    val retainedConstants = constants.map { it.retainLinked() }

    return EnumType(
        protoType, location, documentation, name,
        retainedConstants,
        options.retainLinked()
    )
  }

  fun toElement(): EnumElement {
    return EnumElement(
        location,
        name,
        documentation,
        options.toElements(),
        EnumConstant.toElements(constants)
    )
  }

  companion object {
    internal val ALLOW_ALIAS = ProtoMember.get(ENUM_OPTIONS, "allow_alias")

    @JvmStatic
    fun fromElement(
      protoType: ProtoType,
      enumElement: EnumElement
    ): EnumType {
      val constants = EnumConstant.fromElements(enumElement.constants)
      val options = Options(Options.ENUM_OPTIONS, enumElement.options)

      return EnumType(
          protoType, enumElement.location, enumElement.documentation,
          enumElement.name, constants, options
      )
    }
  }
}
