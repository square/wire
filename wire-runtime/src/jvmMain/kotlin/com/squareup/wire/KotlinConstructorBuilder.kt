/*
 * Copyright (C) 2021 Square, Inc.
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
package com.squareup.wire

import kotlin.collections.set
import okio.ByteString

internal class KotlinConstructorBuilder<M : Message<M, B>, B : Message.Builder<M, B>>(
  private val messageType: Class<M>,
) : Message.Builder<M, B>() {
  private val fieldValueMap: MutableMap<Int, Pair<WireField, Any?>>
  private val repeatedFieldValueMap: MutableMap<Int, Pair<WireField, MutableList<*>>>
  private val mapFieldKeyValueMap: MutableMap<Int, Pair<WireField, MutableMap<*, *>>>
  private val sealedOneofValues: MutableMap<String, Any?> = LinkedHashMap()

  init {
    val fieldCount = messageType.declaredFields.size
    fieldValueMap = LinkedHashMap(fieldCount)
    repeatedFieldValueMap = LinkedHashMap(fieldCount)
    mapFieldKeyValueMap = LinkedHashMap(fieldCount)
  }

  fun set(
    field: WireField,
    value: Any?,
  ) {
    when {
      field.isMap -> {
        mapFieldKeyValueMap[field.tag] = field to (value as MutableMap<*, *>)
      }
      field.label.isRepeated -> {
        repeatedFieldValueMap[field.tag] = field to (value as MutableList<*>)
      }
      else -> {
        fieldValueMap[field.tag] = field to value
        if (value != null && field.label.isOneOf) {
          clobberOtherIsOneOfs(field)
        }
      }
    }
  }

  fun setSealedOneof(fieldName: String, value: Any?) {
    sealedOneofValues[fieldName] = value
  }

  fun getSealedOneof(fieldName: String): Any? = sealedOneofValues[fieldName]

  private fun clobberOtherIsOneOfs(field: WireField) {
    fieldValueMap.values
      .map { it.first }
      .filter { it.oneofName == field.oneofName && it.tag != field.tag }
      .forEach {
        fieldValueMap.remove(it.tag)
      }
  }

  fun get(field: WireField): Any? = if (field.isMap) {
    mapFieldKeyValueMap[field.tag]?.second ?: mapOf<Any, Any>()
  } else if (field.label.isRepeated) {
    repeatedFieldValueMap[field.tag]?.second ?: listOf<Any>()
  } else {
    val value = fieldValueMap[field.tag]?.second
    // Proto3 singular fields have non-nullable types with default parameters, we need to pass
    // the identity value to please the constructor.
    if (value == null && field.label == WireField.Label.OMIT_IDENTITY) {
      ProtoAdapter.get(field.adapter).identity
    } else {
      value
    }
  }

  override fun build(): M {
    data class ConstructorParam(val type: Class<*>, val schemaIndex: Int, val value: () -> Any?)

    // Retrieve constructor explicitly since `Constructor#getParameterCount` was introduced in JDK
    // 1.8 and may not be available. ByteString is for `unknown_fields`.
    val params = messageType.declaredFields.mapNotNull { field ->
      val wireField = field.getAnnotation(WireField::class.java)
      if (wireField != null) {
        return@mapNotNull ConstructorParam(field.type, wireField.schemaIndex) { get(wireField) }
      }
      val sealedOneof = field.getAnnotation(WireSealedOneof::class.java)
      if (sealedOneof != null) {
        val name = field.name
        return@mapNotNull ConstructorParam(field.type, sealedOneof.schemaIndex) { sealedOneofValues[name] }
      }
      null
    }.sortedBy { it.schemaIndex }

    val constructor = messageType.getDeclaredConstructor(
      *(params.map { it.type } + ByteString::class.java).toTypedArray(),
    )
    val args = (params.map { it.value() } + buildUnknownFields()).toTypedArray()
    return constructor.newInstance(*args) as M
  }
}

private val WireField.isMap: Boolean
  get() = keyAdapter.isNotEmpty()
