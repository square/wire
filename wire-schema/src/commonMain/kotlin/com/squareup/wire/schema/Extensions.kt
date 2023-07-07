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

import com.squareup.wire.schema.internal.isValidTag
import com.squareup.wire.schema.internal.parser.ExtensionsElement
import kotlin.jvm.JvmStatic

data class Extensions(
  val location: Location,
  val documentation: String,
  val values: List<Any>,
) {
  fun validate(linker: Linker) {
    @Suppress("NAME_SHADOWING")
    val linker = linker.withContext(this)
    val outOfRangeTags = mutableListOf<String>()
    values.forEach { value ->
      when (value) {
        is Int -> {
          if (!value.isValidTag()) {
            outOfRangeTags.add("$value")
          }
        }
        is IntRange -> {
          if (!value.first.isValidTag() || !value.last.isValidTag()) {
            outOfRangeTags.add("${value.first} to ${value.last}")
          }
        }
        else -> throw AssertionError()
      }
    }
    if (outOfRangeTags.isNotEmpty()) {
      linker.errors += "tags are out of range: ${outOfRangeTags.joinToString(separator = ", ")}"
    }
  }

  companion object {
    @JvmStatic
    fun fromElements(elements: List<ExtensionsElement>) =
      elements.map { Extensions(it.location, it.documentation, it.values) }

    @JvmStatic
    fun toElements(extensions: List<Extensions>) =
      extensions.map { ExtensionsElement(it.location, it.documentation, it.values) }
  }
}
