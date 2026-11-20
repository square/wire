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

  @Test fun skipsAssignmentOfUnrecognizedEnumValues() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |package squareup.protos2;
        |
        |enum Kind {
        |  UNKNOWN = 0;
        |  A = 1;
        |}
        |
        |message Other {
        |  optional string name = 1;
        |}
        |
        |message Message {
        |  optional Kind kind = 1;
        |  optional Kind value = 2;
        |  repeated Kind kinds = 3;
        |  oneof choice {
        |    Kind oneof_kind = 4;
        |    Other oneof_other = 5;
        |  }
        |  oneof other_choice {
        |    Kind other_kind = 6;
        |    string other_name = 7;
        |  }
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("squareup.protos2.Message")

    assertThat(code).contains("case 1: if let _value = try protoReader.decode(Kind.self) { kind = _value }")
    assertThat(code).contains("case 2: if let _value = try protoReader.decode(Kind.self) { value = _value }")
    assertThat(code).contains("case 3: try protoReader.decode(into: &kinds)")
    assertThat(code).contains(
      "case 4: if let _value = try protoReader.decode(Kind.self) { choiceProtoTag = 4; choice = .oneof_kind(_value) }",
    )
    assertThat(code).contains(
      "case 6: if let _value = try protoReader.decode(Kind.self) { other_choice = .other_kind(_value) }",
    )
    assertThat(code).doesNotContain("kind = try protoReader.decode(Kind.self)")
    assertThat(code).doesNotContain("choice = (try protoReader.decode(Kind.self))")
  }

  @Test fun oneOfNamesDoNotCollideWithNestedTypes() {
    for (nestedType in listOf("message Selection {}", "enum Selection { UNKNOWN = 0; }")) {
      val schema = buildSchema {
        add(
          "message.proto".toPath(),
          """
          |syntax = "proto3";
          |
          |message Message {
          |  $nestedType
          |  oneof selection {
          |    string text = 1;
          |    int32 number = 2;
          |  }
          |}
          """.trimMargin(),
        )
      }

      val code = schema.generateSwift("Message")

      assertThat(code).contains("public enum Selection_OneOf {")
      assertThat(code).contains("public var selection: Message.Selection_OneOf?")
    }
  }

  @Test fun dollarSignInStringDefaultStaysPlain() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |message Message {
        |  optional string text = 1 [default = "a${'$'}b \"c\" \\d\n"];
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("Message")

    assertThat(code).contains("""@CustomDefaulted(defaultValue: "a${'$'}b \"c\" \\d\n")""")
    assertThat(code).doesNotContain("{'")
  }

  @Test fun requiredFieldsWithDefaultsAreStoredAsPlainProperties() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |enum Color {
        |  RED = 0;
        |  GREEN = 1;
        |}
        |
        |message Message {
        |  required int32 count = 1 [default = 5];
        |  required string label = 2 [default = "a${'$'}b"];
        |  required bytes payload = 3 [default = "abc"];
        |  required Color color = 4 [default = GREEN];
        |  required bool flag = 5 [default = true];
        |  required float ratio = 6 [default = 1.5];
        |  required double offset = 7 [default = inf];
        |  optional int32 optional_count = 8 [default = 7];
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("Message")

    // The wrapper only accepts optional properties, so required fields must not use it.
    assertThat(code).contains("    public var count: Int32\n")
    assertThat(code).contains("    public var label: String\n")
    assertThat(code).contains("    public var payload: Foundation.Data\n")
    assertThat(code).contains("    public var color: Color\n")
    assertThat(code).contains("    public var flag: Bool\n")
    assertThat(code).contains("    public var ratio: Float\n")
    assertThat(code).contains("    public var offset: Double\n")
    assertThat(code).contains("self.count = count")
    assertThat(code).doesNotContain("self._count")
    assertThat(code).doesNotContain("self._ratio")
    assertThat(code).doesNotContain("Double.infinity")

    // Decoding still fails when a required field is missing.
    assertThat(code).contains("""self.count = try Message.checkIfMissing(count, "count")""")
    assertThat(code).contains("""self.offset = try Message.checkIfMissing(offset, "offset")""")

    // An optional field keeps its default.
    assertThat(code).contains("@CustomDefaulted(defaultValue: 7)\n    public var optional_count: Int32?")
  }

  @Test fun requiredFieldsWithDefaultsAreAlwaysEncodedAsJson() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |enum Color {
        |  RED = 0;
        |  GREEN = 1;
        |}
        |
        |message Message {
        |  required int32 count = 1 [default = 5];
        |  required string label = 2 [default = "abc"];
        |  required Color color = 3 [default = GREEN];
        |  required bool flag = 4 [default = true];
        |  required float ratio = 5 [default = 1.5];
        |  required double offset = 6 [default = nan];
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("Message")

    // Decoding rejects a JSON object without a required field, so none may be left out.
    assertThat(code).doesNotContain("includeDefaults")
    assertThat(code).doesNotContain("!=")
    assertThat(code).contains("""try container.encode(self.count, forKey: "count")""")
    assertThat(code).contains("""try container.encode(self.label, forKey: "label")""")
    assertThat(code).contains("""try container.encode(self.color, forKey: "color")""")
    assertThat(code).contains("""try container.encode(self.flag, forKey: "flag")""")
    assertThat(code).contains("""try container.encode(self.ratio, forKey: "ratio")""")
    assertThat(code).contains("""try container.encode(self.offset, forKey: "offset")""")
  }

  @Test fun requiredFieldsWithoutDefaultsAreAlwaysEncodedAsJson() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |enum Color {
        |  RED = 0;
        |  GREEN = 1;
        |}
        |
        |message Nested {
        |}
        |
        |message Message {
        |  required int32 count = 1;
        |  required bool flag = 2;
        |  required double ratio = 3;
        |  required string label = 4;
        |  required bytes payload = 5;
        |  required Color color = 6;
        |  required Nested nested = 7;
        |  optional int32 optional_count = 8;
        |  repeated int32 counts = 9;
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("Message")

    // Decoding rejects a JSON object without a required field, so a zero value must be written.
    assertThat(code).contains("""try container.encode(self.count, forKey: "count")""")
    assertThat(code).contains("""try container.encode(self.flag, forKey: "flag")""")
    assertThat(code).contains("""try container.encode(self.ratio, forKey: "ratio")""")
    assertThat(code).contains("""try container.encode(self.label, forKey: "label")""")
    assertThat(code).contains("""try container.encode(stringEncoded: self.payload, forKey: "payload")""")
    assertThat(code).contains("""try container.encode(self.color, forKey: "color")""")
    assertThat(code).contains("""try container.encode(self.nested, forKey: "nested")""")
    assertThat(code).doesNotContain("self.count != 0")
    assertThat(code).doesNotContain("self.flag != false")
    assertThat(code).doesNotContain("self.label.isEmpty")
    assertThat(code).doesNotContain("self.payload.isEmpty")
    assertThat(code).doesNotContain("self.color.rawValue")

    // Repeated fields still skip empty values.
    assertThat(code).contains("if includeDefaults || !self.counts.isEmpty {")
  }

  @Test fun proto3FieldsStillSkipJsonDefaults() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto3";
        |
        |message Message {
        |  int32 count = 1;
        |  string label = 2;
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("Message")

    assertThat(code).contains("if includeDefaults || self.count != 0 {")
    assertThat(code).contains("if includeDefaults || !self.label.isEmpty {")
  }

  @Test fun largeMessagesStoreRequiredFieldsWithDefaultsWithoutWrappers() {
    val fields = (1..16).joinToString("\n") { "|  required int32 f$it = $it [default = $it];" }
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        "|syntax = \"proto2\";\n|\n|message Message {\n$fields\n|}".trimMargin(),
      )
    }

    val code = schema.generateSwift("Message")

    assertThat(code).contains("public struct Storage {")
    assertThat(code).contains("        public var f1: Int32\n")
    assertThat(code).contains("        public var f16: Int32\n")
    assertThat(code).doesNotContain("@CustomDefaulted")
    assertThat(code).doesNotContain("wrappedValue")
  }

  @Test fun floatDefaultsWithoutLeadingOrTrailingDigitsAreSwiftLiterals() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |message Message {
        |  optional float a = 1 [default = .5];
        |  optional float b = 2 [default = 2.];
        |  optional float c = 3 [default = 1.5e3];
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("Message")

    assertThat(code).contains("@CustomDefaulted(defaultValue: 0.5)")
    assertThat(code).contains("@CustomDefaulted(defaultValue: 2.0)")
    assertThat(code).contains("@CustomDefaulted(defaultValue: 1.5e3)")
  }

  @Test fun dashesAndDotsInNamesBecomeUnderscores() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto3";
        |
        |enum Color-Kind {
        |  DARK-RED = 0;
        |  LIGHT.BLUE = 1;
        |}
        |
        |message Message-Type {
        |  string field-name = 1;
        |  string dotted.name = 2 [json_name = "dotted.name"];
        |  Color-Kind color-kind = 3;
        |  oneof one-of {
        |    string text-value = 4;
        |    int32 number.value = 5;
        |  }
        |}
        """.trimMargin(),
      )
    }

    val messageCode = schema.generateSwift("Message-Type")
    assertThat(messageCode).contains("public struct Message_Type {")
    assertThat(messageCode).contains("public var field_name: String")
    assertThat(messageCode).contains("public var dotted_name: String")
    assertThat(messageCode).contains("public var color_kind: Color_Kind")
    assertThat(messageCode).contains("public var one_of: Message_Type.One_of?")
    assertThat(messageCode).contains("case text_value(String)")
    assertThat(messageCode).contains("""forKey: "field-name"""")

    val enumCode = schema.generateSwift("Color-Kind")
    assertThat(enumCode).contains("case DARK_RED = 0")
    assertThat(enumCode).contains("case LIGHT_BLUE = 1")
    assertThat(enumCode).contains("""case .DARK_RED: return "DARK-RED"""")
  }

  @Test fun dollarSignInJsonNameStaysPlain() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |message Message {
        |  optional string a = 1 [json_name = "a${'$'}b"];
        |  required string b = 2 [json_name = "${'$'}{x}"];
        |  oneof choice {
        |    string c = 3 [json_name = "${'$'}c"];
        |    int32 d = 4;
        |  }
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("Message")

    assertThat(code).contains("""firstOfKeys: "a${'$'}b", "a"""")
    assertThat(code).contains("""forKey: preferCamelCase ? "a${'$'}b" : "a"""")
    assertThat(code).contains("""firstOfKeys: "${'$'}{x}", "b"""")
    assertThat(code).contains("""forKey: "${'$'}c"""")
    assertThat(code).contains("""forKey: preferCamelCase ? "${'$'}c" : "c"""")
    assertThat(code).doesNotContain("{'")
  }

  @Test fun percentSignInJsonNameStaysPlain() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |message Message {
        |  optional string a = 1 [json_name = "100%"];
        |  optional string b = 2 [json_name = "%s %S %1S"];
        |  oneof choice {
        |    string c = 3 [json_name = "%c"];
        |    int32 d = 4;
        |  }
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("Message")

    assertThat(code).contains("""firstOfKeys: "100%", "a"""")
    assertThat(code).contains("""forKey: preferCamelCase ? "100%" : "a"""")
    assertThat(code).contains("""firstOfKeys: "%s %S %1S", "b"""")
    assertThat(code).contains("""forKey: "%c"""")
    assertThat(code).contains("""forKey: preferCamelCase ? "%c" : "c"""")
  }

  @Test fun enumConstantsWithSpecialSwiftMeaningKeepTheirNames() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |enum Special {
        |  self = 0;
        |  init = 1;
        |  Type = 2;
        |  Protocol = 3;
        |  PLAIN = 4;
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("Special")

    assertThat(code).contains("case `self` = 0")
    assertThat(code).contains("case `init` = 1")
    assertThat(code).contains("case `Type` = 2")
    assertThat(code).contains("case `Protocol` = 3")
    assertThat(code).contains("case PLAIN = 4")
    assertThat(code).contains("""case .`self`: return "self"""")
    assertThat(code).contains("""case .`init`: return "init"""")
    assertThat(code).contains("""case .`Type`: return "Type"""")
    assertThat(code).contains("""case .`Protocol`: return "Protocol"""")
    assertThat(code).contains("""case .PLAIN: return "PLAIN"""")
    assertThat(code).doesNotContain("``")
  }

  @Test fun deprecatedEnumConstantsWithSpecialSwiftMeaningAreEscapedInAllCases() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |enum Special {
        |  self = 0 [deprecated = true];
        |  Type = 1;
        |  Protocol = 2;
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("Special")

    assertThat(code).contains("return [.`self`, .`Type`, .`Protocol`]")
  }

  @Test fun enumDefaultsWithSpecialSwiftMeaningUseImplicitMembers() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |enum Special {
        |  self = 0;
        |  init = 1;
        |  Type = 2;
        |  Protocol = 3;
        |  PLAIN = 4;
        |}
        |
        |message Message {
        |  optional Special a = 1 [default = self];
        |  optional Special b = 2 [default = init];
        |  optional Special c = 3 [default = Type];
        |  optional Special d = 4 [default = Protocol];
        |  optional Special e = 5 [default = PLAIN];
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("Message")

    assertThat(code).contains("@CustomDefaulted(defaultValue: (.`self` as Special))")
    assertThat(code).contains("@CustomDefaulted(defaultValue: (.`init` as Special))")
    assertThat(code).contains("@CustomDefaulted(defaultValue: (.`Type` as Special))")
    assertThat(code).contains("@CustomDefaulted(defaultValue: (.`Protocol` as Special))")
    // Constants without a special meaning keep the same output.
    assertThat(code).contains("@CustomDefaulted(defaultValue: Special.PLAIN)")
  }

  @Test fun proto3EnumWithSpecialZeroConstantHasDefaultedValue() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto3";
        |
        |enum Special {
        |  Type = 0;
        |  OTHER = 1;
        |}
        """.trimMargin(),
      )
    }

    val code = schema.generateSwift("Special")

    assertThat(code).contains("case `Type` = 0")
    assertThat(code).contains("(.`Type` as Special)\n")
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
