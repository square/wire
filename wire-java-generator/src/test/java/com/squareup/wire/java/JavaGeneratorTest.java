/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.java;

import static com.google.common.truth.Truth.assertThat;
import static com.squareup.wire.schema.SchemaHelpersJvmKt.addFromTest;
import static org.junit.Assert.fail;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import com.squareup.wire.SchemaBuilder;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.PruningRules;
import com.squareup.wire.schema.Schema;
import java.io.IOException;
import okio.Path;
import org.junit.Test;

public final class JavaGeneratorTest {
  @Test
  public void sanitizeJavadocStripsTrailingWhitespace() {
    String input = "The quick brown fox  \nJumps over  \n\t \t\nThe lazy dog  ";
    String expected = "The quick brown fox\nJumps over\n\nThe lazy dog";
    assertThat(JavaGenerator.sanitizeJavadoc(input)).isEqualTo(expected);
  }

  @Test
  public void sanitizeJavadocWrapsSeeLinks() {
    String input = "Google query.\n\n@see http://google.com";
    String expected = "Google query.\n\n@see <a href=\"http://google.com\">http://google.com</a>";
    assertThat(JavaGenerator.sanitizeJavadoc(input)).isEqualTo(expected);
  }

  @Test
  public void sanitizeJavadocStarSlash() {
    String input = "/* comment inside comment. */";
    String expected = "/* comment inside comment. &#42;/";
    assertThat(JavaGenerator.sanitizeJavadoc(input)).isEqualTo(expected);
  }

