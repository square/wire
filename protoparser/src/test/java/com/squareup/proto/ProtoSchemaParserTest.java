// Copyright 2013 Square, Inc.
package com.squareup.proto;

import com.squareup.proto.EnumType.Value;
import com.squareup.proto.MessageType.Label;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;

public final class ProtoSchemaParserTest extends TestCase {
  private static final List<Type> NO_TYPES = Collections.emptyList();
  private static final List<String> NO_STRINGS = Collections.emptyList();

  public void testField() throws Exception {
    MessageType.Field field = new MessageType.Field(Label.OPTIONAL, "CType", "ctype", 1, "",
        map("default", "STRING", "deprecated", "true"));
    assertTrue(field.isDeprecated());
    assertEquals("STRING", field.getDefault());
  }

  public void testParseMessageAndFields() throws Exception {
    String proto = ""
        + "message SearchRequest {\n"
        + "  required string query = 1;\n"
        + "  optional int32 page_number = 2;\n"
        + "  optional int32 result_per_page = 3;\n"
        + "}";
    Type expected = new MessageType("SearchRequest", "",
        Arrays.asList(new MessageType.Field(Label.REQUIRED, "string", "query", 1, "", map()),
            new MessageType.Field(Label.OPTIONAL, "int32", "page_number", 2, "", map()),
            new MessageType.Field(Label.OPTIONAL, "int32", "result_per_page", 3, "", map())),
        NO_TYPES);
    ProtoFile protoFile =
        new ProtoFile("search.proto", null, NO_STRINGS, Arrays.asList(expected), map());
    assertEquals(protoFile, new ProtoSchemaParser("search.proto", proto).readProtoFile());
  }

  public void testParseEnum() throws Exception {
    String proto = ""
        + "/**\n"
        + " * What's on my waffles.\n"
        + " * Also works on pancakes.\n"
        + " */\n"
        + "enum Topping {\n"
        + "  FRUIT = 1;\n"
        + "  CREAM = 2;\n"
        + "\n"
        + "  // Quebec Maple syrup\n"
        + "  SYRUP = 3;\n"
        + "}\n";
    Type expected = new EnumType("Topping", "What's on my waffles.\nAlso works on pancakes.",
        Arrays.asList(new Value("FRUIT", 1, ""), new Value("CREAM", 2, ""),
            new Value("SYRUP", 3, "Quebec Maple syrup")));
    ProtoFile protoFile =
        new ProtoFile("waffles.proto", null, NO_STRINGS, Arrays.asList(expected), map());
    ProtoFile actual = new ProtoSchemaParser("waffles.proto", proto).readProtoFile();
    assertEquals(protoFile, actual);
  }

  public void testPackage() throws Exception {
    String proto = ""
        + "package google.protobuf;\n"
        + "option java_package = \"com.google.protobuf\";\n"
        + "\n"
        + "// The protocol compiler can output a FileDescriptorSet containing the .proto\n"
        + "// files it parses.\n"
        + "message FileDescriptorSet {\n"
        + "}\n";
    Type message = new MessageType("FileDescriptorSet", ""
        + "The protocol compiler can output a FileDescriptorSet containing the .proto\n"
        + "files it parses.", Arrays.<MessageType.Field>asList(), NO_TYPES);
    ProtoFile expected =
        new ProtoFile("descriptor.proto", "google.protobuf", NO_STRINGS, Arrays.asList(message),
            map("java_package", "com.google.protobuf"));
    assertEquals(expected, new ProtoSchemaParser("descriptor.proto", proto).readProtoFile());
  }

  public void testNestingInMessage() throws Exception {
    String proto = ""
        + "message FieldOptions {\n"
        + "  optional CType ctype = 1 [default = STRING, deprecated=true];\n"
        + "  enum CType {\n"
        + "    STRING = 0;\n"
        + "  };\n"
        + "  // Clients can define custom options in extensions of this message. See above.\n"
        + "  extensions 500;\n"
        + "  extensions 1000 to max;\n"
        + "}\n";
    Type enumType = new EnumType("CType", "", Arrays.asList(new Value("STRING", 0, "")));
    Type messageType = new MessageType("FieldOptions", "", Arrays.asList(
        new MessageType.Field(Label.OPTIONAL, "CType", "ctype", 1, "",
            map("default", "STRING", "deprecated", "true"))), Arrays.asList(enumType));
    ProtoFile expected =
        new ProtoFile("descriptor.proto", null, NO_STRINGS, Arrays.asList(messageType), map());
    ProtoFile actual = new ProtoSchemaParser("descriptor.proto", proto).readProtoFile();
    assertEquals(expected, actual);
  }

