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
package com.squareup.wire

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.squareup.wire.internal.FieldOrOneOfBinding
import com.squareup.wire.internal.RuntimeMessageAdapter
import java.io.IOException

internal class MessageTypeAdapter<M : Message<M, B>, B : Message.Builder<M, B>>(
  private val messageAdapter: RuntimeMessageAdapter<M, B>,
  private val jsonAdapters: List<TypeAdapter<Any?>>,
) : TypeAdapter<M>() {
  private val nameToField = mutableMapOf<String, JsonField<M, B>>()
    .also { map ->
      for (index in jsonAdapters.indices) {
        val fieldBinding = messageAdapter.fieldBindingsArray[index]
        val jsonField = JsonField(jsonAdapters[index], fieldBinding)
        map[messageAdapter.jsonNames[index]] = jsonField
        val alternateName = messageAdapter.jsonAlternateNames[index]
        if (alternateName != null) {
          map[alternateName] = jsonField
        }
      }
    }

  @Throws(IOException::class)
  override fun write(out: JsonWriter, message: M?) {
    out.beginObject()
    messageAdapter.writeAllFields(
      message = message,
      jsonAdapters = jsonAdapters,
      redactedFieldsAdapter = null,
    ) { name, value, jsonAdapter ->
      out.name(name)
      jsonAdapter.write(out, value)
    }
    out.endObject()
  }

  @Throws(IOException::class)
  override fun read(input: JsonReader): M {
    val builder = messageAdapter.newBuilder()
    input.beginObject()
    while (input.hasNext()) {
      val name = input.nextName()

      val jsonField = nameToField[name]
      if (jsonField == null) {
        input.skipValue()
        continue
      }
      val value = jsonField.adapter.read(input)

      // "If a value is missing in the JSON-encoded data or if its value is null, it will be
      // interpreted as the appropriate default value when parsed into a protocol buffer."
      if (value == null) continue

      jsonField.fieldBinding.set(builder, value)
    }
    input.endObject()
    return builder.build()
  }

  data class JsonField<M : Message<M, B>, B : Message.Builder<M, B>>(
    val adapter: TypeAdapter<Any?>,
    val fieldBinding: FieldOrOneOfBinding<M, B>,
  )
}
