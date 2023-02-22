/*
 * Copyright 2023 Block Inc.
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
package com.squareup.wire.protocwire

import com.google.protobuf.AbstractMessage
import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.GeneratedMessageV3
import com.squareup.wire.schema.internal.parser.OptionElement

internal fun <T : GeneratedMessageV3.ExtendableMessage<T>> parseOptions(options: T, descriptorSource: Plugin.DescriptorSource): List<OptionElement> {
  val optDesc = options.descriptorForType
  val overrideDesc = descriptorSource.findMessageTypeByName(optDesc.fullName)
  if (overrideDesc != null) {
    val dynamicMessage = DynamicMessage.newBuilder(overrideDesc)
      .mergeFrom(options)
      .build()
    return createOptionElements(dynamicMessage)
  }
  return createOptionElements(options)
}

private fun createOptionElements(options: AbstractMessage): List<OptionElement> {
  val elements = mutableListOf<OptionElement>()
  for (entry in options.allFields.entries) {
    val field = entry.key
    val name = if (field.isExtension) field.fullName else field.name
    val (value, kind) = valueOf(entry.value)
    elements.add(OptionElement(name, kind, value, field.isExtension))
  }
  return elements
}

private fun valueOf(value: Any): OptionValueAndKind {
  return when (value) {
    is Number -> OptionValueAndKind(value.toString(), OptionElement.Kind.NUMBER)
    is Boolean -> OptionValueAndKind(value.toString(), OptionElement.Kind.BOOLEAN)
    is String -> OptionValueAndKind(value, OptionElement.Kind.STRING)
    is ByteArray -> OptionValueAndKind(String(toCharArray(value)), OptionElement.Kind.STRING)
    is Descriptors.EnumValueDescriptor -> OptionValueAndKind(value.name, OptionElement.Kind.ENUM)
    is List<*> -> OptionValueAndKind(valueOfList(value), OptionElement.Kind.LIST)
    is AbstractMessage -> OptionValueAndKind(valueOfMessage(value), OptionElement.Kind.MAP)
    else -> throw IllegalStateException("Unexpected field value type: ${value::class.qualifiedName}")
  }
}

private fun toCharArray(bytes: ByteArray): CharArray {
  val chars = CharArray(bytes.size)
  bytes.forEachIndexed { index, element -> chars[index] = element.toInt().toChar() }
  return chars
}

private data class OptionValueAndKind(val value: Any, val kind: OptionElement.Kind)

private fun simpleValue(optionValueAndKind: OptionValueAndKind): Any {
  return if (optionValueAndKind.kind == OptionElement.Kind.BOOLEAN ||
    optionValueAndKind.kind == OptionElement.Kind.ENUM ||
    optionValueAndKind.kind == OptionElement.Kind.NUMBER
  ) {
    OptionElement.OptionPrimitive(optionValueAndKind.kind, optionValueAndKind.value)
  } else {
    optionValueAndKind.value
  }
}

private fun valueOfList(list: List<*>): List<Any> {
  val result = mutableListOf<Any>()
  for (element in list) {
    if (element == null) {
      throw NullPointerException("list value should not contain null")
    }
    result.add(simpleValue(valueOf(element)))
  }
  return result
}

internal fun valueOfMessage(abstractMessage: AbstractMessage): Map<String, Any> {
  val values = mutableMapOf<String, Any>()
  for (entry in abstractMessage.allFields.entries) {
    val fieldDescriptor = entry.key
    val name = if (fieldDescriptor.isExtension) "[${fieldDescriptor.fullName}]" else fieldDescriptor.name
    values[name] = simpleValue(valueOf(entry.value))
  }
  return values
}
