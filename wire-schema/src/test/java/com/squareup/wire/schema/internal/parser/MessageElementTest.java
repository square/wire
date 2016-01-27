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
import com.google.common.collect.Range;
import com.squareup.wire.schema.internal.parser.OptionElement.Kind;
import com.squareup.wire.schema.Location;
import org.junit.Test;

import static com.squareup.wire.schema.Field.Label.OPTIONAL;
import static com.squareup.wire.schema.Field.Label.REPEATED;
import static com.squareup.wire.schema.Field.Label.REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;

public final class MessageElementTest {
  Location location = Location.get("file.proto");

  @Test public void emptyToSchema() {
    TypeElement element = MessageElement.builder(location).name("Message").build();
    String expected = "message Message {}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void simpleToSchema() {
    TypeElement element = MessageElement.builder(location)
        .name("Message")
        .fields(ImmutableList.of(
            FieldElement.builder(location)
                .label(REQUIRED)
                .type("string")
                .name("name")
                .tag(1)
                .build()))
        .build();
    String expected = ""
        + "message Message {\n"
        + "  required string name = 1;\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleFields() {
    FieldElement firstName = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("first_name")
        .tag(1)
        .build();
    FieldElement lastName = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("last_name")
        .tag(2)
        .build();
    MessageElement element = MessageElement.builder(location)
        .name("Message")
        .fields(ImmutableList.of(firstName, lastName))
        .build();
    assertThat(element.fields()).hasSize(2);
  }

  @Test public void simpleWithDocumentationToSchema() {
    TypeElement element = MessageElement.builder(location)
        .name("Message")
        .documentation("Hello")
        .fields(ImmutableList.of(
            FieldElement.builder(location)
                .label(REQUIRED)
                .type("string")
                .name("name")
                .tag(1)
                .build()))
        .build();
    String expected = ""
        + "// Hello\n"
        + "message Message {\n"
        + "  required string name = 1;\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void simpleWithOptionsToSchema() {
    FieldElement field = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("name")
        .tag(1)
        .build();
    TypeElement element = MessageElement.builder(location)
        .name("Message")
        .fields(ImmutableList.of(field))
        .options(ImmutableList.of(
            OptionElement.create("kit", Kind.STRING, "kat")))
        .build();
    String expected = ""
        + "message Message {\n"
        + "  option kit = \"kat\";\n"
        + "\n"
        + "  required string name = 1;\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleOptions() {
    FieldElement field = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("name")
        .tag(1)
        .build();
    OptionElement kitKat = OptionElement.create("kit", Kind.STRING, "kat");
    OptionElement fooBar = OptionElement.create("foo", Kind.STRING, "bar");
    MessageElement element = MessageElement.builder(location)
        .name("Message")
        .fields(ImmutableList.of(field))
        .options(ImmutableList.of(kitKat, fooBar))
        .build();
    assertThat(element.options()).hasSize(2);
  }

  @Test public void simpleWithNestedElementsToSchema() {
    TypeElement element = MessageElement.builder(location)
        .name("Message")
        .fields(ImmutableList.of(FieldElement.builder(location)
            .label(REQUIRED)
            .type("string")
            .name("name")
            .tag(1)
            .build()))
        .nestedTypes(ImmutableList.<TypeElement>of(
            MessageElement.builder(location)
                .name("Nested")
                .fields(ImmutableList.of(
                    FieldElement.builder(location)
                        .label(REQUIRED)
                        .type("string")
                        .name("name")
                        .tag(1)
                        .build()))
                .build()))
        .build();
    String expected = ""
        + "message Message {\n"
        + "  required string name = 1;\n"
        + "\n"
        + "  message Nested {\n"
        + "    required string name = 1;\n"
        + "  }\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleTypes() {
    TypeElement nested1 = MessageElement.builder(location).name("Nested1").build();
    TypeElement nested2 = MessageElement.builder(location).name("Nested2").build();
    TypeElement element = MessageElement.builder(location)
        .name("Message")
        .fields(ImmutableList.of(FieldElement.builder(location)
            .label(REQUIRED)
            .type("string")
            .name("name")
            .tag(1)
            .build()))
        .nestedTypes(ImmutableList.of(nested1, nested2))
        .build();
    assertThat(element.nestedTypes()).hasSize(2);
  }

