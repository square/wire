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
import com.google.gson.JsonSyntaxException
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.squareup.wire.internal.JsonFormatter
import com.squareup.wire.internal.JsonIntegration
import java.lang.reflect.Type

internal object GsonJsonIntegration : JsonIntegration<Gson, TypeAdapter<Any?>>() {
  override fun frameworkAdapter(
    framework: Gson,
    type: Type,
  ): TypeAdapter<Any?> {
    @Suppress("UNCHECKED_CAST")
    return framework.getAdapter(TypeToken.get(type)).nullSafe() as TypeAdapter<Any?>
  }

  override fun listAdapter(elementAdapter: TypeAdapter<Any?>): TypeAdapter<Any?> {
    @Suppress("UNCHECKED_CAST")
    return ListJsonAdapter(elementAdapter).nullSafe() as TypeAdapter<Any?>
  }

  override fun mapAdapter(
    framework: Gson,
    keyFormatter: JsonFormatter<*>,
    valueAdapter: TypeAdapter<Any?>,
  ): TypeAdapter<Any?> {
    @Suppress("UNCHECKED_CAST")
    return MapJsonAdapter(keyFormatter, valueAdapter).nullSafe() as TypeAdapter<Any?>
  }

  fun <T> TypeAdapter<T>.serializeNulls(): TypeAdapter<T> {
    val delegate = this

    return object : TypeAdapter<T>() {
      override fun write(writer: JsonWriter, value: T) {
        val oldSerializeNulls = writer.serializeNulls
        writer.serializeNulls = true
        try {
          delegate.write(writer, value)
        } finally {
          writer.serializeNulls = oldSerializeNulls
        }
      }

      override fun read(reader: JsonReader): T {
        return delegate.read(reader)
      }
    }
  }

  override fun structAdapter(framework: Gson): TypeAdapter<Any?> {
    @Suppress("UNCHECKED_CAST")
    return framework.getAdapter(Object::class.java).serializeNulls().nullSafe() as TypeAdapter<Any?>
  }

  override fun formatterAdapter(jsonStringAdapter: JsonFormatter<*>): TypeAdapter<Any?> {
    @Suppress("UNCHECKED_CAST")
    return FormatterJsonAdapter(jsonStringAdapter).nullSafe() as TypeAdapter<Any?>
  }

  private class FormatterJsonAdapter<T : Any>(
    private val formatter: JsonFormatter<T>,
  ) : TypeAdapter<T>() {
    override fun write(writer: JsonWriter, value: T) {
      val stringOrNumber = formatter.toStringOrNumber(value)
      if (stringOrNumber is Number) {
        writer.value(stringOrNumber)
      } else {
        writer.value(stringOrNumber as String)
      }
    }

    override fun read(reader: JsonReader): T? {
      val string = reader.nextString()
      try {
        return formatter.fromString(string)
      } catch (_: RuntimeException) {
        throw JsonSyntaxException("decode failed: $string at path ${reader.path}")
      }
    }
  }

  /** Adapt a list of values by delegating to an adapter for a single value. */
  private class ListJsonAdapter<T>(
    private val single: TypeAdapter<T>,
  ) : TypeAdapter<List<T?>>() {
    override fun read(reader: JsonReader): List<T?> {
      val result = mutableListOf<T?>()
      reader.beginArray()
      while (reader.hasNext()) {
        result.add(single.read(reader))
      }
      reader.endArray()
      return result
    }

    override fun write(writer: JsonWriter, value: List<T?>?) {
      writer.beginArray()
      for (v in value!!) {
        single.write(writer, v)
      }
      writer.endArray()
    }
  }

  /** Adapt a list of values by delegating to an adapter for a single value. */
  private class MapJsonAdapter<K : Any, V>(
    private val keyFormatter: JsonFormatter<K>,
    private val valueAdapter: TypeAdapter<V>,
  ) : TypeAdapter<Map<K, V>>() {
    override fun read(reader: JsonReader): Map<K, V> {
      val result = mutableMapOf<K, V>()
      reader.beginObject()
      while (reader.hasNext()) {
        val name = reader.nextName()
        val key = keyFormatter.fromString(name) as K
        val value = valueAdapter.read(reader)!!
        result[key] = value
      }
      reader.endObject()
      return result
    }

    override fun write(writer: JsonWriter, value: Map<K, V>?) {
      writer.beginObject()
      for ((k, v) in value!!) {
        writer.name(keyFormatter.toStringOrNumber(k).toString())
        valueAdapter.write(writer, v)
      }
      writer.endObject()
    }
  }
}
