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

public class ProtoFileTest {
  @Test public void retainAllWithotMarkedElement() {
    // given
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

    ProtoFile protoFile = schema.protoFile("service.proto");

    MarkSet markSet = new MarkSet(new IdentifierSet.Builder().build());
    markSet.root(ProtoType.get("B"));

    // when
    ProtoFile expected = protoFile.retainAll(schema, markSet);

    // then
    assertThat(expected.services()).isNullOrEmpty();
  }

}
