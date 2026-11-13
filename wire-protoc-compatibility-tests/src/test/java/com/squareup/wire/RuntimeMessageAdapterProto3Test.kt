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
import assertk.assertions.isNotEqualTo
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.SchemaLoader
import java.nio.file.FileSystems
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import org.junit.Test
import squareup.proto3.java.alltypes.AllTypes
import squareup.proto3.java.alltypes.AllWrappers
import squareup.proto3.kotlin.alltypes.AllTypesOuterClass

/**
 * Java `compact = true` code uses the reflective adapter.
 * It must encode proto3 messages like the generated adapter.
 */
class RuntimeMessageAdapterProto3Test {
  private val allTypesAdapter = ProtoAdapter.newMessageAdapter(
    AllTypes::class.java,
    "type.googleapis.com/squareup.proto3.java.alltypes.AllTypes",
    Syntax.PROTO_3,
    AllTypes::class.java.classLoader,
  )
  private val allWrappersAdapter = ProtoAdapter.newMessageAdapter(
    AllWrappers::class.java,
    "type.googleapis.com/squareup.proto3.java.alltypes.AllWrappers",
    Syntax.PROTO_3,
    AllWrappers::class.java.classLoader,
  )

  private val schema = SchemaLoader(FileSystems.getDefault())
    .apply {
      initRoots(sourcePath = listOf(Location.get("src/main/proto")))
    }
    .loadSchema()

  @Test fun allDefaultValues() {
    val message = AllTypes.Builder().build()

    assertEncodesLikeGenerated(AllTypes.ADAPTER, allTypesAdapter, message)
    val protocBytes = AllTypesOuterClass.AllTypes.getDefaultInstance().toByteArray().toByteString()
    assertThat(allTypesAdapter.encodeByteString(message)).isEqualTo(protocBytes)
  }

  @Test fun mixedValues() {
    val message = AllTypes.Builder()
      .int32(1)
      .uint64(0L)
      .bool(false)
      .float_(-0.0f)
      .double_(-0.0)
      .string("")
      .bytes(ByteString.EMPTY)
      .nested_enum(AllTypes.NestedEnum.UNKNOWN)
      .sfixed64(7L)
      .build()

    assertEncodesLikeGenerated(AllTypes.ADAPTER, allTypesAdapter, message)
  }

  @Test fun identityValuesWithExplicitPresence() {
    val message = AllTypes.Builder()
      .nested_message(AllTypes.NestedMessage(0))
      .opt_int32(0)
      .opt_uint64(0L)
      .opt_bool(false)
      .opt_float(0f)
      .opt_double(0.0)
      .opt_string("")
      .opt_bytes(ByteString.EMPTY)
      .rep_int32(listOf(0))
      .rep_string(listOf(""))
      .rep_nested_enum(listOf(AllTypes.NestedEnum.UNKNOWN))
      .pack_int32(listOf(0))
      .pack_nested_enum(listOf(AllTypes.NestedEnum.UNKNOWN))
      .map_int32_int32(mapOf(0 to 0))
      .map_string_string(mapOf("" to ""))
      .map_string_enum(mapOf("" to AllTypes.NestedEnum.UNKNOWN))
      .oneof_int32(0)
      .build()

    assertThat(AllTypes.ADAPTER.encodeByteString(message)).isNotEqualTo(ByteString.EMPTY)
    assertEncodesLikeGenerated(AllTypes.ADAPTER, allTypesAdapter, message)
  }

  @Test fun oneOfEmptyString() {
    val message = AllTypes.Builder().oneof_string("").build()

    assertEncodesLikeGenerated(AllTypes.ADAPTER, allTypesAdapter, message)
  }

  @Test fun unrecognizedEnumValue() {
    // nested_enum (tag 16) holds 99, which no constant declares.
    val bytes = "800163".decodeHex()

    assertEncodesLikeGenerated(AllTypes.ADAPTER, allTypesAdapter, AllTypes.ADAPTER.decode(bytes))
    assertThat(allTypesAdapter.encodeByteString(allTypesAdapter.decode(bytes))).isEqualTo(bytes)
  }

  @Test fun wrappersWithIdentityValues() {
    val message = AllWrappers.Builder()
      .double_value(0.0)
      .int32_value(0)
      .bool_value(false)
      .string_value("")
      .bytes_value(ByteString.EMPTY)
      .build()

    assertThat(AllWrappers.ADAPTER.encodeByteString(message)).isNotEqualTo(ByteString.EMPTY)
    assertEncodesLikeGenerated(AllWrappers.ADAPTER, allWrappersAdapter, message)
  }

  @Test fun schemaAdapterSkipsScalarIdentityValues() {
    val adapter = schema.protoAdapter("squareup.proto3.java.alltypes.AllTypes", false)
    val message = AllTypes.Builder().double_(-0.0).opt_int32(0).build()
    val expected = AllTypes.ADAPTER.encodeByteString(message)

    // The schema adapter takes enum values as constant names or as tags.
    for (zeroEnum in listOf("UNKNOWN", 0)) {
      val value = mapOf("int32" to 0, "double" to -0.0, "string" to "", "nested_enum" to zeroEnum, "opt_int32" to 0)
      assertThat(adapter.encodeByteString(value)).isEqualTo(expected)
      assertThat(adapter.encodedSize(value)).isEqualTo(expected.size)
    }
  }

  @Test fun schemaAdapterWritesWrappersWithIdentityValues() {
    val adapter = schema.protoAdapter("squareup.proto3.java.alltypes.AllWrappers", false)
    val value = mapOf("int32_value" to 0, "bool_value" to false, "string_value" to "")
    val message = AllWrappers.Builder().int32_value(0).bool_value(false).string_value("").build()

    val expected = AllWrappers.ADAPTER.encodeByteString(message)
    assertThat(expected).isNotEqualTo(ByteString.EMPTY)
    assertThat(adapter.encodeByteString(value)).isEqualTo(expected)
    assertThat(adapter.encodedSize(value)).isEqualTo(expected.size)
  }

  private fun <M : Any> assertEncodesLikeGenerated(
    generated: ProtoAdapter<M>,
    reflective: ProtoAdapter<M>,
    message: M,
  ) {
    val expected = generated.encodeByteString(message)

    assertThat(reflective.encodedSize(message)).isEqualTo(expected.size)
    assertThat(reflective.encodeByteString(message)).isEqualTo(expected)
    val buffer = Buffer()
    reflective.encode(ProtoWriter(buffer), message)
    assertThat(buffer.readByteString()).isEqualTo(expected)
  }
}
