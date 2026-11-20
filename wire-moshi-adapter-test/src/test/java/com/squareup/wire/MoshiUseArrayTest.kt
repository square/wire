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
import com.squareup.wire.protos.kotlin.use_array_redacted.UseArrayRedacted
import org.junit.Test
import squareup.proto2.UseArrayProto2
import squareup.proto2.UseArrayProto2Interop
import squareup.proto3.UseArray
import squareup.proto3.UseArrayInterop
import squareup.proto3.UseArrayTwin

/** Fields with `(wire.use_array)` encode and decode as JSON arrays, like any repeated field. */
class MoshiUseArrayTest {
  private val moshi = Moshi.Builder()
    .add(WireJsonAdapterFactory())
    .build()

  private val writeIdentityMoshi = Moshi.Builder()
    .add(WireJsonAdapterFactory(writeIdentityValues = true))
    .build()

  private fun <T> toJson(type: Class<T>, value: T): String = moshi.adapter(type).toJson(value)

  private fun <T> toJsonWritingIdentityValues(type: Class<T>, value: T): String = writeIdentityMoshi.adapter(type).toJson(value)

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

  @Test
  fun emptyArraysAreWrittenWhenWritingIdentityValues() {
    assertJsonEquals(
      EMPTY_JSON,
      toJsonWritingIdentityValues(UseArray::class.java, UseArray()),
    )
  }

  @Test
  fun proto2ArraysEncodeAsJsonArrays() {
    val message = UseArrayProto2(
      array_int32 = intArrayOf(1, -2),
      array_uint32 = intArrayOf(1, Int.MAX_VALUE),
      array_sint32 = intArrayOf(-3, 3),
      array_fixed32 = intArrayOf(1, 2),
      array_sfixed32 = intArrayOf(-4, 4),
      array_int64 = longArrayOf(1L, Long.MIN_VALUE),
      array_uint64 = longArrayOf(1L, -1L),
      array_sint64 = longArrayOf(-5L, 5L),
      array_fixed64 = longArrayOf(1L, 2L),
      array_sfixed64 = longArrayOf(-6L, 6L),
      array_float = floatArrayOf(1.5f),
      array_double = doubleArrayOf(0.5),
    )
    val json = toJson(UseArrayProto2::class.java, message)
    assertJsonEquals(PROTO2_JSON, json)
    val parsed = fromJson(UseArrayProto2::class.java, json)
    assertThat(parsed.array_uint64.toList()).isEqualTo(listOf(1L, -1L))
    assertThat(parsed.array_int64.toList()).isEqualTo(listOf(1L, Long.MIN_VALUE))
    assertThat(parsed.array_float.toList()).isEqualTo(listOf(1.5f))
  }

  @Test
  fun proto2EmptyArraysAreWrittenAsEmptyArrays() {
    assertJsonEquals(PROTO2_EMPTY_JSON, toJson(UseArrayProto2::class.java, UseArrayProto2()))
    val parsed = fromJson(UseArrayProto2::class.java, PROTO2_EMPTY_JSON)
    assertThat(parsed.array_int32.toList()).isEmpty()
    assertThat(parsed.array_double.toList()).isEmpty()
  }

  @Test
  fun proto2ArraysDecodeFromProtoFieldNames() {
    val message = fromJson(
      UseArrayProto2::class.java,
      """{"array_int32":[1,2],"array_uint64":[18446744073709551615],"arraySint32":[-1]}""",
    )
    assertThat(message.array_int32.toList()).isEqualTo(listOf(1, 2))
    assertThat(message.array_uint64.toList()).isEqualTo(listOf(-1L))
    assertThat(message.array_sint32.toList()).isEqualTo(listOf(-1))
  }

  @Test
  fun proto2ArraysRoundTripWithJavaInterop() {
    val message = UseArrayProto2Interop.Builder()
      .array_int32(intArrayOf(1, -2))
      .array_uint64(longArrayOf(-1L))
      .array_double(doubleArrayOf(0.5))
      .build()
    val json = toJson(UseArrayProto2Interop::class.java, message)
    assertThat(json.contains("\"array_uint64\":[18446744073709551615]")).isEqualTo(true)
    assertThat(fromJson(UseArrayProto2Interop::class.java, json)).isEqualTo(message)
  }

  @Test
  fun nullArrayDecodesAsEmptyArray() {
    val message = fromJson(UseArray::class.java, """{"arrayInt32":null,"arrayDouble":null,"arrayInt64":[1]}""")
    assertThat(message.array_int32.toList()).isEmpty()
    assertThat(message.array_double.toList()).isEmpty()
    assertThat(message.array_int64.toList()).isEqualTo(listOf(1L))
  }

