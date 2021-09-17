/*
 * Copyright 2021 Square Inc.
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
package com.squareup.wire

internal class KotlinConstructorBuilder<M : Message<M, B>, B : Message.Builder<M, B>>(
  private val messageType: Class<M>,
) : Message.Builder<M, B>() {
  private val fieldValueMap: MutableMap<Int, Pair<WireField, Any?>>
  private val repeatedFieldValueMap: MutableMap<Int, Pair<WireField, MutableList<*>>>

  init {
    val fieldCount = messageType.declaredFields.size
    fieldValueMap = LinkedHashMap(fieldCount)
    repeatedFieldValueMap = LinkedHashMap(fieldCount)
  }

  fun set(
    field: WireField,
    value: Any?
  ) {
    if (field.label.isRepeated) {
      repeatedFieldValueMap[field.tag] = field to (value as MutableList<*>)
    } else {
      fieldValueMap[field.tag] = field to value
      if (value != null && field.label.isOneOf) {
        clobberOtherIsOneOfs(field)
      }
    }
  }

  private fun clobberOtherIsOneOfs(field: WireField) {
    fieldValueMap.values
        .map { it.first }
        .filter { it.oneofName == field.oneofName && it.tag != field.tag }
        .forEach {
          fieldValueMap.remove(it.tag)
        }
  }

  fun get(field: WireField): Any? {
    return if (field.label.isRepeated) {
      repeatedFieldValueMap[field.tag]?.second ?: emptyList<Any>()
    } else {
      fieldValueMap[field.tag]?.second
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun build(): M {
    val args = messageType.declaredWireFields()
        .map(::get)
        .plus(buildUnknownFields())
        .toTypedArray()

    val constructor = messageType.declaredConstructors.first()

    return constructor.newInstance(*args) as M
  }

  private fun Class<M>.declaredWireFields() = declaredFields
      .mapNotNull {
        it.declaredAnnotations.filterIsInstance(WireField::class.java).firstOrNull()
      }
}
