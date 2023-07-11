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

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.squareup.wire.internal.EnumJsonFormatter
import com.squareup.wire.internal.createRuntimeMessageAdapter

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
 *
 * In Proto3, if a field is set to its default (or identity) value, it will be omitted in the
 * JSON-encoded data. Set [writeIdentityValues] to true if you want Wire to always write values,
 * including default ones.
 */
class WireTypeAdapterFactory @JvmOverloads constructor(
  private val typeUrlToAdapter: Map<String, ProtoAdapter<*>> = mapOf(),
  private val writeIdentityValues: Boolean = false,
) : TypeAdapterFactory {
  /**
   * Returns a new WireJsonAdapterFactory that can encode the messages for [adapters] if they're
   * used with [AnyMessage].
   */
  fun plus(adapters: List<ProtoAdapter<*>>): WireTypeAdapterFactory {
    val newMap = typeUrlToAdapter.toMutableMap()
    for (adapter in adapters) {
      val key = adapter.typeUrl ?: throw IllegalArgumentException(
        "recompile ${adapter.type} to use it with WireTypeAdapterFactory",
      )
      newMap[key] = adapter
    }
    return WireTypeAdapterFactory(newMap, writeIdentityValues)
  }

  /**
   * Returns a new WireTypeAdapterFactory that can encode the messages for [adapter] if they're
   * used with [AnyMessage].
   */
  fun plus(adapter: ProtoAdapter<*>): WireTypeAdapterFactory {
    return plus(listOf(adapter))
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
    val rawType = type.rawType

    return when {
      rawType == AnyMessage::class.java -> AnyMessageTypeAdapter(gson, typeUrlToAdapter) as TypeAdapter<T>
      Message::class.java.isAssignableFrom(rawType) -> {
        val messageAdapter = createRuntimeMessageAdapter<Nothing, Nothing>(
          rawType as Class<Nothing>,
          writeIdentityValues,
          rawType.classLoader,
        )
        val jsonAdapters = GsonJsonIntegration.jsonAdapters(messageAdapter, gson)
        MessageTypeAdapter(messageAdapter, jsonAdapters).nullSafe() as TypeAdapter<T>
      }
      WireEnum::class.java.isAssignableFrom(rawType) -> {
        val enumAdapter = RuntimeEnumAdapter.create(rawType as Class<Nothing>)
        val enumJsonFormatter = EnumJsonFormatter(enumAdapter)
        EnumTypeAdapter(enumJsonFormatter).nullSafe() as TypeAdapter<T>
      }
      else -> null
    }
  }
}
