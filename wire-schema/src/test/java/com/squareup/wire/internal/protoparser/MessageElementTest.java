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
package com.squareup.wire.internal.protoparser;

import com.squareup.wire.internal.protoparser.OptionElement.Kind;
import com.squareup.wire.schema.Location;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.Test;

import static com.squareup.wire.schema.Field.Label.ONE_OF;
import static com.squareup.wire.schema.Field.Label.REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class MessageElementTest {
  Location location = Location.get("file.proto");

  @Test public void locationRequired() {
    try {
      MessageElement.builder(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("location");
    }
  }

  @Test public void nameRequired() {
    try {
      MessageElement.builder(location).qualifiedName("Test").build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
  }

  @Test public void nameSetsQualifiedName() {
    MessageElement test = MessageElement.builder(location).name("Test").build();
    assertThat(test.name()).isEqualTo("Test");
    assertThat(test.qualifiedName()).isEqualTo("Test");
  }

  @Test public void nullBuilderValuesThrow() {
    try {
      MessageElement.builder(location).name(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
    try {
      MessageElement.builder(location).qualifiedName(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("qualifiedName");
    }
    try {
      MessageElement.builder(location).documentation(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("documentation");
    }
    try {
      MessageElement.builder(location).addField(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("field");
    }
    try {
      MessageElement.builder(location).addFields(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("fields");
    }
    try {
      MessageElement.builder(location).addFields(Collections.<FieldElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("field");
    }
    try {
      MessageElement.builder(location).addType(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("type");
    }
    try {
      MessageElement.builder(location).addTypes(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("types");
    }
    try {
      MessageElement.builder(location).addTypes(Collections.<TypeElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("type");
    }
    try {
      MessageElement.builder(location).addOneOf(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("oneOf");
    }
    try {
      MessageElement.builder(location).addOneOfs(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("oneOfs");
    }
    try {
      MessageElement.builder(location).addOneOfs(Collections.<OneOfElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("oneOf");
    }
    try {
      MessageElement.builder(location).addExtensions((ExtensionsElement) null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("extensions");
    }
    try {
      MessageElement.builder(location).addExtensions((Collection<ExtensionsElement>) null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("extensions");
    }
    try {
      MessageElement.builder(location).addExtensions(Collections.<ExtensionsElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("extensions");
    }
    try {
      MessageElement.builder(location).addOption(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option");
    }
    try {
      MessageElement.builder(location).addOptions(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("options");
    }
    try {
      MessageElement.builder(location).addOptions(Collections.<OptionElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option");
    }
  }

  @Test public void emptyToSchema() {
    TypeElement element = MessageElement.builder(location).name("Message").build();
    String expected = "message Message {}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void simpleToSchema() {
    TypeElement element = MessageElement.builder(location)
        .name("Message")
        .addField(FieldElement.builder(location)
            .label(REQUIRED)
            .type("string")
            .name("name")
            .tag(1)
            .build())
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
        .addFields(Arrays.asList(firstName, lastName))
        .build();
    assertThat(element.fields()).hasSize(2);
  }

  @Test public void simpleWithDocumentationToSchema() {
    TypeElement element = MessageElement.builder(location)
        .name("Message")
        .documentation("Hello")
        .addField(FieldElement.builder(location)
            .label(REQUIRED)
            .type("string")
            .name("name")
            .tag(1)
            .build())
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
        .addField(field)
        .addOption(OptionElement.create("kit", Kind.STRING, "kat"))
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
        .addField(field)
        .addOptions(Arrays.asList(kitKat, fooBar))
        .build();
    assertThat(element.options()).hasSize(2);
  }

  @Test public void simpleWithNestedElementsToSchema() {
    TypeElement element = MessageElement.builder(location)
        .name("Message")
        .addField(FieldElement.builder(location)
            .label(REQUIRED)
            .type("string")
            .name("name")
            .tag(1)
            .build())
        .addType(MessageElement.builder(location)
            .name("Nested")
            .addField(FieldElement.builder(location)
                .label(REQUIRED)
                .type("string")
                .name("name")
                .tag(1)
                .build())
            .build())
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
        .addField(FieldElement.builder(location)
            .label(REQUIRED)
            .type("string")
            .name("name")
            .tag(1)
            .build())
        .addTypes(Arrays.asList(nested1, nested2))
        .build();
    assertThat(element.nestedElements()).hasSize(2);
  }

  @Test public void simpleWithExtensionsToSchema() {
    TypeElement element = MessageElement.builder(location)
        .name("Message")
        .addField(FieldElement.builder(location)
            .label(REQUIRED)
            .type("string")
            .name("name")
            .tag(1)
            .build())
        .addExtensions(ExtensionsElement.create(location, 500, 501))
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
    ExtensionsElement fives = ExtensionsElement.create(location, 500, 501);
    ExtensionsElement sixes = ExtensionsElement.create(location, 600, 601);
    MessageElement element = MessageElement.builder(location)
        .name("Message")
        .addField(FieldElement.builder(location)
            .label(REQUIRED)
            .type("string")
            .name("name")
            .tag(1)
            .build())
        .addExtensions(Arrays.asList(fives, sixes))
        .build();
    assertThat(element.extensions()).hasSize(2);
  }

  @Test public void oneOfToSchema() {
    TypeElement element = MessageElement.builder(location)
        .name("Message")
        .addOneOf(OneOfElement.builder()
            .name("hi")
            .addField(FieldElement.builder(location)
                .label(ONE_OF)
                .type("string")
                .name("name")
                .tag(1)
                .build())
            .build())
        .build();
    String expected = ""
        + "message Message {\n"
        + "  oneof hi {\n"
        + "    string name = 1;\n"
        + "  }\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleOneOfs() {
    OneOfElement hi = OneOfElement.builder()
        .name("hi")
        .addField(FieldElement.builder(location)
            .label(ONE_OF)
            .type("string")
            .name("name")
            .tag(1)
            .build())
        .build();
    OneOfElement hey = OneOfElement.builder()
        .name("hey")
        .addField(FieldElement.builder(location)
            .label(ONE_OF)
            .type("string")
            .name("city")
            .tag(2)
            .build())
        .build();
    MessageElement element = MessageElement.builder(location)
        .name("Message")
        .addOneOfs(Arrays.asList(hi, hey))
        .build();
    assertThat(element.oneOfs()).hasSize(2);
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
        .label(ONE_OF)
        .type("string")
        .name("namey")
        .tag(3)
        .build();
    OneOfElement oneOf1 = OneOfElement.builder()
        .name("thingy")
        .addField(oneOf1Field)
        .build();
    FieldElement oneOf2Field = FieldElement.builder(location)
        .label(ONE_OF)
        .type("string")
        .name("namer")
        .tag(4)
        .build();
    OneOfElement oneOf2 = OneOfElement.builder()
        .name("thinger")
        .addField(oneOf2Field)
        .build();
    ExtensionsElement extensions1 = ExtensionsElement.create(location, 500, 501);
    ExtensionsElement extensions2 = ExtensionsElement.create(location, 503, 503);
    TypeElement nested = MessageElement.builder(location).name("Nested").addField(field1).build();
    OptionElement option = OptionElement.create("kit", Kind.STRING, "kat");
    TypeElement element = MessageElement.builder(location)
        .name("Message")
        .addField(field1)
        .addField(field2)
        .addOneOf(oneOf1)
        .addOneOf(oneOf2)
        .addType(nested)
        .addExtensions(extensions1)
        .addExtensions(extensions2)
        .addOption(option)
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
        .label(ONE_OF)
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
        .addOption(OptionElement.create("kit", Kind.STRING, "kat"))
        .build();
    String expected = "required string name = 1 [\n"
        + "  kit = \"kat\"\n"
        + "];\n";
    assertThat(field.toSchema()).isEqualTo(expected);
  }
}