  @Test
  public void generateTypeUsesNameAllocatorInMessageBuilderBuild() throws Exception {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("message.proto"),
                "" + "message Message {\n" + "  required float long = 1;\n" + "}\n")
            .build();
    assertThat(new JavaWithProfilesGenerator(schema).generateJava("Message"))
        .contains(
            ""
                + "    @Override\n"
                + "    public Message build() {\n"
                + "      if (long_ == null) {\n"
                + "        throw Internal.missingRequiredFields(long_, \"long\");\n"
                + "      }\n"
                + "      return new Message(long_, super.buildUnknownFields());\n"
                + "    }\n");
  }

  @Test
  public void tooManyFieldsTest() throws Exception {
    StringBuilder s = new StringBuilder();
    for (int i = 1; i < 257; i++) {
      s.append("  repeated int32 field_" + i + " = " + i + ";\n");
    }
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("message.proto"),
                ""
                    + "message Message {\n"
                    + s.toString()
                    + "    oneof oneof_name {\n"
                    + "       int32 foo = 257;\n"
                    + "       int32 bar = 258;\n"
                    + "    }\n"
                    + "}\n")
            .build();
    assertThat(new JavaWithProfilesGenerator(schema).generateJava("Message"))
        .contains("" + "public Message(Builder builder, ByteString unknownFields)");
  }

  @Test
  public void map() throws Exception {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("message.proto"),
                ""
                    + "message Message {\n"
                    + "  map<string, CdnResource> templates = 1;\n"
                    + "  message CdnResource {\n"
                    + "  }\n"
                    + "}\n")
            .build();
    MessageType message = (MessageType) schema.getType("Message");
    JavaGenerator javaGenerator = JavaGenerator.get(schema);
    TypeSpec typeSpec = javaGenerator.generateType(message);
    assertThat(JavaFile.builder("", typeSpec).build().toString())
        .contains(
            ""
                + "  @WireField(\n"
                + "      tag = 1,\n"
                + "      keyAdapter = \"com.squareup.wire.ProtoAdapter#STRING\",\n"
                + "      adapter = \"Message$CdnResource#ADAPTER\"\n"
                + "  )\n"
                + "  public final Map<String, CdnResource> templates;\n");
  }

  @Test
  public void generateAbstractAdapter() throws Exception {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("message.proto"),
                ""
                    + "package original.proto;\n"
                    + "option java_package = \"original.java\";\n"
                    + "import \"foo.proto\";\n"
                    + "message ProtoMessage {\n"
                    + "  optional foo.proto.Foo field = 1;\n"
                    + "  repeated int32 numbers = 3;\n"
                    + "  optional foo.proto.CoinFlip coin_flip = 4;\n"
                    + "  map<string, foo.proto.Bar> bars = 2;\n"
                    + "}\n")
            .add(
                Path.get("foo.proto"),
                ""
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
            .build();
    assertThat(
            new JavaWithProfilesGenerator(schema)
                .withProfile(
                    "android.wire",
                    ""
                        + "syntax = \"wire2\";\n"
                        + "import \"message.proto\";\n"
                        + "package original.proto;\n"
                        + "type original.proto.ProtoMessage {\n"
                        + "  target target.java.JavaMessage using target.java.JavaMessage#ADAPTER;\n"
                        + "}\n")
                .generateJava("original.proto.ProtoMessage", "android"))
        .isEqualTo(
            ""
                + "package original.java;\n"
                + "\n"
                + "import com.squareup.wire.FieldEncoding;\n"
                + "import com.squareup.wire.ProtoAdapter;\n"
                + "import com.squareup.wire.ProtoReader;\n"
                + "import com.squareup.wire.ProtoWriter;\n"
                + "import com.squareup.wire.ReverseProtoWriter;\n"
                + "import com.squareup.wire.Syntax;\n"
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
                + "  private ProtoAdapter<Map<String, Bar>> bars;\n"
                + "\n"
                + "  public AbstractProtoMessageAdapter() {\n"
                + "    super(FieldEncoding.LENGTH_DELIMITED, JavaMessage.class, \"type.googleapis.com/original.proto.ProtoMessage\", Syntax.PROTO_2, null, \"message.proto\");\n"
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
                + "    int result = 0;\n"
                + "    result += Foo.ADAPTER.encodedSizeWithTag(1, field(value));\n"
                + "    result += ProtoAdapter.INT32.asRepeated().encodedSizeWithTag(3, numbers(value));\n"
                + "    result += CoinFlip.ADAPTER.encodedSizeWithTag(4, coin_flip(value));\n"
                + "    result += barsAdapter().encodedSizeWithTag(2, bars(value));\n"
                + "    return result;\n"
                + "  }\n"
                + "\n"
                + "  @Override\n"
                + "  public void encode(ProtoWriter writer, JavaMessage value) throws IOException {\n"
                + "    Foo.ADAPTER.encodeWithTag(writer, 1, field(value));\n"
                + "    ProtoAdapter.INT32.asRepeated().encodeWithTag(writer, 3, numbers(value));\n"
                + "    CoinFlip.ADAPTER.encodeWithTag(writer, 4, coin_flip(value));\n"
                + "    barsAdapter().encodeWithTag(writer, 2, bars(value));\n"
                + "  }\n"
                + "\n"
                + "  @Override\n"
                + "  public void encode(ReverseProtoWriter writer, JavaMessage value) throws IOException {\n"
                + "    barsAdapter().encodeWithTag(writer, 2, bars(value));\n"
                + "    CoinFlip.ADAPTER.encodeWithTag(writer, 4, coin_flip(value));\n"
                + "    ProtoAdapter.INT32.asRepeated().encodeWithTag(writer, 3, numbers(value));\n"
                + "    Foo.ADAPTER.encodeWithTag(writer, 1, field(value));\n"
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
                + "        case 2: bars.putAll(barsAdapter().decode(reader)); break;\n"
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
                + "    reader.endMessageAndGetUnknownFields(token);\n"
                + "    return fromProto(field, numbers, coin_flip, bars);\n"
                + "  }\n"
                + "\n"
                + "  @Override\n"
                + "  public JavaMessage redact(JavaMessage value) {\n"
                + "    return value;\n"
                + "  }\n"
                + "\n"
                + "  private ProtoAdapter<Map<String, Bar>> barsAdapter() {\n"
                + "    ProtoAdapter<Map<String, Bar>> result = bars;\n"
                + "    if (result == null) {\n"
                + "      result = ProtoAdapter.newMapAdapter(ProtoAdapter.STRING, Bar.ADAPTER);\n"
                + "      bars = result;\n"
                + "    }\n"
                + "    return result;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void generateAbstractAdapterForEnum() throws Exception {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("message.proto"),
                ""
                    + "package original.proto;\n"
                    + "message ProtoMessage {\n"
                    + "  optional CoinFlip coin_flip = 1;\n"
                    + "}\n"
                    + "enum CoinFlip {\n"
                    + "  // In Canada this is the Queen!\n"
                    + "  HEADS = 1;\n"
                    + "  TAILS = 2;\n"
                    + "}\n")
            .build();
    assertThat(
            new JavaWithProfilesGenerator(schema)
                .withProfile(
                    "android.wire",
                    ""
                        + "syntax = \"wire2\";\n"
                        + "import \"message.proto\";\n"
                        + "package original.proto;\n"
                        + "type original.proto.CoinFlip {\n"
                        + "  target target.java.JavaCoinFlip using target.java.JavaCoinFlip#ADAPTER;\n"
                        + "}\n")
                .generateJava("original.proto.CoinFlip", "android"))
        .isEqualTo(
            ""
                + "package original.proto;\n"
                + "\n"
                + "import com.squareup.wire.FieldEncoding;\n"
                + "import com.squareup.wire.ProtoAdapter;\n"
                + "import com.squareup.wire.ProtoReader;\n"
                + "import com.squareup.wire.ProtoWriter;\n"
                + "import com.squareup.wire.ReverseProtoWriter;\n"
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
                + "  public void encode(ReverseProtoWriter writer, JavaCoinFlip value) throws IOException {\n"
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
                + "\n"
                + "  @Override\n"
                + "  public JavaCoinFlip redact(JavaCoinFlip value) {\n"
                + "    return value;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void generateAbstractAdapterWithRedactedField() throws IOException {
    SchemaBuilder builder =
        new SchemaBuilder()
            .add(
                Path.get("message.proto"),
                ""
                    + "import \"option_redacted.proto\";\n"
                    + "message ProtoMessage {\n"
                    + "  optional string secret = 1 [(squareup.protos.redacted_option.redacted) = true];\n"
                    + "}\n");
    addFromTest(builder, Path.get("option_redacted.proto"));
    Schema schema = builder.build();
    assertThat(
            new JavaWithProfilesGenerator(schema)
                .withProfile(
                    "android.wire",
                    ""
                        + "syntax = \"wire2\";\n"
                        + "import \"message.proto\";\n"
                        + "type ProtoMessage {\n"
                        + "  target JavaMessage using JavaMessage#ADAPTER;\n"
                        + "}\n")
                .generateJava("ProtoMessage", "android"))
        .contains(
            ""
                + "  @Override\n"
                + "  public JavaMessage redact(JavaMessage value) {\n"
                + "    return null;\n"
                + "  }\n");
  }

  @Test
  public void nestedAbstractAdapterIsStatic() throws IOException {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("message.proto"),
                ""
                    + "message A {\n"
                    + "  message B {\n"
                    + "    optional string c = 1;\n"
                    + "  }\n"
                    + "}\n")
            .build();
    assertThat(
            new JavaWithProfilesGenerator(schema)
                .withProfile(
                    "android.wire",
                    ""
                        + "syntax = \"wire2\";\n"
                        + "import \"message.proto\";\n"
                        + "type A.B {\n"
                        + "  target java.lang.String using AbAdapter#INSTANCE;\n"
                        + "}\n")
                .generateJava("A", "android"))
        .contains(
            ""
                + "  public abstract static class AbstractBAdapter extends ProtoAdapter<String> {\n");
  }

  /** https://github.com/square/wire/issues/655 */
  @Test
  public void defaultValues() throws IOException {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("message.proto"),
                ""
                    + "message Message {\n"
                    + "  optional int32 a = 1 [default = 10 ];\n"
                    + "  optional int32 b = 2 [default = 0x20 ];\n"
                    + "  optional int64 c = 3 [default = 11 ];\n"
                    + "  optional int64 d = 4 [default = 0x21 ];\n"
                    + "  optional float e = 5 [default = inf ];\n"
                    + "  optional double f = 6 [default = -inf ];\n"
                    + "  optional double g = 7 [default = nan ];\n"
                    + "  optional double h = 8 [default = -nan ];\n"
                    + "}\n")
            .build();
    String code = new JavaWithProfilesGenerator(schema).generateJava("Message");
    assertThat(code).contains("  public static final Integer DEFAULT_A = 10;");
    assertThat(code).contains("  public static final Integer DEFAULT_B = 32;");
    assertThat(code).contains("  public static final Long DEFAULT_C = 11L;");
    assertThat(code).contains("  public static final Long DEFAULT_D = 33L;");
    assertThat(code).contains("  public static final Float DEFAULT_E = Float.POSITIVE_INFINITY;");
    assertThat(code).contains("  public static final Double DEFAULT_F = Double.NEGATIVE_INFINITY;");
    assertThat(code).contains("  public static final Double DEFAULT_G = Double.NaN;");
    assertThat(code).contains("  public static final Double DEFAULT_H = Double.NaN;");
  }

  @Test
  public void defaultValuesMustNotBeOctal() throws IOException {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("message.proto"),
                ""
                    + "message Message {\n"
                    + "  optional int32 a = 1 [default = 020 ];\n"
                    + "  optional int64 b = 2 [default = 021 ];\n"
                    + "}\n")
            .build();
    try {
      new JavaWithProfilesGenerator(schema).generateJava("Message");
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessageThat().contains("Octal literal unsupported: 020");
    }
  }

  @Test
  public void nullableFieldsWithoutParcelable() throws IOException {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("message.proto"),
                ""
                    + "message A {\n"
                    + "  message B {\n"
                    + "    optional string c = 1;\n"
                    + "  }\n"
                    + "}\n")
            .build();
    MessageType message = (MessageType) schema.getType("A");
    JavaGenerator javaGenerator = JavaGenerator.get(schema).withAndroidAnnotations(true);
    TypeSpec typeSpec = javaGenerator.generateType(message);
    assertThat(JavaFile.builder("", typeSpec).build().toString())
        .contains(
            ""
                + " @WireField(\n"
                + "        tag = 1,\n"
                + "        adapter = \"com.squareup.wire.ProtoAdapter#STRING\"\n"
                + "    )\n"
                + "    @Nullable\n"
                + "    public final String c;");
  }

  @Test
  public void unsortedTagsPrintSchemaIndex() throws IOException {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("message.proto"),
                ""
                    + "message A {\n"
                    + "  optional string two = 2;\n"
                    + "  optional string one = 1;\n"
                    + "}\n")
            .build();
    MessageType message = (MessageType) schema.getType("A");
    JavaGenerator javaGenerator = JavaGenerator.get(schema).withAndroidAnnotations(true);
    TypeSpec typeSpec = javaGenerator.generateType(message);
    String javaOutput = JavaFile.builder("", typeSpec).build().toString();
    System.out.println(javaOutput);
    assertThat(javaOutput)
        .contains(
            ""
                + "  @WireField(\n"
                + "      tag = 2,\n"
                + "      adapter = \"com.squareup.wire.ProtoAdapter#STRING\"\n"
                + "  )\n"
                + "  @Nullable\n"
                + "  public final String two;");
    assertThat(javaOutput)
        .contains(
            ""
                + "  @WireField(\n"
                + "      tag = 1,\n"
                + "      adapter = \"com.squareup.wire.ProtoAdapter#STRING\"\n"
                + "  )\n"
                + "  @Nullable\n"
                + "  public final String one;");
  }

  @Test
  public void androidSupport() throws IOException {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("message.proto"),
                ""
                    + "message A {\n"
                    + "  message B {\n"
                    + "    optional string c = 1;\n"
                    + "  }\n"
                    + "}\n")
            .build();
    MessageType message = (MessageType) schema.getType("A");
    JavaGenerator javaGenerator = JavaGenerator.get(schema).withAndroid(true);
    TypeSpec typeSpec = javaGenerator.generateType(message);
    String javaOutput = JavaFile.builder("", typeSpec).build().toString();
    assertThat(javaOutput)
        .contains(
            ""
                + " @WireField(\n"
                + "        tag = 1,\n"
                + "        adapter = \"com.squareup.wire.ProtoAdapter#STRING\"\n"
                + "    )\n"
                + "    public final String c;");
    assertThat(javaOutput)
        .contains(
            ""
                + "public static final Parcelable.Creator<B> CREATOR = AndroidMessage.newCreator(ADAPTER)");
  }

  @Test
  public void enclosingTypeIsNotMessage() throws IOException {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("message.proto"),
                ""
                    + "message A {\n"
                    + "  message B {\n"
                    + "  }\n"
                    + "  optional B b = 1;\n"
                    + "}\n")
            .build();

    Schema pruned = schema.prune(new PruningRules.Builder().addRoot("A.B").build());

    JavaGenerator javaGenerator = JavaGenerator.get(schema);
    TypeSpec typeSpec = javaGenerator.generateType(pruned.getType("A"));
    String javaOutput = JavaFile.builder("", typeSpec).build().toString();
    assertThat(javaOutput)
        .contains(
            ""
                + "@WireEnclosingType\n"
                + "public final class A {\n"
                + "  private A() {\n"
                + "    throw new AssertionError();\n"
                + "  }");
    assertThat(javaOutput).contains("public static final class B extends Message<B, B.Builder> {");
  }

  @Test
  public void generateTypeUsesPackageNameOnFieldAndClassNameClash() throws Exception {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("person.proto"),
                ""
                    + "package common.proto;\n"
                    + "enum Gender {\n"
                    + "  Gender_Male = 0;\n"
                    + "  Gender_Female = 1;\n"
                    + "}\n"
                    + "message Person {\n"
                    + "  optional Gender Gender = 1;\n"
                    + "}\n")
            .build();
    assertThat(new JavaWithProfilesGenerator(schema).generateJava("common.proto.Person"))
        .contains("public final Gender common_proto_Gender;");
  }

  @Test
  public void buildersOnlyGeneratesNonPublicConstructors() throws Exception {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("message.proto"),
                ""
                    + "syntax = \"proto2\";\n"
                    + "message SomeMessage {\n"
                    + "  optional string a = 1;\n"
                    + "  optional string b = 2;\n"
                    + "  message InnerMessage {\n"
                    + "    optional string c = 3;\n"
                    + "    optional string d = 8;\n"
                    + "  }\n"
                    + "}\n")
            .build();
    String javaOutput =
        new JavaWithProfilesGenerator(schema)
            .generateJava("SomeMessage", null /* profileName */, true /* buildersOnly */);
    assertThat(javaOutput).contains("  SomeMessage(");
    assertThat(javaOutput).contains("  InnerMessage(");
    assertThat(javaOutput).doesNotContain("public SomeMessage(");
    assertThat(javaOutput).doesNotContain("public InnerMessage(");
  }

  @Test
  public void generateTypeUsesPackageNameOnFieldAndClassNameClashWithinPackage() throws Exception {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("a.proto"),
                ""
                    + "package common.proto;\n"
                    + "enum Status {\n"
                    + "  Status_Approved = 0;\n"
                    + "  Status_Denied = 1;\n"
                    + "}\n"
                    + "enum AnotherStatus {\n"
                    + "  AnotherStatus_Processing = 0;\n"
                    + "  AnotherStatus_Completed = 1;\n"
                    + "}\n"
                    + "message A {\n"
                    + "  message B {\n"
                    + "    optional Status Status = 1;\n"
                    + "  }\n"
                    + "  repeated B b = 1;"
                    + "  optional AnotherStatus Status = 2;\n"
                    + "}\n")
            .build();
    assertThat(new JavaWithProfilesGenerator(schema).generateJava("common.proto.A"))
        .contains("public final AnotherStatus common_proto_Status;");
  }

  @Test
  public void fieldHasScalarName() throws Exception {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("example.proto"),
                ""
                    + "package squareup.testing.wire;\n"
                    + "\n"
                    + "option java_package = \"com.squareup.testing.wire\";\n"
                    + "\n"
                    + "message Data {\n"
                    + "  optional string string = 1;\n"
                    + "  repeated string values = 2;\n"
                    + "}\n")
            .build();
    assertThat(new JavaWithProfilesGenerator(schema).generateJava("squareup.testing.wire.Data"))
        .contains(
            ""
                + "    public Builder string(String string) {\n"
                + "      this.string = string;\n"
                + "      return this;\n"
                + "    }");
  }

  @Test
  public void sanitizeStringsOnPrinting() throws Exception {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("example.proto"),
                ""
                    + "message Person {\n"
                    + "  required string name = 1;\n"
                    + "  required int32 id = 2;\n"
                    + "  repeated PhoneNumber phone = 3;\n"
                    + "  repeated string aliases = 4;\n"
                    + "\n"
                    + "  message PhoneNumber {\n"
                    + "    required string number = 1;\n"
                    + "    optional PhoneType type = 2 [default = HOME];\n"
                    + "  }\n"
                    + "  enum PhoneType {\n"
                    + "    HOME = 0;\n"
                    + "    WORK = 1;\n"
                    + "    MOBILE = 2;\n"
                    + "  }\n"
                    + "}\n")
            .build();
    String generatedCode = new JavaWithProfilesGenerator(schema).generateJava("Person");
    assertThat(generatedCode)
        .contains(
            ""
                + "  public String toString() {\n"
                + "    StringBuilder builder = new StringBuilder();\n"
                + "    builder.append(\", name=\").append(Internal.sanitize(name));\n"
                + "    builder.append(\", id=\").append(id);\n"
                + "    if (!phone.isEmpty()) builder.append(\", phone=\").append(phone);\n"
                + "    if (!aliases.isEmpty()) builder.append(\", aliases=\").append(Internal.sanitize(aliases));\n"
                + "    return builder.replace(0, 2, \"Person{\").append('}').toString();\n"
                + "  }");
    assertThat(generatedCode)
        .contains(
            ""
                + "    public String toString() {\n"
                + "      StringBuilder builder = new StringBuilder();\n"
                + "      builder.append(\", number=\").append(Internal.sanitize(number));\n"
                + "      if (type != null) builder.append(\", type=\").append(type);\n"
                + "      return builder.replace(0, 2, \"PhoneNumber{\").append('}').toString();\n"
                + "    }");
  }

  @Test
  public void wirePackageTakesPrecedenceOverJavaPackage() throws IOException {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("proto_package/person.proto"),
                "package proto_package;\n"
                    + "import \"wire/extensions.proto\";\n"
                    + "\n"
                    + "option java_package = \"java_package\";\n"
                    + "option (wire.wire_package) = \"wire_package\";\n"
                    + "\n"
                    + "message Person {\n"
                    + "	required string name = 1;\n"
                    + "}\n")
            .build();
    String code = new JavaWithProfilesGenerator(schema).generateJava("proto_package.Person");
    assertThat(code).contains("package wire_package");
    assertThat(code).contains("class Person");
  }

  @Test
  public void wirePackageTakesPrecedenceOverProtoPackage() throws IOException {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("proto_package/person.proto"),
                "package proto_package;\n"
                    + "import \"wire/extensions.proto\";\n"
                    + "\n"
                    + "option (wire.wire_package) = \"wire_package\";\n"
                    + "\n"
                    + "message Person {\n"
                    + "	required string name = 1;\n"
                    + "}\n")
            .build();
    String code = new JavaWithProfilesGenerator(schema).generateJava("proto_package.Person");
    assertThat(code).contains("package wire_package");
    assertThat(code).contains("class Person");
  }

  @Test
  public void packageNameUsedIfFieldNameIsSameAsNonScalarTypeName() throws IOException {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("common/common_message.proto"),
                "package a.Common;\n"
                    + "option java_package = \"a.common\";"
                    + "message CommonMessage {\n"
                    + "   required string First = 1;\n"
                    + "}\n")
            .add(
                Path.get("example.proto"),
                "package a;\n"
                    + "import \"common/common_message.proto\";\n"
                    + "\n"
                    + "message Example {\n"
                    + "   required Common.CommonMessage CommonMessage = 1;\n"
                    + "}\n")
            .build();
    String code = new JavaWithProfilesGenerator(schema).generateJava("a.Example");
    assertThat(code).contains("package a");
    assertThat(code).contains("import a.common.CommonMessage");
    assertThat(code).contains("public CommonMessage a_CommonMessage");
  }

  @Test
  public void wirePackageUsedInImport() throws IOException {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("proto_package/person.proto"),
                "package proto_package;\n"
                    + "import \"wire/extensions.proto\";\n"
                    + "\n"
                    + "option (wire.wire_package) = \"wire_package\";\n"
                    + "\n"
                    + "message Person {\n"
                    + "	required string name = 1;\n"
                    + "}\n")
            .add(
                Path.get("city_package/home.proto"),
                "package city_package;\n"
                    + "import \"proto_package/person.proto\";\n"
                    + "\n"
                    + "message Home {\n"
                    + "	repeated proto_package.Person person = 1;\n"
                    + "}\n")
            .build();
    String code = new JavaWithProfilesGenerator(schema).generateJava("city_package.Home");
    assertThat(code).contains("package city_package");
    assertThat(code).contains("import wire_package.Person");
  }

  @Test
  public void deprecatedEnum() throws IOException {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("proto_package/person.proto"),
                "package proto_package;\n"
                    + "enum Direction {\n"
                    + "  option deprecated = true;\n"
                    + "  NORTH = 1;\n"
                    + "  EAST = 2;\n"
                    + "  SOUTH = 3;\n"
                    + "  WEST = 4;\n"
                    + "}\n")
            .build();
    String code = new JavaWithProfilesGenerator(schema).generateJava("proto_package.Direction");
    assertThat(code).contains("@Deprecated\npublic enum Direction");
  }

  @Test
  public void deprecatedEnumConstant() throws IOException {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("proto_package/person.proto"),
                "package proto_package;\n"
                    + "enum Direction {\n"
                    + "  NORTH = 1;\n"
                    + "  EAST = 2 [deprecated = true];\n"
                    + "  SOUTH = 3;\n"
                    + "  WEST = 4;\n"
                    + "}\n")
            .build();
    String code = new JavaWithProfilesGenerator(schema).generateJava("proto_package.Direction");
    assertThat(code).contains("  @Deprecated\n  EAST(2)");
  }

  @Test
  public void deprecatedField() throws IOException {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("proto_package/person.proto"),
                "package proto_package;\n"
                    + "message Person {\n"
                    + "  optional string name = 1 [deprecated = true];\n"
                    + "}\n")
            .build();
    String code = new JavaWithProfilesGenerator(schema).generateJava("proto_package.Person");
    assertThat(code).contains("  @Deprecated\n  public final String name;");
  }

  @Test
  public void deprecatedMessage() throws IOException {
    Schema schema =
        new SchemaBuilder()
            .add(
                Path.get("proto_package/person.proto"),
                "package proto_package;\n"
                    + "message Person {\n"
                    + "  option deprecated = true;\n"
                    + "  optional string name = 1;\n"
                    + "}\n")
            .build();
    String code = new JavaWithProfilesGenerator(schema).generateJava("proto_package.Person");
    assertThat(code).contains("@Deprecated\npublic final class Person");
  }
}
