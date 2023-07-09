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

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

fun <T> JsonAdapter<T>.redacting(): JsonAdapter<T> {
  val delegate = this
  return object : JsonAdapter<T>() {
    override fun fromJson(reader: JsonReader): T? {
      return delegate.fromJson(reader)
    }

    override fun toJson(writer: JsonWriter, value: T?) {
      var redactedTag = writer.tag(RedactedTag::class.java)
      if (redactedTag == null) {
        redactedTag = RedactedTag()
        writer.setTag(RedactedTag::class.java, redactedTag)
      }
      val wasRedacted = redactedTag.enabled
      redactedTag.enabled = true
      try {
        delegate.toJson(writer, value)
      } finally {
        redactedTag.enabled = wasRedacted
      }
    }
  }
}

internal class RedactedTag {
  var enabled = false
}
