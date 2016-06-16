/*
 * Copyright (C) 2014 Square, Inc.
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
package com.squareup.wire.schema.internal.parser;

import com.google.common.collect.ImmutableList;
import com.squareup.wire.schema.Location;
import org.junit.Test;

import static com.squareup.wire.schema.internal.parser.OptionElement.Kind.STRING;
import static org.assertj.core.api.Assertions.assertThat;

public final class EnumElementTest {
  Location location = Location.get("file.proto");

  @Test public void emptyToSchema() {
    EnumElement element = EnumElement.builder(location).name("Enum").build();
    String expected = "enum Enum {}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void simpleToSchema() {
    EnumElement element = EnumElement.builder(location)
        .name("Enum")
        .constants(ImmutableList.of(
            EnumConstantElement.builder(location).name("ONE").tag(1).build(),
            EnumConstantElement.builder(location).name("TWO").tag(2).build(),
            EnumConstantElement.builder(location).name("SIX").tag(6).build()))
        .build();
    String expected = ""
        + "enum Enum {\n"
        + "  ONE = 1;\n"
        + "  TWO = 2;\n"
        + "  SIX = 6;\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleConstants() {
    EnumConstantElement one = EnumConstantElement.builder(location).name("ONE").tag(1).build();
    EnumConstantElement two = EnumConstantElement.builder(location).name("TWO").tag(2).build();
    EnumConstantElement six = EnumConstantElement.builder(location).name("SIX").tag(6).build();
    EnumElement element = EnumElement.builder(location)
        .name("Enum")
        .constants(ImmutableList.of(one, two, six))
        .build();
    assertThat(element.constants()).hasSize(3);
  }

  @Test public void simpleWithOptionsToSchema() {
    EnumElement element = EnumElement.builder(location)
        .name("Enum").options(ImmutableList.of(OptionElement.create("kit", STRING, "kat")))
        .constants(ImmutableList.of(
            EnumConstantElement.builder(location).name("ONE").tag(1).build(),
            EnumConstantElement.builder(location).name("TWO").tag(2).build(),
            EnumConstantElement.builder(location).name("SIX").tag(6).build()))
        .build();
    String expected = ""
        + "enum Enum {\n"
        + "  option kit = \"kat\";\n"
        + "\n"
        + "  ONE = 1;\n"
        + "  TWO = 2;\n"
        + "  SIX = 6;\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleOptions() {
    OptionElement kitKat = OptionElement.create("kit", STRING, "kat");
    OptionElement fooBar = OptionElement.create("foo", STRING, "bar");
    EnumElement element = EnumElement.builder(location)
        .name("Enum")
        .options(ImmutableList.of(kitKat, fooBar))
        .constants(ImmutableList.of(
            EnumConstantElement.builder(location).name("ONE").tag(1).build()))
        .build();
    assertThat(element.options()).hasSize(2);
  }

  @Test public void simpleWithDocumentationToSchema() {
    EnumElement element = EnumElement.builder(location)
        .name("Enum")
        .documentation("Hello")
        .constants(ImmutableList.of(
            EnumConstantElement.builder(location).name("ONE").tag(1).build(),
            EnumConstantElement.builder(location).name("TWO").tag(2).build(),
            EnumConstantElement.builder(location).name("SIX").tag(6).build()))
        .build();
    String expected = ""
        + "// Hello\n"
        + "enum Enum {\n"
        + "  ONE = 1;\n"
        + "  TWO = 2;\n"
        + "  SIX = 6;\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void fieldToSchema() {
    EnumConstantElement value = EnumConstantElement.builder(location).name("NAME").tag(1).build();
    String expected = "NAME = 1;\n";
    assertThat(value.toSchema()).isEqualTo(expected);
  }

  @Test public void fieldWithDocumentationToSchema() {
    EnumConstantElement value = EnumConstantElement.builder(location)
        .name("NAME")
        .tag(1)
        .documentation("Hello")
        .build();
    String expected = ""
        + "// Hello\n"
        + "NAME = 1;\n";
    assertThat(value.toSchema()).isEqualTo(expected);
  }

  @Test public void fieldWithOptionsToSchema() {
    EnumConstantElement value = EnumConstantElement.builder(location)
        .name("NAME")
        .tag(1)
        .options(ImmutableList.of(
            OptionElement.create("kit", STRING, "kat", true),
            OptionElement.create("tit", STRING, "tat")))
    .build();
    String expected = "NAME = 1 [\n"
        + "  (kit) = \"kat\",\n"
        + "  tit = \"tat\"\n"
        + "];\n";
    assertThat(value.toSchema()).isEqualTo(expected);
  }
}
