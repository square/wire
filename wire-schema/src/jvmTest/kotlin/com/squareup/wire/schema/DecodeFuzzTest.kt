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
package com.squareup.wire.schema

import com.squareup.wire.buildSchema
import com.squareup.wire.testing.DecodeFuzzer
import com.squareup.wire.testing.DecodeFuzzer.Case
import com.squareup.wire.testing.DecodeFuzzer.Config
import kotlin.test.Test
import okio.Path.Companion.toPath

/**
 * Decodes random wire bytes with adapters that a [Schema] builds at run time. See [DecodeFuzzer]
 * for the contract and for how to run a longer local fuzz.
 */
class DecodeFuzzTest {
  private val schema = buildSchema {
    add(
      "fuzz.proto".toPath(),
      """
          |import "google/protobuf/any.proto";
          |import "google/protobuf/duration.proto";
          |import "google/protobuf/struct.proto";
          |import "google/protobuf/timestamp.proto";
          |
          |message Node {
          |  optional int32 a_int32 = 1;
          |  optional uint32 a_uint32 = 2;
          |  optional sint32 a_sint32 = 3;
          |  optional fixed32 a_fixed32 = 4;
          |  optional sfixed32 a_sfixed32 = 5;
          |  optional int64 a_int64 = 6;
          |  optional uint64 a_uint64 = 7;
          |  optional sint64 a_sint64 = 8;
          |  optional fixed64 a_fixed64 = 9;
          |  optional sfixed64 a_sfixed64 = 10;
          |  optional bool a_bool = 11;
          |  optional float a_float = 12;
          |  optional double a_double = 13;
          |  optional string a_string = 14;
          |  optional bytes a_bytes = 15;
          |  optional Color color = 16;
          |  optional Node child = 17;
          |  repeated Node children = 18;
          |  repeated int32 unpacked = 19;
          |  repeated sint64 packed = 20 [packed = true];
          |  repeated double packed_doubles = 21 [packed = true];
          |  map<string, Node> by_name = 22;
          |  map<int32, string> by_number = 23;
          |  oneof choice {
          |    string text = 24;
          |    Node nested = 25;
          |    fixed32 number = 26;
          |  }
          |  optional google.protobuf.Any any = 27;
          |  optional google.protobuf.Duration duration = 28;
          |  optional google.protobuf.Timestamp timestamp = 29;
          |  optional google.protobuf.Struct struct = 30;
          |
          |  enum Color {
          |    RED = 0;
          |    GREEN = 1;
          |  }
          |}
      """.trimMargin(),
    )
  }

  @Test fun decodingRandomBytesThrowsOnlyIOException() {
    val config = Config(tags = (1..32).toList())
    DecodeFuzzer.fuzzAll(
      listOf(
        Case("Node", schema.protoAdapter("Node", includeUnknown = false), config),
        Case("Node with unknown fields", schema.protoAdapter("Node", includeUnknown = true), config),
        Case("Any", schema.protoAdapter("google.protobuf.Any", includeUnknown = true), config),
        Case("Duration", schema.protoAdapter("google.protobuf.Duration", includeUnknown = false), config),
        Case("Timestamp", schema.protoAdapter("google.protobuf.Timestamp", includeUnknown = false), config),
        Case("Struct", schema.protoAdapter("google.protobuf.Struct", includeUnknown = false), config),
      ),
    )
  }
}
