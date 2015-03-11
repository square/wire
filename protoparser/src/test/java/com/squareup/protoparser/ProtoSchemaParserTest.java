// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static com.squareup.protoparser.MessageElement.Label.ONE_OF;
import static com.squareup.protoparser.MessageElement.Label.OPTIONAL;
import static com.squareup.protoparser.MessageElement.Label.REQUIRED;
import static com.squareup.protoparser.TestUtils.NO_EXTENSIONS;
import static com.squareup.protoparser.TestUtils.NO_FIELDS;
import static com.squareup.protoparser.TestUtils.NO_ONEOFS;
import static com.squareup.protoparser.TestUtils.NO_OPTIONS;
import static com.squareup.protoparser.TestUtils.NO_TYPES;
import static com.squareup.protoparser.TestUtils.list;
import static com.squareup.protoparser.TestUtils.map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

public final class ProtoSchemaParserTest {
  @Test public void field() throws Exception {
    FieldElement field = FieldElement.create(OPTIONAL, "CType", "ctype", 1, "",
        list(OptionElement.create("default", "STRING", false),
            OptionElement.create("deprecated", "true", false)));
    assertThat(field.isDeprecated()).isTrue();
    assertThat(field.getDefault()).isEqualTo("STRING");
    assertThat(field.options()).containsOnly( //
        OptionElement.create("default", "STRING", false), //
        OptionElement.create("deprecated", "true", false));
  }

  @Test public void documentationFormats() {
    // Single-line comment.
    String proto1 = ""
        + "// Test all the things!\n"
        + "message Test {}";
    ProtoFile parsed1 = ProtoSchemaParser.parse("test.proto", proto1);
    MessageElement element1 = (MessageElement) parsed1.typeElements().get(0);
    assertThat(element1.documentation()).isEqualTo("Test all the things!");

    // Multiple, single-line comment.
    String proto2 = ""
        + "// Test all\n"
        + "// the things!\n"
        + "message Test {}";
    String expected2 = ""
        + "Test all\n"
        + "the things!";
    ProtoFile parsed2 = ProtoSchemaParser.parse("test.proto", proto2);
    MessageElement element2 = (MessageElement) parsed2.typeElements().get(0);
    assertThat(element2.documentation()).isEqualTo(expected2);

    // Single-line, Javadoc-esque comment.
    String proto3 = ""
        + "/** Test */\n"
        + "message Test {}";
    ProtoFile parsed3 = ProtoSchemaParser.parse("test.proto", proto3);
    MessageElement element3 = (MessageElement) parsed3.typeElements().get(0);
    assertThat(element3.documentation()).isEqualTo("Test");

    // Multi-line, Javadoc-esque comment.
    String proto4 = ""
        + "/**\n"
        + " * Test\n"
        + " *\n"
        + " * Foo\n"
        + " */\n"
        + "message Test {}";
    String expected4 = ""
        + "Test\n"
        + "\n"
        + "Foo";
    ProtoFile parsed4 = ProtoSchemaParser.parse("test.proto", proto4);
    MessageElement element4 = (MessageElement) parsed4.typeElements().get(0);
    assertThat(element4.documentation()).isEqualTo(expected4);

    // Multiple, single-line comment with leading whitespace
    String proto5 = ""
        + "// Test\n"
        + "//   All\n"
        + "//     The\n"
        + "//       Things!\n"
        + "message Test {}";
    String expected5 = ""
        + "Test\n"
        + "  All\n"
        + "    The\n"
        + "      Things!";
    ProtoFile parsed5 = ProtoSchemaParser.parse("test.proto", proto5);
    MessageElement element5 = (MessageElement) parsed5.typeElements().get(0);
    assertThat(element5.documentation()).isEqualTo(expected5);

    // Multi-line, Javadoc-esque comment.
    String proto6 = ""
        + "/**\n"
        + " * Test\n"
        + " *   All\n"
        + " *     The\n"
        + " *       Things!\n"
        + " */\n"
        + "message Test {}";
    String expected6 = ""
        + "Test\n"
        + "  All\n"
        + "    The\n"
        + "      Things!";
    ProtoFile parsed6 = ProtoSchemaParser.parse("test.proto", proto6);
    MessageElement element6 = (MessageElement) parsed6.typeElements().get(0);
    assertThat(element6.documentation()).isEqualTo(expected6);

    // Multi-line, poorly-formatted Javadoc-esque comment. The lack of leading asterisks prevents
    // us from preserving any leading whitespace.
    String proto7 = ""
        + "/**\n"
        + " Test\n"
        + "   All\n"
        + "     The\n"
        + "       Things!\n"
        + " */\n"
        + "message Test {}";
    String expected7 = ""
        + "Test\n"
        + "All\n"
        + "The\n"
        + "Things!";
    ProtoFile parsed7 = ProtoSchemaParser.parse("test.proto", proto7);
    MessageElement element7 = (MessageElement) parsed7.typeElements().get(0);
    assertThat(element7.documentation()).isEqualTo(expected7);
  }

