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

import com.google.common.collect.ImmutableList;
import java.util.Map;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.squareup.wire.schema.Options.FIELD_OPTIONS;
import static com.squareup.wire.schema.Options.MESSAGE_OPTIONS;
import static org.assertj.core.api.Assertions.assertThat;

public final class PrunerTest {
  @Test public void retainType() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "message MessageA {\n"
            + "}\n"
            + "message MessageB {\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("MessageA")
        .build());
    assertThat(pruned.getType("MessageA")).isNotNull();
    assertThat(pruned.getType("MessageB")).isNull();
  }

  @Test public void retainTypeRetainsEnclosingButNotNested() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "message A {\n"
            + "  message B {\n"
            + "    message C {\n"
            + "    }\n"
            + "  }\n"
            + "  message D {\n"
            + "  }\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("A.B")
        .build());
    assertThat(pruned.getType("A")).isInstanceOf(EnclosingType.class);
    assertThat(pruned.getType("A.B")).isInstanceOf(MessageType.class);
    assertThat(pruned.getType("A.B.C")).isNull();
    assertThat(pruned.getType("A.D")).isNull();
  }

  @Test public void retainTypeRetainsFieldTypesTransitively() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "message MessageA {\n"
            + "  optional MessageB b = 1;\n"
            + "}\n"
            + "message MessageB {\n"
            + "  map<string, MessageC> c = 1;\n"
            + "}\n"
            + "message MessageC {\n"
            + "}\n"
            + "message MessageD {\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("MessageA")
        .build());
    assertThat(pruned.getType("MessageA")).isNotNull();
    assertThat(pruned.getType("MessageB")).isNotNull();
    assertThat(pruned.getType("MessageC")).isNotNull();
    assertThat(pruned.getType("MessageD")).isNull();
  }

  @Test public void retainRpcRetainsRequestAndResponseTypes() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "message RequestA {\n"
            + "}\n"
            + "message ResponseA {\n"
            + "}\n"
            + "message RequestB {\n"
            + "}\n"
            + "message ResponseB {\n"
            + "}\n"
            + "service Service {\n"
            + "  rpc CallA (RequestA) returns (ResponseA);\n"
            + "  rpc CallB (RequestB) returns (ResponseB);\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("Service#CallA")
        .build());
    assertThat(pruned.getService("Service").rpc("CallA")).isNotNull();
    assertThat(pruned.getType("RequestA")).isNotNull();
    assertThat(pruned.getType("ResponseA")).isNotNull();
    assertThat(pruned.getService("Service").rpc("CallB")).isNull();
    assertThat(pruned.getType("RequestB")).isNull();
    assertThat(pruned.getType("ResponseB")).isNull();
  }

  @Test public void retainField() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "message MessageA {\n"
            + "  optional string b = 1;\n"
            + "  map<string, string> c = 2;\n"
            + "}\n"
            + "message MessageB {\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("MessageA#b")
        .build());
    assertThat(((MessageType) pruned.getType("MessageA")).field("b")).isNotNull();
    assertThat(((MessageType) pruned.getType("MessageA")).field("c")).isNull();
    assertThat(pruned.getType("MessageB")).isNull();
  }

  @Test public void retainFieldRetainsFieldTypesTransitively() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "message MessageA {\n"
            + "  optional MessageB b = 1;\n"
            + "  optional MessageD d = 2;\n"
            + "}\n"
            + "message MessageB {\n"
            + "  optional MessageC c = 1;\n"
            + "}\n"
            + "message MessageC {\n"
            + "}\n"
            + "message MessageD {\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("MessageA#b")
        .build());
    assertThat(pruned.getType("MessageA")).isNotNull();
    assertThat(((MessageType) pruned.getType("MessageA")).field("b")).isNotNull();
    assertThat(((MessageType) pruned.getType("MessageA")).field("d")).isNull();
    assertThat(pruned.getType("MessageB")).isNotNull();
    assertThat(pruned.getType("MessageC")).isNotNull();
    assertThat(pruned.getType("MessageD")).isNull();
  }

  @Test public void retainFieldPrunesOneOf() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "message Message {\n"
            + "  oneof selection {\n"
            + "    string a = 1;\n"
            + "    string b = 2;\n"
            + "  }\n"
            + "  optional string c = 3;\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("Message#c")
        .build());
    assertThat(((MessageType) pruned.getType("Message")).oneOfs()).isEmpty();
  }

  @Test public void retainFieldRetainsOneOf() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "message Message {\n"
            + "  oneof selection {\n"
            + "    string a = 1;\n"
            + "    string b = 2;\n"
            + "  }\n"
            + "  optional string c = 3;\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("Message#b")
        .build());
    MessageType message = (MessageType) pruned.getType("Message");
    OneOf onlyOneOf = getOnlyElement(message.oneOfs());
    assertThat(onlyOneOf.name()).isEqualTo("selection");
    assertThat(getOnlyElement(onlyOneOf.fields()).name()).isEqualTo("b");
    assertThat(message.field("a")).isNull();
    assertThat(message.field("c")).isNull();
  }

  @Test public void typeWithRetainedMembersOnlyHasThoseMembersRetained() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "message MessageA {\n"
            + "  optional MessageB b = 1;\n"
            + "}\n"
            + "message MessageB {\n"
            + "  optional MessageC c = 1;\n"
            + "  optional MessageD d = 2;\n"
            + "}\n"
            + "message MessageC {\n"
            + "}\n"
            + "message MessageD {\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("MessageA#b")
        .include("MessageB#c")
        .build());
    assertThat(pruned.getType("MessageA")).isNotNull();
    assertThat(((MessageType) pruned.getType("MessageA")).field("b")).isNotNull();
    assertThat(pruned.getType("MessageB")).isNotNull();
    assertThat(((MessageType) pruned.getType("MessageB")).field("c")).isNotNull();
    assertThat(((MessageType) pruned.getType("MessageB")).field("d")).isNull();
    assertThat(pruned.getType("MessageC")).isNotNull();
    assertThat(pruned.getType("MessageD")).isNull();
  }

  @Test public void retainEnumConstant() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "enum Roshambo {\n"
            + "  ROCK = 0;\n"
            + "  SCISSORS = 1;\n"
            + "  PAPER = 2;\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("Roshambo#SCISSORS")
        .build());
    assertThat(((EnumType) pruned.getType("Roshambo")).constant("ROCK")).isNull();
    assertThat(((EnumType) pruned.getType("Roshambo")).constant("SCISSORS")).isNotNull();
    assertThat(((EnumType) pruned.getType("Roshambo")).constant("PAPER")).isNull();
  }

  @Test public void enumWithRetainedConstantHasThatConstantRetained() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "message Message {\n"
            + "  optional Roshambo roshambo = 1;\n"
            + "}\n"
            + "enum Roshambo {\n"
            + "  ROCK = 0;\n"
            + "  SCISSORS = 1;\n"
            + "  PAPER = 2;\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("Message")
        .include("Roshambo#SCISSORS")
        .build());
    assertThat(pruned.getType("Message")).isNotNull();
    assertThat(((MessageType) pruned.getType("Message")).field("roshambo")).isNotNull();
    assertThat(pruned.getType("Roshambo")).isNotNull();
    assertThat(((EnumType) pruned.getType("Roshambo")).constant("ROCK")).isNull();
    assertThat(((EnumType) pruned.getType("Roshambo")).constant("SCISSORS")).isNotNull();
    assertThat(((EnumType) pruned.getType("Roshambo")).constant("PAPER")).isNull();
  }

  @Test public void retainedOptionRetainsOptionsType() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "import \"google/protobuf/descriptor.proto\";\n"
            + "extend google.protobuf.FieldOptions {\n"
            + "  optional string a = 22001;\n"
            + "}\n"
            + "message Message {\n"
            + "  optional string f = 1 [a = \"a\"];\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("Message#f")
        .build());
    assertThat(((MessageType) pruned.getType("Message")).field("f")).isNotNull();
    assertThat(((MessageType) pruned.getType("google.protobuf.FieldOptions"))).isNotNull();
  }

  @Test public void prunedOptionDoesNotRetainOptionsType() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "import \"google/protobuf/descriptor.proto\";\n"
            + "extend google.protobuf.FieldOptions {\n"
            + "  optional string a = 22001;\n"
            + "}\n"
            + "message Message {\n"
            + "  optional string f = 1 [a = \"a\"];\n"
            + "  optional string g = 2;\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("Message#g")
        .build());
    assertThat(((MessageType) pruned.getType("google.protobuf.FieldOptions"))).isNull();
  }

  @Test public void optionRetainsField() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "import \"google/protobuf/descriptor.proto\";\n"
            + "message SomeFieldOptions {\n"
            + "  optional string a = 1;\n" // Retained via option use.
            + "  optional string b = 2;\n" // Retained explicitly.
            + "  optional string c = 3;\n" // Should be pruned.
            + "}\n"
            + "extend google.protobuf.FieldOptions {\n"
            + "  optional SomeFieldOptions some_field_options = 22001;\n"
            + "}\n"
            + "message Message {\n"
            + "  optional string f = 1 [some_field_options.a = \"a\"];\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("Message")
        .include("SomeFieldOptions#b")
        .build());
    assertThat(((MessageType) pruned.getType("Message")).field("f")).isNotNull();
    assertThat(((MessageType) pruned.getType("SomeFieldOptions")).field("a")).isNotNull();
    assertThat(((MessageType) pruned.getType("SomeFieldOptions")).field("b")).isNotNull();
    assertThat(((MessageType) pruned.getType("SomeFieldOptions")).field("c")).isNull();
  }

  @Test public void optionRetainsType() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "import \"google/protobuf/descriptor.proto\";\n"
            + "message SomeFieldOptions {\n"
            + "  optional string a = 1;\n" // Retained via option use.
            + "  optional string b = 2;\n" // Retained because 'a' is retained.
            + "  optional string c = 3;\n" // Retained because 'a' is retained.
            + "}\n"
            + "extend google.protobuf.FieldOptions {\n"
            + "  optional SomeFieldOptions some_field_options = 22001;\n"
            + "}\n"
            + "message Message {\n"
            + "  optional string f = 1 [some_field_options.a = \"a\"];\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("Message")
        .build());
    assertThat(((MessageType) pruned.getType("Message")).field("f")).isNotNull();
    assertThat(((MessageType) pruned.getType("SomeFieldOptions")).field("a")).isNotNull();
    assertThat(((MessageType) pruned.getType("SomeFieldOptions")).field("b")).isNotNull();
    assertThat(((MessageType) pruned.getType("SomeFieldOptions")).field("c")).isNotNull();
  }

  @Test public void retainExtension() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "message Message {\n"
            + "  optional string a = 1;\n"
            + "}\n"
            + "extend Message {\n"
            + "  optional string b = 2;\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("Message")
        .build());
    assertThat(((MessageType) pruned.getType("Message")).field("a")).isNotNull();
    assertThat(((MessageType) pruned.getType("Message")).extensionField("b")).isNotNull();
  }

  @Test public void retainExtensionMembers() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "message Message {\n"
            + "  optional string a = 1;\n"
            + "  optional string b = 2;\n"
            + "}\n"
            + "extend Message {\n"
            + "  optional string c = 3;\n"
            + "  optional string d = 4;\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("Message#a")
        .include("Message#c")
        .build());
    assertThat(((MessageType) pruned.getType("Message")).field("a")).isNotNull();
    assertThat(((MessageType) pruned.getType("Message")).field("b")).isNull();
    assertThat(((MessageType) pruned.getType("Message")).extensionField("c")).isNotNull();
    assertThat(((MessageType) pruned.getType("Message")).extensionField("d")).isNull();
  }

  /** When we include excludes only, the mark phase is skipped. */
  @Test public void excludeWithoutInclude() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "message MessageA {\n"
            + "  optional string b = 1;\n"
            + "  optional string c = 2;\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .exclude("MessageA#c")
        .build());
    assertThat(((MessageType) pruned.getType("MessageA")).field("b")).isNotNull();
    assertThat(((MessageType) pruned.getType("MessageA")).field("c")).isNull();
  }

  @Test public void excludeField() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "message MessageA {\n"
            + "  optional string b = 1;\n"
            + "  optional string c = 2;\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("MessageA")
        .exclude("MessageA#c")
        .build());
    assertThat(((MessageType) pruned.getType("MessageA")).field("b")).isNotNull();
    assertThat(((MessageType) pruned.getType("MessageA")).field("c")).isNull();
  }

  @Test public void excludeTypeExcludesField() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "message MessageA {\n"
            + "  optional MessageB b = 1;\n"
            + "  map<string, MessageC> c = 2;\n"
            + "}\n"
            + "message MessageB {\n"
            + "}\n"
            + "message MessageC {\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("MessageA")
        .exclude("MessageC")
        .build());
    assertThat(pruned.getType("MessageB")).isNotNull();
    assertThat(((MessageType) pruned.getType("MessageA")).field("b")).isNotNull();
    assertThat(pruned.getType("MessageC")).isNull();
    assertThat(((MessageType) pruned.getType("MessageA")).field("c")).isNull();
  }

  @Test public void excludeTypeExcludesRpc() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "service ServiceA {\n"
            + "  rpc CallB (MessageB) returns (MessageB);\n"
            + "  rpc CallC (MessageC) returns (MessageC);\n"
            + "}\n"
            + "message MessageB {\n"
            + "}\n"
            + "message MessageC {\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("ServiceA")
        .exclude("MessageC")
        .build());
    assertThat(pruned.getType("MessageB")).isNotNull();
    assertThat(pruned.getService("ServiceA").rpc("CallB")).isNotNull();
    assertThat(pruned.getType("MessageC")).isNull();
    assertThat(pruned.getService("ServiceA").rpc("CallC")).isNull();
  }

  @Test public void excludeRpcExcludesTypes() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "service ServiceA {\n"
            + "  rpc CallB (MessageB) returns (MessageB);\n"
            + "  rpc CallC (MessageC) returns (MessageC);\n"
            + "}\n"
            + "message MessageB {\n"
            + "}\n"
            + "message MessageC {\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("ServiceA")
        .exclude("ServiceA#CallC")
        .build());
    assertThat(pruned.getType("MessageB")).isNotNull();
    assertThat(pruned.getService("ServiceA").rpc("CallB")).isNotNull();
    assertThat(pruned.getType("MessageC")).isNull();
    assertThat(pruned.getService("ServiceA").rpc("CallC")).isNull();
  }

  @Test public void excludeFieldExcludesTypes() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("message.proto", ""
            + "message MessageA {\n"
            + "  optional MessageB b = 1;\n"
            + "  optional MessageC c = 2;\n"
            + "  map<string, MessageD> d = 3;\n"
            + "}\n"
            + "message MessageB {\n"
            + "}\n"
            + "message MessageC {\n"
            + "}\n"
            + "message MessageD {\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("MessageA")
        .exclude("MessageA#c")
        .exclude("MessageA#d")
        .build());
    assertThat(((MessageType) pruned.getType("MessageA")).field("b")).isNotNull();
    assertThat(pruned.getType("MessageB")).isNotNull();
    assertThat(((MessageType) pruned.getType("MessageA")).field("c")).isNull();
    assertThat(pruned.getType("MessageC")).isNull();
    assertThat(((MessageType) pruned.getType("MessageA")).field("d")).isNull();
    assertThat(pruned.getType("MessageD")).isNull();
  }

  @Test public void excludeEnumExcludesOptions() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("message.proto", ""
            + "import \"google/protobuf/descriptor.proto\";\n"
            + "enum Enum {\n"
            + "  A = 0;\n"
            + "  B = 1  [message.c = 1];\n"
            + "}\n"
            + "extend google.protobuf.EnumValueOptions {\n"
            + "  optional Message message = 70000;\n"
            + "};\n"
            + "message Message {\n"
            + "  optional int32 c = 1;\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("Enum")
        .exclude("Enum#B")
        .build());
    assertThat(((EnumType) pruned.getType("Enum")).constant("A")).isNotNull();
    assertThat(((EnumType) pruned.getType("Enum")).constant("B")).isNull();
    assertThat(pruned.getType("Message")).isNull();
  }

  @Test public void excludedFieldPrunesTopLevelOption() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "import \"google/protobuf/descriptor.proto\";\n"
            + "extend google.protobuf.FieldOptions {\n"
            + "  optional string a = 22001;\n"
            + "  optional string b = 22002;\n"
            + "}\n"
            + "message Message {\n"
            + "  optional string f = 1 [a = \"a\", b = \"b\"];\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .exclude("google.protobuf.FieldOptions#b")
        .build());
    Field field = ((MessageType) pruned.getType("Message")).field("f");
    assertThat(field.options().get(ProtoMember.get(FIELD_OPTIONS, "a"))).isEqualTo("a");
    assertThat(field.options().get(ProtoMember.get(FIELD_OPTIONS, "b"))).isNull();
  }

  @Test public void excludedTypePrunesTopLevelOption() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "import \"google/protobuf/descriptor.proto\";\n"
            + "message SomeFieldOptions {\n"
            + "  optional string a = 1;\n"
            + "}\n"
            + "extend google.protobuf.FieldOptions {\n"
            + "  optional SomeFieldOptions some_field_options = 22001;\n"
            + "  optional string b = 22002;\n"
            + "}\n"
            + "message Message {\n"
            + "  optional string f = 1 [some_field_options.a = \"a\", b = \"b\"];\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .exclude("SomeFieldOptions")
        .build());
    Field field = ((MessageType) pruned.getType("Message")).field("f");
    Map<ProtoMember, Object> map = field.options().map();
    Map.Entry<?, ?> onlyOption = getOnlyElement(map.entrySet());
    assertThat(((ProtoMember) onlyOption.getKey()).member()).isEqualTo("b");
    assertThat(onlyOption.getValue()).isEqualTo("b");
  }

  @Test public void excludedFieldPrunesNestedOption() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "import \"google/protobuf/descriptor.proto\";\n"
            + "message SomeFieldOptions {\n"
            + "  optional string a = 1;\n"
            + "  optional string b = 2;\n"
            + "}\n"
            + "extend google.protobuf.FieldOptions {\n"
            + "  optional SomeFieldOptions some_field_options = 22001;\n"
            + "}\n"
            + "message Message {\n"
            + "  optional string f = 1 [some_field_options = { a: \"a\", b: \"b\" }];\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .exclude("SomeFieldOptions#b")
        .build());
    Field field = ((MessageType) pruned.getType("Message")).field("f");
    Map<?, ?> map = (Map<?, ?>) field.options().get(
        ProtoMember.get(FIELD_OPTIONS, "some_field_options"));
    Map.Entry<?, ?> onlyOption = getOnlyElement(map.entrySet());
    assertThat(((ProtoMember) onlyOption.getKey()).member()).isEqualTo("a");
    assertThat(onlyOption.getValue()).isEqualTo("a");
  }

  @Test public void excludedTypePrunesNestedOption() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "import \"google/protobuf/descriptor.proto\";\n"
            + "message SomeFieldOptions {\n"
            + "  optional Dimensions dimensions = 1;\n"
            + "}\n"
            + "message Dimensions {\n"
            + "  optional string length = 1;\n"
            + "  optional string width = 2;\n"
            + "}\n"
            + "extend google.protobuf.FieldOptions {\n"
            + "  optional SomeFieldOptions some_field_options = 22001;\n"
            + "  optional string b = 22002;\n"
            + "}\n"
            + "message Message {\n"
            + "  optional string f = 1 [\n"
            + "      some_field_options = {\n"
            + "          dimensions: { length: \"100\" }\n"
            + "      },\n"
            + "      b = \"b\"\n"
            + "  ];\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .exclude("Dimensions")
        .build());
    Field field = ((MessageType) pruned.getType("Message")).field("f");
    Map<ProtoMember, Object> map = field.options().map();
    Map.Entry<?, ?> onlyOption = getOnlyElement(map.entrySet());
    assertThat(((ProtoMember) onlyOption.getKey()).member()).isEqualTo("b");
    assertThat(onlyOption.getValue()).isEqualTo("b");
  }

  @Test public void excludeOptions() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "import \"google/protobuf/descriptor.proto\";\n"
            + "extend google.protobuf.FieldOptions {\n"
            + "  optional string a = 22001;\n"
            + "  optional string b = 22002;\n"
            + "}\n"
            + "message Message {\n"
            + "  optional string f = 1 [ a = \"a\", b = \"b\" ];\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .exclude("google.protobuf.FieldOptions")
        .build());
    Field field = ((MessageType) pruned.getType("Message")).field("f");
    assertThat(field.options().map()).isEmpty();
  }

  @Test public void excludeRepeatedOptions() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "import \"google/protobuf/descriptor.proto\";\n"
            + "extend google.protobuf.MessageOptions {\n"
            + "  repeated string a = 22001;\n"
            + "  repeated string b = 22002;\n"
            + "}\n"
            + "message Message {\n"
            + "  option (a) = \"a1\";\n"
            + "  option (a) = \"a2\";\n"
            + "  option (b) = \"b1\";\n"
            + "  option (b) = \"b2\";\n"
            + "  optional string f = 1;\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .exclude("google.protobuf.MessageOptions#a")
        .build());
    MessageType message = (MessageType) pruned.getType("Message");
    assertThat(message.options().get(ProtoMember.get(MESSAGE_OPTIONS, "a"))).isNull();
    assertThat(message.options().get(ProtoMember.get(MESSAGE_OPTIONS, "b")))
        .isEqualTo(ImmutableList.of("b1", "b2"));
  }

  @Test public void includePackage() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("a/b/messages.proto", ""
            + "package a.b;\n"
            + "message MessageAB {\n"
            + "}\n")
        .add("a/c/messages.proto", ""
            + "package a.c;\n"
            + "message MessageAC {\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .include("a.b.*")
        .build());
    assertThat(pruned.getType("a.b.MessageAB")).isNotNull();
    assertThat(pruned.getType("a.c.MessageAC")).isNull();
  }

  @Test public void excludePackage() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("a/b/messages.proto", ""
            + "package a.b;\n"
            + "message MessageAB {\n"
            + "}\n")
        .add("a/c/messages.proto", ""
            + "package a.c;\n"
            + "message MessageAC {\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .exclude("a.c.*")
        .build());
    assertThat(pruned.getType("a.b.MessageAB")).isNotNull();
    assertThat(pruned.getType("a.c.MessageAC")).isNull();
  }

  @Test public void specialOptionsNotPruned() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("message.proto", ""
            + "option java_package = \"p\";\n"
            + "\n"
            + "message Message {\n"
            + "  optional int32 a = 1 [packed = true, deprecated = true, default = 5];\n"
            + "}\n"
            + "enum Enum {\n"
            + "  option allow_alias = true;\n"
            + "  A = 1;\n"
            + "  B = 1;\n"
            + "}\n")
        .build();
    Schema pruned = schema.prune(new IdentifierSet.Builder()
        .exclude("google.protobuf.*")
        .build());
    ProtoFile protoFile = pruned.protoFile("message.proto");
    assertThat(protoFile.javaPackage()).isEqualTo("p");

    MessageType message = (MessageType) pruned.getType("Message");
    Field field = message.field("a");
    assertThat(field.getDefault()).isEqualTo("5");
    assertThat(field.isDeprecated()).isTrue();
    assertThat(field.isPacked()).isTrue();

    EnumType enumType = (EnumType) pruned.getType("Enum");
    assertThat(enumType.allowAlias()).isTrue();
  }
}
