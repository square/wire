/*
 * Copyright 2016 Square Inc.
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

import kotlin.reflect.KClass

/**
 * An abstract [ProtoAdapter] that converts values of an enum to and from integers.
 */
actual abstract class EnumAdapter<E : WireEnum> protected actual constructor(
  type: KClass<E>
) : ProtoAdapter<E>(FieldEncoding.VARINT, type, null) {
  actual override fun encodedSize(value: E): Int = commonEncodedSize(value)

  actual override fun encode(writer: ProtoWriter, value: E) {
    commonEncode(writer, value)
  }

  actual override fun decode(reader: ProtoReader): E = commonDecode(reader, this::fromValue)

  actual override fun redact(value: E): E = commonRedact(value)

  /**
   * Converts an integer to an enum.
   * Returns null if there is no corresponding enum.
   */
  protected actual abstract fun fromValue(value: Int): E?
}
