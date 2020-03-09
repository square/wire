/*
 * Copyright 2020 Square Inc.
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
import java.io.IOException

internal class AnyMessageJsonAdapter(
  private val moshi: Moshi,
  private val typeUrlToAdapter: Map<String, ProtoAdapter<*>>
) : JsonAdapter<AnyMessage>() {

  @Throws(IOException::class)
  override fun toJson(writer: JsonWriter, value: AnyMessage?) {
    if (value == null) {
      writer.nullValue()
      return
    }
    writer.beginObject()

    writer.name("@type")
    writer.value(value.typeUrl)

    val protoAdapter = typeUrlToAdapter[value.typeUrl]
        ?: throw JsonDataException("Cannot find type for url: ${value.typeUrl} in ${writer.path}")

    val delegate = moshi.adapter(protoAdapter.type!!.java) as JsonAdapter<Message<*, *>>

    val flattenToken = writer.beginFlatten()
    delegate.toJson(writer, value.unpack(protoAdapter) as Message<*, *>)
    writer.endFlatten(flattenToken)
    writer.endObject()
  }

  @Throws(IOException::class)
  override fun fromJson(reader: JsonReader): AnyMessage? {
    if (reader.peek() == JsonReader.Token.NULL) {
      reader.nextNull<Any>()
      return null
    }

    val typeUrl = reader.peekJson().use { it.readStringNamed("@type") }
        ?: throw JsonDataException("expected @type in ${reader.path}")

    val protoAdapter = typeUrlToAdapter[typeUrl]
        ?: throw JsonDataException("Cannot resolve type: $typeUrl in ${reader.path}")

    val delegate = moshi.adapter(protoAdapter.type!!.java) as JsonAdapter<Message<*, *>>

    val value = delegate.fromJson(reader)

    return AnyMessage.pack(value!!)
  }

  /** Returns the string named [name] of an object, or null if no such property exists. */
  private fun JsonReader.readStringNamed(name: String): String? {
    beginObject()
    while (hasNext()) {
      if (nextName() == name) {
        return nextString()
      } else {
        skipValue()
      }
    }
    return null // No such field on 'reader'.
  }
}
