// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import com.squareup.protoparser.DataType.MapType;
import com.squareup.protoparser.DataType.NamedType;
import com.squareup.protoparser.DataType.ScalarType;
import com.squareup.protoparser.OptionElement.Kind;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static com.squareup.protoparser.DataType.ScalarType.ANY;
import static com.squareup.protoparser.DataType.ScalarType.BOOL;
import static com.squareup.protoparser.DataType.ScalarType.BYTES;
import static com.squareup.protoparser.DataType.ScalarType.DOUBLE;
import static com.squareup.protoparser.DataType.ScalarType.FIXED32;
import static com.squareup.protoparser.DataType.ScalarType.FIXED64;
import static com.squareup.protoparser.DataType.ScalarType.FLOAT;
import static com.squareup.protoparser.DataType.ScalarType.INT32;
import static com.squareup.protoparser.DataType.ScalarType.INT64;
import static com.squareup.protoparser.DataType.ScalarType.SFIXED32;
import static com.squareup.protoparser.DataType.ScalarType.SFIXED64;
import static com.squareup.protoparser.DataType.ScalarType.SINT32;
import static com.squareup.protoparser.DataType.ScalarType.SINT64;
import static com.squareup.protoparser.DataType.ScalarType.STRING;
import static com.squareup.protoparser.DataType.ScalarType.UINT32;
import static com.squareup.protoparser.DataType.ScalarType.UINT64;
import static com.squareup.protoparser.FieldElement.Label.ONE_OF;
import static com.squareup.protoparser.FieldElement.Label.OPTIONAL;
import static com.squareup.protoparser.FieldElement.Label.REQUIRED;
import static com.squareup.protoparser.TestUtils.list;
import static com.squareup.protoparser.TestUtils.map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class ProtoParserTest {
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
        + "  required map<string, bool> f17 = 17;\n"
        + "  required map<arbitrary, nested.nested> f18 = 18;\n"
        + "  required arbitrary f19 = 19;\n"
        + "  required nested.nested f20 = 20;\n"
        + "}\n";
    ProtoFile expected = ProtoFile.builder("test.proto")
        .addType(MessageElement.builder()
            .name("Types")
            .addField(FieldElement.builder().label(REQUIRED).type(ANY).name("f1").tag(1).build())
            .addField(FieldElement.builder().label(REQUIRED).type(BOOL).name("f2").tag(2).build())
            .addField(FieldElement.builder().label(REQUIRED).type(BYTES).name("f3").tag(3).build())
            .addField(FieldElement.builder().label(REQUIRED).type(DOUBLE).name("f4").tag(4).build())
            .addField(FieldElement.builder().label(REQUIRED).type(FLOAT).name("f5").tag(5).build())
            .addField(
                FieldElement.builder().label(REQUIRED).type(FIXED32).name("f6").tag(6).build())
            .addField(
                FieldElement.builder().label(REQUIRED).type(FIXED64).name("f7").tag(7).build())
            .addField(FieldElement.builder().label(REQUIRED).type(INT32).name("f8").tag(8).build())
            .addField(FieldElement.builder().label(REQUIRED).type(INT64).name("f9").tag(9).build())
            .addField(
                FieldElement.builder().label(REQUIRED).type(SFIXED32).name("f10").tag(10).build())
            .addField(
                FieldElement.builder().label(REQUIRED).type(SFIXED64).name("f11").tag(11).build())
            .addField(
                FieldElement.builder().label(REQUIRED).type(SINT32).name("f12").tag(12).build())
            .addField(
                FieldElement.builder().label(REQUIRED).type(SINT64).name("f13").tag(13).build())
            .addField(
                FieldElement.builder().label(REQUIRED).type(STRING).name("f14").tag(14).build())
            .addField(
                FieldElement.builder().label(REQUIRED).type(UINT32).name("f15").tag(15).build())
            .addField(
                FieldElement.builder().label(REQUIRED).type(UINT64).name("f16").tag(16).build())
            .addField(FieldElement.builder()
                .label(REQUIRED)
                .type(MapType.create(STRING, BOOL))
                .name("f17")
                .tag(17)
                .build())
            .addField(FieldElement.builder()
                .label(REQUIRED)
                .type(MapType.create(NamedType.create("arbitrary"),
                    NamedType.create("nested.nested")))
                .name("f18")
                .tag(18)
                .build())
            .addField(FieldElement.builder()
                .label(REQUIRED)
                .type(NamedType.create("arbitrary"))
                .name("f19")
                .tag(19)
                .build())
            .addField(FieldElement.builder()
                .label(REQUIRED)
                .type(NamedType.create("nested.nested"))
                .name("f20")
                .tag(20)
                .build())
            .build())
        .build();
    assertThat(ProtoParser.parse("test.proto", proto)).isEqualTo(expected);
  }

  @Test public void singleLineComment() {
    String proto = ""
        + "// Test all the things!\n"
        + "message Test {}";
    ProtoFile parsed = ProtoParser.parse("test.proto", proto);
    TypeElement type = parsed.typeElements().get(0);
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
    ProtoFile parsed = ProtoParser.parse("test.proto", proto);
    TypeElement type = parsed.typeElements().get(0);
    assertThat(type.documentation()).isEqualTo(expected);
  }

  @Test public void singleLineJavadocComment() {
    String proto = ""
        + "/** Test */\n"
        + "message Test {}";
    ProtoFile parsed = ProtoParser.parse("test.proto", proto);
    TypeElement type = parsed.typeElements().get(0);
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
    ProtoFile parsed = ProtoParser.parse("test.proto", proto);
    TypeElement type = parsed.typeElements().get(0);
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
    ProtoFile parsed = ProtoParser.parse("test.proto", proto);
    TypeElement type = parsed.typeElements().get(0);
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
    ProtoFile parsed = ProtoParser.parse("test.proto", proto);
    TypeElement type = parsed.typeElements().get(0);
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
    ProtoFile parsed = ProtoParser.parse("test.proto", proto);
    TypeElement type = parsed.typeElements().get(0);
    assertThat(type.documentation()).isEqualTo(expected);
  }

  @Test public void messageFieldTrailingComment() {
    // Trailing message field comment.
    String proto = ""
        + "message Test {\n"
        + "  optional string name = 1; // Test all the things!\n"
        + "}";
    ProtoFile parsed = ProtoParser.parse("test.proto", proto);
    MessageElement message = (MessageElement) parsed.typeElements().get(0);
    FieldElement field = message.fields().get(0);
    assertThat(field.documentation()).isEqualTo("Test all the things!");
  }

  @Test public void messageFieldLeadingAndTrailingCommentAreCombined() {
    String proto = ""
        + "message Test {\n"
        + "  // Test all...\n"
        + "  optional string name = 1; // ...the things!\n"
        + "}";
    ProtoFile parsed = ProtoParser.parse("test.proto", proto);
    MessageElement message = (MessageElement) parsed.typeElements().get(0);
    FieldElement field = message.fields().get(0);
    assertThat(field.documentation()).isEqualTo("Test all...\n...the things!");
  }

  @Test public void trailingCommentNotAssignedToFollowingField() {
    String proto = ""
        + "message Test {\n"
        + "  optional string first_name = 1; // Testing!\n"
        + "  optional string last_name = 2;\n"
        + "}";
    ProtoFile parsed = ProtoParser.parse("test.proto", proto);
    MessageElement message = (MessageElement) parsed.typeElements().get(0);
    FieldElement field1 = message.fields().get(0);
    assertThat(field1.documentation()).isEqualTo("Testing!");
    FieldElement field2 = message.fields().get(1);
    assertThat(field2.documentation()).isEqualTo("");
  }

  @Test public void enumValueTrailingComment() {
    String proto = ""
        + "enum Test {\n"
        + "  FOO = 1; // Test all the things!\n"
        + "}";
    ProtoFile parsed = ProtoParser.parse("test.proto", proto);
    EnumElement enumElement = (EnumElement) parsed.typeElements().get(0);
    EnumConstantElement value = enumElement.constants().get(0);
    assertThat(value.documentation()).isEqualTo("Test all the things!");
  }

  @Test public void enumValueLeadingAndTrailingCommentsAreCombined() {
    String proto = ""
        + "enum Test {\n"
        + "  // Test all...\n"
        + "  FOO = 1; // ...the things!\n"
        + "}";
    ProtoFile parsed = ProtoParser.parse("test.proto", proto);
    EnumElement enumElement = (EnumElement) parsed.typeElements().get(0);
    EnumConstantElement value = enumElement.constants().get(0);
    assertThat(value.documentation()).isEqualTo("Test all...\n...the things!");
  }

  @Test public void syntaxNotRequired() throws Exception {
    String proto = "message Foo {}";
    ProtoFile parsed = ProtoParser.parse("test.proto", proto);
    assertThat(parsed.syntax()).isNull();
  }

  @Test public void syntaxSpecified() throws Exception {
    String proto = ""
        + "syntax \"proto3\";\n"
        + "message Foo {}";
    ProtoFile expected = ProtoFile.builder("test.proto")
        .syntax(ProtoFile.Syntax.PROTO_3)
        .addType(MessageElement.builder().name("Foo").build())
        .build();
    assertThat(ProtoParser.parse("test.proto", proto)).isEqualTo(expected);
  }

  @Test public void invalidSyntaxValueThrows() throws Exception {
    String proto = ""
        + "syntax \"proto4\";\n"
        + "message Foo {}";
    try {
      ProtoParser.parse("test.proto", proto);
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage(
          "Syntax error in test.proto at 1:16: 'syntax' must be 'proto2' or 'proto3'. Found: proto4");
    }
  }

  @Test public void syntaxInWrongContextThrows() {
    String proto = ""
        + "message Foo {\n"
        + "  syntax \"proto2\";\n"
        + "}";
    try {
      ProtoParser.parse("test.proto", proto);
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Syntax error in test.proto at 2:9: 'syntax' in MESSAGE");
    }
  }

  @Test public void parseMessageAndFields() throws Exception {
    String proto = ""
        + "message SearchRequest {\n"
        + "  required string query = 1;\n"
        + "  optional int32 page_number = 2;\n"
        + "  optional int32 result_per_page = 3;\n"
        + "}";
    ProtoFile expected = ProtoFile.builder("search.proto")
        .addType(MessageElement.builder()
            .name("SearchRequest")
            .addField(FieldElement.builder()
                .label(REQUIRED)
                .type(STRING)
                .name("query")
                .tag(1)
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(INT32)
                .name("page_number")
                .tag(2)
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(INT32)
                .name("result_per_page")
                .tag(3)
                .build())
            .build())
        .build();
    assertThat(ProtoParser.parse("search.proto", proto)).isEqualTo(expected);
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
    ProtoFile expected = ProtoFile.builder("search.proto")
        .addType(MessageElement.builder()
            .name("SearchRequest")
            .addField(FieldElement.builder()
                .label(REQUIRED)
                .type(STRING)
                .name("query")
                .tag(1)
                .build())
            .addOneOf(OneOfElement.builder()
                .name("page_info")
                .addField(FieldElement.builder()
                    .label(ONE_OF)
                    .type(INT32)
                    .name("page_number")
                    .tag(2)
                    .build())
                .addField(FieldElement.builder()
                    .label(ONE_OF)
                    .type(INT32)
                    .name("result_per_page")
                    .tag(3)
                    .build())
                .build())
            .build())
        .build();
    assertThat(ProtoParser.parse("search.proto", proto)).isEqualTo(expected);
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
    ProtoFile expected = ProtoFile.builder("waffles.proto")
        .addType(EnumElement.builder()
            .name("Topping")
            .documentation("What's on my waffles.\nAlso works on pancakes.")
            .addConstant(EnumConstantElement.builder().name("FRUIT").tag(1).build())
            .addConstant(EnumConstantElement.builder()
                .name("CREAM")
                .tag(2)
                .documentation("Yummy, yummy cream.")
                .build())
            .addConstant(EnumConstantElement.builder()
                .name("SYRUP")
                .tag(3)
                .documentation("Quebec Maple syrup")
                .build())
            .build())
        .build();
    assertThat(ProtoParser.parse("waffles.proto", proto)).isEqualTo(expected);
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
    ProtoFile expected = ProtoFile.builder("waffles.proto")
        .addType(EnumElement.builder()
            .name("Topping")
            .qualifiedName("Topping")
            .documentation("What's on my waffles.\nAlso works on pancakes.")
            .addOption(OptionElement.create("max_choices", Kind.NUMBER, "2", true))
            .addConstant(EnumConstantElement.builder()
                .name("FRUIT")
                .tag(1)
                .addOption(OptionElement.create("healthy", Kind.BOOLEAN, "true", true))
                .build())
            .addConstant(EnumConstantElement.builder()
                .name("CREAM")
                .tag(2)
                .documentation("Yummy, yummy cream.")
                .build())
            .addConstant(EnumConstantElement.builder()
                .name("SYRUP")
                .tag(3)
                .documentation("Quebec Maple syrup")
                .build())
            .build())
        .build();
    assertThat(ProtoParser.parse("waffles.proto", proto)).isEqualTo(expected);
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
    ProtoFile expected = ProtoFile.builder("descriptor.proto")
        .packageName("google.protobuf")
        .addType(MessageElement.builder()
            .name("FileDescriptorSet")
            .qualifiedName("google.protobuf.FileDescriptorSet")
            .documentation(
                "The protocol compiler can output a FileDescriptorSet containing the .proto\nfiles it parses.")
            .build())
        .addOption(OptionElement.create("java_package", Kind.STRING, "com.google.protobuf"))
        .build();
    assertThat(ProtoParser.parse("descriptor.proto", proto)).isEqualTo(expected);
  }

  @Test public void nestingInMessage() throws Exception {
    String proto = ""
        + "message FieldOptions {\n"
        + "  optional CType ctype = 1 [default = STRING, deprecated=true];\n"
        + "  enum CType {\n"
        + "    STRING = 0[(opt_a) = 1, (opt_b) = 2];\n"
        + "  };\n"
        + "  // Clients can define custom options in extensions of this message. See above.\n"
        + "  extensions 500;\n"
        + "  extensions 1000 to max;\n"
        + "}\n";
    TypeElement enumElement = EnumElement.builder()
        .name("CType")
        .qualifiedName("FieldOptions.CType")
        .addConstant(EnumConstantElement.builder()
            .name("STRING")
            .tag(0)
            .addOption(OptionElement.create("opt_a", Kind.NUMBER, "1", true))
            .addOption(OptionElement.create("opt_b", Kind.NUMBER, "2", true))
            .build())
        .build();
    FieldElement field = FieldElement.builder()
        .label(OPTIONAL)
        .type(NamedType.create("CType"))
        .name("ctype")
        .tag(1)
        .addOption(OptionElement.create("default", Kind.ENUM, "STRING"))
        .addOption(OptionElement.create("deprecated", Kind.BOOLEAN, "true"))
        .build();
    assertThat(field.options()).containsOnly( //
        OptionElement.create("default", Kind.ENUM, "STRING"), //
        OptionElement.create("deprecated", Kind.BOOLEAN, "true"));

    TypeElement messageElement = MessageElement.builder()
        .name("FieldOptions")
        .addField(field)
        .addType(enumElement)
        .addExtensions(ExtensionsElement.create(500, 500,
            "Clients can define custom options in extensions of this message. See above."))
        .addExtensions(ExtensionsElement.create(1000, ProtoFile.MAX_TAG_VALUE))
        .build();
    ProtoFile expected = ProtoFile.builder("descriptor.proto").addType(messageElement).build();
    ProtoFile actual = ProtoParser.parse("descriptor.proto", proto);
    assertThat(actual).isEqualTo(expected);
  }

  @Test public void optionParentheses() throws Exception {
    String proto = ""
        + "message Chickens {\n"
        + "  optional bool koka_ko_koka_ko = 1 [default = true];\n"
        + "  optional bool coodle_doodle_do = 2 [(delay) = 100, default = false];\n"
        + "  optional bool coo_coo_ca_cha = 3 [default = true, (delay) = 200];\n"
        + "  optional bool cha_chee_cha = 4;\n"
        + "}\n";

    ProtoFile expected = ProtoFile.builder("chickens.proto")
        .addType(MessageElement.builder()
            .name("Chickens")
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(BOOL)
                .name("koka_ko_koka_ko")
                .tag(1)
                .addOption(OptionElement.create("default", Kind.BOOLEAN, "true"))
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(BOOL)
                .name("coodle_doodle_do")
                .tag(2)
                .addOption(OptionElement.create("delay", Kind.NUMBER, "100", true))
                .addOption(OptionElement.create("default", Kind.BOOLEAN, "false"))
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(BOOL)
                .name("coo_coo_ca_cha")
                .tag(3)
                .addOption(OptionElement.create("default", Kind.BOOLEAN, "true"))
                .addOption(OptionElement.create("delay", Kind.NUMBER, "200", true))
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(BOOL)
                .name("cha_chee_cha")
                .tag(4)
                .build())
            .build())
        .build();
    assertThat(ProtoParser.parse("chickens.proto", proto)).isEqualTo(expected);
  }

  @Test public void imports() throws Exception {
    String proto = "import \"src/test/resources/unittest_import.proto\";\n";
    ProtoFile expected = ProtoFile.builder("descriptor.proto")
        .addDependency("src/test/resources/unittest_import.proto")
        .build();
    assertThat(ProtoParser.parse("descriptor.proto", proto)).isEqualTo(expected);
  }

  @Test public void publicImports() throws Exception {
    String proto = "import public \"src/test/resources/unittest_import.proto\";\n";
    ProtoFile expected = ProtoFile.builder("descriptor.proto")
        .addPublicDependency("src/test/resources/unittest_import.proto")
        .build();
    assertThat(ProtoParser.parse("descriptor.proto", proto)).isEqualTo(expected);
  }

  @Test public void extend() throws Exception {
    String proto = ""
        + "// Extends Foo\n"
        + "extend Foo {\n"
        + "  optional int32 bar = 126;\n"
        + "}";
    ProtoFile expected = ProtoFile.builder("descriptor.proto")
        .addExtendDeclaration(ExtendElement.builder()
            .name("Foo")
            .documentation("Extends Foo")
            .addField(
                FieldElement.builder().label(OPTIONAL).type(INT32).name("bar").tag(126).build())
            .build())
        .build();
    assertThat(ProtoParser.parse("descriptor.proto", proto)).isEqualTo(expected);
  }

  @Test public void extendInMessage() throws Exception {
    String proto = ""
        + "message Bar {\n"
        + "  extend Foo {\n"
        + "    optional Bar bar = 126;\n"
        + "  }\n"
        + "}";
    ProtoFile expected = ProtoFile.builder("descriptor.proto")
        .addType(MessageElement.builder().name("Bar").build())
        .addExtendDeclaration(ExtendElement.builder()
            .name("Foo")
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(NamedType.create("Bar"))
                .name("bar")
                .tag(126)
                .build())
            .build())
        .build();
    assertThat(ProtoParser.parse("descriptor.proto", proto)).isEqualTo(expected);
  }

  @Test public void extendInMessageWithPackage() throws Exception {
    String proto = ""
        + "package kit.kat;\n"
        + ""
        + "message Bar {\n"
        + "  extend Foo {\n"
        + "    optional Bar bar = 126;\n"
        + "  }\n"
        + "}";
    ProtoFile expected = ProtoFile.builder("descriptor.proto")
        .packageName("kit.kat")
        .addType(MessageElement.builder().name("Bar").qualifiedName("kit.kat.Bar").build())
        .addExtendDeclaration(ExtendElement.builder()
            .name("Foo")
            .qualifiedName("kit.kat.Foo")
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(NamedType.create("Bar"))
                .name("bar")
                .tag(126)
                .build())
            .build())
        .build();
    assertThat(ProtoParser.parse("descriptor.proto", proto)).isEqualTo(expected);
  }

  @Test public void fqcnExtendInMessage() throws Exception {
    String proto = ""
        + "message Bar {\n"
        + "  extend example.Foo {\n"
        + "    optional Bar bar = 126;\n"
        + "  }\n"
        + "}";
    ProtoFile expected = ProtoFile.builder("descriptor.proto")
        .addType(MessageElement.builder().name("Bar").build())
        .addExtendDeclaration(ExtendElement.builder()
            .name("example.Foo")
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(NamedType.create("Bar"))
                .name("bar")
                .tag(126)
                .build())
            .build())
        .build();
    assertThat(ProtoParser.parse("descriptor.proto", proto)).isEqualTo(expected);
  }

  @Test public void fqcnExtendInMessageWithPackage() throws Exception {
    String proto = ""
        + "package kit.kat;\n"
        + ""
        + "message Bar {\n"
        + "  extend example.Foo {\n"
        + "    optional Bar bar = 126;\n"
        + "  }\n"
        + "}";
    ProtoFile expected = ProtoFile.builder("descriptor.proto")
        .packageName("kit.kat")
        .addType(MessageElement.builder().name("Bar").qualifiedName("kit.kat.Bar").build())
        .addExtendDeclaration(ExtendElement.builder()
            .name("example.Foo")
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(NamedType.create("Bar"))
                .name("bar")
                .tag(126)
                .build())
            .build())
        .build();
    assertThat(ProtoParser.parse("descriptor.proto", proto)).isEqualTo(expected);
  }

  @Test public void defaultFieldWithParen() throws Exception {
    String proto = ""
        + "message Foo {\n"
        + "  optional string claim_token = 2 [(squareup.redacted) = true];\n"
        + "}";
    FieldElement field = FieldElement.builder()
        .label(OPTIONAL)
        .type(STRING)
        .name("claim_token")
        .tag(2)
        .addOption(OptionElement.create("squareup.redacted", Kind.BOOLEAN, "true", true))
        .build();
    assertThat(field.options()).containsOnly(
        OptionElement.create("squareup.redacted", Kind.BOOLEAN, "true", true));

    TypeElement messageElement = MessageElement.builder().name("Foo").addField(field).build();
    ProtoFile expected = ProtoFile.builder("descriptor.proto").addType(messageElement).build();
    assertThat(ProtoParser.parse("descriptor.proto", proto))
        .isEqualTo(expected);
  }

  // Parse \a, \b, \f, \n, \r, \t, \v, \[0-7]{1-3}, and \[xX]{0-9a-fA-F]{1,2}
  @Test public void defaultFieldWithStringEscapes() throws Exception {
    String proto = ""
        + "message Foo {\n"
        + "  optional string name = 1 "
        + "[default = \"\\a\\b\\f\\n\\r\\t\\v\1f\01\001\11\011\111\\xe\\Xe\\xE\\xE\\x41\\X41\"];\n"
        + "}";
    FieldElement
        field = FieldElement.builder()
        .label(OPTIONAL)
        .type(STRING)
        .name("name")
        .tag(1)
        .addOption(OptionElement.create("default", Kind.STRING,
            "\u0007\b\f\n\r\t\u000b\u0001f\u0001\u0001\u0009\u0009I\u000e\u000e\u000e\u000eAA"))
        .build();
    assertThat(field.options()).containsOnly(OptionElement.create("default", Kind.STRING,
        "\u0007\b\f\n\r\t\u000b\u0001f\u0001\u0001\u0009\u0009I\u000e\u000e\u000e\u000eAA"));

    TypeElement messageElement = MessageElement.builder().name("Foo").addField(field).build();
    ProtoFile expected = ProtoFile.builder("foo.proto").addType(messageElement).build();
    assertThat(ProtoParser.parse("foo.proto", proto))
        .isEqualTo(expected);
  }

  @Test public void invalidHexStringEscape() throws Exception {
    String proto = ""
        + "message Foo {\n"
        + "  optional string name = 1 "
        + "[default = \"\\xW\"];\n"
        + "}";
    try {
      ProtoParser.parse("foo.proto", proto);
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
        + "  rpc Search (SearchRequest) returns (SearchResponse);"
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
    ProtoFile expected = ProtoFile.builder("descriptor.proto")
        .addService(ServiceElement.builder()
            .name("SearchService")
            .addOption(OptionElement.create("default_timeout", Kind.NUMBER, "30", true))
            .addRpc(RpcElement.builder()
                .name("Search")
                .requestType(NamedType.create("SearchRequest"))
                .responseType(NamedType.create("SearchResponse"))
                .build())
            .addRpc(RpcElement.builder()
                .name("Purchase")
                .requestType(NamedType.create("PurchaseRequest"))
                .responseType(NamedType.create("PurchaseResponse"))
                .addOption(OptionElement.create("squareup.sake.timeout", Kind.NUMBER, "15", true))
                .addOption(OptionElement.create("squareup.a.b", Kind.MAP, map("value", //
                    list("FOO", "BAR")), true))
                .build())
            .build())
        .build();
    assertThat(ProtoParser.parse("descriptor.proto", proto)).isEqualTo(expected);
  }

  @Test public void serviceTypesMustBeNamed() {
    try {
      String proto = ""
          + "service SearchService {\n"
          + "  rpc Search (string) returns (SearchResponse);"
          + "}";
      ProtoParser.parse("test.proto", proto);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Syntax error in test.proto at 2:21: expected message but was string");
    }
    try {
      String proto = ""
          + "service SearchService {\n"
          + "  rpc Search (SearchRequest) returns (string);"
          + "}";
      ProtoParser.parse("test.proto", proto);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Syntax error in test.proto at 2:45: expected message but was string");
    }
  }

  @Test public void hexTag() throws Exception {
    String proto = ""
        + "message HexTag {\n"
        + "  required string hex = 0x10;\n"
        + "}";
    ProtoFile expected = ProtoFile.builder("hex.proto")
        .addType(MessageElement.builder()
            .name("HexTag")
            .addField(FieldElement.builder()
                .label(REQUIRED)
                .type(STRING)
                .name("hex")
                .tag(16)
                .build())
            .build())
        .build();
    assertThat(ProtoParser.parse("hex.proto", proto)).isEqualTo(expected);
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

    MessageElement.Builder expectedBuilder = MessageElement.builder().name("ExoticOptions");

    Map<String, String> option_one_map = new LinkedHashMap<>();
    option_one_map.put("name", "Name");
    option_one_map.put("class_name", "ClassName");
    expectedBuilder.addOption(OptionElement.create("squareup.one", Kind.MAP, option_one_map, true));
    Map<String, Object> option_two_a_map = new LinkedHashMap<>();
    option_two_a_map.put("[squareup.options.type]", "EXOTIC");
    expectedBuilder.addOption(OptionElement.create("squareup.two.a", Kind.MAP, option_two_a_map, true));
    Map<String, List<String>> option_two_b_map = new LinkedHashMap<>();
    option_two_b_map.put("names", Arrays.asList("Foo", "Bar"));
    expectedBuilder.addOption(OptionElement.create("squareup.two.b", Kind.MAP, option_two_b_map, true));
    Map<String, Map<String, ?>> option_three_map = new LinkedHashMap<>();
    Map<String, Object> option_three_nested_map = new LinkedHashMap<>();
    option_three_nested_map.put("y", Arrays.asList("1", "2"));
    option_three_map.put("x", option_three_nested_map);
    expectedBuilder.addOption(OptionElement.create("squareup.three", Kind.MAP, option_three_map, true));

    Map<String, Map<String, ?>> option_four_map = new LinkedHashMap<>();
    Map<String, Object> option_four_map_1 = new LinkedHashMap<>();
    Map<String, Object> option_four_map_2_a = new LinkedHashMap<>();
    option_four_map_2_a.put("z", "1");
    Map<String, Object> option_four_map_2_b = new LinkedHashMap<>();
    option_four_map_2_b.put("z", "2");
    option_four_map_1.put("y", Arrays.asList(option_four_map_2_a, option_four_map_2_b));
    option_four_map.put("x", option_four_map_1);
    expectedBuilder.addOption(OptionElement.create("squareup.four", Kind.MAP, option_four_map, true));

    ProtoFile expected = ProtoFile.builder("exotic.proto").addType(expectedBuilder.build()).build();
    assertThat(ProtoParser.parse("exotic.proto", proto)).isEqualTo(expected);
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
    FieldElement field = FieldElement.builder()
        .label(OPTIONAL)
        .type(NamedType.create("field.type"))
        .name("has_options")
        .tag(3)
        .addOption(OptionElement.create("option_map", Kind.MAP,
            map("nested_map", map("key", "value", "key2", list("value2a", "value2b"))), true))
        .addOption(OptionElement.create("option_string", Kind.LIST, list("string1", "string2"), true))
        .build();
    assertThat(field.options()).containsOnly( //
        OptionElement.create("option_map", Kind.MAP,
            map("nested_map", map("key", "value", "key2", list("value2a", "value2b"))), true),
        OptionElement.create("option_string", Kind.LIST, list("string1", "string2"), true));

    TypeElement expected =
        MessageElement.builder().name("StructuredOption").addField(field).build();
    ProtoFile protoFile = ProtoFile.builder("nestedmaps.proto").addType(expected).build();
    assertThat(ProtoParser.parse("nestedmaps.proto", proto))
        .isEqualTo(protoFile);
  }

  @Test public void optionNumericalBounds() {
    String proto = ""
        + "message Test {"
        + "  optional int32 default_int32 = 401 [default = 2147483647 ];\n"
        + "  optional uint32 default_uint32 = 402 [default = 4294967295 ];\n"
        + "  optional sint32 default_sint32 = 403 [default = -2147483648 ];\n"
        + "  optional fixed32 default_fixed32 = 404 [default = 4294967295 ];\n"
        + "  optional sfixed32 default_sfixed32 = 405 [default = -2147483648 ];\n"
        + "  optional int64 default_int64 = 406 [default = 9223372036854775807 ];\n"
        + "  optional uint64 default_uint64 = 407 [default = 18446744073709551615 ];\n"
        + "  optional sint64 default_sint64 = 408 [default = -9223372036854775808 ];\n"
        + "  optional fixed64 default_fixed64 = 409 [default = 18446744073709551615 ];\n"
        + "  optional sfixed64 default_sfixed64 = 410 [default = -9223372036854775808 ];\n"
        + "  optional bool default_bool = 411 [default = true ];\n"
        + "  optional float default_float = 412 [default = 123.456e7 ];\n"
        + "  optional double default_double = 413 [default = 123.456e78 ];\n"
        + "  optional string default_string = 414 [default = \"çok\\a\\b\\f\\n\\r\\t\\v\\1\\01\\001\\17\\017\\176\\x1\\x01\\x11\\X1\\X01\\X11güzel\" ];\n"
        + "  optional bytes default_bytes = 415 [default = \"çok\\a\\b\\f\\n\\r\\t\\v\\1\\01\\001\\17\\017\\176\\x1\\x01\\x11\\X1\\X01\\X11güzel\" ];\n"
        + "  optional NestedEnum default_nested_enum = 416 [default = A ];"
        + "}";
    ProtoFile expected = ProtoFile.builder("test.proto")
        .addType(MessageElement.builder()
            .name("Test")
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(ScalarType.INT32)
                .name("default_int32")
                .tag(401)
                .addOption(OptionElement.create("default", Kind.NUMBER, "2147483647"))
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(ScalarType.UINT32)
                .name("default_uint32")
                .tag(402)
                .addOption(OptionElement.create("default", Kind.NUMBER, "4294967295"))
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(ScalarType.SINT32)
                .name("default_sint32")
                .tag(403)
                .addOption(OptionElement.create("default", Kind.NUMBER, "-2147483648"))
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(ScalarType.FIXED32)
                .name("default_fixed32")
                .tag(404)
                .addOption(OptionElement.create("default", Kind.NUMBER, "4294967295"))
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(ScalarType.SFIXED32)
                .name("default_sfixed32")
                .tag(405)
                .addOption(OptionElement.create("default", Kind.NUMBER, "-2147483648"))
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(ScalarType.INT64)
                .name("default_int64")
                .tag(406)
                .addOption(OptionElement.create("default", Kind.NUMBER, "9223372036854775807"))
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(ScalarType.UINT64)
                .name("default_uint64")
                .tag(407)
                .addOption(OptionElement.create("default", Kind.NUMBER, "18446744073709551615"))
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(ScalarType.SINT64)
                .name("default_sint64")
                .tag(408)
                .addOption(OptionElement.create("default", Kind.NUMBER, "-9223372036854775808"))
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(ScalarType.FIXED64)
                .name("default_fixed64")
                .tag(409)
                .addOption(OptionElement.create("default", Kind.NUMBER, "18446744073709551615"))
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(ScalarType.SFIXED64)
                .name("default_sfixed64")
                .tag(410)
                .addOption(OptionElement.create("default", Kind.NUMBER, "-9223372036854775808"))
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(ScalarType.BOOL)
                .name("default_bool")
                .tag(411)
                .addOption(OptionElement.create("default", Kind.BOOLEAN, "true"))
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(ScalarType.FLOAT)
                .name("default_float")
                .tag(412)
                .addOption(OptionElement.create("default", Kind.NUMBER, "123.456e7"))
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(ScalarType.DOUBLE)
                .name("default_double")
                .tag(413)
                .addOption(OptionElement.create("default", Kind.NUMBER, "123.456e78"))
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(ScalarType.STRING)
                .name("default_string")
                .tag(414)
                .addOption(OptionElement.create("default", Kind.STRING,
                    "çok\u0007\b\f\n\r\t\u000b\u0001\u0001\u0001\u000f\u000f~\u0001\u0001\u0011\u0001\u0001\u0011güzel"))
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(ScalarType.BYTES)
                .name("default_bytes")
                .tag(415)
                .addOption(OptionElement.create("default", Kind.STRING,
                    "çok\u0007\b\f\n\r\t\u000b\u0001\u0001\u0001\u000f\u000f~\u0001\u0001\u0011\u0001\u0001\u0011güzel"))
                .build())
            .addField(FieldElement.builder()
                .label(OPTIONAL)
                .type(NamedType.create("NestedEnum"))
                .name("default_nested_enum")
                .tag(416)
                .addOption(OptionElement.create("default", Kind.ENUM, "A"))
                .build())
            .build())
        .build();
    assertThat(ProtoParser.parse("test.proto", proto)).isEqualTo(expected);
  }

  @Test public void extensionWithNestedMessage() throws Exception {
    String proto = ""
        + "message Foo {\n"
        + "  optional int32 bar = 1 [\n"
        + "      (validation.range).min = 1,\n"
        + "      (validation.range).max = 100,\n"
        + "      default = 20\n"
        + "  ];\n"
        + "}";
    FieldElement field = FieldElement.builder()
        .label(OPTIONAL)
        .type(INT32)
        .name("bar")
        .tag(1)
        .addOption(OptionElement.create("validation.range", Kind.OPTION,
            OptionElement.create("min", Kind.NUMBER, "1"), true))
        .addOption(OptionElement.create("validation.range", Kind.OPTION,
            OptionElement.create("max", Kind.NUMBER, "100"), true))
        .addOption(OptionElement.create("default", Kind.NUMBER, "20"))
        .build();
    assertThat(field.options()).containsOnly( //
        OptionElement.create("validation.range", Kind.OPTION,
            OptionElement.create("min", Kind.NUMBER, "1"), true), //
        OptionElement.create("validation.range", Kind.OPTION,
            OptionElement.create("max", Kind.NUMBER, "100"), true), //
        OptionElement.create("default", Kind.NUMBER, "20"));

    TypeElement expected = MessageElement.builder().name("Foo").addField(field).build();
    ProtoFile protoFile = ProtoFile.builder("foo.proto").addType(expected).build();
    assertThat(ProtoParser.parse("foo.proto", proto)).isEqualTo(protoFile);
  }

  @Test public void noWhitespace() {
    String proto = "message C {optional A.B ab = 1;}";
    ProtoFile expected = ProtoFile.builder("test.proto")
        .addType(MessageElement.builder()
                .name("C")
                .addField(FieldElement.builder()
                    .label(OPTIONAL)
                    .type(NamedType.create("A.B"))
                    .name("ab")
                    .tag(1)
                    .build())
                .build())
        .build();
    assertThat(ProtoParser.parse("test.proto", proto)).isEqualTo(expected);
  }
}
