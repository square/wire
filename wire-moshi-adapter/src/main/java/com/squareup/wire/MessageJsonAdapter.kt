/*
 * Copyright (C) 2018 Square, Inc.
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

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.wire.internal.FieldOrOneOfBinding
import com.squareup.wire.internal.RuntimeMessageAdapter
import java.io.IOException

internal class MessageJsonAdapter<M : Message<M, B>, B : Message.Builder<M, B>>(
  private val messageAdapter: RuntimeMessageAdapter<M, B>,
  private val jsonAdapters: List<JsonAdapter<Any?>>,
  private val redactedFieldsAdapter: JsonAdapter<List<String>>,
) : JsonAdapter<M>() {
  private val jsonNames = messageAdapter.jsonNames
  private val jsonAlternateNames = messageAdapter.jsonAlternateNames

  private val options: JsonReader.Options = run {
    val optionStrings = mutableListOf<String>()
    for (i in jsonNames.indices) {
      val encodeName = jsonNames[i]
      optionStrings += encodeName
      optionStrings += jsonAlternateNames[i] ?: "$encodeName\u0000"
    }
    return@run JsonReader.Options.of(*optionStrings.toTypedArray())
  }

  @Throws(IOException::class)
  override fun toJson(out: JsonWriter, message: M?) {
    @Suppress("UNCHECKED_CAST")
    val redactedFieldsAdapter = when (out.tag(RedactedTag::class.java)?.enabled) {
      true -> redactedFieldsAdapter
      else -> null
    } as JsonAdapter<Any?>?

    out.beginObject()
    messageAdapter.writeAllFields(
      message = message,
      jsonAdapters = jsonAdapters,
      redactedFieldsAdapter = redactedFieldsAdapter,
    ) { name, value, jsonAdapter ->
      out.name(name)
      jsonAdapter.toJson(out, value)
    }
    out.endObject()
  }

  @Throws(IOException::class)
  override fun fromJson(input: JsonReader): M {
    val builder = messageAdapter.newBuilder()
    input.beginObject()
    while (input.hasNext()) {
      val option = input.selectName(options)
      if (option == -1) {
        input.skipName()
        input.skipValue()
        continue
      }
      val index = option / 2

      val value = jsonAdapters[index].fromJson(input)

      // "If a value is missing in the JSON-encoded data or if its value is null, it will be
      // interpreted as the appropriate default value when parsed into a protocol buffer."
      if (value == null) continue

      val fieldBinding = messageAdapter.fieldBindingsArray[index]

      // "null values are not allowed within repeated fields. google.protobuf.NullValue is a
      // special exception to this behavior." Struct types (Value, NullValue, ListValue, Struct)
      // use null as a valid JSON value. https://protobuf.dev/programming-guides/json/
      if (fieldBinding.label.isRepeated && !fieldBinding.isStruct && value is List<*> && null in value) {
        throw JsonDataException(
          "Repeated field ${jsonNames[index]} cannot contain a null element. Null values are " +
            "not allowed inside repeated fields in proto JSON.",
        )
      }

      fieldBinding.set(builder, value)
    }
    input.endObject()
    return builder.build()
  }
}

/** True if this field's values are struct types, for which null is a valid JSON value. */
private val FieldOrOneOfBinding<*, *>.isStruct: Boolean
  get() = singleAdapter == ProtoAdapter.STRUCT_MAP ||
    singleAdapter == ProtoAdapter.STRUCT_LIST ||
    singleAdapter == ProtoAdapter.STRUCT_VALUE ||
    singleAdapter == ProtoAdapter.STRUCT_NULL
