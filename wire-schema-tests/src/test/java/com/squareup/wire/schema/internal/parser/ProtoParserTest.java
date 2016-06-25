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
package com.squareup.wire.schema.internal.parser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.internal.Util;
import com.squareup.wire.schema.internal.parser.OptionElement.Kind;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static com.squareup.wire.schema.Field.Label.OPTIONAL;
import static com.squareup.wire.schema.Field.Label.REPEATED;
import static com.squareup.wire.schema.Field.Label.REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class ProtoParserTest {
  Location location = Location.get("file.proto");

  @Test public void typeParsing() {
    String proto = ""
        + "message Types {\n"
        + "  required any f1 = 1;\n"
        + "  required bool f2 = 2;\n"
        + "  required bytes f3 = 3;\n"
        + "  required double f4 = 4;\n"
        + "  required float f5 = 5;\n"
        + "  required fixed32 f6 = 6;\n"
        + "  required fixed64 f7 = 7;\n"
        + "  required int32 f8 = 8;\n"
        + "  required int64 f9 = 9;\n"
        + "  required sfixed32 f10 = 10;\n"
        + "  required sfixed64 f11 = 11;\n"
        + "  required sint32 f12 = 12;\n"
        + "  required sint64 f13 = 13;\n"
        + "  required string f14 = 14;\n"
        + "  required uint32 f15 = 15;\n"
        + "  required uint64 f16 = 16;\n"
        + "  map<string, bool> f17 = 17;\n"
        + "  map<arbitrary, nested.nested> f18 = 18;\n"
        + "  required arbitrary f19 = 19;\n"
        + "  required nested.nested f20 = 20;\n"
        + "}\n";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(1, 1))
                .name("Types")
                .fields(ImmutableList.of(
                    FieldElement.builder(location.at(2, 3))
                        .label(REQUIRED)
                        .type("any")
                        .name("f1")
                        .tag(1)
                        .build(),
                    FieldElement.builder(location.at(3, 3))
                        .label(REQUIRED)
                        .type("bool")
                        .name("f2")
                        .tag(2)
                        .build(),
                    FieldElement.builder(location.at(4, 3))
                        .label(REQUIRED)
                        .type("bytes")
                        .name("f3")
                        .tag(3)
                        .build(),
                    FieldElement.builder(location.at(5, 3))
                        .label(REQUIRED)
                        .type("double")
                        .name("f4")
                        .tag(4)
                        .build(),
                    FieldElement.builder(location.at(6, 3))
                        .label(REQUIRED)
                        .type("float")
                        .name("f5")
                        .tag(5)
                        .build(),
                    FieldElement.builder(location.at(7, 3))
                        .label(REQUIRED)
                        .type("fixed32")
                        .name("f6")
                        .tag(6)
                        .build(),
                    FieldElement.builder(location.at(8, 3))
                        .label(REQUIRED)
                        .type("fixed64")
                        .name("f7")
                        .tag(7)
                        .build(),
                    FieldElement.builder(location.at(9, 3))
                        .label(REQUIRED)
                        .type("int32")
                        .name("f8")
                        .tag(8)
                        .build(),
                    FieldElement.builder(location.at(10, 3))
                        .label(REQUIRED)
                        .type("int64")
                        .name("f9")
                        .tag(9)
                        .build(),
                    FieldElement.builder(location.at(11, 3))
                        .label(REQUIRED)
                        .type("sfixed32")
                        .name("f10")
                        .tag(10)
                        .build(),
                    FieldElement.builder(location.at(12, 3))
                        .label(REQUIRED)
                        .type("sfixed64")
                        .name("f11")
                        .tag(11)
                        .build(),
                    FieldElement.builder(location.at(13, 3))
                        .label(REQUIRED)
                        .type("sint32")
                        .name("f12")
                        .tag(12)
                        .build(),
                    FieldElement.builder(location.at(14, 3))
                        .label(REQUIRED)
                        .type("sint64")
                        .name("f13")
                        .tag(13)
                        .build(),
                    FieldElement.builder(location.at(15, 3))
                        .label(REQUIRED)
                        .type("string")
                        .name("f14")
                        .tag(14)
                        .build(),
                    FieldElement.builder(location.at(16, 3))
                        .label(REQUIRED)
                        .type("uint32")
                        .name("f15")
                        .tag(15)
                        .build(),
                    FieldElement.builder(location.at(17, 3))
                        .label(REQUIRED)
                        .type("uint64")
                        .name("f16")
                        .tag(16)
                        .build(),
                    FieldElement.builder(location.at(18, 3))
                        .type("map<string, bool>")
                        .name("f17")
                        .tag(17)
                        .build(),
                    FieldElement.builder(location.at(19, 3))
                        .type("map<arbitrary, nested.nested>")
                        .name("f18")
                        .tag(18)
                        .build(),
                    FieldElement.builder(location.at(20, 3))
                        .label(REQUIRED)
                        .type("arbitrary")
                        .name("f19")
                        .tag(19)
                        .build(),
                    FieldElement.builder(location.at(21, 3))
                        .label(REQUIRED)
                        .type("nested.nested")
                        .name("f20")
                        .tag(20)
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void mapWithLabelThrows() {
    try {
      ProtoParser.parse(location, "message Hey { required map<string, string> a = 1; }");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage(
          "Syntax error in file.proto at 1:15: 'map' type cannot have label");
    }
    try {
      ProtoParser.parse(location, "message Hey { optional map<string, string> a = 1; }");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage(
          "Syntax error in file.proto at 1:15: 'map' type cannot have label");
    }
    try {
      ProtoParser.parse(location, "message Hey { repeated map<string, string> a = 1; }");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage(
          "Syntax error in file.proto at 1:15: 'map' type cannot have label");
    }
  }

  /** It looks like an option, but 'default' is special. It's missing from descriptor.proto! */
  @Test public void defaultFieldOptionIsSpecial() {
    String proto = ""
        + "message Message {\n"
        + "  required string a = 1 [default = \"b\", faulted = \"c\"];\n"
        + "}\n";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(1, 1))
                .name("Message")
                .fields(ImmutableList.of(
                    FieldElement.builder(location.at(2, 3))
                        .label(REQUIRED)
                        .type("string")
                        .name("a")
                        .defaultValue("b")
                        .options(ImmutableList.of(
                            OptionElement.create("faulted", Kind.STRING, "c")))
                        .tag(1)
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void singleLineComment() {
    String proto = ""
        + "// Test all the things!\n"
        + "message Test {}";
    ProtoFileElement parsed = ProtoParser.parse(location, proto);
    TypeElement type = parsed.types().get(0);
    assertThat(type.documentation()).isEqualTo("Test all the things!");
  }

  @Test public void multipleSingleLineComments() {
    String proto = ""
        + "// Test all\n"
        + "// the things!\n"
        + "message Test {}";
    String expected = ""
        + "Test all\n"
        + "the things!";
    ProtoFileElement parsed = ProtoParser.parse(location, proto);
    TypeElement type = parsed.types().get(0);
    assertThat(type.documentation()).isEqualTo(expected);
  }

  @Test public void singleLineJavadocComment() {
    String proto = ""
        + "/** Test */\n"
        + "message Test {}";
    ProtoFileElement parsed = ProtoParser.parse(location, proto);
    TypeElement type = parsed.types().get(0);
    assertThat(type.documentation()).isEqualTo("Test");
  }

  @Test public void multilineJavadocComment() {
    String proto = ""
        + "/**\n"
        + " * Test\n"
        + " *\n"
        + " * Foo\n"
        + " */\n"
        + "message Test {}";
    String expected = ""
        + "Test\n"
        + "\n"
        + "Foo";
    ProtoFileElement parsed = ProtoParser.parse(location, proto);
    TypeElement type = parsed.types().get(0);
    assertThat(type.documentation()).isEqualTo(expected);
  }

  @Test public void multipleSingleLineCommentsWithLeadingWhitespace() {
    String proto = ""
        + "// Test\n"
        + "//   All\n"
        + "//     The\n"
        + "//       Things!\n"
        + "message Test {}";
    String expected = ""
        + "Test\n"
        + "  All\n"
        + "    The\n"
        + "      Things!";
    ProtoFileElement parsed = ProtoParser.parse(location, proto);
    TypeElement type = parsed.types().get(0);
    assertThat(type.documentation()).isEqualTo(expected);
  }

  @Test public void multilineJavadocCommentWithLeadingWhitespace() {
    String proto = ""
        + "/**\n"
        + " * Test\n"
        + " *   All\n"
        + " *     The\n"
        + " *       Things!\n"
        + " */\n"
        + "message Test {}";
    String expected = ""
        + "Test\n"
        + "  All\n"
        + "    The\n"
        + "      Things!";
    ProtoFileElement parsed = ProtoParser.parse(location, proto);
    TypeElement type = parsed.types().get(0);
    assertThat(type.documentation()).isEqualTo(expected);
  }

  @Test public void multilineJavadocCommentWithoutLeadingAsterisks() {
    // We do not honor leading whitespace when the comment lacks leading asterisks.
    String proto = ""
        + "/**\n"
        + " Test\n"
        + "   All\n"
        + "     The\n"
        + "       Things!\n"
        + " */\n"
        + "message Test {}";
    String expected = ""
        + "Test\n"
        + "All\n"
        + "The\n"
        + "Things!";
    ProtoFileElement parsed = ProtoParser.parse(location, proto);
    TypeElement type = parsed.types().get(0);
    assertThat(type.documentation()).isEqualTo(expected);
  }

  @Test public void messageFieldTrailingComment() {
    // Trailing message field comment.
    String proto = ""
        + "message Test {\n"
        + "  optional string name = 1; // Test all the things!\n"
        + "}";
    ProtoFileElement parsed = ProtoParser.parse(location, proto);
    MessageElement message = (MessageElement) parsed.types().get(0);
    FieldElement field = message.fields().get(0);
    assertThat(field.documentation()).isEqualTo("Test all the things!");
  }

  @Test public void messageFieldLeadingAndTrailingCommentAreCombined() {
    String proto = ""
        + "message Test {\n"
        + "  // Test all...\n"
        + "  optional string name = 1; // ...the things!\n"
        + "}";
    ProtoFileElement parsed = ProtoParser.parse(location, proto);
    MessageElement message = (MessageElement) parsed.types().get(0);
    FieldElement field = message.fields().get(0);
    assertThat(field.documentation()).isEqualTo("Test all...\n...the things!");
  }

  @Test public void trailingCommentNotAssignedToFollowingField() {
    String proto = ""
        + "message Test {\n"
        + "  optional string first_name = 1; // Testing!\n"
        + "  optional string last_name = 2;\n"
        + "}";
    ProtoFileElement parsed = ProtoParser.parse(location, proto);
    MessageElement message = (MessageElement) parsed.types().get(0);
    FieldElement field1 = message.fields().get(0);
    assertThat(field1.documentation()).isEqualTo("Testing!");
    FieldElement field2 = message.fields().get(1);
    assertThat(field2.documentation()).isEqualTo("");
  }

  @Test public void enumValueTrailingComment() {
    String proto = ""
        + "enum Test {\n"
        + "  FOO = 1; // Test all the things!   \n"
        + "}";
    ProtoFileElement parsed = ProtoParser.parse(location, proto);
    EnumElement enumElement = (EnumElement) parsed.types().get(0);
    EnumConstantElement value = enumElement.constants().get(0);
    assertThat(value.documentation()).isEqualTo("Test all the things!");
  }

  @Test public void trailingMultilineComment() {
    String proto = ""
        + "enum Test {\n"
        + "  FOO = 1; /* Test all the things!  */  \n"
        + "  BAR = 2;/*Test all the things!*/\n"
        + "}";
    ProtoFileElement parsed = ProtoParser.parse(location, proto);
    EnumElement enumElement = (EnumElement) parsed.types().get(0);
    EnumConstantElement foo = enumElement.constants().get(0);
    assertThat(foo.documentation()).isEqualTo("Test all the things!");
    EnumConstantElement bar = enumElement.constants().get(1);
    assertThat(bar.documentation()).isEqualTo("Test all the things!");
  }

  @Test public void trailingUnclosedMultilineCommentThrows() {
    String proto = ""
        + "enum Test {\n"
        + "  FOO = 1; /* Test all the things!   \n"
        + "}";
    try {
      ProtoParser.parse(location, proto);
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage(
          "Syntax error in file.proto at 2:38: trailing comment must be closed on the same line");
    }
  }

  @Test public void trailingMultilineCommentMustBeLastOnLineThrows() {
    String proto = ""
        + "enum Test {\n"
        + "  FOO = 1; /* Test all the things! */ BAR = 2;\n"
        + "}";
    try {
      ProtoParser.parse(location, proto);
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage(
          "Syntax error in file.proto at 2:40: no syntax may follow trailing comment");
    }
  }

  @Test public void invalidTrailingComment() {
    String proto = ""
        + "enum Test {\n"
        + "  FOO = 1; /\n"
        + "}";
    try {
      ProtoParser.parse(location, proto);
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage(
          "Syntax error in file.proto at 2:12: expected '//' or '/*'");
    }
  }

  @Test public void enumValueLeadingAndTrailingCommentsAreCombined() {
    String proto = ""
        + "enum Test {\n"
        + "  // Test all...\n"
        + "  FOO = 1; // ...the things!\n"
        + "}";
    ProtoFileElement parsed = ProtoParser.parse(location, proto);
    EnumElement enumElement = (EnumElement) parsed.types().get(0);
    EnumConstantElement value = enumElement.constants().get(0);
    assertThat(value.documentation()).isEqualTo("Test all...\n...the things!");
  }

  @Test public void trailingCommentNotCombinedWhenEmpty() {
    String proto = ""
        + "enum Test {\n"
        + "  // Test all...\n"
        + "  FOO = 1; //      \n"
        + "}";
    ProtoFileElement parsed = ProtoParser.parse(location, proto);
    EnumElement enumElement = (EnumElement) parsed.types().get(0);
    EnumConstantElement value = enumElement.constants().get(0);
    assertThat(value.documentation()).isEqualTo("Test all...");
  }

  @Test public void syntaxNotRequired() throws Exception {
    String proto = "message Foo {}";
    ProtoFileElement parsed = ProtoParser.parse(location, proto);
    assertThat(parsed.syntax()).isNull();
  }

  @Test public void syntaxSpecified() throws Exception {
    String proto = ""
        + "syntax = \"proto3\";\n"
        + "message Foo {}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .syntax(ProtoFile.Syntax.PROTO_3)
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(2, 1))
                .name("Foo")
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void invalidSyntaxValueThrows() throws Exception {
    String proto = ""
        + "syntax = \"proto4\";\n"
        + "message Foo {}";
    try {
      ProtoParser.parse(location, proto);
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Syntax error in file.proto at 1:1: unexpected syntax: proto4");
    }
  }

  @Test public void syntaxInWrongContextThrows() {
    String proto = ""
        + "message Foo {\n"
        + "  syntax = \"proto2\";\n"
        + "}";
    try {
      ProtoParser.parse(location, proto);
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Syntax error in file.proto at 2:3: 'syntax' in MESSAGE");
    }
  }

  @Test public void syntaxNotFirstDeclarationThrows() throws Exception {
    String proto = ""
        + "message Foo {}\n"
        + "syntax = \"proto3\";\n";
    try {
      ProtoParser.parse(location, proto);
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage("Syntax error in file.proto at 2:1: "
          + "'syntax' element must be the first declaration in a file");
    }
  }

  @Test public void syntaxMayFollowCommentsAndEmptyLines() throws Exception {
    String proto = ""
        + "/* comment 1 */\n"
        + "// comment 2\n"
        + "\n"
        + "syntax = \"proto3\";\n"
        + "message Foo {}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .syntax(ProtoFile.Syntax.PROTO_3)
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(5, 1))
                .name("Foo")
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void proto3MessageFieldsDoNotRequireLabels() throws Exception {
    String proto = ""
        + "syntax = \"proto3\";\n"
        + "message Message {\n"
        + "  string a = 1;\n"
        + "  int32 b = 2;\n"
        + "}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .syntax(ProtoFile.Syntax.PROTO_3)
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(2, 1))
                .name("Message")
                .fields(ImmutableList.of(
                    FieldElement.builder(location.at(3, 3))
                        .type("string")
                        .name("a")
                        .tag(1)
                        .build(),
                    FieldElement.builder(location.at(4, 3))
                        .type("int32")
                        .name("b")
                        .tag(2)
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void proto3ExtensionFieldsDoNotRequireLabels() throws Exception {
    String proto = ""
        + "syntax = \"proto3\";\n"
        + "message Message {\n"
        + "}\n"
        + "extend Message {\n"
        + "  string a = 1;\n"
        + "  int32 b = 2;\n"
        + "}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .syntax(ProtoFile.Syntax.PROTO_3)
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(2, 1))
                .name("Message")
                .build()))
        .extendDeclarations(ImmutableList.of(
            ExtendElement.builder(location.at(4, 1))
                .name("Message")
                .fields(ImmutableList.of(
                    FieldElement.builder(location.at(5, 3))
                        .type("string")
                        .name("a")
                        .tag(1)
                        .build(),
                    FieldElement.builder(location.at(6, 3))
                        .type("int32")
                        .name("b")
                        .tag(2)
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void proto3MessageFieldsForbidOptional() throws Exception {
    String proto = ""
        + "syntax = \"proto3\";\n"
        + "message Message {\n"
        + "  optional string a = 1;\n"
        + "}";
    try {
      ProtoParser.parse(location, proto);
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage("Syntax error in file.proto at 3:3: "
          + "'optional' label forbidden in proto3 field declarations");
    }
  }

  @Test public void proto3MessageFieldsForbidRequired() throws Exception {
    String proto = ""
        + "syntax = \"proto3\";\n"
        + "message Message {\n"
        + "  required string a = 1;\n"
        + "}";
    try {
      ProtoParser.parse(location, proto);
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage("Syntax error in file.proto at 3:3: "
          + "'required' label forbidden in proto3 field declarations");
    }
  }

  @Test public void proto3ExtensionFieldsForbidsOptional() throws Exception {
    String proto = ""
        + "syntax = \"proto3\";\n"
        + "message Message {\n"
        + "}\n"
        + "extend Message {\n"
        + "  optional string a = 1;\n"
        + "}";
    try {
      ProtoParser.parse(location, proto);
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage("Syntax error in file.proto at 5:3: "
          + "'optional' label forbidden in proto3 field declarations");
    }
  }

  @Test public void proto3ExtensionFieldsForbidsRequired() throws Exception {
    String proto = ""
        + "syntax = \"proto3\";\n"
        + "message Message {\n"
        + "}\n"
        + "extend Message {\n"
        + "  required string a = 1;\n"
        + "}";
    try {
      ProtoParser.parse(location, proto);
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage("Syntax error in file.proto at 5:3: "
          + "'required' label forbidden in proto3 field declarations");
    }
  }

  @Test public void proto3MessageFieldsPermitRepeated() throws Exception {
    String proto = ""
        + "syntax = \"proto3\";\n"
        + "message Message {\n"
        + "  repeated string a = 1;\n"
        + "}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .syntax(ProtoFile.Syntax.PROTO_3)
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(2, 1))
                .name("Message")
                .fields(ImmutableList.of(
                    FieldElement.builder(location.at(3, 3))
                        .label(REPEATED)
                        .type("string")
                        .name("a")
                        .tag(1)
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void proto3ExtensionFieldsPermitRepeated() throws Exception {
    String proto = ""
        + "syntax = \"proto3\";\n"
        + "message Message {\n"
        + "}\n"
        + "extend Message {\n"
        + "  repeated string a = 1;\n"
        + "}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .syntax(ProtoFile.Syntax.PROTO_3)
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(2, 1))
                .name("Message")
                .build()))
        .extendDeclarations(ImmutableList.of(
            ExtendElement.builder(location.at(4, 1))
                .name("Message")
                .fields(ImmutableList.of(
                    FieldElement.builder(location.at(5, 3))
                        .label(REPEATED)
                        .type("string")
                        .name("a")
                        .tag(1)
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void parseMessageAndFields() throws Exception {
    String proto = ""
        + "message SearchRequest {\n"
        + "  required string query = 1;\n"
        + "  optional int32 page_number = 2;\n"
        + "  optional int32 result_per_page = 3;\n"
        + "}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(1, 1))
                .name("SearchRequest")
                .fields(ImmutableList.of(
                    FieldElement.builder(location.at(2, 3))
                        .label(REQUIRED)
                        .type("string")
                        .name("query")
                        .tag(1)
                        .build(),
                    FieldElement.builder(location.at(3, 3))
                        .label(OPTIONAL)
                        .type("int32")
                        .name("page_number")
                        .tag(2)
                        .build(),
                    FieldElement.builder(location.at(4, 3))
                        .label(OPTIONAL)
                        .type("int32")
                        .name("result_per_page")
                        .tag(3)
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void group() throws Exception {
    String proto = ""
        + "message SearchResponse {\n"
        + "  repeated group Result = 1 {\n"
        + "    required string url = 2;\n"
        + "    optional string title = 3;\n"
        + "    repeated string snippets = 4;\n"
        + "  }\n"
        + "}";
    TypeElement message = MessageElement.builder(location.at(1, 1))
        .name("SearchResponse")
        .groups(ImmutableList.of(
            GroupElement.builder()
                .label(REPEATED)
                .name("Result")
                .tag(1)
                .fields(ImmutableList.of(
                    FieldElement.builder(location.at(3, 5))
                        .label(REQUIRED)
                        .type("string")
                        .name("url")
                        .tag(2)
                        .build(),
                    FieldElement.builder(location.at(4, 5))
                        .label(OPTIONAL)
                        .type("string")
                        .name("title")
                        .tag(3)
                        .build(),
                    FieldElement.builder(location.at(5, 5))
                        .label(REPEATED)
                        .type("string")
                        .name("snippets")
                        .tag(4)
                        .build()
                ))
                .build()
        ))
        .build();
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.of(message))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void parseMessageAndOneOf() throws Exception {
    String proto = ""
        + "message SearchRequest {\n"
        + "  required string query = 1;\n"
        + "  oneof page_info {\n"
        + "    int32 page_number = 2;\n"
        + "    int32 result_per_page = 3;\n"
        + "  }\n"
        + "}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(1, 1))
                .name("SearchRequest")
                .fields(ImmutableList.of(
                    FieldElement.builder(location.at(2, 3))
                        .label(REQUIRED)
                        .type("string")
                        .name("query")
                        .tag(1)
                        .build()))
                .oneOfs(ImmutableList.of(
                    OneOfElement.builder()
                        .name("page_info")
                        .fields(ImmutableList.of(FieldElement.builder(location.at(4, 5))
                                .type("int32")
                                .name("page_number")
                                .tag(2)
                                .build(),
                            FieldElement.builder(location.at(5, 5))
                                .type("int32")
                                .name("result_per_page")
                                .tag(3)
                                .build()))
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void parseMessageAndOneOfWithGroup() throws Exception {
    String proto = ""
        + "message SearchRequest {\n"
        + "  required string query = 1;\n"
        + "  oneof page_info {\n"
        + "    int32 page_number = 2;\n"
        + "    group Stuff = 3 {\n"
        + "      optional int32 result_per_page = 4;\n"
        + "      optional int32 page_count = 5;\n"
        + "    }\n"
        + "  }\n"
        + "}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(1, 1))
                .name("SearchRequest")
                .fields(ImmutableList.of(
                    FieldElement.builder(location.at(2, 3))
                        .label(REQUIRED)
                        .type("string")
                        .name("query")
                        .tag(1)
                        .build()))
                .oneOfs(ImmutableList.of(
                    OneOfElement.builder()
                        .name("page_info")
                        .fields(ImmutableList.of(FieldElement.builder(location.at(4, 5))
                                .type("int32")
                                .name("page_number")
                                .tag(2)
                                .build()))
                        .groups(ImmutableList.of(GroupElement.builder()
                            .name("Stuff")
                            .tag(3)
                            .fields(ImmutableList.of(
                                FieldElement.builder(location.at(6, 7))
                                    .label(OPTIONAL)
                                    .type("int32")
                                    .name("result_per_page")
                                    .tag(4)
                                    .build(),
                                FieldElement.builder(location.at(7, 7))
                                    .label(OPTIONAL)
                                    .type("int32")
                                    .name("page_count")
                                    .tag(5)
                                    .build()
                            ))
                            .build()))
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void parseEnum() throws Exception {
    String proto = ""
        + "/**\n"
        + " * What's on my waffles.\n"
        + " * Also works on pancakes.\n"
        + " */\n"
        + "enum Topping {\n"
        + "  FRUIT = 1;\n"
        + "  /** Yummy, yummy cream. */\n"
        + "  CREAM = 2;\n"
        + "\n"
        + "  // Quebec Maple syrup\n"
        + "  SYRUP = 3;\n"
        + "}\n";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.<TypeElement>of(
            EnumElement.builder(location.at(5, 1))
                .name("Topping")
                .documentation("What's on my waffles.\nAlso works on pancakes.")
                .constants(ImmutableList.of(
                    EnumConstantElement.builder(location.at(6, 3))
                        .name("FRUIT")
                        .tag(1)
                        .build(),
                    EnumConstantElement.builder(location.at(8, 3))
                        .name("CREAM")
                        .tag(2)
                        .documentation("Yummy, yummy cream.")
                        .build(),
                    EnumConstantElement.builder(location.at(11, 3))
                        .name("SYRUP")
                        .tag(3)
                        .documentation("Quebec Maple syrup")
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void parseEnumWithOptions() throws Exception {
    String proto = ""
        + "/**\n"
        + " * What's on my waffles.\n"
        + " * Also works on pancakes.\n"
        + " */\n"
        + "enum Topping {\n"
        + "  option (max_choices) = 2;\n"
        + "\n"
        + "  FRUIT = 1 [(healthy) = true];\n"
        + "  /** Yummy, yummy cream. */\n"
        + "  CREAM = 2;\n"
        + "\n"
        + "  // Quebec Maple syrup\n"
        + "  SYRUP = 3;\n"
        + "}\n";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.<TypeElement>of(
            EnumElement.builder(location.at(5, 1))
                .name("Topping")
                .documentation("What's on my waffles.\nAlso works on pancakes.")
                .options(
                    ImmutableList.of(OptionElement.create("max_choices", Kind.NUMBER, "2", true)))
                .constants(ImmutableList.of(
                    EnumConstantElement.builder(location.at(8, 3))
                        .name("FRUIT")
                        .tag(1)
                        .options(ImmutableList.of(
                            OptionElement.create("healthy", Kind.BOOLEAN, "true", true)))
                        .build(),
                    EnumConstantElement.builder(location.at(10, 3))
                        .name("CREAM")
                        .tag(2)
                        .documentation("Yummy, yummy cream.")
                        .build(),
                    EnumConstantElement.builder(location.at(13, 3))
                        .name("SYRUP")
                        .tag(3)
                        .documentation("Quebec Maple syrup")
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void packageDeclaration() throws Exception {
    String proto = ""
        + "package google.protobuf;\n"
        + "option java_package = \"com.google.protobuf\";\n"
        + "\n"
        + "// The protocol compiler can output a FileDescriptorSet containing the .proto\n"
        + "// files it parses.\n"
        + "message FileDescriptorSet {\n"
        + "}\n";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .packageName("google.protobuf")
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(6, 1))
                .name("FileDescriptorSet")
                .documentation("The protocol compiler can output a FileDescriptorSet containing "
                    + "the .proto\nfiles it parses.")
                .build()))
        .options(ImmutableList.of(
            OptionElement.create("java_package", Kind.STRING, "com.google.protobuf")))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void nestingInMessage() throws Exception {
    String proto = ""
        + "message FieldOptions {\n"
        + "  optional CType ctype = 1 [old_default = STRING, deprecated=true];\n"
        + "  enum CType {\n"
        + "    STRING = 0[(opt_a) = 1, (opt_b) = 2];\n"
        + "  };\n"
        + "  // Clients can define custom options in extensions of this message. See above.\n"
        + "  extensions 500;\n"
        + "  extensions 1000 to max;\n"
        + "}\n";
    TypeElement enumElement = EnumElement.builder(location.at(3, 3))
        .name("CType")
        .constants(ImmutableList.of(EnumConstantElement.builder(location.at(4, 5))
            .name("STRING")
            .tag(0)
            .options(ImmutableList.of(
                OptionElement.create("opt_a", Kind.NUMBER, "1", true),
                OptionElement.create("opt_b", Kind.NUMBER, "2", true)))
            .build()))
        .build();
    FieldElement field = FieldElement.builder(location.at(2, 3))
        .label(OPTIONAL)
        .type("CType")
        .name("ctype")
        .tag(1)
        .options(ImmutableList.of(
            OptionElement.create("old_default", Kind.ENUM, "STRING"),
            OptionElement.create("deprecated", Kind.BOOLEAN, "true")))
        .build();
    assertThat(field.options()).containsOnly( //
        OptionElement.create("old_default", Kind.ENUM, "STRING"), //
        OptionElement.create("deprecated", Kind.BOOLEAN, "true"));

    TypeElement messageElement = MessageElement.builder(location.at(1, 1))
        .name("FieldOptions")
        .fields(ImmutableList.of(field))
        .nestedTypes(ImmutableList.of(enumElement))
        .extensions(ImmutableList.of(
            ExtensionsElement.create(location.at(7, 3), 500, 500,
                "Clients can define custom options in extensions of this message. See above."),
            ExtensionsElement.create(location.at(8, 3), 1000, Util.MAX_TAG_VALUE, "")))
        .build();
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.of(messageElement))
        .build();
    ProtoFileElement actual = ProtoParser.parse(location, proto);
    assertThat(actual).isEqualTo(expected);
  }

  @Test public void optionParentheses() throws Exception {
    String proto = ""
        + "message Chickens {\n"
        + "  optional bool koka_ko_koka_ko = 1 [old_default = true];\n"
        + "  optional bool coodle_doodle_do = 2 [(delay) = 100, old_default = false];\n"
        + "  optional bool coo_coo_ca_cha = 3 [old_default = true, (delay) = 200];\n"
        + "  optional bool cha_chee_cha = 4;\n"
        + "}\n";

    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(1, 1))
                .name("Chickens")
                .fields(ImmutableList.of(
                    FieldElement.builder(location.at(2, 3))
                        .label(OPTIONAL)
                        .type("bool")
                        .name("koka_ko_koka_ko")
                        .tag(1)
                        .options(ImmutableList.of(
                            OptionElement.create("old_default", Kind.BOOLEAN, "true")))
                        .build(),
                    FieldElement.builder(location.at(3, 3))
                        .label(OPTIONAL)
                        .type("bool")
                        .name("coodle_doodle_do")
                        .tag(2)
                        .options(ImmutableList.of(
                            OptionElement.create("delay", Kind.NUMBER, "100", true),
                            OptionElement.create("old_default", Kind.BOOLEAN, "false")))
                        .build(),
                    FieldElement.builder(location.at(4, 3))
                        .label(OPTIONAL)
                        .type("bool")
                        .name("coo_coo_ca_cha")
                        .tag(3)
                        .options(ImmutableList.of(
                            OptionElement.create("old_default", Kind.BOOLEAN, "true"),
                            OptionElement.create("delay", Kind.NUMBER, "200", true)))
                        .build(),
                    FieldElement.builder(location.at(5, 3))
                        .label(OPTIONAL)
                        .type("bool")
                        .name("cha_chee_cha")
                        .tag(4)
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void imports() throws Exception {
    String proto = "import \"src/test/resources/unittest_import.proto\";\n";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .imports(ImmutableList.of(
            "src/test/resources/unittest_import.proto"))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void publicImports() throws Exception {
    String proto = "import public \"src/test/resources/unittest_import.proto\";\n";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .publicImports(ImmutableList.of("src/test/resources/unittest_import.proto"))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void extend() throws Exception {
    String proto = ""
        + "// Extends Foo\n"
        + "extend Foo {\n"
        + "  optional int32 bar = 126;\n"
        + "}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .extendDeclarations(ImmutableList.of(
            ExtendElement.builder(location.at(2, 1))
                .name("Foo")
                .documentation("Extends Foo")
                .fields(ImmutableList.of(
                    FieldElement.builder(location.at(3, 3))
                        .label(OPTIONAL)
                        .type("int32")
                        .name("bar")
                        .tag(126)
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void extendInMessage() throws Exception {
    String proto = ""
        + "message Bar {\n"
        + "  extend Foo {\n"
        + "    optional Bar bar = 126;\n"
        + "  }\n"
        + "}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(1, 1)).name("Bar").build()))
        .extendDeclarations(ImmutableList.of(
            ExtendElement.builder(location.at(2, 3))
                .name("Foo")
                .fields(ImmutableList.of(
                    FieldElement.builder(location.at(3, 5))
                        .label(OPTIONAL)
                        .type("Bar")
                        .name("bar")
                        .tag(126)
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void extendInMessageWithPackage() throws Exception {
    String proto = ""
        + "package kit.kat;\n"
        + "\n"
        + "message Bar {\n"
        + "  extend Foo {\n"
        + "    optional Bar bar = 126;\n"
        + "  }\n"
        + "}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .packageName("kit.kat")
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(3, 1))
                .name("Bar")
                .build()))
        .extendDeclarations(ImmutableList.of(
            ExtendElement.builder(location.at(4, 3))
                .name("Foo")
                .fields(ImmutableList.of(
                    FieldElement.builder(location.at(5, 5))
                        .label(OPTIONAL)
                        .type("Bar")
                        .name("bar")
                        .tag(126)
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void fqcnExtendInMessage() throws Exception {
    String proto = ""
        + "message Bar {\n"
        + "  extend example.Foo {\n"
        + "    optional Bar bar = 126;\n"
        + "  }\n"
        + "}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(1, 1)).name("Bar").build()))
        .extendDeclarations(ImmutableList.of(
            ExtendElement.builder(location.at(2, 3))
                .name("example.Foo")
                .fields(ImmutableList.of(
                    FieldElement.builder(location.at(3, 5))
                        .label(OPTIONAL)
                        .type("Bar")
                        .name("bar")
                        .tag(126)
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void fqcnExtendInMessageWithPackage() throws Exception {
    String proto = ""
        + "package kit.kat;\n"
        + "\n"
        + "message Bar {\n"
        + "  extend example.Foo {\n"
        + "    optional Bar bar = 126;\n"
        + "  }\n"
        + "}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .packageName("kit.kat")
        .types(ImmutableList.<TypeElement>of(MessageElement.builder(location.at(3, 1))
            .name("Bar")
            .build()))
        .extendDeclarations(ImmutableList.of(
            ExtendElement.builder(location.at(4, 3))
                .name("example.Foo")
                .fields(ImmutableList.of(
                    FieldElement.builder(location.at(5, 5))
                        .label(OPTIONAL)
                        .type("Bar")
                        .name("bar")
                        .tag(126)
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void defaultFieldWithParen() throws Exception {
    String proto = ""
        + "message Foo {\n"
        + "  optional string claim_token = 2 [(squareup.redacted) = true];\n"
        + "}";
    FieldElement field = FieldElement.builder(location.at(2, 3))
        .label(OPTIONAL)
        .type("string")
        .name("claim_token")
        .tag(2)
        .options(ImmutableList.of(
            OptionElement.create("squareup.redacted", Kind.BOOLEAN, "true", true)))
        .build();
    assertThat(field.options()).containsOnly(
        OptionElement.create("squareup.redacted", Kind.BOOLEAN, "true", true));

    TypeElement messageElement = MessageElement.builder(location.at(1, 1))
        .name("Foo")
        .fields(ImmutableList.of(field))
        .build();
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.of(messageElement))
        .build();
    assertThat(ProtoParser.parse(location, proto))
        .isEqualTo(expected);
  }

  // Parse \a, \b, \f, \n, \r, \t, \v, \[0-7]{1-3}, and \[xX]{0-9a-fA-F]{1,2}
  @Test public void defaultFieldWithStringEscapes() throws Exception {
    String proto = ""
        + "message Foo {\n"
        + "  optional string name = 1 [\n"
        + "    x = \"\\a\\b\\f\\n\\r\\t\\v\1f\01\001\11\011\111\\xe\\Xe\\xE\\xE\\x41\\X41\"\n"
        + "  ];\n"
        + "}";
    FieldElement field = FieldElement.builder(location.at(2, 3))
        .label(OPTIONAL)
        .type("string")
        .name("name")
        .tag(1)
        .options(ImmutableList.of(OptionElement.create("x", Kind.STRING,
            "\u0007\b\f\n\r\t\u000b\u0001f\u0001\u0001\u0009\u0009I\u000e\u000e\u000e\u000eAA")))
        .build();
    assertThat(field.options()).containsOnly(OptionElement.create("x", Kind.STRING,
        "\u0007\b\f\n\r\t\u000b\u0001f\u0001\u0001\u0009\u0009I\u000e\u000e\u000e\u000eAA"));

    TypeElement messageElement = MessageElement.builder(location.at(1, 1))
        .name("Foo")
        .fields(ImmutableList.of(field))
        .build();
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.of(messageElement))
        .build();
    assertThat(ProtoParser.parse(location, proto))
        .isEqualTo(expected);
  }

  @Test public void stringWithSingleQuotes() {
    String proto = ""
        + "message Foo {\n"
        + "  optional string name = 1 [default = 'single\"quotes'];\n"
        + "}";

    FieldElement field = FieldElement.builder(location.at(2, 3))
        .label(OPTIONAL)
        .type("string")
        .name("name")
        .tag(1)
        .defaultValue("single\"quotes")
        .build();
    TypeElement messageElement = MessageElement.builder(location.at(1, 1))
        .name("Foo")
        .fields(ImmutableList.of(field))
        .build();
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.of(messageElement))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void adjacentStringsConcatenated() {
    String proto = ""
        + "message Foo {\n"
        + "  optional string name = 1 [\n"
        + "    default = \"concat \"\n"
        + "              'these '\n"
        + "              \"please\"\n"
        + "  ];\n"
        + "}";

    FieldElement field = FieldElement.builder(location.at(2, 3))
        .label(OPTIONAL)
        .type("string")
        .name("name")
        .tag(1)
        .defaultValue("concat these please")
        .build();
    TypeElement messageElement = MessageElement.builder(location.at(1, 1))
        .name("Foo")
        .fields(ImmutableList.of(field))
        .build();
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.of(messageElement))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void invalidHexStringEscape() throws Exception {
    String proto = ""
        + "message Foo {\n"
        + "  optional string name = 1 "
        + "[default = \"\\xW\"];\n"
        + "}";
    try {
      ProtoParser.parse(location, proto);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage().contains("expected a digit after \\x or \\X"));
    }
  }

  @Test public void service() throws Exception {
    String proto = ""
        + "service SearchService {\n"
        + "  option (default_timeout) = 30;\n"
        + "\n"
        + "  rpc Search (SearchRequest) returns (SearchResponse);\n"
        + "  rpc Purchase (PurchaseRequest) returns (PurchaseResponse) {\n"
        + "    option (squareup.sake.timeout) = 15; \n"
        + "    option (squareup.a.b) = {"
        + "      value: ["
        + "        FOO,"
        + "        BAR"
        + "      ]"
        + "    };\n"
        + "  }\n"
        + "}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .services(ImmutableList.of(
            ServiceElement.builder(location.at(1, 1))
                .name("SearchService")
                .options(ImmutableList.of(
                    OptionElement.create("default_timeout", Kind.NUMBER, "30", true)))
                .rpcs(ImmutableList.of(
                    RpcElement.builder(location.at(4, 3))
                        .name("Search")
                        .requestType("SearchRequest")
                        .responseType("SearchResponse")
                        .build(),
                    RpcElement.builder(location.at(5, 3))
                        .name("Purchase")
                        .requestType("PurchaseRequest")
                        .responseType("PurchaseResponse")
                        .options(ImmutableList.of(
                            OptionElement.create("squareup.sake.timeout", Kind.NUMBER, "15", true),
                            OptionElement.create("squareup.a.b", Kind.MAP, ImmutableMap.of("value",
                                ImmutableList.of("FOO", "BAR")), true)))
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void streamingService() throws Exception {
    String proto = ""
        + "service RouteGuide {\n"
        + "  rpc GetFeature(Point) returns (Feature) {}\n"
        + "  rpc ListFeatures(Rectangle) returns (stream Feature) {}\n"
        + "  rpc RecordRoute(stream Point) returns (RouteSummary) {}\n"
        + "  rpc RouteChat(stream RouteNote) returns (stream RouteNote) {}\n"
        + "}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .services(ImmutableList.of(
            ServiceElement.builder(location.at(1, 1))
                .name("RouteGuide")
                .rpcs(ImmutableList.of(
                    RpcElement.builder(location.at(2, 3))
                        .name("GetFeature")
                        .requestType("Point")
                        .responseType("Feature")
                        .build(),
                    RpcElement.builder(location.at(3, 3))
                        .name("ListFeatures")
                        .requestType("Rectangle")
                        .responseType("Feature")
                        .responseStreaming(true)
                        .build(),
                    RpcElement.builder(location.at(4, 3))
                        .name("RecordRoute")
                        .requestType("Point")
                        .responseType("RouteSummary")
                        .requestStreaming(true)
                        .build(),
                    RpcElement.builder(location.at(5, 3))
                        .name("RouteChat")
                        .requestType("RouteNote")
                        .responseType("RouteNote")
                        .requestStreaming(true)
                        .responseStreaming(true)
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void hexTag() throws Exception {
    String proto = ""
        + "message HexTag {\n"
        + "  required string hex = 0x10;\n"
        + "  required string uppercase_x_hex = 0X11;\n"
        + "}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(1, 1))
                .name("HexTag")
                .fields(ImmutableList.of(
                    FieldElement.builder(location.at(2, 3))
                        .label(REQUIRED)
                        .type("string")
                        .name("hex")
                        .tag(16)
                        .build(),
                    FieldElement.builder(location.at(3, 3))
                        .label(REQUIRED)
                        .type("string")
                        .name("uppercase_x_hex")
                        .tag(17)
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void structuredOption() throws Exception {
    String proto = ""
        + "message ExoticOptions {\n"
        + "  option (squareup.one) = {name: \"Name\", class_name:\"ClassName\"};\n"
        + "  option (squareup.two.a) = {[squareup.options.type]: EXOTIC};\n"
        + "  option (squareup.two.b) = {names: [\"Foo\", \"Bar\"]};\n"
        + "  option (squareup.three) = {x: {y: 1 y: 2}};\n" // NOTE: Omitted optional comma
        + "  option (squareup.four) = {x: {y: {z: 1}, y: {z: 2}}};\n"
        + "}";

    Map<String, String> option_one_map = new LinkedHashMap<>();
    option_one_map.put("name", "Name");
    option_one_map.put("class_name", "ClassName");
    Map<String, Object> option_two_a_map = new LinkedHashMap<>();
    option_two_a_map.put("[squareup.options.type]", "EXOTIC");
    Map<String, List<String>> option_two_b_map = new LinkedHashMap<>();
    option_two_b_map.put("names", Arrays.asList("Foo", "Bar"));
    Map<String, Map<String, ?>> option_three_map = new LinkedHashMap<>();
    Map<String, Object> option_three_nested_map = new LinkedHashMap<>();
    option_three_nested_map.put("y", Arrays.asList("1", "2"));
    option_three_map.put("x", option_three_nested_map);

    Map<String, Map<String, ?>> option_four_map = new LinkedHashMap<>();
    Map<String, Object> option_four_map_1 = new LinkedHashMap<>();
    Map<String, Object> option_four_map_2_a = new LinkedHashMap<>();
    option_four_map_2_a.put("z", "1");
    Map<String, Object> option_four_map_2_b = new LinkedHashMap<>();
    option_four_map_2_b.put("z", "2");
    option_four_map_1.put("y", Arrays.asList(option_four_map_2_a, option_four_map_2_b));
    option_four_map.put("x", option_four_map_1);

    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(1, 1))
                .name("ExoticOptions")
                .options(ImmutableList.of(
                    OptionElement.create("squareup.one", Kind.MAP, option_one_map, true),
                    OptionElement.create("squareup.two.a", Kind.MAP, option_two_a_map, true),
                    OptionElement.create("squareup.two.b", Kind.MAP, option_two_b_map, true),
                    OptionElement.create("squareup.three", Kind.MAP, option_three_map, true),
                    OptionElement.create("squareup.four", Kind.MAP, option_four_map, true)))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void optionsWithNestedMapsAndTrailingCommas() throws Exception {
    String proto = ""
        + "message StructuredOption {\n"
        + "    optional field.type has_options = 3 [\n"
        + "            (option_map) = {\n"
        + "                nested_map: {key:\"value\" key2:[\"value2a\",\"value2b\"]},\n"
        + "            }\n"
        + "            (option_string) = [\"string1\",\"string2\"]\n"
        + "    ];\n"
        + "}";
    FieldElement field = FieldElement.builder(location.at(2, 5))
        .label(OPTIONAL)
        .type("field.type")
        .name("has_options")
        .tag(3)
        .options(ImmutableList.of(
            OptionElement.create("option_map", Kind.MAP,
                ImmutableMap.of("nested_map", ImmutableMap.of("key", "value", "key2",
                    ImmutableList.of("value2a", "value2b"))), true),
            OptionElement.create("option_string", Kind.LIST,
                ImmutableList.of("string1", "string2"), true)))
        .build();
    assertThat(field.options()).containsOnly( //
        OptionElement.create("option_map", Kind.MAP, ImmutableMap.of("nested_map",
            ImmutableMap.of("key", "value", "key2", ImmutableList.of("value2a", "value2b"))), true),
        OptionElement.create("option_string", Kind.LIST,
            ImmutableList.of("string1", "string2"), true));

    TypeElement expected = MessageElement.builder(location.at(1, 1))
        .name("StructuredOption")
        .fields(ImmutableList.of(field))
        .build();
    ProtoFileElement protoFile = ProtoFileElement.builder(location)
        .types(ImmutableList.of(expected))
        .build();
    assertThat(ProtoParser.parse(location, proto))
        .isEqualTo(protoFile);
  }

  @Test public void optionNumericalBounds() {
    String proto = ""
        + "message Test {\n"
        + "  optional int32 default_int32 = 401 [x = 2147483647 ];\n"
        + "  optional uint32 default_uint32 = 402 [x = 4294967295 ];\n"
        + "  optional sint32 default_sint32 = 403 [x = -2147483648 ];\n"
        + "  optional fixed32 default_fixed32 = 404 [x = 4294967295 ];\n"
        + "  optional sfixed32 default_sfixed32 = 405 [x = -2147483648 ];\n"
        + "  optional int64 default_int64 = 406 [x = 9223372036854775807 ];\n"
        + "  optional uint64 default_uint64 = 407 [x = 18446744073709551615 ];\n"
        + "  optional sint64 default_sint64 = 408 [x = -9223372036854775808 ];\n"
        + "  optional fixed64 default_fixed64 = 409 [x = 18446744073709551615 ];\n"
        + "  optional sfixed64 default_sfixed64 = 410 [x = -9223372036854775808 ];\n"
        + "  optional bool default_bool = 411 [x = true ];\n"
        + "  optional float default_float = 412 [x = 123.456e7 ];\n"
        + "  optional double default_double = 413 [x = 123.456e78 ];\n"
        + "  optional string default_string = 414 [x = \"çok\\a\\b\\f\\n\\r\\t\\v\\1\\01\\001\\17\\017\\176\\x1\\x01\\x11\\X1\\X01\\X11güzel\" ];\n"
        + "  optional bytes default_bytes = 415 [x = \"çok\\a\\b\\f\\n\\r\\t\\v\\1\\01\\001\\17\\017\\176\\x1\\x01\\x11\\X1\\X01\\X11güzel\" ];\n"
        + "  optional NestedEnum default_nested_enum = 416 [x = A ];"
        + "}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(1, 1))
                .name("Test")
                .fields(ImmutableList.of(FieldElement.builder(location.at(2, 3))
                        .label(OPTIONAL)
                        .type("int32")
                        .name("default_int32")
                        .tag(401)
                        .options(ImmutableList.of(
                            OptionElement.create("x", Kind.NUMBER, "2147483647")))
                        .build(),
                    FieldElement.builder(location.at(3, 3))
                        .label(OPTIONAL)
                        .type("uint32")
                        .name("default_uint32")
                        .tag(402)
                        .options(ImmutableList.of(
                            OptionElement.create("x", Kind.NUMBER, "4294967295")))
                        .build(),
                    FieldElement.builder(location.at(4, 3))
                        .label(OPTIONAL)
                        .type("sint32")
                        .name("default_sint32")
                        .tag(403)
                        .options(ImmutableList.of(
                            OptionElement.create("x", Kind.NUMBER, "-2147483648")))
                        .build(),
                    FieldElement.builder(location.at(5, 3))
                        .label(OPTIONAL)
                        .type("fixed32")
                        .name("default_fixed32")
                        .tag(404)
                        .options(ImmutableList.of(
                            OptionElement.create("x", Kind.NUMBER, "4294967295")))
                        .build(),
                    FieldElement.builder(location.at(6, 3))
                        .label(OPTIONAL)
                        .type("sfixed32")
                        .name("default_sfixed32")
                        .tag(405)
                        .options(ImmutableList.of(
                            OptionElement.create("x", Kind.NUMBER, "-2147483648")))
                        .build(),
                    FieldElement.builder(location.at(7, 3))
                        .label(OPTIONAL)
                        .type("int64")
                        .name("default_int64")
                        .tag(406)
                        .options(ImmutableList.of(
                            OptionElement.create("x", Kind.NUMBER, "9223372036854775807")))
                        .build(),
                    FieldElement.builder(location.at(8, 3))
                        .label(OPTIONAL)
                        .type("uint64")
                        .name("default_uint64")
                        .tag(407)
                        .options(ImmutableList.of(
                            OptionElement.create("x", Kind.NUMBER, "18446744073709551615")))
                        .build(),
                    FieldElement.builder(location.at(9, 3))
                        .label(OPTIONAL)
                        .type("sint64")
                        .name("default_sint64")
                        .tag(408)
                        .options(ImmutableList.of(
                            OptionElement.create("x", Kind.NUMBER, "-9223372036854775808")))
                        .build(),
                    FieldElement.builder(location.at(10, 3))
                        .label(OPTIONAL)
                        .type("fixed64")
                        .name("default_fixed64")
                        .tag(409)
                        .options(ImmutableList.of(
                            OptionElement.create("x", Kind.NUMBER, "18446744073709551615")))
                        .build(),
                    FieldElement.builder(location.at(11, 3))
                        .label(OPTIONAL)
                        .type("sfixed64")
                        .name("default_sfixed64")
                        .tag(410)
                        .options(ImmutableList.of(
                            OptionElement.create("x", Kind.NUMBER, "-9223372036854775808")))
                        .build(),
                    FieldElement.builder(location.at(12, 3))
                        .label(OPTIONAL)
                        .type("bool")
                        .name("default_bool")
                        .tag(411)
                        .options(ImmutableList.of(
                            OptionElement.create("x", Kind.BOOLEAN, "true")))
                        .build(),
                    FieldElement.builder(location.at(13, 3))
                        .label(OPTIONAL)
                        .type("float")
                        .name("default_float")
                        .tag(412)
                        .options(ImmutableList.of(
                            OptionElement.create("x", Kind.NUMBER, "123.456e7")))
                        .build(),
                    FieldElement.builder(location.at(14, 3))
                        .label(OPTIONAL)
                        .type("double")
                        .name("default_double")
                        .tag(413)
                        .options(ImmutableList.of(
                            OptionElement.create("x", Kind.NUMBER, "123.456e78")))
                        .build(),
                    FieldElement.builder(location.at(15, 3))
                        .label(OPTIONAL)
                        .type("string")
                        .name("default_string")
                        .tag(414)
                        .options(ImmutableList.of(
                            OptionElement.create("x", Kind.STRING, "çok\u0007\b\f\n\r\t\u000b"
                                + "\u0001\u0001\u0001\u000f\u000f~\u0001\u0001\u0011\u0001\u0001"
                                + "\u0011güzel")))
                        .build(),
                    FieldElement.builder(location.at(17, 3))
                        .label(OPTIONAL)
                        .type("bytes")
                        .name("default_bytes")
                        .tag(415)
                        .options(ImmutableList.of(
                            OptionElement.create("x", Kind.STRING, "çok\u0007\b\f\n\r\t\u000b"
                                + "\u0001\u0001\u0001\u000f\u000f~\u0001\u0001\u0011\u0001\u0001"
                                + "\u0011güzel")))
                        .build(),
                    FieldElement.builder(location.at(19, 3))
                        .label(OPTIONAL)
                        .type("NestedEnum")
                        .name("default_nested_enum")
                        .tag(416)
                        .options(ImmutableList.of(OptionElement.create("x", Kind.ENUM, "A")))
                        .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void extensionWithNestedMessage() throws Exception {
    String proto = ""
        + "message Foo {\n"
        + "  optional int32 bar = 1 [\n"
        + "      (validation.range).min = 1,\n"
        + "      (validation.range).max = 100,\n"
        + "      old_default = 20\n"
        + "  ];\n"
        + "}";
    FieldElement field = FieldElement.builder(location.at(2, 3))
        .label(OPTIONAL)
        .type("int32")
        .name("bar")
        .tag(1)
        .options(ImmutableList.of(
            OptionElement.create("validation.range", Kind.OPTION,
                OptionElement.create("min", Kind.NUMBER, "1"), true),
            OptionElement.create("validation.range", Kind.OPTION,
                OptionElement.create("max", Kind.NUMBER, "100"), true),
            OptionElement.create("old_default", Kind.NUMBER, "20")))
        .build();
    assertThat(field.options()).containsOnly( //
        OptionElement.create("validation.range", Kind.OPTION,
            OptionElement.create("min", Kind.NUMBER, "1"), true), //
        OptionElement.create("validation.range", Kind.OPTION,
            OptionElement.create("max", Kind.NUMBER, "100"), true), //
        OptionElement.create("old_default", Kind.NUMBER, "20"));

    TypeElement expected = MessageElement.builder(location.at(1, 1))
        .name("Foo").fields(ImmutableList.of(field))
        .build();
    ProtoFileElement protoFile = ProtoFileElement.builder(location)
        .types(ImmutableList.of(expected))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(protoFile);
  }

  @Test public void reserved() {
    String proto = ""
        + "message Foo {\n"
        + "  reserved 10, 12 to 14, 'foo';"
        + "}";
    TypeElement message = MessageElement.builder(location.at(1, 1))
        .name("Foo")
        .reserveds(ImmutableList.of(ReservedElement.create(location.at(2, 3), "",
            ImmutableList.<Object>of(10, Range.closed(12, 14), "foo"))))
        .build();
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.of(message))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }

  @Test public void noWhitespace() {
    String proto = "message C {optional A.B ab = 1;}";
    ProtoFileElement expected = ProtoFileElement.builder(location)
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location.at(1, 1))
                .name("C").fields(ImmutableList.of(FieldElement.builder(location.at(1, 12))
                .label(OPTIONAL)
                .type("A.B")
                .name("ab")
                .tag(1)
                .build()))
                .build()))
        .build();
    assertThat(ProtoParser.parse(location, proto)).isEqualTo(expected);
  }
}
