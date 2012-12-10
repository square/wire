/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.protoss.schema;

import com.squareup.protoss.schema.EnumType.Value;
import com.squareup.protoss.schema.MessageType.Field;
import com.squareup.protoss.schema.MessageType.Label;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import junit.framework.TestCase;

public final class ProtoSchemaParserTest extends TestCase {
  public void testField() throws Exception {
    Field field = new Field(Label.OPTIONAL, "CType", "ctype", 1, "",
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

    ProtoSchemaParser parser = new ProtoSchemaParser(proto);
    MessageType expected = new MessageType("SearchRequest", "", Arrays.asList(
        new Field(Label.REQUIRED, "string", "query", 1, "", map()),
        new Field(Label.OPTIONAL, "int32", "page_number", 2, "", map()),
        new Field(Label.OPTIONAL, "int32", "result_per_page", 3, "", map())));
    ProtoFile protoFile = new ProtoFile("search.proto", null, Collections.<String>emptyList(),
        Collections.singletonList(expected), Collections.<EnumType>emptyList());
    assertEquals(protoFile, parser.readProtoFile("search.proto"));
  }

  public void testParseEnum() throws Exception {
    String proto = ""
        + "/** What's on my waffles. */\n"
        + "enum Topping {\n"
        + "  FRUIT = 1;\n"
        + "  CREAM = 2;\n"
        + "\n"
        + "  // Quebec Maple syrup\n"
        + "  SYRUP = 3;\n"
        + "}\n";

    ProtoSchemaParser parser = new ProtoSchemaParser(proto);
    EnumType expected = new EnumType("Topping", "* What's on my waffles. ", Arrays.asList(
        new Value("FRUIT", 1, ""),
        new Value("CREAM", 2, ""),
        new Value("SYRUP", 3, " Quebec Maple syrup")
    ));
    ProtoFile protoFile = new ProtoFile("waffles.proto", null, Collections.<String>emptyList(),
        Collections.<MessageType>emptyList(), Collections.singletonList(expected));
    Object actual = parser.readProtoFile("waffles.proto");
    assertEquals(protoFile, actual);
  }

  public void testPackage() throws Exception {
    String proto = ""
        + "package google.protobuf;\n"
        + "option java_package = \"com.google.protobuf\";\n"
        + "option java_outer_classname = \"DescriptorProtos\";\n"
        + "\n"
        + "// descriptor.proto must be optimized for speed because reflection-based\n"
        + "// algorithms don't work during bootstrapping.\n"
        + "option optimize_for = SPEED;\n"
        + "\n"
        + "// The protocol compiler can output a FileDescriptorSet containing the .proto\n"
        + "// files it parses.\n"
        + "message FileDescriptorSet {\n"
        + "}\n";

    ProtoSchemaParser parser = new ProtoSchemaParser(proto);

    ProtoFile expected = new ProtoFile("descriptor.proto", "google.protobuf",
        Collections.<String>emptyList(),
        Collections.singletonList(new MessageType("FileDescriptorSet", ""
            + " The protocol compiler can output a FileDescriptorSet containing the .proto\n"
            + " files it parses.", Collections.<Field>emptyList())),
        Collections.<EnumType>emptyList());
    ProtoFile actual = parser.readProtoFile("descriptor.proto");
    assertEquals(expected, actual);
  }

  public void testNestingInMessage() throws Exception {
    String proto = ""
        + "message FieldOptions {\n"
        + "  optional CType ctype = 1 [default = STRING, deprecated=true];\n"
        + "  enum CType {\n"
        + "    STRING = 0;\n"
        + "  }\n"
        + "  // Clients can define custom options in extensions of this message. See above.\n"
        + "  extensions 1000 to max;\n"
        + "}\n";

    ProtoSchemaParser parser = new ProtoSchemaParser(proto);

    ProtoFile expected = new ProtoFile("descriptor.proto", null,
        Collections.<String>emptyList(),
        Collections.singletonList(new MessageType("FieldOptions", "", Arrays.asList(
            new Field(Label.OPTIONAL, "CType", "ctype", 1, "",
                map("default", "STRING", "deprecated", "true"))))),
        Collections.singletonList(
            new EnumType("CType", "", Arrays.asList(new Value("STRING", 0, "")))));
    ProtoFile actual = parser.readProtoFile("descriptor.proto");
    assertEquals(expected, actual);
  }

  public void testImports() throws Exception {
    String proto = ""
        + "import \"src/test/resources/unittest_import.proto\";\n";

    ProtoSchemaParser parser = new ProtoSchemaParser(proto);

    ProtoFile expected = new ProtoFile("descriptor.proto", null,
        Collections.singletonList("src/test/resources/unittest_import.proto"),
        Collections.<MessageType>emptyList(),
        Collections.<EnumType>emptyList());
    ProtoFile actual = parser.readProtoFile("descriptor.proto");
    assertEquals(expected, actual);
  }

  public void testExtend() throws Exception {
    String proto = ""
        + "extend Foo {\n"
        + "  optional int32 bar = 126;\n"
        + "}";

    ProtoSchemaParser parser = new ProtoSchemaParser(proto);

    ProtoFile expected = new ProtoFile("descriptor.proto", null,
        Collections.<String>emptyList(),
        Collections.<MessageType>emptyList(),
        Collections.<EnumType>emptyList());
    ProtoFile actual = parser.readProtoFile("descriptor.proto");
    assertEquals(expected, actual);
  }

  public void testService() throws Exception {
    String proto = ""
        + "service SearchService {\n"
        + "  rpc Search (SearchRequest) returns (SearchResponse);\n"
        + "}";

    ProtoSchemaParser parser = new ProtoSchemaParser(proto);

    ProtoFile expected = new ProtoFile("descriptor.proto", null,
        Collections.<String>emptyList(),
        Collections.<MessageType>emptyList(),
        Collections.<EnumType>emptyList());
    ProtoFile actual = parser.readProtoFile("descriptor.proto");
    assertEquals(expected, actual);
  }

  private Map<String, String> map(String... keysAndValues) {
    Map<String, String> result = new LinkedHashMap<String, String>();
    for (int i = 0; i < keysAndValues.length; i+=2) {
      result.put(keysAndValues[i], keysAndValues[i+1]);
    }
    return result;
  }
}
