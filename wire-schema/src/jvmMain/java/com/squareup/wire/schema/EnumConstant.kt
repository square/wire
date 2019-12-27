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

import com.squareup.wire.schema.internal.parser.EnumConstantElement

class EnumConstant private constructor(
  val location: Location,
  val name: String,
  val tag: Int,
  val documentation: String,
  val options: Options
) {
  internal fun toElement() =
    EnumConstantElement(location, name, tag, documentation, options.elements)

  internal fun linkOptions(linker: Linker) = options.link(linker)

  internal fun retainAll(
    schema: Schema,
    markSet: MarkSet
  ) = EnumConstant(location, name, tag, documentation, options.retainAll(schema, markSet))

  internal fun retainLinked() =
      EnumConstant(location, name, tag, documentation, options.retainLinked())

  companion object {
    internal fun fromElements(elements: List<EnumConstantElement>) =
      elements.map {
        EnumConstant(
            it.location, it.name, it.tag, it.documentation,
            Options(Options.ENUM_VALUE_OPTIONS, it.options)
        )
      }

    internal fun toElements(constants: List<EnumConstant>) = constants.map { it.toElement() }
  }
}
