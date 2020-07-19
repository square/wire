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
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.wire.internal.FieldBinding.JsonFormatter
import com.squareup.wire.internal.RuntimeMessageAdapter
import java.io.IOException
import java.lang.reflect.Type

internal class MessageJsonAdapter<M : Message<M, B>, B : Message.Builder<M, B>>(
  moshi: Moshi,
  type: Type
) : JsonAdapter<M>() {
  private val defaultAdapter = ProtoAdapter.get(type as Class<M>)
  private val messageAdapter = RuntimeMessageAdapter.create(
      messageType = type as Class<M>,
      typeUrl = defaultAdapter.typeUrl,
      syntax = defaultAdapter.syntax
  )
  private val fieldBindings = messageAdapter.fieldBindings.values.toTypedArray()
  private val encodeNames: List<String>
  private val options: JsonReader.Options

  init {
    val optionStrings = mutableListOf<String>()
    val encodeNames = mutableListOf<String>()
    for (fieldBinding in fieldBindings) {
      // Add it for the declared name.
      val declaredName = fieldBinding.declaredName
      optionStrings += declaredName

      val jsonName = fieldBinding.jsonName
      encodeNames += jsonName

      // Make sure we have exactly 2*N unique option strings so the indexes line up. If the camel
      // case name and the declared name are the same, pad the list with a bogus name.
      optionStrings += if (jsonName == declaredName) "$jsonName\u0000" else jsonName
    }
    this.options = JsonReader.Options.of(*optionStrings.toTypedArray())
    this.encodeNames = encodeNames
  }

  private val jsonAdapters: List<JsonAdapter<Any?>> = fieldBindings.map { fieldBinding ->
    var fieldType: Type = fieldBinding.singleAdapter().type?.javaObjectType as Type
    if (fieldBinding.isStruct) {
      return@map StructJsonAdapter.serializeNulls()
    }
    if (fieldBinding.isMap) {
      val keyType = fieldBinding.keyAdapter().type?.javaObjectType
      fieldType = Types.newParameterizedType(Map::class.java, keyType, fieldType)
    } else if (fieldBinding.label.isRepeated) {
      fieldType = Types.newParameterizedType(List::class.java, fieldType)
    }

    val jsonStringAdapter = fieldBinding.jsonStringAdapter(defaultAdapter.syntax)
    if (jsonStringAdapter != null) {
      val single = FormatterJsonAdapter(jsonStringAdapter)
      return@map when {
        fieldBinding.label.isRepeated -> ListJsonAdapter(single).nullSafe() as JsonAdapter<Any?>
        else -> single.nullSafe() as JsonAdapter<Any?>
      }
    }

    return@map moshi.adapter<Any?>(fieldType)
  }

  @Throws(IOException::class)
  override fun toJson(out: JsonWriter, message: M?) {
    if (message == null) {
      out.nullValue()
      return
    }
    out.beginObject()
    for (index in fieldBindings.indices) {
      val fieldBinding = fieldBindings[index]
      val value = fieldBinding[message]
      if (fieldBinding.label == WireField.Label.OMIT_IDENTITY && value == fieldBinding.identity) {
        continue
      }
      out.name(encodeNames[index])
      jsonAdapters[index].toJson(out, value)
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
      val option = input.selectName(options)
      if (option == -1) {
        input.skipName()
        input.skipValue()
        continue
      }
      val index = option / 2
      val fieldBinding = fieldBindings[index]
      val value = jsonAdapters[index]?.fromJson(input) ?: continue

      // If the value was explicitly null we ignore it rather than forcing null into the field.
      // Otherwise malformed JSON that sets a list to null will create a malformed message, and
      // we'd rather just ignore that problem.
      fieldBinding[builder] = value
    }
    input.endObject()
    return builder.build()
  }

  private class FormatterJsonAdapter<T : Any>(
    private val formatter: JsonFormatter<T>
  ) : JsonAdapter<T>() {
    override fun toJson(writer: JsonWriter, value: T?) {
      val stringOrNumber = formatter.toStringOrNumber(value!!)
      if (stringOrNumber is Number) {
        writer.value(stringOrNumber)
      } else {
        writer.value(stringOrNumber as String)
      }
    }

    override fun fromJson(reader: JsonReader): T? {
      val string = reader.nextString()
      try {
        return formatter.fromString(string)
      } catch (_: RuntimeException) {
        throw JsonDataException("decode failed: $string at path ${reader.path}")
      }
    }
  }

  /** Adapt a list of values by delegating to an adapter for a single value. */
  private class ListJsonAdapter<T>(
    private val single: JsonAdapter<T>
  ) : JsonAdapter<List<T?>>() {
    override fun fromJson(reader: JsonReader): List<T?> {
      val result = mutableListOf<T?>()
      reader.beginArray()
      while (reader.hasNext()) {
        result.add(single.fromJson(reader))
      }
      reader.endArray()
      return result
    }

    override fun toJson(writer: JsonWriter, value: List<T?>?) {
      writer.beginArray()
      for (v in value!!) {
        single.toJson(writer, v)
      }
      writer.endArray()
    }
  }
}
