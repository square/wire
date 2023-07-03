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

import com.squareup.wire.schema.Options.Companion.ONEOF_OPTIONS
import com.squareup.wire.schema.internal.parser.OneOfElement
import kotlin.jvm.JvmStatic

data class OneOf(
  val name: String,
  val documentation: String,
  val fields: List<Field>,
  val location: Location,
  val options: Options,
) {
  fun link(linker: Linker) {
    for (field in fields) {
      field.link(linker)
    }
  }

  fun linkOptions(linker: Linker, syntaxRules: SyntaxRules, validate: Boolean) {
    for (field in fields) {
      field.linkOptions(linker, syntaxRules, validate)
    }
    options.link(linker, location, validate)
  }

  fun retainAll(schema: Schema, markSet: MarkSet, enclosingType: ProtoType): OneOf? {
    val retainedFields = Field.retainAll(schema, markSet, enclosingType, fields)
    return when {
      retainedFields.isEmpty() -> null
      else ->
        OneOf(name, documentation, retainedFields, location, options.retainAll(schema, markSet))
    }
  }

  fun retainLinked(): OneOf? {
    val retainedFields = Field.retainLinked(fields)
    return when {
      retainedFields.isEmpty() -> null
      else -> OneOf(name, documentation, retainedFields, location, options.retainLinked())
    }
  }

  companion object {
    @JvmStatic
    fun fromElements(
      namespaces: List<String>,
      elements: List<OneOfElement>,
    ) = elements.map { element ->
      if (element.groups.isNotEmpty()) {
        val (_, location) = element.groups[0]
        throw IllegalStateException("$location: 'group' is not supported")
      }

      return@map OneOf(
        name = element.name,
        documentation = element.documentation,
        fields = Field.fromElements(namespaces, element.fields, extension = false, oneOf = true),
        location = element.location,
        options = Options(ONEOF_OPTIONS, element.options),
      )
    }

    @JvmStatic
    fun toElements(oneOfs: List<OneOf>) =
      oneOfs.map {
        OneOfElement(
          name = it.name,
          documentation = it.documentation,
          fields = Field.toElements(it.fields),
          groups = emptyList(),
          location = it.location,
          options = it.options.elements,
        )
      }
  }
}
