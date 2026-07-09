/*
 * Copyright (C) 2026 Square, Inc.
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
package com.squareup.wire.internal

import com.squareup.wire.FieldMask

/**
 * Encodes a field mask as a JSON string like "user.displayName,photo".
 *
 * The case conversions match protobuf-java's `FieldMaskUtil` and are not lossless for paths that
 * violate the protobuf style guide: underscores followed by numbers, consecutive underscores, and
 * leading, trailing, or uppercase characters do not survive a round trip.
 */
object FieldMaskJsonFormatter : JsonFormatter<FieldMask> {
  override fun toStringOrNumber(value: FieldMask): String = value.paths
    .filter { it.isNotEmpty() }
    .joinToString(separator = ",") { it.lowerUnderscoreToLowerCamel() }

  override fun fromString(value: String): FieldMask = FieldMask(
    value.split(',')
      .filter { it.isNotEmpty() }
      .map { it.lowerCamelToLowerUnderscore() },
  )

  private fun String.lowerUnderscoreToLowerCamel(): String {
    val result = StringBuilder()
    var capitalizeNext = false
    for (c in this) {
      if (c == '_') {
        capitalizeNext = true
      } else if (capitalizeNext) {
        result.append(c.uppercaseChar())
        capitalizeNext = false
      } else {
        result.append(c.lowercaseChar())
      }
    }
    return result.toString()
  }

  private fun String.lowerCamelToLowerUnderscore(): String {
    val result = StringBuilder()
    for (c in this) {
      if (c in 'A'..'Z') {
        result.append('_')
      }
      result.append(c.lowercaseChar())
    }
    return result.toString()
  }
}
