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
import org.junit.Test;

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
    Schema pruned = schema.retainRoots(ImmutableList.of("MessageA"));
    assertThat(pruned.getType("MessageA")).isNotNull();
    assertThat(pruned.getType("MessageB")).isNull();
  }

  @Test public void retainTypeRetainsFieldTypesTransitively() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("service.proto", ""
            + "message MessageA {\n"
            + "  optional MessageB b = 1;\n"
            + "}\n"
            + "message MessageB {\n"
            + "  optional MessageC c = 1;\n"
            + "}\n"
            + "message MessageC {\n"
            + "}\n"
            + "message MessageD {\n"
            + "}\n")
        .build();
    Schema pruned = schema.retainRoots(ImmutableList.of("MessageA"));
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
    Schema pruned = schema.retainRoots(ImmutableList.of("Service#CallA"));
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
            + "  optional string c = 2;\n"
            + "}\n"
            + "message MessageB {\n"
            + "}\n")
        .build();
    Schema pruned = schema.retainRoots(ImmutableList.of("MessageA#b"));
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
    Schema pruned = schema.retainRoots(ImmutableList.of("MessageA#b"));
    assertThat(pruned.getType("MessageA")).isNotNull();
    assertThat(((MessageType) pruned.getType("MessageA")).field("b")).isNotNull();
    assertThat(((MessageType) pruned.getType("MessageA")).field("d")).isNull();
    assertThat(pruned.getType("MessageB")).isNotNull();
    assertThat(pruned.getType("MessageC")).isNotNull();
    assertThat(pruned.getType("MessageD")).isNull();
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
    Schema pruned = schema.retainRoots(ImmutableList.of("MessageA#b", "MessageB#c"));
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
    Schema pruned = schema.retainRoots(ImmutableList.of("Roshambo#SCISSORS"));
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
    Schema pruned = schema.retainRoots(ImmutableList.of("Message", "Roshambo#SCISSORS"));
    assertThat(pruned.getType("Message")).isNotNull();
    assertThat(((MessageType) pruned.getType("Message")).field("roshambo")).isNotNull();
    assertThat(pruned.getType("Roshambo")).isNotNull();
    assertThat(((EnumType) pruned.getType("Roshambo")).constant("ROCK")).isNull();
    assertThat(((EnumType) pruned.getType("Roshambo")).constant("SCISSORS")).isNotNull();
    assertThat(((EnumType) pruned.getType("Roshambo")).constant("PAPER")).isNull();
  }
}
