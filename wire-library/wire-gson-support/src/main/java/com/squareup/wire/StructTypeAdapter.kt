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

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.lang.RuntimeException

/**
 * Type adapter for structs, list values, values, or null values as defined in
 * `google/protobuf/struct.proto`.
 */
internal object StructTypeAdapter : TypeAdapter<Any>() {
  override fun write(out: JsonWriter, any: Any?) {
    val serializeNulls = out.serializeNulls
    out.serializeNulls = true
    when (any) {
      null -> out.nullValue()
      is Double -> out.value(any)
      is String -> out.value(any)
      is Boolean -> out.value(any)
      is Map<*, *> -> {
        out.beginObject()
        for ((key, value) in any) {
          out.name(key as String)
          write(out, value)
        }
        out.endObject()
      }
      is List<*> -> {
        out.beginArray()
        for (value in any) {
          write(out, value)
        }
        out.endArray()
      }
      else -> throw IllegalArgumentException("unexpected struct value: $any")
    }
    out.serializeNulls = serializeNulls
  }

  override fun read(input: JsonReader): Any {
    // TODO(benoit) Ask for help. We kind of don't need it.
    throw RuntimeException("No explicit support for reading Struct.")
  }
}
