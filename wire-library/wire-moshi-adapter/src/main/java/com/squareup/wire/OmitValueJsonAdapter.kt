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
import java.io.IOException

private class OmitValueJsonAdapter<T : Any>(
  private val delegate: JsonAdapter<T>,
  private val omittedValue: T
) : JsonAdapter<T>() {
  override fun fromJson(reader: JsonReader): T? {
    return delegate.fromJson(reader)
  }

  @Throws(IOException::class)
  override fun toJson(writer: JsonWriter, value: T?) {
    if (value == omittedValue) {
      writer.nullValue()
    } else {
      delegate.toJson(writer, value)
    }
  }

  override fun toString(): String {
    return "$delegate.omitValue($omittedValue)"
  }
}

/**
 * Returns a JSON adapter equal to this JSON adapter, except this will not emit if the value is
 * equal to [omittedValue].
 */
internal fun <T : Any> JsonAdapter<T>.omitValue(omittedValue: T): JsonAdapter<T> {
  return if (this is OmitValueJsonAdapter<*>) {
    throw IllegalArgumentException("JsonAdapter $this is already of type OmitValueJsonAdapter")
  } else {
    OmitValueJsonAdapter(this, omittedValue)
  }
}
