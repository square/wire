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

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.RepoBuilder;
import com.squareup.wire.schema.Schema;
import java.io.IOException;
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

  @Test public void generateTypeUsesNameAllocatorInMessageBuilderBuild() throws Exception {
    RepoBuilder repoBuilder = new RepoBuilder()
        .add("message.proto", ""
            + "message Message {\n"
            + "  required float long = 1;\n"
            + "}\n");
    assertThat(repoBuilder.generateCode("Message")).contains(""
        + "    @Override\n"
        + "    public Message build() {\n"
        + "      if (long_ == null) {\n"
        + "        throw Internal.missingRequiredFields(long_, \"long\");\n"
        + "      }\n"
        + "      return new Message(long_, super.buildUnknownFields());\n"
        + "    }\n");
  }

  @Test public void map() throws Exception {
    Schema schema = new RepoBuilder()
        .add("message.proto", ""
            + "message Message {\n"
            + "  map<string, CdnResource> templates = 1;\n"
            + "  message CdnResource {\n"
            + "  }\n"
            + "}\n")
        .schema();
    MessageType message = (MessageType) schema.getType("Message");
    JavaGenerator javaGenerator = JavaGenerator.get(schema);
    TypeSpec typeSpec = javaGenerator.generateType(message);
    assertThat(JavaFile.builder("", typeSpec).build().toString()).contains(""
        + "  @WireField(\n"
        + "      tag = 1,\n"
        + "      keyAdapter = \"com.squareup.wire.ProtoAdapter#STRING\",\n"
        + "      adapter = \"Message$CdnResource#ADAPTER\"\n"
        + "  )\n"
        + "  public final Map<String, CdnResource> templates;\n");
  }

  @Test public void generateAbstractAdapter() throws Exception {
    RepoBuilder repoBuilder = new RepoBuilder()
        .add("message.proto", ""
            + "package original.proto;\n"
            + "option java_package = \"original.java\";\n"
            + "import \"foo.proto\";\n"
            + "message ProtoMessage {\n"
            + "  optional foo.proto.Foo field = 1;\n"
            + "  repeated int32 numbers = 3;\n"
            + "  optional foo.proto.CoinFlip coin_flip = 4;\n"
            + "  map<string, foo.proto.Bar> bars = 2;\n"
            + "}\n")
        .add("foo.proto", ""
            + "package foo.proto;\n"
            + "option java_package = \"foo.java\";\n"
            + "message Foo {\n"
            + "}\n"
            + "message Bar {\n"
            + "}\n"
            + "enum CoinFlip {\n"
            + "  HEADS = 1;\n"
            + "  TAILS = 2;\n"
            + "}\n")
        .add("android.wire", ""
            + "syntax = \"wire2\";\n"
            + "import \"message.proto\";\n"
            + "package original.proto;\n"
            + "type original.proto.ProtoMessage {\n"
            + "  target target.java.JavaMessage using target.java.JavaMessage#ADAPTER;\n"
            + "}\n");
    assertThat(repoBuilder.generateCode("original.proto.ProtoMessage", "android")).isEqualTo(""
        + "package original.java;\n"
        + "\n"
        + "import com.squareup.wire.FieldEncoding;\n"
        + "import com.squareup.wire.ProtoAdapter;\n"
        + "import com.squareup.wire.ProtoReader;\n"
        + "import com.squareup.wire.ProtoWriter;\n"
        + "import com.squareup.wire.internal.Internal;\n"
        + "import foo.java.Bar;\n"
        + "import foo.java.CoinFlip;\n"
        + "import foo.java.Foo;\n"
        + "import java.io.IOException;\n"
        + "import java.lang.Integer;\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "import java.util.List;\n"
        + "import java.util.Map;\n"
        + "import target.java.JavaMessage;\n"
        + "\n"
        + "public abstract class AbstractProtoMessageAdapter extends ProtoAdapter<JavaMessage> {\n"
        + "  private final ProtoAdapter<Map<String, Bar>> bars = ProtoAdapter.newMapAdapter(ProtoAdapter.STRING, Bar.ADAPTER);\n"
        + "\n"
        + "  public AbstractProtoMessageAdapter() {\n"
        + "    super(FieldEncoding.LENGTH_DELIMITED, JavaMessage.class);\n"
        + "  }\n"
        + "\n"
        + "  public abstract Foo field(JavaMessage value);\n"
        + "\n"
        + "  public abstract List<Integer> numbers(JavaMessage value);\n"
        + "\n"
        + "  public abstract CoinFlip coin_flip(JavaMessage value);\n"
        + "\n"
        + "  public abstract Map<String, Bar> bars(JavaMessage value);\n"
        + "\n"
        + "  public abstract JavaMessage fromProto(Foo field, List<Integer> numbers, CoinFlip coin_flip,\n"
        + "      Map<String, Bar> bars);\n"
        + "\n"
        + "  @Override\n"
        + "  public int encodedSize(JavaMessage value) {\n"
        + "    return Foo.ADAPTER.encodedSizeWithTag(1, field(value))\n"
        + "        + ProtoAdapter.INT32.asRepeated().encodedSizeWithTag(3, numbers(value))\n"
        + "        + CoinFlip.ADAPTER.encodedSizeWithTag(4, coin_flip(value))\n"
        + "        + bars.encodedSizeWithTag(2, bars(value));\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public void encode(ProtoWriter writer, JavaMessage value) throws IOException {\n"
        + "    Foo.ADAPTER.encodeWithTag(writer, 1, field(value));\n"
        + "    ProtoAdapter.INT32.asRepeated().encodeWithTag(writer, 3, numbers(value));\n"
        + "    CoinFlip.ADAPTER.encodeWithTag(writer, 4, coin_flip(value));\n"
        + "    bars.encodeWithTag(writer, 2, bars(value));\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public JavaMessage decode(ProtoReader reader) throws IOException {\n"
        + "    Foo field = null;\n"
        + "    Map<String, Bar> bars = Internal.newMutableMap();\n"
        + "    List<Integer> numbers = Internal.newMutableList();\n"
        + "    CoinFlip coin_flip = null;\n"
        + "    long token = reader.beginMessage();\n"
        + "    for (int tag; (tag = reader.nextTag()) != -1;) {\n"
        + "      switch (tag) {\n"
        + "        case 1: field = Foo.ADAPTER.decode(reader); break;\n"
        + "        case 2: bars.putAll(bars.decode(reader)); break;\n"
        + "        case 3: numbers.add(ProtoAdapter.INT32.decode(reader)); break;\n"
        + "        case 4: {\n"
        + "          try {\n"
        + "            coin_flip = CoinFlip.ADAPTER.decode(reader);\n"
        + "          } catch (ProtoAdapter.EnumConstantNotFoundException ignored) {\n"
        + "          }\n"
        + "          break;\n"
        + "        }\n"
        + "        default: {\n"
        + "          reader.skip();\n"
        + "        }\n"
        + "      }\n"
        + "    }\n"
        + "    reader.endMessage(token);\n"
        + "    return fromProto(field, numbers, coin_flip, bars);\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public JavaMessage redact(JavaMessage value) {\n"
        + "    return value;\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void generateAbstractAdapterWithRedactedField() throws IOException {
    RepoBuilder repoBuilder = new RepoBuilder()
        .add("message.proto", ""
            + "import \"option_redacted.proto\";\n"
            + "message ProtoMessage {\n"
            + "  optional string secret = 1 [(squareup.protos.redacted_option.redacted) = true];\n"
            + "}\n")
        .add("option_redacted.proto")
        .add("android.wire", ""
            + "syntax = \"wire2\";\n"
            + "import \"message.proto\";\n"
            + "type ProtoMessage {\n"
            + "  target JavaMessage using JavaMessage#ADAPTER;\n"
            + "}\n");
    assertThat(repoBuilder.generateCode("ProtoMessage", "android")).contains(""
        + "  @Override\n"
        + "  public JavaMessage redact(JavaMessage value) {\n"
        + "    return null;\n"
        + "  }\n");
  }

  @Test public void nestedAbstractAdapterIsStatic() throws IOException {
    RepoBuilder repoBuilder = new RepoBuilder()
        .add("message.proto", ""
            + "message A {\n"
            + "  message B {\n"
            + "    optional string c = 1;\n"
            + "  }\n"
            + "}\n")
        .add("android.wire", ""
            + "syntax = \"wire2\";\n"
            + "import \"message.proto\";\n"
            + "type A.B {\n"
            + "  target java.lang.String using AbAdapter#INSTANCE;\n"
            + "}\n");
    assertThat(repoBuilder.generateCode("A", "android")).contains(""
        + "  public abstract static class AbstractBAdapter extends ProtoAdapter<String> {\n");
  }
}
