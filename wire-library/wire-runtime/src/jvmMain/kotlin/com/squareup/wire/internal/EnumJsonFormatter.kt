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
package com.squareup.wire.internal

import com.squareup.wire.EnumAdapter
import com.squareup.wire.WireEnum

/** Encodes enums using their names. Decodes using either their names or their tags. */
class EnumJsonFormatter<E : WireEnum>(adapter: EnumAdapter<E>) : JsonFormatter<E> {
  private val stringToValue = run {
    val map = mutableMapOf<String, E>()
    // E is a subtype of Enum<*>, but we don't know that statically.
    for (t in adapter.type!!.java.enumConstants) {
      map[(t as Enum<*>).name] = t as E
      map[t.value.toString()] = t as E
    }
    return@run map
  }

  override fun fromString(value: String): E? = stringToValue[value]

  override fun toStringOrNumber(value: E): String = (value as Enum<*>).name
}
