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
import static org.junit.Assert.fail;

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

  @Test public void tooManyFieldsTest() throws Exception {
    StringBuilder s = new StringBuilder();
    for (int i = 1; i < 257; i++) {
      s.append("  repeated int32 field_" + i + " = " + i + ";\n");
    }
    RepoBuilder repoBuilder = new RepoBuilder()
        .add("message.proto", ""
            + "message Message {\n"
            + s.toString()
            + "    oneof oneof_name {\n"
            + "       int32 foo = 257;\n"
            + "       int32 bar = 258;\n"
            + "    }\n"
            + "}\n");
    assertThat(repoBuilder.generateCode("Message")).contains(""
        + "public Message(Builder builder, ByteString unknownFields)");
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

  @Test public void generateAbstractAdapterForEnum() throws Exception {
    RepoBuilder repoBuilder = new RepoBuilder()
        .add("message.proto", ""
            + "package original.proto;\n"
            + "message ProtoMessage {\n"
            + "  optional CoinFlip coin_flip = 1;\n"
            + "}\n"
            + "enum CoinFlip {\n"
            + "  // In Canada this is the Queen!\n"
            + "  HEADS = 1;\n"
            + "  TAILS = 2;\n"
            + "}\n")
        .add("android.wire", ""
            + "syntax = \"wire2\";\n"
            + "import \"message.proto\";\n"
            + "package original.proto;\n"
            + "type original.proto.CoinFlip {\n"
            + "  target target.java.JavaCoinFlip using target.java.JavaCoinFlip#ADAPTER;\n"
            + "}\n");
    assertThat(repoBuilder.generateCode("original.proto.CoinFlip", "android")).isEqualTo(""
        + "package original.proto;\n"
        + "\n"
        + "import com.squareup.wire.FieldEncoding;\n"
        + "import com.squareup.wire.ProtoAdapter;\n"
        + "import com.squareup.wire.ProtoReader;\n"
        + "import com.squareup.wire.ProtoWriter;\n"
        + "import java.io.IOException;\n"
        + "import java.lang.Override;\n"
        + "import java.net.ProtocolException;\n"
        + "import target.java.JavaCoinFlip;\n"
        + "\n"
        + "public class CoinFlipAdapter extends ProtoAdapter<JavaCoinFlip> {\n"
        + "  /**\n"
        + "   * In Canada this is the Queen!\n"
        + "   */\n"
        + "  protected final JavaCoinFlip HEADS;\n"
        + "\n"
        + "  protected final JavaCoinFlip TAILS;\n"
        + "\n"
        + "  public CoinFlipAdapter(JavaCoinFlip HEADS, JavaCoinFlip TAILS) {\n"
        + "    super(FieldEncoding.VARINT, JavaCoinFlip.class);\n"
        + "    this.HEADS = HEADS;\n"
        + "    this.TAILS = TAILS;\n"
        + "  }\n"
        + "\n"
        + "  protected int toValue(JavaCoinFlip value) {\n"
        + "    if (value.equals(HEADS)) return 1;\n"
        + "    if (value.equals(TAILS)) return 2;\n"
        + "    return -1;\n"
        + "  }\n"
        + "\n"
        + "  protected JavaCoinFlip fromValue(int value) {\n"
        + "    switch (value) {\n"
        + "      case 1: return HEADS;\n"
        + "      case 2: return TAILS;\n"
        + "      default: throw new ProtoAdapter.EnumConstantNotFoundException(value, JavaCoinFlip.class);\n"
        + "    }\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public int encodedSize(JavaCoinFlip value) {\n"
        + "    return ProtoAdapter.UINT32.encodedSize(toValue(value));\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public void encode(ProtoWriter writer, JavaCoinFlip value) throws IOException {\n"
        + "    int i = toValue(value);\n"
        + "    if (i == -1) throw new ProtocolException(\"Unexpected enum constant: \" + value);\n"
        + "    writer.writeVarint32(i);\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public JavaCoinFlip decode(ProtoReader reader) throws IOException {\n"
        + "    int value = reader.readVarint32();\n"
        + "    return fromValue(value);\n"
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

  /** https://github.com/square/wire/issues/655 */
  @Test public void defaultValues() throws IOException {
    RepoBuilder repoBuilder = new RepoBuilder()
        .add("message.proto", ""
            + "message Message {\n"
            + "  optional int32 a = 1 [default = 10 ];\n"
            + "  optional int32 b = 2 [default = 0x20 ];\n"
            + "  optional int64 c = 3 [default = 11 ];\n"
            + "  optional int64 d = 4 [default = 0x21 ];\n"
            + "}\n");
    String code = repoBuilder.generateCode("Message");
    assertThat(code).contains("  public static final Integer DEFAULT_A = 10;");
    assertThat(code).contains("  public static final Integer DEFAULT_B = 32;");
    assertThat(code).contains("  public static final Long DEFAULT_C = 11L;");
    assertThat(code).contains("  public static final Long DEFAULT_D = 33L;");
  }

  @Test public void defaultValuesMustNotBeOctal() throws IOException {
    RepoBuilder repoBuilder = new RepoBuilder()
        .add("message.proto", ""
            + "message Message {\n"
            + "  optional int32 a = 1 [default = 020 ];\n"
            + "  optional int64 b = 2 [default = 021 ];\n"
            + "}\n");
    try {
      repoBuilder.generateCode("Message");
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage("Octal literal unsupported: 020");
    }
  }

  @Test public void nullableFieldsWithoutParcelable() {
    Schema schema = new RepoBuilder()
        .add("message.proto", ""
            + "message A {\n"
            + "  message B {\n"
            + "    optional string c = 1;\n"
            + "  }\n"
            + "}\n")
        .schema();
    MessageType message = (MessageType) schema.getType("A");
    JavaGenerator javaGenerator = JavaGenerator.get(schema).withAndroidAnnotations(true);
    TypeSpec typeSpec = javaGenerator.generateType(message);
    assertThat(JavaFile.builder("", typeSpec).build().toString()).contains(""
        + " @WireField(\n"
        + "        tag = 1,\n"
        + "        adapter = \"com.squareup.wire.ProtoAdapter#STRING\"\n"
        + "    )\n"
        + "    @Nullable\n"
        + "    public final String c;");
  }

  @Test public void androidSupport() {
    Schema schema = new RepoBuilder()
        .add("message.proto", ""
            + "message A {\n"
            + "  message B {\n"
            + "    optional string c = 1;\n"
            + "  }\n"
            + "}\n")
        .schema();
    MessageType message = (MessageType) schema.getType("A");
    JavaGenerator javaGenerator = JavaGenerator.get(schema).withAndroid(true);
    TypeSpec typeSpec = javaGenerator.generateType(message);
    assertThat(JavaFile.builder("", typeSpec).build().toString()).contains(""
        + " @WireField(\n"
        + "        tag = 1,\n"
        + "        adapter = \"com.squareup.wire.ProtoAdapter#STRING\"\n"
        + "    )\n"
        + "    @Nullable\n"
        + "    public final String c;")
        .contains(""
        + "public static final Parcelable.Creator<B> CREATOR = AndroidMessage.newCreator(ADAPTER)");
        ;
  }
}