  @Test
  fun specialFloatingPointValuesBehaveLikeLists() {
    val inputs = listOf(
      """{"arrayFloat":["NaN","Infinity","-Infinity"]}""",
      """{"arrayDouble":["NaN","Infinity","-Infinity"]}""",
      """{"arrayFloat":["abc"]}""",
      """{"arrayDouble":[1e400]}""",
    )
    for (json in inputs) {
      val array = outcome { fromJson(UseArray::class.java, json).let { "${it.array_float.toList()}${it.array_double.toList()}" } }
      val list = outcome { fromJson(UseArrayTwin::class.java, json).let { "${it.array_float}${it.array_double}" } }
      assertThat(array).isEqualTo(list)
    }
    val written = outcome {
      toJson(UseArray::class.java, UseArray(array_float = floatArrayOf(Float.NaN), array_double = doubleArrayOf(Double.POSITIVE_INFINITY)))
    }
    val writtenList = outcome {
      toJson(UseArrayTwin::class.java, UseArrayTwin(array_float = listOf(Float.NaN), array_double = listOf(Double.POSITIVE_INFINITY)))
    }
    assertThat(written).isEqualTo(writtenList)
  }

  @Test
  fun sameJsonDecodesToSameValuesAsList() {
    val array = fromJson(UseArray::class.java, EXPECTED_JSON)
    val list = fromJson(UseArrayTwin::class.java, EXPECTED_JSON)
    assertThat(array.array_int32.toList()).isEqualTo(list.array_int32)
    assertThat(array.array_uint32.toList()).isEqualTo(list.array_uint32)
    assertThat(array.array_sint32.toList()).isEqualTo(list.array_sint32)
    assertThat(array.array_fixed32.toList()).isEqualTo(list.array_fixed32)
    assertThat(array.array_sfixed32.toList()).isEqualTo(list.array_sfixed32)
    assertThat(array.array_int64.toList()).isEqualTo(list.array_int64)
    assertThat(array.array_uint64.toList()).isEqualTo(list.array_uint64)
    assertThat(array.array_sint64.toList()).isEqualTo(list.array_sint64)
    assertThat(array.array_fixed64.toList()).isEqualTo(list.array_fixed64)
    assertThat(array.array_sfixed64.toList()).isEqualTo(list.array_sfixed64)
    assertThat(array.array_float.toList()).isEqualTo(list.array_float)
    assertThat(array.array_double.toList()).isEqualTo(list.array_double)
    assertJsonEquals(toJson(UseArrayTwin::class.java, list), toJson(UseArray::class.java, array))
  }

  private fun outcome(block: () -> String): String = try {
    block()
  } catch (e: Exception) {
    "${e::class.java.name}: ${e.message}"
  }

  @Test
  fun redactedArrayIsListedAsRedacted() {
    val message = UseArrayRedacted(secret = intArrayOf(1, 2), visible = intArrayOf(3))
    val json = moshi.adapter(UseArrayRedacted::class.java).redacting().toJson(message)
    assertJsonEquals("""{"visible":[3],"__redacted_fields":["secret"]}""", json)
    assertThat(moshi.adapter(UseArrayRedacted::class.java).toJson(message))
      .isEqualTo("""{"secret":[1,2],"visible":[3]}""")
  }

  private companion object {
    val PROTO2_JSON =
      """
      |{
      |  "array_int32": [1, -2],
      |  "array_uint32": [1, 2147483647],
      |  "array_sint32": [-3, 3],
      |  "array_fixed32": [1, 2],
      |  "array_sfixed32": [-4, 4],
      |  "array_int64": [1, -9223372036854775808],
      |  "array_uint64": [1, 18446744073709551615],
      |  "array_sint64": [-5, 5],
      |  "array_fixed64": [1, 2],
      |  "array_sfixed64": [-6, 6],
      |  "array_float": [1.5],
      |  "array_double": [0.5]
      |}
      """.trimMargin()

    val PROTO2_EMPTY_JSON =
      """
      |{
      |  "array_int32": [],
      |  "array_uint32": [],
      |  "array_sint32": [],
      |  "array_fixed32": [],
      |  "array_sfixed32": [],
      |  "array_int64": [],
      |  "array_uint64": [],
      |  "array_sint64": [],
      |  "array_fixed64": [],
      |  "array_sfixed64": [],
      |  "array_float": [],
      |  "array_double": []
      |}
      """.trimMargin()

    val EMPTY_JSON =
      """
      |{
      |  "arrayInt32": [],
      |  "arrayUint32": [],
      |  "arraySint32": [],
      |  "arrayFixed32": [],
      |  "arraySfixed32": [],
      |  "arrayInt64": [],
      |  "arrayUint64": [],
      |  "arraySint64": [],
      |  "arrayFixed64": [],
      |  "arraySfixed64": [],
      |  "arrayFloat": [],
      |  "arrayDouble": []
      |}
      """.trimMargin()

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
