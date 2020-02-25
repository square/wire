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
package com.squareup.wire.schema.internal.parser

import com.squareup.wire.schema.Field.Label.OPTIONAL
import com.squareup.wire.schema.Field.Label.REQUIRED
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.SyntaxRules
import com.squareup.wire.schema.SyntaxRules.Companion.PROTO_2_SYNTAX_RULES
import com.squareup.wire.schema.SyntaxRules.Companion.PROTO_3_SYNTAX_RULES
import com.squareup.wire.schema.internal.parser.OptionElement.Kind
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class FieldElementTest {
  internal var location = Location.get("file.proto")

  @Test
  fun field() {
    val field = FieldElement(
        location = location,
        label = OPTIONAL,
        type = "CType",
        name = "ctype",
        tag = 1,
        options = listOf(
            OptionElement.create("default", Kind.ENUM, "TEST"),
            OptionElement.create("deprecated", Kind.BOOLEAN, "true")
        )
    )

    assertThat(field.options)
        .containsOnly(
            OptionElement.create("default", Kind.ENUM, "TEST"),
            OptionElement.create("deprecated", Kind.BOOLEAN, "true")
        )
  }

  @Test
  fun addMultipleOptions() {
    val kitKat = OptionElement.create("kit", Kind.STRING, "kat")
    val fooBar = OptionElement.create("foo", Kind.STRING, "bar")
    val field = FieldElement(
        location = location,
        label = REQUIRED,
        type = "string",
        name = "name",
        tag = 1,
        options = listOf(kitKat, fooBar)
    )

    assertThat(field.options).hasSize(2)
  }

  @Test
  fun defaultIsSetInProto2File() {
    val field = FieldElement(
        location = location,
        label = REQUIRED,
        type = "string",
        name = "name",
        tag = 1,
        defaultValue = "defaultValue"
    )

    assertThat(field.toSchema(PROTO_2_SYNTAX_RULES))
        .isEqualTo("required string name = 1 [default = \"defaultValue\"];\n")
  }

  @Test
  fun defaultIsNotSetInProto3File() {
    val field = FieldElement(
        location = location,
        label = REQUIRED,
        type = "string",
        name = "name",
        tag = 1,
        defaultValue = "default value shouldn't be set"
    )

    assertThat(field.toSchema(PROTO_3_SYNTAX_RULES)).isEqualTo("required string name = 1;\n")
  }
}
