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
@file:Suppress("UsePropertyAccessSyntax")

package com.squareup.wire.schema

import com.squareup.wire.schema.Options.Companion.FIELD_OPTIONS
import com.squareup.wire.schema.Options.Companion.MESSAGE_OPTIONS
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("MessageA")
        .build())
    assertThat(pruned.getType("MessageA")).isNotNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("MessageA")
        .build())
    assertThat(pruned.getType("MessageA")).isNotNull()
    assertThat(pruned.getField(ProtoMember.get("MessageA#maps"))).isNotNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("MessageA")
        .prune("MessageA#maps")
        .build())
    assertThat(pruned.getType("MessageA")).isNotNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("A.B")
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("MessageA")
        .build())
    assertThat(pruned.getType("MessageA")).isNotNull()
    assertThat(pruned.getType("MessageB")).isNotNull()
    assertThat(pruned.getType("MessageC")).isNotNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Service#CallA")
        .build())
    assertThat(pruned.getService("Service")!!.rpc("CallA")).isNotNull()
    assertThat(pruned.getType("RequestA")).isNotNull()
    assertThat(pruned.getType("ResponseA")).isNotNull()
    assertThat(pruned.getService("Service")!!.rpc("CallB")).isNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("MessageA#b")
        .build())
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("MessageA#b")
        .build())
    assertThat(pruned.getType("MessageA")).isNotNull()
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull()
    assertThat((pruned.getType("MessageA") as MessageType).field("d")).isNull()
    assertThat(pruned.getType("MessageB")).isNotNull()
    assertThat(pruned.getType("MessageC")).isNotNull()
    assertThat(pruned.getType("MessageD")).isNull()
  }

  @Test
  fun oneOf() {
    val schema = RepoBuilder()
        .add("one_of_message.proto", """
             |package oneof;
             |
             |message OneOfMessage {
             |  oneof choice {
             |    int32 foo = 1;
             |    string bar = 3;
             |    string baz = 4;
             |  }
             |}
             """.trimMargin())
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("oneof.OneOfMessage")
        .build())
    val oneOfs = (pruned.getType("oneof.OneOfMessage") as MessageType).oneOfs
    assertThat(oneOfs).isNotEmpty()
    assertThat(oneOfs.first().fields).hasSize(3)
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Message#c")
        .build())
    assertThat((pruned.getType("Message") as MessageType).oneOfs).isEmpty()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Message#b")
        .build())
    val message = pruned.getType("Message") as MessageType
    val onlyOneOf = message.oneOfs.single()
    assertThat(onlyOneOf.name).isEqualTo("selection")
    assertThat(onlyOneOf.fields.single().name).isEqualTo("b")
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("MessageA#b")
        .addRoot("MessageB#c")
        .build())
    assertThat(pruned.getType("MessageA")).isNotNull()
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull()
    assertThat(pruned.getType("MessageB")).isNotNull()
    assertThat((pruned.getType("MessageB") as MessageType).field("c")).isNotNull()
    assertThat((pruned.getType("MessageB") as MessageType).field("d")).isNull()
    assertThat(pruned.getType("MessageC")).isNotNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Roshambo#SCISSORS")
        .build())
    assertThat((pruned.getType("Roshambo") as EnumType).constant("ROCK")).isNull()
    assertThat((pruned.getType("Roshambo") as EnumType).constant("SCISSORS")).isNotNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Message")
        .addRoot("Roshambo#SCISSORS")
        .build())
    assertThat(pruned.getType("Message")).isNotNull()
    assertThat((pruned.getType("Message") as MessageType).field("roshambo")).isNotNull()
    assertThat(pruned.getType("Roshambo")).isNotNull()
    assertThat((pruned.getType("Roshambo") as EnumType).constant("ROCK")).isNull()
    assertThat((pruned.getType("Roshambo") as EnumType).constant("SCISSORS")).isNotNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Message#f")
        .build())
    assertThat((pruned.getType("Message") as MessageType).field("f")).isNotNull()
    assertThat(pruned.getType("google.protobuf.FieldOptions") as MessageType).isNotNull()
  }

  @Test
  fun prunedExtensionOptionDoesNotRetainExtension() {
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Message#g")
        .build())

    val fieldOptions = pruned.getType("google.protobuf.FieldOptions") as MessageType
    assertThat(fieldOptions.extensionField("a")).isNull()

    val service = pruned.protoFile("service.proto")!!
    assertThat(service.extendList).isEmpty()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Message")
        .addRoot("SomeFieldOptions#b")
        .build())
    assertThat((pruned.getType("Message") as MessageType).field("f")).isNotNull()
    assertThat((pruned.getType("SomeFieldOptions") as MessageType).field("a")).isNotNull()
    assertThat((pruned.getType("SomeFieldOptions") as MessageType).field("b")).isNotNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Message")
        .build())
    assertThat((pruned.getType("Message") as MessageType).field("f")).isNotNull()
    assertThat((pruned.getType("SomeFieldOptions") as MessageType).field("a")).isNotNull()
    assertThat((pruned.getType("SomeFieldOptions") as MessageType).field("b")).isNotNull() // TODO
    assertThat((pruned.getType("SomeFieldOptions") as MessageType).field("c")).isNotNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Message")
        .build())
    assertThat((pruned.getType("Message") as MessageType).field("a")).isNotNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("b")).isNotNull()
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
             |  repeated string e = 5;
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Message#a")
        .addRoot("Message#c")
        .build())
    assertThat((pruned.getType("Message") as MessageType).field("a")).isNotNull()
    assertThat((pruned.getType("Message") as MessageType).field("b")).isNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("c")).isNotNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("d")).isNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("e")).isNull()
  }

  @Test
  fun retainingTypeRetainsExtensionMembers() {
    val schema = RepoBuilder()
        .add("service.proto", """
             |message Message {
             |  optional string a = 1;
             |  optional string b = 2;
             |}
             |extend Message {
             |  optional string c = 3;
             |  optional string d = 4;
             |  repeated string e = 5;
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Message")
        .build())
    assertThat((pruned.getType("Message") as MessageType).field("a")).isNotNull()
    assertThat((pruned.getType("Message") as MessageType).field("b")).isNotNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("c")).isNotNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("d")).isNotNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("e")).isNotNull()
  }

  @Test
  fun includeExtensionMemberPrunesPeerMembers() {
    val schema = RepoBuilder()
        .add("service.proto", """
             |message Message {
             |  optional string a = 1;
             |  optional string b = 2;
             |}
             |extend Message {
             |  optional string c = 3;
             |  optional string d = 4;
             |  repeated string e = 5;
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Message#c")
        .build())
    assertThat((pruned.getType("Message") as MessageType).field("a")).isNull()
    assertThat((pruned.getType("Message") as MessageType).field("b")).isNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("c")).isNotNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("d")).isNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("e")).isNull()
  }

  @Test
  fun namespacedExtensionFieldsAreRetained() {
    val schema = RepoBuilder()
        .add("message.proto", """
             |package squareup;
             |
             |message ExternalMessage {
             |  extensions 100 to 200;
             |
             |  optional float f = 1;
             |}
             |
             |message Message {
             |  optional string a = 1;
             |  optional ExternalMessage external_message = 2;
             |}
             |
             |extend ExternalMessage {
             |  repeated int32 extension_field = 121;
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("squareup.Message")
        .build())
    val message = pruned.getType("squareup.Message") as MessageType
    assertThat(message.field("a")).isNotNull()
    assertThat(message.field("external_message")).isNotNull()
    val externalMessage = pruned.getType("squareup.ExternalMessage") as MessageType
    assertThat(externalMessage.field("f")).isNotNull()
    assertThat(externalMessage.extensionField("squareup.extension_field")).isNotNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .prune("MessageA#c")
        .build())
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("MessageA")
        .prune("MessageA#c")
        .build())
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("MessageA")
        .prune("MessageC")
        .build())
    assertThat(pruned.getType("MessageB")).isNotNull()
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("ServiceA")
        .prune("MessageC")
        .build())
    assertThat(pruned.getType("MessageB")).isNotNull()
    assertThat(pruned.getService("ServiceA")!!.rpc("CallB")).isNotNull()
    assertThat(pruned.getType("MessageC")).isNull()
    assertThat(pruned.getService("ServiceA")!!.rpc("CallC")).isNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("ServiceA")
        .prune("ServiceA#CallC")
        .build())
    assertThat(pruned.getType("MessageB")).isNotNull()
    assertThat(pruned.getService("ServiceA")!!.rpc("CallB")).isNotNull()
    assertThat(pruned.getType("MessageC")).isNull()
    assertThat(pruned.getService("ServiceA")!!.rpc("CallC")).isNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("MessageA")
        .prune("MessageA#c")
        .prune("MessageA#d")
        .build())
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull()
    assertThat(pruned.getType("MessageB")).isNotNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Enum")
        .prune("Enum#B")
        .build())
    assertThat((pruned.getType("Enum") as EnumType).constant("A")).isNotNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .prune("google.protobuf.FieldOptions#b")
        .build())
    val field = (pruned.getType("Message") as MessageType).field("f")!!
    assertThat(field.options.get(ProtoMember.get(FIELD_OPTIONS, "a"))).isEqualTo("a")
    assertThat(field.options.get(ProtoMember.get(FIELD_OPTIONS, "b"))).isNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .prune("SomeFieldOptions")
        .build())
    val field = (pruned.getType("Message") as MessageType).field("f")
    val map = field!!.options.map
    val onlyOption = map.entries.single()
    assertThat(onlyOption.key.member).isEqualTo("b")
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
    val pruned = schema.prune(PruningRules.Builder()
        .prune("SomeFieldOptions#b")
        .build())
    val field = (pruned.getType("Message") as MessageType).field("f")
    val map = field!!.options.get(
        ProtoMember.get(FIELD_OPTIONS, "some_field_options")) as Map<*, *>
    val onlyOption = map.entries.single()
    assertThat((onlyOption.key as ProtoMember).member).isEqualTo("a")
    assertThat(onlyOption.value).isEqualTo("a")
  }

  @Test
  fun prunedFieldDocumentationsGetPruned() {
    val schema = RepoBuilder()
        .add("period.proto", """
             |enum Period {
             |  /* This is A. */
             |  A = 1;
             |
             |  /* This is reserved. */
             |  reserved 2;
             |
             |  /* This is C. */
             |  C = 3;
             |}
             """.trimMargin())
        .schema()

    val pruned = schema.prune(PruningRules.Builder().build())

    val type = pruned.getType("Period") as EnumType
    assertThat(type.constant("A")!!.documentation).isEqualTo("This is A.")
    assertThat(type.constant("C")!!.documentation).isEqualTo("This is C.")
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
    val pruned = schema.prune(PruningRules.Builder()
        .prune("Dimensions")
        .build())
    val field = (pruned.getType("Message") as MessageType).field("f")
    val map = field!!.options.map
    val onlyOption = map.entries.single()
    assertThat(onlyOption.key.member).isEqualTo("b")
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
    val pruned = schema.prune(PruningRules.Builder()
        .prune("google.protobuf.FieldOptions")
        .build())
    val field = (pruned.getType("Message") as MessageType).field("f")
    assertThat(field!!.options.map).isEmpty()
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
    val pruned = schema.prune(PruningRules.Builder()
        .prune("google.protobuf.MessageOptions#a")
        .build())
    val message = pruned.getType("Message") as MessageType
    assertThat(message.options.get(ProtoMember.get(MESSAGE_OPTIONS, "a"))).isNull()
    assertThat(message.options.get(ProtoMember.get(MESSAGE_OPTIONS, "b")))
        .isEqualTo(listOf("b1", "b2"))
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
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("a.b.*")
        .build())
    assertThat(pruned.getType("a.b.MessageAB")).isNotNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .prune("a.c.*")
        .build())
    assertThat(pruned.getType("a.b.MessageAB")).isNotNull()
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
    val pruned = schema.prune(PruningRules.Builder()
        .prune("google.protobuf.*")
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

  @Test
  fun excludeUnusedImports() {
    val schema = RepoBuilder()
        .add("message.proto", """
             |import 'footer.proto';
             |import 'title.proto';
             |
             |message Message {
             |  optional Title title = 1;
             |}
             """.trimMargin()
        )
        .add("title.proto", """
             |message Title {
             |  optional string label = 1;
             |}
             """.trimMargin()
        )
        .add("footer.proto", """
             |message Footer {
             |  optional string label = 1;
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Message")
        .build())

    assertThat(pruned.protoFile("footer.proto")!!.types).isEmpty()
    assertThat(pruned.protoFile("title.proto")!!.types).isNotEmpty()

    val message = pruned.protoFile("message.proto")!!
    assertThat(message.imports).containsExactly("title.proto")
  }

  @Test
  fun enumsAreKeptsIfUsed() {
    val schema = RepoBuilder()
        .add("currency_code.proto", """
             |import "google/protobuf/descriptor.proto";
             |
             |enum RoundingMode {
             |  PLAIN = 0;
             |  DOWN = 1;
             |  UP = 2;
             |  BANKERS = 3;
             |  DOWN_ON_HALF = 4;
             |}
             |
             |extend google.protobuf.EnumValueOptions {
             |  optional int32 cash_rounding = 54000;
             |  optional RoundingMode rounding_mode = 54002;
             |}
             |
             |enum CurrencyCode {
             |  AFN = 971;
             |  ANG = 532;
             |  NZD = 554 [(cash_rounding) = 10, (rounding_mode) = DOWN_ON_HALF];
             |}
             |
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("CurrencyCode")
        .build())

    assertThat(pruned.getType("RoundingMode")).isNotNull()
  }

  /**
   * When an extension field is reached via an option, consider only that option to be reachable.
   * Do not recursively mark the other extensions.
   */
  @Test
  fun markingExtensionFieldDoesNotMarkPeerFields() {
    val schema = RepoBuilder()
        .add("message.proto", """
             |import "google/protobuf/descriptor.proto";
             |
             |extend google.protobuf.FieldOptions {
             |  optional string a = 54000;
             |  optional string b = 54001;
             |}
             |
             |message Message {
             |  optional string s = 1 [a = "a"];
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Message")
        .build())

    val fieldOptions = pruned.getType("google.protobuf.FieldOptions") as MessageType
    assertThat(fieldOptions.extensionField("a")).isNotNull()
    assertThat(fieldOptions.extensionField("b")).isNull()
  }

  /**
   * When a message type is reached via an option, consider the entire type to be reachable.
   * Recursively mark the message's other fields.
   */
  @Test
  fun markingNonExtensionFieldDoesMarkPeerFields() {
    val schema = RepoBuilder()
        .add("message.proto", """
             |import "google/protobuf/descriptor.proto";
             |
             |extend google.protobuf.FieldOptions {
             |  optional MessageOption message_option = 54000;
             |}
             |
             |message MessageOption {
             |  optional string a = 1;
             |  optional string b = 2;
             |}
             |
             |message Message {
             |  optional string s = 1 [message_option.a = "a"];
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Message")
        .build())

    val messageOption = pruned.getType("MessageOption") as MessageType
    assertThat(messageOption.field("a")).isNotNull()
    assertThat(messageOption.field("b")).isNotNull()
  }

  /**
   * If we're pruning some members of a message type, reaching a different member via an option is
   * not sufficient to cause the entire message to be reachable.
   */
  @Test
  fun markingNonExtensionFieldDoesMarkPeerFieldsIfTypesMembersAreBeingPruned() {
    val schema = RepoBuilder()
        .add("message.proto", """
             |import "google/protobuf/descriptor.proto";
             |
             |extend google.protobuf.FieldOptions {
             |  optional MessageOption message_option = 54000;
             |}
             |
             |message MessageOption {
             |  optional string a = 1;
             |  optional string b = 2;
             |  optional string c = 3;
             |}
             |
             |message Message {
             |  optional string s = 1 [message_option.a = "a"];
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Message")
        .addRoot("MessageOption#c")
        .build())

    val messageOption = pruned.getType("MessageOption") as MessageType
    assertThat(messageOption.field("a")).isNotNull()
    assertThat(messageOption.field("b")).isNull()
    assertThat(messageOption.field("c")).isNotNull()
  }

  @Test
  fun includingFieldDoesNotIncludePeerFields() {
    val schema = RepoBuilder()
        .add("message.proto", """
             |message Message {
             |  optional string a = 1;
             |  optional string b = 2;
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("Message#a")
        .build())

    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("a")).isNotNull()
    assertThat(message.field("b")).isNull()
  }

  @Test
  fun excludingGoogleProtobufPrunesAllOptionsOnEnums() {
    val schema = RepoBuilder()
        .add("currency_code.proto", """
             |package squareup;
             |
             |import "google/protobuf/descriptor.proto";
             |
             |message MessageOption {
             |  optional string a = 1;
             |  optional string b = 2;
             |  optional string c = 3;
             |}
             |
             |enum Style {
             |  VERBOSE = 0;
             |  POETIC = 1;
             |  RUDE = 2;
             |}
             |
             |extend google.protobuf.EnumValueOptions {
             |  optional int32 max_length = 54000;
             |  optional MessageOption message_option = 54001;
             |  optional Style style = 54002;
             |}
             |
             |enum Author {
             |  ZEUS = 1 [(style) = POETIC, (max_length) = 12];
             |  ARTEMIS = 2;
             |  APOLLO = 3 [(style) = RUDE, (message_option) = {a: "some", c: "things"}];
             |  POSEIDON = 4 [deprecated = true];
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("squareup.Author")
        .prune("google.protobuf.*")
        .build())

    assertThat(pruned.getType("squareup.Style")).isNull()
    assertThat(pruned.getType("squareup.MessageOption")).isNull()
    assertThat(pruned.getType("google.protobuf.EnumValueOptions")).isNotNull()

    val authorType = pruned.getType("squareup.Author") as EnumType
    assertThat(authorType.constant("ZEUS")!!.options.fields().isEmpty()).isTrue()
    assertThat(authorType.constant("ARTEMIS")!!.options.fields().isEmpty()).isTrue()
    assertThat(authorType.constant("APOLLO")!!.options.fields().isEmpty()).isTrue()
    // Options defined in google.protobuf.descriptor.proto are not pruned.
    assertThat(authorType.constant("POSEIDON")!!.options.fields().values()).hasSize(1)
  }

  @Test
  fun excludingGoogleProtobufPrunesAllOptionsOnMessages() {
    val schema = RepoBuilder()
        .add("currency_code.proto", """
             |package squareup;
             |
             |import "google/protobuf/descriptor.proto";
             |
             |message MessageOption {
             |  optional string a = 1;
             |  optional string b = 2;
             |  optional string c = 3;
             |}
             |
             |enum Style {
             |  VERBOSE = 0;
             |  POETIC = 1;
             |  RUDE = 2;
             |}
             |
             |extend google.protobuf.FieldOptions {
             |  optional int32 max_length = 54000;
             |  optional MessageOption message_option = 54001;
             |  optional Style style = 54002;
             |}
             |
             |message Letter {
             |  optional string header = 1 [(max_length) = 20];
             |  optional bool add_margin = 2 [deprecated = true];
             |  optional string author = 3 [(style) = RUDE, (message_option) = {a: "some", c: "things"}];
             |  optional string signature = 4 [default = "Sent from Wire"];
             |}
             """.trimMargin()
        )
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("squareup.Letter")
        .prune("google.protobuf.*")
        .build())

    assertThat(pruned.getType("squareup.Style")).isNull()
    assertThat(pruned.getType("squareup.MessageOption")).isNull()

    val enumValueOptions = pruned.getType("google.protobuf.FieldOptions") as MessageType
    assertThat(enumValueOptions.field("deprecated")).isNotNull()

    val letterType = pruned.getType("squareup.Letter") as MessageType
    assertThat(letterType.field("header")!!.options.fields().isEmpty()).isTrue()

    // Options defined in google.protobuf.descriptor.proto are not pruned.
    assertThat(letterType.field("add_margin")!!.options.fields().values()).hasSize(1)
    assertThat(letterType.field("add_margin")!!.isDeprecated).isTrue()

    assertThat(letterType.field("author")!!.options.fields().isEmpty()).isTrue()

    // Default are not options.
    assertThat(letterType.field("signature")!!.default).isEqualTo("Sent from Wire")
    assertThat(letterType.field("signature")!!.options.fields().isEmpty()).isTrue()
  }

  @Test
  fun sinceAndUntilRetainOlder() {
    val schema = RepoBuilder()
        .add("message.proto", """
            |import "wire/extensions.proto";
            |
            |message Message {
            |  optional string radio = 1 [(wire.until) = "1950"];
            |  optional string video = 2 [(wire.since) = "1950"];
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .since("1949")
        .until("1950")
        .build())
    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("radio")).isNotNull()
    assertThat(message.field("video")).isNull()
  }

  @Test
  fun sinceAndUntilRetainNewer() {
    val schema = RepoBuilder()
        .add("message.proto", """
            |import "wire/extensions.proto";
            |
            |message Message {
            |  optional string radio = 1 [(wire.until) = "1950"];
            |  optional string video = 2 [(wire.since) = "1950"];
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .since("1950")
        .until("1951")
        .build())
    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("radio")).isNull()
    assertThat(message.field("video")).isNotNull()
  }

  @Test
  fun sinceRetainedWhenLessThanOrEqualToUntil() {
    val schema = RepoBuilder()
        .add("message.proto", """
            |import "wire/extensions.proto";
            |
            |message Message {
            |  optional string since_19 = 1 [(wire.since) = "19"];
            |  optional string since_20 = 2 [(wire.since) = "20"];
            |  optional string since_21 = 3 [(wire.since) = "21"];
            |  
            |  optional string since_29 = 4 [(wire.since) = "29"];
            |  optional string since_30 = 5 [(wire.since) = "30"];
            |  optional string since_31 = 6 [(wire.since) = "31"];
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .since("20")
        .until("30")
        .build())
    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("since_19")).isNotNull()
    assertThat(message.field("since_20")).isNotNull()
    assertThat(message.field("since_21")).isNotNull()

    assertThat(message.field("since_29")).isNotNull()
    assertThat(message.field("since_30")).isNull()
    assertThat(message.field("since_31")).isNull()
  }

  @Test
  fun untilRetainedWhenGreaterThanSince() {
    val schema = RepoBuilder()
        .add("message.proto", """
            |import "wire/extensions.proto";
            |
            |message Message {
            |  optional string until_19 = 1 [(wire.until) = "19"];
            |  optional string until_20 = 2 [(wire.until) = "20"];
            |  optional string until_21 = 3 [(wire.until) = "21"];
            |  
            |  optional string until_29 = 4 [(wire.until) = "29"];
            |  optional string until_30 = 5 [(wire.until) = "30"];
            |  optional string until_31 = 6 [(wire.until) = "31"];
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .since("20")
        .until("30")
        .build())
    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("until_19")).isNull()
    assertThat(message.field("until_20")).isNull()
    assertThat(message.field("until_21")).isNotNull()
    assertThat(message.field("until_29")).isNotNull()
    assertThat(message.field("until_30")).isNotNull()
    assertThat(message.field("until_31")).isNotNull()
  }

  @Test
  fun sinceAndUntilDoNothingWithoutVersionPruning() {
    val schema = RepoBuilder()
        .add("message.proto", """
            |import "wire/extensions.proto";
            |
            |message Message {
            |  optional string until_20 = 1 [(wire.until) = "20"];
            |  optional string since_20 = 2 [(wire.since) = "20"];
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(PruningRules.Builder().build())
    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("since_20")).isNotNull()
    assertThat(message.field("until_20")).isNotNull()
  }

  @Test
  fun versionPruningDoesNotImpactFieldsWithoutSinceAndUntil() {
    val schema = RepoBuilder()
        .add("message.proto", """
            |import "wire/extensions.proto";
            |
            |message Message {
            |  optional string always = 1;
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .since("20")
        .until("30")
        .build())
    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("always")).isNotNull()
  }

  @Test
  fun sinceUntilOnEnumConstant() {
    val schema = RepoBuilder()
        .add("roshambo.proto", """
            |import "wire/extensions.proto";
            |
            |enum Roshambo {
            |  ROCK = 1 [(wire.until) = "29"];
            |  SCISSORS = 2 [(wire.since) = "30"];
            |  PAPER = 3 [(wire.since) = "29"];
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .since("29")
        .until("30")
        .build())
    val enum = pruned.getType("Roshambo") as EnumType
    assertThat(enum.constant("ROCK")).isNull()
    assertThat(enum.constant("SCISSORS")).isNull()
    assertThat(enum.constant("PAPER")).isNotNull()
  }

  @Test
  fun semVer() {
    val schema = RepoBuilder()
        .add("message.proto", """
            |import "wire/extensions.proto";
            |
            |message Message {
            |  optional string field_1 = 1 [(wire.until) = "1.0.0-alpha"];
            |  optional string field_2 = 2 [(wire.until) = "1.0.0-alpha.1"];
            |  optional string field_3 = 3 [(wire.until) = "1.0.0-alpha.beta"];
            |  optional string field_4 = 4 [(wire.since) = "1.0.0-beta"];
            |  optional string field_5 = 5 [(wire.since) = "1.0.0"];
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .since("1.0.0-alpha.1")
        .until("1.0.0-beta")
        .build())
    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("field_1")).isNull()
    assertThat(message.field("field_2")).isNull()
    assertThat(message.field("field_3")).isNotNull()
    assertThat(message.field("field_4")).isNull()
    assertThat(message.field("field_5")).isNull()
  }

  @Test
  fun typeIsRetainedIfMorePreciseRuleExists() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |package wire;
            |
            |message MessageA {
            |}
            |message MessageB {
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("wire.MessageA")
        .prune("wire.*")
        .build())
    assertThat(pruned.getType("wire.MessageA")).isNotNull()
    assertThat(pruned.getType("wire.MessageB")).isNull()
  }

  @Test
  fun fieldIsRetainedIfMorePreciseRuleExists() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |message MyMessage {
            |  optional string a = 1;
            |  optional string b = 2;
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("MyMessage#a")
        .prune("MyMessage")
        .build())
    assertThat(pruned.getType("MyMessage")).isNotNull()
    val myMessageType = pruned.getType("MyMessage") as MessageType
    assertThat(myMessageType.field("a")).isNotNull()
    assertThat(myMessageType.field("b")).isNull()
  }

  @Test
  fun enumConstantIsRetainedIfMorePreciseRuleExists() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |enum MyEnum {
            |  A = 1;
            |  B = 2;
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("MyEnum#A")
        .prune("MyEnum")
        .build())
    assertThat(pruned.getType("MyEnum")).isNotNull()
    val myEnumType = pruned.getType("MyEnum") as EnumType
    assertThat(myEnumType.constant("A")).isNotNull()
    assertThat(myEnumType.constant("B")).isNull()
  }

  @Test
  fun optionFieldIsRetainedIfMorePreciseRuleExists() {
    val schema = RepoBuilder()
        .add("lecture.proto", """
             |package wire;
             |
             |import "google/protobuf/descriptor.proto";
             |
             |extend google.protobuf.FieldOptions {
             |  optional bool relevant = 22301;
             |  optional bool irrelevant = 22302;
             |  optional bool unused = 22303;
             |}
             |
             |message Lecture {
             |  optional string title = 1 [(wire.relevant) = true];
             |  optional string content = 2 [(wire.irrelevant) = true];
             |}
             """.trimMargin())
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("wire.Lecture")
        .addRoot("google.protobuf.FieldOptions#wire.relevant")
        .prune("google.protobuf.*")
        .build())
    val fieldOptions = pruned.getType("google.protobuf.FieldOptions") as MessageType
    assertThat(fieldOptions.extensionField("wire.relevant")).isNotNull()
    assertThat(fieldOptions.extensionField("wire.unused")).isNull()
    assertThat(fieldOptions.extensionField("wire.irrelevant")).isNull()

    val messageType = pruned.getType("wire.Lecture") as MessageType
    assertThat(messageType.field("title")).isNotNull()
    assertThat(messageType.field("title")!!.options.elements).hasSize(1)
    assertThat(messageType.field("content")).isNotNull()
    assertThat(messageType.field("content")!!.options.elements).isEmpty()
  }

  @Test
  fun nestedInclusion() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |message MyMessage {
            |  optional string a = 1;
            |  optional MyEnum b = 2;
            |}
            |
            |enum MyEnum {
            |  C = 1;
            |  D = 2;
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .prune("MyEnum#D")
        .addRoot("MyMessage#b")
        .prune("MyMessage")
        .build())
    assertThat(pruned.getType("MyMessage")).isNotNull()
    assertThat(pruned.getType("MyEnum")).isNotNull()
    val myMessageType = pruned.getType("MyMessage") as MessageType
    assertThat(myMessageType.field("a")).isNull()
    assertThat(myMessageType.field("b")).isNotNull()
    val myEnumType = pruned.getType("MyEnum") as EnumType
    assertThat(myEnumType.constant("C")).isNotNull()
    assertThat(myEnumType.constant("D")).isNull()
  }

  @Test
  fun includeMemberOfExcludedType() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |message MessageA {
            |  optional string a = 1;
            |  optional Book book = 2;
            |}
            |
            |message MessageB {
            |  optional string a = 1;
            |  optional Book book = 2;
            |}
            |
            |message Book {
            |  optional string title = 1;
            |}
            """.trimMargin())
        .schema()
    val pruned = schema.prune(PruningRules.Builder()
        .addRoot("MessageA#book")
        .addRoot("MessageB")
        .prune("Book")
        .prune("Stuff")
        .build())

    val messageA = pruned.getType("MessageA") as MessageType
    assertThat(messageA.field("a")).isNull()
    // Book is excluded but the member is included so we keep it.
    assertThat(messageA.field("book")).isNotNull()

    val messageB = pruned.getType("MessageB") as MessageType
    assertThat(messageB.field("a")).isNotNull()
    // Book is excluded and MessageB#book isn't included so the field should be gone.
    assertThat(messageB.field("book")).isNull()

    // Book is excluded but because MessageA#book is included, we keep the type.
    val bookType = pruned.getType("Book") as MessageType
    assertThat(bookType.field("title")).isNotNull()
  }
}
