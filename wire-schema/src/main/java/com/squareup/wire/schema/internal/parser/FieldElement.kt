/*
 * Copyright (C) 2014 Square, Inc.
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

import com.squareup.wire.schema.Field
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.internal.Util
import com.squareup.wire.schema.internal.Util.appendDocumentation
import java.util.Locale

data class FieldElement(
  val location: Location,
  val label: Field.Label? = null,
  val type: String,
  val name: String,
  val defaultValue: String? = null,
  val tag: Int = 0,
  val documentation: String = "",
  val options: List<OptionElement> = emptyList()
) {
  fun toSchema() = buildString {
    appendDocumentation(this, documentation)

    if (label != null) {
      append("${label.name.toLowerCase(Locale.US)} ")
    }
    append("$type $name = $tag")

    if (options.isNotEmpty()) {
      append(' ')
      Util.appendOptions(this, options)
    }

    append(";\n")
  }
}
