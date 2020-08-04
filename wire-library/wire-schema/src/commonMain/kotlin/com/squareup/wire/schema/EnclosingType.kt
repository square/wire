/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.wire.schema

import com.squareup.wire.Syntax
import com.squareup.wire.schema.internal.parser.MessageElement

/** An empty type which only holds nested types.  */
data class EnclosingType(
  override val location: Location,
  override val type: ProtoType,
  override val documentation: String,
  override val nestedTypes: List<Type>,
  override val syntax: Syntax
) : Type() {
  override val options
    get() = Options(Options.MESSAGE_OPTIONS, listOf())

  override fun linkMembers(linker: Linker) {}
  override fun linkOptions(linker: Linker, syntaxRules: SyntaxRules, validate: Boolean) {
    nestedTypes.forEach { it.linkOptions(linker, syntaxRules, validate) }
  }

  override fun validate(linker: Linker, syntaxRules: SyntaxRules) {
    nestedTypes.forEach { it.validate(linker, syntaxRules) }
  }

  override fun retainAll(schema: Schema, markSet: MarkSet): Type? {
    val retainedNestedTypes = nestedTypes.mapNotNull { it.retainAll(schema, markSet) }
    if (retainedNestedTypes.isEmpty()) return null
    return EnclosingType(location, type, documentation, retainedNestedTypes, syntax)
  }

  override fun retainLinked(linkedTypes: Set<ProtoType>): Type? {
    val retainedNestedTypes = nestedTypes.mapNotNull { it.retainLinked(linkedTypes) }
    if (retainedNestedTypes.isEmpty()) return null
    return EnclosingType(location, type, documentation, retainedNestedTypes, syntax)
  }

  fun toElement() = MessageElement(
      location = location,
      name = type.simpleName,
      nestedTypes = toElements(nestedTypes)
  )
}
