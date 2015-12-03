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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceTest {

  @Test public void retainAllWithotMarkedElement() {
    // when
    Schema schema = new SchemaBuilder()
      .add("service.proto", ""
        + "service A {\n"
        + "  rpc Call (Request) returns (Response);\n"
        + "}\n"
        + "message Request {\n"
        + "}\n"
        + "message Response {\n"
        + "}\n")
      .build();

    Service service = schema.getService("A");

    MarkSet markSet1 = new MarkSet(new IdentifierSet.Builder().build());
    markSet1.root(ProtoType.get("B"));

    MarkSet markSet2 = new MarkSet(new IdentifierSet.Builder().build());
    markSet2.root(ProtoType.get("A"));
    markSet2.mark(ProtoType.get("Request"));
    markSet2.mark(ProtoType.get("Response"));

    MarkSet markSet3 = new MarkSet(new IdentifierSet.Builder().exclude("A#Call").build());
    markSet3.root(ProtoType.get("A"));
    markSet3.mark(ProtoType.get("Request"));
    markSet3.mark(ProtoType.get("Response"));

    MarkSet markSet4 = new MarkSet(new IdentifierSet.Builder().build());
    markSet4.root(ProtoType.get("A"));
    markSet4.mark(ProtoMember.get("A#Call"));

    // then
    assertThat(service.retainAll(schema, markSet1)).isNull();;
    assertThat(service.retainAll(schema, markSet2).rpc("Call")).isNotNull();
    assertThat(service.retainAll(schema, markSet3).rpc("Call")).isNull();
    assertThat(service.retainAll(schema, markSet4).rpc("Call")).isNull();
  }
}
