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
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

/**
 * Json adapter for structs, list values, values, or null values as defined in
 * `google/protobuf/struct.proto`.
 */
internal object StructJsonAdapter : JsonAdapter<Any>() {
  override fun toJson(out: JsonWriter, any: Any?) {
    when (any) {
      null -> out.nullValue()
      is Double -> out.value(any)
      is String -> out.value(any)
      is Boolean -> out.value(any)
      is Map<*, *> -> {
        out.beginObject()
        for ((key, value) in any) {
          out.name(key as String)
          toJson(out, value)
        }
        out.endObject()
      }
      is List<*> -> {
        out.beginArray()
        for (value in any) {
          toJson(out, value)
        }
        out.endArray()
      }
      else -> throw IllegalArgumentException("unexpected struct value: $any")
    }
  }

  override fun fromJson(input: JsonReader): Any? {
    return input.readJsonValue()
  }
}