  @Test public void simpleWithExtensionsToSchema() {
    TypeElement element = MessageElement.builder(location)
        .name("Message")
        .fields(ImmutableList.of(FieldElement.builder(location)
            .label(REQUIRED)
            .type("string")
            .name("name")
            .tag(1)
            .build()))
        .extensions(ImmutableList.of(
            ExtensionsElement.create(location, 500, 501, "")))
        .build();
    String expected = ""
        + "message Message {\n"
        + "  required string name = 1;\n"
        + "\n"
        + "  extensions 500 to 501;\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleExtensions() {
    ExtensionsElement fives = ExtensionsElement.create(location, 500, 501, "");
    ExtensionsElement sixes = ExtensionsElement.create(location, 600, 601, "");
    MessageElement element = MessageElement.builder(location)
        .name("Message")
        .fields(ImmutableList.of(
            FieldElement.builder(location)
                .label(REQUIRED)
                .type("string")
                .name("name")
                .tag(1)
                .build()))
        .extensions(ImmutableList.of(fives, sixes))
        .build();
    assertThat(element.extensions()).hasSize(2);
  }

  @Test public void oneOfToSchema() {
    TypeElement element = MessageElement.builder(location)
        .name("Message")
        .oneOfs(ImmutableList.of(
            OneOfElement.builder()
                .name("hi")
                .fields(ImmutableList.of(
                    FieldElement.builder(location)
                        .type("string")
                        .name("name")
                        .tag(1)
                        .build()))
                .build()))
        .build();
    String expected = ""
        + "message Message {\n"
        + "  oneof hi {\n"
        + "    string name = 1;\n"
        + "  }\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void oneOfWithGroupToSchema() {
    TypeElement element = MessageElement.builder(location)
        .name("Message")
        .oneOfs(ImmutableList.of(
            OneOfElement.builder()
                .name("hi")
                .fields(ImmutableList.of(
                    FieldElement.builder(location)
                        .type("string")
                        .name("name")
                        .tag(1)
                        .build()))
            .groups(ImmutableList.of(GroupElement.builder()
                .name("Stuff")
                .tag(3)
                .fields(ImmutableList.of(
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
                ))
                .build()))
            .build()))
        .build();
    String expected = ""
        + "message Message {\n"
        + "  oneof hi {\n"
        + "    string name = 1;\n"
        + "  \n"
        + "    group Stuff = 3 {\n"
        + "      optional int32 result_per_page = 4;\n"
        + "      optional int32 page_count = 5;\n"
        + "    }\n"
        + "  }\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleOneOfs() {
    OneOfElement hi = OneOfElement.builder()
        .name("hi")
        .fields(ImmutableList.of(
            FieldElement.builder(location)
                .type("string")
                .name("name")
                .tag(1)
                .build()))
        .build();
    OneOfElement hey = OneOfElement.builder()
        .name("hey")
        .fields(ImmutableList.of(
            FieldElement.builder(location)
                .type("string")
                .name("city")
                .tag(2)
                .build()))
        .build();
    MessageElement element = MessageElement.builder(location)
        .name("Message")
        .oneOfs(ImmutableList.of(hi, hey))
        .build();
    assertThat(element.oneOfs()).hasSize(2);
  }

