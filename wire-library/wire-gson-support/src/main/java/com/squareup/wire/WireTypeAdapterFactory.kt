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
import okio.ByteString
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

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
class WireTypeAdapterFactory : TypeAdapterFactory {
  override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
    val rawType = type.rawType
    return when {
      rawType == ByteString::class.java -> ByteStringTypeAdapter() as TypeAdapter<T>
      Message::class.java.isAssignableFrom(rawType) ->
        MessageTypeAdapter<Nothing, Nothing>(gson, type as TypeToken<Nothing>) as TypeAdapter<T>
      rawType == Duration::class.java -> DurationTypeAdapter as TypeAdapter<T>
      rawType == Any::class.java -> StructTypeAdapter as TypeAdapter<T>
      rawType == Unit::class.java -> StructTypeAdapter as TypeAdapter<T>
      type.type.isMapStringStar() -> StructTypeAdapter as TypeAdapter<T>
      type.type.isListStar() -> StructTypeAdapter as TypeAdapter<T>
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
