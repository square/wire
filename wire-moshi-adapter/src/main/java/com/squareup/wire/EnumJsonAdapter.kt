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
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.wire.internal.EnumJsonFormatter
import java.io.IOException

internal class EnumJsonAdapter<E>(
  private val enumJsonFormatter: EnumJsonFormatter<E>,
) : JsonAdapter<E>() where E : Enum<E>, E : WireEnum {

  @Throws(IOException::class)
  override fun toJson(out: JsonWriter, value: E?) {
    if (value == null) {
      out.nullValue()
    } else {
      val formatted = enumJsonFormatter.toStringOrNumber(value)
      when {
        formatted is Number -> out.value(formatted)
        else -> out.value(formatted.toString())
      }
    }
  }

  @Throws(IOException::class)
  override fun fromJson(input: JsonReader): E {
    val nextString = input.nextString()

    return enumJsonFormatter.fromString(nextString)
      ?: throw JsonDataException("Unexpected $nextString at path ${input.path}")
  }
}
