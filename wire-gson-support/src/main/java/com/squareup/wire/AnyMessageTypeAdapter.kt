/*
 * Copyright (C) 2020 Square, Inc.
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

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException

class AnyMessageTypeAdapter(
  private val gson: Gson,
  private val typeUrlToAdapter: Map<String, ProtoAdapter<*>>,
) : TypeAdapter<AnyMessage>() {
  private val elementAdapter: TypeAdapter<JsonElement> = gson.getAdapter(JsonElement::class.java)

  @Throws(IOException::class)
  override fun write(writer: JsonWriter, value: AnyMessage?) {
    if (value == null) {
      writer.nullValue()
      return
    }
    writer.beginObject()

    writer.name("@type")
    writer.value(value.typeUrl)

    val protoAdapter = typeUrlToAdapter[value.typeUrl]
      ?: throw IOException("Cannot find type for url: ${value.typeUrl}")

    @Suppress("UNCHECKED_CAST")
    val delegate = gson.getAdapter(protoAdapter.type!!.java) as TypeAdapter<Message<*, *>>

    val jsonObject = delegate.toJsonTree(value.unpack(protoAdapter) as Message<*, *>).asJsonObject
    for ((name: String, element: JsonElement) in jsonObject.entrySet()) {
      writer.name(name)
      elementAdapter.write(writer, element)
    }
    writer.endObject()
  }

  @Throws(IOException::class)
  override fun read(reader: JsonReader): AnyMessage? {
    if (reader.peek() == JsonToken.NULL) {
      reader.nextNull()
      return null
    }

    val jsonElement = elementAdapter.read(reader)
    val typeUrlEntry = jsonElement.asJsonObject.get("@type")
      ?: throw IOException("expected @type in ${reader.path}")
    val typeUrl = typeUrlEntry.asString

    val protoAdapter = typeUrlToAdapter[typeUrl]
      ?: throw IOException("Cannot resolve type: $typeUrl in ${reader.path}")

    @Suppress("UNCHECKED_CAST")
    val delegate = gson.getAdapter(protoAdapter.type!!.java) as TypeAdapter<Message<*, *>>

    val value = delegate.fromJsonTree(jsonElement)

    return AnyMessage.pack(value!!)
  }
}
