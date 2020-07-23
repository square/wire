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
import com.squareup.moshi.Types
import com.squareup.wire.internal.FieldBinding.JsonFormatter
import com.squareup.wire.internal.JsonIntegration
import java.lang.reflect.Type

internal object MoshiJsonIntegration : JsonIntegration<Moshi, JsonAdapter<Any?>>() {
  override fun frameworkAdapter(
    framework: Moshi,
    type: Type
  ): JsonAdapter<Any?> = framework.adapter<Any?>(type).nullSafe()

  override fun listAdapter(elementAdapter: JsonAdapter<Any?>): JsonAdapter<Any?> =
    ListJsonAdapter(elementAdapter).nullSafe() as JsonAdapter<Any?>

  override fun mapAdapter(
    framework: Moshi,
    keyType: Type,
    valueType: Type
  ): JsonAdapter<Any?> {
    val mapType = Types.newParameterizedType(Map::class.java, keyType, valueType)
    return framework.adapter(mapType)
  }

  override fun structAdapter(): JsonAdapter<Any?> =
    StructJsonAdapter.serializeNulls() as JsonAdapter<Any?>

  override fun formatterAdapter(jsonFormatter: JsonFormatter<*>): JsonAdapter<Any?> =
    FormatterJsonAdapter(jsonFormatter).nullSafe() as JsonAdapter<Any?>

  private class FormatterJsonAdapter<T : Any>(
    private val formatter: JsonFormatter<T>
  ) : JsonAdapter<T>() {
    override fun toJson(
      writer: JsonWriter,
      value: T?
    ) {
      val stringOrNumber = formatter.toStringOrNumber(value!!)
      if (stringOrNumber is Number) {
        writer.value(stringOrNumber)
      } else {
        writer.value(stringOrNumber as String)
      }
    }

    override fun fromJson(reader: JsonReader): T? {
      val string = reader.nextString()
      try {
        return formatter.fromString(string)
      } catch (_: RuntimeException) {
        throw JsonDataException("decode failed: $string at path ${reader.path}")
      }
    }
  }

  /** Adapt a list of values by delegating to an adapter for a single value. */
  private class ListJsonAdapter<T>(
    private val single: JsonAdapter<T>
  ) : JsonAdapter<List<T?>>() {
    override fun fromJson(reader: JsonReader): List<T?> {
      val result = mutableListOf<T?>()
      reader.beginArray()
      while (reader.hasNext()) {
        result.add(single.fromJson(reader))
      }
      reader.endArray()
      return result
    }

    override fun toJson(
      writer: JsonWriter,
      value: List<T?>?
    ) {
      writer.beginArray()
      for (v in value!!) {
        single.toJson(writer, v)
      }
      writer.endArray()
    }
  }
}
