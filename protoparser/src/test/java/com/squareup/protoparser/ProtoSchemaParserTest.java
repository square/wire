// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import com.squareup.protoparser.EnumType.Value;
import com.squareup.protoparser.MessageType.Label;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.fest.assertions.api.Fail;
import org.junit.Test;

import static com.squareup.protoparser.TestUtils.NO_EXTEND_DECLARATIONS;
import static com.squareup.protoparser.TestUtils.NO_EXTENSIONS;
import static com.squareup.protoparser.TestUtils.NO_FIELDS;
import static com.squareup.protoparser.TestUtils.NO_OPTIONS;
import static com.squareup.protoparser.TestUtils.NO_SERVICES;
import static com.squareup.protoparser.TestUtils.NO_STRINGS;
import static com.squareup.protoparser.TestUtils.NO_TYPES;
import static com.squareup.protoparser.TestUtils.list;
import static com.squareup.protoparser.TestUtils.map;
import static org.fest.assertions.api.Assertions.assertThat;

public final class ProtoSchemaParserTest {
  @Test public void field() throws Exception {
    MessageType.Field field = new MessageType.Field(Label.OPTIONAL, "CType", "ctype", 1, "",
        list(new Option("default", "STRING", false), new Option("deprecated", "true", false)));
    assertThat(field.isDeprecated()).isTrue();
    assertThat(field.getDefault()).isEqualTo("STRING");
    assertThat(field.getOptions()).containsOnly( //
        new Option("default", "STRING", false), //
        new Option("deprecated", "true", false));
  }

  @Test public void documentationFormats() {
    // Single-line comment.
    String proto1 = ""
        + "// Test all the things!\n"
        + "message Test {}";
    ProtoFile parsed1 = ProtoSchemaParser.parse("test.proto", proto1);
    MessageType type1 = (MessageType) parsed1.getTypes().get(0);
    assertThat(type1.getDocumentation()).isEqualTo("Test all the things!");

    // Multiple, single-line comment.
    String proto2 = ""
        + "// Test all\n"
        + "// the things!\n"
        + "message Test {}";
    String expected2 = ""
        + "Test all\n"
        + "the things!";
    ProtoFile parsed2 = ProtoSchemaParser.parse("test.proto", proto2);
    MessageType type2 = (MessageType) parsed2.getTypes().get(0);
    assertThat(type2.getDocumentation()).isEqualTo(expected2);

    // Single-line, Javadoc-esque comment.
    String proto3 = ""
        + "/** Test */\n"
        + "message Test {}";
    ProtoFile parsed3 = ProtoSchemaParser.parse("test.proto", proto3);
    MessageType type3 = (MessageType) parsed3.getTypes().get(0);
    assertThat(type3.getDocumentation()).isEqualTo("Test");

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
    MessageType type4 = (MessageType) parsed4.getTypes().get(0);
    assertThat(type4.getDocumentation()).isEqualTo(expected4);

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
    MessageType type5 = (MessageType) parsed5.getTypes().get(0);
    assertThat(type5.getDocumentation()).isEqualTo(expected5);

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
    MessageType type6 = (MessageType) parsed6.getTypes().get(0);
    assertThat(type6.getDocumentation()).isEqualTo(expected6);

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
    MessageType type7 = (MessageType) parsed7.getTypes().get(0);
    assertThat(type7.getDocumentation()).isEqualTo(expected7);
  }

