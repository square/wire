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
import com.squareup.wire.schema.Field.Label.OPTIONAL
import com.squareup.wire.schema.Field.Label.REPEATED
import com.squareup.wire.schema.Field.Label.REQUIRED
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.internal.MAX_TAG_VALUE
import com.squareup.wire.schema.internal.parser.OptionElement.Kind
import kotlin.test.Test

class MessageElementTest {
  internal var location = Location.get("file.proto")

  @Test
  fun emptyToSchema() {
    val element = MessageElement(
      location = location,
      name = "Message",
    )
    val expected = "message Message {}\n"
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun simpleToSchema() {
    val element = MessageElement(
      location = location,
      name = "Message",
      fields = listOf(
        FieldElement(
          location = location,
          label = REQUIRED,
          type = "string",
          name = "name",
          tag = 1,
        ),
      ),
    )
    val expected = """
        |message Message {
        |  required string name = 1;
        |}
        |
    """.trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleFields() {
    val firstName = FieldElement(
      location = location,
      label = REQUIRED,
      type = "string",
      name = "first_name",
      tag = 1,
    )
    val lastName = FieldElement(
      location = location,
      label = REQUIRED,
      type = "string",
      name = "last_name",
      tag = 2,
    )
    val element = MessageElement(
      location = location,
      name = "Message",
      fields = listOf(firstName, lastName),
    )
    assertThat(element.fields).hasSize(2)
  }

  @Test
  fun simpleWithDocumentationToSchema() {
    val element = MessageElement(
      location = location,
      name = "Message",
      documentation = "Hello",
      fields = listOf(
        FieldElement(
          location = location,
          label = REQUIRED,
          type = "string",
          name = "name",
          tag = 1,
        ),
      ),
    )
    val expected = """
        |// Hello
        |message Message {
        |  required string name = 1;
        |}
        |
    """.trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun simpleWithOptionsToSchema() {
    val field = FieldElement(
      location = location,
      label = REQUIRED,
      type = "string",
      name = "name",
      tag = 1,
    )
    val element = MessageElement(
      location = location,
      name = "Message",
      fields = listOf(field),
      options = listOf(OptionElement.create("kit", Kind.STRING, "kat")),
    )
    val expected =
      """message Message {
        |  option kit = "kat";
        |
        |  required string name = 1;
        |}
        |
      """.trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleOptions() {
    val field = FieldElement(
      location = location,
      label = REQUIRED,
      type = "string",
      name = "name",
      tag = 1,
    )
    val kitKat = OptionElement.create("kit", Kind.STRING, "kat")
    val fooBar = OptionElement.create("foo", Kind.STRING, "bar")
    val element = MessageElement(
      location = location,
      name = "Message",
      fields = listOf(field),
      options = listOf(kitKat, fooBar),
    )
    assertThat(element.options).hasSize(2)
  }

  @Test
  fun simpleWithNestedElementsToSchema() {
    val element = MessageElement(
      location = location,
      name = "Message",
      fields = listOf(
        FieldElement(
          location = location,
          label = REQUIRED,
          type = "string",
          name = "name",
          tag = 1,
        ),
      ),
      nestedTypes = listOf(
        MessageElement(
          location = location,
          name = "Nested",
          fields = listOf(
            FieldElement(
              location = location,
              label = REQUIRED,
              type = "string",
              name = "name",
              tag = 1,
            ),
          ),
        ),
      ),
    )
    val expected = """
        |message Message {
        |  required string name = 1;
        |
        |  message Nested {
        |    required string name = 1;
        |  }
        |}
        |
    """.trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleTypes() {
    val nested1 = MessageElement(
      location = location,
      name = "Nested1",
    )
    val nested2 = MessageElement(
      location = location,
      name = "Nested2",
    )
    val element = MessageElement(
      location = location,
      name = "Message",
      fields = listOf(
        FieldElement(
          location = location,
          label = REQUIRED,
          type = "string",
          name = "name",
          tag = 1,
        ),
      ),
      nestedTypes = listOf(nested1, nested2),
    )
    assertThat(element.nestedTypes).hasSize(2)
  }

  @Test
  fun simpleWithExtensionsToSchema() {
    val element = MessageElement(
      location = location,
      name = "Message",
      fields = listOf(
        FieldElement(
          location = location,
          label = REQUIRED,
          type = "string",
          name = "name",
          tag = 1,
        ),
      ),
      extensions = listOf(ExtensionsElement(location = location, values = listOf(500..501))),
    )
    val expected = """
        |message Message {
        |  required string name = 1;
        |
        |  extensions 500 to 501;
        |}
        |
    """.trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleExtensions() {
    val fives = ExtensionsElement(location = location, values = listOf(500..501))
    val sixes = ExtensionsElement(location = location, values = listOf(600..601))
    val element = MessageElement(
      location = location,
      name = "Message",
      fields = listOf(
        FieldElement(
          location = location,
          label = REQUIRED,
          type = "string",
          name = "name",
          tag = 1,
        ),
      ),
      extensions = listOf(fives, sixes),
    )
    assertThat(element.extensions).hasSize(2)
  }

  @Test
  fun oneOfToSchema() {
    val element = MessageElement(
      location = location,
      name = "Message",
      oneOfs = listOf(
        OneOfElement(
          location = location,
          name = "hi",
          fields = listOf(
            FieldElement(
              location = location,
              type = "string",
              name = "name",
              tag = 1,
            ),
          ),
        ),
      ),
    )
    val expected = """
        |message Message {
        |  oneof hi {
        |    string name = 1;
        |  }
        |}
        |
    """.trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun oneOfWithGroupToSchema() {
    val element = MessageElement(
      location = location,
      name = "Message",
      oneOfs = listOf(
        OneOfElement(
          location = location,
          name = "hi",
          fields = listOf(
            FieldElement(
              location = location,
              type = "string",
              name = "name",
              tag = 1,
            ),
          ),
          groups = listOf(
            GroupElement(
              location = location.at(5, 5),
              name = "Stuff",
              tag = 3,
              fields = listOf(
                FieldElement(
                  location = location.at(6, 7),
                  label = OPTIONAL,
                  type = "int32",
                  name = "result_per_page",
                  tag = 4,
                ),
                FieldElement(
                  location = location.at(7, 7),
                  label = OPTIONAL,
                  type = "int32",
                  name = "page_count",
                  tag = 5,
                ),
              ),
            ),
          ),
        ),
      ),
    )
    // spotless:off because spotless will remove the indents (trailing spaces) in the oneof block.
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
        |
    """.trimMargin()
    // spotless:on
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleOneOfs() {
    val hi = OneOfElement(
      location = location,
      name = "hi",
      fields = listOf(
        FieldElement(
          location = location,
          type = "string",
          name = "name",
          tag = 1,
        ),
      ),
    )
    val hey = OneOfElement(
      location = location,
      name = "hey",
      fields = listOf(
        FieldElement(
          location = location,
          type = "string",
          name = "city",
          tag = 2,
        ),
      ),
    )
    val element = MessageElement(
      location = location,
      name = "Message",
      oneOfs = listOf(hi, hey),
    )
    assertThat(element.oneOfs).hasSize(2)
  }

  @Test
  fun reservedToSchema() {
    val element = MessageElement(
      location = location,
      name = "Message",
      reserveds = listOf(
        ReservedElement(location = location, values = listOf(10, 12..14, "foo")),
        ReservedElement(location = location, values = listOf(10)),
        ReservedElement(location = location, values = listOf(12..MAX_TAG_VALUE)),
        ReservedElement(location = location, values = listOf("foo")),
      ),
    )
    val expected = """
        |message Message {
        |  reserved 10, 12 to 14, "foo";
        |  reserved 10;
        |  reserved 12 to max;
        |  reserved "foo";
        |}
        |
    """.trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun groupToSchema() {
    val element = MessageElement(
      location = location.at(1, 1),
      name = "SearchResponse",
      groups = listOf(
        GroupElement(
          location = location.at(2, 3),
          label = REPEATED,
          name = "Result",
          tag = 1,
          fields = listOf(
            FieldElement(
              location = location.at(3, 5),
              label = REQUIRED,
              type = "string",
              name = "url",
              tag = 2,
            ),
            FieldElement(
              location = location.at(4, 5),
              label = OPTIONAL,
              type = "string",
              name = "title",
              tag = 3,
            ),
            FieldElement(
              location = location.at(5, 5),
              label = REPEATED,
              type = "string",
              name = "snippets",
              tag = 4,
            ),
          ),
        ),
      ),
    )
    val expected = """
        |message SearchResponse {
        |  repeated group Result = 1 {
        |    required string url = 2;
        |    optional string title = 3;
        |    repeated string snippets = 4;
        |  }
        |}
        |
    """.trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun multipleEverythingToSchema() {
    val field1 = FieldElement(
      location = location.at(1, 2),
      label = REQUIRED,
      type = "string",
      name = "name",
      tag = 2,
    )
    val oneOf1Field1 = FieldElement(
      location = location.at(1, 1),
      type = "string",
      name = "namey",
      tag = 1,
    )
    val oneOf1Field2 = FieldElement(
      location = location.at(2, 1),
      type = "int32",
      name = "aField",
      tag = 5,
    )

    val oneOf1 = OneOfElement(
      location = location,
      name = "thingy",
      fields = listOf(oneOf1Field1, oneOf1Field2),
    )
    val field2 = FieldElement(
      location = location.at(2, 3),
      label = REQUIRED,
      type = "bool",
      name = "other_name",
      tag = 3,
    )
    val oneOf2Field = FieldElement(
      location = location.at(3, 0),
      type = "string",
      name = "namer",
      tag = 4,
    )
    val oneOf2 = OneOfElement(
      location = location,
      name = "thinger",
      fields = listOf(oneOf2Field),
    )
    val extensions1 = ExtensionsElement(location = location.at(5, 0), values = listOf(500..501))
    val extensions2 = ExtensionsElement(location = location.at(6, 2), values = listOf(503))
    val nested = MessageElement(
      location = location.at(7, 1),
      name = "Nested",
      fields = listOf(field1),
    )
    val option = OptionElement.create("kit", Kind.STRING, "kat")
    val element = MessageElement(
      location = location.at(0, 0),
      name = "Message",
      fields = listOf(field1, field2),
      oneOfs = listOf(oneOf1, oneOf2),
      nestedTypes = listOf(nested),
      extensions = listOf(extensions1, extensions2),
      options = listOf(option),
    )
    val expected = """
        |message Message {
        |  option kit = "kat";
        |
        |  oneof thingy {
        |    string namey = 1;
        |    int32 aField = 5;
        |  }
        |
        |  required string name = 2;
        |
        |  required bool other_name = 3;
        |
        |  oneof thinger {
        |    string namer = 4;
        |  }
        |
        |  extensions 500 to 501;
        |  extensions 503;
        |
        |  message Nested {
        |    required string name = 2;
        |  }
        |}
        |
    """.trimMargin()
    assertThat(element.toSchema()).isEqualTo(expected)
  }

  @Test
  fun fieldToSchema() {
    val field = FieldElement(
      location = location,
      label = REQUIRED,
      type = "string",
      name = "name",
      tag = 1,
    )
    val expected = "required string name = 1;\n"
    assertThat(field.toSchema()).isEqualTo(expected)
  }

  @Test
  fun fieldWithDefaultStringToSchemaInProto2() {
    val field = FieldElement(
      location = location,
      label = REQUIRED,
      type = "string",
      name = "name",
      tag = 1,
      defaultValue = "benoît",
    )
    val expected = "required string name = 1 [default = \"benoît\"];\n"
    assertThat(field.toSchema()).isEqualTo(expected)
  }

  @Test
  fun fieldWithDefaultNumberToSchema() {
    val field = FieldElement(
      location = location,
      label = REQUIRED,
      type = "int32",
      name = "age",
      tag = 1,
      defaultValue = "34",
    )
    val expected = "required int32 age = 1 [default = 34];\n"
    assertThat(field.toSchema()).isEqualTo(expected)
  }

  @Test
  fun fieldWithDefaultBoolToSchema() {
    val field = FieldElement(
      location = location,
      label = REQUIRED,
      type = "bool",
      name = "human",
      tag = 1,
      defaultValue = "true",
    )
    val expected = "required bool human = 1 [default = true];\n"
    assertThat(field.toSchema()).isEqualTo(expected)
  }

  @Test
  fun oneOfFieldToSchema() {
    val field = FieldElement(
      location = location,
      type = "string",
      name = "name",
      tag = 1,
    )
    val expected = "string name = 1;\n"
    assertThat(field.toSchema()).isEqualTo(expected)
  }

  @Test
  fun fieldWithDocumentationToSchema() {
    val field = FieldElement(
      location = location,
      label = REQUIRED,
      type = "string",
      name = "name",
      tag = 1,
      documentation = "Hello",
    )
    val expected =
      """// Hello
        |required string name = 1;
        |
      """.trimMargin()
    assertThat(field.toSchema()).isEqualTo(expected)
  }

  @Test
  fun fieldWithOneOptionToSchema() {
    val field = FieldElement(
      location = location,
      label = REQUIRED,
      type = "string",
      name = "name",
      tag = 1,
      options = listOf(OptionElement.create("kit", Kind.STRING, "kat")),
    )
    val expected =
      """required string name = 1 [kit = "kat"];
        |
      """.trimMargin()
    assertThat(field.toSchema()).isEqualTo(expected)
  }

  @Test
  fun fieldWithMoreThanOneOptionToSchema() {
    val field = FieldElement(
      location = location,
      label = REQUIRED,
      type = "string",
      name = "name",
      tag = 1,
      options = listOf(
        OptionElement.create("kit", Kind.STRING, "kat"),
        OptionElement.create("dup", Kind.STRING, "lo"),
      ),
    )
    val expected =
      """required string name = 1 [
        |  kit = "kat",
        |  dup = "lo"
        |];
        |
      """.trimMargin()
    assertThat(field.toSchema()).isEqualTo(expected)
  }

  @Test fun oneOfWithOptions() {
    val expected = """
        |oneof page_info {
        |  option (my_option) = true;
        |
        |  int32 page_number = 2;
        |  int32 result_per_page = 3;
        |}
        |
    """.trimMargin()
    val oneOf = OneOfElement(
      location = location,
      name = "page_info",
      fields = listOf(
        FieldElement(
          location = location.at(4, 5),
          type = "int32",
          name = "page_number",
          tag = 2,
        ),
        FieldElement(
          location = location.at(5, 5),
          type = "int32",
          name = "result_per_page",
          tag = 3,
        ),
      ),
      options = listOf(
        OptionElement.create("my_option", Kind.BOOLEAN, "true", true),
      ),
    )
    assertThat(oneOf.toSchema()).isEqualTo(expected)
  }
}
