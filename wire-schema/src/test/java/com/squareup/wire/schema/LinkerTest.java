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
  @Test(expected = IllegalStateException.class) public void resolveEmptyContext() throws Exception {
    ProtoFile protoFile = ProtoFile.get(ProtoFileElement.builder(Location.get("empty.proto")).build());
    Linker linker = new Linker(ImmutableList.of(protoFile));

    linker.resolve("A", ImmutableMap.of("A", protoFile));

    fail("Linker should throw IllegalStateException when resolving empty context");
  }

  @Test public void packageName() throws Exception {
    ProtoFile emptyProtoFile = ProtoFile.get(ProtoFileElement.builder(Location.get("empty.proto")).build());
    ProtoFile protoFile = ProtoFile.get(ProtoFileElement.builder(Location.get("packaged.proto")).packageName("a.b.c").build());
    Linker nullLinker = new Linker(ImmutableList.of(emptyProtoFile));
    Linker emptyPackageNameLinker = new Linker(ImmutableList.of(emptyProtoFile)).withContext(emptyProtoFile);
    Linker notProtoFileLinker = new Linker(ImmutableList.of(emptyProtoFile)).withContext(Field.PACKED);
    Linker linker = new Linker(ImmutableList.of(protoFile)).withContext(protoFile);

    assertThat(nullLinker.packageName()).isNull();
    assertThat(emptyPackageNameLinker.packageName()).isNull();
    assertThat(notProtoFileLinker.packageName()).isNull();
    assertThat(linker.packageName()).isEqualTo("a.b.c");
  }

  @Test public void dereference() throws Exception {
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

    Linker enumLinker = new Linker(ImmutableList.of(schema.protoFile("enum.proto")));
    Linker messageLinker = new Linker(ImmutableList.of(schema.protoFile("enum.proto")));

    assertThat(enumLinker.dereference(schema.getField(ProtoMember.get("Message#N")), "[A]")).isNull();
    assertThat(messageLinker.dereference(schema.getField(ProtoMember.get("Message#M")), "[S]")).isNull();
  }

  @Ignore    // this bug is not fixed
  @Test (expected = IllegalArgumentException.class) public void dereferenceFieldWithoutOpenBrace() throws Exception {
    // given
    ProtoFile protoFile = ProtoFile.get(ProtoFileElement.builder(Location.get("empty.proto")).build());
    Linker linker = new Linker(ImmutableList.of(protoFile));
    Field field = new Field("", FieldElement.builder(Location.get("message.proto")).build(), null, false);

    // when
    linker.dereference(field, "withoutOpenBrace]");

    // then
    fail("Linker should throw IllegalArgumentException when try to dereference field with only one brace");
  }

  @Ignore    // this bug is not fixed
  @Test (expected = IllegalArgumentException.class) public void dereferenceFieldWithoutClosingBrace() throws Exception {
    // given
    ProtoFile protoFile = ProtoFile.get(ProtoFileElement.builder(Location.get("empty.proto")).build());
    Linker linker = new Linker(ImmutableList.of(protoFile));
    Field field = new Field("", FieldElement.builder(Location.get("message.proto")).build(), null, false);

    // when
    linker.dereference(field, "[withoutClosingBrace");

    // then
    fail("Linker should throw IllegalArgumentException when try to dereference field with only one brace");
  }

}
