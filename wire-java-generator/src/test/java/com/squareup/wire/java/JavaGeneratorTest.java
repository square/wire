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

import com.google.common.collect.ImmutableMap;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
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

  @Test public void generateProtoFields() {
    Schema schema = new SchemaBuilder()
        .add("message.proto", ""
            + "import \"foo.proto\";\n"
            + "message Message {\n"
            + "  optional foo_package.Foo field = 1;\n"
            + "  map<string, foo_package.Bar> bars = 2;\n"
            + "  repeated int32 numbers = 3;\n"
            + "}\n")
        .add("foo.proto", ""
            + "package foo_package;\n"
            + "message Foo {\n"
            + "}\n"
            + "message Bar {\n"
            + "}\n")
        .build();

    MessageType message = (MessageType) schema.getType("Message");

    ProtoType protoType = ProtoType.get("Message");
    ClassName className = ClassName.bestGuess("Message");

    Map<ProtoType, TypeName> nameToJavaName = new LinkedHashMap<>();
    nameToJavaName.put(protoType, className);

    Map<ProtoType, Adapter> nameToAdapter = new LinkedHashMap<>();
    Adapter adapter = new Adapter(className, "ADAPTER");
    nameToAdapter.put(protoType, adapter);

    JavaGenerator javaGenerator = JavaGenerator.get(schema)
        .withCustomProtoAdapter(ImmutableMap.copyOf(nameToJavaName),
            ImmutableMap.copyOf(nameToAdapter));
    TypeSpec typeSpec = javaGenerator.generateProtoFields(message);

    assertThat(JavaFile.builder("com.squareup.message", typeSpec).build().toString()).isEqualTo(""
        + "package com.squareup.message;\n"
        + "\n"
        + "import com.squareup.wire.ProtoAdapter;\n"
        + "import com.squareup.wire.ProtoField;\n"
        + "import foo_package.Bar;\n"
        + "import foo_package.Foo;\n"
        + "import java.lang.Integer;\n"
        + "import java.lang.String;\n"
        + "import java.util.List;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "public final class MessageProtoFields {\n"
        + "  public static final ProtoField<Foo> field = new ProtoField(1, Foo.ADAPTER, null);\n"
        + "\n"
        + "  public static final ProtoField<Map<String, Bar>> bars = new ProtoField(2, ProtoAdapter.newMapAdapter(ProtoAdapter.STRING, Bar.ADAPTER), null);\n"
        + "\n"
        + "  public static final ProtoField<List<Integer>> numbers = new ProtoField(3, ProtoAdapter.INT32.asRepeated(), null);\n"
        + "}\n");
  }
}
