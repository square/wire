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

import com.squareup.wire.schema.SyntaxRules
import com.squareup.wire.schema.internal.appendDocumentation
import com.squareup.wire.schema.internal.appendIndented

data class OneOfElement(
  val name: String,
  val documentation: String = "",
  val fields: List<FieldElement> = emptyList(),
  val groups: List<GroupElement> = emptyList()
) {
  fun toSchema(syntaxRules: SyntaxRules = SyntaxRules.get(syntax = null)) = buildString {
    appendDocumentation(documentation)
    append("oneof $name {")

    if (fields.isNotEmpty()) {
      append('\n')
      for (field in fields) {
        appendIndented(field.toSchema(syntaxRules))
      }
    }
    if (groups.isNotEmpty()) {
      append('\n')
      for (group in groups) {
        appendIndented(group.toSchema(syntaxRules))
      }
    }
    append("}\n")
  }
}
