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
package com.squareup.wire.schema.internal

import com.squareup.wire.schema.Location
import com.squareup.wire.schema.internal.parser.OptionElement

/**
 * Configures how Wire will generate code for a specific type. This configuration belongs in a
 * `build.wire` file that is in the same directory as the configured type.
 */
data class TypeConfigElement(
  val location: Location,
  val type: String? = null,
  val documentation: String = "",
  val with: List<OptionElement> = emptyList(),
  val target: String? = null,
  val adapter: String? = null,
) {
  fun toSchema() = buildString {
    appendDocumentation(documentation)
    append("type $type {\n")
    for (option in with) {
      appendIndented("with ${option.toSchema()};\n")
    }
    append("  target $target using $adapter;\n")
    append("}\n")
  }
}
