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

import com.squareup.wire.schema.internal.parser.OneOfElement
import kotlin.jvm.JvmStatic

class OneOf private constructor(
  val name: String,
  val documentation: String,
  val fields: List<Field>
) {
  fun link(linker: Linker) {
    for (field in fields) {
      field.link(linker)
    }
  }

  fun linkOptions(linker: Linker) {
    for (field in fields) {
      field.linkOptions(linker)
    }
  }

  fun retainAll(schema: Schema, markSet: MarkSet, enclosingType: ProtoType): OneOf? {
    val retainedFields = Field.retainAll(schema, markSet, enclosingType, fields)
    return when {
      retainedFields.isEmpty() -> null
      else -> OneOf(name, documentation, retainedFields)
    }
  }

  fun retainLinked(): OneOf? {
    val retainedFields = Field.retainLinked(fields)
    return when {
      retainedFields.isEmpty() -> null
      else -> OneOf(name, documentation, retainedFields)
    }
  }

  companion object {
    @JvmStatic
    fun fromElements(
      packageName: String?,
      elements: List<OneOfElement>,
      extension: Boolean
    ) = elements.map {
      if (it.groups.isNotEmpty()) {
        val (_, location) = it.groups[0]
        throw IllegalStateException("$location: 'group' is not supported")
      }

      return@map OneOf(
          name = it.name,
          documentation = it.documentation,
          fields = Field.fromElements(packageName, it.fields, extension)
      )
    }

    @JvmStatic
    fun toElements(oneOfs: List<OneOf>) =
      oneOfs.map {
        OneOfElement(
            name = it.name,
            documentation = it.documentation,
            fields = Field.toElements(it.fields),
            groups = emptyList()
        )
      }
  }
}
