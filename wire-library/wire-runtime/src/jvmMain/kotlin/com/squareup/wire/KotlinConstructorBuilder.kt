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

import com.squareup.wire.WireField.Label.REPEATED

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
    val protoFields = messageType.declaredProtoFields()

    // KotlinGenerator emits different kinds of properties for various proto fields depending on
    // their type:
    //
    // - For certain types (group A), a primary constructor property is created
    // - For other types (group B), a primary constructor parameter and a simple property are
    //   created
    //
    // This leads to a mismatch between the order in which proto fields annotated with WireField
    // appear in generated code (which matches the order in which they appear in the proto file) and
    // the order of matching constructor parameters (https://github.com/square/wire/issues/2153).
    // Therefore, we'll partition fields from groups A and B separately, where fields in each group
    // will appear in the right order. This will allow us to iterate over the constructor parameters
    // and retrieve the matching proto field based on the parameter type.
    val fieldsWithSimpleProperties = ArrayDeque<ProtoField>()
    val fieldsWithPrimaryConstructorProperties = ArrayDeque<ProtoField>()
    for (protoField in protoFields) {
      // TODO(egor): Repeated fields aren't the only ones for which no primary constructor property
      // is generated. Need to extend this solution to cover other types.
      if (protoField.wireField.label == REPEATED) {
        fieldsWithSimpleProperties += protoField
      } else {
        fieldsWithPrimaryConstructorProperties += protoField
      }
    }

    // We'll assume there's only one constructor with this number of parameters.
    val constructor = messageType.constructors.first {
      it.parameterCount == protoFields.size + 1 // +1 for the unknown_fields.
    }
    val args = constructor.parameters.mapIndexed { index, param ->
      when {
        param.type == List::class.java -> get(fieldsWithSimpleProperties.removeFirst().wireField)
        index == protoFields.size -> buildUnknownFields()
        else -> get(fieldsWithPrimaryConstructorProperties.removeFirst().wireField)
      }
    }
    return constructor.newInstance(*args.toTypedArray()) as M
  }

  private fun Class<M>.declaredProtoFields(): List<ProtoField> = declaredFields
      .mapNotNull { field ->
        val wireField = field.declaredAnnotations.filterIsInstance(WireField::class.java)
            .firstOrNull()
        return@mapNotNull wireField?.let { ProtoField(field.type, wireField) }
      }

  private class ProtoField(
    val type: Class<*>,
    val wireField: WireField,
  )
}
