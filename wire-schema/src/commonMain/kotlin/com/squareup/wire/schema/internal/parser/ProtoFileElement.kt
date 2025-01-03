/*
 * Copyright (C) 2013 Square, Inc.
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

import com.squareup.wire.Syntax
import com.squareup.wire.schema.Location
import kotlin.jvm.JvmStatic

/** A single `.proto` file.  */
data class ProtoFileElement(
  val location: Location,
  val packageName: String? = null,
  val syntax: Syntax? = null,
  val imports: List<String> = emptyList(),
  val publicImports: List<String> = emptyList(),
  val weakImports: List<String> = emptyList(),
  val types: List<TypeElement> = emptyList(),
  val services: List<ServiceElement> = emptyList(),
  val extendDeclarations: List<ExtendElement> = emptyList(),
  val options: List<OptionElement> = emptyList(),
) {
  fun toSchema() = buildString {
    append("// Proto schema formatted by Wire, do not edit.\n")
    append("// Source: ${location.withPathOnly()}\n")

    if (syntax != null) {
      append('\n')
      append("syntax = \"$syntax\";\n")
    }
    if (packageName != null) {
      append('\n')
      append("package $packageName;\n")
    }
    if (imports.isNotEmpty() || publicImports.isNotEmpty() || weakImports.isNotEmpty()) {
      append('\n')
      for (file in imports) {
        append("import \"$file\";\n")
      }
      for (file in publicImports) {
        append("import public \"$file\";\n")
      }
      for (file in weakImports) {
        append("import weak \"$file\";\n")
      }
    }
    if (options.isNotEmpty()) {
      append('\n')
      for (option in options) {
        append(option.toSchemaDeclaration())
      }
    }
    if (types.isNotEmpty()) {
      for (typeElement in types) {
        append('\n')
        append(typeElement.toSchema())
      }
    }
    if (extendDeclarations.isNotEmpty()) {
      for (extendDeclaration in extendDeclarations) {
        append('\n')
        append(extendDeclaration.toSchema())
      }
    }
    if (services.isNotEmpty()) {
      for (service in services) {
        append('\n')
        append(service.toSchema())
      }
    }
  }

  companion object {
    /** Returns an empty proto file to serve as a null object when a file cannot be found. */
    @JvmStatic
    fun empty(path: String): ProtoFileElement {
      return ProtoFileElement(location = Location.get(path))
    }
  }
}
