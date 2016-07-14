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
package com.squareup.wire.java;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaBuilder;
import java.util.Collections;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class JavaGeneratorTest {
  @Test public void sanitizeJavadocStripsTrailingWhitespace() {
    String input = "The quick brown fox  \nJumps over  \n\t \t\nThe lazy dog  ";
    String expected = "The quick brown fox\nJumps over\n\nThe lazy dog";
    assertThat(JavaGenerator.sanitizeJavadoc(input)).isEqualTo(expected);
  }

  @Test public void sanitizeJavadocWrapsSeeLinks() {
    String input = "Google query.\n\n@see http://google.com";
    String expected = "Google query.\n\n@see <a href=\"http://google.com\">http://google.com</a>";
    assertThat(JavaGenerator.sanitizeJavadoc(input)).isEqualTo(expected);
  }

  @Test public void sanitizeJavadocStarSlash() {
    String input = "/* comment inside comment. */";
    String expected = "/* comment inside comment. &#42;/";
    assertThat(JavaGenerator.sanitizeJavadoc(input)).isEqualTo(expected);
  }

  @Test public void generateTypeUsesNameAllocatorInMessageBuilderBuild() {
    Schema schema = new SchemaBuilder()
        .add("message.proto", ""
            + "message Message {\n"
            + "  required float long = 1;\n"
            + "}\n")
        .build();
    MessageType message = (MessageType) schema.getType("Message");
    JavaGenerator javaGenerator = JavaGenerator.get(schema);
    TypeSpec typeSpec = javaGenerator.generateType(message);
    assertThat(JavaFile.builder("com.squareup.message", typeSpec).build().toString()).contains(""
        + "    @Override\n"
        + "    public Message build() {\n"
        + "      if (long_ == null) {\n"
        + "        throw Internal.missingRequiredFields(long_, \"long\");\n"
        + "      }\n"
        + "      return new Message(long_, super.buildUnknownFields());\n"
        + "    }\n");
  }

  @Test public void generateProtoFields() {
    Schema schema = new SchemaBuilder()
        .add("message.proto", ""
            + "package original.proto;\n"
            + "option java_package = \"original.java\";\n"
            + "import \"foo.proto\";\n"
            + "message ProtoMessage {\n"
            + "  optional foo.proto.Foo field = 1;\n"
            + "  map<string, foo.proto.Bar> bars = 2;\n"
            + "  repeated int32 numbers = 3;\n"
            + "}\n")
        .add("foo.proto", ""
            + "package foo.proto;\n"
            + "option java_package = \"foo.java\";\n"
            + "message Foo {\n"
            + "}\n"
            + "message Bar {\n"
            + "}\n")
        .build();

    ProtoType protoType = ProtoType.get("original.proto", "ProtoMessage");
    ClassName className = ClassName.get("target.java", "JavaMessage");

    MessageType message = (MessageType) schema.getType(protoType);

    JavaGenerator javaGenerator = JavaGenerator.get(schema)
        .withCustomProtoAdapter(
            Collections.singletonMap(protoType, className),
            Collections.singletonMap(protoType, new AdapterConstant(className, "ADAPTER")));
    TypeSpec typeSpec = javaGenerator.generateProtoFields(message);
    ClassName typeName = javaGenerator.protoFieldsTypeName(protoType);

    assertThat(JavaFile.builder(typeName.packageName(), typeSpec).build().toString()).isEqualTo(""
        + "package original.java;\n"
        + "\n"
        + "import com.squareup.wire.ProtoAdapter;\n"
        + "import com.squareup.wire.ProtoField;\n"
        + "import foo.java.Bar;\n"
        + "import foo.java.Foo;\n"
        + "import java.lang.Integer;\n"
        + "import java.lang.String;\n"
        + "import java.util.List;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public final class ProtoMessageProtoFields {\n"
        + "  public static final ProtoField<Foo> field = new ProtoField(1, Foo.ADAPTER, null);\n"
        + "\n"
        + "  public static final ProtoField<Map<String, Bar>> bars = new ProtoField(2, ProtoAdapter.newMapAdapter(ProtoAdapter.STRING, Bar.ADAPTER), null);\n"
        + "\n"
        + "  public static final ProtoField<List<Integer>> numbers = new ProtoField(3, ProtoAdapter.INT32.asRepeated(), null);\n"
        + "}\n");
  }
}
