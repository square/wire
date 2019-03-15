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

import com.squareup.wire.schema.Location
import com.squareup.wire.schema.ProtoFile.Syntax.PROTO_2
import com.squareup.wire.schema.internal.parser.OptionElement.Kind
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ProtoFileElementTest {
  internal var location = Location.get("file.proto")

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
    val element = MessageElement.builder(location)
        .name("Message")
        .build()
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
    val element = MessageElement.builder(location)
        .name("Message")
        .build()
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
    val element = MessageElement.builder(location)
        .name("Message")
        .build()
    val file = ProtoFileElement(
        location = location,
        imports = listOf("example.other", "example.another"),
        types = listOf(element)
    )
    assertThat(file.imports).hasSize(2)
  }

  @Test
  fun simpleWithPublicImportsToSchema() {
    val element = MessageElement.builder(location)
        .name("Message")
        .build()
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
    val element = MessageElement.builder(location)
        .name("Message")
        .build()
    val file = ProtoFileElement(location = location,
        publicImports = listOf("example.other", "example.another"),
        types = listOf(element)
    )
    assertThat(file.publicImports).hasSize(2)
  }

  @Test
  fun simpleWithBothImportsToSchema() {
    val element = MessageElement.builder(location)
        .name("Message")
        .build()
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
    val element = MessageElement.builder(location)
        .name("Message")
        .build()
    val service = ServiceElement.builder(location)
        .name("Service")
        .build()
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
    val service1 = ServiceElement.builder(location)
        .name("Service1")
        .build()
    val service2 = ServiceElement.builder(location)
        .name("Service2")
        .build()
    val file = ProtoFileElement(
        location = location,
        services = listOf(service1, service2)
    )
    assertThat(file.services).hasSize(2)
  }

  @Test
  fun simpleWithOptionsToSchema() {
    val element = MessageElement.builder(location)
        .name("Message")
        .build()
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
    val element = MessageElement.builder(location)
        .name("Message")
        .build()
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
        extendDeclarations = listOf(
            ExtendElement.builder(location.at(5, 1)).name("Extend").build()
        ),
        types = listOf(MessageElement.builder(location).name("Message").build())
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
    val extend1 = ExtendElement.builder(location)
        .name("Extend1")
        .build()
    val extend2 = ExtendElement.builder(location)
        .name("Extend2")
        .build()
    val file = ProtoFileElement(
        location = location,
        extendDeclarations = listOf(extend1, extend2)
    )
    assertThat(file.extendDeclarations).hasSize(2)
  }

  @Test
  fun multipleEverythingToSchema() {
    val element1 = MessageElement.builder(location.at(10, 1))
        .name("Message1")
        .build()
    val element2 = MessageElement.builder(location.at(11, 1))
        .name("Message2")
        .build()
    val extend1 = ExtendElement.builder(location.at(13, 1))
        .name("Extend1")
        .build()
    val extend2 = ExtendElement.builder(location.at(14, 1))
        .name("Extend2")
        .build()
    val option1 = OptionElement.create("kit", Kind.STRING, "kat")
    val option2 = OptionElement.create("foo", Kind.STRING, "bar")
    val service1 = ServiceElement.builder(location.at(16, 1))
        .name("Service1")
        .build()
    val service2 = ServiceElement.builder(location.at(17, 1))
        .name("Service2")
        .build()
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
    val element = MessageElement.builder(location)
        .name("Message")
        .build()
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
}
