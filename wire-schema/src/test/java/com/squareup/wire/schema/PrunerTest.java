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
}