  @Test public void parseMessageAndFields() throws Exception {
    String proto = ""
        + "message SearchRequest {\n"
        + "  required string query = 1;\n"
        + "  optional int32 page_number = 2;\n"
        + "  optional int32 result_per_page = 3;\n"
        + "}";
    Type expected = new MessageType("SearchRequest", "SearchRequest", "",
        Arrays.asList(new MessageType.Field(Label.REQUIRED, "string", "query", 1, "", NO_OPTIONS),
            new MessageType.Field(Label.OPTIONAL, "int32", "page_number", 2, "", NO_OPTIONS),
            new MessageType.Field(Label.OPTIONAL, "int32", "result_per_page", 3, "", NO_OPTIONS)),
        NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile protoFile =
        new ProtoFile("search.proto", null, NO_STRINGS, NO_STRINGS, Arrays.asList(expected),
            NO_SERVICES, NO_OPTIONS, NO_EXTEND_DECLARATIONS);
    assertThat(ProtoSchemaParser.parse("search.proto", new StringReader(proto))).isEqualTo(protoFile);
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
    Type expected = new EnumType("Topping", "Topping",
       "What's on my waffles.\nAlso works on pancakes.", NO_OPTIONS,
        Arrays.asList(new Value("FRUIT", 1, "", NO_OPTIONS), new Value("CREAM", 2, "Yummy, yummy cream.",
            NO_OPTIONS), new Value("SYRUP", 3, "Quebec Maple syrup", NO_OPTIONS)));
    ProtoFile protoFile =
        new ProtoFile("waffles.proto", null, NO_STRINGS, NO_STRINGS, Arrays.asList(expected),
            NO_SERVICES, NO_OPTIONS, NO_EXTEND_DECLARATIONS);
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
    List<Option> fruitOptions = list(new Option("healthy", true, true));
    List<Option> toppingOptions = list(new Option("max_choices", 2, true));
    Type expected = new EnumType("Topping", "Topping",
        "What's on my waffles.\nAlso works on pancakes.", toppingOptions,
        list(new Value("FRUIT", 1, "", fruitOptions),
            new Value("CREAM", 2, "Yummy, yummy cream.", NO_OPTIONS),
            new Value("SYRUP", 3, "Quebec Maple syrup", NO_OPTIONS)));
    ProtoFile protoFile =
        new ProtoFile("waffles.proto", null, NO_STRINGS, NO_STRINGS, Arrays.asList(expected),
            NO_SERVICES, NO_OPTIONS, NO_EXTEND_DECLARATIONS);
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
    Type message = new MessageType("FileDescriptorSet", "google.protobuf.FileDescriptorSet", ""
        + "The protocol compiler can output a FileDescriptorSet containing the .proto\n"
        + "files it parses.", Arrays.<MessageType.Field>asList(), NO_TYPES, NO_EXTENSIONS,
        NO_OPTIONS);
    ProtoFile expected =
        new ProtoFile("descriptor.proto", "google.protobuf", NO_STRINGS, NO_STRINGS,
            Arrays.asList(message), NO_SERVICES,
            list(new Option("java_package", "com.google.protobuf", false)), NO_EXTEND_DECLARATIONS);
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
    Type enumType = new EnumType("CType", "FieldOptions.CType", "", NO_OPTIONS,
        Arrays.asList(new Value("STRING", 0, "",
            Arrays.asList(new Option("opt_a", 1, true), new Option("opt_b", 2, true)))));
    MessageType.Field field = new MessageType.Field(Label.OPTIONAL, "CType", "ctype", 1, "",
        list(new Option("default", EnumType.Value.anonymous("STRING"), false),
            new Option("deprecated", true, false)));
    assertThat(field.getOptions()).containsOnly( //
        new Option("default", EnumType.Value.anonymous("STRING"), false), //
        new Option("deprecated", true, false));

    Type messageType = new MessageType("FieldOptions", "FieldOptions", "", Arrays.asList(field),
        Arrays.asList(enumType), list(new Extensions(
        "Clients can define custom options in extensions of this message. See above.", 500, 500),
        new Extensions("", 1000, ProtoFile.MAX_TAG_VALUE)), NO_OPTIONS);
    ProtoFile expected =
        new ProtoFile("descriptor.proto", null, NO_STRINGS, NO_STRINGS, Arrays.asList(messageType),
            NO_SERVICES, NO_OPTIONS, NO_EXTEND_DECLARATIONS);
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

    MessageType.Field gobChicken = new MessageType.Field(Label.OPTIONAL, "boolean",
        "koka_ko_koka_ko", 1, "", list(new Option("default", true, false)));
    MessageType.Field lucilleChicken = new MessageType.Field(Label.OPTIONAL, "boolean",
        "coodle_doodle_do", 2, "", list(new Option("delay", 100, true),
        new Option("default", false, false)));
    MessageType.Field georgeSrChicken = new MessageType.Field(Label.OPTIONAL, "boolean",
        "coo_coo_ca_cha", 3, "", list(new Option("default", true, false),
        new Option("delay", 200, true)));
    MessageType.Field lindsayChicken = new MessageType.Field(Label.OPTIONAL, "boolean",
        "cha_chee_cha", 4, "", NO_OPTIONS);

    Type messageType = new MessageType("Chickens", "Chickens", "",
        Arrays.asList(gobChicken, lucilleChicken, georgeSrChicken, lindsayChicken),
        NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);

    ProtoFile expected =
        new ProtoFile("chickens.proto", null, NO_STRINGS, NO_STRINGS, Arrays.asList(messageType),
            NO_SERVICES, NO_OPTIONS, NO_EXTEND_DECLARATIONS);

    ProtoFile actual = ProtoSchemaParser.parse("chickens.proto", proto);
    assertThat(actual).isEqualTo(expected);
  }

  @Test public void imports() throws Exception {
    String proto = "import \"src/test/resources/unittest_import.proto\";\n";
    ProtoFile expected = new ProtoFile("descriptor.proto", null,
        Arrays.asList("src/test/resources/unittest_import.proto"), NO_STRINGS, NO_TYPES,
        NO_SERVICES, NO_OPTIONS, NO_EXTEND_DECLARATIONS);
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(expected);
  }

  @Test public void publicImports() throws Exception {
    String proto = "import public \"src/test/resources/unittest_import.proto\";\n";
    ProtoFile expected = new ProtoFile("descriptor.proto", null,
        NO_STRINGS, Arrays.asList("src/test/resources/unittest_import.proto"), NO_TYPES,
        NO_SERVICES, NO_OPTIONS, NO_EXTEND_DECLARATIONS);
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(expected);
  }

  @Test public void extend() throws Exception {
    String proto = ""
        + "// Extends Foo\n"
        + "extend Foo {\n"
        + "  optional int32 bar = 126;\n"
        + "}";
    List<ExtendDeclaration> extendDeclarations = new ArrayList<ExtendDeclaration>();
    extendDeclarations.add(new ExtendDeclaration("Foo", "Foo", "Extends Foo",
        Arrays.asList(new MessageType.Field(Label.OPTIONAL, "int32", "bar", 126, "", NO_OPTIONS))));
    ProtoFile expected =
        new ProtoFile("descriptor.proto", null, NO_STRINGS, NO_STRINGS, NO_TYPES, NO_SERVICES,
            NO_OPTIONS, extendDeclarations);
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
    List<ExtendDeclaration> extendDeclarations = new ArrayList<ExtendDeclaration>();
    extendDeclarations.add(new ExtendDeclaration("Foo", "Foo", "",
        Arrays.asList(new MessageType.Field(Label.OPTIONAL, "Bar", "bar", 126, "", NO_OPTIONS))));
    Type messageType =
        new MessageType("Bar", "Bar", "", NO_FIELDS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile expected =
        new ProtoFile("descriptor.proto", null, NO_STRINGS, NO_STRINGS, Arrays.asList(messageType),
            NO_SERVICES, NO_OPTIONS, extendDeclarations);
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
    List<ExtendDeclaration> extendDeclarations = new ArrayList<ExtendDeclaration>();
    extendDeclarations.add(new ExtendDeclaration("Foo", "kit.kat.Foo", "",
        Arrays.asList(new MessageType.Field(Label.OPTIONAL, "Bar", "bar", 126, "", NO_OPTIONS))));
    Type messageType =
        new MessageType("Bar", "kit.kat.Bar", "", NO_FIELDS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile expected =
        new ProtoFile("descriptor.proto", "kit.kat", NO_STRINGS, NO_STRINGS,
            Arrays.asList(messageType), NO_SERVICES, NO_OPTIONS, extendDeclarations);
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
    List<ExtendDeclaration> extendDeclarations = new ArrayList<ExtendDeclaration>();
    extendDeclarations.add(new ExtendDeclaration("example.Foo", "example.Foo", "",
        Arrays.asList(new MessageType.Field(Label.OPTIONAL, "Bar", "bar", 126, "", NO_OPTIONS))));
    Type messageType =
        new MessageType("Bar", "Bar", "", NO_FIELDS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile expected =
        new ProtoFile("descriptor.proto", null, NO_STRINGS, NO_STRINGS, Arrays.asList(messageType),
            NO_SERVICES, NO_OPTIONS, extendDeclarations);
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
    List<ExtendDeclaration> extendDeclarations = new ArrayList<ExtendDeclaration>();
    extendDeclarations.add(new ExtendDeclaration("example.Foo", "example.Foo", "",
        Arrays.asList(new MessageType.Field(Label.OPTIONAL, "Bar", "bar", 126, "", NO_OPTIONS))));
    Type messageType =
        new MessageType("Bar", "kit.kat.Bar", "", NO_FIELDS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile expected =
        new ProtoFile("descriptor.proto", "kit.kat", NO_STRINGS, NO_STRINGS,
            Arrays.asList(messageType), NO_SERVICES, NO_OPTIONS, extendDeclarations);
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(expected);
  }

  @Test public void defaultFieldWithParen() throws Exception {
    String proto = ""
        + "message Foo {\n"
        + "  optional string claim_token = 2 [(squareup.redacted) = true];\n"
        + "}";
    MessageType.Field field = new MessageType.Field(Label.OPTIONAL, "string", "claim_token", 2, "",
        list(new Option("squareup.redacted", true, true)));
    assertThat(field.getOptions()).containsOnly(new Option("squareup.redacted", true, true));

    Type messageType = new MessageType("Foo", "Foo", "", Arrays.asList(field), NO_TYPES,
        NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile expected = new ProtoFile("descriptor.proto", null, NO_STRINGS, NO_STRINGS,
        Arrays.<Type>asList(messageType), NO_SERVICES, NO_OPTIONS, NO_EXTEND_DECLARATIONS);
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
    MessageType.Field field = new MessageType.Field(Label.OPTIONAL, "string", "name", 1, "", list(
        new Option("default",
            "\u0007\b\f\n\r\t\u000b\u0001f\u0001\u0001\u0009\u0009I\u000e\u000e\u000e\u000eAA",
            false)));
    assertThat(field.getOptions()).containsOnly(new Option("default",
        "\u0007\b\f\n\r\t\u000b\u0001f\u0001\u0001\u0009\u0009I\u000e\u000e\u000e\u000eAA", false));

    Type messageType = new MessageType("Foo", "Foo", "", Arrays.asList(field),
            NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile expected =
        new ProtoFile("foo.proto", null, NO_STRINGS, NO_STRINGS, Arrays.<Type>asList(messageType),
            NO_SERVICES, NO_OPTIONS, NO_EXTEND_DECLARATIONS);
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
      Fail.fail("Expected parse error");
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
    List<Option> options = list(new Option("default_timeout", 30, true));
    Service expected = new Service("SearchService", "SearchService", "", options, list(
        new Service.Method("Search", "", "SearchRequest", "SearchResponse", NO_OPTIONS),
        new Service.Method("Purchase", "", "PurchaseRequest", "PurchaseResponse", list( //
            new Option("squareup.sake.timeout", 15, true), //
            new Option("squareup.a.b",
                map("value", //
                    list(EnumType.Value.anonymous("FOO"), EnumType.Value.anonymous("BAR"))),
                true)))));
    ProtoFile protoFile = new ProtoFile("descriptor.proto", null, NO_STRINGS, NO_STRINGS, NO_TYPES,
        Arrays.asList(expected), NO_OPTIONS, NO_EXTEND_DECLARATIONS);
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(protoFile);
  }

  @Test public void hexTag() throws Exception {
    String proto = ""
        + "message HexTag {\n"
        + "  required string hex = 0x10;\n"
        + "}";
    Type expected = new MessageType("HexTag", "HexTag", "",
        Arrays.asList(new MessageType.Field(Label.REQUIRED, "string", "hex", 16, "", NO_OPTIONS)),
        NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile protoFile =
        new ProtoFile("hex.proto", null, NO_STRINGS, NO_STRINGS, Arrays.asList(expected),
            NO_SERVICES, NO_OPTIONS, NO_EXTEND_DECLARATIONS);
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

    List<Option> options = new ArrayList<Option>();
    Map<String, String> option_one_map = new LinkedHashMap<String, String>();
    option_one_map.put("name", "Name");
    option_one_map.put("class_name", "ClassName");
    options.add(new Option("squareup.one", option_one_map, true));
    Map<String, Object> option_two_a_map = new LinkedHashMap<String, Object>();
    option_two_a_map.put("[squareup.options.type]", EnumType.Value.anonymous("EXOTIC"));
    options.add(new Option("squareup.two.a", option_two_a_map, true));
    Map<String, List<String>> option_two_b_map = new LinkedHashMap<String, List<String>>();
    option_two_b_map.put("names", Arrays.asList("Foo", "Bar"));
    options.add(new Option("squareup.two.b", option_two_b_map, true));
    Map<String, Map<String, ?>> option_three_map = new LinkedHashMap<String, Map<String, ?>>();
    Map<String, Object> option_three_nested_map = new LinkedHashMap<String, Object>();
    option_three_nested_map.put("y", Arrays.asList(1, 2));
    option_three_map.put("x", option_three_nested_map);
    options.add(new Option("squareup.three", option_three_map, true));

    Map<String, Map<String, ?>> option_four_map = new LinkedHashMap<String, Map<String, ?>>();
    Map<String, Object> option_four_map_1 = new LinkedHashMap<String, Object>();
    Map<String, Object> option_four_map_2_a = new LinkedHashMap<String, Object>();
    option_four_map_2_a.put("z", 1);
    Map<String, Object> option_four_map_2_b = new LinkedHashMap<String, Object>();
    option_four_map_2_b.put("z", 2);
    option_four_map_1.put("y", Arrays.asList(option_four_map_2_a, option_four_map_2_b));
    option_four_map.put("x", option_four_map_1);
    options.add(new Option("squareup.four", option_four_map, true));

    Type expected =
        new MessageType("ExoticOptions", "ExoticOptions", "", Arrays.<MessageType.Field>asList(),
            NO_TYPES, NO_EXTENSIONS, options);
    ProtoFile protoFile =
        new ProtoFile("exotic.proto", null, NO_STRINGS, NO_STRINGS, Arrays.asList(expected),
            NO_SERVICES, NO_OPTIONS, NO_EXTEND_DECLARATIONS);
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
    MessageType.Field field = new MessageType.Field(Label.OPTIONAL, "field.type", "has_options", 3,
        "", list(
        new Option("option_map", map("nested_map", map("key", "value", "key2",
          list("value2a", "value2b"))), true),
        new Option("option_string", list("string1","string2"), true)));
    assertThat(field.getOptions()).containsOnly( //
        new Option("option_map",
            map("nested_map", map("key", "value", "key2", list("value2a", "value2b"))), true),
        new Option("option_string", list("string1", "string2"), true));

    Type expected = new MessageType("StructuredOption", "StructuredOption", "",
        Arrays.<MessageType.Field>asList(field), NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile protoFile =
        new ProtoFile("nestedmaps.proto", null, NO_STRINGS, NO_STRINGS, Arrays.asList(expected),
            NO_SERVICES, NO_OPTIONS, NO_EXTEND_DECLARATIONS);
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
    MessageType.Field field = new MessageType.Field(Label.OPTIONAL, "int32", "bar", 1, "",
        list(new Option("validation.range", new Option("min", 1, true), true),
            new Option("validation.range", new Option("max", 100, true), true),
            new Option("default", 20, false)));
    assertThat(field.getOptions()).containsOnly( //
        new Option("validation.range", new Option("min", 1, true), true), //
        new Option("validation.range", new Option("max", 100, true), true), //
        new Option("default", 20, false));

    Type expected = new MessageType("Foo", "Foo", "", Arrays.asList(field), NO_TYPES, NO_EXTENSIONS,
        NO_OPTIONS);
    ProtoFile protoFile =
        new ProtoFile("foo.proto", null, NO_STRINGS, NO_STRINGS, Arrays.asList(expected),
            NO_SERVICES, NO_OPTIONS, NO_EXTEND_DECLARATIONS);
    assertThat(ProtoSchemaParser.parse("foo.proto", proto)).isEqualTo(protoFile);
  }

  @Test public void noWhitespace() {
    String proto = "message C {optional A.B ab = 1;}";
    ProtoFile parse = ProtoSchemaParser.parse("test.proto", proto);

    MessageType.Field field = new MessageType.Field(Label.OPTIONAL, "A.B", "ab", 1, "", NO_OPTIONS);
    Type type = new MessageType("C", "C", "", list(field), NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile expected =
        new ProtoFile("test.proto", null, NO_STRINGS, NO_STRINGS, list(type), NO_SERVICES,
            NO_OPTIONS, NO_EXTEND_DECLARATIONS);

    assertThat(parse).isEqualTo(expected);
  }
}
