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

import com.squareup.wire.schema.Options.Companion.ENUM_VALUE_OPTIONS
import com.squareup.wire.schema.internal.parser.EnumConstantElement

data class EnumConstant(
  val location: Location,
  val name: String,
  val tag: Int,
  val documentation: String,
  val options: Options,
) {
  val isDeprecated: Boolean
    get() = "true" == options.get(DEPRECATED)

  internal fun toElement() =
    EnumConstantElement(location, name, tag, documentation, options.elements)

  internal fun linkOptions(linker: Linker, validate: Boolean) {
    @Suppress("NAME_SHADOWING")
    val linker = linker.withContext(this)
    options.link(linker, location, validate)
  }

  internal fun retainAll(
    schema: Schema,
    markSet: MarkSet,
  ) = EnumConstant(location, name, tag, documentation, options.retainAll(schema, markSet))

  internal fun retainLinked() =
    EnumConstant(location, name, tag, documentation, options.retainLinked())

  companion object {
    private val DEPRECATED = ProtoMember.get(ENUM_VALUE_OPTIONS, "deprecated")

    internal fun fromElements(elements: List<EnumConstantElement>) =
      elements.map {
        EnumConstant(
          it.location,
          it.name,
          it.tag,
          it.documentation,
          Options(ENUM_VALUE_OPTIONS, it.options),
        )
      }

    internal fun toElements(constants: List<EnumConstant>) = constants.map { it.toElement() }
  }
}
