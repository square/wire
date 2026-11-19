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

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import okio.ByteString.Companion.decodeHex
import squareup.proto3.AllStructs

/**
 * Tests meant to be executed against both Java generated and Kotlin generated code. The reflective
 * adapter must redact Struct, ListValue, and Value fields like the generated adapter does.
 */
class RuntimeMessageAdapterStructRedactTest {
  private val reflective: ProtoAdapter<AllStructs> = ProtoAdapter.newMessageAdapter(AllStructs::class.java)

  private val struct: Map<String, Any?> = mapOf(
    "s" to "secret",
    "n" to 1.0,
    "b" to true,
    "z" to null,
    "nested" to mapOf("t" to "text", "n" to 2.0),
    "l" to listOf("a", 3.0, null, false),
  )
  private val redactedStruct: Map<String, Any?> = mapOf(
    "s" to null,
    "n" to 1.0,
    "b" to true,
    "z" to null,
    "nested" to mapOf("t" to null, "n" to 2.0),
    "l" to listOf(null, 3.0, null, false),
  )

  private val list: List<Any?> = listOf("a", 3.0, true, null, mapOf("k" to "v"), listOf("b", 4.0))
  private val redactedList: List<Any?> =
    listOf(null, 3.0, true, null, mapOf("k" to null), listOf(null, 4.0))

  /** Redacts [message] reflectively, and checks that the generated adapter agrees. */
  private fun redact(message: AllStructs): AllStructs {
    val redacted = reflective.redact(message)
    assertThat(redacted).isEqualTo(AllStructs.ADAPTER.redact(message))
    return redacted
  }

  @Test fun singularStructAndListValue() {
    val message = AllStructs.Builder().struct(struct).list(list).build()
    val expected = AllStructs.Builder().struct(redactedStruct).list(redactedList).build()
    assertThat(redact(message)).isEqualTo(expected)
    // The original message is unchanged.
    assertThat(message.struct).isEqualTo(struct)
    assertThat(message.list).isEqualTo(list)
  }

  @Test fun singularValues() {
    val message = AllStructs.Builder()
      .value_a("secret")
      .value_b(33.0)
      .value_c(true)
      .value_d(emptyMap<String, Any?>())
      .value_e(struct)
      .value_f(list)
      .build()
    val expected = AllStructs.Builder()
      .value_b(33.0)
      .value_c(true)
      .value_d(emptyMap<String, Any?>())
      .value_e(redactedStruct)
      .value_f(redactedList)
      .build()
    assertThat(redact(message)).isEqualTo(expected)
    assertThat(message.value_a).isEqualTo("secret")
    assertThat(message.value_e).isEqualTo(struct)
  }

  @Test fun repeatedStructs() {
    val message = AllStructs.Builder()
      .rep_struct(listOf(struct, emptyMap<String, Any?>()))
      .rep_list(listOf(list, emptyList<Any?>()))
      .rep_value_a(listOf("secret", 1.0, true, struct, list))
      .build()
    val redacted = redact(message)
    // Builders reject the null that redacting a string produces, so check the fields.
    assertThat(redacted.rep_struct).isEqualTo(listOf(redactedStruct, emptyMap<String, Any?>()))
    assertThat(redacted.rep_list).isEqualTo(listOf(redactedList, emptyList<Any?>()))
    assertThat(redacted.rep_value_a).isEqualTo(listOf(null, 1.0, true, redactedStruct, redactedList))
    assertThat(message.rep_struct).isEqualTo(listOf(struct, emptyMap<String, Any?>()))
    assertThat(message.rep_value_a).isEqualTo(listOf("secret", 1.0, true, struct, list))
  }

  @Test fun structMapValues() {
    val message = AllStructs.Builder()
      .map_int32_struct(mapOf(1 to struct, 2 to emptyMap<String, Any?>()))
      .map_int32_list(mapOf(1 to list, 2 to emptyList<Any?>()))
      .map_int32_value_a(mapOf(1 to "secret", 2 to 1.0, 3 to true, 5 to struct, 6 to list))
      .build()
    val redacted = redact(message)
    assertThat(redacted.map_int32_struct).isEqualTo(mapOf(1 to redactedStruct, 2 to emptyMap<String, Any?>()))
    assertThat(redacted.map_int32_list).isEqualTo(mapOf(1 to redactedList, 2 to emptyList<Any?>()))
    assertThat(redacted.map_int32_value_a)
      .isEqualTo(mapOf(1 to null, 2 to 1.0, 3 to true, 5 to redactedStruct, 6 to redactedList))
    assertThat(message.map_int32_struct).isEqualTo(mapOf(1 to struct, 2 to emptyMap<String, Any?>()))
    assertThat(message.map_int32_value_a[1]).isEqualTo("secret")
  }

  /** Builders reject null elements, so this message comes from the wire. */
  @Test fun nullValueElementsStayNull() {
    // rep_value_a = [null_value], map_int32_value_a = {1: null_value}.
    val bytes = "ba06020800fa1206080112020800".decodeHex()
    val message = AllStructs.ADAPTER.decode(bytes)
    assertThat(message.rep_value_a).isEqualTo(listOf<Any?>(null))
    assertThat(message.map_int32_value_a).isEqualTo(mapOf<Int, Any?>(1 to null))
    assertThat(redact(message)).isEqualTo(message)
  }

  @Test fun structOneOfMember() {
    val message = AllStructs.Builder().oneof_struct(struct).build()
    val expected = AllStructs.Builder().oneof_struct(redactedStruct).build()
    assertThat(redact(message)).isEqualTo(expected)
    assertThat(message.oneof_struct).isEqualTo(struct)
  }

  @Test fun listValueOneOfMember() {
    val message = AllStructs.Builder().oneof_list(list).build()
    val expected = AllStructs.Builder().oneof_list(redactedList).build()
    assertThat(redact(message)).isEqualTo(expected)
    assertThat(message.oneof_list).isEqualTo(list)
  }

  @Test fun absentStructFieldsStayAbsent() {
    val message = AllStructs.Builder().build()
    assertThat(redact(message)).isEqualTo(message)
  }
}
