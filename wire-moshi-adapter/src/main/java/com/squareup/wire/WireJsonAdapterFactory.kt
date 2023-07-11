/*
 * Copyright (C) 2018 Square, Inc.
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

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.wire.internal.EnumJsonFormatter
import com.squareup.wire.internal.createRuntimeMessageAdapter
import java.lang.reflect.Type

/**
 * A [JsonAdapter.Factory] that allows Wire messages to be serialized and deserialized using the
 * Moshi Json library.
 *
 * ```
 * Moshi moshi = new Moshi.Builder()
 *     .add(new WireJsonAdapterFactory())
 *     .build();
 * ```
 *
 * The resulting [Moshi] instance will be able to serialize and deserialize Wire [Message] types,
 * including extensions. It ignores unknown field values. The JSON encoding is intended to be
 * compatible with the [protobuf-java-format](https://code.google.com/p/protobuf-java-format/)
 * library.
 *
 * In Proto3, if a field is set to its default (or identity) value, it will be omitted in the
 * JSON-encoded data. Set [writeIdentityValues] to true if you want Wire to always write values,
 * including default ones.
 */
class WireJsonAdapterFactory @JvmOverloads constructor(
  private val typeUrlToAdapter: Map<String, ProtoAdapter<*>> = mapOf(),
  private val writeIdentityValues: Boolean = false,
) : JsonAdapter.Factory {
  /**
   * Returns a new WireJsonAdapterFactory that can encode the messages for [adapters] if they're
   * used with [AnyMessage].
   */
  fun plus(adapters: List<ProtoAdapter<*>>): WireJsonAdapterFactory {
    val newMap = typeUrlToAdapter.toMutableMap()
    for (adapter in adapters) {
      val key = adapter.typeUrl ?: throw IllegalArgumentException(
        "recompile ${adapter.type} to use it with WireJsonAdapterFactory",
      )
      newMap[key] = adapter
    }
    return WireJsonAdapterFactory(newMap, writeIdentityValues)
  }

  /**
   * Returns a new WireJsonAdapterFactory that can encode the messages for [adapter] if they're
   * used with [AnyMessage].
   */
  fun plus(adapter: ProtoAdapter<*>): WireJsonAdapterFactory {
    return plus(listOf(adapter))
  }

  @Suppress("UNCHECKED_CAST")
  override fun create(
    type: Type,
    annotations: Set<Annotation>,
    moshi: Moshi,
  ): JsonAdapter<*>? {
    val rawType = Types.getRawType(type)

    return when {
      annotations.isNotEmpty() -> null
      rawType == AnyMessage::class.java -> AnyMessageJsonAdapter(moshi, typeUrlToAdapter)
      Message::class.java.isAssignableFrom(rawType) -> {
        val messageAdapter = createRuntimeMessageAdapter<Nothing, Nothing>(
          type as Class<Nothing>,
          writeIdentityValues,
          rawType.classLoader,
        )
        val jsonAdapters = MoshiJsonIntegration.jsonAdapters(messageAdapter, moshi)
        val redactedFieldsAdapter = moshi.adapter<List<String>>(
          Types.newParameterizedType(List::class.java, String::class.java),
        )
        MessageJsonAdapter(messageAdapter, jsonAdapters, redactedFieldsAdapter).nullSafe()
      }
      WireEnum::class.java.isAssignableFrom(rawType) -> {
        val enumAdapter = RuntimeEnumAdapter.create(type as Class<Nothing>)
        val enumJsonFormatter = EnumJsonFormatter(enumAdapter)
        EnumJsonAdapter(enumJsonFormatter).nullSafe()
      }
      else -> null
    }
  }
}
