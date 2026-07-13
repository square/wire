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
