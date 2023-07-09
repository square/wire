/*
 * Copyright (C) 2013 Square, Inc.
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
package com.squareup.wire.internal

import com.squareup.wire.FieldEncoding
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.ReverseProtoWriter
import com.squareup.wire.WireField

class RuntimeMessageAdapter<M : Any, B : Any>(
  private val binding: MessageBinding<M, B>,
) : ProtoAdapter<M>(
  fieldEncoding = FieldEncoding.LENGTH_DELIMITED,
  type = binding.messageType,
  typeUrl = binding.typeUrl,
  syntax = binding.syntax,
) {
  private val messageType = binding.messageType
  val fields = binding.fields

  /**
   * Field bindings by index. The indexes are consistent across all related fields including
   * [jsonNames], [jsonAlternateNames], and the result of [jsonAdapters].
   */
  val fieldBindingsArray: Array<FieldOrOneOfBinding<M, B>> = fields.values.toTypedArray()
  val jsonNames: List<String> = fieldBindingsArray.map { it.jsonName }

  /**
   * When reading JSON these are alternate names for each field. If null the field has no alternate
   * name.
   *
   * For reserved keywords in Kotlin or Java, a field like `public` are written as so although the
   * generated field name is `public_`. We want to read in either form. As well, in proto3 fields
   * declared in snake_case like `customer_id` are written in camelCase like `customerId`, but can
   * be read in either form. We can use exclusive logic between the two cases because there's no
   * keyword defined in snake_case. The alternate name will be absent if the field name isn't a
   * keyword or if the snake and camel cases are the same, such as in single-word identifiers.
   * Lastly, in order to deserialize camelCased field from proto2, we add a camelCase alternative
   * if the `jsonName` doesn't already match it.
   */
  val jsonAlternateNames: List<String?> = run {
    val jsonNames = fieldBindingsArray.map { it.jsonName }
    return@run fieldBindingsArray.map {
      when {
        it.jsonName != it.declaredName -> it.declaredName
        it.jsonName != it.name -> it.name
        else -> {
          val camelCaseDeclaredName = camelCase(it.declaredName)
          if (it.jsonName != camelCaseDeclaredName &&
            // We do not want to shadow an existing `jsonName`.
            !jsonNames.contains(camelCaseDeclaredName)
          ) {
            camelCaseDeclaredName
          } else {
            null
          }
        }
      }
    }
  }

  /** When writing each field as JSON this is the name to use. */
  val FieldOrOneOfBinding<*, *>.jsonName: String
    get() = if (wireFieldJsonName.isEmpty()) declaredName else wireFieldJsonName

  fun newBuilder(): B = binding.newBuilder()

  override fun encodedSize(value: M): Int {
    val cachedSerializedSize = binding.getCachedSerializedSize(value)
    if (cachedSerializedSize != 0) return cachedSerializedSize

    var size = 0
    for (field in fields.values) {
      val fieldValue = field[value] ?: continue
      size += field.adapter.encodedSizeWithTag(field.tag, fieldValue)
    }
    size += binding.unknownFields(value).size

    binding.setCachedSerializedSize(value, size)
    return size
  }

  override fun encode(writer: ProtoWriter, value: M) {
    for (field in fields.values) {
      val binding = field[value] ?: continue
      field.adapter.encodeWithTag(writer, field.tag, binding)
    }
    writer.writeBytes(binding.unknownFields(value))
  }

  override fun encode(writer: ReverseProtoWriter, value: M) {
    writer.writeBytes(binding.unknownFields(value))
    for (f in fieldBindingsArray.size - 1 downTo 0) {
      val field = fieldBindingsArray[f]
      val binding = field[value] ?: continue
      field.adapter.encodeWithTag(writer, field.tag, binding)
    }
  }

  override fun redact(value: M): M {
    val builder = binding.newBuilder()
    for (field in fields.values) {
      if (field.redacted && field.label == WireField.Label.REQUIRED) {
        throw UnsupportedOperationException(
          "Field '${field.name}' in $type is required and cannot be redacted.",
        )
      }
      val isMessage = field.isMessage
      if (field.redacted || isMessage && !field.label.isRepeated) {
        val builderValue = field.getFromBuilder(builder)
        if (builderValue != null) {
          val redactedValue = field.adapter.redact(builderValue)
          field.set(builder, redactedValue)
        }
      } else if (isMessage && field.label.isRepeated) {
        @Suppress("UNCHECKED_CAST")
        val values = field.getFromBuilder(builder) as List<Any>

        @Suppress("UNCHECKED_CAST")
        val adapter = field.singleAdapter as ProtoAdapter<Any>
        field.set(builder, values.redactElements(adapter))
      }
    }
    binding.clearUnknownFields(builder)
    return binding.build(builder)
  }

  override fun equals(other: Any?): Boolean {
    return other is RuntimeMessageAdapter<*, *> && other.messageType == messageType
  }

  override fun hashCode(): Int = messageType.hashCode()

  override fun toString(value: M): String = buildString {
    append(messageType.simpleName)
    append('{')
    var first = true
    for (field in fields.values) {
      val binding = field[value] ?: continue
      if (!first) append(", ")
      first = false
      append(field.name)
      append('=')
      append(if (field.redacted) REDACTED else binding)
    }
    append('}')
  }

  override fun decode(reader: ProtoReader): M {
    val builder = newBuilder()
    val token = reader.beginMessage()
    while (true) {
      val tag = reader.nextTag()
      if (tag == -1) break
      val field = fields[tag]
      try {
        if (field != null) {
          val adapter = if (field.isMap) {
            field.adapter
          } else {
            field.singleAdapter
          }
          val value = adapter.decode(reader)
          field.value(builder, value!!)
        } else {
          val fieldEncoding = reader.peekFieldEncoding()!!
          val value = fieldEncoding.rawProtoAdapter().decode(reader)
          binding.addUnknownField(builder, tag, fieldEncoding, value)
        }
      } catch (e: EnumConstantNotFoundException) {
        // An unknown Enum value was encountered, store it as an unknown field.
        binding.addUnknownField(builder, tag, FieldEncoding.VARINT, e.value.toLong())
      }
    }
    reader.endMessageAndGetUnknownFields(token) // Ignore return value

    return binding.build(builder)
  }

  /**
   * Walk the fields of [message] and invoke [encodeValue] on each that should be written as JSON.
   * This omits fields that have the identity value when that is required.
   */
  fun <A> writeAllFields(
    message: M?,
    jsonAdapters: List<A>,
    redactedFieldsAdapter: A?,
    encodeValue: (String, Any?, A) -> Unit,
  ) {
    var redactedFields: MutableList<String>? = null
    for (index in fieldBindingsArray.indices) {
      val field = fieldBindingsArray[index]
      val value = field[message!!]
      if (field.omitFromJson(syntax, value)) continue
      if (field.redacted && redactedFieldsAdapter != null && value != null) {
        // We initialize here to avoid a performance hit for non-redacted code.
        if (redactedFields == null) {
          redactedFields = mutableListOf()
        }
        redactedFields.add(jsonNames[index])
        continue
      }
      encodeValue(jsonNames[index], value, jsonAdapters[index])
    }
    if (redactedFields?.isNotEmpty() == true) {
      encodeValue("__redacted_fields", redactedFields, redactedFieldsAdapter!!)
    }
  }

  companion object {
    private const val REDACTED = "\u2588\u2588"
  }
}
