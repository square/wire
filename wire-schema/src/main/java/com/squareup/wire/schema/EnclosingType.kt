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

import com.squareup.wire.schema.internal.parser.MessageElement

/** An empty type which only holds nested types.  */
class EnclosingType internal constructor(
  private val location: Location,
  private val type: ProtoType,
  private val documentation: String,
  private val nestedTypes: List<Type>
) : Type() {
  // TODO(jrodbx): Konvert to overridden vals, once Type is konverted
  override fun location() = location
  override fun type() = type
  override fun documentation() = documentation
  override fun options() = throw UnsupportedOperationException()
  override fun nestedTypes() = nestedTypes

  internal override fun linkMembers(linker: Linker) {}
  internal override fun linkOptions(linker: Linker) = nestedTypes.forEach { it.linkOptions(linker) }
  internal override fun validate(linker: Linker) = nestedTypes.forEach { it.validate(linker) }

  internal override fun retainAll(
    schema: Schema,
    markSet: MarkSet
  ): Type? {
    val retainedNestedTypes = nestedTypes.mapNotNull { it.retainAll(schema, markSet) }
    return if (retainedNestedTypes.isEmpty()) null
    else EnclosingType(location, type, documentation, retainedNestedTypes)
  }

  override fun retainLinked(linkedTypes: MutableSet<ProtoType>?): Type? {
    val retainedNestedTypes = nestedTypes.mapNotNull { it.retainLinked(linkedTypes) }
    return if (retainedNestedTypes.isEmpty()) null
    else EnclosingType(location, type, documentation, retainedNestedTypes)
  }

  fun toElement() = MessageElement(
      location = location,
      name = type.simpleName(),
      nestedTypes = Type.toElements(nestedTypes)
  )
}
