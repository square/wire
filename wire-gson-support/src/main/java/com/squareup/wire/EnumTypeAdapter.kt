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

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.squareup.wire.internal.EnumJsonFormatter
import java.io.IOException

internal class EnumTypeAdapter<E>(
  private val enumJsonFormatter: EnumJsonFormatter<E>,
) : TypeAdapter<E>() where E : Enum<E>, E : WireEnum {

  @Throws(IOException::class)
  override fun write(out: JsonWriter, value: E) {
    val formatted = enumJsonFormatter.toStringOrNumber(value)
    when {
      formatted is Number -> out.value(formatted)
      else -> out.value(formatted.toString())
    }
  }

  @Throws(IOException::class)
  override fun read(input: JsonReader): E {
    val path: String = input.path
    val nextString = input.nextString()

    return enumJsonFormatter.fromString(nextString)
      ?: throw IOException("Unexpected $nextString at path $path")
  }
}
