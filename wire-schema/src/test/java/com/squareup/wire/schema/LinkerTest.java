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

import autovalue.shaded.com.google.common.common.collect.ImmutableList;
import autovalue.shaded.com.google.common.common.collect.ImmutableMap;
import com.squareup.wire.schema.internal.parser.FieldElement;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class LinkerTest {
  @Test public void resolveEmptyContext() throws Exception {
    // given
    ProtoFile protoFile = ProtoFile.get(ProtoFileElement.builder(Location.get("empty.proto")).build());
    Linker linker = new Linker(ImmutableList.of(protoFile));

    try {
      // when
      linker.resolve("A", ImmutableMap.of("A", protoFile));

      // then
      fail("Linker should throw IllegalStateException when resolving empty context");
    }
    catch (IllegalStateException e) {
      assertThat(e.getMessage()).isNull();
    }
  }

  @Test public void packageName() throws Exception {
    // given
    ProtoFile emptyProtoFile = ProtoFile.get(ProtoFileElement.builder(Location.get("empty.proto")).build());
    ProtoFile protoFile = ProtoFile.get(ProtoFileElement.builder(Location.get("packaged.proto")).packageName("a.b.c").build());

    // when
    Linker nullLinker = new Linker(ImmutableList.of(emptyProtoFile));
    // then
    assertThat(nullLinker.packageName()).isNull();

    // when
    Linker emptyPackageNameLinker = new Linker(ImmutableList.of(emptyProtoFile)).withContext(emptyProtoFile);
    // then
    assertThat(emptyPackageNameLinker.packageName()).isNull();

    // when
    Linker notProtoFileLinker = new Linker(ImmutableList.of(emptyProtoFile)).withContext(Field.PACKED);
    // then
    assertThat(notProtoFileLinker.packageName()).isNull();

    // when
    Linker linker = new Linker(ImmutableList.of(protoFile)).withContext(protoFile);
    // then
    assertThat(linker.packageName()).isEqualTo("a.b.c");
  }

  @Test public void dereference() {
    // given
    Schema schema = new SchemaBuilder()
      .add("enum.proto", ""
          + "enum Enum {\n"
          + "\n"
          + "  A = 1;\n"
          + "  B = 2;\n"
          + "}\n")
      .add("message.proto", ""
        + "import enum.proto;"
        + "message Message {\n"
        + "  optional Nested M = 1;\n"
        + "\n"
        + "  required Enum N = 2;\n"
        + "}\n"
        + "\n"
        + "message Nested {\n"
        + "}\n"
        + "\n"
        + "extend Nested {\n"
        + "  optional string S = 1;\n"
        + "}\n")
      .build();

    // when
    Linker enumLinker = new Linker(ImmutableList.of(schema.protoFile("enum.proto")));
    Linker messageLinker = new Linker(ImmutableList.of(schema.protoFile("enum.proto")));

    // then
    assertThat(enumLinker.dereference(schema.getField(ProtoMember.get("Message#N")), "[A]")).isNull();
    assertThat(messageLinker.dereference(schema.getField(ProtoMember.get("Message#M")), "[S]")).isNull();
  }
}
