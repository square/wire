/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.wire.schema

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.message
import com.squareup.wire.buildSchema
import kotlin.test.Test
import okio.Path.Companion.toPath

class OptionsTest {
  @Test
  fun structuredAndUnstructuredOptions() {
    // From https://developers.google.com/protocol-buffers/docs/proto#options
    val schema = buildSchema {
      add(
        "foo.proto".toPath(),
        """
            |import "google/protobuf/descriptor.proto";
            |message FooOptions {
            |  optional int32 opt1 = 1;
            |  optional string opt2 = 2;
            |}
            |
            |extend google.protobuf.FieldOptions {
            |  optional FooOptions foo_options = 1234;
            |}
            |
            |message Bar {
            |  optional int32 a = 1 [(foo_options).opt1 = 123, (foo_options).opt2 = "baz"];
            |  optional int32 b = 2 [(foo_options) = { opt1: 456 opt2: "quux" }];
            |}
        """.trimMargin(),
      )
    }

    val fooOptions = ProtoMember.get(Options.FIELD_OPTIONS, "foo_options")
    val opt1 = ProtoMember.get(ProtoType.get("FooOptions"), "opt1")
    val opt2 = ProtoMember.get(ProtoType.get("FooOptions"), "opt2")

    val bar = schema.getType("Bar") as MessageType
    assertThat(bar.field("a")!!.options.map)
      .isEqualTo(mapOf(fooOptions to mapOf(opt1 to "123", opt2 to "baz")))
    assertThat(bar.field("b")!!.options.map)
      .isEqualTo(mapOf(fooOptions to mapOf(opt1 to "456", opt2 to "quux")))
  }

  @Test
  fun textFormatCanOmitMapValueSeparator() {
    val schema = buildSchema {
      add(
        "foo.proto".toPath(),
        """
            |import "google/protobuf/descriptor.proto";
            |message FooOptions {
            |  optional BarOptions bar = 2;
            |}
            |message BarOptions {
            |  optional int32 baz = 2;
            |}
            |
            |extend google.protobuf.FieldOptions {
            |  optional FooOptions foo = 1234;
            |}
            |
            |message Message {
            |  optional int32 b = 2 [(foo) = { bar { baz: 123 } }];
            |}
        """.trimMargin(),
      )
    }

    val foo = ProtoMember.get(Options.FIELD_OPTIONS, "foo")
    val bar = ProtoMember.get(ProtoType.get("FooOptions"), "bar")
    val baz = ProtoMember.get(ProtoType.get("BarOptions"), "baz")

    val message = schema.getType("Message") as MessageType
    assertThat(message.field("b")!!.options.map)
      .isEqualTo(mapOf(foo to mapOf(bar to mapOf(baz to "123"))))
  }

  @Test
  fun testOptionsToSchema() {
    val schema = buildSchema {
      add(
        "foo.proto".toPath(),
        """
            |import "google/protobuf/descriptor.proto";
            |enum FooParameterType {
            |   NUMBER = 1;
            |   STRING = 2;
            |}
            |enum Scheme {
            |  UNKNOWN = 0;
            |  HTTP = 1;
            |  HTTPS = 2;
            |}
            |
            |message FooOptions {
            |  optional string name = 1;
            |  optional FooParameterType type = 2;
            |  repeated Scheme schemes = 3;
            |}
            |extend google.protobuf.MessageOptions {
            |  repeated FooOptions foo = 12345;
            |}
            |
            |message Message {
            |  option (foo) = {
            |    name: "test"
            |    type: STRING
            |    schemes: HTTP
            |    schemes: HTTPS
            |  };
            |
            |  option (foo) = {
            |    name: "test2"
            |    type: NUMBER
            |    schemes: [HTTP, HTTPS]
            |  };
            |
            |  optional int32 b = 2;
            |}
        """.trimMargin(),
      )
    }

    val protoFile = schema.protoFile("foo.proto")

    val optionElements = protoFile!!.types
      .first { it is MessageType && it.toElement().name == "Message" }
      .options.elements

    assertThat(optionElements[0].toSchema())
      .isEqualTo(
        """|(foo) = {
           |  name: "test",
           |  type: STRING,
           |  schemes: [
           |    HTTP,
           |    HTTPS
           |  ]
           |}
        """.trimMargin(),
      )

    val foo = ProtoMember.get(Options.MESSAGE_OPTIONS, "foo")

    val name = ProtoMember.get(ProtoType.get("FooOptions"), "name")
    val type = ProtoMember.get(ProtoType.get("FooOptions"), "type")
    val schemes = ProtoMember.get(ProtoType.get("FooOptions"), "schemes")

    val message = schema.getType("Message") as MessageType
    message.toElement().name

    assertThat(message.options.map)
      .isEqualTo(
        mapOf(
          foo to
            arrayListOf(
              mapOf(name to "test", type to "STRING", schemes to listOf("HTTP", "HTTPS")),
              mapOf(name to "test2", type to "NUMBER", schemes to listOf("HTTP", "HTTPS")),
            ),
        ),
      )
  }

