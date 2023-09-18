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
import assertk.assertions.isEqualTo
import com.squareup.wire.schema.internal.parser.OptionElement.Kind.BOOLEAN
import com.squareup.wire.schema.internal.parser.OptionElement.Kind.LIST
import com.squareup.wire.schema.internal.parser.OptionElement.Kind.MAP
import com.squareup.wire.schema.internal.parser.OptionElement.Kind.OPTION
import com.squareup.wire.schema.internal.parser.OptionElement.Kind.STRING
import kotlin.test.Test

class OptionElementTest {
  @Test
  fun simpleToSchema() {
    val option = OptionElement.create("foo", STRING, "bar")
    val expected = """foo = "bar""""
    assertThat(option.toSchema()).isEqualTo(expected)
  }

  @Test
  fun nestedToSchema() {
    val option =
      OptionElement.create("foo.boo", OPTION, OptionElement.create("bar", STRING, "baz"), true)
    val expected = """(foo.boo).bar = "baz""""
    assertThat(option.toSchema()).isEqualTo(expected)
  }

  @Test
  fun listToSchema() {
    val option = OptionElement.create(
      "foo",
      LIST,
      listOf(
        OptionElement.create("ping", STRING, "pong", true),
        OptionElement.create("kit", STRING, "kat"),
      ),
      true,
    )
    val expected = """
        |(foo) = [
        |  (ping) = "pong",
        |  kit = "kat"
        |]
    """.trimMargin()
    assertThat(option.toSchema()).isEqualTo(expected)
  }

  @Test
  fun mapToSchema() {
    val option = OptionElement.create(
      "foo",
      MAP,
      mapOf("ping" to "pong", "kit" to listOf("kat", "kot")),
    )
    val expected = """
        |foo = {
        |  ping: "pong",
        |  kit: [
        |    "kat",
        |    "kot"
        |  ]
        |}
    """.trimMargin()
    assertThat(option.toSchema()).isEqualTo(expected)
  }

  @Test
  fun booleanToSchema() {
    val option = OptionElement.create("foo", BOOLEAN, "false")
    val expected = "foo = false"
    assertThat(option.toSchema()).isEqualTo(expected)
  }
}
