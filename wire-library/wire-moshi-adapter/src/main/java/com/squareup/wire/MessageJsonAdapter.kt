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
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.wire.internal.RuntimeMessageAdapter
import java.io.IOException
import java.lang.reflect.Type

internal class MessageJsonAdapter<M : Message<M, B>, B : Message.Builder<M, B>>(
  moshi: Moshi,
  type: Type
) : JsonAdapter<M>() {
  private val messageAdapter = RuntimeMessageAdapter.create(type as Class<M>, "square.github.io/wire/unknown")
  private val fieldBindings = messageAdapter.fieldBindings.values.toTypedArray()
  private val options = JsonReader.Options.of(*fieldBindings.map { it.name }.toTypedArray())

  private val jsonAdapters = fieldBindings.map { fieldBinding ->
    var fieldType: Type = fieldBinding.singleAdapter().type?.javaObjectType as Type
    if (fieldBinding.isMap) {
      val keyType = fieldBinding.keyAdapter().type?.javaObjectType
      fieldType = Types.newParameterizedType(Map::class.java, keyType, fieldType)
    } else if (fieldBinding.label.isRepeated) {
      fieldType = Types.newParameterizedType(List::class.java, fieldType)
    }
    var qualifiers: Array<Class<out Annotation>> = emptyArray()
    if (fieldBinding.singleAdapter() === ProtoAdapter.UINT64) {
      qualifiers = arrayOf(Uint64::class.java)
    }
    return@map moshi.adapter<Any>(fieldType, *qualifiers)
  }

  @Throws(IOException::class)
  override fun toJson(out: JsonWriter, message: M?) {
    if (message == null) {
      out.nullValue()
      return
    }
    out.beginObject()
    fieldBindings.forEachIndexed { index, fieldBinding ->
      out.name(fieldBinding.name)
      val value = fieldBinding[message]
      jsonAdapters[index]?.toJson(out, value)
    }
    out.endObject()
  }

  @Throws(IOException::class)
  override fun fromJson(input: JsonReader): M? {
    if (input.peek() == JsonReader.Token.NULL) {
      input.nextNull<Any>()
      return null
    }
    val builder = messageAdapter.newBuilder()
    input.beginObject()
    while (input.hasNext()) {
      val index = input.selectName(options)
      if (index == -1) {
        input.skipName()
        input.skipValue()
        continue
      }
      val fieldBinding = fieldBindings[index]
      if (fieldBinding == null) {
        input.skipValue()
        continue
      }
      val value = jsonAdapters[index]?.fromJson(input) ?: continue

      // If the value was explicitly null we ignore it rather than forcing null into the field.
      // Otherwise malformed JSON that sets a list to null will create a malformed message, and
      // we'd rather just ignore that problem.
      fieldBinding[builder] = value
    }
    input.endObject()
    return builder.build()
  }
}
