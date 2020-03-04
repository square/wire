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
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.SyntaxRules
import kotlin.jvm.JvmStatic

/** A single `.proto` file.  */
data class ProtoFileElement(
  val location: Location,
  val packageName: String? = null,
  val syntax: ProtoFile.Syntax? = null,
  val imports: List<String> = emptyList(),
  val publicImports: List<String> = emptyList(),
  val types: List<TypeElement> = emptyList(),
  val services: List<ServiceElement> = emptyList(),
  val extendDeclarations: List<ExtendElement> = emptyList(),
  val options: List<OptionElement> = emptyList()
) {
  fun toSchema() = buildString {
    val syntaxRules = SyntaxRules.get(syntax)

    append("// ")
    append(location.withPathOnly())
    append('\n')

    if (syntax != null) {
       append("syntax = \"$syntax\";\n")
    }
    if (packageName != null) {
       append("package $packageName;\n")
    }
    if (imports.isNotEmpty() || publicImports.isNotEmpty()) {
      append('\n')
      for (file in imports) {
        append("import \"$file\";\n")
      }
      for (file in publicImports) {
        append("import public \"$file\";\n")
      }
    }
    if (options.isNotEmpty()) {
      append('\n')
      for (option in options) {
        append(option.toSchemaDeclaration())
      }
    }
    if (types.isNotEmpty()) {
      append('\n')
      for (typeElement in types) {
        append(typeElement.toSchema(syntaxRules))
      }
    }
    if (extendDeclarations.isNotEmpty()) {
      append('\n')
      for (extendDeclaration in extendDeclarations) {
        append(extendDeclaration.toSchema(syntaxRules))
      }
    }
    if (services.isNotEmpty()) {
      append('\n')
      for (service in services) {
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
