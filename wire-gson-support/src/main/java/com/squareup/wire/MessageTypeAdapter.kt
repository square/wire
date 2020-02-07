/*
 * Copyright 2013 Square Inc.
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

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.squareup.wire.WireField.Label
import com.squareup.wire.internal.FieldBinding
import com.squareup.wire.internal.RuntimeMessageAdapter
import java.io.IOException
import java.math.BigInteger

// 2^64, used to convert sint64 values >= 2^63 to unsigned decimal form
private val POWER_64 = BigInteger("18446744073709551616")

internal class MessageTypeAdapter<M : Message<M, B>, B : Message.Builder<M, B>>(
  private val gson: Gson,
  type: TypeToken<M>
) : TypeAdapter<M>() {
  private val messageAdapter: RuntimeMessageAdapter<M, B> =
      RuntimeMessageAdapter.create(type.rawType as Class<M>, "square.github.io/wire/unknown")
  private val fieldBindings: Map<String, FieldBinding<M, B>> =
      messageAdapter.fieldBindings.values.associateBy { it.name }

  @Throws(IOException::class)
  override fun write(out: JsonWriter, message: M?) {
    if (message == null) {
      out.nullValue()
      return
    }

    out.beginObject()
    for (tagBinding in messageAdapter.fieldBindings.values) {
      val value = tagBinding[message] ?: continue
      out.name(tagBinding.name)
      emitJson(out, value, tagBinding.singleAdapter(), tagBinding.label)
    }
    out.endObject()
  }

  private fun emitJson(out: JsonWriter, value: Any, adapter: ProtoAdapter<*>, label: Label) {
    if (adapter === ProtoAdapter.UINT64) {
      if (label.isRepeated) {
        val longs = value as List<Long>
        out.beginArray()
        for (i in 0 until longs.size) {
          emitUint64(longs[i], out)
        }
        out.endArray()
      } else {
        emitUint64(value as Long, out)
      }
    } else {
      gson.toJson(value, value.javaClass, out)
    }
  }

  private fun emitUint64(value: Long, out: JsonWriter) {
    if (value < 0) {
      val unsigned = POWER_64.add(BigInteger.valueOf(value))
      out.value(unsigned)
    } else {
      out.value(value)
    }
  }

  @Throws(IOException::class)
  override fun read(input: JsonReader): M? {
    if (input.peek() == JsonToken.NULL) {
      input.nextNull()
      return null
    }

    val elementAdapter = gson.getAdapter(JsonElement::class.java)
    val builder = messageAdapter.newBuilder()

    input.beginObject()
    while (input.peek() != JsonToken.END_OBJECT) {
      val name = input.nextName()

      val fieldBinding = fieldBindings[name]
      if (fieldBinding == null) {
        input.skipValue()
      } else {
        val element = elementAdapter.read(input)
        val value = parseValue(fieldBinding, element)
        fieldBinding[builder] = value
      }
    }

    input.endObject()
    return builder.build()
  }

  private fun parseValue(fieldBinding: FieldBinding<*, *>, element: JsonElement): Any? {
    if (fieldBinding.label.isRepeated) {
      if (element.isJsonNull) {
        return emptyList<Any>()
      }
      val itemType = fieldBinding.singleAdapter().type!!.javaObjectType
      val adapter = gson.getAdapter(itemType)
      return element.asJsonArray.map(adapter::fromJsonTree)
    }

    if (fieldBinding.isMap) {
      if (element.isJsonNull) {
        return emptyMap<Any, Any>()
      }

      val keyType = fieldBinding.keyAdapter().type!!.javaObjectType
      val valueType = fieldBinding.singleAdapter().type!!.javaObjectType
      val valueAdapter = gson.getAdapter(valueType)

      val jsonObject = element.asJsonObject
      return jsonObject.entrySet().associateBy(
          { gson.fromJson(it.key, keyType) },
          { valueAdapter.fromJsonTree(it.value) }
      )
    }

    val elementType = fieldBinding.singleAdapter().type!!.javaObjectType
    return gson.fromJson(element, elementType)
  }
}
