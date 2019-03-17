/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.wire.schema.internal.parser

import com.google.common.collect.Range
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.internal.Util.appendDocumentation

data class ReservedElement(
  val location: Location,
  val documentation: String = "",
  /** A [String] name or [Integer] or [Range<Int>][Range] tag. */
  val values: List<Any>
) {
  fun toSchema() = buildString {
    appendDocumentation(this, documentation)
    append("reserved ")

    val value = values
    for (i in value.indices) {
      if (i > 0) append(", ")

      val reservation = value[i]
      when (reservation) {
        is String -> append("\"$reservation\"")
        is Int -> append(reservation)
        is Range<*> -> {
          val range = reservation as Range<Int>
          append("${range.lowerEndpoint()} to ${range.upperEndpoint()}")
        }
        else -> throw AssertionError()
      }
    }
    append(";\n")
  }
}