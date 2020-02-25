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

import com.squareup.wire.schema.EnumType.Companion.fromElement
import com.squareup.wire.schema.MessageType.Companion.fromElement
import com.squareup.wire.schema.internal.parser.EnumElement
import com.squareup.wire.schema.internal.parser.MessageElement
import com.squareup.wire.schema.internal.parser.TypeElement
import kotlin.jvm.JvmStatic

abstract class Type {
  abstract val location: Location
  abstract val type: ProtoType
  abstract val documentation: String
  abstract val options: Options
  abstract val nestedTypes: List<Type>
  abstract fun linkMembers(linker: Linker)
  abstract fun linkOptions(linker: Linker, syntaxRules: SyntaxRules)
  abstract fun validate(linker: Linker, syntaxRules: SyntaxRules)
  abstract fun retainAll(schema: Schema, markSet: MarkSet): Type?

  /**
   * Returns a copy of this containing only the types in [linkedTypes], or null if that set is
   * empty. This will return an [EnclosingType] if it is itself not linked, but its nested types are
   * linked.
   *
   * The returned type is a shadow of its former self. It it useful for linking against, but lacks
   * most of the members of the original type.
   */
  abstract fun retainLinked(linkedTypes: Set<ProtoType>): Type?

  /**
   * Returns all types and subtypes which are linked to the type.
   */
  fun typesAndNestedTypes(): List<Type> {
    val typesAndNestedTypes = mutableListOf<Type>()
    typesAndNestedTypes.add(this)
    for (type in nestedTypes) {
      typesAndNestedTypes.addAll(type.typesAndNestedTypes())
    }
    return typesAndNestedTypes
  }

  companion object {
    operator fun get(packageName: String?, protoType: ProtoType, type: TypeElement): Type {
      return when (type) {
        is EnumElement -> fromElement(protoType, type)
        is MessageElement -> fromElement(packageName, protoType, type)
        else -> throw IllegalArgumentException("unexpected type: $type")
      }
    }

    @JvmStatic
    fun fromElements(
      packageName: String?,
      elements: List<TypeElement>
    ) = elements.map {
      val protoType = ProtoType.get(packageName, it.name)
      return@map this[packageName, protoType, it]
    }

    private fun toElement(type: Type): TypeElement {
      return when (type) {
        is EnumType -> type.toElement()
        is MessageType -> type.toElement()
        is EnclosingType -> type.toElement()
        else -> throw IllegalArgumentException("unexpected type: $type")
      }
    }

    @JvmStatic
    fun toElements(types: List<Type>) = types.map { toElement(it) }
  }
}
