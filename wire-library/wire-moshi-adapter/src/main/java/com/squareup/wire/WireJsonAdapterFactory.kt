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
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

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
 */
class WireJsonAdapterFactory private constructor(
  private val typeUrlToAdapter: Map<String, ProtoAdapter<*>>
) : JsonAdapter.Factory {
  constructor() : this(mapOf())

  /**
   * Returns a new WireJsonAdapterFactory that can encode the messages for [adapters] if they're
   * used with [AnyMessage].
   */
  fun plus(adapters: List<ProtoAdapter<*>>): WireJsonAdapterFactory {
    val newMap = typeUrlToAdapter.toMutableMap()
    for (adapter in adapters) {
      val key = adapter.typeUrl ?: throw IllegalArgumentException(
          "recompile ${adapter.type} to use it with WireJsonAdapterFactory")
      newMap[key] = adapter
    }
    return WireJsonAdapterFactory(newMap)
  }

  /**
   * Returns a new WireJsonAdapterFactory that can encode the messages for [adapter] if they're
   * used with [AnyMessage].
   */
  fun plus(adapter: ProtoAdapter<*>): WireJsonAdapterFactory {
    return plus(listOf(adapter))
  }

  override fun create(
    type: Type,
    annotations: Set<Annotation>,
    moshi: Moshi
  ): JsonAdapter<*>? {
    val rawType = Types.getRawType(type)

    return when {
      annotations.isNotEmpty() -> null
      rawType == AnyMessage::class.java -> AnyMessageJsonAdapter(moshi, typeUrlToAdapter)
      rawType == Duration::class.java -> DurationJsonAdapter.nullSafe()
      rawType == Instant::class.java -> InstantJsonAdapter.nullSafe()
      rawType == Any::class.java -> StructJsonAdapter.serializeNulls()
      rawType == Unit::class.java -> StructJsonAdapter.serializeNulls()
      type.isMapStringStar() -> StructJsonAdapter.serializeNulls()
      type.isListStar() -> StructJsonAdapter.serializeNulls()
      Message::class.java.isAssignableFrom(rawType) -> {
        MessageJsonAdapter<Nothing, Nothing>(moshi, type)
      }
      else -> null
    }
  }

  companion object {
    /** Returns true if [this] is a `Map<String, *>`. */
    private fun Type.isMapStringStar(): Boolean {
      if (this !is ParameterizedType) return false
      if (rawType != Map::class.java) return false

      val keyType = actualTypeArguments[0]
      val valueType = actualTypeArguments[1]
      if (keyType != String::class.java) return false

      if (valueType !is WildcardType) return false
      if (valueType.lowerBounds.isNotEmpty()) return false
      if (valueType.upperBounds != Object::class.java) return false

      return true
    }

    /** Returns true if [this] is a `List<*>`. */
    private fun Type.isListStar(): Boolean {
      if (this !is ParameterizedType) return false
      if (rawType != List::class.java) return false

      val valueType = actualTypeArguments[0]
      if (valueType !is WildcardType) return false
      if (valueType.lowerBounds.isNotEmpty()) return false
      if (valueType.upperBounds != Object::class.java) return false

      return true
    }
  }
}
