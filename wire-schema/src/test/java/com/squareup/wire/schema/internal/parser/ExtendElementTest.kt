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

import com.squareup.wire.schema.Field.Label.REQUIRED
import com.squareup.wire.schema.Location
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ExtendElementTest {
  internal var location = Location.get("file.proto")

  @Test
  fun emptyToSchema() {
    val extend = ExtendElement(
        location = location,
        name = "Name"
    )
    val expected = "extend Name {}\n"
    assertThat(extend.toSchema()).isEqualTo(expected)
  }

  @Test
  fun simpleToSchema() {
    val extend = ExtendElement(
        location = location,
        name = "Name",
        fields = listOf(
            FieldElement.builder(location)
                .label(REQUIRED)
                .type("string")
                .name("name")
                .tag(1)
                .build()
        ))
    val expected = """
        |extend Name {
        |  required string name = 1;
        |}
        |""".trimMargin()
    assertThat(extend.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleFields() {
    val firstName = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("first_name")
        .tag(1)
        .build()
    val lastName = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("last_name")
        .tag(2)
        .build()
    val extend = ExtendElement(
        location = location,
        name = "Name",
        fields = listOf(firstName, lastName)
    )
    assertThat(extend.fields).hasSize(2)
  }

  @Test
  fun simpleWithDocumentationToSchema() {
    val extend = ExtendElement(
        location = location,
        name = "Name",
        documentation = "Hello",
        fields = listOf(
            FieldElement.builder(location)
                .label(REQUIRED)
                .type("string")
                .name("name")
                .tag(1)
                .build()
        )
    )
    val expected = """
        |// Hello
        |extend Name {
        |  required string name = 1;
        |}
        |""".trimMargin()
    assertThat(extend.toSchema()).isEqualTo(expected)
  }
}
