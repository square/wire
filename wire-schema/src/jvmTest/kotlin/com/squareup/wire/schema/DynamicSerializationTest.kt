/*
 * Copyright (C) 2023 Square, Inc.
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
package com.squareup.wire.schema

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.wire.buildSchema
import com.squareup.wire.durationOfSeconds
import com.squareup.wire.ofEpochSecond
import kotlin.test.Ignore
import kotlin.test.Test
import okio.ByteString.Companion.encodeUtf8
import okio.Path.Companion.toPath

class DynamicSerializationTest {
  @Test
  fun proto3TypesTest() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
       |syntax = "proto3";
       |import "google/protobuf/duration.proto";
       |import "google/protobuf/timestamp.proto";
       |import "google/protobuf/empty.proto";
       |import "google/protobuf/struct.proto";
       |import "google/protobuf/wrappers.proto";
       |
       |message Message {
       |  google.protobuf.Duration duration_field = 1;
       |  google.protobuf.Timestamp timestamp_field = 2;
       |  google.protobuf.Empty empty_field = 3;
       |  google.protobuf.Struct struct_field = 4;
       |  google.protobuf.Value value_field = 5;
       |  google.protobuf.ListValue list_value_field = 6;
       |  google.protobuf.NullValue null_value_field = 7;
       |  google.protobuf.DoubleValue double_value_field = 8;
       |  google.protobuf.FloatValue float_value_field = 9;
       |  google.protobuf.Int64Value int64_value_field = 10;
       |  google.protobuf.UInt64Value uInt64_value_field = 11;
       |  google.protobuf.Int32Value int32_value_field = 12;
       |  google.protobuf.UInt32Value uInt32_value_field = 13;
       |  google.protobuf.BoolValue bool_value_field = 14;
       |  google.protobuf.StringValue string_value_field = 15;
       |  google.protobuf.BytesValue bytes_value_field = 16;
       |}
       |
        """.trimMargin(),
      )
    }

    val adapter = schema.protoAdapter(typeName = "Message", includeUnknown = true)
    val expected = mapOf(
      "duration_field" to durationOfSeconds(seconds = 60 * 60 * 48L, nano = 0L), // 2 days.
      "timestamp_field" to ofEpochSecond(epochSecond = 123131234L, nano = 23432423L),
      "empty_field" to Unit,
      "struct_field" to mapOf("one" to 1.0, "two" to 2.0),
      "value_field" to "Can be Anything",
      "list_value_field" to listOf(listOf(1.0, 2.0, 3.0), 5.0, false, "boom"),
      "double_value_field" to 33.0,
      "float_value_field" to 33f,
      "int64_value_field" to 33L,
      "uInt64_value_field" to 33L,
      "int32_value_field" to 33,
      "uInt32_value_field" to 33,
      "bool_value_field" to true,
      "string_value_field" to "πάμε",
      "bytes_value_field" to "πάμε".encodeUtf8(),
    )
    assertThat(adapter.decode(adapter.encode(expected))).isEqualTo(expected)
  }

  @Ignore // Not supported.
  @Test
  fun mapTest() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
       |syntax = "proto3";
       |
       |message Message {
       |  map<string, int64> currencies = 1;
       |}
       |
        """.trimMargin(),
      )
    }

    val adapter = schema.protoAdapter(typeName = "Message", includeUnknown = true)
    val expected = mapOf(
      "currencies" to mapOf("USD" to 44L, "EUR" to 33L),
    )
    assertThat(adapter.decode(adapter.encode(expected))).isEqualTo(expected)
  }
}