  @Test
  fun fullyQualifiedOptionFields() {
    val schema = buildSchema {
      add(
        "a/b/more_options.proto".toPath(),
        """
            |syntax = "proto2";
            |package a.b;
            |
            |import "google/protobuf/descriptor.proto";
            |
            |extend google.protobuf.MessageOptions {
            |  optional MoreOptions more_options = 17000;
            |}
            |
            |message MoreOptions {
            |  extensions 100 to 200;
            |}
        """.trimMargin(),
      )
      add(
        "a/c/event_more_options.proto".toPath(),
        """
            |syntax = "proto2";
            |package a.c;
            |
            |import "a/b/more_options.proto";
            |
            |extend a.b.MoreOptions {
            |  optional EvenMoreOptions even_more_options = 100;
            |}
            |
            |message EvenMoreOptions {
            |  optional string string_option = 1;
            |}
        """.trimMargin(),
      )
      add(
        "a/d/message.proto".toPath(),
        """
            |syntax = "proto2";
            |package a.d;
            |
            |import "a/b/more_options.proto";
            |import "a/c/event_more_options.proto";
            |
            |message Message {
            |  option (a.b.more_options) = {
            |    [a.c.even_more_options]: {string_option: "foo"}
            |  };
            |}
        """.trimMargin(),
      )
    }
    val moreOptionsType = ProtoType.get("a.b.MoreOptions")
    val evenMoreOptionsType = ProtoType.get("a.c.EvenMoreOptions")
    val moreOptions = ProtoMember.get(Options.MESSAGE_OPTIONS, "a.b.more_options")
    val evenMoreOptions = ProtoMember.get(moreOptionsType, "a.c.even_more_options")
    val stringOption = ProtoMember.get(evenMoreOptionsType, "string_option")
    val message = schema.getType("a.d.Message") as MessageType

    assertThat(message.options.map)
      .isEqualTo(mapOf(moreOptions to mapOf(evenMoreOptions to mapOf(stringOption to "foo"))))
  }

  @Test
  fun resolveFieldPathMatchesLeadingDotFirstSegment() {
    assertThat(Options.resolveFieldPath(".a.b.c.d", setOf("a", "z", "y"))!!)
      .containsExactly("a", "b", "c", "d")
  }

  @Test
  fun resolveFieldPathMatchesFirstSegment() {
    assertThat(Options.resolveFieldPath("a.b.c.d", setOf("a", "z", "y"))!!)
      .containsExactly("a", "b", "c", "d")
  }

  @Test
  fun resolveFieldPathMatchesMultipleSegments() {
    assertThat(Options.resolveFieldPath("a.b.c.d", setOf("a.b", "z.b", "y.b"))!!)
      .containsExactly("a.b", "c", "d")
  }

  @Test
  fun resolveFieldPathMatchesAllSegments() {
    assertThat(Options.resolveFieldPath("a.b.c.d", setOf("a.b.c.d", "z.b.c.d"))!!)
      .containsExactly("a.b.c.d")
  }

  @Test
  fun resolveFieldPathMatchesOnlySegment() {
    assertThat(Options.resolveFieldPath("a", setOf("a", "b"))!!).containsExactly("a")
  }

  @Test
  fun resolveFieldPathDoesntMatch() {
    assertThat(Options.resolveFieldPath("a.b", setOf("c", "d"))).isNull()
  }

