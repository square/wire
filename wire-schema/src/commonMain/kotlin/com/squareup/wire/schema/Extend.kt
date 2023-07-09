/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.wire.schema

import com.squareup.wire.schema.Field.Companion.retainAll
import com.squareup.wire.schema.internal.parser.ExtendElement
import kotlin.jvm.JvmStatic

data class Extend(
  val location: Location,
  val documentation: String,
  val name: String,
  val fields: List<Field>,
) {
  // Null until this extend is linked.
  var type: ProtoType? = null
    private set

  fun member(field: Field): ProtoMember = ProtoMember.get(type!!, field)

  fun link(linker: Linker) {
    @Suppress("NAME_SHADOWING")
    val linker = linker.withContext(this)
    type = linker.resolveMessageType(name)
    val type = linker.get(type!!)
    if (type != null) {
      (type as MessageType).addExtensionFields(fields)
    }
    for (field in fields) {
      field.link(linker)
    }
  }

  fun linkOptions(linker: Linker, syntaxRules: SyntaxRules, validate: Boolean) {
    @Suppress("NAME_SHADOWING")
    val linker = linker.withContext(this)
    for (field in fields) {
      field.linkOptions(linker, syntaxRules, validate)
    }
  }

  fun validate(linker: Linker, syntaxRules: SyntaxRules) {
    @Suppress("NAME_SHADOWING")
    val linker = linker.withContext(this)
    linker.validateImportForType(location, type!!)

    syntaxRules.validateExtension(ProtoType.get(name), linker.errors)
  }

  fun retainAll(schema: Schema, markSet: MarkSet): Extend? {
    val retainedFields = retainAll(schema, markSet, type!!, fields)
    if (retainedFields.isEmpty()) return null
    val result = Extend(location, documentation, name, retainedFields)
    result.type = type
    return result
  }

  fun retainLinked(linkedFields: Set<Field>): Extend? {
    val retainedFields = fields.filter { it in linkedFields }
    if (retainedFields.isEmpty()) return null
    val result = Extend(location, documentation, name, retainedFields)
    result.type = type
    return result
  }

  companion object {
    @JvmStatic
    fun fromElements(
      namespaces: List<String>,
      extendElements: List<ExtendElement>,
    ) = extendElements.map {
      Extend(
        location = it.location,
        documentation = it.documentation,
        name = it.name,
        fields = Field.fromElements(namespaces, it.fields, extension = true, oneOf = false),
      )
    }

    @JvmStatic
    fun toElements(extendList: List<Extend>) = extendList.map {
      ExtendElement(
        location = it.location,
        name = it.name,
        documentation = it.documentation,
        fields = Field.toElements(it.fields),
      )
    }
  }
}
