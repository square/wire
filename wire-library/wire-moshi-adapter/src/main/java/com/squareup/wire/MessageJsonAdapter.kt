/*
 * Copyright 2018 Square Inc.
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

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.wire.internal.RuntimeMessageAdapter
import java.io.IOException

internal class MessageJsonAdapter<M : Message<M, B>, B : Message.Builder<M, B>>(
  private val messageAdapter: RuntimeMessageAdapter<M, B>,
  private val jsonAdapters: List<JsonAdapter<Any?>>
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
    out.beginObject()
    messageAdapter.writeAllFields(message, jsonAdapters) { name, value, jsonAdapter ->
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
      val value = jsonAdapters[index].fromJson(input) ?: continue

      // If the value was explicitly null we ignore it rather than forcing null into the field.
      // Otherwise malformed JSON that sets a list to null will create a malformed message, and
      // we'd rather just ignore that problem.
      val fieldBinding = messageAdapter.fieldBindingsArray[index]
      fieldBinding.set(builder, value)
    }
    input.endObject()
    return builder.build()
  }
}
