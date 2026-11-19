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
import java.io.StringWriter

/** The JSON readers of Gson and Moshi reject documents nested more than 255 levels deep. */
private const val MAX_WRITE_DEPTH = 254

/** The number of JSON levels open inside [AnyMessage] values being written by the current thread. */
private val writeDepth: ThreadLocal<Int> = ThreadLocal.withInitial { 0 }

/**
 * Writes the JSON of the message inside an [AnyMessage], and counts the levels that it opens. The
 * first object is not counted, because its fields are written into the object of the [AnyMessage].
 */
private class DepthLimitedWriter(out: StringWriter) : JsonWriter(out) {
  private var open = 0

  override fun beginArray(): JsonWriter {
    enter()
    return super.beginArray()
  }

  override fun beginObject(): JsonWriter {
    enter()
    return super.beginObject()
  }

  override fun endArray(): JsonWriter {
    val result = super.endArray()
    leave()
    return result
  }

  override fun endObject(): JsonWriter {
    val result = super.endObject()
    leave()
    return result
  }

  private fun enter() {
    if (open > 0) {
      val depth = writeDepth.get() + 1
      if (depth > MAX_WRITE_DEPTH) {
        throw IOException("JSON inside Any messages is nested more than $MAX_WRITE_DEPTH levels deep")
      }
      writeDepth.set(depth)
    }
    open++
  }

  private fun leave() {
    open--
    if (open > 0) writeDepth.set(writeDepth.get() - 1)
  }
}

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

    // Gson's writer has no nesting limit, so deep nesting inside Any values could overflow the stack.
    val depth = writeDepth.get()
    // An Any that is not inside another Any writes its own object to a writer that does not count it.
    if (depth == 0) writeDepth.set(1)
    try {
      writeAny(writer, value)
    } finally {
      writeDepth.set(depth)
    }
  }

  private fun writeAny(writer: JsonWriter, value: AnyMessage) {
    writer.beginObject()

    writer.name("@type")
    writer.value(value.typeUrl)

    val protoAdapter = typeUrlToAdapter[value.typeUrl]
      ?: throw IOException("Cannot find type for url: ${value.typeUrl}")

    if (protoAdapter == ProtoAdapter.FIELD_MASK) {
      writer.name("value")
      gson.getAdapter(FieldMask::class.java).write(writer, value.unpack(ProtoAdapter.FIELD_MASK))
      writer.endObject()
      return
    }

    @Suppress("UNCHECKED_CAST")
    val delegate = gson.getAdapter(protoAdapter.type!!.java) as TypeAdapter<Message<*, *>>

    val out = StringWriter()
    delegate.write(DepthLimitedWriter(out), value.unpack(protoAdapter) as Message<*, *>)
    val jsonObject = elementAdapter.fromJson(out.toString()).asJsonObject
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
    val typeUrl = try {
      typeUrlEntry.asString
    } catch (_: RuntimeException) {
      throw IOException("expected @type to be a string in ${reader.path}")
    }

    val protoAdapter = typeUrlToAdapter[typeUrl]
      ?: throw IOException("Cannot resolve type: $typeUrl in ${reader.path}")

    if (protoAdapter == ProtoAdapter.FIELD_MASK) {
      val fieldMask = jsonElement.asJsonObject.get("value")?.let {
        gson.getAdapter(FieldMask::class.java).fromJsonTree(it)
      } ?: FieldMask()
      return AnyMessage(typeUrl, ProtoAdapter.FIELD_MASK.encodeByteString(fieldMask))
    }

    @Suppress("UNCHECKED_CAST")
    val delegate = gson.getAdapter(protoAdapter.type!!.java) as TypeAdapter<Message<*, *>>

    val value = delegate.fromJsonTree(jsonElement)

    return AnyMessage.pack(value!!)
  }
}
