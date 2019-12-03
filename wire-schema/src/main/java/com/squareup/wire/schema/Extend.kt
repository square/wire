/*
 * Copyright (C) 2015 Square, Inc.
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

import com.google.common.collect.ImmutableList
import com.squareup.wire.schema.Field.Companion.fromElements
import com.squareup.wire.schema.Field.Companion.retainAll
import com.squareup.wire.schema.Field.Companion.toElements
import com.squareup.wire.schema.internal.parser.ExtendElement

class Extend private constructor(
  val location: Location,
  val documentation: String,
  val name: String,
  val fields: List<Field>
) {
  // Null until this extend is linked.
  var type: ProtoType? = null
    private set

  fun link(linker: Linker) {
    val linker = linker.withContext(this)
    type = linker.resolveMessageType(name)
    val type = linker[type!!]
    if (type != null) {
      (type as MessageType).addExtensionFields(fields)
    }
  }

  fun validate(linker: Linker) {
    val linker = linker.withContext(this)
    linker.validateImport(location, type!!)
  }

  fun retainAll(schema: Schema?, markSet: MarkSet?): Extend? {
    val retainedFields = retainAll(schema!!, markSet!!, type!!, fields)
    if (retainedFields.isEmpty()) return null
    return Extend(location, documentation, name, retainedFields)
  }

  companion object {
    @JvmStatic
    fun fromElements(
      packageName: String?,
      extendElements: List<ExtendElement>
    ): ImmutableList<Extend> {
      val extendBuilder = ImmutableList.Builder<Extend>()
      for (element in extendElements) {
        extendBuilder.add(Extend(
            location = element.location,
            documentation = element.documentation,
            name = element.name,
            fields = fromElements(packageName, element.fields, true)
        ))
      }
      return extendBuilder.build()
    }

    @JvmStatic
    fun toElements(extendList: List<Extend>): ImmutableList<ExtendElement> {
      val elements = ImmutableList.Builder<ExtendElement>()
      for (extend in extendList) {
        elements.add(ExtendElement(
            location = extend.location,
            name = extend.name,
            documentation = extend.documentation,
            fields = toElements(extend.fields)))
      }
      return elements.build()
    }
  }
}
