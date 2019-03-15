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
import com.google.common.collect.Range
import com.squareup.wire.schema.Field.Label.OPTIONAL
import com.squareup.wire.schema.Field.Label.REPEATED
import com.squareup.wire.schema.Field.Label.REQUIRED
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.internal.parser.OptionElement.Kind
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MessageElementTest {
  internal var location = Location.get("file.proto")
  @Test
  fun emptyToSchema() {
    val element = MessageElement.builder(location)
        .name("Message")
        .build()
    val expected = "message Message {}\n"
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun simpleToSchema() {
    val element = MessageElement.builder(location)
        .name("Message")
        .fields(
            ImmutableList.of(
                FieldElement.builder(location)
                    .label(REQUIRED)
                    .type("string")
                    .name("name")
                    .tag(1)
                    .build()
            )
        )
        .build()
    val expected = """
        |message Message {
        |  required string name = 1;
        |}
        |""".trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
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
    val element = MessageElement.builder(location)
        .name("Message")
        .fields(ImmutableList.of(firstName, lastName))
        .build()
    assertThat(element.fields()).hasSize(2)
  }

  @Test
  fun simpleWithDocumentationToSchema() {
    val element = MessageElement.builder(location)
        .name("Message")
        .documentation("Hello")
        .fields(
            ImmutableList.of(
                FieldElement.builder(location)
                    .label(REQUIRED)
                    .type("string")
                    .name("name")
                    .tag(1)
                    .build()
            )
        )
        .build()
    val expected = """
        |// Hello
        |message Message {
        |  required string name = 1;
        |}
        |""".trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun simpleWithOptionsToSchema() {
    val field = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("name")
        .tag(1)
        .build()
    val element = MessageElement.builder(location)
        .name("Message")
        .fields(ImmutableList.of(field))
        .options(
            ImmutableList.of(
                OptionElement.create("kit", Kind.STRING, "kat")
            )
        )
        .build()
    val expected =
        """message Message {
        |  option kit = "kat";
        |
        |  required string name = 1;
        |}
        |""".trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleOptions() {
    val field = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("name")
        .tag(1)
        .build()
    val kitKat = OptionElement.create("kit", Kind.STRING, "kat")
    val fooBar = OptionElement.create("foo", Kind.STRING, "bar")
    val element = MessageElement.builder(location)
        .name("Message")
        .fields(ImmutableList.of(field))
        .options(ImmutableList.of(kitKat, fooBar))
        .build()
    assertThat(element.options()).hasSize(2)
  }

  @Test
  fun simpleWithNestedElementsToSchema() {
    val element = MessageElement.builder(location)
        .name("Message")
        .fields(
            ImmutableList.of(
                FieldElement.builder(location)
                    .label(REQUIRED)
                    .type("string")
                    .name("name")
                    .tag(1)
                    .build()
            )
        )
        .nestedTypes(
            ImmutableList.of(
                MessageElement.builder(location)
                    .name("Nested")
                    .fields(
                        ImmutableList.of(
                            FieldElement.builder(location)
                                .label(REQUIRED)
                                .type("string")
                                .name("name")
                                .tag(1)
                                .build()
                        )
                    )
                    .build()
            )
        )
        .build()
    val expected = """
        |message Message {
        |  required string name = 1;
        |
        |  message Nested {
        |    required string name = 1;
        |  }
        |}
        |""".trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleTypes() {
    val nested1 = MessageElement.builder(location)
        .name("Nested1")
        .build()
    val nested2 = MessageElement.builder(location)
        .name("Nested2")
        .build()
    val element = MessageElement.builder(location)
        .name("Message")
        .fields(
            ImmutableList.of(
                FieldElement.builder(location)
                    .label(REQUIRED)
                    .type("string")
                    .name("name")
                    .tag(1)
                    .build()
            )
        )
        .nestedTypes(ImmutableList.of(nested1, nested2))
        .build()
    assertThat(element.nestedTypes()).hasSize(2)
  }

  @Test
  fun simpleWithExtensionsToSchema() {
    val element = MessageElement.builder(location)
        .name("Message")
        .fields(
            ImmutableList.of(
                FieldElement.builder(location)
                    .label(REQUIRED)
                    .type("string")
                    .name("name")
                    .tag(1)
                    .build()
            )
        )
        .extensions(
            ImmutableList.of(
                ExtensionsElement.create(location, 500, 501, "")
            )
        )
        .build()
    val expected = """
        |message Message {
        |  required string name = 1;
        |
        |  extensions 500 to 501;
        |}
        |""".trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleExtensions() {
    val fives = ExtensionsElement.create(location, 500, 501, "")
    val sixes = ExtensionsElement.create(location, 600, 601, "")
    val element = MessageElement.builder(location)
        .name("Message")
        .fields(
            ImmutableList.of(
                FieldElement.builder(location)
                    .label(REQUIRED)
                    .type("string")
                    .name("name")
                    .tag(1)
                    .build()
            )
        )
        .extensions(ImmutableList.of(fives, sixes))
        .build()
    assertThat(element.extensions()).hasSize(2)
  }

  @Test
  fun oneOfToSchema() {
    val element = MessageElement.builder(location)
        .name("Message")
        .oneOfs(
            ImmutableList.of(
                OneOfElement.builder()
                    .name("hi")
                    .fields(
                        ImmutableList.of(
                            FieldElement.builder(location)
                                .type("string")
                                .name("name")
                                .tag(1)
                                .build()
                        )
                    )
                    .build()
            )
        )
        .build()
    val expected = """
        |message Message {
        |  oneof hi {
        |    string name = 1;
        |  }
        |}
        |""".trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun oneOfWithGroupToSchema() {
    val element = MessageElement.builder(location)
        .name("Message")
        .oneOfs(
            ImmutableList.of(
                OneOfElement.builder()
                    .name("hi")
                    .fields(
                        ImmutableList.of(
                            FieldElement.builder(location)
                                .type("string")
                                .name("name")
                                .tag(1)
                                .build()
                        )
                    )
                    .groups(
                        ImmutableList.of(
                            GroupElement.builder(location.at(5, 5))
                                .name("Stuff")
                                .tag(3)
                                .fields(
                                    ImmutableList.of(
                                        FieldElement.builder(location.at(6, 7))
                                            .label(OPTIONAL)
                                            .type("int32")
                                            .name("result_per_page")
                                            .tag(4)
                                            .build(),
                                        FieldElement.builder(location.at(7, 7))
                                            .label(OPTIONAL)
                                            .type("int32")
                                            .name("page_count")
                                            .tag(5)
                                            .build()
                                    )
                                )
                                .build()
                        )
                    )
                    .build()
            )
        )
        .build()
    val expected = """
        |message Message {
        |  oneof hi {
        |    string name = 1;
        |
        |    group Stuff = 3 {
        |      optional int32 result_per_page = 4;
        |      optional int32 page_count = 5;
        |    }
        |  }
        |}
        |""".trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleOneOfs() {
    val hi = OneOfElement.builder()
        .name("hi")
        .fields(
            ImmutableList.of(
                FieldElement.builder(location)
                    .type("string")
                    .name("name")
                    .tag(1)
                    .build()
            )
        )
        .build()
    val hey = OneOfElement.builder()
        .name("hey")
        .fields(
            ImmutableList.of(
                FieldElement.builder(location)
                    .type("string")
                    .name("city")
                    .tag(2)
                    .build()
            )
        )
        .build()
    val element = MessageElement.builder(location)
        .name("Message")
        .oneOfs(ImmutableList.of(hi, hey))
        .build()
    assertThat(element.oneOfs()).hasSize(2)
  }

  @Test
  fun reservedToSchema() {
    val element = MessageElement.builder(location)
        .name("Message")
        .reserveds(
            ImmutableList.of(
                ReservedElement.create(
                    location, "", ImmutableList.of(10, Range.closed(12, 14), "foo")
                ),
                ReservedElement.create(location, "", ImmutableList.of(10)),
                ReservedElement.create(location, "", ImmutableList.of(Range.closed(12, 14))),
                ReservedElement.create(location, "", ImmutableList.of("foo"))
            )
        )
        .build()
    val expected = """
        |message Message {
        |  reserved 10, 12 to 14, "foo";
        |  reserved 10;
        |  reserved 12 to 14;
        |  reserved "foo";
        |}
        |""".trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun groupToSchema() {
    val element = MessageElement.builder(location.at(1, 1))
        .name("SearchResponse")
        .groups(
            ImmutableList.of(
                GroupElement.builder(location.at(2, 3))
                    .label(REPEATED)
                    .name("Result")
                    .tag(1)
                    .fields(
                        ImmutableList.of(
                            FieldElement.builder(location.at(3, 5))
                                .label(REQUIRED)
                                .type("string")
                                .name("url")
                                .tag(2)
                                .build(),
                            FieldElement.builder(location.at(4, 5))
                                .label(OPTIONAL)
                                .type("string")
                                .name("title")
                                .tag(3)
                                .build(),
                            FieldElement.builder(location.at(5, 5))
                                .label(REPEATED)
                                .type("string")
                                .name("snippets")
                                .tag(4)
                                .build()
                        )
                    )
                    .build()
            )
        )
        .build()
    val expected = """
        |message SearchResponse {
        |  repeated group Result = 1 {
        |    required string url = 2;
        |    optional string title = 3;
        |    repeated string snippets = 4;
        |  }
        |}
        |""".trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun multipleEverythingToSchema() {
    val field1 = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("name")
        .tag(1)
        .build()
    val field2 = FieldElement.builder(location)
        .label(REQUIRED)
        .type("bool")
        .name("other_name")
        .tag(2)
        .build()
    val oneOf1Field = FieldElement.builder(location)
        .type("string")
        .name("namey")
        .tag(3)
        .build()
    val oneOf1 = OneOfElement.builder()
        .name("thingy")
        .fields(ImmutableList.of(oneOf1Field))
        .build()
    val oneOf2Field = FieldElement.builder(location)
        .type("string")
        .name("namer")
        .tag(4)
        .build()
    val oneOf2 = OneOfElement.builder()
        .name("thinger")
        .fields(ImmutableList.of(oneOf2Field))
        .build()
    val extensions1 = ExtensionsElement.create(location, 500, 501, "")
    val extensions2 = ExtensionsElement.create(location, 503, 503, "")
    val nested = MessageElement.builder(location)
        .name("Nested")
        .fields(ImmutableList.of(field1))
        .build()
    val option = OptionElement.create("kit", Kind.STRING, "kat")
    val element = MessageElement.builder(location)
        .name("Message")
        .fields(ImmutableList.of(field1, field2))
        .oneOfs(ImmutableList.of(oneOf1, oneOf2))
        .nestedTypes(ImmutableList.of(nested))
        .extensions(ImmutableList.of(extensions1, extensions2))
        .options(ImmutableList.of(option))
        .build()
    val expected = """
        |message Message {
        |  option kit = "kat";
        |
        |  required string name = 1;
        |  required bool other_name = 2;
        |
        |  oneof thingy {
        |    string namey = 3;
        |  }
        |  oneof thinger {
        |    string namer = 4;
        |  }
        |
        |  extensions 500 to 501;
        |  extensions 503;
        |
        |  message Nested {
        |    required string name = 1;
        |  }
        |}
        |""".trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun fieldToSchema() {
    val field = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("name")
        .tag(1)
        .build()
    val expected = "required string name = 1;\n"
    assertThat(field.toSchema()).isEqualTo(expected)
  }

  @Test
  fun oneOfFieldToSchema() {
    val field = FieldElement.builder(location)
        .type("string")
        .name("name")
        .tag(1)
        .build()
    val expected = "string name = 1;\n"
    assertThat(field.toSchema()).isEqualTo(expected)
  }

  @Test
  fun fieldWithDocumentationToSchema() {
    val field = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("name")
        .tag(1)
        .documentation("Hello")
        .build()
    val expected =
        """// Hello
        |required string name = 1;
        |""".trimMargin()
    assertThat(field.toSchema()).isEqualTo(expected)
  }

  @Test
  fun fieldWithOptionsToSchema() {
    val field = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("name")
        .tag(1)
        .options(ImmutableList.of(OptionElement.create("kit", Kind.STRING, "kat")))
        .build()
    val expected =
        """required string name = 1 [
        |  kit = "kat"
        |];
        |""".trimMargin()
    assertThat(field.toSchema()).isEqualTo(expected)
  }
}
