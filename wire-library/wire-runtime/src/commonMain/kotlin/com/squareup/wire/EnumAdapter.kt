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

import com.squareup.wire.internal.Throws
import okio.IOException
import kotlin.reflect.KClass

/**
 * An abstract [ProtoAdapter] that converts values of an enum to and from integers.
 */
expect abstract class EnumAdapter<E : WireEnum> protected constructor(
  type: KClass<E>
) : ProtoAdapter<E> {
  override fun encodedSize(value: E): Int

  @Throws(IOException::class)
  override fun encode(writer: ProtoWriter, value: E)

  @Throws(IOException::class)
  override fun decode(reader: ProtoReader): E

  override fun redact(value: E): E

  /**
   * Converts an integer to an enum.
   * Returns null if there is no corresponding enum.
   */
  protected abstract fun fromValue(value: Int): E?
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E : WireEnum> commonEncodedSize(value: E): Int {
  return ProtoWriter.varint32Size(value.value)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E : WireEnum> commonEncode(writer: ProtoWriter, value: E) {
  writer.writeVarint32(value.value)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E : WireEnum> EnumAdapter<E>.commonDecode(
  reader: ProtoReader,
  fromValue: (Int) -> E?
): E {
  val value = reader.readVarint32()
  return fromValue(value) ?: throw ProtoAdapter.EnumConstantNotFoundException(value, type)
}

@Suppress("NOTHING_TO_INLINE", "UNUSED_PARAMETER")
internal inline fun <E : WireEnum> commonRedact(value: E): E {
  throw UnsupportedOperationException()
}