  @Test public void reservedToSchema() {
    TypeElement element = MessageElement.builder(location)
        .name("Message")
        .reserveds(ImmutableList.of(
            ReservedElement.create(location, "", ImmutableList.<Object>of(10, Range.closed(12, 14), "foo")),
            ReservedElement.create(location, "", ImmutableList.<Object>of(10)),
            ReservedElement.create(location, "", ImmutableList.<Object>of(Range.closed(12, 14))),
            ReservedElement.create(location, "", ImmutableList.<Object>of("foo"))))
        .build();
    String expected = ""
        + "message Message {\n"
        + "  reserved 10, 12 to 14, \"foo\";\n"
        + "  reserved 10;\n"
        + "  reserved 12 to 14;\n"
        + "  reserved \"foo\";\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void groupToSchema() {
    TypeElement element = MessageElement.builder(location.at(1, 1))
        .name("SearchResponse")
        .groups(ImmutableList.of(
            GroupElement.builder()
                .label(REPEATED)
                .name("Result")
                .tag(1)
                .fields(ImmutableList.of(
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
                ))
                .build()
        ))
        .build();
    String expected = ""
        + "message SearchResponse {\n"
        + "  repeated group Result = 1 {\n"
        + "    required string url = 2;\n"
        + "    optional string title = 3;\n"
        + "    repeated string snippets = 4;\n"
        + "  }\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void multipleEverythingToSchema() {
    FieldElement field1 = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("name")
        .tag(1)
        .build();
    FieldElement field2 = FieldElement.builder(location)
        .label(REQUIRED)
        .type("bool")
        .name("other_name")
        .tag(2)
        .build();
    FieldElement oneOf1Field = FieldElement.builder(location)
        .type("string")
        .name("namey")
        .tag(3)
        .build();
    OneOfElement oneOf1 = OneOfElement.builder()
        .name("thingy")
        .fields(ImmutableList.of(oneOf1Field))
        .build();
    FieldElement oneOf2Field = FieldElement.builder(location)
        .type("string")
        .name("namer")
        .tag(4)
        .build();
    OneOfElement oneOf2 = OneOfElement.builder()
        .name("thinger")
        .fields(ImmutableList.of(oneOf2Field))
        .build();
    ExtensionsElement extensions1 = ExtensionsElement.create(location, 500, 501, "");
    ExtensionsElement extensions2 = ExtensionsElement.create(location, 503, 503, "");
    TypeElement nested = MessageElement.builder(location)
        .name("Nested")
        .fields(ImmutableList.of(field1))
        .build();
    OptionElement option = OptionElement.create("kit", Kind.STRING, "kat");
    TypeElement element = MessageElement.builder(location)
        .name("Message")
        .fields(ImmutableList.of(field1, field2))
        .oneOfs(ImmutableList.of(oneOf1, oneOf2))
        .nestedTypes(ImmutableList.of(nested))
        .extensions(ImmutableList.of(extensions1, extensions2))
        .options(ImmutableList.of(option))
        .build();
    String expected = ""
        + "message Message {\n"
        + "  option kit = \"kat\";\n"
        + "\n"
        + "  required string name = 1;\n"
        + "  required bool other_name = 2;\n"
        + "\n"
        + "  oneof thingy {\n"
        + "    string namey = 3;\n"
        + "  }\n"
        + "  oneof thinger {\n"
        + "    string namer = 4;\n"
        + "  }\n"
        + "\n"
        + "  extensions 500 to 501;\n"
        + "  extensions 503;\n"
        + "\n"
        + "  message Nested {\n"
        + "    required string name = 1;\n"
        + "  }\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void fieldToSchema() {
    FieldElement field = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("name")
        .tag(1)
        .build();
    String expected = "required string name = 1;\n";
    assertThat(field.toSchema()).isEqualTo(expected);
  }

  @Test public void oneOfFieldToSchema() {
    FieldElement field = FieldElement.builder(location)
        .type("string")
        .name("name")
        .tag(1)
        .build();
    String expected = "string name = 1;\n";
    assertThat(field.toSchema()).isEqualTo(expected);
  }

  @Test public void fieldWithDocumentationToSchema() {
    FieldElement field = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("name")
        .tag(1)
        .documentation("Hello")
        .build();
    String expected = ""
        + "// Hello\n"
        + "required string name = 1;\n";
    assertThat(field.toSchema()).isEqualTo(expected);
  }

  @Test public void fieldWithOptionsToSchema() {
    FieldElement field = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("name")
        .tag(1)
        .options(ImmutableList.of(OptionElement.create("kit", Kind.STRING, "kat")))
        .build();
    String expected = "required string name = 1 [\n"
        + "  kit = \"kat\"\n"
        + "];\n";
    assertThat(field.toSchema()).isEqualTo(expected);
  }
}