  @Test public void syntaxNotRequired() throws Exception {
    String proto = "message Foo {}";
    ProtoFile parsed = ProtoSchemaParser.parse("test.proto", proto);
    assertThat(parsed.syntax()).isNull();
  }

  @Test public void syntaxSpecified() throws Exception {
    String proto = ""
        + "syntax \"proto3\";\n"
        + "message Foo {}";
    TypeElement foo =
        MessageElement.create("Foo", "Foo", "", NO_FIELDS, NO_ONEOFS, NO_TYPES, NO_EXTENSIONS,
            NO_OPTIONS);
    ProtoFile expected =
        ProtoFile.builder("test.proto").setSyntax(ProtoFile.Syntax.PROTO_3).addType(foo).build();
    assertThat(ProtoSchemaParser.parse("test.proto", proto)).isEqualTo(expected);
  }

  @Test public void invalidSyntaxValueThrows() throws Exception {
    String proto = ""
        + "syntax \"proto4\";\n"
        + "message Foo {}";
    try {
      ProtoSchemaParser.parse("test.proto", proto);
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
      ProtoSchemaParser.parse("test.proto", proto);
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
    TypeElement expected = MessageElement.create("SearchRequest", "SearchRequest", "", Arrays.asList(
        FieldElement.create(REQUIRED, "string", "query", 1, "", NO_OPTIONS),
        FieldElement.create(OPTIONAL, "int32", "page_number", 2, "", NO_OPTIONS),
        FieldElement.create(OPTIONAL, "int32", "result_per_page", 3, "", NO_OPTIONS)),
        NO_ONEOFS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile protoFile = ProtoFile.builder("search.proto").addType(expected).build();
    assertThat(ProtoSchemaParser.parse("search.proto", new StringReader(proto))) //
        .isEqualTo(protoFile);
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
    TypeElement expected = MessageElement.create("SearchRequest", "SearchRequest", "", list(
            FieldElement.create(REQUIRED, "string", "query", 1, "", NO_OPTIONS)),
        list(OneOfElement.create("page_info", "",
            list(FieldElement.create(ONE_OF, "int32", "page_number", 2, "", NO_OPTIONS),
                FieldElement.create(ONE_OF, "int32", "result_per_page", 3, "", NO_OPTIONS)))),
        NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile protoFile = ProtoFile.builder("search.proto").addType(expected).build();
    assertThat(ProtoSchemaParser.parse("search.proto", new StringReader(proto))) //
        .isEqualTo(protoFile);
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
    TypeElement expected =
        EnumElement.create("Topping", "Topping", "What's on my waffles.\nAlso works on pancakes.",
            NO_OPTIONS, Arrays.asList(EnumConstantElement.create("FRUIT", 1, "", NO_OPTIONS),
            EnumConstantElement.create("CREAM", 2, "Yummy, yummy cream.", NO_OPTIONS),
            EnumConstantElement.create("SYRUP", 3, "Quebec Maple syrup", NO_OPTIONS)));
    ProtoFile protoFile = ProtoFile.builder("waffles.proto").addType(expected).build();
    ProtoFile actual = ProtoSchemaParser.parse("waffles.proto", proto);
    assertThat(actual).isEqualTo(protoFile);
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
    List<OptionElement> fruitOptions = list(OptionElement.create("healthy", true, true));
    List<OptionElement> toppingOptions = list(OptionElement.create("max_choices", 2, true));
    TypeElement expected = EnumElement.create("Topping", "Topping",
        "What's on my waffles.\nAlso works on pancakes.", toppingOptions,
        list(EnumConstantElement.create("FRUIT", 1, "", fruitOptions),
            EnumConstantElement.create("CREAM", 2, "Yummy, yummy cream.", NO_OPTIONS),
            EnumConstantElement.create("SYRUP", 3, "Quebec Maple syrup", NO_OPTIONS)));
    ProtoFile protoFile = ProtoFile.builder("waffles.proto").addType(expected).build();
    ProtoFile actual = ProtoSchemaParser.parse("waffles.proto", proto);
    assertThat(actual).isEqualTo(protoFile);
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
    TypeElement message =
        MessageElement.create("FileDescriptorSet", "google.protobuf.FileDescriptorSet", ""
                + "The protocol compiler can output a FileDescriptorSet containing the .proto\n"
                + "files it parses.", NO_FIELDS, NO_ONEOFS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    OptionElement option = OptionElement.create("java_package", "com.google.protobuf", false);
    ProtoFile expected = ProtoFile.builder("descriptor.proto")
        .setPackageName("google.protobuf")
        .addType(message)
        .addOption(option)
        .build();
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(expected);
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
    TypeElement enumElement = EnumElement.create("CType", "FieldOptions.CType", "", NO_OPTIONS,
        Arrays.asList(EnumConstantElement.create("STRING", 0, "",
            Arrays.asList(OptionElement.create("opt_a", 1, true),
                OptionElement.create("opt_b", 2, true)))));
    FieldElement field = FieldElement.create(OPTIONAL, "CType", "ctype", 1, "",
        list(OptionElement.create("default", EnumConstantElement.anonymous("STRING"), false),
            OptionElement.create("deprecated", true, false)));
    assertThat(field.options()).containsOnly( //
        OptionElement.create("default", EnumConstantElement.anonymous("STRING"), false), //
        OptionElement.create("deprecated", true, false));

    TypeElement messageElement =
        MessageElement.create("FieldOptions", "FieldOptions", "", Arrays.asList(field), NO_ONEOFS,
            Arrays.asList(enumElement), list(ExtensionsElement.create(
                "Clients can define custom options in extensions of this message. See above.", 500,
                500), ExtensionsElement.create("", 1000, ProtoFile.MAX_TAG_VALUE)), NO_OPTIONS);
    ProtoFile expected = ProtoFile.builder("descriptor.proto").addType(messageElement).build();
    ProtoFile actual = ProtoSchemaParser.parse("descriptor.proto", proto);
    assertThat(actual).isEqualTo(expected);
  }

  @Test public void optionParentheses() throws Exception {
    String proto = ""
        + "message Chickens {\n"
        + "  optional boolean koka_ko_koka_ko = 1 [default = true];\n"
        + "  optional boolean coodle_doodle_do = 2 [(delay) = 100, default = false];\n"
        + "  optional boolean coo_coo_ca_cha = 3 [default = true, (delay) = 200];\n"
        + "  optional boolean cha_chee_cha = 4;\n"
        + "}\n";

    FieldElement
        gobChicken = FieldElement.create(OPTIONAL, "boolean", "koka_ko_koka_ko", 1, "",
        list(OptionElement.create("default", true, false)));
    FieldElement
        lucilleChicken = FieldElement.create(OPTIONAL, "boolean", "coodle_doodle_do", 2, "",
        list(OptionElement.create("delay", 100, true),
            OptionElement.create("default", false, false)));
    FieldElement
        georgeSrChicken = FieldElement.create(OPTIONAL, "boolean", "coo_coo_ca_cha", 3, "",
        list(OptionElement.create("default", true, false),
            OptionElement.create("delay", 200, true)));
    FieldElement
        lindsayChicken = FieldElement.create(OPTIONAL, "boolean", "cha_chee_cha", 4, "", NO_OPTIONS);

    TypeElement messageElement = MessageElement.create("Chickens", "Chickens", "",
        Arrays.asList(gobChicken, lucilleChicken, georgeSrChicken, lindsayChicken), NO_ONEOFS,
        NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);

    ProtoFile expected = ProtoFile.builder("chickens.proto").addType(messageElement).build();

    ProtoFile actual = ProtoSchemaParser.parse("chickens.proto", proto);
    assertThat(actual).isEqualTo(expected);
  }

  @Test public void imports() throws Exception {
    String proto = "import \"src/test/resources/unittest_import.proto\";\n";
    ProtoFile expected = ProtoFile.builder("descriptor.proto")
        .addDependency("src/test/resources/unittest_import.proto")
        .build();
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(expected);
  }

  @Test public void publicImports() throws Exception {
    String proto = "import public \"src/test/resources/unittest_import.proto\";\n";
    ProtoFile expected = ProtoFile.builder("descriptor.proto")
        .addPublicDependency("src/test/resources/unittest_import.proto")
        .build();
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(expected);
  }

  @Test public void extend() throws Exception {
    String proto = ""
        + "// Extends Foo\n"
        + "extend Foo {\n"
        + "  optional int32 bar = 126;\n"
        + "}";
    ExtendElement extendDeclaration = ExtendElement.create("Foo", "Foo", "Extends Foo",
        Arrays.asList(FieldElement.create(OPTIONAL, "int32", "bar", 126, "", NO_OPTIONS)));
    ProtoFile expected =
        ProtoFile.builder("descriptor.proto").addExtendDeclaration(extendDeclaration).build();
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(expected);
  }

  @Test public void extendInMessage() throws Exception {
    String proto = ""
        + "message Bar {\n"
        + "  extend Foo {\n"
        + "    optional Bar bar = 126;\n"
        + "  }\n"
        + "}";
    ExtendElement extendElement = ExtendElement.create("Foo", "Foo", "",
        Arrays.asList(FieldElement.create(OPTIONAL, "Bar", "bar", 126, "", NO_OPTIONS)));
    TypeElement messageElement =
        MessageElement.create("Bar", "Bar", "", NO_FIELDS, NO_ONEOFS, NO_TYPES, NO_EXTENSIONS,
            NO_OPTIONS);
    ProtoFile expected = ProtoFile.builder("descriptor.proto")
        .addType(messageElement)
        .addExtendDeclaration(extendElement)
        .build();
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(expected);
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
    ExtendElement extendElement = ExtendElement.create("Foo", "kit.kat.Foo", "",
        Arrays.asList(FieldElement.create(OPTIONAL, "Bar", "bar", 126, "", NO_OPTIONS)));
    TypeElement messageElement =
        MessageElement.create("Bar", "kit.kat.Bar", "", NO_FIELDS, NO_ONEOFS, NO_TYPES,
            NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile expected = ProtoFile.builder("descriptor.proto")
        .setPackageName("kit.kat")
        .addType(messageElement)
        .addExtendDeclaration(extendElement)
        .build();
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(expected);
  }

  @Test public void fqcnExtendInMessage() throws Exception {
    String proto = ""
        + "message Bar {\n"
        + "  extend example.Foo {\n"
        + "    optional Bar bar = 126;\n"
        + "  }\n"
        + "}";
    ExtendElement extendElement = ExtendElement.create("example.Foo", "example.Foo", "",
        Arrays.asList(FieldElement.create(OPTIONAL, "Bar", "bar", 126, "", NO_OPTIONS)));
    TypeElement messageElement =
        MessageElement.create("Bar", "Bar", "", NO_FIELDS, NO_ONEOFS, NO_TYPES, NO_EXTENSIONS,
            NO_OPTIONS);
    ProtoFile expected = ProtoFile.builder("descriptor.proto")
        .addType(messageElement)
        .addExtendDeclaration(extendElement)
        .build();
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(expected);
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
    ExtendElement extendElement = ExtendElement.create("example.Foo", "example.Foo", "",
        Arrays.asList(FieldElement.create(OPTIONAL, "Bar", "bar", 126, "", NO_OPTIONS)));
    TypeElement messageElement =
        MessageElement.create("Bar", "kit.kat.Bar", "", NO_FIELDS, NO_ONEOFS, NO_TYPES,
            NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile expected = ProtoFile.builder("descriptor.proto")
        .setPackageName("kit.kat")
        .addType(messageElement)
        .addExtendDeclaration(extendElement)
        .build();
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(expected);
  }

  @Test public void defaultFieldWithParen() throws Exception {
    String proto = ""
        + "message Foo {\n"
        + "  optional string claim_token = 2 [(squareup.redacted) = true];\n"
        + "}";
    FieldElement field =
        FieldElement.create(OPTIONAL, "string", "claim_token", 2, "",
            list(OptionElement.create("squareup.redacted", true, true)));
    assertThat(field.options()).containsOnly(OptionElement.create("squareup.redacted", true, true));

    TypeElement messageElement =
        MessageElement.create("Foo", "Foo", "", Arrays.asList(field), NO_ONEOFS, NO_TYPES,
            NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile expected = ProtoFile.builder("descriptor.proto").addType(messageElement).build();
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
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
        field = FieldElement.create(OPTIONAL, "string", "name", 1, "", list(
        OptionElement.create("default",
            "\u0007\b\f\n\r\t\u000b\u0001f\u0001\u0001\u0009\u0009I\u000e\u000e\u000e\u000eAA",
            false)));
    assertThat(field.options()).containsOnly(OptionElement.create("default",
        "\u0007\b\f\n\r\t\u000b\u0001f\u0001\u0001\u0009\u0009I\u000e\u000e\u000e\u000eAA", false));

    TypeElement messageElement = MessageElement.create("Foo", "Foo", "", Arrays.asList(field),
            NO_ONEOFS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile expected = ProtoFile.builder("foo.proto").addType(messageElement).build();
    assertThat(ProtoSchemaParser.parse("foo.proto", proto))
        .isEqualTo(expected);
  }

  @Test public void invalidHexStringEscape() throws Exception {
    String proto = ""
        + "message Foo {\n"
        + "  optional string name = 1 "
        + "[default = \"\\xW\"];\n"
        + "}";
    try {
      ProtoSchemaParser.parse("foo.proto", proto);
      fail("Expected parse error");
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
        + "    option (squareup.a.b) = { value: [FOO, BAR] };\n"
        + "  }\n"
        + "}";
    List<OptionElement> options = list(OptionElement.create("default_timeout", 30, true));
    ServiceElement expected = ServiceElement.create("SearchService", "SearchService", "", options,
        list(RpcElement.create("Search", "", "SearchRequest", "SearchResponse", NO_OPTIONS),
            RpcElement.create("Purchase", "", "PurchaseRequest", "PurchaseResponse", list( //
                    OptionElement.create("squareup.sake.timeout", 15, true), //
                    OptionElement.create("squareup.a.b", map("value", //
                        list(EnumConstantElement.anonymous("FOO"),
                            EnumConstantElement.anonymous("BAR"))), true)))));
    ProtoFile protoFile = ProtoFile.builder("descriptor.proto").addService(expected).build();
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(protoFile);
  }

  @Test public void hexTag() throws Exception {
    String proto = ""
        + "message HexTag {\n"
        + "  required string hex = 0x10;\n"
        + "}";
    TypeElement expected = MessageElement.create("HexTag", "HexTag", "", Arrays.asList(
            FieldElement.create(REQUIRED, "string", "hex", 16, "", NO_OPTIONS)), NO_ONEOFS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile protoFile = ProtoFile.builder("hex.proto").addType(expected).build();
    assertThat(ProtoSchemaParser.parse("hex.proto", proto)).isEqualTo(protoFile);
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

    List<OptionElement> options = new ArrayList<>();
    Map<String, String> option_one_map = new LinkedHashMap<>();
    option_one_map.put("name", "Name");
    option_one_map.put("class_name", "ClassName");
    options.add(OptionElement.create("squareup.one", option_one_map, true));
    Map<String, Object> option_two_a_map = new LinkedHashMap<>();
    option_two_a_map.put("[squareup.options.type]", EnumConstantElement.anonymous("EXOTIC"));
    options.add(OptionElement.create("squareup.two.a", option_two_a_map, true));
    Map<String, List<String>> option_two_b_map = new LinkedHashMap<>();
    option_two_b_map.put("names", Arrays.asList("Foo", "Bar"));
    options.add(OptionElement.create("squareup.two.b", option_two_b_map, true));
    Map<String, Map<String, ?>> option_three_map = new LinkedHashMap<>();
    Map<String, Object> option_three_nested_map = new LinkedHashMap<>();
    option_three_nested_map.put("y", Arrays.asList(1, 2));
    option_three_map.put("x", option_three_nested_map);
    options.add(OptionElement.create("squareup.three", option_three_map, true));

    Map<String, Map<String, ?>> option_four_map = new LinkedHashMap<>();
    Map<String, Object> option_four_map_1 = new LinkedHashMap<>();
    Map<String, Object> option_four_map_2_a = new LinkedHashMap<>();
    option_four_map_2_a.put("z", 1);
    Map<String, Object> option_four_map_2_b = new LinkedHashMap<>();
    option_four_map_2_b.put("z", 2);
    option_four_map_1.put("y", Arrays.asList(option_four_map_2_a, option_four_map_2_b));
    option_four_map.put("x", option_four_map_1);
    options.add(OptionElement.create("squareup.four", option_four_map, true));

    TypeElement expected =
        MessageElement.create("ExoticOptions", "ExoticOptions", "", NO_FIELDS, NO_ONEOFS,
            NO_TYPES, NO_EXTENSIONS, options);
    ProtoFile protoFile = ProtoFile.builder("exotic.proto").addType(expected).build();
    assertThat(ProtoSchemaParser.parse("exotic.proto", proto)).isEqualTo(protoFile);
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
    FieldElement field =
        FieldElement.create(OPTIONAL, "field.type", "has_options", 3, "", list(
            OptionElement.create("option_map",
                map("nested_map", map("key", "value", "key2", list("value2a", "value2b"))), true),
            OptionElement.create("option_string", list("string1", "string2"), true)));
    assertThat(field.options()).containsOnly( //
        OptionElement.create("option_map",
            map("nested_map", map("key", "value", "key2", list("value2a", "value2b"))), true),
        OptionElement.create("option_string", list("string1", "string2"), true));

    TypeElement expected = MessageElement.create("StructuredOption", "StructuredOption", "",
        Arrays.asList(field), NO_ONEOFS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile protoFile = ProtoFile.builder("nestedmaps.proto").addType(expected).build();
    assertThat(ProtoSchemaParser.parse("nestedmaps.proto", proto))
        .isEqualTo(protoFile);
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
    FieldElement
        field = FieldElement.create(OPTIONAL, "int32", "bar", 1, "",
        list(OptionElement.create("validation.range", OptionElement.create("min", 1, true), true),
            OptionElement.create("validation.range", OptionElement.create("max", 100, true), true),
            OptionElement.create("default", 20, false)));
    assertThat(field.options()).containsOnly( //
        OptionElement.create("validation.range", OptionElement.create("min", 1, true), true), //
        OptionElement.create("validation.range", OptionElement.create("max", 100, true), true), //
        OptionElement.create("default", 20, false));

    TypeElement expected =
        MessageElement.create("Foo", "Foo", "", Arrays.asList(field), NO_ONEOFS, NO_TYPES,
            NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile protoFile = ProtoFile.builder("foo.proto").addType(expected).build();
    assertThat(ProtoSchemaParser.parse("foo.proto", proto)).isEqualTo(protoFile);
  }

  @Test public void noWhitespace() {
    String proto = "message C {optional A.B ab = 1;}";
    ProtoFile parse = ProtoSchemaParser.parse("test.proto", proto);

    FieldElement field =
        FieldElement.create(OPTIONAL, "A.B", "ab", 1, "", NO_OPTIONS);
    TypeElement element =
        MessageElement.create("C", "C", "", list(field), NO_ONEOFS, NO_TYPES, NO_EXTENSIONS,
            NO_OPTIONS);
    ProtoFile expected = ProtoFile.builder("test.proto").addType(element).build();

    assertThat(parse).isEqualTo(expected);
  }
}
