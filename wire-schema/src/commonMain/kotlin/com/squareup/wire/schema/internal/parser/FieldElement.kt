/*
 * Copyright (C) 2014 Square, Inc.
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

import com.squareup.wire.schema.Field
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.internal.appendDocumentation
import com.squareup.wire.schema.internal.appendRenderedOptions
import com.squareup.wire.schema.internal.escapeQuotedString
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
  val options: List<OptionElement> = emptyList(),
) {
  fun toSchema() = buildString {
    appendDocumentation(documentation)

    if (label != null) {
      append("${label.name.toEnglishLowerCase()} ")
    }
    append("$type $name = $tag")

    val optionsWithDefault = optionsWithSpecialValues()
    if (optionsWithDefault.isNotEmpty()) {
      append(' ')
      appendRenderedOptions(optionsWithDefault)
    }

    append(";\n")
  }

  /**
   * Both `default` and `json_name` are defined in the schema like options but they are actually
   * not options themselves as they're missing from `google.protobuf.FieldOptions`.
   */
  private fun optionsWithSpecialValues(): List<String> {
    val result = options.mapTo(mutableListOf()) { it.toSchema() }

    if (defaultValue != null) {
      val protoType = ProtoType.get(type)
      result += when (protoType) {
        // A bytes default has one char per byte, so chars above 0x7E must be written as octal.
        ProtoType.BYTES -> """default = "${defaultValue.escapeQuotedString(bytes = true)}""""
        else -> OptionElement.create("default", protoType.toKind(), defaultValue).toSchema()
      }
    }

    if (jsonName != null) {
      result += OptionElement.create("json_name", OptionElement.Kind.STRING, jsonName).toSchema()
    }

    return result
  }

  // Only non-repeated scalar types and Enums support default values.
  private fun ProtoType.toKind(): OptionElement.Kind = when (simpleName) {
    "bool" -> OptionElement.Kind.BOOLEAN
    "string",
    "bytes",
    -> OptionElement.Kind.STRING
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
    "uint64",
    -> OptionElement.Kind.NUMBER
    else -> OptionElement.Kind.ENUM
  }
}
