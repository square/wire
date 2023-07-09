/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.wire.schema.internal.parser

import com.squareup.wire.schema.Location
import com.squareup.wire.schema.internal.MAX_TAG_VALUE
import com.squareup.wire.schema.internal.appendDocumentation

data class ReservedElement(
  val location: Location,
  val documentation: String = "",
  /** A [String] name or [Int] or [IntRange] tag. */
  val values: List<Any>,
) {
  fun toSchema() = buildString {
    appendDocumentation(documentation)
    append("reserved ")

    values.forEachIndexed { index, value ->
      if (index > 0) append(", ")

      when (value) {
        is String -> append("\"$value\"")
        is Int -> append(value)
        is IntRange -> {
          append("${value.first} to ")
          if (value.last < MAX_TAG_VALUE) {
            append(value.last)
          } else {
            append("max")
          }
        }
        else -> throw AssertionError()
      }
    }
    append(";\n")
  }
}
