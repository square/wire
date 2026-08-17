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
package com.squareup.wire.swift

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import com.squareup.wire.buildSchema
import com.squareup.wire.schema.Schema
import io.outfoxx.swiftpoet.FileSpec
import kotlin.test.Test
import okio.Path.Companion.toPath

class SwiftGeneratorTest {
  @Test fun singularProto3ExtensionAccessorsAreOptional() {
    val schema = buildSchema {
      add(
        "custom_options.proto".toPath(),
        """
        |syntax = "proto3";
        |
        |package squareup.protos3.kotlin.custom_options;
        |
        |import "google/protobuf/descriptor.proto";
        |
        |enum Proto3ExtensionEnum {
        |  UNKNOWN = 0;
        |  ENABLED = 1;
        |}
        |
        |extend google.protobuf.MessageOptions {
        |  string implicit_scalar = 50001;
        |  Proto3ExtensionEnum implicit_enum = 50002;
        |  repeated string repeated_scalar = 50003;
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("google.protobuf.MessageOptions")

    assertThat(code).contains("public var implicit_scalar: String? {")
    assertThat(code).contains("self.parseUnknownField(fieldNumber: 50001, type: String.self)")
    assertThat(code).doesNotContain("public var implicit_scalar: String {")

    assertThat(code).contains("public var implicit_enum: Proto3ExtensionEnum? {")
    assertThat(code).contains("self.parseUnknownField(fieldNumber: 50002, type: Proto3ExtensionEnum.self)")
    assertThat(code).doesNotContain("public var implicit_enum: Proto3ExtensionEnum {")

    assertThat(code).contains("public var repeated_scalar: [String] {")
    assertThat(code).contains("self.parseUnknownField(fieldNumber: 50003)")
  }

  @Test fun extensionFieldNumbersAreExposedAsConstants() {
    val schema = buildSchema {
      add(
        "custom_options.proto".toPath(),
        """
        |syntax = "proto3";
        |
        |package squareup.protos3.kotlin.custom_options;
        |
        |import "google/protobuf/descriptor.proto";
        |
        |extend google.protobuf.MessageOptions {
        |  string implicit_scalar = 50001;
        |  repeated string repeated_scalar = 50003;
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("google.protobuf.MessageOptions")

    assertThat(code).contains("public static let fieldNumber_implicit_scalar: UInt32 = 50001")
    assertThat(code).contains("public static let fieldNumber_repeated_scalar: UInt32 = 50003")
  }

  @Test fun extensionFieldEncodingsAreExposedAsConstants() {
    val schema = buildSchema {
      add(
        "extensible_message.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |package squareup.protos2.kotlin;
        |
        |message ExtensibleMessage {
        |  extensions 100 to 200;
        |}
        |
        |extend ExtensibleMessage {
        |  optional int32 ext_int32 = 100;
        |  optional sint32 ext_sint32 = 101;
        |  optional fixed32 ext_fixed32 = 102;
        |  repeated sint64 rep_ext_sint64 = 103;
        |  optional string ext_string = 104;
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("squareup.protos2.kotlin.ExtensibleMessage")

    assertThat(code).contains("public static let fieldEncoding_ext_int32: ProtoIntEncoding = .variable")
    assertThat(code).contains("public static let fieldEncoding_ext_sint32: ProtoIntEncoding = .signed")
    assertThat(code).contains("public static let fieldEncoding_ext_fixed32: ProtoIntEncoding = .fixed")
    assertThat(code).contains("public static let fieldEncoding_rep_ext_sint64: ProtoIntEncoding = .signed")
    // Only integer fields take an explicit encoding in parseUnknownField/setUnknownField.
    assertThat(code).doesNotContain("fieldEncoding_ext_string")
  }

  @Test fun extensionFieldNumberConstantsAreGeneratedOnceForHeapAllocatedMessages() {
    val schema = buildSchema {
      add(
        "big_message.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |package squareup.protos2.kotlin;
        |
        |message BigMessage {
        |  optional int32 f1 = 1;
        |  optional int32 f2 = 2;
        |  optional int32 f3 = 3;
        |  optional int32 f4 = 4;
        |  optional int32 f5 = 5;
        |  optional int32 f6 = 6;
        |  optional int32 f7 = 7;
        |  optional int32 f8 = 8;
        |  optional int32 f9 = 9;
        |  optional int32 f10 = 10;
        |  optional int32 f11 = 11;
        |  optional int32 f12 = 12;
        |  optional int32 f13 = 13;
        |  optional int32 f14 = 14;
        |  optional int32 f15 = 15;
        |  optional int32 f16 = 16;
        |
        |  extensions 1000 to 1999;
        |}
        |
        |extend BigMessage {
        |  optional string extra = 1000;
        |  optional sint32 extra_signed = 1001;
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("squareup.protos2.kotlin.BigMessage")

    // Precondition: the message must actually be heap-allocated, or the single-emission
    // assertion below passes trivially because only one extension block is generated at all.
    assertThat(code).contains("public struct Storage")

    val constant = "public static let fieldNumber_extra: UInt32 = 1000"
    assertThat(code).contains(constant)
    // The constant belongs on the extended type only, not on its CopyOnWrite storage type.
    assertThat(code.indexOf(constant)).isEqualTo(code.lastIndexOf(constant))

    val encodingConstant = "public static let fieldEncoding_extra_signed: ProtoIntEncoding = .signed"
    assertThat(code).contains(encodingConstant)
    assertThat(code.indexOf(encodingConstant)).isEqualTo(code.lastIndexOf(encodingConstant))
  }

  @Test fun usesFieldMask() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto3";
        |
        |package squareup.protos3;
        |
        |import "google/protobuf/field_mask.proto";
        |
        |message Message {
        |  google.protobuf.FieldMask mask = 1;
        |  repeated google.protobuf.FieldMask masks = 2;
        |  map<int32, google.protobuf.FieldMask> masks_by_id = 3;
        |  oneof choice {
        |    google.protobuf.FieldMask oneof_mask = 4;
        |    string name = 5;
        |  }
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("squareup.protos3.Message")

    assertThat(code).contains("import Wire")
    assertThat(code).contains("public var mask: FieldMask?")
    assertThat(code).contains("public var masks: [FieldMask]")
    assertThat(code).contains("public var masks_by_id: [Int32 : FieldMask]")
    assertThat(code).contains("case oneof_mask(FieldMask)")
    assertThat(code).contains("case 1: try protoReader.decodeMessage(into: &maskProtoData)")
    assertThat(code).contains("mask = try protoReader.decodeMergedMessage(FieldMask.self, from: maskProtoData)")
    assertThat(code).contains("try protoReader.decode(into: &masks)")
    assertThat(code).contains("try protoReader.decode(into: &masks_by_id, keyEncoding: .variable)")
    assertThat(code).contains("case 4: if choiceProtoTag != 4 { oneof_maskProtoData = nil }")
    assertThat(code).contains(
      "choice = .oneof_mask(try protoReader.decodeMergedMessage(FieldMask.self, from: oneof_maskProtoData))",
    )
    assertThat(code).doesNotContain("@ProtoDefaulted")
  }

  @Test fun mergesDuplicatedSingularMessageFields() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto3";
        |
        |package squareup.protos3;
        |
        |message Other {
        |  string name = 1;
        |}
        |
        |message Message {
        |  Other other = 1;
        |  repeated Other others = 2;
        |  oneof choice {
        |    Other oneof_other = 3;
        |  }
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("squareup.protos3.Message")

    assertThat(code).contains("case 1: try protoReader.decodeMessage(into: &otherProtoData)")
    assertThat(code).contains("other = try protoReader.decodeMergedMessage(Other.self, from: otherProtoData)")
    assertThat(code).contains("try protoReader.decode(into: &others)")
    assertThat(code).contains("case 3: if choiceProtoTag != 3 { oneof_otherProtoData = nil }")
    assertThat(code).contains(
      "choice = .oneof_other(try protoReader.decodeMergedMessage(Other.self, from: oneof_otherProtoData))",
    )
  }

  private fun Schema.generateSwift(typeName: String): String {
    val swiftGenerator = SwiftGenerator(this)
    val type = requireNotNull(getType(typeName))
    val generatedTypeName = swiftGenerator.generatedTypeName(type)
    return FileSpec.builder(generatedTypeName.moduleName, generatedTypeName.simpleName)
      .indent("    ")
      .apply {
        swiftGenerator.generateTypeTo(type, this)
      }
      .build()
      .toString()
  }
}
