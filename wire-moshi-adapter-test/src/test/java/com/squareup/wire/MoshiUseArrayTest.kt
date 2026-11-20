/*
 * Copyright (C) 2026 Square, Inc.
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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.wire.json.assertJsonEquals
import org.junit.Test
import squareup.proto3.UseArray
import squareup.proto3.UseArrayInterop

/** Fields with `(wire.use_array)` encode and decode as JSON arrays, like any repeated field. */
class MoshiUseArrayTest {
  private val moshi = Moshi.Builder()
    .add(WireJsonAdapterFactory())
    .build()

  private fun <T> toJson(type: Class<T>, value: T): String = moshi.adapter(type).toJson(value)

  private fun <T> fromJson(type: Class<T>, json: String): T = moshi.adapter(type).fromJson(json)!!

  private val dataException = JsonDataException::class

  @Test
  fun arraysEncodeAsJsonArrays() {
    val message = UseArray(
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
    assertJsonEquals(EXPECTED_JSON, toJson(UseArray::class.java, message))
  }

  @Test
  fun arraysDecodeFromJsonArrays() {
    val message = fromJson(UseArray::class.java, EXPECTED_JSON)
    assertThat(message.array_int32.toList()).isEqualTo(listOf(1, -2, Int.MAX_VALUE))
    assertThat(message.array_uint32.toList()).isEqualTo(listOf(1, -1))
    assertThat(message.array_sint32.toList()).isEqualTo(listOf(-3, 3))
    assertThat(message.array_fixed32.toList()).isEqualTo(listOf(1, -1))
    assertThat(message.array_sfixed32.toList()).isEqualTo(listOf(-4, 4))
    assertThat(message.array_int64.toList()).isEqualTo(listOf(1L, -2L, Long.MAX_VALUE))
    assertThat(message.array_uint64.toList()).isEqualTo(listOf(1L, -1L))
    assertThat(message.array_sint64.toList()).isEqualTo(listOf(-5L, 5L))
    assertThat(message.array_fixed64.toList()).isEqualTo(listOf(1L, -1L))
    assertThat(message.array_sfixed64.toList()).isEqualTo(listOf(-6L, 6L))
    assertThat(message.array_float.toList()).isEqualTo(listOf(1.5f, -2.25f))
    assertThat(message.array_double.toList()).isEqualTo(listOf(0.5, -1.0e10))
  }

  @Test
  fun emptyArraysAreOmitted() {
    assertThat(toJson(UseArray::class.java, UseArray())).isEqualTo("{}")
  }

  @Test
  fun missingArraysDecodeAsEmpty() {
    val message = fromJson(UseArray::class.java, """{"arrayInt32":[7]}""")
    assertThat(message.array_int32.toList()).isEqualTo(listOf(7))
    assertThat(message.array_int64.toList()).isEmpty()
    assertThat(message.array_float.toList()).isEmpty()
  }

  @Test
  fun arraysRoundTripWithJavaInterop() {
    val message = UseArrayInterop.Builder()
      .array_int32(intArrayOf(1, -2))
      .array_uint32(intArrayOf(-1))
      .array_int64(longArrayOf(Long.MIN_VALUE))
      .array_uint64(longArrayOf(-1L))
      .array_float(floatArrayOf(1.5f))
      .array_double(doubleArrayOf(0.5))
      .build()
    val json = toJson(UseArrayInterop::class.java, message)
    assertJsonEquals(
      """
      |{
      |  "arrayInt32": [1, -2],
      |  "arrayUint32": [4294967295],
      |  "arrayInt64": ["-9223372036854775808"],
      |  "arrayUint64": ["18446744073709551615"],
      |  "arrayFloat": [1.5],
      |  "arrayDouble": [0.5]
      |}
      """.trimMargin(),
      json,
    )
    assertThat(fromJson(UseArrayInterop::class.java, json)).isEqualTo(message)
  }

  @Test
  fun arrayWithNullElementIsRejected() {
    assertFailure { fromJson(UseArray::class.java, """{"arrayInt32":[1,null]}""") }
      .isInstanceOf(dataException)
      .messageContains("Repeated field arrayInt32 cannot contain a null element")
  }

  private companion object {
    val EXPECTED_JSON =
      """
      |{
      |  "arrayInt32": [1, -2, 2147483647],
      |  "arrayUint32": [1, 4294967295],
      |  "arraySint32": [-3, 3],
      |  "arrayFixed32": [1, 4294967295],
      |  "arraySfixed32": [-4, 4],
      |  "arrayInt64": ["1", "-2", "9223372036854775807"],
      |  "arrayUint64": ["1", "18446744073709551615"],
      |  "arraySint64": ["-5", "5"],
      |  "arrayFixed64": ["1", "18446744073709551615"],
      |  "arraySfixed64": ["-6", "6"],
      |  "arrayFloat": [1.5, -2.25],
      |  "arrayDouble": [0.5, -1.0E10]
      |}
      """.trimMargin()
  }
}
