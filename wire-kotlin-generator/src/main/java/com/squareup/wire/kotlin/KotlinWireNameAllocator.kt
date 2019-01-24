/*
 * Copyright 2019 Square Inc.
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
package com.squareup.wire.kotlin

import com.squareup.kotlinpoet.NameAllocator
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.Type

internal class KotlinWireNameAllocator(
    private val emitAndroid: Boolean,
    private val javaInterOp: Boolean
) {
  private val nameAllocatorStore = mutableMapOf<Type, NameAllocator>()


  internal fun create(message: Type): NameAllocator {
    return nameAllocatorStore.getOrPut(message) {
      NameAllocator().apply {
        addNameMappingsForType(message)
      }
    }
  }

  private fun NameAllocator.addNameMappingsForType(message: Type) {
    when (message) {
      is EnumType -> addNameMappingsForEnumType(message)
      is MessageType -> addNameMappingsForMessageType(message)
    }
  }

  private fun NameAllocator.addNameMappingsForEnumType(message: EnumType) {
    newName("value", "value")
    newName("ADAPTER", "ADAPTER")
    message.constants().forEach { constant ->
      newName(constant.name(), constant)
    }
  }

  private fun NameAllocator.addNameMappingsForMessageType(message: MessageType) {
    newName("unknownFields", "unknownFields")
    newName("ADAPTER", "ADAPTER")
    newName("reader", "reader")
    newName("Builder", "Builder")
    newName("builder", "builder")

    if (emitAndroid) {
      newName("CREATOR", "CREATOR")
    }
    message.fieldsAndOneOfFields().forEach { field ->
      newName(field.name(), field)
    }

    if (!javaInterOp) {
      message.oneOfs().forEach { oneOf ->
        newName(oneOf.name(), oneOf)
      }
    }
  }
}