  public void testImports() throws Exception {
    String proto = "import \"src/test/resources/unittest_import.proto\";\n";
    ProtoFile expected = new ProtoFile("descriptor.proto", null,
        Arrays.asList("src/test/resources/unittest_import.proto"), NO_TYPES, map());
    assertEquals(expected, new ProtoSchemaParser("descriptor.proto", proto).readProtoFile());
  }

  public void testExtend() throws Exception {
    String proto = ""
        + "extend Foo {\n"
        + "  optional int32 bar = 126;\n"
        + "}";
    ProtoFile expected = new ProtoFile("descriptor.proto", null, NO_STRINGS, NO_TYPES, map());
    assertEquals(expected, new ProtoSchemaParser("descriptor.proto", proto).readProtoFile());
  }

  public void testDefaultFieldWithParen() throws Exception {
    String proto = ""
        + "message Foo {\n"
        + "  optional string claim_token = 2 [(squareup.redacted) = true];\n"
        + "}";
    Type messageType = new MessageType("Foo", "", Arrays.asList(
        new MessageType.Field(Label.OPTIONAL, "string", "claim_token", 2, "",
            map("squareup.redacted", "true"))), NO_TYPES);
    ProtoFile expected =
        new ProtoFile("descriptor.proto", null, NO_STRINGS, Arrays.<Type>asList(messageType),
            map());
    assertEquals(expected, new ProtoSchemaParser("descriptor.proto", proto).readProtoFile());
  }

  public void testService() throws Exception {
    String proto = ""
        + "service SearchService {\n"
        + "  rpc Search (SearchRequest) returns (SearchResponse);"
        + "  rpc Purchase (PurchaseRequest) returns (PurchaseResponse) {\n"
        + "    option (squareup.sake.timeout) = 15; \n"
        + "  }\n"
        + "}";
    ProtoFile expected = new ProtoFile("descriptor.proto", null, NO_STRINGS, NO_TYPES, map());
    assertEquals(expected, new ProtoSchemaParser("descriptor.proto", proto).readProtoFile());
  }

  public void testHexTag() throws Exception {
    String proto = ""
        + "message HexTag {\n"
        + "  required string hex = 0x10;\n"
        + "}";
    Type expected = new MessageType("HexTag", "",
        Arrays.asList(new MessageType.Field(Label.REQUIRED, "string", "hex", 16, "", map())),
        NO_TYPES);
    ProtoFile protoFile =
        new ProtoFile("hex.proto", null, NO_STRINGS, Arrays.asList(expected), map());
    assertEquals(protoFile, new ProtoSchemaParser("hex.proto", proto).readProtoFile());
  }

  public void testStructuredOption() throws Exception {
    String proto = ""
        + "message ExoticOptions {\n"
        + "  option (squareup.one) = {name: \"Name\", class_name:\"ClassName\"};\n"
        + "  option (squareup.two.a) = {[squareup.options.type]: EXOTIC};\n"
        + "}";
    Type expected =
        new MessageType("ExoticOptions", "", Arrays.<MessageType.Field>asList(), NO_TYPES);
    ProtoFile protoFile =
        new ProtoFile("exotic.proto", null, NO_STRINGS, Arrays.asList(expected), map());
    assertEquals(protoFile, new ProtoSchemaParser("exotic.proto", proto).readProtoFile());
  }

  public void testOptionsWithNestedMapsAndTrailingCommas() throws Exception {
    String proto = ""
        + "message StructuredOption {\n"
        + "    optional field.type has_options = 3 [\n"
        + "            (option_map) = {\n"
        + "                nested_map: {key:\"value\", key2:\"value2\"},\n" // Note trailing ','.
        + "            },\n"
        + "            (option_string) = \"string\"\n"
        + "    ];\n"
        + "}";
    Type expected = new MessageType("StructuredOption", "", Arrays.<MessageType.Field>asList(
        new MessageType.Field(Label.OPTIONAL, "field.type", "has_options", 3, "",
            map("option_map", map("nested_map", map("key", "value", "key2", "value2")),
                "option_string", "string"))), NO_TYPES);
    ProtoFile protoFile =
        new ProtoFile("nestedmaps.proto", null, NO_STRINGS, Arrays.asList(expected), map());
    assertEquals(protoFile, new ProtoSchemaParser("nestedmaps.proto", proto).readProtoFile());
  }

  private Map<String, Object> map(Object... keysAndValues) {
    Map<String, Object> result = new LinkedHashMap<String, Object>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      result.put((String) keysAndValues[i], keysAndValues[i + 1]);
    }
    return result;
  }
}
