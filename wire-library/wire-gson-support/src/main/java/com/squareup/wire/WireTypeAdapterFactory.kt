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
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.squareup.wire.internal.RuntimeMessageAdapter
import okio.ByteString

/**
 * A [TypeAdapterFactory] that allows Wire messages to be serialized and deserialized
 * using the GSON Json library. To create a [Gson] instance that works with Wire,
 * use the [com.google.gson.GsonBuilder] interface:
 *
 * ```
 * Gson gson = new GsonBuilder()
 *     .registerTypeAdapterFactory(new WireTypeAdapterFactory())
 *     .create();
 * ```
 *
 * The resulting [Gson] instance will be able to serialize and deserialize any Wire
 * [Message] type, including extensions and unknown field values. The JSON encoding is
 * intended to be compatible with the
 * [protobuf-java-format](https://code.google.com/p/protobuf-java-format/)
 * library. Note that version 1.2 of that API has a
 * [bug](https://code.google.com/p/protobuf-java-format/issues/detail?id=47)
 * in the way it serializes unknown fields, so we use our own approach for this case.
 */
class WireTypeAdapterFactory(
  private val typeUrlToAdapter: Map<String, ProtoAdapter<*>>
) : TypeAdapterFactory {
  constructor() : this(mapOf())

  /**
   * Returns a new WireJsonAdapterFactory that can encode the messages for [adapters] if they're
   * used with [AnyMessage].
   */
  fun plus(adapters: List<ProtoAdapter<*>>): WireTypeAdapterFactory {
    val newMap = typeUrlToAdapter.toMutableMap()
    for (adapter in adapters) {
      val key = adapter.typeUrl ?: throw IllegalArgumentException(
          "recompile ${adapter.type} to use it with WireTypeAdapterFactory")
      newMap[key] = adapter
    }
    return WireTypeAdapterFactory(newMap)
  }

  /**
   * Returns a new WireTypeAdapterFactory that can encode the messages for [adapter] if they're
   * used with [AnyMessage].
   */
  fun plus(adapter: ProtoAdapter<*>): WireTypeAdapterFactory {
    return plus(listOf(adapter))
  }

  override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
    val rawType = type.rawType

    return when {
      rawType == AnyMessage::class.java -> AnyMessageTypeAdapter(gson, typeUrlToAdapter) as TypeAdapter<T>
      Message::class.java.isAssignableFrom(rawType) -> {
        val messageAdapter = RuntimeMessageAdapter.create<Nothing, Nothing>(rawType as Class<Nothing>)
        val jsonAdapters = messageAdapter.jsonAdapters(GsonJsonIntegration, gson)
        MessageTypeAdapter(messageAdapter, jsonAdapters).nullSafe() as TypeAdapter<T>
      }
      else -> null
    }
  }
}