  @Test
  fun mapFieldEntriesLinking() {
    val schema = buildSchema {
      add(
        "my_package/some_enum.proto".toPath(),
        """
        |syntax = "proto2";
        |import "google/protobuf/descriptor.proto";
        |package my_package;
        |
        |enum SomeEnum {
        |  TEST = 0 [(my_package.my_option) = {
        |    entries: [
        |      {
        |        key: 'key-1',
        |        value: {
        |          some_string: "value-1"
        |        }
        |      },
        |      {
        |        value: {
        |          some_string: "value-2"
        |        }
        |      },
        |      {
        |        key: 'key-3',
        |        value: {
        |        }
        |      },
        |      {
        |        key: 'key-4',
        |      },
        |      {
        |        key: 'key-5',
        |        value: {
        |          some_string: "value-5",
        |          some_int32: 5
        |        }
        |      }
        |    ]
        |  }];
        |}
        |
        |message SomeMessage {
        |  optional string some_string = 1;
        |  optional int32 some_int32 = 2;
        |}
        |
        |message MyOption {
        |  map<string, SomeMessage> entries = 2;
        |}
        |
        |extend google.protobuf.EnumValueOptions {
        |  optional MyOption my_option = 1000;
        |}
        """.trimMargin(),
      )
    }

    val enumType = schema.getType(ProtoType.get("my_package.SomeEnum")) as EnumType
    val myOption = ProtoMember.get(Options.ENUM_VALUE_OPTIONS, "my_package.my_option")
    val entries = ProtoMember.get(ProtoType.get("my_package.MyOption"), "entries")
    val someString = ProtoMember.get(ProtoType.get("my_package.SomeMessage"), "some_string")
    val someInt32 = ProtoMember.get(ProtoType.get("my_package.SomeMessage"), "some_int32")
    assertThat(enumType.constant(tag = 0)!!.options.map)
      .isEqualTo(
        mapOf(
          myOption to mapOf(
            entries to listOf(
              mapOf("key-1" to mapOf(someString to "value-1")),
              mapOf(null to mapOf(someString to "value-2")),
              mapOf("key-3" to mapOf()),
              mapOf("key-4" to null),
              mapOf("key-5" to mapOf(someString to "value-5", someInt32 to "5")),
            ),
          ),
        ),
      )
  }

  @Test
  fun mapFieldEntriesWriting() {
    val path = "my_package/some_enum.proto".toPath()
    val schema = buildSchema {
      add(
        path,
        """
        |syntax = "proto3";
        |import "google/protobuf/descriptor.proto";
        |package my_package;
        |
        |enum SomeEnum {
        |  TEST = 0 [(my_package.my_option) = {
        |    entries: [
        |      {
        |        key: 'key-1',
        |        value: {
        |          some_string: "value-1"
        |        }
        |      },
        |      {
        |        key: 'key-2',
        |        value: {
        |          some_string: "value-2",
        |          some_int32: 2
        |        }
        |      }
        |    ]
        |  }];
        |}
        |
        |message SomeMessage {
        |  string some_string = 1;
        |  int32 some_int32 = 2;
        |}
        |
        |message MyOption {
        |  map<string, SomeMessage> entries = 2;
        |}
        |
        |extend google.protobuf.EnumValueOptions {
        |  MyOption my_option = 1000;
        |}
        """.trimMargin(),
      )
    }

    val enumType = schema.getType(ProtoType.get("my_package.SomeEnum")) as EnumType
    val optionElement = enumType.constant(tag = 0)!!.options.elements.first()
    // We do print "key" and "value" keys for map fields, even though the linked schema doesn't
    // know about them.
    val expected = """
      |(my_package.my_option) = {
      |  entries: [
      |    {
      |      key: "key-1",
      |      value: {
      |        some_string: "value-1"
      |      }
      |    },
      |    {
      |      key: "key-2",
      |      value: {
      |        some_string: "value-2",
      |        some_int32: 2
      |      }
      |    }
      |  ]
      |}
    """.trimMargin()
    assertThat(optionElement.toSchema()).isEqualTo(expected)
  }
}
