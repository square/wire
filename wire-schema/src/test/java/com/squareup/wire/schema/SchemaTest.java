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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import okio.Buffer;
import okio.Source;
import org.junit.Test;

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
    assertThat(call.requestType()).isEqualTo(schema.getType("Request").name());
    assertThat(call.responseType()).isEqualTo(schema.getType("Response").name());
  }

  @Test public void linkMessage() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("message.proto", ""
            + "import \"foo.proto\";\n"
            + "message Message {\n"
            + "  optional foo_package.Foo field = 1;\n"
            + "}\n")
        .add("foo.proto", ""
            + "package foo_package;\n"
            + "message Foo {\n"
            + "}\n")
        .build();

    MessageType message = (MessageType) schema.getType("Message");
    Field field = message.field("field");
    assertThat(field.type()).isEqualTo(schema.getType("foo_package.Foo").name());
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
              + "  required foo_package.Foo unknown = 1;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("unable to resolve foo_package.Foo\n"
          + "  for field unknown (message.proto at 4:3)\n"
          + "  in extend Message (message.proto at 3:1)");
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
              + "  required string name1 = 1;\n"
              + "  required string name2 = 1;\n"
              + "}\n")
          .build();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage("multiple fields share tag 1:\n"
          + "  1. name1 (message.proto at 4:3)\n"
          + "  2. name2 (message.proto at 5:3)\n"
          + "  for extend Message (message.proto at 3:1)");
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
              + "  option (allow_alias) = false;\n"
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
            + "  option (allow_alias) = true;\n"
            + "  A = 1;\n"
            + "  B = 1;\n"
            + "}\n")
        .build();
    EnumType enumType = (EnumType) schema.getType("Enum");
    assertThat(enumType.constant("A").tag()).isEqualTo(1);
    assertThat(enumType.constant("B").tag()).isEqualTo(1);
  }

  static class SchemaBuilder {
    final Map<String, String> paths = new LinkedHashMap<>();

    public SchemaBuilder add(String name, String protoFile) {
      paths.put(name, protoFile);
      return this;
    }

    public Schema build() throws IOException {
      Loader.IO io = new Loader.IO() {
        @Override public Location locate(String path) throws IOException {
          return Location.get(path);
        }

        @Override public Source open(Location location) throws IOException {
          String protoFile = paths.get(location.path());
          return new Buffer().writeUtf8(protoFile);
        }
      };

      return new Loader(io).load(paths.keySet());
    }
  }
}
