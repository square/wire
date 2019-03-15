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
package com.squareup.wire.schema.internal.parser

import com.google.common.collect.ImmutableList
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.internal.parser.OptionElement.Kind.STRING
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class EnumElementTest {
  internal var location = Location.get("file.proto")

  @Test
  fun emptyToSchema() {
    val element = EnumElement.builder(location)
        .name("Enum")
        .build()
    val expected = "enum Enum {}\n"
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun simpleToSchema() {
    val element = EnumElement.builder(location)
        .name("Enum")
        .constants(
            ImmutableList.of(
                EnumConstantElement.builder(location).name("ONE").tag(1).build(),
                EnumConstantElement.builder(location).name("TWO").tag(2).build(),
                EnumConstantElement.builder(location).name("SIX").tag(6).build()
            )
        )
        .build()
    val expected = """
        |enum Enum {
        |  ONE = 1;
        |  TWO = 2;
        |  SIX = 6;
        |}
        |""".trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleConstants() {
    val one = EnumConstantElement.builder(location)
        .name("ONE")
        .tag(1)
        .build()
    val two = EnumConstantElement.builder(location)
        .name("TWO")
        .tag(2)
        .build()
    val six = EnumConstantElement.builder(location)
        .name("SIX")
        .tag(6)
        .build()
    val element = EnumElement.builder(location)
        .name("Enum")
        .constants(ImmutableList.of(one, two, six))
        .build()
    assertThat(element.constants()).hasSize(3)
  }

  @Test
  fun simpleWithOptionsToSchema() {
    val element = EnumElement.builder(location)
        .name("Enum")
        .options(ImmutableList.of(OptionElement.create("kit", STRING, "kat")))
        .constants(
            ImmutableList.of(
                EnumConstantElement.builder(location).name("ONE").tag(1).build(),
                EnumConstantElement.builder(location).name("TWO").tag(2).build(),
                EnumConstantElement.builder(location).name("SIX").tag(6).build()
            )
        )
        .build()
    val expected = """
        |enum Enum {
        |  option kit = "kat";
        |
        |  ONE = 1;
        |  TWO = 2;
        |  SIX = 6;
        |}
        |""".trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleOptions() {
    val kitKat = OptionElement.create("kit", STRING, "kat")
    val fooBar = OptionElement.create("foo", STRING, "bar")
    val element = EnumElement.builder(location)
        .name("Enum")
        .options(ImmutableList.of(kitKat, fooBar))
        .constants(
            ImmutableList.of(
                EnumConstantElement.builder(location).name("ONE").tag(1).build()
            )
        )
        .build()
    assertThat(element.options()).hasSize(2)
  }

  @Test
  fun simpleWithDocumentationToSchema() {
    val element = EnumElement.builder(location)
        .name("Enum")
        .documentation("Hello")
        .constants(
            ImmutableList.of(
                EnumConstantElement.builder(location).name("ONE").tag(1).build(),
                EnumConstantElement.builder(location).name("TWO").tag(2).build(),
                EnumConstantElement.builder(location).name("SIX").tag(6).build()
            )
        )
        .build()
    val expected = """
        |// Hello
        |enum Enum {
        |  ONE = 1;
        |  TWO = 2;
        |  SIX = 6;
        |}
        |""".trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun fieldToSchema() {
    val value = EnumConstantElement.builder(location)
        .name("NAME")
        .tag(1)
        .build()
    val expected = "NAME = 1;\n"
    assertThat(value.toSchema()).isEqualTo(expected)
  }

  @Test
  fun fieldWithDocumentationToSchema() {
    val value = EnumConstantElement.builder(location)
        .name("NAME")
        .tag(1)
        .documentation("Hello")
        .build()
    val expected = """
        |// Hello
        |NAME = 1;
        |""".trimMargin()
    assertThat(value.toSchema()).isEqualTo(expected)
  }

  @Test
  fun fieldWithOptionsToSchema() {
    val value = EnumConstantElement.builder(location)
        .name("NAME")
        .tag(1)
        .options(
            ImmutableList.of(
                OptionElement.create("kit", STRING, "kat", true),
                OptionElement.create("tit", STRING, "tat")
            )
        )
        .build()
    val expected = """
        |NAME = 1 [
        |  (kit) = "kat",
        |  tit = "tat"
        |];
        |""".trimMargin()
    assertThat(value.toSchema()).isEqualTo(expected)
  }
}
