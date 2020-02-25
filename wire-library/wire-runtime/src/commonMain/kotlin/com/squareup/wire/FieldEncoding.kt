/*
 * Copyright 2013 Square Inc.
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

import com.squareup.wire.internal.ProtocolException
import com.squareup.wire.internal.Throws
import okio.IOException
import kotlin.jvm.JvmStatic

enum class FieldEncoding(internal val value: Int) {
  VARINT(0), FIXED64(1), LENGTH_DELIMITED(2), FIXED32(5);

  /**
   * Returns a Wire adapter that reads this field encoding without interpretation. For example,
   * messages are returned as byte strings and enums are returned as integers.
   */
  fun rawProtoAdapter(): ProtoAdapter<*> = when (this) {
    VARINT -> ProtoAdapter.UINT64
    FIXED32 -> ProtoAdapter.FIXED32
    FIXED64 -> ProtoAdapter.FIXED64
    LENGTH_DELIMITED -> ProtoAdapter.BYTES
  }

  companion object {
    @JvmStatic
    @Throws(IOException::class)
    internal operator fun get(value: Int): FieldEncoding = when (value) {
      0 -> VARINT
      1 -> FIXED64
      2 -> LENGTH_DELIMITED
      5 -> FIXED32
      else -> throw ProtocolException("Unexpected FieldEncoding: $value")
    }
  }
}
