/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.wire.schema;

import com.squareup.wire.schema.internal.Util;
import org.junit.Test;

import static com.squareup.wire.schema.Options.FIELD_OPTIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class SchemaTest {
  @Test public void linkService() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "import \"request.proto\";\n"
            + "import \"response.proto\";\n"
            + "service Service {\n"
            + "  rpc Call (Request) returns (Response);\n"
            + "}\n")
        .add("request.proto", ""
            + "message Request {\n"
            + "}\n")
        .add("response.proto", ""
            + "message Response {\n"
            + "}\n")
        .build();

    Service service = schema.getService("Service");
    Rpc call = service.rpc("Call");
    assertThat(call.requestType()).isEqualTo(schema.getType("Request").type());
    assertThat(call.responseType()).isEqualTo(schema.getType("Response").type());
  }

  @Test public void linkMessage() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("message.proto", ""
            + "import \"foo.proto\";\n"
            + "message Message {\n"
            + "  optional foo_package.Foo field = 1;\n"
            + "  map<string, foo_package.Bar> bars = 2;\n"
            + "}\n")
        .add("foo.proto", ""
            + "package foo_package;\n"
            + "message Foo {\n"
            + "}\n"
            + "message Bar {\n"
            + "}\n")
        .build();

    MessageType message = (MessageType) schema.getType("Message");
    Field field = message.field("field");
    assertThat(field.type()).isEqualTo(schema.getType("foo_package.Foo").type());
    ProtoType bars = message.field("bars").type();
    assertThat(bars.keyType()).isEqualTo(ProtoType.STRING);
    assertThat(bars.valueType()).isEqualTo(schema.getType("foo_package.Bar").type());
  }

  @Test public void isValidTag() {
    assertThat(Util.isValidTag(0)).isFalse(); // Less than minimum.
    assertThat(Util.isValidTag(1)).isTrue();
    assertThat(Util.isValidTag(1234)).isTrue();
    assertThat(Util.isValidTag(19222)).isFalse(); // Reserved range.
    assertThat(Util.isValidTag(2319573)).isTrue();
    assertThat(Util.isValidTag(536870911)).isTrue();
    assertThat(Util.isValidTag(536870912)).isFalse(); // Greater than maximum.
  }

  @Test public void fieldInvalidTag() throws Exception {
    try {
      new SchemaBuilder()
          .add("message.proto", ""
              + "message Message {\n"
              + "  optional int32 a = 0;\n"
              + "  optional int32 b = 1;\n"
              + "  optional int32 c = 18999;\n"
              + "  optional int32 d = 19000;\n"
              + "  optional int32 e = 19999;\n"
              + "  optional int32 f = 20000;\n"
              + "  optional int32 g = 536870911;\n"
              + "  optional int32 h = 536870912;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected.getMessage()).isEqualTo(""
          + "tag is out of range: 0\n"
          + "  for field a (message.proto at 2:3)\n"
          + "  in message Message (message.proto at 1:1)\n"
          + "tag is out of range: 19000\n"
          + "  for field d (message.proto at 5:3)\n"
          + "  in message Message (message.proto at 1:1)\n"
          + "tag is out of range: 19999\n"
          + "  for field e (message.proto at 6:3)\n"
          + "  in message Message (message.proto at 1:1)\n"
          + "tag is out of range: 536870912\n"
          + "  for field h (message.proto at 9:3)\n"
          + "  in message Message (message.proto at 1:1)");
    }
  }

  @Test public void extensionsInvalidTag() throws Exception {
    try {
      new SchemaBuilder()
          .add("message.proto", ""
              + "message Message {\n"
              + "  extensions 0;\n"
              + "  extensions 1;\n"
              + "  extensions 18999;\n"
              + "  extensions 19000;\n"
              + "  extensions 19999;\n"
              + "  extensions 20000;\n"
              + "  extensions 536870911;\n"
              + "  extensions 536870912;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage(""
          + "tags are out of range: 0 to 0\n"
          + "  for extensions (message.proto at 2:3)\n"
          + "  in message Message (message.proto at 1:1)\n"
          + "tags are out of range: 19000 to 19000\n"
          + "  for extensions (message.proto at 5:3)\n"
          + "  in message Message (message.proto at 1:1)\n"
          + "tags are out of range: 19999 to 19999\n"
          + "  for extensions (message.proto at 6:3)\n"
          + "  in message Message (message.proto at 1:1)\n"
          + "tags are out of range: 536870912 to 536870912\n"
          + "  for extensions (message.proto at 9:3)\n"
          + "  in message Message (message.proto at 1:1)");
    }
  }

  @Test public void scalarFieldIsPacked() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("message.proto", ""
            + "message Message {\n"
            + "  repeated int32 a = 1;\n"
            + "  repeated int32 b = 2 [packed=false];\n"
            + "  repeated int32 c = 3 [packed=true];\n"
            + "}\n")
        .build();

    MessageType message = (MessageType) schema.getType("Message");
    assertThat(message.field("a").isPacked()).isFalse();
    assertThat(message.field("b").isPacked()).isFalse();
    assertThat(message.field("c").isPacked()).isTrue();
  }

  @Test public void enumFieldIsPacked() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("message.proto", ""
            + "message Message {\n"
            + "  repeated HabitablePlanet home_planet = 1 [packed=true];\n"
            + "  enum HabitablePlanet {\n"
            + "    EARTH = 1;\n"
            + "  }\n"
            + "}\n")
        .build();
    MessageType message = (MessageType) schema.getType("Message");
    assertThat(message.field("home_planet").isPacked()).isTrue();
  }

  @Test public void fieldIsPackedButShouldntBe() throws Exception {
    try {
      new SchemaBuilder()
          .add("message.proto", ""
              + "message Message {\n"
              + "  repeated bytes a = 1 [packed=false];\n"
              + "  repeated bytes b = 2 [packed=true];\n"
              + "  repeated string c = 3 [packed=false];\n"
              + "  repeated string d = 4 [packed=true];\n"
              + "  repeated Message e = 5 [packed=false];\n"
              + "  repeated Message f = 6 [packed=true];\n"
              + "}\n"
              + "extend Message {\n"
              + "  repeated bytes g = 7 [packed=false];\n"
              + "  repeated bytes h = 8 [packed=true];\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage(""
          + "packed=true not permitted on bytes\n"
          + "  for field b (message.proto at 3:3)\n"
          + "  in message Message (message.proto at 1:1)\n"
          + "packed=true not permitted on string\n"
          + "  for field d (message.proto at 5:3)\n"
          + "  in message Message (message.proto at 1:1)\n"
          + "packed=true not permitted on Message\n"
          + "  for field f (message.proto at 7:3)\n"
          + "  in message Message (message.proto at 1:1)\n"
          + "packed=true not permitted on bytes\n"
          + "  for field h (message.proto at 11:3)\n"
          + "  in message Message (message.proto at 1:1)");
    }
  }

  @Test public void fieldIsDeprecated() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("message.proto", ""
            + "message Message {\n"
            + "  optional int32 a = 1;\n"
            + "  optional int32 b = 2 [deprecated=false];\n"
            + "  optional int32 c = 3 [deprecated=true];\n"
            + "}\n")
        .build();

    MessageType message = (MessageType) schema.getType("Message");
    assertThat(message.field("a").isDeprecated()).isFalse();
    assertThat(message.field("b").isDeprecated()).isFalse();
    assertThat(message.field("c").isDeprecated()).isTrue();
  }

  @Test public void fieldDefault() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("message.proto", ""
            + "message Message {\n"
            + "  optional int32 a = 1;\n"
            + "  optional int32 b = 2 [default = 5];\n"
            + "  optional bool c = 3 [default = true];\n"
            + "  optional string d = 4 [default = \"foo\"];\n"
            + "  optional Roshambo e = 5 [default = PAPER];\n"
            + "  enum Roshambo {\n"
            + "    ROCK = 0;\n"
            + "    SCISSORS = 1;\n"
            + "    PAPER = 2;\n"
            + "  }\n"
            + "}\n")
        .build();

    MessageType message = (MessageType) schema.getType("Message");
    assertThat(message.field("a").getDefault()).isNull();
    assertThat(message.field("b").getDefault()).isEqualTo("5");
    assertThat(message.field("c").getDefault()).isEqualTo("true");
    assertThat(message.field("d").getDefault()).isEqualTo("foo");
    assertThat(message.field("e").getDefault()).isEqualTo("PAPER");
  }

  @Test public void fieldOptions() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("message.proto", ""
            + "import \"google/protobuf/descriptor.proto\";\n"
            + "message Message {\n"
            + "  optional int32 a = 1;\n"
            + "  optional int32 b = 2 [color=red, deprecated=true, packed=true];\n"
            + "}\n"
            + "extend google.protobuf.FieldOptions {\n"
            + "  optional string color = 60001;\n"
            + "}\n")
        .build();
    MessageType message = (MessageType) schema.getType("Message");

    Options aOptions = message.field("a").options();
    assertThat(aOptions.get(ProtoMember.get(FIELD_OPTIONS, "color"))).isNull();
    assertThat(aOptions.get(ProtoMember.get(FIELD_OPTIONS, "deprecated"))).isNull();
    assertThat(aOptions.get(ProtoMember.get(FIELD_OPTIONS, "packed"))).isNull();

    Options bOptions = message.field("b").options();
    assertThat(bOptions.get(ProtoMember.get(FIELD_OPTIONS, "color"))).isEqualTo("red");
    assertThat(bOptions.get(ProtoMember.get(FIELD_OPTIONS, "deprecated"))).isEqualTo("true");
    assertThat(bOptions.get(ProtoMember.get(FIELD_OPTIONS, "packed"))).isEqualTo("true");
  }

  @Test public void duplicateOption() throws Exception {
    try {
      new SchemaBuilder()
          .add("message.proto", ""
              + "import \"google/protobuf/descriptor.proto\";\n"
              + "message Message {\n"
              + "  optional int32 a = 1 [color=red, color=blue];\n"
              + "}\n"
              + "extend google.protobuf.FieldOptions {\n"
              + "  optional string color = 60001;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("conflicting options: red, blue\n"
          + "  for field a (message.proto at 3:3)\n"
          + "  in message Message (message.proto at 2:1)");
    }
  }

  @Test public void messageFieldTypeUnknown() throws Exception {
    try {
      new SchemaBuilder()
          .add("message.proto", ""
              + "message Message {\n"
              + "  optional foo_package.Foo unknown = 1;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("unable to resolve foo_package.Foo\n"
          + "  for field unknown (message.proto at 2:3)\n"
          + "  in message Message (message.proto at 1:1)");
    }
  }

  @Test public void oneofFieldTypeUnknown() throws Exception {
    try {
      new SchemaBuilder()
          .add("message.proto", ""
              + "message Message {\n"
              + "  oneof selection {\n"
              + "    int32 known = 1;\n"
              + "    foo_package.Foo unknown = 2;\n"
              + "  }\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("unable to resolve foo_package.Foo\n"
          + "  for field unknown (message.proto at 4:5)\n"
          + "  in message Message (message.proto at 1:1)");
    }
  }

  @Test public void serviceTypesMustBeNamed() throws Exception {
    try {
      new SchemaBuilder()
          .add("service.proto", ""
              + "service Service {\n"
              + "  rpc Call (string) returns (Response);\n"
              + "}\n"
              + "message Response {\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("expected a message but was string\n"
          + "  for rpc Call (service.proto at 2:3)\n"
          + "  in service Service (service.proto at 1:1)");
    }
    try {
      new SchemaBuilder()
          .add("service.proto", ""
              + "service Service {\n"
              + "  rpc Call (Request) returns (string);\n"
              + "}\n"
              + "message Request {\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("expected a message but was string\n"
          + "  for rpc Call (service.proto at 2:3)\n"
          + "  in service Service (service.proto at 1:1)");
    }
  }

  @Test public void serviceTypesUnknown() throws Exception {
    try {
      new SchemaBuilder()
          .add("service.proto", ""
              + "service Service {\n"
              + "  rpc Call (foo_package.Foo) returns (Response);\n"
              + "}\n"
              + "message Response {\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("unable to resolve foo_package.Foo\n"
          + "  for rpc Call (service.proto at 2:3)\n"
          + "  in service Service (service.proto at 1:1)");
    }
    try {
      new SchemaBuilder()
          .add("service.proto", ""
              + "service Service {\n"
              + "  rpc Call (Request) returns (foo_package.Foo);\n"
              + "}\n"
              + "message Request {\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("unable to resolve foo_package.Foo\n"
          + "  for rpc Call (service.proto at 2:3)\n"
          + "  in service Service (service.proto at 1:1)");
    }
  }

  @Test public void extendedTypeUnknown() throws Exception {
    try {
      new SchemaBuilder()
          .add("extend.proto", ""
              + "extend foo_package.Foo {\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("unable to resolve foo_package.Foo\n"
          + "  for extend (extend.proto at 1:1)");
    }
  }

  @Test public void extendedTypeMustBeNamed() throws Exception {
    try {
      new SchemaBuilder()
          .add("extend.proto", ""
              + "extend string {\n"
              + "  optional Value value = 1000;\n"
              + "}\n"
              + "message Value {\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("expected a message but was string\n"
          + "  for extend (extend.proto at 1:1)");
    }
  }

  @Test public void extendFieldTypeUnknown() throws Exception {
    try {
      new SchemaBuilder()
          .add("message.proto", ""
              + "message Message {\n"
              + "}\n"
              + "extend Message {\n"
              + "  optional foo_package.Foo unknown = 1;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("unable to resolve foo_package.Foo\n"
          + "  for field unknown (message.proto at 4:3)\n"
          + "  in message Message (message.proto at 1:1)");
    }
  }

  @Test public void multipleErrors() throws Exception {
    try {
      new SchemaBuilder()
          .add("message.proto", ""
              + "message Message {\n"
              + "  optional foo_package.Foo unknown = 1;\n"
              + "  optional foo_package.Foo also_unknown = 2;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage(""
          + "unable to resolve foo_package.Foo\n"
          + "  for field unknown (message.proto at 2:3)\n"
          + "  in message Message (message.proto at 1:1)\n"
          + "unable to resolve foo_package.Foo\n"
          + "  for field also_unknown (message.proto at 3:3)\n"
          + "  in message Message (message.proto at 1:1)");
    }
  }

  @Test public void duplicateMessageTagDisallowed() throws Exception {
    try {
      new SchemaBuilder()
          .add("message.proto", ""
              + "message Message {\n"
              + "  required string name1 = 1;\n"
              + "  required string name2 = 1;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("multiple fields share tag 1:\n"
          + "  1. name1 (message.proto at 2:3)\n"
          + "  2. name2 (message.proto at 3:3)\n"
          + "  for message Message (message.proto at 1:1)");
    }
  }

  @Test public void duplicateTagValueDisallowedInOneOf() throws Exception {
    try {
      new SchemaBuilder()
          .add("message.proto", ""
              + "message Message {\n"
              + "  required string name1 = 1;\n"
              + "  oneof selection {\n"
              + "    string name2 = 1;\n"
              + "  }\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("multiple fields share tag 1:\n"
          + "  1. name1 (message.proto at 2:3)\n"
          + "  2. name2 (message.proto at 4:5)\n"
          + "  for message Message (message.proto at 1:1)");
    }
  }

  @Test public void duplicateExtendTagDisallowed() throws Exception {
    try {
      new SchemaBuilder()
          .add("message.proto", ""
              + "message Message {\n"
              + "}\n"
              + "extend Message {\n"
              + "  optional string name1 = 1;\n"
              + "  optional string name2 = 1;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("multiple fields share tag 1:\n"
          + "  1. name1 (message.proto at 4:3)\n"
          + "  2. name2 (message.proto at 5:3)\n"
          + "  for message Message (message.proto at 1:1)");
    }
  }

  @Test public void messageNameCollisionDisallowed() throws Exception {
    try {
      new SchemaBuilder()
          .add("message.proto", ""
              + "message Message {\n"
              + "  optional string a = 1;\n"
              + "  optional string a = 2;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("multiple fields share name a:\n"
          + "  1. a (message.proto at 2:3)\n"
          + "  2. a (message.proto at 3:3)\n"
          + "  for message Message (message.proto at 1:1)");
    }
  }

  @Test public void messsageAndExtensionNameCollision() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("message.proto", ""
            + "message Message {\n"
            + "  optional string a = 1;\n"
            + "}\n")
        .add("extend.proto", ""
            + "package p;\n"
            + "import \"message.proto\";\n"
            + "extend Message {\n"
            + "  optional string a = 2;\n"
            + "}\n")
        .build();
    MessageType messageType = (MessageType) schema.getType("Message");

    assertThat(messageType.field("a").tag()).isEqualTo(1);
    assertThat(messageType.extensionField("p.a").tag()).isEqualTo(2);
  }

  @Test public void extendNameCollisionInSamePackageDisallowed() throws Exception {
    try {
      new SchemaBuilder()
          .add("message.proto", ""
              + "message Message {\n"
              + "}\n")
          .add("extend1.proto", ""
              + "import \"message.proto\";\n"
              + "extend Message {\n"
              + "  optional string a = 1;\n"
              + "}\n")
          .add("extend2.proto", ""
              + "import \"message.proto\";\n"
              + "extend Message {\n"
              + "  optional string a = 2;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("multiple fields share name a:\n"
          + "  1. a (extend1.proto at 3:3)\n"
          + "  2. a (extend2.proto at 3:3)\n"
          + "  for message Message (message.proto at 1:1)");
    }
  }

  @Test public void extendNameCollisionInDifferentPackagesAllowed() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("message.proto", ""
            + "message Message {\n"
            + "}\n")
        .add("extend1.proto", ""
            + "package p1;\n"
            + "import \"message.proto\";\n"
            + "extend Message {\n"
            + "  optional string a = 1;\n"
            + "}\n")
        .add("extend2.proto", ""
            + "package p2;\n"
            + "import \"message.proto\";\n"
            + "extend Message {\n"
            + "  optional string a = 2;\n"
            + "}\n")
        .build();
    MessageType messageType = (MessageType) schema.getType("Message");

    assertThat(messageType.field("a")).isNull();
    assertThat(messageType.extensionField("p1.a").packageName()).isEqualTo("p1");
    assertThat(messageType.extensionField("p2.a").packageName()).isEqualTo("p2");
  }

  @Test public void extendEnumDisallowed() throws Exception {
    try {
      new SchemaBuilder()
          .add("enum.proto", ""
              + "enum Enum {\n"
              + "  A = 1;\n"
              + "  B = 2;\n"
              + "}\n")
          .add("extend.proto", ""
              + "import \"enum.proto\";\n"
              + "extend Enum {\n"
              + "  optional string a = 2;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("expected a message but was Enum\n"
          + "  for extend (extend.proto at 2:1)");
    }
  }

  @Test public void requiredExtendFieldDisallowed() throws Exception {
    try {
      new SchemaBuilder()
          .add("message.proto", ""
              + "message Message {\n"
              + "}\n"
              + "extend Message {\n"
              + "  required string a = 1;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("extension fields cannot be required\n"
          + "  for field a (message.proto at 4:3)\n"
          + "  in message Message (message.proto at 1:1)");
    }
  }

  @Test public void oneofLabelDisallowed() throws Exception {
    try {
      new SchemaBuilder()
          .add("message.proto", ""
              + "message Message {\n"
              + "  oneof string s = 1;\n"
              + "}\n")
          .build();
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage("Syntax error in message.proto at 2:17: expected '{'");
    }
  }

  @Test public void duplicateEnumValueTagInScopeDisallowed() throws Exception {
    try {
      new SchemaBuilder()
          .add("message.proto", ""
              + "message Message {\n"
              + "  enum Enum1 {\n"
              + "    VALUE = 1;\n"
              + "  }\n"
              + "  enum Enum2 {\n"
              + "    VALUE = 2;\n"
              + "  }\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("multiple enums share constant VALUE:\n"
          + "  1. Message.Enum1.VALUE (message.proto at 3:5)\n"
          + "  2. Message.Enum2.VALUE (message.proto at 6:5)\n"
          + "  for message Message (message.proto at 1:1)");
    }
  }

  @Test public void duplicateEnumConstantTagWithoutAllowAliasDisallowed() throws Exception {
    try {
      new SchemaBuilder()
          .add("message.proto", ""
              + "enum Enum {\n"
              + "  A = 1;\n"
              + "  B = 1;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("multiple enum constants share tag 1:\n"
          + "  1. A (message.proto at 2:3)\n"
          + "  2. B (message.proto at 3:3)\n"
          + "  for enum Enum (message.proto at 1:1)");
    }
  }

  @Test public void duplicateEnumConstantTagWithAllowAliasFalseDisallowed() throws Exception {
    try {
      new SchemaBuilder()
          .add("message.proto", ""
              + "enum Enum {\n"
              + "  option allow_alias = false;\n"
              + "  A = 1;\n"
              + "  B = 1;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("multiple enum constants share tag 1:\n"
          + "  1. A (message.proto at 3:3)\n"
          + "  2. B (message.proto at 4:3)\n"
          + "  for enum Enum (message.proto at 1:1)");
    }
  }

  @Test public void duplicateEnumConstantTagWithAllowAliasTrueAllowed() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("message.proto", ""
            + "enum Enum {\n"
            + "  option allow_alias = true;\n"
            + "  A = 1;\n"
            + "  B = 1;\n"
            + "}\n")
        .build();
    EnumType enumType = (EnumType) schema.getType("Enum");
    assertThat(enumType.constant("A").tag()).isEqualTo(1);
    assertThat(enumType.constant("B").tag()).isEqualTo(1);
  }

  @Test public void fieldTypeImported() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("a.proto", ""
            + "package pa;\n"
            + "import \"b.proto\";\n"
            + "message A {\n"
            + "  optional pb.B b = 1;\n"
            + "}\n")
        .add("b.proto", ""
            + "package pb;\n"
            + "message B {\n"
            + "}\n")
        .build();
    MessageType a = (MessageType) schema.getType("pa.A");
    MessageType b = (MessageType) schema.getType("pb.B");
    assertThat(a.field("b").type()).isEqualTo(b.type());
  }

  @Test public void fieldMapTypeImported() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("a.proto", ""
            + "package pa;\n"
            + "import \"b.proto\";\n"
            + "message A {\n"
            + "  map<string, pb.B> b = 1;\n"
            + "}\n")
        .add("b.proto", ""
            + "package pb;\n"
            + "message B {\n"
            + "}\n")
        .build();
    MessageType a = (MessageType) schema.getType("pa.A");
    MessageType b = (MessageType) schema.getType("pb.B");
    assertThat(a.field("b").type().valueType()).isEqualTo(b.type());
  }

  @Test public void fieldTypeNotImported() throws Exception {
    try {
      new SchemaBuilder()
          .add("a.proto", ""
              + "package pa;\n"
              + "message A {\n"
              + "  optional pb.B b = 1;\n"
              + "}\n")
          .add("b.proto", ""
              + "package pb;\n"
              + "message B {\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected.getMessage()).isEqualTo("a.proto needs to import b.proto\n"
          + "  for field b (a.proto at 3:3)\n"
          + "  in message pa.A (a.proto at 2:1)");
    }
  }

  @Test public void fieldMapTypeNotImported() throws Exception {
    try {
      new SchemaBuilder()
          .add("a.proto", ""
              + "package pa;\n"
              + "message A {\n"
              + "  map<string, pb.B> b = 1;\n"
              + "}\n")
          .add("b.proto", ""
              + "package pb;\n"
              + "message B {\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected.getMessage()).isEqualTo("a.proto needs to import b.proto\n"
          + "  for field b (a.proto at 3:3)\n"
          + "  in message pa.A (a.proto at 2:1)");
    }
  }

  @Test public void rpcTypeImported() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("a.proto", ""
            + "package pa;\n"
            + "import \"b.proto\";\n"
            + "service Service {\n"
            + "  rpc Call (pb.B) returns (pb.B);\n"
            + "}\n")
        .add("b.proto", ""
            + "package pb;\n"
            + "message B {\n"
            + "}\n")
        .build();
    Service service = schema.getService("pa.Service");
    MessageType b = (MessageType) schema.getType("pb.B");
    assertThat(service.rpcs().get(0).requestType()).isEqualTo(b.type());
    assertThat(service.rpcs().get(0).responseType()).isEqualTo(b.type());
  }

  @Test public void rpcTypeNotImported() throws Exception {
    try {
      new SchemaBuilder()
          .add("a.proto", ""
              + "package pa;\n"
              + "service Service {\n"
              + "  rpc Call (pb.B) returns (pb.B);\n"
              + "}\n")
          .add("b.proto", ""
              + "package pb;\n"
              + "message B {\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected.getMessage()).isEqualTo(""
          + "a.proto needs to import b.proto\n"
          + "  for rpc Call (a.proto at 3:3)\n"
          + "  in service pa.Service (a.proto at 2:1)\n"
          + "a.proto needs to import b.proto\n"
          + "  for rpc Call (a.proto at 3:3)\n"
          + "  in service pa.Service (a.proto at 2:1)");
    }
  }

  @Test public void extendTypeImported() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("a.proto", ""
            + "package pa;\n"
            + "import \"b.proto\";\n"
            + "extend pb.B {\n"
            + "  optional string a = 1;\n"
            + "}\n")
        .add("b.proto", ""
            + "package pb;\n"
            + "message B {\n"
            + "  extensions 1;\n"
            + "}\n")
        .build();
    Extend extendB = schema.protoFiles().get(0).extendList().get(0);
    MessageType b = (MessageType) schema.getType("pb.B");
    assertThat(extendB.type()).isEqualTo(b.type());
  }

  @Test public void extendTypeNotImported() throws Exception {
    try {
      new SchemaBuilder()
          .add("a.proto", ""
              + "package pa;\n"
              + "extend pb.B {\n"
              + "  optional string a = 1;\n"
              + "}\n")
          .add("b.proto", ""
              + "package pb;\n"
              + "message B {\n"
              + "  extensions 1;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected.getMessage()).isEqualTo("a.proto needs to import b.proto\n"
          + "  for extend pb.B (a.proto at 2:1)");
    }
  }

  @Test public void transitiveImportNotFollowed() throws Exception {
    try {
      new SchemaBuilder()
          .add("a.proto", ""
              + "package pa;\n"
              + "import \"b.proto\";\n"
              + "message A {\n"
              + "  optional pc.C c = 1;\n"
              + "}\n")
          .add("b.proto", ""
              + "package pb;\n"
              + "import \"c.proto\";\n"
              + "message B {\n"
              + "}\n")
          .add("c.proto", ""
              + "package pc;\n"
              + "message C {\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected.getMessage()).isEqualTo("a.proto needs to import c.proto\n"
          + "  for field c (a.proto at 4:3)\n"
          + "  in message pa.A (a.proto at 3:1)");
    }
  }

  @Test public void transitivePublicImportFollowed() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("a.proto", ""
            + "package pa;\n"
            + "import \"b.proto\";\n"
            + "message A {\n"
            + "  optional pc.C c = 1;\n"
            + "}\n")
        .add("b.proto", ""
            + "package pb;\n"
            + "import public \"c.proto\";\n"
            + "message B {\n"
            + "}\n")
        .add("c.proto", ""
            + "package pc;\n"
            + "message C {\n"
            + "}\n")
        .build();
    MessageType a = (MessageType) schema.getType("pa.A");
    MessageType c = (MessageType) schema.getType("pc.C");
    assertThat(a.field("c").type()).isEqualTo(c.type());
  }

  @Test public void importSamePackageDifferentFile() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("a_b_1.proto", ""
            + "package a.b;\n"
            + "\n"
            + "import \"a_b_2.proto\";\n"
            + "\n"
            + "message MessageB {\n"
            + "  optional .a.b.MessageC c1 = 1;\n"
            + "  optional a.b.MessageC c2 = 2;\n"
            + "  optional b.MessageC c3 = 3;\n"
            + "  optional MessageC c4 = 4;\n"
            + "}\n")
        .add("a_b_2.proto", ""
            + "package a.b;\n"
            + "\n"
            + "message MessageC {\n"
            + "}\n")
        .build();
    MessageType messageC = (MessageType) schema.getType("a.b.MessageB");
    assertThat(messageC.field("c1").type()).isEqualTo(ProtoType.get("a.b.MessageC"));
    assertThat(messageC.field("c2").type()).isEqualTo(ProtoType.get("a.b.MessageC"));
    assertThat(messageC.field("c3").type()).isEqualTo(ProtoType.get("a.b.MessageC"));
    assertThat(messageC.field("c4").type()).isEqualTo(ProtoType.get("a.b.MessageC"));
  }

  @Test public void importResolvesEnclosingPackageSuffix() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("a_b.proto", ""
            + "package a.b;\n"
            + "\n"
            + "message MessageB {\n"
            + "}\n")
        .add("a_b_c.proto", ""
            + "package a.b.c;\n"
            + "\n"
            + "import \"a_b.proto\";\n"
            + "\n"
            + "message MessageC {\n"
            + "  optional b.MessageB message_b = 1;\n"
            + "}\n")
        .build();
    MessageType messageC = (MessageType) schema.getType("a.b.c.MessageC");
    assertThat(messageC.field("message_b").type()).isEqualTo(ProtoType.get("a.b.MessageB"));
  }

  @Test public void importResolvesNestedPackageSuffix() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("a_b.proto", ""
            + "package a.b;\n"
            + "\n"
            + "import \"a_b_c.proto\";\n"
            + "\n"
            + "message MessageB {\n"
            + "  optional c.MessageC message_c = 1;\n"
            + "}\n")
        .add("a_b_c.proto", ""
            + "package a.b.c;\n"
            + "\n"
            + "message MessageC {\n"
            + "}\n")
        .build();
    MessageType messageC = (MessageType) schema.getType("a.b.MessageB");
    assertThat(messageC.field("message_c").type()).isEqualTo(ProtoType.get("a.b.c.MessageC"));
  }

  @Test public void nestedPackagePreferredOverEnclosingPackage() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("a.proto", ""
            + "package a;\n"
            + "\n"
            + "message MessageA {\n"
            + "}\n")
        .add("a_b.proto", ""
            + "package a.b;\n"
            + "\n"
            + "import \"a.proto\";\n"
            + "import \"a_b_a.proto\";\n"
            + "\n"
            + "message MessageB {\n"
            + "  optional a.MessageA message_a = 1;\n"
            + "}\n")
        .add("a_b_a.proto", ""
            + "package a.b.a;\n"
            + "\n"
            + "message MessageA {\n"
            + "}\n")
        .build();
    MessageType messageC = (MessageType) schema.getType("a.b.MessageB");
    assertThat(messageC.field("message_a").type()).isEqualTo(ProtoType.get("a.b.a.MessageA"));
  }

  @Test public void dotPrefixRefersToRootPackage() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("a.proto", ""
            + "package a;\n"
            + "\n"
            + "message MessageA {\n"
            + "}\n")
        .add("a_b.proto", ""
            + "package a.b;\n"
            + "\n"
            + "import \"a.proto\";\n"
            + "import \"a_b_a.proto\";\n"
            + "\n"
            + "message MessageB {\n"
            + "  optional .a.MessageA message_a = 1;\n"
            + "}\n")
        .add("a_b_a.proto", ""
            + "package a.b.a;\n"
            + "\n"
            + "message MessageA {\n"
            + "}\n")
        .build();
    MessageType messageC = (MessageType) schema.getType("a.b.MessageB");
    assertThat(messageC.field("message_a").type()).isEqualTo(ProtoType.get("a.MessageA"));
  }

  @Test public void dotPrefixMustBeRoot() throws Exception {
    try {
      new SchemaBuilder()
          .add("a_b.proto", ""
              + "package a.b;\n"
              + "\n"
              + "message MessageB {\n"
              + "}\n")
          .add("a_b_c.proto", ""
              + "package a.b.c;\n"
              + "\n"
              + "import \"a_b.proto\";\n"
              + "\n"
              + "message MessageC {\n"
              + "  optional .b.MessageB message_b = 1;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("unable to resolve .b.MessageB\n"
          + "  for field message_b (a_b_c.proto at 6:3)\n"
          + "  in message a.b.c.MessageC (a_b_c.proto at 5:1)");
    }
  }

  @Test public void groupsThrow() throws Exception {
    try {
      new SchemaBuilder()
          .add("test.proto", ""
              + "message SearchResponse {\n"
              + "  repeated group Result = 1 {\n"
              + "    required string url = 2;\n"
              + "    optional string title = 3;\n"
              + "    repeated string snippets = 4;\n"
              + "  }\n"
              + "}\n")
          .build();
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage("'group' is not supported");
    }
  }

  @Test public void oneOfGroupsThrow() throws Exception {
    try {
      new SchemaBuilder()
          .add("test.proto", ""
              + "message Message {\n"
              + "  oneof hi {\n"
              + "    string name = 1;\n"
              + "  \n"
              + "    group Stuff = 3 {\n"
              + "      optional int32 result_per_page = 4;\n"
              + "      optional int32 page_count = 5;\n"
              + "    }\n"
              + "  }\n"
              + "}\n")
          .build();
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage("'group' is not supported");
    }
  }

  @Test public void reservedTagThrowsWhenUsed() throws Exception {
    try {
      new SchemaBuilder()
          .add("test.proto", ""
              + "message Message {\n"
              + "  reserved 1;\n"
              + "  optional string name = 1;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("tag 1 is reserved (test.proto at 2:3)\n"
          + "  for field name (test.proto at 3:3)\n"
          + "  in message Message (test.proto at 1:1)");
    }
  }

  @Test public void reservedTagRangeThrowsWhenUsed() throws Exception {
    try {
      new SchemaBuilder()
          .add("test.proto", ""
              + "message Message {\n"
              + "  reserved 1 to 3;\n"
              + "  optional string name = 2;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("tag 2 is reserved (test.proto at 2:3)\n"
          + "  for field name (test.proto at 3:3)\n"
          + "  in message Message (test.proto at 1:1)");
    }
  }

  @Test public void reservedNameThrowsWhenUsed() throws Exception {
    try {
      new SchemaBuilder()
          .add("test.proto", ""
              + "message Message {\n"
              + "  reserved 'foo';\n"
              + "  optional string foo = 1;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("name 'foo' is reserved (test.proto at 2:3)\n"
          + "  for field foo (test.proto at 3:3)\n"
          + "  in message Message (test.proto at 1:1)");
    }
  }

  @Test public void reservedTagAndNameBothReported() throws Exception {
    try {
      new SchemaBuilder()
          .add("test.proto", ""
              + "message Message {\n"
              + "  reserved 'foo';\n"
              + "  reserved 1;\n"
              + "  optional string foo = 1;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("name 'foo' is reserved (test.proto at 2:3)\n"
          + "  for field foo (test.proto at 4:3)\n"
          + "  in message Message (test.proto at 1:1)\n"
          + "tag 1 is reserved (test.proto at 3:3)\n"
          + "  for field foo (test.proto at 4:3)\n"
          + "  in message Message (test.proto at 1:1)");
    }
  }
}
