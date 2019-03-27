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
    return when {
      type.rawType == ByteString::class.java -> ByteStringTypeAdapter() as TypeAdapter<T>
      Message::class.java.isAssignableFrom(type.rawType) ->
        MessageTypeAdapter<Nothing, Nothing>(gson, type as TypeToken<Nothing>) as TypeAdapter<T>
      else -> null
    }
  }
}
