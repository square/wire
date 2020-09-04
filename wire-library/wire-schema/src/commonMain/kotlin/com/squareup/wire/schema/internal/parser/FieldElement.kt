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
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.SyntaxRules
import com.squareup.wire.schema.internal.appendDocumentation
import com.squareup.wire.schema.internal.appendOptions
import com.squareup.wire.schema.internal.toEnglishLowerCase

data class FieldElement(
  val location: Location,
  val label: Field.Label? = null,
  val type: String,
  val name: String,
  val defaultValue: String? = null,
  val jsonName: String? = null,
  val tag: Int = 0,
  val documentation: String = "",
  val options: List<OptionElement> = emptyList()
) {
  fun toSchema(syntaxRules: SyntaxRules = SyntaxRules.get(syntax = null)) = buildString {
    appendDocumentation(documentation)

    if (label != null) {
      append("${label.name.toEnglishLowerCase()} ")
    }
    append("$type $name = $tag")

    val optionsWithDefault = optionsWithSpecialValues(syntaxRules)
    if (optionsWithDefault.isNotEmpty()) {
      append(' ')
      appendOptions(optionsWithDefault)
    }

    append(";\n")
  }

  /**
   * Both `default` and `json_name` are defined in the schema like options but they are actually
   * not options themselves as they're missing from `google.protobuf.FieldOptions`.
   */
  private fun optionsWithSpecialValues(syntaxRules: SyntaxRules): List<OptionElement> {
    var options =
        if (defaultValue == null || !syntaxRules.allowUserDefinedDefaultValue()) {
          options
        } else {
          val protoType = ProtoType.get(type)
          options + OptionElement.create("default", protoType.toKind(), defaultValue)
        }

    options =
        if (jsonName == null) options
        else options + OptionElement.create("json_name", OptionElement.Kind.STRING, jsonName)

    return options
  }

  // Only non-repeated scalar types and Enums support default values.
  private fun ProtoType.toKind(): OptionElement.Kind {
    return when (simpleName) {
      "bool" -> OptionElement.Kind.BOOLEAN
      "string" -> OptionElement.Kind.STRING
      "bytes",
      "double",
      "float",
      "fixed32",
      "fixed64",
      "int32",
      "int64",
      "sfixed32",
      "sfixed64",
      "sint32",
      "sint64",
      "uint32",
      "uint64" -> OptionElement.Kind.NUMBER
      else -> OptionElement.Kind.ENUM
    }
  }
}
