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
@file:Suppress("UsePropertyAccessSyntax")

package com.squareup.wire.schema

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.message
import com.squareup.wire.buildSchema
import com.squareup.wire.schema.Options.Companion.FIELD_OPTIONS
import com.squareup.wire.schema.Options.Companion.MESSAGE_OPTIONS
import kotlin.test.Ignore
import kotlin.test.Test
import okio.Path.Companion.toPath

class PrunerTest {
  @Test
  fun retainType() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
             |message MessageA {
             |}
             |message MessageB {
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("MessageA")
        .build(),
    )
    assertThat(pruned.getType("MessageA")).isNotNull()
    assertThat(pruned.getType("MessageB")).isNull()
  }

  /**
   * We test that all references of an opaque type are getting pruned correctly.
   */
  @Test
  fun opaqueTypePrunesItsReferences() {
    val schema = buildSchema {
      add(
        "source-path/cafe/cafe.proto".toPath(),
        """
          |syntax = "proto2";
          |
          |package cafe;
          |
          |message CafeDrink {
          |  optional int32 size_ounces = 1;
          |  repeated EspressoShot shots = 2;
          |}
          |
          |message EspressoShot {
          |  optional Roast roast = 1;
          |  optional bool decaf = 2;
          |}
          |
          |enum Roast {
          |  MEDIUM = 1;
          |  DARK = 2;
          |}
        """.trimMargin(),
      )
      addOpaqueTypes(ProtoType.get("cafe.EspressoShot"))
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("cafe.CafeDrink")
        .build(),
    )
    assertThat(pruned.getType("cafe.CafeDrink")).isNotNull()
    // The field should not be pruned, and is of the opaque type `bytes`.
    assertThat(pruned.getField("cafe.CafeDrink", "shots")!!.type)
      .isEqualTo(ProtoType.BYTES)
    // Types which were originally referred by `shots` are now pruned since the field is opaqued.
    assertThat(pruned.getType("cafe.EspressoShot")).isNull()
    assertThat(pruned.getType("cafe.Roast")).isNull()
  }

  @Test
  fun oneOfOptionsAreNotArbitrarilyPruned() {
    val schema = buildSchema {
      add(
        "test_event.proto".toPath(),
        """
          |syntax = "proto3";
          |
          |import "test_event_custom_option.proto";
          |
          |package test.oneOf.options.test;
          |
          |message TestMessage {
          |  oneof element {
          |    option (my_custom_oneOf_option) = true;
          |    string one = 1;
          |    string two = 2;
          |  }
          |}
        """.trimMargin(),
      )
      add(
        "test_event_custom_option.proto".toPath(),
        """
          |syntax = "proto3";
          |
          |import "google/protobuf/descriptor.proto";
          |
          |package test.oneOf.options;
          |
          |extend google.protobuf.OneofOptions {
          |  bool my_custom_oneOf_option = 101400;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("test.oneOf.options.test.TestMessage")
        .build(),
    )
    assertThat(pruned.protoFile("test_event.proto")!!.toSchema())
      .isEqualTo(
        """
          |// Proto schema formatted by Wire, do not edit.
          |// Source: test_event.proto
          |
          |syntax = "proto3";
          |
          |package test.oneOf.options.test;
          |
          |import "test_event_custom_option.proto";
          |
          |message TestMessage {
          |  oneof element {
          |    option (my_custom_oneOf_option) = true;
          | $space
          |    string one = 1;
          |    string two = 2;
          |  }
          |}
          |
        """.trimMargin(),
      )
  }

  @Test
  fun rootCanHandleInlinedOptionWithMapFields() {
    val schema = buildSchema {
      add(
        "test.proto".toPath(),
        """
          |syntax = "proto3";
          |
          |import "google/protobuf/descriptor.proto";
          |
          |package wire.issue;
          |
          |message Options {
          |  map<string, ConfigPayload> config = 1;
          |  map<string, SettingPayload> setting = 2;
          |  map<string, string> extra = 3;
          |}
          |
          |message ConfigPayload {
          |  optional string data = 1;
          |}
          |
          |message SettingPayload {
          |  optional string data = 1;
          |}
          |
          |extend google.protobuf.MessageOptions {
          |  repeated Options opt = 80000;
          |}
          |
          |message SomeMessage {
          |  option (wire.issue.opt) = {
          |    config: [
          |      {
          |        key: "some_config_key_1",
          |        value: { data: "some_config_data_1" }
          |      },
          |    ],
          |    setting: [
          |      {
          |        key: "some_setting_key_1",
          |        value: { data: "some_setting_data_1" }
          |      },
          |      {
          |        key: "some_setting_key_2",
          |        value: { data: "some_setting_data_2" }
          |      },
          |    ],
          |    extra: [
          |      {
          |        key: "some_extra_key_1",
          |        value: "some_extra_data_1"
          |      },
          |    ],
          |  };
          |
          |  string id = 1;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("wire.issue.Options#config")
        .build(),
    )
    assertThat(pruned.protoFile("test.proto")!!.toSchema())
      .isEqualTo(
        """
          |// Proto schema formatted by Wire, do not edit.
          |// Source: test.proto
          |
          |syntax = "proto3";
          |
          |package wire.issue;
          |
          |message Options {
          |  map<string, ConfigPayload> config = 1;
          |}
          |
          |message ConfigPayload {
          |  optional string data = 1;
          |}
          |
        """.trimMargin(),
      )
  }

  @Ignore("Pruning inlined map options is not supported")
  @Test
  fun pruneCanHandleInlinedOptionMemberWithMapFields() {
    val schema = buildSchema {
      add(
        "test.proto".toPath(),
        """
          |syntax = "proto3";
          |
          |import "google/protobuf/descriptor.proto";
          |
          |package wire.issue;
          |
          |message Options {
          |  map<string, ConfigPayload> config = 1;
          |  map<string, SettingPayload> setting = 2;
          |  map<string, string> extra = 3;
          |}
          |
          |message ConfigPayload {
          |  optional string data = 1;
          |}
          |
          |message SettingPayload {
          |  optional string data = 1;
          |}
          |
          |extend google.protobuf.MessageOptions {
          |  repeated Options opt = 80000;
          |}
          |
          |message SomeMessage {
          |  option (wire.issue.opt) = {
          |    config: [
          |      {
          |        key: "some_config_key_1",
          |        value: { data: "some_config_data_1" }
          |      },
          |    ],
          |    setting: [
          |      {
          |        key: "some_setting_key_1",
          |        value: { data: "some_setting_data_1" }
          |      },
          |      {
          |        key: "some_setting_key_2",
          |        value: { data: "some_setting_data_2" }
          |      },
          |    ],
          |    extra: [
          |      {
          |        key: "some_extra_key_1",
          |        value: "some_extra_data_1"
          |      },
          |    ],
          |  };
          |
          |  string id = 1;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .prune("wire.issue.Options#config")
        .build(),
    )
    assertThat(pruned.protoFile("test.proto")!!.toSchema())
      .isEqualTo(
        """
         |// Proto schema formatted by Wire, do not edit.
         |// Source: test.proto
         |
         |syntax = "proto3";
         |
         |package wire.issue;
         |
         |import "google/protobuf/descriptor.proto";
         |
         |message Options {
         |  map<string, SettingPayload> setting = 2;
         |
         |  map<string, string> extra = 3;
         |}
         |
         |message SettingPayload {
         |  optional string data = 1;
         |}
         |
         |message SomeMessage {
         |  option (wire.issue.opt) = {
         |    setting: [
         |      {
         |        key: "some_setting_key_1",
         |        value: {
         |          data: "some_setting_data_1"
         |        }
         |      },
         |      {
         |        key: "some_setting_key_2",
         |        value: {
         |          data: "some_setting_data_2"
         |        }
         |      }
         |    ],
         |    extra: [
         |      {
         |        key: "some_extra_key_1",
         |        value: "some_extra_data_1"
         |      }
         |    ]
         |  };
         |
         |  string id = 1;
         |}
         |
         |extend google.protobuf.MessageOptions {
         |  repeated Options opt = 80000;
         |}
          |
        """.trimMargin(),
      )
  }

  @Ignore("Pruning inlined map options is not supported")
  @Test
  fun pruneCanHandleInlinedOptionTypeWithMapFields() {
    val schema = buildSchema {
      add(
        "test.proto".toPath(),
        """
          |syntax = "proto3";
          |
          |import "google/protobuf/descriptor.proto";
          |
          |package wire.issue;
          |
          |message Options {
          |  map<string, ConfigPayload> config = 1;
          |  map<string, SettingPayload> setting = 2;
          |  map<string, string> extra = 3;
          |}
          |
          |message ConfigPayload {
          |  optional string data = 1;
          |}
          |
          |message SettingPayload {
          |  optional string data = 1;
          |}
          |
          |extend google.protobuf.MessageOptions {
          |  repeated Options opt = 80000;
          |}
          |
          |message SomeMessage {
          |  option (wire.issue.opt) = {
          |    config: [
          |      {
          |        key: "some_config_key_1",
          |        value: { data: "some_config_data_1" }
          |      },
          |    ],
          |    setting: [
          |      {
          |        key: "some_setting_key_1",
          |        value: { data: "some_setting_data_1" }
          |      },
          |      {
          |        key: "some_setting_key_2",
          |        value: { data: "some_setting_data_2" }
          |      },
          |    ],
          |    extra: [
          |      {
          |        key: "some_extra_key_1",
          |        value: "some_extra_data_1"
          |      },
          |    ],
          |  };
          |
          |  string id = 1;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .prune("wire.issue.ConfigPayload")
        .build(),
    )
    assertThat(pruned.protoFile("test.proto")!!.toSchema())
      .isEqualTo(
        """
         |// Proto schema formatted by Wire, do not edit.
         |// Source: test.proto
         |
         |syntax = "proto3";
         |
         |package wire.issue;
         |
         |import "google/protobuf/descriptor.proto";
         |
         |message Options {
         |  map<string, SettingPayload> setting = 2;
         |
         |  map<string, string> extra = 3;
         |}
         |
         |message SettingPayload {
         |  optional string data = 1;
         |}
         |
         |message SomeMessage {
         |  option (wire.issue.opt) = {
         |    setting: [
         |      {
         |        key: "some_setting_key_1",
         |        value: {
         |          data: "some_setting_data_1"
         |        }
         |      },
         |      {
         |        key: "some_setting_key_2",
         |        value: {
         |          data: "some_setting_data_2"
         |        }
         |      }
         |    ],
         |    extra: [
         |      {
         |        key: "some_extra_key_1",
         |        value: "some_extra_data_1"
         |      }
         |    ]
         |  };
         |
         |  string id = 1;
         |}
         |
         |extend google.protobuf.MessageOptions {
         |  repeated Options opt = 80000;
         |}
          |
        """.trimMargin(),
      )
  }

  @Test
  fun oneOfOptionsArePruned() {
    val schema = buildSchema {
      add(
        "test_event.proto".toPath(),
        """
          |syntax = "proto3";
          |
          |import "test_event_custom_option.proto";
          |
          |package test.oneOf.options.test;
          |
          |message TestMessage {
          |  oneof element {
          |    option (my_custom_oneOf_option) = true;
          |    string one = 1;
          |    string two = 2;
          |  }
          |}
        """.trimMargin(),
      )
      add(
        "test_event_custom_option.proto".toPath(),
        """
          |syntax = "proto3";
          |
          |import "google/protobuf/descriptor.proto";
          |
          |package test.oneOf.options;
          |
          |extend google.protobuf.OneofOptions {
          |  bool my_custom_oneOf_option = 101400;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .prune("google.protobuf.OneofOptions#test.oneOf.options.my_custom_oneOf_option")
        .build(),
    )
    assertThat(pruned.protoFile("test_event.proto")!!.toSchema())
      .isEqualTo(
        """
          |// Proto schema formatted by Wire, do not edit.
          |// Source: test_event.proto
          |
          |syntax = "proto3";
          |
          |package test.oneOf.options.test;
          |
          |message TestMessage {
          |  oneof element {
          |    string one = 1;
          |    string two = 2;
          |  }
          |}
          |
        """.trimMargin(),
      )
  }

  @Test
  fun retainMap() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
            |message MessageA {
            |  map<string, MessageB> maps = 1;
            |  message MessageB {
            |  }
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("MessageA")
        .build(),
    )
    assertThat(pruned.getType("MessageA")).isNotNull()
    assertThat(pruned.getField(ProtoMember.get("MessageA#maps"))).isNotNull()
  }

  @Test
  fun excludeMap() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
            |message MessageA {
            |  map<string, MessageB> maps = 1;
            |  message MessageB {
            |  }
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("MessageA")
        .prune("MessageA#maps")
        .build(),
    )
    assertThat(pruned.getType("MessageA")).isNotNull()
    assertThat(pruned.getField(ProtoMember.get("MessageA#maps"))).isNull()
  }

  @Test
  fun retainTypeRetainsEnclosingButNotNested() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
            |message A {
            |  message B {
            |    message C {
            |    }
            |  }
            |  message D {
            |  }
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("A.B")
        .build(),
    )
    assertThat(pruned.getType("A")!!).isInstanceOf<EnclosingType>()
    assertThat(pruned.getType("A.B")!!).isInstanceOf<MessageType>()
    assertThat(pruned.getType("A.B.C")).isNull()
    assertThat(pruned.getType("A.D")).isNull()
  }

  @Test
  fun retainTypeRetainsFieldTypesTransitively() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("MessageA")
        .build(),
    )
    assertThat(pruned.getType("MessageA")).isNotNull()
    assertThat(pruned.getType("MessageB")).isNotNull()
    assertThat(pruned.getType("MessageC")).isNotNull()
    assertThat(pruned.getType("MessageD")).isNull()
  }

  @Test
  fun retainRpcRetainsRequestAndResponseTypes() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Service#CallA")
        .build(),
    )
    assertThat(pruned.getService("Service")!!.rpc("CallA")).isNotNull()
    assertThat(pruned.getType("RequestA")).isNotNull()
    assertThat(pruned.getType("ResponseA")).isNotNull()
    assertThat(pruned.getService("Service")!!.rpc("CallB")).isNull()
    assertThat(pruned.getType("RequestB")).isNull()
    assertThat(pruned.getType("ResponseB")).isNull()
  }

  @Test
  fun retainField() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
            |message MessageA {
            |  optional string b = 1;
            |  map<string, string> c = 2;
            |}
            |message MessageB {
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("MessageA#b")
        .build(),
    )
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull()
    assertThat((pruned.getType("MessageA") as MessageType).field("c")).isNull()
    assertThat(pruned.getType("MessageB")).isNull()
  }

  @Test
  fun retainFieldRetainsFieldTypesTransitively() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("MessageA#b")
        .build(),
    )
    assertThat(pruned.getType("MessageA")).isNotNull()
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull()
    assertThat((pruned.getType("MessageA") as MessageType).field("d")).isNull()
    assertThat(pruned.getType("MessageB")).isNotNull()
    assertThat(pruned.getType("MessageC")).isNotNull()
    assertThat(pruned.getType("MessageD")).isNull()
  }

  @Test
  fun oneOf() {
    val schema = buildSchema {
      add(
        "one_of_message.proto".toPath(),
        """
             |package oneof;
             |
             |message OneOfMessage {
             |  oneof choice {
             |    int32 foo = 1;
             |    string bar = 3;
             |    string baz = 4;
             |  }
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("oneof.OneOfMessage")
        .build(),
    )
    val oneOfs = (pruned.getType("oneof.OneOfMessage") as MessageType).oneOfs
    assertThat(oneOfs).isNotEmpty()
    assertThat(oneOfs.first().fields).hasSize(3)
  }

  @Test
  fun retainFieldPrunesOneOf() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
            |message Message {
            |  oneof selection {
            |    string a = 1;
            |    string b = 2;
            |  }
            |  optional string c = 3;
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message#c")
        .build(),
    )
    assertThat((pruned.getType("Message") as MessageType).oneOfs).isEmpty()
  }

  @Test
  fun retainFieldRetainsOneOf() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
            |message Message {
            |  oneof selection {
            |    string a = 1;
            |    string b = 2;
            |  }
            |  optional string c = 3;
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message#b")
        .build(),
    )
    val message = pruned.getType("Message") as MessageType
    val onlyOneOf = message.oneOfs.single()
    assertThat(onlyOneOf.name).isEqualTo("selection")
    assertThat(onlyOneOf.fields.single().name).isEqualTo("b")
    assertThat(message.field("a")).isNull()
    assertThat(message.field("c")).isNull()
  }

  @Test
  fun typeWithRetainedMembersOnlyHasThoseMembersRetained() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("MessageA#b")
        .addRoot("MessageB#c")
        .build(),
    )
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
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
            |enum Roshambo {
            |  ROCK = 0;
            |  SCISSORS = 1;
            |  PAPER = 2;
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Roshambo#SCISSORS")
        .build(),
    )
    assertThat((pruned.getType("Roshambo") as EnumType).constant("ROCK")).isNull()
    assertThat((pruned.getType("Roshambo") as EnumType).constant("SCISSORS")).isNotNull()
    assertThat((pruned.getType("Roshambo") as EnumType).constant("PAPER")).isNull()
  }

  @Test
  fun enumWithRetainedConstantHasThatConstantRetained() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
            |message Message {
            |  optional Roshambo roshambo = 1;
            |}
            |enum Roshambo {
            |  ROCK = 0;
            |  SCISSORS = 1;
            |  PAPER = 2;
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .addRoot("Roshambo#SCISSORS")
        .build(),
    )
    assertThat(pruned.getType("Message")).isNotNull()
    assertThat((pruned.getType("Message") as MessageType).field("roshambo")).isNotNull()
    assertThat(pruned.getType("Roshambo")).isNotNull()
    assertThat((pruned.getType("Roshambo") as EnumType).constant("ROCK")).isNull()
    assertThat((pruned.getType("Roshambo") as EnumType).constant("SCISSORS")).isNotNull()
    assertThat((pruned.getType("Roshambo") as EnumType).constant("PAPER")).isNull()
  }

  @Test
  fun retainedOptionRetainsOptionsType() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
            |import "google/protobuf/descriptor.proto";
            |extend google.protobuf.FieldOptions {
            |  optional string a = 22001;
            |}
            |message Message {
            |  optional string f = 1 [a = "a"];
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message#f")
        .build(),
    )
    assertThat((pruned.getType("Message") as MessageType).field("f")).isNotNull()
    assertThat(pruned.getType("google.protobuf.FieldOptions") as MessageType).isNotNull()
  }

  @Test
  fun prunedExtensionOptionDoesNotRetainExtension() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
              |import "google/protobuf/descriptor.proto";
              |extend google.protobuf.FieldOptions {
              |  optional string a = 22001;
              |}
              |message Message {
              |  optional string f = 1 [a = "a"];
              |  optional string g = 2;
              |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message#g")
        .build(),
    )

    val fieldOptions = pruned.getType("google.protobuf.FieldOptions") as MessageType
    assertThat(fieldOptions.extensionField("a")).isNull()

    val service = pruned.protoFile("service.proto")!!
    assertThat(service.extendList).isEmpty()
  }

  @Test
  fun optionRetainsField() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .addRoot("SomeFieldOptions#b")
        .build(),
    )
    assertThat((pruned.getType("Message") as MessageType).field("f")).isNotNull()
    assertThat((pruned.getType("SomeFieldOptions") as MessageType).field("a")).isNotNull()
    assertThat((pruned.getType("SomeFieldOptions") as MessageType).field("b")).isNotNull()
    assertThat((pruned.getType("SomeFieldOptions") as MessageType).field("c")).isNull()
  }

  @Test
  fun optionRetainsType() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .build(),
    )
    assertThat((pruned.getType("Message") as MessageType).field("f")).isNotNull()
    assertThat((pruned.getType("SomeFieldOptions") as MessageType).field("a")).isNotNull()
    assertThat((pruned.getType("SomeFieldOptions") as MessageType).field("b")).isNotNull() // TODO
    assertThat((pruned.getType("SomeFieldOptions") as MessageType).field("c")).isNotNull()
  }

  @Test
  fun retainExtension() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
             |message Message {
             |  optional string a = 1;
             |}
             |extend Message {
             |  optional string b = 2;
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .build(),
    )
    assertThat((pruned.getType("Message") as MessageType).field("a")).isNotNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("b")).isNotNull()
  }

  @Test
  fun retainExtensionMembers() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
             |message Message {
             |  optional string a = 1;
             |  optional string b = 2;
             |}
             |extend Message {
             |  optional string c = 3;
             |  optional string d = 4;
             |  repeated string e = 5;
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message#a")
        .addRoot("Message#c")
        .build(),
    )
    assertThat((pruned.getType("Message") as MessageType).field("a")).isNotNull()
    assertThat((pruned.getType("Message") as MessageType).field("b")).isNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("c")).isNotNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("d")).isNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("e")).isNull()
  }

  @Test
  fun retainingTypeRetainsExtensionMembers() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
             |message Message {
             |  optional string a = 1;
             |  optional string b = 2;
             |}
             |extend Message {
             |  optional string c = 3;
             |  optional string d = 4;
             |  repeated string e = 5;
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .build(),
    )
    assertThat((pruned.getType("Message") as MessageType).field("a")).isNotNull()
    assertThat((pruned.getType("Message") as MessageType).field("b")).isNotNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("c")).isNotNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("d")).isNotNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("e")).isNotNull()
  }

  @Test
  fun includeExtensionMemberPrunesPeerMembers() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
             |message Message {
             |  optional string a = 1;
             |  optional string b = 2;
             |}
             |extend Message {
             |  optional string c = 3;
             |  optional string d = 4;
             |  repeated string e = 5;
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message#c")
        .build(),
    )
    assertThat((pruned.getType("Message") as MessageType).field("a")).isNull()
    assertThat((pruned.getType("Message") as MessageType).field("b")).isNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("c")).isNotNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("d")).isNull()
    assertThat((pruned.getType("Message") as MessageType).extensionField("e")).isNull()
  }

  @Test
  fun namespacedExtensionFieldsAreRetained() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("squareup.Message")
        .build(),
    )
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
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
             |message MessageA {
             |  optional string b = 1;
             |  optional string c = 2;
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .prune("MessageA#c")
        .build(),
    )
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull()
    assertThat((pruned.getType("MessageA") as MessageType).field("c")).isNull()
  }

  @Test
  fun excludeField() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
             |message MessageA {
             |  optional string b = 1;
             |  optional string c = 2;
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("MessageA")
        .prune("MessageA#c")
        .build(),
    )
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull()
    assertThat((pruned.getType("MessageA") as MessageType).field("c")).isNull()
  }

  @Test
  fun excludeTypeExcludesField() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
             |message MessageA {
             |  optional MessageB b = 1;
             |  map<string, MessageC> c = 2;
             |}
             |message MessageB {
             |}
             |message MessageC {
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("MessageA")
        .prune("MessageC")
        .build(),
    )
    assertThat(pruned.getType("MessageB")).isNotNull()
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull()
    assertThat(pruned.getType("MessageC")).isNull()
    assertThat((pruned.getType("MessageA") as MessageType).field("c")).isNull()
  }

  @Test
  fun excludeTypeExcludesRpc() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
             |service ServiceA {
             |  rpc CallB (MessageB) returns (MessageB);
             |  rpc CallC (MessageC) returns (MessageC);
             |}
             |message MessageB {
             |}
             |message MessageC {
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("ServiceA")
        .prune("MessageC")
        .build(),
    )
    assertThat(pruned.getType("MessageB")).isNotNull()
    assertThat(pruned.getService("ServiceA")!!.rpc("CallB")).isNotNull()
    assertThat(pruned.getType("MessageC")).isNull()
    assertThat(pruned.getService("ServiceA")!!.rpc("CallC")).isNull()
  }

  @Test
  fun excludeRpcExcludesTypes() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
             |service ServiceA {
             |  rpc CallB (MessageB) returns (MessageB);
             |  rpc CallC (MessageC) returns (MessageC);
             |}
             |message MessageB {
             |}
             |message MessageC {
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("ServiceA")
        .prune("ServiceA#CallC")
        .build(),
    )
    assertThat(pruned.getType("MessageB")).isNotNull()
    assertThat(pruned.getService("ServiceA")!!.rpc("CallB")).isNotNull()
    assertThat(pruned.getType("MessageC")).isNull()
    assertThat(pruned.getService("ServiceA")!!.rpc("CallC")).isNull()
  }

  @Test
  fun excludeFieldExcludesTypes() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("MessageA")
        .prune("MessageA#c")
        .prune("MessageA#d")
        .build(),
    )
    assertThat((pruned.getType("MessageA") as MessageType).field("b")).isNotNull()
    assertThat(pruned.getType("MessageB")).isNotNull()
    assertThat((pruned.getType("MessageA") as MessageType).field("c")).isNull()
    assertThat(pruned.getType("MessageC")).isNull()
    assertThat((pruned.getType("MessageA") as MessageType).field("d")).isNull()
    assertThat(pruned.getType("MessageD")).isNull()
  }

  @Test
  fun excludeEnumExcludesOptions() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Enum")
        .prune("Enum#B")
        .build(),
    )
    assertThat((pruned.getType("Enum") as EnumType).constant("A")).isNotNull()
    assertThat((pruned.getType("Enum") as EnumType).constant("B")).isNull()
    assertThat(pruned.getType("Message")).isNull()
  }

  @Test
  fun excludedFieldPrunesTopLevelOption() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
              |import "google/protobuf/descriptor.proto";
              |extend google.protobuf.FieldOptions {
              |  optional string a = 22001;
              |  optional string b = 22002;
              |}
              |message Message {
              |  optional string f = 1 [a = "a", b = "b"];
              |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .prune("google.protobuf.FieldOptions#b")
        .build(),
    )
    val field = (pruned.getType("Message") as MessageType).field("f")!!
    assertThat(field.options.get(ProtoMember.get(FIELD_OPTIONS, "a"))).isEqualTo("a")
    assertThat(field.options.get(ProtoMember.get(FIELD_OPTIONS, "b"))).isNull()
  }

  @Test
  fun excludedTypePrunesTopLevelOption() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .prune("SomeFieldOptions")
        .build(),
    )
    val field = (pruned.getType("Message") as MessageType).field("f")
    val map = field!!.options.map
    val onlyOption = map.entries.single()
    assertThat(onlyOption.key.member).isEqualTo("b")
    assertThat(onlyOption.value).isEqualTo("b")
  }

  @Test
  fun excludedFieldPrunesNestedOption() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .prune("SomeFieldOptions#b")
        .build(),
    )
    val field = (pruned.getType("Message") as MessageType).field("f")
    val map = field!!.options.get(
      ProtoMember.get(FIELD_OPTIONS, "some_field_options"),
    ) as Map<*, *>
    val onlyOption = map.entries.single()
    assertThat((onlyOption.key as ProtoMember).member).isEqualTo("a")
    assertThat(onlyOption.value).isEqualTo("a")
  }

  @Test
  fun prunedFieldDocumentationsGetPruned() {
    val schema = buildSchema {
      add(
        "period.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }

    val pruned = schema.prune(PruningRules.Builder().build())

    val type = pruned.getType("Period") as EnumType
    assertThat(type.constant("A")!!.documentation).isEqualTo("This is A.")
    assertThat(type.constant("C")!!.documentation).isEqualTo("This is C.")
  }

  @Test
  fun excludedTypePrunesNestedOption() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .prune("Dimensions")
        .build(),
    )
    val field = (pruned.getType("Message") as MessageType).field("f")
    val map = field!!.options.map
    val onlyOption = map.entries.single()
    assertThat(onlyOption.key.member).isEqualTo("b")
    assertThat(onlyOption.value).isEqualTo("b")
  }

  @Test
  fun excludeOptions() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
             |import "google/protobuf/descriptor.proto";
             |extend google.protobuf.FieldOptions {
             |  optional string a = 22001;
             |  optional string b = 22002;
             |}
             |message Message {
             |  optional string f = 1 [ a = "a", b = "b" ];
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .prune("google.protobuf.FieldOptions")
        .build(),
    )
    val field = (pruned.getType("Message") as MessageType).field("f")
    assertThat(field!!.options.map).isEmpty()
  }

  @Test
  fun excludeOneOfOptions() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
             |syntax = "proto3";
             |import "google/protobuf/descriptor.proto";
             |extend google.protobuf.OneofOptions {
             |  string my_oneof_option = 22101;
             |}
             |message Message {
             |  oneof choice {
             |    option (my_oneof_option) = "Well done";
             |
             |    string one = 1;
             |    string two = 2;
             |  }
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .prune("google.protobuf.OneofOptions")
        .build(),
    )
    val oneOf = (pruned.getType("Message") as MessageType).oneOfs[0]
    assertThat(oneOf.options.map).isEmpty()
  }

  @Test
  fun excludeRepeatedOptions() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .prune("google.protobuf.MessageOptions#a")
        .build(),
    )
    val message = pruned.getType("Message") as MessageType
    assertThat(message.options.get(ProtoMember.get(MESSAGE_OPTIONS, "a"))).isNull()
    assertThat(message.options.get(ProtoMember.get(MESSAGE_OPTIONS, "b")))
      .isEqualTo(listOf("b1", "b2"))
  }

  @Test
  fun includePackage() {
    val schema = buildSchema {
      add(
        "a/b/messages.proto".toPath(),
        """
             |package a.b;
             |message MessageAB {
             |}
        """.trimMargin(),
      )
      add(
        "a/c/messages.proto".toPath(),
        """
             |package a.c;
             |message MessageAC {
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("a.b.*")
        .build(),
    )
    assertThat(pruned.getType("a.b.MessageAB")).isNotNull()
    assertThat(pruned.getType("a.c.MessageAC")).isNull()
  }

  @Test
  fun excludePackage() {
    val schema = buildSchema {
      add(
        "a/b/messages.proto".toPath(),
        """
             |package a.b;
             |message MessageAB {
             |}
        """.trimMargin(),
      )
      add(
        "a/c/messages.proto".toPath(),
        """
             |package a.c;
             |message MessageAC {
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .prune("a.c.*")
        .build(),
    )
    assertThat(pruned.getType("a.b.MessageAB")).isNotNull()
    assertThat(pruned.getType("a.c.MessageAC")).isNull()
  }

  @Test
  fun specialOptionsNotPruned() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
             |option java_package = "p";
             |
             |message Message {
             |  optional int32 a = 1 [deprecated = true, default = 5];
             |  repeated int32 b = 2 [packed = true, deprecated = true];
             |}
             |enum Enum {
             |  option allow_alias = true;
             |  option deprecated = true;
             |  A = 1;
             |  B = 1;
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .prune("google.protobuf.*")
        .build(),
    )
    val protoFile = pruned.protoFile("message.proto")
    assertThat(protoFile!!.javaPackage()).isEqualTo("p")

    val message = pruned.getType("Message") as MessageType
    val fieldA = message.field("a")!!
    assertThat(fieldA.default).isEqualTo("5")
    assertThat(fieldA.isDeprecated).isTrue()
    val fieldB = message.field("b")!!
    assertThat(fieldB.isDeprecated).isTrue()
    assertThat(fieldB.isPacked).isTrue()

    val enumType = pruned.getType("Enum") as EnumType
    assertThat(enumType.allowAlias()).isTrue()
    assertThat(enumType.isDeprecated).isTrue()
  }

  @Test
  fun excludeUnusedImports() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
             |import 'footer.proto';
             |import 'title.proto';
             |
             |message Message {
             |  optional Title title = 1;
             |}
        """.trimMargin(),
      )
      add(
        "title.proto".toPath(),
        """
             |message Title {
             |  optional string label = 1;
             |}
        """.trimMargin(),
      )
      add(
        "footer.proto".toPath(),
        """
             |message Footer {
             |  optional string label = 1;
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .build(),
    )

    assertThat(pruned.protoFile("footer.proto")!!.types).isEmpty()
    assertThat(pruned.protoFile("title.proto")!!.types).isNotEmpty()

    val message = pruned.protoFile("message.proto")!!
    assertThat(message.imports).containsExactly("title.proto")
  }

  @Test
  fun excludeUnusedPublicImports() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
             |import public 'footer.proto';
             |import public 'title.proto';
             |
             |message Message {
             |}
        """.trimMargin(),
      )
      add(
        "title.proto".toPath(),
        """
             |message Title {
             |  optional string label = 1;
             |}
        """.trimMargin(),
      )
      add(
        "footer.proto".toPath(),
        """
             |message Footer {
             |  optional string label = 1;
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .addRoot("Title")
        .build(),
    )

    assertThat(pruned.protoFile("footer.proto")!!.types).isEmpty()
    assertThat(pruned.protoFile("title.proto")!!.types).isNotEmpty()

    val message = pruned.protoFile("message.proto")!!
    assertThat(message.publicImports).containsExactly("title.proto")
  }

  /**
   * We had a bug in import pruning where we retained imports if the files were non-empty,
   * even if those imports were unnecessary.
   */
  @Test
  fun importPruningIsPrecise() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
             |import 'footer.proto';
             |
             |message Message {
             |  optional string label = 1;
             |}
             |
             |message AnotherMessage {
             |  optional Footer footer = 1;
             |}
        """.trimMargin(),
      )
      add(
        "footer.proto".toPath(),
        """
             |message Footer {
             |  optional string label = 1;
             |}
             |
             |message Shoe {
             |  optional string label = 1;
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .addRoot("Shoe")
        .build(),
    )

    assertThat(pruned.protoFile("footer.proto")!!.types).isNotEmpty()

    val message = pruned.protoFile("message.proto")!!
    assertThat(message.imports).isEmpty()
  }

  @Test
  fun retainImportWhenUsedForMessageField() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
          |import 'title.proto';
          |import 'footer.proto';
          |
          |message Message {
          |  optional Title title = 1;
          |}
          |
          |message AnotherMessage {
          |  optional Footer footer = 1;
          |}
        """.trimMargin(),
      )
      add(
        "title.proto".toPath(),
        """
          |message Title {
          |  optional string label = 1;
          |}
        """.trimMargin(),
      )
      add(
        "footer.proto".toPath(),
        """
          |message Footer {
          |  optional string label = 1;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .build(),
    )

    val message = pruned.protoFile("message.proto")!!
    assertThat(message.imports).containsExactly("title.proto")
  }

  @Test
  fun retainImportWhenUsedForMessageOneOf() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
          |import 'title.proto';
          |import 'footer.proto';
          |
          |message Message {
          |  oneof title_value {
          |    Title title = 1;
          |    string value = 2;
          |  }
          |}
          |
          |message AnotherMessage {
          |  oneof footer_value {
          |    Footer footer = 1;
          |    string value = 2;
          |  }
          |}
        """.trimMargin(),
      )
      add(
        "title.proto".toPath(),
        """
          |message Title {
          |  optional string label = 1;
          |}
        """.trimMargin(),
      )
      add(
        "footer.proto".toPath(),
        """
          |message Footer {
          |  optional string label = 1;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .build(),
    )

    val message = pruned.protoFile("message.proto")!!
    assertThat(message.imports).containsExactly("title.proto")
  }

  @Test
  fun retainImportWhenUsedForServiceRpc() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
          |import 'call.proto';
          |import 'another_call.proto';
          |
          |service Service {
          |  rpc Call (CallRequest) returns (CallResponse);
          |  rpc AnotherCall (AnotherCallRequest) returns (AnotherCallResponse);
          |}
        """.trimMargin(),
      )
      add(
        "call.proto".toPath(),
        """
          |message CallRequest {
          |}
          |
          |message CallResponse {
          |}
        """.trimMargin(),
      )
      add(
        "another_call.proto".toPath(),
        """
          |message AnotherCallRequest {
          |}
          |
          |message AnotherCallResponse {
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Service#Call")
        .build(),
    )

    val service = pruned.protoFile("service.proto")!!
    assertThat(service.imports).containsExactly("call.proto")
  }

  @Test
  fun retainImportWhenUsedForExtendedMessage() {
    val schema = buildSchema {
      add(
        "extension.proto".toPath(),
        """
          |import 'message.proto';
          |import 'title.proto';
          |import 'footer.proto';
          |
          |extend Message {
          |  optional Title title = 2;
          |}
          |
          |extend AnotherMessage {
          |  optional Footer footer = 2;
          |}
        """.trimMargin(),
      )
      add(
        "message.proto".toPath(),
        """
          |message Message {
          |  optional string value = 1;
          |}
          |
          |message AnotherMessage {
          |  optional string value = 1;
          |}
        """.trimMargin(),
      )
      add(
        "title.proto".toPath(),
        """
          |message Title {
          |  optional string label = 1;
          |}
        """.trimMargin(),
      )
      add(
        "footer.proto".toPath(),
        """
          |message Footer {
          |  optional string label = 1;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .build(),
    )

    val extension = pruned.protoFile("extension.proto")!!
    assertThat(extension.imports).containsExactly("message.proto", "title.proto")
  }

  @Test
  fun retainImportWhenUsedForExtendingOptions() {
    val schema = buildSchema {
      add(
        "extension.proto".toPath(),
        """
          |syntax = "proto2";
          |
          |import "google/protobuf/descriptor.proto";
          |
          |extend google.protobuf.MessageOptions {
          |  optional string value = 10000;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("google.protobuf.MessageOptions")
        .build(),
    )

    val extension = pruned.protoFile("extension.proto")!!
    assertThat(extension.imports).containsExactly("google/protobuf/descriptor.proto")
  }

  @Test
  fun retainImportWhenUsedInMaps() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
          |import 'title.proto';
          |import 'footer.proto';
          |
          |message OuterMessage {
          |  map<int32, Title> titles = 1;
          |
          |  message InnerMessage {
          |    map <string, Footer> footers = 1;
          |  }
          |}
        """.trimMargin(),
      )
      add(
        "title.proto".toPath(),
        """
          |message Title {
          |  optional string label = 1;
          |}
        """.trimMargin(),
      )
      add(
        "footer.proto".toPath(),
        """
          |message Footer {
          |  optional string label = 1;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("OuterMessage")
        .build(),
    )

    val message = pruned.protoFile("message.proto")!!
    assertThat(message.imports).containsExactly("title.proto")
  }

  @Test
  fun retainImportWhenUsedInMapsWithinInnerTypes() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
          |import 'title.proto';
          |import 'footer.proto';
          |
          |message OuterMessage {
          |  map<int32, Title> titles = 1;
          |
          |  message InnerMessage {
          |    map <string, Footer> footers = 1;
          |  }
          |}
        """.trimMargin(),
      )
      add(
        "title.proto".toPath(),
        """
          |message Title {
          |  optional string label = 1;
          |}
        """.trimMargin(),
      )
      add(
        "footer.proto".toPath(),
        """
          |message Footer {
          |  optional string label = 1;
          |}
        """.trimMargin(),
      )
    }

    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("OuterMessage.InnerMessage")
        .build(),
    )

    val message = pruned.protoFile("message.proto")!!
    assertThat(message.imports).containsExactly("footer.proto")
  }

  @Test
  fun retainImportWhenUsedForNestedMessageField() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
          |import 'title.proto';
          |import 'footer.proto';
          |
          |message Outer {
          |  message Message {
          |    optional Title title = 1;
          |  }
          |
          |  message AnotherMessage {
          |    optional Footer footer = 1;
          |  }
          |}
        """.trimMargin(),
      )
      add(
        "title.proto".toPath(),
        """
          |message Title {
          |  optional string label = 1;
          |}
        """.trimMargin(),
      )
      add(
        "footer.proto".toPath(),
        """
          |message Footer {
          |  optional string label = 1;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Outer.Message")
        .build(),
    )

    val message = pruned.protoFile("message.proto")!!
    assertThat(message.imports).containsExactly("title.proto")
  }

  @Test
  fun retainImportWhenUsedForNestedExtendedMessage() {
    val schema = buildSchema {
      add(
        "extension.proto".toPath(),
        """
          |import 'message.proto';
          |import 'title.proto';
          |import 'footer.proto';
          |
          |message Outer {
          |  extend Message {
          |    optional Title title = 2;
          |  }
          |
          |  extend AnotherMessage {
          |    optional Footer footer = 2;
          |  }
          |}
        """.trimMargin(),
      )
      add(
        "message.proto".toPath(),
        """
          |message Message {
          |  optional string value = 1;
          |}
          |
          |message AnotherMessage {
          |  optional string value = 1;
          |}
        """.trimMargin(),
      )
      add(
        "title.proto".toPath(),
        """
          |message Title {
          |  optional string label = 1;
          |}
        """.trimMargin(),
      )
      add(
        "footer.proto".toPath(),
        """
          |message Footer {
          |  optional string label = 1;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .build(),
    )

    val extension = pruned.protoFile("extension.proto")!!
    assertThat(extension.imports).containsExactly("title.proto")
  }

  @Test
  fun retainImportWhenUsedForFileOption() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
          |import 'option.proto';
          |
          |option custom_option = true;
          |
          |message Message {
          |}
        """.trimMargin(),
      )
      add(
        "option.proto".toPath(),
        """
          |import "google/protobuf/descriptor.proto";
          |
          |extend google.protobuf.FileOptions {
          |  optional bool custom_option = 10000;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .build(),
    )

    val message = pruned.protoFile("message.proto")!!
    assertThat(message.imports).containsExactly("option.proto")
  }

  @Test
  fun retainImportWhenUsedForMessageOption() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
          |import 'option.proto';
          |import 'another_option.proto';
          |
          |message Message {
          |  option custom_option = true;
          |}
          |
          |message AnotherMessage {
          |  option another_custom_option = true;
          |}
        """.trimMargin(),
      )
      add(
        "option.proto".toPath(),
        """
          |import "google/protobuf/descriptor.proto";
          |
          |extend google.protobuf.MessageOptions {
          |  optional bool custom_option = 10000;
          |}
        """.trimMargin(),
      )
      add(
        "another_option.proto".toPath(),
        """
          |import "google/protobuf/descriptor.proto";
          |
          |extend google.protobuf.MessageOptions {
          |  optional bool another_custom_option = 10001;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .build(),
    )

    val message = pruned.protoFile("message.proto")!!
    assertThat(message.imports).containsExactly("option.proto")
  }

  @Test
  fun retainImportWhenUsedForFieldOption() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
          |import 'option.proto';
          |import 'another_option.proto';
          |
          |message Message {
          |  optional string value = 1 [custom_option = true];
          |}
          |
          |message AnotherMessage {
          |  optional string value = 1 [another_custom_option = true];
          |}
        """.trimMargin(),
      )
      add(
        "option.proto".toPath(),
        """
          |import "google/protobuf/descriptor.proto";
          |
          |extend google.protobuf.FieldOptions {
          |  optional bool custom_option = 10000;
          |}
        """.trimMargin(),
      )
      add(
        "another_option.proto".toPath(),
        """
          |import "google/protobuf/descriptor.proto";
          |
          |extend google.protobuf.FieldOptions {
          |  optional bool another_custom_option = 10001;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .build(),
    )

    val message = pruned.protoFile("message.proto")!!
    assertThat(message.imports).containsExactly("option.proto")
  }

  @Test
  fun retainImportWhenUsedForOneOfOption() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
          |import 'option.proto';
          |import 'another_option.proto';
          |
          |message Message {
          |  oneof value {
          |    option custom_option = true;
          |    string value_1 = 1;
          |    string value_2 = 2;
          |  }
          |}
          |
          |message AnotherMessage {
          |  oneof value {
          |    option another_custom_option = true;
          |    string value_1 = 1;
          |    string value_2 = 2;
          |  }
          |}
        """.trimMargin(),
      )
      add(
        "option.proto".toPath(),
        """
          |import "google/protobuf/descriptor.proto";
          |
          |extend google.protobuf.OneofOptions {
          |  optional bool custom_option = 10000;
          |}
        """.trimMargin(),
      )
      add(
        "another_option.proto".toPath(),
        """
          |import "google/protobuf/descriptor.proto";
          |
          |extend google.protobuf.OneofOptions {
          |  optional bool another_custom_option = 10001;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .build(),
    )

    val message = pruned.protoFile("message.proto")!!
    assertThat(message.imports).containsExactly("option.proto")
  }

  @Test
  fun retainImportWhenUsedForOneOfFieldOption() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |import 'option.proto';
        |import 'another_option.proto';
        |
        |message Message {
        |  oneof value {
        |    string value_1 = 1 [custom_option = true];
        |    string value_2 = 2;
        |  }
        |}
        |
        |message AnotherMessage {
        |  oneof value {
        |    string value_1 = 1 [another_custom_option = true];
        |    string value_2 = 2;
        |  }
        |}
        """.trimMargin(),
      )
      add(
        "option.proto".toPath(),
        """
        |import "google/protobuf/descriptor.proto";
        |
        |extend google.protobuf.FieldOptions {
        |  optional bool custom_option = 10000;
        |}
        """.trimMargin(),
      )
      add(
        "another_option.proto".toPath(),
        """
        |import "google/protobuf/descriptor.proto";
        |
        |extend google.protobuf.FieldOptions {
        |  optional bool another_custom_option = 10001;
        |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .build(),
    )

    val message = pruned.protoFile("message.proto")!!
    assertThat(message.imports).containsExactly("option.proto")
  }

  @Test
  fun retainImportWhenUsedForEnumOption() {
    val schema = buildSchema {
      add(
        "enum.proto".toPath(),
        """
          |import 'option.proto';
          |import 'another_option.proto';
          |
          |enum Enum {
          |  option custom_option = true;
          |  ENUM_UNSPECIFIED = 0;
          |  ENUM_VALUE = 1;
          |}
          |
          |enum AnotherEnum {
          |  option another_custom_option = true;
          |  ANOTHER_ENUM_UNSPECIFIED = 0;
          |  ANOTHER_ENUM_VALUE = 1;
          |}
        """.trimMargin(),
      )
      add(
        "option.proto".toPath(),
        """
          |import "google/protobuf/descriptor.proto";
          |
          |extend google.protobuf.EnumOptions {
          |  optional bool custom_option = 10000;
          |}
        """.trimMargin(),
      )
      add(
        "another_option.proto".toPath(),
        """
          |import "google/protobuf/descriptor.proto";
          |
          |extend google.protobuf.EnumOptions {
          |  optional bool another_custom_option = 10001;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Enum")
        .build(),
    )

    val enum = pruned.protoFile("enum.proto")!!
    assertThat(enum.imports).containsExactly("option.proto")
  }

  @Test
  fun retainImportWhenUsedForEnumValueOption() {
    val schema = buildSchema {
      add(
        "enum.proto".toPath(),
        """
          |import 'option.proto';
          |import 'another_option.proto';
          |
          |enum Enum {
          |  ENUM_UNSPECIFIED = 0;
          |  ENUM_VALUE = 1 [custom_option = true];
          |}
          |
          |enum AnotherEnum {
          |  ANOTHER_ENUM_UNSPECIFIED = 0;
          |  ANOTHER_ENUM_VALUE = 1 [another_custom_option = true];
          |}
        """.trimMargin(),
      )
      add(
        "option.proto".toPath(),
        """
          |import "google/protobuf/descriptor.proto";
          |
          |extend google.protobuf.EnumValueOptions {
          |  optional bool custom_option = 10000;
          |}
        """.trimMargin(),
      )
      add(
        "another_option.proto".toPath(),
        """
          |import "google/protobuf/descriptor.proto";
          |
          |extend google.protobuf.EnumValueOptions {
          |  optional bool another_custom_option = 10001;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Enum")
        .build(),
    )

    val enum = pruned.protoFile("enum.proto")!!
    assertThat(enum.imports).containsExactly("option.proto")
  }

  @Test
  fun retainImportWhenUsedForServiceOption() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
          |import 'option.proto';
          |import 'another_option.proto';
          |
          |message CallRequest {
          |}
          |
          |message CallResponse {
          |}
          |
          |service Service {
          |  option custom_option = true;
          |  rpc Call (CallRequest) returns (CallResponse);
          |}
          |
          |service AnotherService {
          |  option another_custom_option = true;
          |  rpc Call (CallRequest) returns (CallResponse);
          |}
        """.trimMargin(),
      )
      add(
        "option.proto".toPath(),
        """
          |import "google/protobuf/descriptor.proto";
          |
          |extend google.protobuf.ServiceOptions {
          |  optional bool custom_option = 10000;
          |}
        """.trimMargin(),
      )
      add(
        "another_option.proto".toPath(),
        """
          |import "google/protobuf/descriptor.proto";
          |
          |extend google.protobuf.ServiceOptions {
          |  optional bool another_custom_option = 10001;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Service")
        .build(),
    )

    val service = pruned.protoFile("service.proto")!!
    assertThat(service.imports).containsExactly("option.proto")
  }

  @Test
  fun retainImportWhenUsedForMethodOption() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
          |import 'option.proto';
          |import 'another_option.proto';
          |
          |message CallRequest {
          |}
          |
          |message CallResponse {
          |}
          |
          |service Service {
          |  rpc Call (CallRequest) returns (CallResponse) {
          |    option custom_option = true;
          |  }
          |}
          |
          |service AnotherService {
          |  rpc Call (CallRequest) returns (CallResponse) {
          |    option another_custom_option = true;
          |  }
          |}
        """.trimMargin(),
      )
      add(
        "option.proto".toPath(),
        """
          |import "google/protobuf/descriptor.proto";
          |
          |extend google.protobuf.MethodOptions {
          |  optional bool custom_option = 10000;
          |}
        """.trimMargin(),
      )
      add(
        "another_option.proto".toPath(),
        """
          |import "google/protobuf/descriptor.proto";
          |
          |extend google.protobuf.MethodOptions {
          |  optional bool another_custom_option = 10001;
          |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Service")
        .build(),
    )

    val service = pruned.protoFile("service.proto")!!
    assertThat(service.imports).containsExactly("option.proto")
  }

  @Test
  fun enumsAreKeptsIfUsed() {
    val schema = buildSchema {
      add(
        "currency_code.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("CurrencyCode")
        .build(),
    )

    assertThat(pruned.getType("RoundingMode")).isNotNull()
  }

  /**
   * When an extension field is reached via an option, consider only that option to be reachable.
   * Do not recursively mark the other extensions.
   */
  @Test
  fun markingExtensionFieldDoesNotMarkPeerFields() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .build(),
    )

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
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .build(),
    )

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
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message")
        .addRoot("MessageOption#c")
        .build(),
    )

    val messageOption = pruned.getType("MessageOption") as MessageType
    assertThat(messageOption.field("a")).isNotNull()
    assertThat(messageOption.field("b")).isNull()
    assertThat(messageOption.field("c")).isNotNull()
  }

  @Test
  fun includingFieldDoesNotIncludePeerFields() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
             |message Message {
             |  optional string a = 1;
             |  optional string b = 2;
             |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("Message#a")
        .build(),
    )

    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("a")).isNotNull()
    assertThat(message.field("b")).isNull()
  }

  @Test
  fun excludingGoogleProtobufPrunesAllOptionsOnEnums() {
    val schema = buildSchema {
      add(
        "currency_code.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("squareup.Author")
        .prune("google.protobuf.*")
        .build(),
    )

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
    val schema = buildSchema {
      add(
        "currency_code.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("squareup.Letter")
        .prune("google.protobuf.*")
        .build(),
    )

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
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |import "wire/extensions.proto";
            |
            |message Message {
            |  optional string radio = 1 [(wire.until) = "1950"];
            |  optional string video = 2 [(wire.since) = "1950"];
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .since("1949")
        .until("1950")
        .build(),
    )
    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("radio")).isNotNull()
    assertThat(message.field("video")).isNull()
  }

  @Test
  fun onlyRetainOlder() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |import "wire/extensions.proto";
            |
            |message Message {
            |  optional string radio = 1 [(wire.until) = "1950"];
            |  optional string video = 2 [(wire.since) = "1950"];
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .only("1949")
        .build(),
    )
    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("radio")).isNotNull()
    assertThat(message.field("video")).isNull()
  }

  @Test
  fun sinceAndUntilRetainNewer() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |import "wire/extensions.proto";
            |
            |message Message {
            |  optional string radio = 1 [(wire.until) = "1950"];
            |  optional string video = 2 [(wire.since) = "1950"];
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .since("1950")
        .until("1951")
        .build(),
    )
    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("radio")).isNull()
    assertThat(message.field("video")).isNotNull()
  }

  @Test
  fun onlyRetainNewer() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |import "wire/extensions.proto";
            |
            |message Message {
            |  optional string radio = 1 [(wire.until) = "1950"];
            |  optional string video = 2 [(wire.since) = "1950"];
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .only("1950")
        .build(),
    )
    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("radio")).isNull()
    assertThat(message.field("video")).isNotNull()
  }

  @Test
  fun sinceRetainedWhenLessThanOrEqualToUntil() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .since("20")
        .until("30")
        .build(),
    )
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
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .since("20")
        .until("30")
        .build(),
    )
    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("until_19")).isNull()
    assertThat(message.field("until_20")).isNull()
    assertThat(message.field("until_21")).isNotNull()
    assertThat(message.field("until_29")).isNotNull()
    assertThat(message.field("until_30")).isNotNull()
    assertThat(message.field("until_31")).isNotNull()
  }

  @Test
  fun sinceRetainedWhenLessThanOrEqualToOnly() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .only("20")
        .build(),
    )
    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("since_19")).isNotNull()
    assertThat(message.field("since_20")).isNotNull()
    assertThat(message.field("since_21")).isNull()

    assertThat(message.field("since_29")).isNull()
    assertThat(message.field("since_30")).isNull()
    assertThat(message.field("since_31")).isNull()
  }

  @Test
  fun untilRetainedWhenGreaterThanOnly() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .only("20")
        .build(),
    )
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
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |import "wire/extensions.proto";
            |
            |message Message {
            |  optional string until_20 = 1 [(wire.until) = "20"];
            |  optional string since_20 = 2 [(wire.since) = "20"];
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(PruningRules.Builder().build())
    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("since_20")).isNotNull()
    assertThat(message.field("until_20")).isNotNull()
  }

  @Test
  fun versionPruningDoesNotImpactFieldsWithoutSinceAndUntil() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |import "wire/extensions.proto";
            |
            |message Message {
            |  optional string always = 1;
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .since("20")
        .until("30")
        .build(),
    )
    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("always")).isNotNull()
  }

  @Test
  fun onlyVersionPruningDoesNotImpactFieldsWithoutSinceAndUntil() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |import "wire/extensions.proto";
            |
            |message Message {
            |  optional string always = 1;
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .only("20")
        .build(),
    )
    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("always")).isNotNull()
  }

  @Test
  fun sinceUntilOnEnumConstant() {
    val schema = buildSchema {
      add(
        "roshambo.proto".toPath(),
        """
            |import "wire/extensions.proto";
            |
            |enum Roshambo {
            |  ROCK = 1 [(wire.constant_until) = "29"];
            |  SCISSORS = 2 [(wire.constant_since) = "30"];
            |  PAPER = 3 [(wire.constant_since) = "29"];
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .since("29")
        .until("30")
        .build(),
    )
    val enum = pruned.getType("Roshambo") as EnumType
    assertThat(enum.constant("ROCK")).isNull()
    assertThat(enum.constant("SCISSORS")).isNull()
    assertThat(enum.constant("PAPER")).isNotNull()
  }

  @Test
  fun onlyOnEnumConstant() {
    val schema = buildSchema {
      add(
        "roshambo.proto".toPath(),
        """
            |import "wire/extensions.proto";
            |
            |enum Roshambo {
            |  ROCK = 1 [(wire.constant_until) = "29"];
            |  SCISSORS = 2 [(wire.constant_since) = "30"];
            |  PAPER = 3 [(wire.constant_since) = "29"];
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .only("29")
        .build(),
    )
    val enum = pruned.getType("Roshambo") as EnumType
    assertThat(enum.constant("ROCK")).isNull()
    assertThat(enum.constant("SCISSORS")).isNull()
    assertThat(enum.constant("PAPER")).isNotNull()
  }

  @Test
  fun semVer() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |import "wire/extensions.proto";
            |
            |message Message {
            |  optional string field_1 = 1 [(wire.until) = "1.0.0-alpha"];
            |  optional string field_2 = 2 [(wire.until) = "1.0.0-alpha.1"];
            |  optional string field_3 = 3 [(wire.until) = "1.0.0-alpha.beta"];
            |  optional string field_4 = 4 [(wire.since) = "1.0.0-beta"];
            |  optional string field_5 = 5 [(wire.since) = "1.0.0"];
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .since("1.0.0-alpha.1")
        .until("1.0.0-beta")
        .build(),
    )
    val message = pruned.getType("Message") as MessageType
    assertThat(message.field("field_1")).isNull()
    assertThat(message.field("field_2")).isNull()
    assertThat(message.field("field_3")).isNotNull()
    assertThat(message.field("field_4")).isNull()
    assertThat(message.field("field_5")).isNull()
  }

  @Test
  fun typeIsRetainedIfMorePreciseRuleExists() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
            |package wire;
            |
            |message MessageA {
            |}
            |message MessageB {
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("wire.MessageA")
        .prune("wire.*")
        .build(),
    )
    assertThat(pruned.getType("wire.MessageA")).isNotNull()
    assertThat(pruned.getType("wire.MessageB")).isNull()
  }

  @Test
  fun fieldIsRetainedIfMorePreciseRuleExists() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
            |message MyMessage {
            |  optional string a = 1;
            |  optional string b = 2;
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("MyMessage#a")
        .prune("MyMessage")
        .build(),
    )
    assertThat(pruned.getType("MyMessage")).isNotNull()
    val myMessageType = pruned.getType("MyMessage") as MessageType
    assertThat(myMessageType.field("a")).isNotNull()
    assertThat(myMessageType.field("b")).isNull()
  }

  @Test
  fun enumConstantIsRetainedIfMorePreciseRuleExists() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
            |enum MyEnum {
            |  A = 1;
            |  B = 2;
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("MyEnum#A")
        .prune("MyEnum")
        .build(),
    )
    assertThat(pruned.getType("MyEnum")).isNotNull()
    val myEnumType = pruned.getType("MyEnum") as EnumType
    assertThat(myEnumType.constant("A")).isNotNull()
    assertThat(myEnumType.constant("B")).isNull()
  }

  @Test
  fun optionFieldIsRetainedIfMorePreciseRuleExists() {
    val schema = buildSchema {
      add(
        "lecture.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("wire.Lecture")
        .addRoot("google.protobuf.FieldOptions#wire.relevant")
        .prune("google.protobuf.*")
        .build(),
    )
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
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
            |message MyMessage {
            |  optional string a = 1;
            |  optional MyEnum b = 2;
            |}
            |
            |enum MyEnum {
            |  C = 1;
            |  D = 2;
            |}
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .prune("MyEnum#D")
        .addRoot("MyMessage#b")
        .prune("MyMessage")
        .build(),
    )
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
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }
    val pruned = schema.prune(
      PruningRules.Builder()
        .addRoot("MessageA#book")
        .addRoot("MessageB")
        .prune("Book")
        .prune("Stuff")
        .build(),
    )

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

// Used so that spotless or the IDE doesn't trim them away.
private const val space = " "
