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
package com.squareup.wire.schema.internal.parser

import com.squareup.wire.schema.Location
import com.squareup.wire.schema.SyntaxRules
import com.squareup.wire.schema.internal.appendDocumentation
import com.squareup.wire.schema.internal.appendIndented

data class MessageElement(
  override val location: Location,
  override val name: String,
  override val documentation: String = "",
  override val nestedTypes: List<TypeElement> = emptyList(),
  override val options: List<OptionElement> = emptyList(),
  val reserveds: List<ReservedElement> = emptyList(),
  val fields: List<FieldElement> = emptyList(),
  val oneOfs: List<OneOfElement> = emptyList(),
  val extensions: List<ExtensionsElement> = emptyList(),
  val groups: List<GroupElement> = emptyList()
) : TypeElement {
  override fun toSchema(syntaxRules: SyntaxRules) = buildString {
    appendDocumentation(documentation)
    append("message $name {")

    if (reserveds.isNotEmpty()) {
      append('\n')
      for (reserved in reserveds) {
        appendIndented(reserved.toSchema())
      }
    }
    if (options.isNotEmpty()) {
      append('\n')
      for (option in options) {
        appendIndented(option.toSchemaDeclaration())
      }
    }
    if (fields.isNotEmpty()) {
      append('\n')
      for (field in fields) {
        appendIndented(field.toSchema(syntaxRules))
      }
    }
    if (oneOfs.isNotEmpty()) {
      append('\n')
      for (oneOf in oneOfs) {
        appendIndented(oneOf.toSchema(syntaxRules))
      }
    }
    if (groups.isNotEmpty()) {
      append('\n')
      for (group in groups) {
        appendIndented(group.toSchema(syntaxRules))
      }
    }
    if (extensions.isNotEmpty()) {
      append('\n')
      for (extension in extensions) {
        appendIndented(extension.toSchema())
      }
    }
    if (nestedTypes.isNotEmpty()) {
      append('\n')
      for (type in nestedTypes) {
        appendIndented(type.toSchema(syntaxRules))
      }
    }
    append("}\n")
  }
}
