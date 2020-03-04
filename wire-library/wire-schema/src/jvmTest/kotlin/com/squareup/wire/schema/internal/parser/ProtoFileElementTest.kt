/*
 * Copyright (C) 2013 Square, Inc.
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

import com.squareup.wire.schema.Field
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.ProtoFile.Syntax.PROTO_2
import com.squareup.wire.schema.ProtoFile.Syntax.PROTO_3
import com.squareup.wire.schema.internal.parser.OptionElement.Companion.PACKED_OPTION_ELEMENT
import com.squareup.wire.schema.internal.parser.OptionElement.Kind
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ProtoFileElementTest {
  internal var location = Location.get("some/folder", "file.proto")

  @Test
  fun emptyToSchema() {
    val file = ProtoFileElement(location = location)
    val expected = "// file.proto\n"
    assertThat(file.toSchema()).isEqualTo(expected)
  }

  @Test
  fun emptyWithPackageToSchema() {
    val file = ProtoFileElement(
        location = location,
        packageName = "example.simple"
    )
    val expected = """
        |// file.proto
        |package example.simple;
        |""".trimMargin()
    assertThat(file.toSchema()).isEqualTo(expected)
  }

  @Test
  fun simpleToSchema() {
    val element = MessageElement(
        location = location,
        name = "Message"
    )
    val file = ProtoFileElement(
        location = location,
        types = listOf(element)
    )
    val expected = """
        |// file.proto
        |
        |message Message {}
        |""".trimMargin()
    assertThat(file.toSchema()).isEqualTo(expected)
  }

  @Test
  fun simpleWithImportsToSchema() {
    val element = MessageElement(
        location = location,
        name = "Message"
    )
    val file = ProtoFileElement(
        location = location,
        imports = listOf("example.other"),
        types = listOf(element)
    )
    val expected = """
        |// file.proto
        |
        |import "example.other";
        |
        |message Message {}
        |""".trimMargin()
    assertThat(file.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleDependencies() {
    val element = MessageElement(
        location = location,
        name = "Message"
    )
    val file = ProtoFileElement(
        location = location,
        imports = listOf("example.other", "example.another"),
        types = listOf(element)
    )
    assertThat(file.imports).hasSize(2)
  }

  @Test
  fun simpleWithPublicImportsToSchema() {
    val element = MessageElement(
        location = location,
        name = "Message"
    )
    val file = ProtoFileElement(
        location = location,
        publicImports = listOf("example.other"),
        types = listOf(element)
    )
    val expected = """
        |// file.proto
        |
        |import public "example.other";
        |
        |message Message {}
        |""".trimMargin()
    assertThat(file.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultiplePublicDependencies() {
    val element = MessageElement(
        location = location,
        name = "Message"
    )
    val file = ProtoFileElement(location = location,
        publicImports = listOf("example.other", "example.another"),
        types = listOf(element)
    )
    assertThat(file.publicImports).hasSize(2)
  }

  @Test
  fun simpleWithBothImportsToSchema() {
    val element = MessageElement(
        location = location,
        name = "Message"
    )
    val file = ProtoFileElement(location = location,
        imports = listOf("example.thing"),
        publicImports = listOf("example.other"),
        types = listOf(element)
    )
    val expected = """
        |// file.proto
        |
        |import "example.thing";
        |import public "example.other";
        |
        |message Message {}
        |""".trimMargin()
    assertThat(file.toSchema()).isEqualTo(expected)
  }

  @Test
  fun simpleWithServicesToSchema() {
    val element = MessageElement(
        location = location,
        name = "Message"
    )
    val service = ServiceElement(
        location = location,
        name = "Service"
    )
    val file = ProtoFileElement(
        location = location,
        types = listOf(element),
        services = listOf(service)
    )
    val expected = """
        |// file.proto
        |
        |message Message {}
        |
        |service Service {}
        |""".trimMargin()
    assertThat(file.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleServices() {
    val service1 = ServiceElement(
        location = location,
        name = "Service1"
    )
    val service2 = ServiceElement(
        location = location,
        name = "Service2"
    )
    val file = ProtoFileElement(
        location = location,
        services = listOf(service1, service2)
    )
    assertThat(file.services).hasSize(2)
  }

  @Test
  fun simpleWithOptionsToSchema() {
    val element = MessageElement(
        location = location,
        name = "Message"
    )
    val option = OptionElement.create("kit", Kind.STRING, "kat")
    val file = ProtoFileElement(
        location = location,
        options = listOf(option),
        types = listOf(element)
    )
    val expected = """
        |// file.proto
        |
        |option kit = "kat";
        |
        |message Message {}
        |""".trimMargin()
    assertThat(file.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleOptions() {
    val element = MessageElement(
        location = location,
        name = "Message"
    )
    val kitKat = OptionElement.create("kit", Kind.STRING, "kat")
    val fooBar = OptionElement.create("foo", Kind.STRING, "bar")
    val file = ProtoFileElement(
        location = location,
        options = listOf(kitKat, fooBar),
        types = listOf(element)
    )
    assertThat(file.options).hasSize(2)
  }

  @Test
  fun simpleWithExtendsToSchema() {
    val file = ProtoFileElement(
        location = location,
        extendDeclarations = listOf(ExtendElement(location = location.at(5, 1), name = "Extend")),
        types = listOf(MessageElement(location = location, name = "Message"))
    )
    val expected = """
        |// file.proto
        |
        |message Message {}
        |
        |extend Extend {}
        |""".trimMargin()
    assertThat(file.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleExtends() {
    val extend1 = ExtendElement(location = location, name = "Extend1")
    val extend2 = ExtendElement(location = location, name = "Extend2")
    val file = ProtoFileElement(
        location = location,
        extendDeclarations = listOf(extend1, extend2)
    )
    assertThat(file.extendDeclarations).hasSize(2)
  }

  @Test
  fun multipleEverythingToSchema() {
    val element1 = MessageElement(location = location.at(10, 1), name = "Message1")
    val element2 = MessageElement(location = location.at(11, 1), name = "Message2")
    val extend1 = ExtendElement(location = location.at(13, 1), name = "Extend1")
    val extend2 = ExtendElement(location = location.at(14, 1), name = "Extend2")
    val option1 = OptionElement.create("kit", Kind.STRING, "kat")
    val option2 = OptionElement.create("foo", Kind.STRING, "bar")
    val service1 = ServiceElement(
        location = location.at(16, 1),
        name = "Service1"
    )
    val service2 = ServiceElement(
        location = location.at(17, 1),
        name = "Service2"
    )
    val file = ProtoFileElement(
        location = location,
        packageName = "example.simple",
        imports = listOf("example.thing"),
        publicImports = listOf("example.other"),
        types = listOf(element1, element2),
        services = listOf(service1, service2),
        extendDeclarations = listOf(extend1, extend2),
        options = listOf(option1, option2)
    )
    val expected = """
        |// file.proto
        |package example.simple;
        |
        |import "example.thing";
        |import public "example.other";
        |
        |option kit = "kat";
        |option foo = "bar";
        |
        |message Message1 {}
        |message Message2 {}
        |
        |extend Extend1 {}
        |extend Extend2 {}
        |
        |service Service1 {}
        |service Service2 {}
        |""".trimMargin()
    assertThat(file.toSchema()).isEqualTo(expected)

    // Re-parse the expected string into a ProtoFile and ensure they're equal.
    val parsed = ProtoParser.parse(location, expected)
    assertThat(parsed).isEqualTo(file)
  }

  @Test
  fun syntaxToSchema() {
    val element = MessageElement(location = location, name = "Message")
    val file = ProtoFileElement(
        location = location,
        syntax = PROTO_2,
        types = listOf(element)
    )
    val expected = """
        |// file.proto
        |syntax = "proto2";
        |
        |message Message {}
        |""".trimMargin()
    assertThat(file.toSchema()).isEqualTo(expected)
  }

  @Test
  fun defaultIsSetInProto2() {
    val field = FieldElement(
        location = location.at(9, 3),
        label = Field.Label.REQUIRED,
        type = "string",
        name = "name",
        tag = 1,
        defaultValue = "defaultValue"
    )
    val message =
        MessageElement(location = location.at(8, 1), name = "Message", fields = listOf(field))
    val file = ProtoFileElement(
        syntax = PROTO_2,
        location = location,
        packageName = "example.simple",
        imports = listOf("example.thing"),
        publicImports = listOf("example.other"),
        types = listOf(message)
    )
    val expected = """
        |// file.proto
        |syntax = "proto2";
        |package example.simple;
        |
        |import "example.thing";
        |import public "example.other";
        |
        |message Message {
        |  required string name = 1 [default = "defaultValue"];
        |}
        |""".trimMargin()
    assertThat(file.toSchema()).isEqualTo(expected)

    // Re-parse the expected string into a ProtoFile and ensure they're equal.
    val parsed = ProtoParser.parse(location, expected)
    assertThat(parsed).isEqualTo(file)
  }

  @Test
  fun defaultIsNotSetInProto3() {
    val field = FieldElement(
        location = location.at(9, 3),
        label = Field.Label.REQUIRED,
        type = "string",
        name = "name",
        tag = 1,
        defaultValue = "defaultValue"
    )
    val message =
        MessageElement(location = location.at(8, 1), name = "Message", fields = listOf(field))
    val file = ProtoFileElement(
        syntax = PROTO_3,
        location = location,
        packageName = "example.simple",
        imports = listOf("example.thing"),
        publicImports = listOf("example.other"),
        types = listOf(message)
    )
    assertThat(file.toSchema()).doesNotContain("defaultValue")

    // TODO(benoit|masaru): Ignore the test below now. This will be fixed by #1386.
    // val expected = """
    //     |// file.proto
    //     |syntax = "proto3";
    //     |package example.simple;
    //     |
    //     |import "example.thing";
    //     |import public "example.other";
    //     |
    //     |message Message {
    //     |  string name = 1;
    //     |}
    //     |""".trimMargin()
    // assertThat(file.toSchema()).isEqualTo(expected)
    // // Re-parse the expected string into a ProtoFile and ensure they're equal.
    // val parsed = ProtoParser.parse(location, expected)
    // assertThat(parsed).isEqualTo(file)
  }

  @Test
  fun convertPackedOptionFromWireSchemaInProto2() {
    val fieldNumeric = FieldElement(
        location = location.at(6, 3),
        label = Field.Label.REPEATED,
        type = "int32",
        name = "numeric_without_packed_option",
        tag = 1
    )
    val fieldNumericPackedTrue = FieldElement(
        location = location.at(7, 3),
        label = Field.Label.REPEATED,
        type = "int32",
        name = "numeric_packed_true",
        tag = 2,
        options = listOf(PACKED_OPTION_ELEMENT)
    )
    val fieldNumericPackedFalse = FieldElement(
        location = location.at(8, 3),
        label = Field.Label.REPEATED,
        type = "int32",
        name = "numeric_packed_false",
        tag = 3,
        options = listOf(PACKED_OPTION_ELEMENT.copy(value = "false"))
    )
    val fieldString = FieldElement(
        location = location.at(9, 3),
        label = Field.Label.REPEATED,
        type = "string",
        name = "string_without_packed_option",
        tag = 4
    )
    val fieldStringPackedTrue = FieldElement(
        location = location.at(10, 3),
        label = Field.Label.REPEATED,
        type = "string",
        name = "string_packed_true",
        tag = 5,
        options = listOf(PACKED_OPTION_ELEMENT)
    )
    val fieldStringPackedFalse = FieldElement(
        location = location.at(11, 3),
        label = Field.Label.REPEATED,
        type = "string",
        name = "string_packed_false",
        tag = 6,
        options = listOf(PACKED_OPTION_ELEMENT.copy(value = "false"))
    )

    val message = MessageElement(
        location = location.at(5, 1),
        name = "Message",
        fields = listOf(fieldNumeric, fieldNumericPackedTrue, fieldNumericPackedFalse, fieldString,
            fieldStringPackedTrue, fieldStringPackedFalse))
    val file = ProtoFileElement(
        syntax = PROTO_2,
        location = location,
        packageName = "example.simple",
        imports = emptyList(),
        publicImports = emptyList(),
        types = listOf(message)
    )
    val expected = """
        |// file.proto
        |syntax = "proto2";
        |package example.simple;
        |
        |message Message {
        |  repeated int32 numeric_without_packed_option = 1;
        |  repeated int32 numeric_packed_true = 2 [packed = true];
        |  repeated int32 numeric_packed_false = 3 [packed = false];
        |  repeated string string_without_packed_option = 4;
        |  repeated string string_packed_true = 5 [packed = true];
        |  repeated string string_packed_false = 6 [packed = false];
        |}
        |""".trimMargin()
    assertThat(file.toSchema()).isEqualTo(expected)

    // Re-parse the expected string into a ProtoFile and ensure they're equal.
    val parsed = ProtoParser.parse(location, expected)
    assertThat(parsed).isEqualTo(file)
  }

  @Test
  fun convertPackedOptionFromWireSchemaInProto3() {
    val fieldNumeric = FieldElement(
        location = location.at(6, 3),
        label = Field.Label.REPEATED,
        type = "int32",
        name = "numeric_without_packed_option",
        tag = 1
    )
    val fieldNumericPackedTrue = FieldElement(
        location = location.at(7, 3),
        label = Field.Label.REPEATED,
        type = "int32",
        name = "numeric_packed_true",
        tag = 2,
        options = listOf(PACKED_OPTION_ELEMENT)
    )
    val fieldNumericPackedFalse = FieldElement(
        location = location.at(8, 3),
        label = Field.Label.REPEATED,
        type = "int32",
        name = "numeric_packed_false",
        tag = 3,
        options = listOf(PACKED_OPTION_ELEMENT.copy(value = "false"))
    )
    val fieldString = FieldElement(
        location = location.at(9, 3),
        label = Field.Label.REPEATED,
        type = "string",
        name = "string_without_packed_option",
        tag = 4
    )
    val fieldStringPackedTrue = FieldElement(
        location = location.at(10, 3),
        label = Field.Label.REPEATED,
        type = "string",
        name = "string_packed_true",
        tag = 5,
        options = listOf(PACKED_OPTION_ELEMENT)
    )
    val fieldStringPackedFalse = FieldElement(
        location = location.at(11, 3),
        label = Field.Label.REPEATED,
        type = "string",
        name = "string_packed_false",
        tag = 6,
        options = listOf(PACKED_OPTION_ELEMENT.copy(value = "false"))
    )

    val message = MessageElement(
        location = location.at(5, 1),
        name = "Message",
        fields = listOf(fieldNumeric, fieldNumericPackedTrue, fieldNumericPackedFalse, fieldString,
            fieldStringPackedTrue, fieldStringPackedFalse)
    )
    val file = ProtoFileElement(
        syntax = PROTO_3,
        location = location,
        packageName = "example.simple",
        imports = emptyList(),
        publicImports = emptyList(),
        types = listOf(message)
    )
    val expected = """
        |// file.proto
        |syntax = "proto3";
        |package example.simple;
        |
        |message Message {
        |  repeated int32 numeric_without_packed_option = 1;
        |  repeated int32 numeric_packed_true = 2 [packed = true];
        |  repeated int32 numeric_packed_false = 3 [packed = false];
        |  repeated string string_without_packed_option = 4;
        |  repeated string string_packed_true = 5 [packed = true];
        |  repeated string string_packed_false = 6 [packed = false];
        |}
        |""".trimMargin()
    assertThat(file.toSchema()).isEqualTo(expected)

    // Re-parse the expected string into a ProtoFile and ensure they're equal.
    val parsed = ProtoParser.parse(location, expected)
    assertThat(parsed).isEqualTo(file)
  }
}
