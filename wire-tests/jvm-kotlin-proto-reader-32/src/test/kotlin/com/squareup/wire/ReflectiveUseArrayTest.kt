/*
 * Copyright (C) 2024 Square, Inc.
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

import com.squareup.wire.protos.kotlin.use_array_redacted.UseArrayRedacted
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import okio.Buffer
import okio.ByteString.Companion.decodeHex
import squareup.proto2.UseArrayProto2
import squareup.proto3.UseArray

/**
 * Fields that use primitive arrays work with the adapter that [ProtoAdapter.newMessageAdapter]
 * creates for generated code, the same as with the generated adapter.
 */
class ReflectiveUseArrayTest {
  private val proto3Reflective = ProtoAdapter.newMessageAdapter(UseArray::class.java)
  private val proto2Reflective = ProtoAdapter.newMessageAdapter(UseArrayProto2::class.java)

  private val proto3Message = UseArray(
    array_int32 = intArrayOf(1, -2, Int.MAX_VALUE),
    array_uint32 = intArrayOf(1, -1),
    array_sint32 = intArrayOf(-3, 3),
    array_fixed32 = intArrayOf(1, -1),
    array_sfixed32 = intArrayOf(-4, 4),
    array_int64 = longArrayOf(1L, -2L, Long.MAX_VALUE),
    array_uint64 = longArrayOf(1L, -1L),
    array_sint64 = longArrayOf(-5L, 5L),
    array_fixed64 = longArrayOf(1L, -1L),
    array_sfixed64 = longArrayOf(-6L, 6L),
    array_float = floatArrayOf(1.5f, -2.25f),
    array_double = doubleArrayOf(0.5, -1.0e10),
  )

  private val proto2Message = UseArrayProto2(
    array_int32 = proto3Message.array_int32,
    array_uint32 = proto3Message.array_uint32,
    array_sint32 = proto3Message.array_sint32,
    array_fixed32 = proto3Message.array_fixed32,
    array_sfixed32 = proto3Message.array_sfixed32,
    array_int64 = proto3Message.array_int64,
    array_uint64 = proto3Message.array_uint64,
    array_sint64 = proto3Message.array_sint64,
    array_fixed64 = proto3Message.array_fixed64,
    array_sfixed64 = proto3Message.array_sfixed64,
    array_float = proto3Message.array_float,
    array_double = proto3Message.array_double,
  )

  @Test fun encodeMatchesGeneratedAdapter() {
    assertEquals(UseArray.ADAPTER.encodeByteString(proto3Message), proto3Reflective.encodeByteString(proto3Message))
    assertEquals(UseArrayProto2.ADAPTER.encodeByteString(proto2Message), proto2Reflective.encodeByteString(proto2Message))
    assertEquals(UseArray.ADAPTER.encodedSize(proto3Message), proto3Reflective.encodedSize(proto3Message))
    assertEquals(UseArrayProto2.ADAPTER.encodedSize(proto2Message), proto2Reflective.encodedSize(proto2Message))
  }

  @Test fun decodeMatchesGeneratedAdapter() {
    val proto3Bytes = UseArray.ADAPTER.encodeByteString(proto3Message)
    val decoded3 = proto3Reflective.decode(proto3Bytes)
    assertEquals(proto3Message, decoded3)
    assertEquals(UseArray.ADAPTER.decode(proto3Bytes), decoded3)
    assertEquals(proto3Message, proto3Reflective.decode(Buffer().write(proto3Bytes)))

    val proto2Bytes = UseArrayProto2.ADAPTER.encodeByteString(proto2Message)
    val decoded2 = proto2Reflective.decode(proto2Bytes)
    assertEquals(proto2Message, decoded2)
    assertEquals(UseArrayProto2.ADAPTER.decode(proto2Bytes), decoded2)
    assertEquals(proto2Message, proto2Reflective.decode(Buffer().write(proto2Bytes)))
  }

  @Test fun decodeOfEmptyMessageGivesEmptyArrays() {
    assertEquals(UseArray(), proto3Reflective.decode(ByteArray(0)))
    assertEquals(UseArrayProto2(), proto2Reflective.decode(ByteArray(0)))
    assertContentEquals(intArrayOf(), proto3Reflective.decode(ByteArray(0)).array_int32)
  }

  @Test fun decodeMergesPackedChunksAndUnpackedValues() {
    val bytes = (
      // array_int32: packed [1, 2], packed [3], unpacked 4.
      "0a020102" + "0a0103" + "0804" +
        // array_uint32: an empty packed chunk.
        "1200" +
        // array_sint32: unpacked zigzag 3, which is -2.
        "1803" +
        // array_fixed32: unpacked 1, then packed [2, 3].
        "2501000000" + "220802000000" + "03000000" +
        // array_int64: unpacked 5, then packed [6, 7].
        "3005" + "32020607" +
        // array_float: unpacked 1.5.
        "5d0000c03f" +
        // array_double: unpacked 0.5.
        "610000000000" + "00e03f"
      ).decodeHex()

    val expected3 = UseArray.ADAPTER.decode(bytes)
    assertContentEquals(intArrayOf(1, 2, 3, 4), expected3.array_int32)
    assertContentEquals(intArrayOf(-2), expected3.array_sint32)
    assertContentEquals(intArrayOf(1, 2, 3), expected3.array_fixed32)
    assertContentEquals(longArrayOf(5L, 6L, 7L), expected3.array_int64)
    assertContentEquals(floatArrayOf(1.5f), expected3.array_float)
    assertContentEquals(doubleArrayOf(0.5), expected3.array_double)
    assertEquals(expected3, proto3Reflective.decode(bytes))
    assertEquals(expected3, proto3Reflective.decode(Buffer().write(bytes)))

    val expected2 = UseArrayProto2.ADAPTER.decode(bytes)
    assertEquals(expected2, proto2Reflective.decode(bytes))
    assertEquals(expected2, proto2Reflective.decode(Buffer().write(bytes)))
  }

  @Test fun redactMatchesGeneratedAdapter() {
    assertEquals(UseArray.ADAPTER.redact(proto3Message), proto3Reflective.redact(proto3Message))
    assertEquals(UseArrayProto2.ADAPTER.redact(proto2Message), proto2Reflective.redact(proto2Message))
    assertEquals(proto3Message, proto3Reflective.redact(proto3Message))
    assertEquals(UseArray(), proto3Reflective.redact(UseArray()))
  }

  @Test fun toStringMatchesGeneratedToString() {
    assertEquals(proto3Message.toString(), proto3Reflective.toString(proto3Message))
    assertEquals(proto2Message.toString(), proto2Reflective.toString(proto2Message))
    val partial = UseArray(array_int32 = intArrayOf(7), array_double = doubleArrayOf(0.5))
    assertEquals(partial.toString(), proto3Reflective.toString(partial))
  }

  @Test fun redactedArrayFieldIsClearedAndHiddenInToString() {
    val adapter = ProtoAdapter.newMessageAdapter(UseArrayRedacted::class.java)
    val message = UseArrayRedacted(secret = intArrayOf(1, 2), visible = intArrayOf(3))

    val redacted = adapter.redact(message)
    assertEquals(UseArrayRedacted.ADAPTER.redact(message), redacted)
    assertContentEquals(intArrayOf(), redacted.secret)
    assertContentEquals(intArrayOf(3), redacted.visible)

    assertEquals("UseArrayRedacted{secret=\u2588\u2588, visible=[3]}", adapter.toString(message))
    assertEquals(message.toString(), adapter.toString(message))
  }
}
