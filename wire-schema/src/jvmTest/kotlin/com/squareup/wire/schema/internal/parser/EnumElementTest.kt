/*
 * Copyright (C) 2014 Square, Inc.
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
package com.squareup.wire.schema.internal.parser

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.internal.MAX_TAG_VALUE
import com.squareup.wire.schema.internal.parser.OptionElement.Kind.STRING
import kotlin.test.Test

class EnumElementTest {
  internal var location = Location.get("file.proto")

  @Test
  fun emptyToSchema() {
    val element = EnumElement(
      location = location,
      name = "Enum",
    )
    val expected = "enum Enum {}\n"
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun simpleToSchema() {
    val element = EnumElement(
      location = location,
      name = "Enum",
      constants = listOf(
        EnumConstantElement(location = location, name = "ONE", tag = 1),
        EnumConstantElement(location = location, name = "TWO", tag = 2),
        EnumConstantElement(location = location, name = "SIX", tag = 6),
      ),
    )
    val expected = """
        |enum Enum {
        |  ONE = 1;
        |  TWO = 2;
        |  SIX = 6;
        |}
        |
    """.trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleConstants() {
    val one = EnumConstantElement(location = location, name = "ONE", tag = 1)
    val two = EnumConstantElement(location = location, name = "TWO", tag = 2)
    val six = EnumConstantElement(location = location, name = "SIX", tag = 6)
    val element = EnumElement(
      location = location,
      name = "Enum",
      constants = listOf(one, two, six),
    )
    assertThat(element.constants).hasSize(3)
  }

  @Test
  fun simpleWithOptionsToSchema() {
    val element = EnumElement(
      location = location,
      name = "Enum",
      options = listOf(OptionElement.create("kit", STRING, "kat")),
      constants = listOf(
        EnumConstantElement(location = location, name = "ONE", tag = 1),
        EnumConstantElement(location = location, name = "TWO", tag = 2),
        EnumConstantElement(location = location, name = "SIX", tag = 6),
      ),
    )
    val expected = """
        |enum Enum {
        |  option kit = "kat";
        |  ONE = 1;
        |  TWO = 2;
        |  SIX = 6;
        |}
        |
    """.trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleOptions() {
    val kitKat = OptionElement.create("kit", STRING, "kat")
    val fooBar = OptionElement.create("foo", STRING, "bar")
    val element = EnumElement(
      location = location,
      name = "Enum",
      options = listOf(kitKat, fooBar),
      constants = listOf(EnumConstantElement(location = location, name = "ONE", tag = 1)),
    )
    assertThat(element.options).hasSize(2)
  }

  @Test
  fun simpleWithDocumentationToSchema() {
    val element = EnumElement(
      location = location,
      name = "Enum",
      documentation = "Hello",
      constants =
      listOf(
        EnumConstantElement(location = location, name = "ONE", tag = 1),
        EnumConstantElement(location = location, name = "TWO", tag = 2),
        EnumConstantElement(location = location, name = "SIX", tag = 6),
      ),
    )
    val expected = """
        |// Hello
        |enum Enum {
        |  ONE = 1;
        |  TWO = 2;
        |  SIX = 6;
        |}
        |
    """.trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun simpleWithReservedToSchema() {
    val element = EnumElement(
      location = location,
      name = "Enum",
      reserveds = listOf(
        ReservedElement(location = location, values = listOf(10, 12..14, "FOO")),
        ReservedElement(location = location, values = listOf(10)),
        ReservedElement(location = location, values = listOf(25..MAX_TAG_VALUE)),
        ReservedElement(location = location, values = listOf("FOO")),
      ),
      constants = listOf(
        EnumConstantElement(location = location, name = "ONE", tag = 1),
        EnumConstantElement(location = location, name = "TWO", tag = 2),
        EnumConstantElement(location = location, name = "SIX", tag = 6),
      ),
    )
    val expected = """
        |enum Enum {
        |  reserved 10, 12 to 14, "FOO";
        |  reserved 10;
        |  reserved 25 to max;
        |  reserved "FOO";
        |  ONE = 1;
        |  TWO = 2;
        |  SIX = 6;
        |}
        |
    """.trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun fieldToSchema() {
    val value = EnumConstantElement(location = location, name = "NAME", tag = 1)
    val expected = "NAME = 1;\n"
    assertThat(value.toSchema()).isEqualTo(expected)
  }

  @Test
  fun fieldWithDocumentationToSchema() {
    val value = EnumConstantElement(
      location = location,
      name = "NAME",
      tag = 1,
      documentation = "Hello",
    )
    val expected = """
        |// Hello
        |NAME = 1;
        |
    """.trimMargin()
    assertThat(value.toSchema()).isEqualTo(expected)
  }

  @Test
  fun fieldWithOptionsToSchema() {
    val value = EnumConstantElement(
      location = location,
      name = "NAME",
      tag = 1,
      options = listOf(
        OptionElement.create("kit", STRING, "kat", true),
        OptionElement.create("tit", STRING, "tat"),
      ),
    )
    val expected = """
        |NAME = 1 [
        |  (kit) = "kat",
        |  tit = "tat"
        |];
        |
    """.trimMargin()
    assertThat(value.toSchema()).isEqualTo(expected)
  }
}
