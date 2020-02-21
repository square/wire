/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema.internal.parser

import com.squareup.wire.schema.Field.Label.OPTIONAL
import com.squareup.wire.schema.Field.Label.REPEATED
import com.squareup.wire.schema.Field.Label.REQUIRED
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.ProtoFile.Syntax.PROTO_3
import com.squareup.wire.schema.internal.MAX_TAG_VALUE
import com.squareup.wire.schema.internal.parser.OptionElement.Kind
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Test
import java.util.Arrays
import java.util.LinkedHashMap

class ProtoParserTest {
  internal var location = Location.get("file.proto")

  @Test
  fun typeParsing() {
    val proto = """
        |message Types {
        |  required any f1 = 1;
        |  required bool f2 = 2;
        |  required bytes f3 = 3;
        |  required double f4 = 4;
        |  required float f5 = 5;
        |  required fixed32 f6 = 6;
        |  required fixed64 f7 = 7;
        |  required int32 f8 = 8;
        |  required int64 f9 = 9;
        |  required sfixed32 f10 = 10;
        |  required sfixed64 f11 = 11;
        |  required sint32 f12 = 12;
        |  required sint64 f13 = 13;
        |  required string f14 = 14;
        |  required uint32 f15 = 15;
        |  required uint64 f16 = 16;
        |  map<string, bool> f17 = 17;
        |  map<arbitrary, nested.nested> f18 = 18;
        |  required arbitrary f19 = 19;
        |  required nested.nested f20 = 20;
        |}
        """.trimMargin()

    val expected = ProtoFileElement(
        location = location,
        types = listOf(
            MessageElement(
                location = location.at(1, 1),
                name = "Types",
                fields = listOf(
                    FieldElement(
                        location = location.at(2, 3),
                        label = REQUIRED,
                        type = "any",
                        name = "f1",
                        tag = 1
                    ),
                    FieldElement(
                        location = location.at(3, 3),
                        label = REQUIRED,
                        type = "bool",
                        name = "f2",
                        tag = 2
                    ),
                    FieldElement(
                        location = location.at(4, 3),
                        label = REQUIRED,
                        type = "bytes",
                        name = "f3",
                        tag = 3
                    ),
                    FieldElement(
                        location = location.at(5, 3),
                        label = REQUIRED,
                        type = "double",
                        name = "f4",
                        tag = 4
                    ),
                    FieldElement(
                        location = location.at(6, 3),
                        label = REQUIRED,
                        type = "float",
                        name = "f5",
                        tag = 5
                    ),
                    FieldElement(
                        location = location.at(7, 3),
                        label = REQUIRED,
                        type = "fixed32",
                        name = "f6",
                        tag = 6
                    ),
                    FieldElement(
                        location = location.at(8, 3),
                        label = REQUIRED,
                        type = "fixed64",
                        name = "f7",
                        tag = 7
                    ),
                    FieldElement(
                        location = location.at(9, 3),
                        label = REQUIRED,
                        type = "int32",
                        name = "f8",
                        tag = 8
                    ),
                    FieldElement(
                        location = location.at(10, 3),
                        label = REQUIRED,
                        type = "int64",
                        name = "f9",
                        tag = 9
                    ),
                    FieldElement(
                        location = location.at(11, 3),
                        label = REQUIRED,
                        type = "sfixed32",
                        name = "f10",
                        tag = 10
                    ),
                    FieldElement(
                        location = location.at(12, 3),
                        label = REQUIRED,
                        type = "sfixed64",
                        name = "f11",
                        tag = 11
                    ),
                    FieldElement(
                        location = location.at(13, 3),
                        label = REQUIRED,
                        type = "sint32",
                        name = "f12",
                        tag = 12
                    ),
                    FieldElement(
                        location = location.at(14, 3),
                        label = REQUIRED,
                        type = "sint64",
                        name = "f13",
                        tag = 13
                    ),
                    FieldElement(
                        location = location.at(15, 3),
                        label = REQUIRED,
                        type = "string",
                        name = "f14",
                        tag = 14
                    ),
                    FieldElement(
                        location = location.at(16, 3),
                        label = REQUIRED,
                        type = "uint32",
                        name = "f15",
                        tag = 15
                    ),
                    FieldElement(
                        location = location.at(17, 3),
                        label = REQUIRED,
                        type = "uint64",
                        name = "f16",
                        tag = 16
                    ),
                    FieldElement(
                        location = location.at(18, 3),
                        type = "map<string, bool>",
                        name = "f17",
                        tag = 17
                    ),
                    FieldElement(
                        location = location.at(19, 3),
                        type = "map<arbitrary, nested.nested>",
                        name = "f18",
                        tag = 18
                    ),
                    FieldElement(
                        location = location.at(20, 3),
                        label = REQUIRED,
                        type = "arbitrary",
                        name = "f19",
                        tag = 19
                    ),
                    FieldElement(
                        location = location.at(21, 3),
                        label = REQUIRED,
                        type = "nested.nested",
                        name = "f20",
                        tag = 20
                    )
                )
            )
        )
    )

    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun mapWithLabelThrows() {
    try {
      ProtoParser.parse(location, "message Hey { required map<string, string> a = 1; }")
      fail()
    } catch (e: IllegalStateException) {
      assertThat(e).hasMessage(
          "Syntax error in file.proto at 1:15: 'map' type cannot have label"
      )
    }

    try {
      ProtoParser.parse(location, "message Hey { optional map<string, string> a = 1; }")
      fail()
    } catch (e: IllegalStateException) {
      assertThat(e).hasMessage(
          "Syntax error in file.proto at 1:15: 'map' type cannot have label"
      )
    }

    try {
      ProtoParser.parse(location, "message Hey { repeated map<string, string> a = 1; }")
      fail()
    } catch (e: IllegalStateException) {
      assertThat(e).hasMessage(
          "Syntax error in file.proto at 1:15: 'map' type cannot have label"
      )
    }
  }

  /** It looks like an option, but 'default' is special. It's missing from descriptor.proto!  */
  @Test
  fun defaultFieldOptionIsSpecial() {
    val proto = """
        |message Message {
        |  required string a = 1 [default = "b", faulted = "c"];
        |}
        |""".trimMargin()

    val expected = ProtoFileElement(
        location = location,
        types = listOf(
            MessageElement(
                location = location.at(1, 1),
                name = "Message",
                fields = listOf(
                    FieldElement(
                        location = location.at(2, 3),
                        label = REQUIRED,
                        type = "string",
                        name = "a",
                        defaultValue = "b",
                        options = listOf(OptionElement.create("faulted", Kind.STRING, "c")),
                        tag = 1
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun singleLineComment() {
    val proto = """
        |// Test all the things!
        |message Test {}
        """.trimMargin()
    val parsed = ProtoParser.parse(location, proto)
    val type = parsed.types[0]
    assertThat(type.documentation).isEqualTo("Test all the things!")
  }

  @Test
  fun multipleSingleLineComments() {
    val proto = """
        |// Test all
        |// the things!
        |message Test {}
        """.trimMargin()
    val expected = """
        |Test all
        |the things!
        """.trimMargin()

    val parsed = ProtoParser.parse(location, proto)
    val type = parsed.types[0]
    assertThat(type.documentation).isEqualTo(expected)
  }

  @Test
  fun singleLineJavadocComment() {
    val proto = """
        |/** Test */
        |message Test {}
        |""".trimMargin()
    val parsed = ProtoParser.parse(location, proto)
    val type = parsed.types[0]
    assertThat(type.documentation).isEqualTo("Test")
  }

  @Test
  fun multilineJavadocComment() {
    val proto = """
        |/**
        | * Test
        | *
        | * Foo
        | */
        |message Test {}
        |""".trimMargin()
    val expected = """
        |Test
        |
        |Foo
        """.trimMargin()
    val parsed = ProtoParser.parse(location, proto)
    val type = parsed.types[0]
    assertThat(type.documentation).isEqualTo(expected)
  }

  @Test
  fun multipleSingleLineCommentsWithLeadingWhitespace() {
    val proto = """
        |// Test
        |//   All
        |//     The
        |//       Things!
        |message Test {}
        """.trimMargin()
    val expected = """
        |Test
        |  All
        |    The
        |      Things!
        """.trimMargin()
    val parsed = ProtoParser.parse(location, proto)
    val type = parsed.types[0]
    assertThat(type.documentation).isEqualTo(expected)
  }

  @Test
  fun multilineJavadocCommentWithLeadingWhitespace() {
    val proto = """
        |/**
        | * Test
        | *   All
        | *     The
        | *       Things!
        | */
        |message Test {}
        """.trimMargin()
    val expected = """
        |Test
        |  All
        |    The
        |      Things!
        """.trimMargin()
    val parsed = ProtoParser.parse(location, proto)
    val type = parsed.types[0]
    assertThat(type.documentation).isEqualTo(expected)
  }

  @Test
  fun multilineJavadocCommentWithoutLeadingAsterisks() {
    // We do not honor leading whitespace when the comment lacks leading asterisks.
    val proto = """
        |/**
        | Test
        |   All
        |     The
        |       Things!
        | */
        |message Test {}
        """.trimMargin()
    val expected = """
        |Test
        |All
        |The
        |Things!
        """.trimMargin()
    val parsed = ProtoParser.parse(location, proto)
    val type = parsed.types[0]
    assertThat(type.documentation).isEqualTo(expected)
  }

  @Test
  fun messageFieldTrailingComment() {
    // Trailing message field comment.
    val proto = """
        |message Test {
        |  optional string name = 1; // Test all the things!
        |}
        """.trimMargin()
    val parsed = ProtoParser.parse(location, proto)
    val message = parsed.types[0] as MessageElement
    val field = message.fields[0]
    assertThat(field.documentation).isEqualTo("Test all the things!")
  }

  @Test
  fun messageFieldLeadingAndTrailingCommentAreCombined() {
    val proto = """
        |message Test {
        |  // Test all...
        |  optional string name = 1; // ...the things!
        |}
        """.trimMargin()
    val parsed = ProtoParser.parse(location, proto)
    val message = parsed.types[0] as MessageElement
    val field = message.fields[0]
    assertThat(field.documentation).isEqualTo("Test all...\n...the things!")
  }

  @Test
  fun trailingCommentNotAssignedToFollowingField() {
    val proto = """
        |message Test {
        |  optional string first_name = 1; // Testing!
        |  optional string last_name = 2;
        |}
        """.trimMargin()
    val parsed = ProtoParser.parse(location, proto)
    val message = parsed.types[0] as MessageElement
    val field1 = message.fields[0]
    assertThat(field1.documentation).isEqualTo("Testing!")
    val field2 = message.fields[1]
    assertThat(field2.documentation).isEqualTo("")
  }

  @Test
  fun enumValueTrailingComment() {
    val proto = """
        |enum Test {
        |  FOO = 1; // Test all the things!
        |}
        """.trimMargin()
    val parsed = ProtoParser.parse(location, proto)
    val enumElement = parsed.types[0] as EnumElement
    val value = enumElement.constants[0]
    assertThat(value.documentation).isEqualTo("Test all the things!")
  }

  @Test
  fun trailingSinglelineComment() {
    val proto = """
        |enum Test {
        |  FOO = 1; /* Test all the things!  */
        |  BAR = 2;/*Test all the things!*/
        |}
        """.trimMargin()
    val parsed = ProtoParser.parse(location, proto)
    val enumElement = parsed.types[0] as EnumElement
    val foo = enumElement.constants[0]
    assertThat(foo.documentation).isEqualTo("Test all the things!")
    val bar = enumElement.constants[1]
    assertThat(bar.documentation).isEqualTo("Test all the things!")
  }

  @Test
  fun trailingMultilineComment() {
    val proto = """
        |enum Test {
        |  FOO = 1; /* Test all the
        |things! */
        |}
        """.trimMargin()
    val parsed = ProtoParser.parse(location, proto)
    val enumElement = parsed.types[0] as EnumElement
    val value = enumElement.constants[0]
    assertThat(value.documentation).isEqualTo("Test all the\nthings!")
  }

  @Test
  fun trailingMultilineCommentMustBeLastOnLineThrows() {
    val proto = """
        |enum Test {
        |  FOO = 1; /* Test all the things! */ BAR = 2;
        |}
        """.trimMargin()
    try {
      ProtoParser.parse(location, proto)
    } catch (e: IllegalStateException) {
      assertThat(e).hasMessage(
          "Syntax error in file.proto at 2:40: no syntax may follow trailing comment"
      )
    }
  }

  @Test
  fun invalidTrailingComment() {
    val proto = """
        |enum Test {
        |  FOO = 1; /
        |}
        """.trimMargin()
    try {
      ProtoParser.parse(location, proto)
    } catch (e: IllegalStateException) {
      assertThat(e).hasMessage(
          "Syntax error in file.proto at 2:13: expected '//' or '/*'"
      )
    }
  }

  @Test
  fun enumValueLeadingAndTrailingCommentsAreCombined() {
    val proto = """
        |enum Test {
        |  // Test all...
        |  FOO = 1; // ...the things!
        |}
        """.trimMargin()
    val parsed = ProtoParser.parse(location, proto)
    val enumElement = parsed.types[0] as EnumElement
    val value = enumElement.constants[0]
    assertThat(value.documentation).isEqualTo("Test all...\n...the things!")
  }

  @Test
  fun trailingCommentNotCombinedWhenEmpty() {
    // Can't use raw strings here; otherwise, the formatter removes the trailing whitespace on line 3.
    val proto = "enum Test {\n" +
        "  // Test all...\n" +
        "  FOO = 1; //       \n" +
        "}"
    val parsed = ProtoParser.parse(location, proto)
    val enumElement = parsed.types[0] as EnumElement
    val value = enumElement.constants[0]
    assertThat(value.documentation).isEqualTo("Test all...")
  }

  @Test
  fun syntaxNotRequired() {
    val proto = "message Foo {}"
    val parsed = ProtoParser.parse(location, proto)
    assertThat(parsed.syntax).isNull()
  }

  @Test
  fun syntaxSpecified() {
    val proto = """
        |syntax = "proto3";
        |message Foo {}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        syntax = PROTO_3,
        types = listOf(
            MessageElement(
                location = location.at(2, 1),
                name = "Foo"
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun invalidSyntaxValueThrows() {
    val proto = """
        |syntax = "proto4";
        |message Foo {}
        """.trimMargin()
    try {
      ProtoParser.parse(location, proto)
    } catch (e: IllegalStateException) {
      assertThat(e).hasMessage("Syntax error in file.proto at 1:1: unexpected syntax: proto4")
    }
  }

  @Test
  fun syntaxInWrongContextThrows() {
    val proto = """
        |message Foo {
        |  syntax = "proto2";
        |}
        """.trimMargin()
    try {
      ProtoParser.parse(location, proto)
    } catch (e: IllegalStateException) {
      assertThat(e).hasMessage("Syntax error in file.proto at 2:3: 'syntax' in MESSAGE")
    }
  }

  @Test
  fun syntaxNotFirstDeclarationThrows() {
    val proto = """
        |message Foo {}
        |syntax = "proto3";
        """.trimMargin()
    try {
      ProtoParser.parse(location, proto)
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage(
          "Syntax error in file.proto at 2:1: 'syntax' element must be the first declaration in a file"
      )
    }
  }

  @Test
  fun syntaxMayFollowCommentsAndEmptyLines() {
    val proto = """
        |/* comment 1 */
        |// comment 2
        |
        |syntax = "proto3";
        |message Foo {}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        syntax = PROTO_3,
        types = listOf(
            MessageElement(
                location = location.at(5, 1),
                name = "Foo"
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun proto3MessageFieldsDoNotRequireLabels() {
    val proto = """
        |syntax = "proto3";
        |message Message {
        |  string a = 1;
        |  int32 b = 2;
        |}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        syntax = PROTO_3,
        types = listOf(
            MessageElement(
                location = location.at(2, 1),
                name = "Message",
                fields = listOf(
                    FieldElement(
                        location = location.at(3, 3),
                        type = "string",
                        name = "a",
                        tag = 1
                    ),
                    FieldElement(
                        location = location.at(4, 3),
                        type = "int32",
                        name = "b",
                        tag = 2
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun proto3ExtensionFieldsDoNotRequireLabels() {
    val proto = """
        |syntax = "proto3";
        |message Message {
        |}
        |extend Message {
        |  string a = 1;
        |  int32 b = 2;
        |}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        syntax = PROTO_3,
        types = listOf(
            MessageElement(
                location = location.at(2, 1),
                name = "Message"
            )
        ),
        extendDeclarations = listOf(
            ExtendElement(
                location = location.at(4, 1),
                name = "Message",
                fields = listOf(
                    FieldElement(
                        location = location.at(5, 3),
                        type = "string",
                        name = "a",
                        tag = 1
                    ),
                    FieldElement(
                        location = location.at(6, 3),
                        type = "int32",
                        name = "b",
                        tag = 2
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun proto3MessageFieldsForbidOptional() {
    val proto = """
        |syntax = "proto3";
        |message Message {
        |  optional string a = 1;
        |}
        """.trimMargin()
    try {
      ProtoParser.parse(location, proto)
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage(
          "Syntax error in file.proto at 3:3: 'optional' label forbidden in proto3 field declarations"
      )
    }
  }

  @Test
  fun proto3MessageFieldsForbidRequired() {
    val proto = """
        |syntax = "proto3";
        |message Message {
        |  required string a = 1;
        |}
        """.trimMargin()
    try {
      ProtoParser.parse(location, proto)
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage(
          "Syntax error in file.proto at 3:3: 'required' label forbidden in proto3 field declarations"
      )
    }
  }

  @Test
  fun proto3ExtensionFieldsForbidsOptional() {
    val proto = """
        |syntax = "proto3";
        |message Message {
        |}
        |extend Message {
        |  optional string a = 1;
        |}
        """.trimMargin()
    try {
      ProtoParser.parse(location, proto)
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage(
          "Syntax error in file.proto at 5:3: 'optional' label forbidden in proto3 field declarations"
      )
    }
  }

  @Test
  fun proto3ExtensionFieldsForbidsRequired() {
    val proto = """
        |syntax = "proto3";
        |message Message {
        |}
        |extend Message {
        |  required string a = 1;
        |}
        """.trimMargin()
    try {
      ProtoParser.parse(location, proto)
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage(
          "Syntax error in file.proto at 5:3: 'required' label forbidden in proto3 field declarations"
      )
    }
  }

  @Test
  fun proto3MessageFieldsPermitRepeated() {
    val proto = """
        |syntax = "proto3";
        |message Message {
        |  repeated string a = 1;
        |}
        """.trimMargin()

    val expected = ProtoFileElement(
        location = location,
        syntax = PROTO_3,
        types = listOf(
            MessageElement(
                location = location.at(2, 1),
                name = "Message",
                fields = listOf(
                    FieldElement(
                        location = location.at(3, 3),
                        label = REPEATED,
                        type = "string",
                        name = "a",
                        tag = 1
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun proto3ExtensionFieldsPermitRepeated() {
    val proto = """
        |syntax = "proto3";
        |message Message {
        |}
        |extend Message {
        |  repeated string a = 1;
        |}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        syntax = PROTO_3,
        types = listOf(
            MessageElement(
                location = location.at(2, 1),
                name = "Message"
            )
        ),
        extendDeclarations = listOf(
            ExtendElement(
                location = location.at(4, 1),
                name = "Message",
                fields = listOf(
                    FieldElement(
                        location = location.at(5, 3),
                        label = REPEATED,
                        type = "string",
                        name = "a",
                        tag = 1
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun parseMessageAndFields() {
    val proto = """
        |message SearchRequest {
        |  required string query = 1;
        |  optional int32 page_number = 2;
        |  optional int32 result_per_page = 3;
        |}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        types = listOf(
            MessageElement(
                location = location.at(1, 1),
                name = "SearchRequest",
                fields = listOf(
                    FieldElement(
                        location = location.at(2, 3),
                        label = REQUIRED,
                        type = "string",
                        name = "query",
                        tag = 1
                    ),
                    FieldElement(
                        location = location.at(3, 3),
                        label = OPTIONAL,
                        type = "int32",
                        name = "page_number",
                        tag = 2
                    ),
                    FieldElement(
                        location = location.at(4, 3),
                        label = OPTIONAL,
                        type = "int32",
                        name = "result_per_page",
                        tag = 3
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun group() {
    val proto = """
        |message SearchResponse {
        |  repeated group Result = 1 {
        |    required string url = 2;
        |    optional string title = 3;
        |    repeated string snippets = 4;
        |  }
        |}
        """.trimMargin()
    val message = MessageElement(
        location = location.at(1, 1),
        name = "SearchResponse",
        groups = listOf(
            GroupElement(
                location = location.at(2, 3),
                label = REPEATED,
                name = "Result",
                tag = 1,
                fields = listOf(
                    FieldElement(
                        location = location.at(3, 5),
                        label = REQUIRED,
                        type = "string",
                        name = "url",
                        tag = 2
                    ),
                    FieldElement(
                        location = location.at(4, 5),
                        label = OPTIONAL,
                        type = "string",
                        name = "title",
                        tag = 3
                    ),
                    FieldElement(
                        location = location.at(5, 5),
                        label = REPEATED,
                        type = "string",
                        name = "snippets",
                        tag = 4
                    )
                )
            )
        )
    )
    val expected = ProtoFileElement(
        location = location,
        types = listOf(message)
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun parseMessageAndOneOf() {
    val proto = """
        |message SearchRequest {
        |  required string query = 1;
        |  oneof page_info {
        |    int32 page_number = 2;
        |    int32 result_per_page = 3;
        |  }
        |}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        types = listOf(
            MessageElement(
                location = location.at(1, 1),
                name = "SearchRequest",
                fields = listOf(
                    FieldElement(
                        location = location.at(2, 3),
                        label = REQUIRED,
                        type = "string",
                        name = "query",
                        tag = 1
                    )
                ),
                oneOfs = listOf(
                    OneOfElement(
                        name = "page_info",
                        fields = listOf(
                            FieldElement(
                                location = location.at(4, 5),
                                type = "int32",
                                name = "page_number",
                                tag = 2
                            ),
                            FieldElement(
                                location = location.at(5, 5),
                                type = "int32",
                                name = "result_per_page",
                                tag = 3
                            )
                        )
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun parseMessageAndOneOfWithGroup() {
    val proto = """
        |message SearchRequest {
        |  required string query = 1;
        |  oneof page_info {
        |    int32 page_number = 2;
        |    group Stuff = 3 {
        |      optional int32 result_per_page = 4;
        |      optional int32 page_count = 5;
        |    }
        |  }
        |}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        types = listOf(
            MessageElement(
                location = location.at(1, 1),
                name = "SearchRequest",
                fields = listOf(
                    FieldElement(
                        location = location.at(2, 3),
                        label = REQUIRED,
                        type = "string",
                        name = "query",
                        tag = 1
                    )
                ),
                oneOfs = listOf(
                    OneOfElement(
                        name = "page_info",
                        fields = listOf(
                            FieldElement(
                                location = location.at(4, 5),
                                type = "int32",
                                name = "page_number",
                                tag = 2
                            )
                        ),
                        groups = listOf(
                            GroupElement(
                                location = location.at(5, 5),
                                name = "Stuff",
                                tag = 3,
                                fields = listOf(
                                    FieldElement(
                                        location = location.at(6, 7),
                                        label = OPTIONAL,
                                        type = "int32",
                                        name = "result_per_page",
                                        tag = 4
                                    ),
                                    FieldElement(
                                        location = location.at(7, 7),
                                        label = OPTIONAL,
                                        type = "int32",
                                        name = "page_count",
                                        tag = 5
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun parseEnum() {
    val proto = """
        |/**
        | * What's on my waffles.
        | * Also works on pancakes.
        | */
        |enum Topping {
        |  FRUIT = 1;
        |  /** Yummy, yummy cream. */
        |  CREAM = 2;
        |
        |  // Quebec Maple syrup
        |  SYRUP = 3;
        |}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        types = listOf(
            EnumElement(
                location = location.at(5, 1),
                name = "Topping",
                documentation = "What's on my waffles.\nAlso works on pancakes.",
                constants = listOf(
                    EnumConstantElement(
                        location = location.at(6, 3),
                        name = "FRUIT",
                        tag = 1
                    ),
                    EnumConstantElement(
                        location = location.at(8, 3),
                        name = "CREAM",
                        tag = 2,
                        documentation = "Yummy, yummy cream."
                    ),
                    EnumConstantElement(
                        location = location.at(11, 3),
                        name = "SYRUP",
                        tag = 3,
                        documentation = "Quebec Maple syrup"
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun parseEnumWithOptions() {
    val proto = """
        |/**
        | * What's on my waffles.
        | * Also works on pancakes.
        | */
        |enum Topping {
        |  option(max_choices) = 2;
        |
        |  FRUIT = 1[(healthy) = true];
        |  /** Yummy, yummy cream. */
        |  CREAM = 2;
        |
        |  // Quebec Maple syrup
        |  SYRUP = 3;
        |}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        types = listOf(
            EnumElement(
                location = location.at(5, 1),
                name = "Topping",
                documentation = "What's on my waffles.\nAlso works on pancakes.",
                options = listOf(OptionElement.create("max_choices", Kind.NUMBER, "2", true)),
                constants = listOf(
                    EnumConstantElement(
                        location = location.at(8, 3),
                        name = "FRUIT",
                        tag = 1,
                        options = listOf(
                            OptionElement.create("healthy", Kind.BOOLEAN, "true", true)
                        )
                    ),
                    EnumConstantElement(
                        location = location.at(10, 3),
                        name = "CREAM",
                        tag = 2,
                        documentation = "Yummy, yummy cream."
                    ),
                    EnumConstantElement(
                        location = location.at(13, 3),
                        name = "SYRUP",
                        tag = 3,
                        documentation = "Quebec Maple syrup"
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun packageDeclaration() {
    val proto = """
        |package google.protobuf;
        |option java_package = "com.google.protobuf";
        |
        |// The protocol compiler can output a FileDescriptorSet containing the .proto
        |// files it parses.
        |message FileDescriptorSet {
        |}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        packageName = "google.protobuf",
        types = listOf(
            MessageElement(
                location = location.at(6, 1),
                name = "FileDescriptorSet",
                documentation = "The protocol compiler can output a FileDescriptorSet containing the .proto\nfiles it parses."
            )
        ),
        options = listOf(OptionElement.create("java_package", Kind.STRING, "com.google.protobuf"))
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun nestingInMessage() {
    val proto = """
        |message FieldOptions {
        |  optional CType ctype = 1[old_default = STRING, deprecated = true];
        |  enum CType {
        |    STRING = 0[(opt_a) = 1, (opt_b) = 2];
        |  };
        |  // Clients can define custom options in extensions of this message. See above.
        |  extensions 500;
        |  extensions 1000 to max;
        |}
        """.trimMargin()
    val enumElement = EnumElement(
        location = location.at(3, 3),
        name = "CType",
        constants = listOf(
            EnumConstantElement(
                location = location.at(4, 5),
                name = "STRING",
                tag = 0,
                options = listOf(
                    OptionElement.create("opt_a", Kind.NUMBER, "1", true),
                    OptionElement.create("opt_b", Kind.NUMBER, "2", true)
                )
            )
        )
    )
    val field = FieldElement(
        location = location.at(2, 3),
        label = OPTIONAL,
        type = "CType",
        name = "ctype",
        tag = 1,
        options = listOf(
            OptionElement.create("old_default", Kind.ENUM, "STRING"),
            OptionElement.create("deprecated", Kind.BOOLEAN, "true")
        )
    )
    assertThat(field.options)
        .containsOnly(
            OptionElement.create("old_default", Kind.ENUM, "STRING"),
            OptionElement.create("deprecated", Kind.BOOLEAN, "true")
        )

    val messageElement = MessageElement(
        location = location.at(1, 1),
        name = "FieldOptions",
        fields = listOf(field),
        nestedTypes = listOf(enumElement),
        extensions = listOf(
            ExtensionsElement(
                location = location.at(7, 3),
                documentation = "Clients can define custom options in extensions of this message. See above.",
                values = listOf(500)
            ),
            ExtensionsElement(location.at(8, 3), "", listOf(1000..MAX_TAG_VALUE))
        )
    )
    val expected = ProtoFileElement(
        location = location,
        types = listOf(messageElement)
    )
    val actual = ProtoParser.parse(location, proto)
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun multiRangesExtensions() {
    val proto = """
        |message MeGustaExtensions {
        |  extensions 1, 5 to 200, 500, 1000 to max;
        |}
        """.trimMargin()
    val messageElement = MessageElement(
        location = location.at(1, 1),
        name = "MeGustaExtensions",
        fields = emptyList(),
        nestedTypes = emptyList(),
        extensions = listOf(
            ExtensionsElement(
                location = location.at(2, 3),
                values = listOf(1, 5..200, 500, 1000..MAX_TAG_VALUE)
            )
        )
    )
    val expected = ProtoFileElement(
        location = location,
        types = listOf(messageElement)
    )
    val actual = ProtoParser.parse(location, proto)
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun optionParentheses() {
    val proto = """
        |message Chickens {
        |  optional bool koka_ko_koka_ko = 1[old_default = true];
        |  optional bool coodle_doodle_do = 2[(delay) = 100, old_default = false];
        |  optional bool coo_coo_ca_cha = 3[old_default = true, (delay) = 200];
        |  optional bool cha_chee_cha = 4;
        |}
        """.trimMargin()

    val expected = ProtoFileElement(
        location = location,
        types = listOf(
            MessageElement(
                location = location.at(1, 1),
                name = "Chickens",
                fields = listOf(
                    FieldElement(
                        location = location.at(2, 3),
                        label = OPTIONAL,
                        type = "bool",
                        name = "koka_ko_koka_ko",
                        tag = 1,
                        options = listOf(
                            OptionElement.create("old_default", Kind.BOOLEAN, "true")
                        )
                    ),
                    FieldElement(
                        location = location.at(3, 3),
                        label = OPTIONAL,
                        type = "bool",
                        name = "coodle_doodle_do",
                        tag = 2,
                        options = listOf(
                            OptionElement.create("delay", Kind.NUMBER, "100", true),
                            OptionElement.create("old_default", Kind.BOOLEAN, "false")
                        )
                    ),
                    FieldElement(
                        location = location.at(4, 3),
                        label = OPTIONAL,
                        type = "bool",
                        name = "coo_coo_ca_cha",
                        tag = 3,
                        options = listOf(
                            OptionElement.create("old_default", Kind.BOOLEAN, "true"),
                            OptionElement.create("delay", Kind.NUMBER, "200", true)
                        )
                    ),
                    FieldElement(
                        location = location.at(5, 3),
                        label = OPTIONAL,
                        type = "bool",
                        name = "cha_chee_cha",
                        tag = 4
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun imports() {
    val proto = "import \"src/test/resources/unittest_import.proto\";\n"
    val expected = ProtoFileElement(
        location = location,
        imports = listOf("src/test/resources/unittest_import.proto")
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun publicImports() {
    val proto = "import public \"src/test/resources/unittest_import.proto\";\n"
    val expected = ProtoFileElement(
        location = location,
        publicImports = listOf("src/test/resources/unittest_import.proto")
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun extend() {
    val proto = """
        |// Extends Foo
        |extend Foo {
        |  optional int32 bar = 126;
        |}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        extendDeclarations = listOf(
            ExtendElement(
                location = location.at(2, 1),
                name = "Foo",
                documentation = "Extends Foo",
                fields = listOf(
                    FieldElement(
                        location = location.at(3, 3),
                        label = OPTIONAL,
                        type = "int32",
                        name = "bar",
                        tag = 126
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun extendInMessage() {
    val proto = """
        |message Bar {
        |  extend Foo {
        |    optional Bar bar = 126;
        |  }
        |}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        types = listOf(MessageElement(location = location.at(1, 1), name = "Bar")),
        extendDeclarations = listOf(
            ExtendElement(
                location = location.at(2, 3),
                name = "Foo",
                fields = listOf(
                    FieldElement(
                        location = location.at(3, 5),
                        label = OPTIONAL,
                        type = "Bar",
                        name = "bar",
                        tag = 126
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun extendInMessageWithPackage() {
    val proto = """
        |package kit.kat;
        |
        |message Bar {
        |  extend Foo {
        |    optional Bar bar = 126;
        |  }
        |}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        packageName = "kit.kat",
        types = listOf(
            MessageElement(location = location.at(3, 1), name = "Bar")

        ),
        extendDeclarations = listOf(
            ExtendElement(
                location = location.at(4, 3),
                name = "Foo",
                fields = listOf(
                    FieldElement(
                        location = location.at(5, 5),
                        label = OPTIONAL,
                        type = "Bar",
                        name = "bar",
                        tag = 126
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun fqcnExtendInMessage() {
    val proto = """
        |message Bar {
        |  extend example.Foo {
        |    optional Bar bar = 126;
        |  }
        |}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        types = listOf(MessageElement(location = location.at(1, 1), name = "Bar")),
        extendDeclarations = listOf(
            ExtendElement(
                location = location.at(2, 3),
                name = "example.Foo",
                fields = listOf(
                    FieldElement(
                        location = location.at(3, 5),
                        label = OPTIONAL,
                        type = "Bar",
                        name = "bar",
                        tag = 126
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun fqcnExtendInMessageWithPackage() {
    val proto = """
        |package kit.kat;
        |
        |message Bar {
        |  extend example.Foo {
        |    optional Bar bar = 126;
        |  }
        |}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        packageName = "kit.kat",
        types = listOf(
            MessageElement(location = location.at(3, 1), name = "Bar")

        ),
        extendDeclarations = listOf(
            ExtendElement(
                location = location.at(4, 3),
                name = "example.Foo",
                fields = listOf(
                    FieldElement(
                        location = location.at(5, 5),
                        label = OPTIONAL,
                        type = "Bar",
                        name = "bar",
                        tag = 126
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun defaultFieldWithParen() {
    val proto = """
        |message Foo {
        |  optional string claim_token = 2[(squareup.redacted) = true];
        |}
        """.trimMargin()
    val field = FieldElement(
        location = location.at(2, 3),
        label = OPTIONAL,
        type = "string",
        name = "claim_token",
        tag = 2,
        options = listOf(OptionElement.create("squareup.redacted", Kind.BOOLEAN, "true", true))
    )
    assertThat(field.options)
        .containsOnly(OptionElement.create("squareup.redacted", Kind.BOOLEAN, "true", true))

    val messageElement = MessageElement(
        location = location.at(1, 1),
        name = "Foo",
        fields = listOf(field)
    )
    val expected = ProtoFileElement(
        location = location,
        types = listOf(messageElement)
    )
    assertThat(ProtoParser.parse(location, proto))
        .isEqualTo(expected)
  }

  // Parse \a, \b, \f, \n, \r, \t, \v, \[0-7]{1-3}, and \[xX]{0-9a-fA-F]{1,2}
  @Test
  fun defaultFieldWithStringEscapes() {
    val proto = """
        |message Foo {
        |  optional string name = 1 [
        |    x = "\a\b\f\n\r\t\v\1f\01\001\11\011\111\xe\Xe\xE\xE\x41\X41"
        |  ];
        |}
        """.trimMargin()
    val field = FieldElement(
        location = location.at(2, 3),
        label = OPTIONAL,
        type = "string",
        name = "name",
        tag = 1,
        options = listOf(
            OptionElement.create(
                "x", Kind.STRING,
                "\u0007\b\u000C\n\r\t\u000b\u0001f\u0001\u0001\u0009\u0009I\u000e\u000e\u000e\u000eAA"
            )
        )
    )
    assertThat(field.options)
        .containsOnly(
            OptionElement.create(
                "x", Kind.STRING,
                "\u0007\b\u000C\n\r\t\u000b\u0001f\u0001\u0001\u0009\u0009I\u000e\u000e\u000e\u000eAA"
            )
        )

    val messageElement = MessageElement(
        location = location.at(1, 1),
        name = "Foo",
        fields = listOf(field)
    )
    val expected = ProtoFileElement(
        location = location,
        types = listOf(messageElement)
    )
    assertThat(ProtoParser.parse(location, proto))
        .isEqualTo(expected)
  }

  @Test
  fun stringWithSingleQuotes() {
    val proto = """
        |message Foo {
        |  optional string name = 1[default = 'single\"quotes'];
        |}
        """.trimMargin()

    val field = FieldElement(
        location = location.at(2, 3),
        label = OPTIONAL,
        type = "string",
        name = "name",
        tag = 1,
        defaultValue = "single\"quotes"
    )
    val messageElement = MessageElement(
        location = location.at(1, 1),
        name = "Foo",
        fields = listOf(field)
    )
    val expected = ProtoFileElement(
        location = location,
        types = listOf(messageElement)
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun adjacentStringsConcatenated() {
    val proto = """
        |message Foo {
        |  optional string name = 1 [
        |    default = "concat "
        |              'these '
        |              "please"
        |  ];
        |}
        """.trimMargin()

    val field = FieldElement(
        location = location.at(2, 3),
        label = OPTIONAL,
        type = "string",
        name = "name",
        tag = 1,
        defaultValue = "concat these please"
    )
    val messageElement = MessageElement(
        location = location.at(1, 1),
        name = "Foo",
        fields = listOf(field)
    )
    val expected = ProtoFileElement(
        location = location,
        types = listOf(messageElement)
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun invalidHexStringEscape() {
    val proto = """
        |message Foo {
        |  optional string name = 1 [default = "\xW"];
        |}
        """.trimMargin()
    try {
      ProtoParser.parse(location, proto)
      fail()
    } catch (e: IllegalStateException) {
      assertThat(e.message!!.contains("expected a digit after \\x or \\X"))
    }
  }

  @Test
  fun service() {
    val proto = """
        |service SearchService {
        |  option (default_timeout) = 30;
        |
        |  rpc Search (SearchRequest) returns (SearchResponse);
        |  rpc Purchase (PurchaseRequest) returns (PurchaseResponse) {
        |    option (squareup.sake.timeout) = 15;
        |    option (squareup.a.b) = {
        |      value: [
        |        FOO,
        |        BAR
        |      ]
        |    };
        |  }
        |}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        services = listOf(
            ServiceElement(
                location = location.at(1, 1),
                name = "SearchService",
                options = listOf(
                    OptionElement.create("default_timeout", Kind.NUMBER, "30", true)
                ),
                rpcs = listOf(
                    RpcElement(
                        location = location.at(4, 3),
                        name = "Search",
                        requestType = "SearchRequest",
                        responseType = "SearchResponse"
                    ),
                    RpcElement(
                        location = location.at(5, 3),
                        name = "Purchase",
                        requestType = "PurchaseRequest",
                        responseType = "PurchaseResponse",
                        options = listOf(
                            OptionElement.create("squareup.sake.timeout", Kind.NUMBER, "15", true),
                            OptionElement.create(
                                "squareup.a.b",
                                Kind.MAP,
                                mapOf("value" to listOf("FOO", "BAR")),
                                true
                            )
                        )
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun streamingService() {
    val proto = """
        |service RouteGuide {
        |  rpc GetFeature (Point) returns (Feature) {}
        |  rpc ListFeatures (Rectangle) returns (stream Feature) {}
        |  rpc RecordRoute (stream Point) returns (RouteSummary) {}
        |  rpc RouteChat (stream RouteNote) returns (stream RouteNote) {}
        |}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        services = listOf(
            ServiceElement(
                location = location.at(1, 1),
                name = "RouteGuide",
                rpcs = listOf(
                    RpcElement(
                        location = location.at(2, 3),
                        name = "GetFeature",
                        requestType = "Point",
                        responseType = "Feature"
                    ),
                    RpcElement(
                        location = location.at(3, 3),
                        name = "ListFeatures",
                        requestType = "Rectangle",
                        responseType = "Feature",
                        responseStreaming = true
                    ),
                    RpcElement(
                        location = location.at(4, 3),
                        name = "RecordRoute",
                        requestType = "Point",
                        responseType = "RouteSummary",
                        requestStreaming = true
                    ),
                    RpcElement(
                        location = location.at(5, 3),
                        name = "RouteChat",
                        requestType = "RouteNote",
                        responseType = "RouteNote",
                        requestStreaming = true,
                        responseStreaming = true
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun hexTag() {
    val proto = """
        |message HexTag {
        |  required string hex = 0x10;
        |  required string uppercase_x_hex = 0X11;
        |}
        """.trimMargin()
    val expected = ProtoFileElement(
        location = location,
        types = listOf(
            MessageElement(
                location = location.at(1, 1),
                name = "HexTag",
                fields = listOf(
                    FieldElement(
                        location = location.at(2, 3),
                        label = REQUIRED,
                        type = "string",
                        name = "hex",
                        tag = 16
                    ),
                    FieldElement(
                        location = location.at(3, 3),
                        label = REQUIRED,
                        type = "string",
                        name = "uppercase_x_hex",
                        tag = 17
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun structuredOption() {
    val proto = """
        |message ExoticOptions {
        |  option (squareup.one) = {name: "Name", class_name:"ClassName"};
        |  option (squareup.two.a) = {[squareup.options.type]: EXOTIC};
        |  option (squareup.two.b) = {names: ["Foo", "Bar"]};
        |  option (squareup.three) = {x: {y: 1 y: 2 } }; // NOTE: Omitted optional comma
        |  option (squareup.four) = {x: {y: {z: 1 }, y: {z: 2 }}};
        |}
        """.trimMargin()

    val option_one_map = LinkedHashMap<String, String>()
    option_one_map["name"] = "Name"
    option_one_map["class_name"] = "ClassName"
    val option_two_a_map = LinkedHashMap<String, Any>()
    option_two_a_map["[squareup.options.type]"] = "EXOTIC"
    val option_two_b_map = LinkedHashMap<String, List<String>>()
    option_two_b_map["names"] = Arrays.asList("Foo", "Bar")
    val option_three_map = LinkedHashMap<String, Map<String, *>>()
    val option_three_nested_map = LinkedHashMap<String, Any>()
    option_three_nested_map["y"] = Arrays.asList("1", "2")
    option_three_map["x"] = option_three_nested_map

    val option_four_map = LinkedHashMap<String, Map<String, *>>()
    val option_four_map_1 = LinkedHashMap<String, Any>()
    val option_four_map_2_a = LinkedHashMap<String, Any>()
    option_four_map_2_a["z"] = "1"
    val option_four_map_2_b = LinkedHashMap<String, Any>()
    option_four_map_2_b["z"] = "2"
    option_four_map_1["y"] = listOf(option_four_map_2_a, option_four_map_2_b)
    option_four_map["x"] = option_four_map_1

    val expected = ProtoFileElement(
        location = location,
        types = listOf(
            MessageElement(
                location = location.at(1, 1),
                name = "ExoticOptions",
                options = listOf(
                    OptionElement.create("squareup.one", Kind.MAP, option_one_map, true),
                    OptionElement.create(
                        "squareup.two.a", Kind.MAP, option_two_a_map, true
                    ),
                    OptionElement.create(
                        "squareup.two.b", Kind.MAP, option_two_b_map, true
                    ),
                    OptionElement.create(
                        "squareup.three", Kind.MAP, option_three_map, true
                    ),
                    OptionElement.create("squareup.four", Kind.MAP, option_four_map, true)
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun optionsWithNestedMapsAndTrailingCommas() {
    val proto = """
        |message StructuredOption {
        |    optional field.type has_options = 3 [
        |            (option_map) = {
        |                nested_map: {key:"value", key2:["value2a","value2b"]},
        |             },
        |            (option_string) = ["string1","string2"]
        |    ];
        |}
        """.trimMargin()
    val field = FieldElement(
        location = location.at(2, 5),
        label = OPTIONAL,
        type = "field.type",
        name = "has_options",
        tag = 3,
        options = listOf(
            OptionElement.create(
                "option_map",
                Kind.MAP,
                mapOf(
                    "nested_map" to mapOf("key" to "value", "key2" to listOf("value2a", "value2b"))
                ),
                true
            ),
            OptionElement.create("option_string", Kind.LIST, listOf("string1", "string2"), true)
        )
    )
    assertThat(field.options)
        .containsOnly(
            OptionElement.create(
                "option_map",
                Kind.MAP,
                mapOf(
                    "nested_map" to mapOf("key" to "value", "key2" to listOf("value2a", "value2b"))
                ),
                true
            ),
            OptionElement.create("option_string", Kind.LIST, listOf("string1", "string2"), true)
        )

    val expected = MessageElement(
        location = location.at(1, 1),
        name = "StructuredOption",
        fields = listOf(field)
    )
    val protoFile = ProtoFileElement(
        location = location,
        types = listOf(expected)
    )
    assertThat(ProtoParser.parse(location, proto))
        .isEqualTo(protoFile)
  }

  @Test
  fun optionNumericalBounds() {
    val proto = """
        |message Test {
        |  optional int32 default_int32 = 401 [x = 2147483647];
        |  optional uint32 default_uint32 = 402 [x = 4294967295];
        |  optional sint32 default_sint32 = 403 [x = -2147483648];
        |  optional fixed32 default_fixed32 = 404 [x = 4294967295];
        |  optional sfixed32 default_sfixed32 = 405 [x = -2147483648];
        |  optional int64 default_int64 = 406 [x = 9223372036854775807];
        |  optional uint64 default_uint64 = 407 [x = 18446744073709551615];
        |  optional sint64 default_sint64 = 408 [x = -9223372036854775808];
        |  optional fixed64 default_fixed64 = 409 [x = 18446744073709551615];
        |  optional sfixed64 default_sfixed64 = 410 [x = -9223372036854775808];
        |  optional bool default_bool = 411 [x = true];
        |  optional float default_float = 412 [x = 123.456e7];
        |  optional double default_double = 413 [x = 123.456e78];
        |  optional string default_string = 414 [x = "çok\a\b\f\n\r\t\v\1\01\001\17\017\176\x1\x01\x11\X1\X01\X11güzel" ];
        |  optional bytes default_bytes = 415 [x = "çok\a\b\f\n\r\t\v\1\01\001\17\017\176\x1\x01\x11\X1\X01\X11güzel" ];
        |  optional NestedEnum default_nested_enum = 416 [x = A ];
        |}""".trimMargin()
    val expected = ProtoFileElement(
        location = location,
        types = listOf(
            MessageElement(
                location = location.at(1, 1),
                name = "Test",
                fields = listOf(
                    FieldElement(
                        location = location.at(2, 3),
                        label = OPTIONAL,
                        type = "int32",
                        name = "default_int32",
                        tag = 401,
                        options = listOf(OptionElement.create("x", Kind.NUMBER, "2147483647"))
                    ),
                    FieldElement(
                        location = location.at(3, 3),
                        label = OPTIONAL,
                        type = "uint32",
                        name = "default_uint32",
                        tag = 402,
                        options = listOf(OptionElement.create("x", Kind.NUMBER, "4294967295"))
                    ),
                    FieldElement(
                        location = location.at(4, 3),
                        label = OPTIONAL,
                        type = "sint32",
                        name = "default_sint32",
                        tag = 403,
                        options = listOf(OptionElement.create("x", Kind.NUMBER, "-2147483648"))
                    ),
                    FieldElement(
                        location = location.at(5, 3),
                        label = OPTIONAL,
                        type = "fixed32",
                        name = "default_fixed32",
                        tag = 404,
                        options = listOf(OptionElement.create("x", Kind.NUMBER, "4294967295"))
                    ),
                    FieldElement(
                        location = location.at(6, 3),
                        label = OPTIONAL,
                        type = "sfixed32",
                        name = "default_sfixed32",
                        tag = 405,
                        options = listOf(OptionElement.create("x", Kind.NUMBER, "-2147483648"))
                    ),
                    FieldElement(
                        location = location.at(7, 3),
                        label = OPTIONAL,
                        type = "int64",
                        name = "default_int64",
                        tag = 406,
                        options = listOf(
                            OptionElement.create("x", Kind.NUMBER, "9223372036854775807")
                        )
                    ),
                    FieldElement(
                        location = location.at(8, 3),
                        label = OPTIONAL,
                        type = "uint64",
                        name = "default_uint64",
                        tag = 407,
                        options = listOf(
                            OptionElement.create("x", Kind.NUMBER, "18446744073709551615")
                        )
                    ),
                    FieldElement(
                        location = location.at(9, 3),
                        label = OPTIONAL,
                        type = "sint64",
                        name = "default_sint64",
                        tag = 408,
                        options = listOf(
                            OptionElement.create("x", Kind.NUMBER, "-9223372036854775808")
                        )
                    ),
                    FieldElement(
                        location = location.at(10, 3),
                        label = OPTIONAL,
                        type = "fixed64",
                        name = "default_fixed64",
                        tag = 409,
                        options = listOf(
                            OptionElement.create("x", Kind.NUMBER, "18446744073709551615")
                        )
                    ),
                    FieldElement(
                        location = location.at(11, 3),
                        label = OPTIONAL,
                        type = "sfixed64",
                        name = "default_sfixed64",
                        tag = 410,
                        options = listOf(
                            OptionElement.create("x", Kind.NUMBER, "-9223372036854775808")
                        )
                    ),
                    FieldElement(
                        location = location.at(12, 3),
                        label = OPTIONAL,
                        type = "bool",
                        name = "default_bool",
                        tag = 411,
                        options = listOf(OptionElement.create("x", Kind.BOOLEAN, "true"))
                    ),
                    FieldElement(
                        location = location.at(13, 3),
                        label = OPTIONAL,
                        type = "float",
                        name = "default_float",
                        tag = 412,
                        options = listOf(OptionElement.create("x", Kind.NUMBER, "123.456e7"))
                    ),
                    FieldElement(
                        location = location.at(14, 3),
                        label = OPTIONAL,
                        type = "double",
                        name = "default_double",
                        tag = 413,
                        options = listOf(OptionElement.create("x", Kind.NUMBER, "123.456e78"))
                    ),
                    FieldElement(
                        location = location.at(15, 3),
                        label = OPTIONAL,
                        type = "string",
                        name = "default_string",
                        tag = 414,
                        options = listOf(
                            OptionElement.create(
                                "x",
                                Kind.STRING,
                                "çok\u0007\b\u000C\n\r\t\u000b\u0001\u0001\u0001\u000f\u000f~\u0001\u0001\u0011\u0001\u0001\u0011güzel"
                            )
                        )
                    ),
                    FieldElement(
                        location = location.at(17, 3),
                        label = OPTIONAL,
                        type = "bytes",
                        name = "default_bytes",
                        tag = 415,
                        options = listOf(
                            OptionElement.create(
                                "x",
                                Kind.STRING,
                                "çok\u0007\b\u000C\n\r\t\u000b\u0001\u0001\u0001\u000f\u000f~\u0001\u0001\u0011\u0001\u0001\u0011güzel"
                            )
                        )
                    ),
                    FieldElement(
                        location = location.at(19, 3),
                        label = OPTIONAL,
                        type = "NestedEnum",
                        name = "default_nested_enum",
                        tag = 416,
                        options = listOf(OptionElement.create("x", Kind.ENUM, "A"))
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun extensionWithNestedMessage() {
    val proto = """
        |message Foo {
        |  optional int32 bar = 1[
        |      (validation.range).min = 1,
        |      (validation.range).max = 100,
        |      old_default = 20
        |  ];
        |}
        """.trimMargin()
    val field = FieldElement(
        location = location.at(2, 3),
        label = OPTIONAL,
        type = "int32",
        name = "bar",
        tag = 1,
        options = listOf(
            OptionElement.create(
                "validation.range",
                Kind.OPTION,
                OptionElement.create("min", Kind.NUMBER, "1"),
                true
            ),
            OptionElement.create(
                "validation.range",
                Kind.OPTION,
                OptionElement.create("max", Kind.NUMBER, "100"),
                true
            ),
            OptionElement.create("old_default", Kind.NUMBER, "20")
        )
    )
    assertThat(field.options)
        .containsOnly(
            OptionElement.create(
                "validation.range",
                Kind.OPTION,
                OptionElement.create("min", Kind.NUMBER, "1"),
                true
            ),
            OptionElement.create(
                "validation.range",
                Kind.OPTION,
                OptionElement.create("max", Kind.NUMBER, "100"),
                true
            ),
            OptionElement.create("old_default", Kind.NUMBER, "20")
        )

    val expected = MessageElement(
        location = location.at(1, 1),
        name = "Foo",
        fields = listOf(field)
    )
    val protoFile = ProtoFileElement(
        location = location,
        types = listOf(expected)
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(protoFile)
  }

  @Test
  fun reserved() {
    val proto = """
        |message Foo {
        |  reserved 10, 12 to 14, 'foo';
        |}
        """.trimMargin()
    val message = MessageElement(
        location = location.at(1, 1),
        name = "Foo",
        reserveds = listOf(
            ReservedElement(
                location = location.at(2, 3),
                values = listOf(10, 12..14, "foo")
            )
        )
    )
    val expected = ProtoFileElement(
        location = location,
        types = listOf(message)
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun reservedWithComments() {
    val proto = """
        |message Foo {
        |  optional string a = 1; // This is A.
        |  reserved 2; // This is reserved.
        |  optional string c = 3; // This is C.
        |}
        """.trimMargin()

    val message = MessageElement(
        location = location.at(1, 1),
        name = "Foo",
        fields = listOf(
            FieldElement(
                location = location.at(2, 3),
                label = OPTIONAL,
                type = "string",
                name = "a",
                tag = 1,
                documentation = "This is A."
            ),
            FieldElement(
                location = location.at(4, 3),
                label = OPTIONAL,
                type = "string",
                name = "c",
                tag = 3,
                documentation = "This is C."
            )
        ),
        reserveds = listOf(
            ReservedElement(
                location = location.at(3, 3),
                values = listOf(2),
                documentation = "This is reserved."
            )
        )
    )
    val expected = ProtoFileElement(
        location = location,
        types = listOf(message)
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }

  @Test
  fun noWhitespace() {
    val proto = "message C {optional A.B ab = 1;}"
    val expected = ProtoFileElement(
        location = location,
        types = listOf(
            MessageElement(
                location = location.at(1, 1),
                name = "C",
                fields = listOf(
                    FieldElement(
                        location = location.at(1, 12),
                        label = OPTIONAL,
                        type = "A.B",
                        name = "ab",
                        tag = 1
                    )
                )
            )
        )
    )
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected)
  }
}
