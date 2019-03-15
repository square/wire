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
package com.squareup.wire.schema

import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterables.getOnlyElement
import com.squareup.wire.schema.Options.FIELD_OPTIONS
import com.squareup.wire.schema.Options.MESSAGE_OPTIONS
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PrunerTest {
  @Test
  fun retainType() {
    val schema = RepoBuilder()
        .add("service.proto", """
             |message MessageA {
             |}
             |message MessageB {
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("MessageA")
        .build())
    assertThat(pruned.getType("MessageA")).isNotNull
    assertThat(pruned.getType("MessageB")).isNull()
  }

  @Test
  fun retainMap() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |message MessageA {
            |  map<string, MessageB> maps = 1;
            |  message MessageB {
            |  }
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("MessageA")
        .build())
    assertThat(pruned.getType("MessageA")).isNotNull
    assertThat(pruned.getField(ProtoMember.get("MessageA#maps"))).isNotNull
  }

  @Test
  fun excludeMap() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |message MessageA {
            |  map<string, MessageB> maps = 1;
            |  message MessageB {
            |  }
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("MessageA")
        .exclude("MessageA#maps")
        .build())
    assertThat(pruned.getType("MessageA")).isNotNull
    assertThat(pruned.getField(ProtoMember.get("MessageA#maps"))).isNull()
  }

  @Test
  fun retainTypeRetainsEnclosingButNotNested() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |message A {
            |  message B {
            |    message C {
            |    }
            |  }
            |  message D {
            |  }
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("A.B")
        .build())
    assertThat(pruned.getType("A")).isInstanceOf(EnclosingType::class.java)
    assertThat(pruned.getType("A.B")).isInstanceOf(MessageType::class.java)
    assertThat(pruned.getType("A.B.C")).isNull()
    assertThat(pruned.getType("A.D")).isNull()
  }

  @Test
  fun retainTypeRetainsFieldTypesTransitively() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |message MessageA {
            |  optional MessageB b = 1;
            |}
            |message MessageB {
            |  map<string, MessageC> c = 1;
            |}
            |message MessageC {
            |}
            |message MessageD {
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("MessageA")
        .build())
    assertThat(pruned.getType("MessageA")).isNotNull
    assertThat(pruned.getType("MessageB")).isNotNull
    assertThat(pruned.getType("MessageC")).isNotNull
    assertThat(pruned.getType("MessageD")).isNull()
  }

  @Test
  fun retainRpcRetainsRequestAndResponseTypes() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |message RequestA {
            |}
            |message ResponseA {
            |}
            |message RequestB {
            |}
            |message ResponseB {
            |}
            |service Service {
            |  rpc CallA (RequestA) returns (ResponseA);
            |  rpc CallB (RequestB) returns (ResponseB);
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("Service#CallA")
        .build())
    assertThat(pruned.getService("Service").rpc("CallA")).isNotNull
    assertThat(pruned.getType("RequestA")).isNotNull
    assertThat(pruned.getType("ResponseA")).isNotNull
    assertThat(pruned.getService("Service").rpc("CallB")).isNull()
    assertThat(pruned.getType("RequestB")).isNull()
    assertThat(pruned.getType("ResponseB")).isNull()
  }

  @Test
  fun retainField() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |message MessageA {
            |  optional string b = 1;
            |  map<string, string> c = 2;
            |}
            |message MessageB {
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("MessageA#b")
        .build())
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull
    assertThat((pruned.getType("MessageA") as MessageType).field("c")).isNull()
    assertThat(pruned.getType("MessageB")).isNull()
  }

  @Test
  fun retainFieldRetainsFieldTypesTransitively() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |message MessageA {
            |  optional MessageB b = 1;
            |  optional MessageD d = 2;
            |}
            |message MessageB {
            |  optional MessageC c = 1;
            |}
            |message MessageC {
            |}
            |message MessageD {
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("MessageA#b")
        .build())
    assertThat(pruned.getType("MessageA")).isNotNull
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull
    assertThat((pruned.getType("MessageA") as MessageType).field("d")).isNull()
    assertThat(pruned.getType("MessageB")).isNotNull
    assertThat(pruned.getType("MessageC")).isNotNull
    assertThat(pruned.getType("MessageD")).isNull()
  }

  @Test
  fun retainFieldPrunesOneOf() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |message Message {
            |  oneof selection {
            |    string a = 1;
            |    string b = 2;
            |  }
            |  optional string c = 3;
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("Message#c")
        .build())
    assertThat((pruned.getType("Message") as MessageType).oneOfs()).isEmpty()
  }

  @Test
  fun retainFieldRetainsOneOf() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |message Message {
            |  oneof selection {
            |    string a = 1;
            |    string b = 2;
            |  }
            |  optional string c = 3;
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("Message#b")
        .build())
    val message = pruned.getType("Message") as MessageType
    val onlyOneOf = getOnlyElement(message.oneOfs())
    assertThat(onlyOneOf.name()).isEqualTo("selection")
    assertThat(getOnlyElement(onlyOneOf.fields()).name()).isEqualTo("b")
    assertThat(message.field("a")).isNull()
    assertThat(message.field("c")).isNull()
  }

  @Test
  fun typeWithRetainedMembersOnlyHasThoseMembersRetained() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |message MessageA {
            |  optional MessageB b = 1;
            |}
            |message MessageB {
            |  optional MessageC c = 1;
            |  optional MessageD d = 2;
            |}
            |message MessageC {
            |}
            |message MessageD {
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("MessageA#b")
        .include("MessageB#c")
        .build())
    assertThat(pruned.getType("MessageA")).isNotNull
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull
    assertThat(pruned.getType("MessageB")).isNotNull
    assertThat((pruned.getType("MessageB") as MessageType).field("c")).isNotNull
    assertThat((pruned.getType("MessageB") as MessageType).field("d")).isNull()
    assertThat(pruned.getType("MessageC")).isNotNull
    assertThat(pruned.getType("MessageD")).isNull()
  }

  @Test
  fun retainEnumConstant() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |enum Roshambo {
            |  ROCK = 0;
            |  SCISSORS = 1;
            |  PAPER = 2;
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("Roshambo#SCISSORS")
        .build())
    assertThat((pruned.getType("Roshambo") as EnumType).constant("ROCK")).isNull()
    assertThat((pruned.getType("Roshambo") as EnumType).constant("SCISSORS")).isNotNull
    assertThat((pruned.getType("Roshambo") as EnumType).constant("PAPER")).isNull()
  }

  @Test
  fun enumWithRetainedConstantHasThatConstantRetained() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |message Message {
            |  optional Roshambo roshambo = 1;
            |}
            |enum Roshambo {
            |  ROCK = 0;
            |  SCISSORS = 1;
            |  PAPER = 2;
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("Message")
        .include("Roshambo#SCISSORS")
        .build())
    assertThat(pruned.getType("Message")).isNotNull
    assertThat((pruned.getType("Message") as MessageType).field("roshambo")).isNotNull
    assertThat(pruned.getType("Roshambo")).isNotNull
    assertThat((pruned.getType("Roshambo") as EnumType).constant("ROCK")).isNull()
    assertThat((pruned.getType("Roshambo") as EnumType).constant("SCISSORS")).isNotNull
    assertThat((pruned.getType("Roshambo") as EnumType).constant("PAPER")).isNull()
  }

  @Test
  fun retainedOptionRetainsOptionsType() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |import "google/protobuf/descriptor.proto";
            |extend google.protobuf.FieldOptions {
            |  optional string a = 22001;
            |}
            |message Message {
            |  optional string f = 1 [a = "a"];
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("Message#f")
        .build())
    assertThat((pruned.getType("Message") as MessageType).field("f")).isNotNull
    assertThat(pruned.getType("google.protobuf.FieldOptions") as MessageType).isNotNull
  }

  @Test
  fun prunedOptionDoesNotRetainOptionsType() {
    val schema = RepoBuilder()
        .add("service.proto", """
              |import "google/protobuf/descriptor.proto";
              |extend google.protobuf.FieldOptions {
              |  optional string a = 22001;
              |}
              |message Message {
              |  optional string f = 1 [a = "a"];
              |  optional string g = 2;
              |}
              """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("Message#g")
        .build())
    assertThat(pruned.getType("google.protobuf.FieldOptions") as MessageType?).isNull()
  }

  @Test
  fun optionRetainsField() {
    val schema = RepoBuilder()
        .add("service.proto", """
             |import "google/protobuf/descriptor.proto";
             |message SomeFieldOptions {
             |  optional string a = 1; // Retained via option use.
             |  optional string b = 2; // Retained explicitly.
             |  optional string c = 3; // Should be pruned.
             |}
             |extend google.protobuf.FieldOptions {
             |  optional SomeFieldOptions some_field_options = 22001;
             |}
             |message Message {
             |  optional string f = 1 [some_field_options.a = "a"];
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("Message")
        .include("SomeFieldOptions#b")
        .build())
    assertThat((pruned.getType("Message") as MessageType).field("f")).isNotNull
    assertThat((pruned.getType("SomeFieldOptions") as MessageType).field("a")).isNotNull
    assertThat((pruned.getType("SomeFieldOptions") as MessageType).field("b")).isNotNull
    assertThat((pruned.getType("SomeFieldOptions") as MessageType).field("c")).isNull()
  }

  @Test
  fun optionRetainsType() {
    val schema = RepoBuilder()
        .add("service.proto", """
             |import "google/protobuf/descriptor.proto";
             |message SomeFieldOptions {
             |  optional string a = 1; // Retained via option use.
             |  optional string b = 2; // Retained because 'a' is retained.
             |  optional string c = 3; // Retained because 'a' is retained.
             |}
             |extend google.protobuf.FieldOptions {
             |  optional SomeFieldOptions some_field_options = 22001;
             |}
             |message Message {
             |  optional string f = 1 [some_field_options.a = "a"];
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("Message")
        .build())
    assertThat((pruned.getType("Message") as MessageType).field("f")).isNotNull
    assertThat((pruned.getType("SomeFieldOptions") as MessageType).field("a")).isNotNull
    assertThat((pruned.getType("SomeFieldOptions") as MessageType).field("b")).isNotNull
    assertThat((pruned.getType("SomeFieldOptions") as MessageType).field("c")).isNotNull
  }

  @Test
  fun retainExtension() {
    val schema = RepoBuilder()
        .add("service.proto", """
             |message Message {
             |  optional string a = 1;
             |}
             |extend Message {
             |  optional string b = 2;
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("Message")
        .build())
    assertThat((pruned.getType("Message") as MessageType).field("a")).isNotNull
    assertThat((pruned.getType("Message") as MessageType).extensionField("b")).isNotNull
  }

  @Test
  fun retainExtensionMembers() {
    val schema = RepoBuilder()
        .add("service.proto", """
             |message Message {
             |  optional string a = 1;
             |  optional string b = 2;
             |}
             |extend Message {
             |  optional string c = 3;
             |  optional string d = 4;
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("Message#a")
        .include("Message#c")
        .build())
    assertThat((pruned.getType("Message") as MessageType).field("a")).isNotNull
    assertThat((pruned.getType("Message") as MessageType).field("b")).isNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("c")).isNotNull
    assertThat((pruned.getType("Message") as MessageType).extensionField("d")).isNull()
  }

  /** When we include excludes only, the mark phase is skipped.  */
  @Test
  fun excludeWithoutInclude() {
    val schema = RepoBuilder()
        .add("service.proto", """
             |message MessageA {
             |  optional string b = 1;
             |  optional string c = 2;
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .exclude("MessageA#c")
        .build())
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull
    assertThat((pruned.getType("MessageA") as MessageType).field("c")).isNull()
  }

  @Test
  fun excludeField() {
    val schema = RepoBuilder()
        .add("service.proto", """
             |message MessageA {
             |  optional string b = 1;
             |  optional string c = 2;
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("MessageA")
        .exclude("MessageA#c")
        .build())
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull
    assertThat((pruned.getType("MessageA") as MessageType).field("c")).isNull()
  }

  @Test
  fun excludeTypeExcludesField() {
    val schema = RepoBuilder()
        .add("service.proto", """
             |message MessageA {
             |  optional MessageB b = 1;
             |  map<string, MessageC> c = 2;
             |}
             |message MessageB {
             |}
             |message MessageC {
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("MessageA")
        .exclude("MessageC")
        .build())
    assertThat(pruned.getType("MessageB")).isNotNull
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull
    assertThat(pruned.getType("MessageC")).isNull()
    assertThat((pruned.getType("MessageA") as MessageType).field("c")).isNull()
  }

  @Test
  fun excludeTypeExcludesRpc() {
    val schema = RepoBuilder()
        .add("service.proto", """
             |service ServiceA {
             |  rpc CallB (MessageB) returns (MessageB);
             |  rpc CallC (MessageC) returns (MessageC);
             |}
             |message MessageB {
             |}
             |message MessageC {
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("ServiceA")
        .exclude("MessageC")
        .build())
    assertThat(pruned.getType("MessageB")).isNotNull
    assertThat(pruned.getService("ServiceA").rpc("CallB")).isNotNull
    assertThat(pruned.getType("MessageC")).isNull()
    assertThat(pruned.getService("ServiceA").rpc("CallC")).isNull()
  }

  @Test
  fun excludeRpcExcludesTypes() {
    val schema = RepoBuilder()
        .add("service.proto", """
             |service ServiceA {
             |  rpc CallB (MessageB) returns (MessageB);
             |  rpc CallC (MessageC) returns (MessageC);
             |}
             |message MessageB {
             |}
             |message MessageC {
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("ServiceA")
        .exclude("ServiceA#CallC")
        .build())
    assertThat(pruned.getType("MessageB")).isNotNull
    assertThat(pruned.getService("ServiceA").rpc("CallB")).isNotNull
    assertThat(pruned.getType("MessageC")).isNull()
    assertThat(pruned.getService("ServiceA").rpc("CallC")).isNull()
  }

  @Test
  fun excludeFieldExcludesTypes() {
    val schema = RepoBuilder()
        .add("message.proto", """
             |message MessageA {
             |  optional MessageB b = 1;
             |  optional MessageC c = 2;
             |  map<string, MessageD> d = 3;
             |}
             |message MessageB {
             |}
             |message MessageC {
             |}
             |message MessageD {
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("MessageA")
        .exclude("MessageA#c")
        .exclude("MessageA#d")
        .build())
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull
    assertThat(pruned.getType("MessageB")).isNotNull
    assertThat((pruned.getType("MessageA") as MessageType).field("c")).isNull()
    assertThat(pruned.getType("MessageC")).isNull()
    assertThat((pruned.getType("MessageA") as MessageType).field("d")).isNull()
    assertThat(pruned.getType("MessageD")).isNull()
  }

  @Test
  fun excludeEnumExcludesOptions() {
    val schema = RepoBuilder()
        .add("message.proto", """
              |import "google/protobuf/descriptor.proto";
              |enum Enum {
              |  A = 0;
              |  B = 1  [message.c = 1];
              |}
              |extend google.protobuf.EnumValueOptions {
              |  optional Message message = 70000;
              |};
              |message Message {
              |  optional int32 c = 1;
              |}
              """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("Enum")
        .exclude("Enum#B")
        .build())
    assertThat((pruned.getType("Enum") as EnumType).constant("A")).isNotNull
    assertThat((pruned.getType("Enum") as EnumType).constant("B")).isNull()
    assertThat(pruned.getType("Message")).isNull()
  }

  @Test
  fun excludedFieldPrunesTopLevelOption() {
    val schema = RepoBuilder()
        .add("service.proto", """
              |import "google/protobuf/descriptor.proto";
              |extend google.protobuf.FieldOptions {
              |  optional string a = 22001;
              |  optional string b = 22002;
              |}
              |message Message {
              |  optional string f = 1 [a = "a", b = "b"];
              |}
              """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .exclude("google.protobuf.FieldOptions#b")
        .build())
    val field = (pruned.getType("Message") as MessageType).field("f")!!
    assertThat(field.options().get(ProtoMember.get(FIELD_OPTIONS, "a"))).isEqualTo("a")
    assertThat(field.options().get(ProtoMember.get(FIELD_OPTIONS, "b"))).isNull()
  }

  @Test
  fun excludedTypePrunesTopLevelOption() {
    val schema = RepoBuilder()
        .add("service.proto", """
              |import "google/protobuf/descriptor.proto";
              |message SomeFieldOptions {
              |  optional string a = 1;
              |}
              |extend google.protobuf.FieldOptions {
              |  optional SomeFieldOptions some_field_options = 22001;
              |  optional string b = 22002;
              |}
              |message Message {
              |  optional string f = 1 [some_field_options.a = "a", b = "b"];
              |}
              """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .exclude("SomeFieldOptions")
        .build())
    val field = (pruned.getType("Message") as MessageType).field("f")
    val map = field!!.options().map()
    val onlyOption = getOnlyElement(map.entries)
    assertThat((onlyOption.key as ProtoMember).member()).isEqualTo("b")
    assertThat(onlyOption.value).isEqualTo("b")
  }

  @Test
  fun excludedFieldPrunesNestedOption() {
    val schema = RepoBuilder()
        .add("service.proto", """
             |import "google/protobuf/descriptor.proto";
             |message SomeFieldOptions {
             |  optional string a = 1;
             |  optional string b = 2;
             |}
             |extend google.protobuf.FieldOptions {
             |  optional SomeFieldOptions some_field_options = 22001;
             |}
             |message Message {
             |  optional string f = 1 [some_field_options = { a: "a", b: "b" }];
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .exclude("SomeFieldOptions#b")
        .build())
    val field = (pruned.getType("Message") as MessageType).field("f")
    val map = field!!.options().get(
        ProtoMember.get(FIELD_OPTIONS, "some_field_options")) as Map<*, *>
    val onlyOption = getOnlyElement(map.entries)
    assertThat((onlyOption.key as ProtoMember).member()).isEqualTo("a")
    assertThat(onlyOption.value).isEqualTo("a")
  }

  @Test
  fun excludedTypePrunesNestedOption() {
    val schema = RepoBuilder()
        .add("service.proto", """
             |import "google/protobuf/descriptor.proto";
             |message SomeFieldOptions {
             |  optional Dimensions dimensions = 1;
             |}
             |message Dimensions {
             |  optional string length = 1;
             |  optional string width = 2;
             |}
             |extend google.protobuf.FieldOptions {
             |  optional SomeFieldOptions some_field_options = 22001;
             |  optional string b = 22002;
             |}
             |message Message {
             |  optional string f = 1 [
             |      some_field_options = {
             |          dimensions: { length: "100" }
             |      },
             |      b = "b"
             |  ];
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .exclude("Dimensions")
        .build())
    val field = (pruned.getType("Message") as MessageType).field("f")
    val map = field!!.options().map()
    val onlyOption = getOnlyElement(map.entries)
    assertThat((onlyOption.key as ProtoMember).member()).isEqualTo("b")
    assertThat(onlyOption.value).isEqualTo("b")
  }

  @Test
  fun excludeOptions() {
    val schema = RepoBuilder()
        .add("service.proto", """
             |import "google/protobuf/descriptor.proto";
             |extend google.protobuf.FieldOptions {
             |  optional string a = 22001;
             |  optional string b = 22002;
             |}
             |message Message {
             |  optional string f = 1 [ a = "a", b = "b" ];
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .exclude("google.protobuf.FieldOptions")
        .build())
    val field = (pruned.getType("Message") as MessageType).field("f")
    assertThat(field!!.options().map()).isEmpty()
  }

  @Test
  fun excludeRepeatedOptions() {
    val schema = RepoBuilder()
        .add("service.proto", """
             |import "google/protobuf/descriptor.proto";
             |extend google.protobuf.MessageOptions {
             |  repeated string a = 22001;
             |  repeated string b = 22002;
             |}
             |message Message {
             |  option (a) = "a1";
             |  option (a) = "a2";
             |  option (b) = "b1";
             |  option (b) = "b2";
             |  optional string f = 1;
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .exclude("google.protobuf.MessageOptions#a")
        .build())
    val message = pruned.getType("Message") as MessageType
    assertThat(message.options().get(ProtoMember.get(MESSAGE_OPTIONS, "a"))).isNull()
    assertThat(message.options().get(ProtoMember.get(MESSAGE_OPTIONS, "b")))
        .isEqualTo(ImmutableList.of("b1", "b2"))
  }

  @Test
  fun includePackage() {
    val schema = RepoBuilder()
        .add("a/b/messages.proto", """
             |package a.b;
             |message MessageAB {
             |}
             """.trimMargin()
        )
        .add("a/c/messages.proto", """
             |package a.c;
             |message MessageAC {
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .include("a.b.*")
        .build())
    assertThat(pruned.getType("a.b.MessageAB")).isNotNull
    assertThat(pruned.getType("a.c.MessageAC")).isNull()
  }

  @Test
  fun excludePackage() {
    val schema = RepoBuilder()
        .add("a/b/messages.proto", """
             |package a.b;
             |message MessageAB {
             |}
             """.trimMargin()
        )
        .add("a/c/messages.proto", """
             |package a.c;
             |message MessageAC {
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .exclude("a.c.*")
        .build())
    assertThat(pruned.getType("a.b.MessageAB")).isNotNull
    assertThat(pruned.getType("a.c.MessageAC")).isNull()
  }

  @Test
  fun specialOptionsNotPruned() {
    val schema = RepoBuilder()
        .add("message.proto", """
             |option java_package = "p";
             |
             |message Message {
             |  optional int32 a = 1 [packed = true, deprecated = true, default = 5];
             |}
             |enum Enum {
             |  option allow_alias = true;
             |  A = 1;
             |  B = 1;
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(IdentifierSet.Builder()
        .exclude("google.protobuf.*")
        .build())
    val protoFile = pruned.protoFile("message.proto")
    assertThat(protoFile!!.javaPackage()).isEqualTo("p")

    val message = pruned.getType("Message") as MessageType
    val field = message.field("a")
    assertThat(field!!.default).isEqualTo("5")
    assertThat(field.isDeprecated).isTrue()
    assertThat(field.isPacked).isTrue()

    val enumType = pruned.getType("Enum") as EnumType
    assertThat(enumType.allowAlias()).isTrue()
  }
}
