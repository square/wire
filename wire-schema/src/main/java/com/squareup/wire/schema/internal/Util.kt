/*
 * Copyright (C) 2013 Square, Inc.
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
package com.squareup.wire.schema.internal

import com.squareup.wire.schema.internal.parser.OptionElement

object Util {
  const val MIN_TAG_VALUE = 1
  const val MAX_TAG_VALUE = (1 shl 29) - 1 // 536,870,911

  private const val RESERVED_TAG_VALUE_START = 19000
  private const val RESERVED_TAG_VALUE_END = 19999

  fun appendDocumentation(
    builder: StringBuilder,
    documentation: String
  ) {
    if (documentation.isEmpty()) {
      return
    }
    for (line in documentation.split("\n")) {
      builder.append("// ")
          .append(line)
          .append('\n')
    }
  }

  fun appendOptions(
    builder: StringBuilder,
    options: List<OptionElement>
  ) {
    val count = options.size
    if (count == 1) {
      builder.append("[")
          .append(options[0].toSchema())
          .append(']')
      return
    }
    builder.append("[\n")
    for (i in 0 until count) {
      val endl = if (i < count - 1) "," else ""
      appendIndented(builder, options[i].toSchema() + endl)
    }
    builder.append(']')
  }

  fun appendIndented(
    builder: StringBuilder,
    value: String
  ) {
    for (line in value.split("\n")) {
      builder.append("  ")
          .append(line)
          .append('\n')
    }
  }

  /** True if the supplied value is in the valid tag range and not reserved.  */
  fun isValidTag(value: Int) =
    value in MIN_TAG_VALUE until RESERVED_TAG_VALUE_START ||
        value in (RESERVED_TAG_VALUE_END + 1) until MAX_TAG_VALUE + 1

}