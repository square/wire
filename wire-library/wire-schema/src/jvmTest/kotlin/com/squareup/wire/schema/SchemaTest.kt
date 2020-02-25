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
@file:Suppress("UsePropertyAccessSyntax")

package com.squareup.wire.schema

import com.squareup.wire.schema.Options.Companion.FIELD_OPTIONS
import com.squareup.wire.schema.internal.isValidTag
import com.squareup.wire.schema.internal.parser.OptionElement
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test

class SchemaTest {
  @Test
  fun linkService() {
    val schema = RepoBuilder()
        .add("service.proto", """
            |import "request.proto";
            |import "response.proto";
            |service Service {
            |  rpc Call (Request) returns (Response);
            |}
            """.trimMargin())
        .add("request.proto", """
            |message Request {
            |}
            """.trimMargin())
        .add("response.proto", """
            |message Response {
            |}
            """.trimMargin())
        .schema()

    val service = schema.getService("Service")!!
    val call = service.rpc("Call")!!
    assertThat(call.requestType).isEqualTo(schema.getType("Request")!!.type)
    assertThat(call.responseType).isEqualTo(schema.getType("Response")!!.type)
  }

  @Test
  fun linkMessage() {
    val schema = RepoBuilder()
        .add("message.proto", """
            |import "foo.proto";
            |message Message {
            |  optional foo_package.Foo field = 1;
            |  map<string, foo_package.Bar> bars = 2;
            |}
            """.trimMargin())
        .add("foo.proto", """
            |package foo_package;
            |message Foo {
            |}
            |message Bar {
            |}
            """.trimMargin())
        .schema()

    val message = schema.getType("Message") as MessageType
    val field = message.field("field")!!
    assertThat(field.type).isEqualTo(schema.getType("foo_package.Foo")!!.type)
    val bars = message.field("bars")!!.type
    assertThat(bars!!.keyType).isEqualTo(ProtoType.STRING)
    assertThat(bars.valueType).isEqualTo(schema.getType("foo_package.Bar")!!.type)
  }

  @Ignore("Resolution happens from the root not from inside Outer and so this fails.")
  @Test
  fun linkExtendTypeInOuterMessage() {
    val schema = RepoBuilder()
        .add("foo.proto", """
            |message Other {
            |  extensions 1;
            |}
            |message Outer {
            |  enum Choice {
            |    ZERO = 0;
            |    ONE = 1;
            |  }
            |
            |  extend Other {
            |    optional Choice choice = 1;
            |  }
            """.trimMargin())
        .schema()

    val message = schema.getType("Other") as MessageType
    val field = message.field("choice")!!
    assertThat(field.type).isEqualTo(schema.getType("Outer.Choice")!!.type)
  }

  @Test
  fun isValidTag() {
    assertThat(0.isValidTag()).isFalse() // Less than minimum.
    assertThat(1.isValidTag()).isTrue()
    assertThat(1234.isValidTag()).isTrue()
    assertThat(19222.isValidTag()).isFalse() // Reserved range.
    assertThat(2319573.isValidTag()).isTrue()
    assertThat(536870911.isValidTag()).isTrue()
    assertThat(536870912.isValidTag()).isFalse() // Greater than maximum.
  }

  @Test
  fun fieldInvalidTag() {
    try {
      RepoBuilder()
          .add("message.proto", """
            |message Message {
            |  optional int32 a = 0;
            |  optional int32 b = 1;
            |  optional int32 c = 18999;
            |  optional int32 d = 19000;
            |  optional int32 e = 19999;
            |  optional int32 f = 20000;
            |  optional int32 g = 536870911;
            |  optional int32 h = 536870912;
            |}
            """.trimMargin())
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected.message).isEqualTo("""
            |tag is out of range: 0
            |  for field a (/source/message.proto at 2:3)
            |  in message Message (/source/message.proto at 1:1)
            |tag is out of range: 19000
            |  for field d (/source/message.proto at 5:3)
            |  in message Message (/source/message.proto at 1:1)
            |tag is out of range: 19999
            |  for field e (/source/message.proto at 6:3)
            |  in message Message (/source/message.proto at 1:1)
            |tag is out of range: 536870912
            |  for field h (/source/message.proto at 9:3)
            |  in message Message (/source/message.proto at 1:1)
            """.trimMargin())
    }
  }

  @Test
  fun extensionsInvalidTag() {
    try {
      RepoBuilder()
          .add("message.proto", """
               |message Message {
               |  extensions 0;
               |  extensions 1;
               |  extensions 18999;
               |  extensions 19000, 19001 to 19998, 19999;
               |  extensions 20000;
               |  extensions 536870911;
               |  extensions 536870912;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |tags are out of range: 0
            |  for extensions (/source/message.proto at 2:3)
            |  in message Message (/source/message.proto at 1:1)
            |tags are out of range: 19000, 19001 to 19998, 19999
            |  for extensions (/source/message.proto at 5:3)
            |  in message Message (/source/message.proto at 1:1)
            |tags are out of range: 536870912
            |  for extensions (/source/message.proto at 8:3)
            |  in message Message (/source/message.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun scalarFieldIsPacked() {
    val schema = RepoBuilder()
        .add("message.proto", """
             |message Message {
             |  repeated int32 a = 1;
             |  repeated int32 b = 2 [packed=false];
             |  repeated int32 c = 3 [packed=true];
             |}
             """.trimMargin()
        )
        .schema()

    val message = schema.getType("Message") as MessageType
    assertThat(message.field("a")!!.isPacked).isFalse()
    assertThat(message.field("b")!!.isPacked).isFalse()
    assertThat(message.field("c")!!.isPacked).isTrue()
  }

  @Test
  fun enumFieldIsPacked() {
    val schema = RepoBuilder()
        .add("message.proto", """
             |message Message {
             |  repeated HabitablePlanet home_planet = 1 [packed=true];
             |  enum HabitablePlanet {
             |    EARTH = 1;
             |  }
             |}
             """.trimMargin()
        )
        .schema()
    val message = schema.getType("Message") as MessageType
    assertThat(message.field("home_planet")!!.isPacked).isTrue()
  }

  @Test
  fun fieldIsPackedButShouldntBe() {
    try {
      RepoBuilder()
          .add("message.proto", """
               |message Message {
               |  repeated bytes a = 1 [packed=false];
               |  repeated bytes b = 2 [packed=true];
               |  repeated string c = 3 [packed=false];
               |  repeated string d = 4 [packed=true];
               |  repeated Message e = 5 [packed=false];
               |  repeated Message f = 6 [packed=true];
               |}
               |extend Message {
               |  repeated bytes g = 7 [packed=false];
               |  repeated bytes h = 8 [packed=true];
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |packed=true not permitted on bytes
            |  for field b (/source/message.proto at 3:3)
            |  in message Message (/source/message.proto at 1:1)
            |packed=true not permitted on string
            |  for field d (/source/message.proto at 5:3)
            |  in message Message (/source/message.proto at 1:1)
            |packed=true not permitted on Message
            |  for field f (/source/message.proto at 7:3)
            |  in message Message (/source/message.proto at 1:1)
            |packed=true not permitted on bytes
            |  for field h (/source/message.proto at 11:3)
            |  in message Message (/source/message.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun fieldIsDeprecated() {
    val schema = RepoBuilder()
        .add("message.proto", """
             |message Message {
             |  optional int32 a = 1;
             |  optional int32 b = 2 [deprecated=false];
             |  optional int32 c = 3 [deprecated=true];
             |}
             """.trimMargin()
        )
        .schema()

    val message = schema.getType("Message") as MessageType
    assertThat(message.field("a")!!.isDeprecated).isFalse()
    assertThat(message.field("b")!!.isDeprecated).isFalse()
    assertThat(message.field("c")!!.isDeprecated).isTrue()
  }

  @Test
  fun fieldDefault() {
    val schema = RepoBuilder()
        .add("message.proto", """
             |message Message {
             |  optional int32 a = 1;
             |  optional int32 b = 2 [default = 5];
             |  optional bool c = 3 [default = true];
             |  optional string d = 4 [default = "foo"];
             |  optional Roshambo e = 5 [default = PAPER];
             |  enum Roshambo {
             |    ROCK = 0;
             |    SCISSORS = 1;
             |    PAPER = 2;
             |  }
             |}
             """.trimMargin()
        )
        .schema()

    val message = schema.getType("Message") as MessageType
    assertThat(message.field("a")!!.default).isNull()
    assertThat(message.field("b")!!.default).isEqualTo("5")
    assertThat(message.field("c")!!.default).isEqualTo("true")
    assertThat(message.field("d")!!.default).isEqualTo("foo")
    assertThat(message.field("e")!!.default).isEqualTo("PAPER")
  }

  @Test
  fun fieldOptions() {
    val schema = RepoBuilder()
        .add("message.proto", """
             |import "google/protobuf/descriptor.proto";
             |message Message {
             |  optional int32 a = 1;
             |  optional int32 b = 2 [color=red, deprecated=true, packed=true];
             |}
             |extend google.protobuf.FieldOptions {
             |  optional string color = 60001;
             |}
             """.trimMargin()
        )
        .schema()
    val message = schema.getType("Message") as MessageType

    val aOptions = message.field("a")!!.options
    assertThat(aOptions.get(ProtoMember.get(FIELD_OPTIONS, "color"))).isNull()
    assertThat(aOptions.get(ProtoMember.get(FIELD_OPTIONS, "deprecated"))).isNull()
    assertThat(aOptions.get(ProtoMember.get(FIELD_OPTIONS, "packed"))).isNull()

    val bOptions = message.field("b")!!.options
    assertThat(bOptions.get(ProtoMember.get(FIELD_OPTIONS, "color"))).isEqualTo("red")
    assertThat(bOptions.get(ProtoMember.get(FIELD_OPTIONS, "deprecated"))).isEqualTo("true")
    assertThat(bOptions.get(ProtoMember.get(FIELD_OPTIONS, "packed"))).isEqualTo("true")
  }

  @Test
  fun duplicateOption() {
    try {
      RepoBuilder()
          .add("message.proto", """
               |import "google/protobuf/descriptor.proto";
               |message Message {
               |  optional int32 a = 1 [color=red, color=blue];
               |}
               |extend google.protobuf.FieldOptions {
               |  optional string color = 60001;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |conflicting options: red, blue
            |  for field a (/source/message.proto at 3:3)
            |  in message Message (/source/message.proto at 2:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun messageFieldTypeUnknown() {
    try {
      RepoBuilder()
          .add("message.proto", """
               |message Message {
               |  optional foo_package.Foo unknown = 1;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |unable to resolve foo_package.Foo
            |  for field unknown (/source/message.proto at 2:3)
            |  in message Message (/source/message.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun oneOfFieldTypeUnknown() {
    try {
      RepoBuilder()
          .add("message.proto", """
               |message Message {
               |  oneof selection {
               |    int32 known = 1;
               |    foo_package.Foo unknown = 2;
               |  }
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |unable to resolve foo_package.Foo
            |  for field unknown (/source/message.proto at 4:5)
            |  in message Message (/source/message.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun serviceTypesMustBeNamed() {
    try {
      RepoBuilder()
          .add("service.proto", """
               |service Service {
               |  rpc Call (string) returns (Response);
               |}
               |message Response {
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |expected a message but was string
            |  for rpc Call (/source/service.proto at 2:3)
            |  in service Service (/source/service.proto at 1:1)
            """.trimMargin()
      )
    }

    try {
      RepoBuilder()
          .add("service.proto", """
               |service Service {
               |  rpc Call (Request) returns (string);
               |}
               |message Request {
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |expected a message but was string
            |  for rpc Call (/source/service.proto at 2:3)
            |  in service Service (/source/service.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun serviceTypesUnknown() {
    try {
      RepoBuilder()
          .add("service.proto", """
               |service Service {
               |  rpc Call (foo_package.Foo) returns (Response);
               |}
               |message Response {
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |unable to resolve foo_package.Foo
            |  for rpc Call (/source/service.proto at 2:3)
            |  in service Service (/source/service.proto at 1:1)
            """.trimMargin()
      )
    }

    try {
      RepoBuilder()
          .add("service.proto", """
               |service Service {
               |  rpc Call (Request) returns (foo_package.Foo);
               |}
               |message Request {
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |unable to resolve foo_package.Foo
            |  for rpc Call (/source/service.proto at 2:3)
            |  in service Service (/source/service.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun extendedTypeUnknown() {
    try {
      RepoBuilder()
          .add("extend.proto", """
               |extend foo_package.Foo {
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |unable to resolve foo_package.Foo
            |  for extend (/source/extend.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun extendedTypeMustBeNamed() {
    try {
      RepoBuilder()
          .add("extend.proto", """
               |extend string {
               |  optional Value value = 1000;
               |}
               |message Value {
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |expected a message but was string
            |  for extend (/source/extend.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun extendFieldTypeUnknown() {
    try {
      RepoBuilder()
          .add("message.proto", """
               |message Message {
               |}
               |extend Message {
               |  optional foo_package.Foo unknown = 1;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |unable to resolve foo_package.Foo
            |  for field unknown (/source/message.proto at 4:3)
            |  in message Message (/source/message.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun multipleErrors() {
    try {
      RepoBuilder()
          .add("message.proto", """
               |message Message {
               |  optional foo_package.Foo unknown = 1;
               |  optional foo_package.Foo also_unknown = 2;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |unable to resolve foo_package.Foo
            |  for field unknown (/source/message.proto at 2:3)
            |  in message Message (/source/message.proto at 1:1)
            |unable to resolve foo_package.Foo
            |  for field also_unknown (/source/message.proto at 3:3)
            |  in message Message (/source/message.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun duplicateMessageTagDisallowed() {
    try {
      RepoBuilder()
          .add("message.proto", """
               |message Message {
               |  required string name1 = 1;
               |  required string name2 = 1;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |multiple fields share tag 1:
            |  1. name1 (/source/message.proto at 2:3)
            |  2. name2 (/source/message.proto at 3:3)
            |  for message Message (/source/message.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun duplicateTagValueDisallowedInOneOf() {
    try {
      RepoBuilder()
          .add("message.proto", """
               |message Message {
               |  required string name1 = 1;
               |  oneof selection {
               |    string name2 = 1;
               |  }
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |multiple fields share tag 1:
            |  1. name1 (/source/message.proto at 2:3)
            |  2. name2 (/source/message.proto at 4:5)
            |  for message Message (/source/message.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun duplicateExtendTagDisallowed() {
    try {
      RepoBuilder()
          .add("message.proto", """
               |message Message {
               |}
               |extend Message {
               |  optional string name1 = 1;
               |  optional string name2 = 1;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |multiple fields share tag 1:
            |  1. name1 (/source/message.proto at 4:3)
            |  2. name2 (/source/message.proto at 5:3)
            |  for message Message (/source/message.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun messageNameCollisionDisallowed() {
    try {
      RepoBuilder()
          .add("message.proto", """
               |message Message {
               |  optional string a = 1;
               |  optional string a = 2;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |multiple fields share name a:
            |  1. a (/source/message.proto at 2:3)
            |  2. a (/source/message.proto at 3:3)
            |  for message Message (/source/message.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun messageAndExtensionNameCollision() {
    val schema = RepoBuilder()
        .add("message.proto", """
             |message Message {
             |  optional string a = 1;
             |}
             """.trimMargin()
        )
        .add("extend.proto", """
             |package p;
             |import "message.proto";
             |extend Message {
             |  optional string a = 2;
             |}
             """.trimMargin()
        )
        .schema()
    val messageType = schema.getType("Message") as MessageType

    assertThat(messageType.field("a")!!.tag).isEqualTo(1)
    assertThat(messageType.extensionField("p.a")!!.tag).isEqualTo(2)
  }

  @Test
  fun extendNameCollisionInSamePackageDisallowed() {
    try {
      RepoBuilder()
          .add("message.proto", """
               |message Message {
               |}
               """.trimMargin())
          .add("extend1.proto", """
               |import "message.proto";
               |extend Message {
               |  optional string a = 1;
               |}
               """.trimMargin())
          .add("extend2.proto", """
               |import "message.proto";
               |extend Message {
               |  optional string a = 2;
               |}
               """.trimMargin())
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |multiple fields share name a:
            |  1. a (/source/extend1.proto at 3:3)
            |  2. a (/source/extend2.proto at 3:3)
            |  for message Message (/source/message.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun extendNameCollisionInDifferentPackagesAllowed() {
    val schema = RepoBuilder()
        .add("message.proto", """
             |message Message {
             |}
             """.trimMargin()
        )
        .add("extend1.proto", """
             |package p1;
             |import "message.proto";
             |extend Message {
             |  optional string a = 1;
             |}
             """.trimMargin()
        )
        .add("extend2.proto", """
             |package p2;
             |import "message.proto";
             |extend Message {
             |  optional string a = 2;
             |}
             """.trimMargin()
        )
        .schema()
    val messageType = schema.getType("Message") as MessageType

    assertThat(messageType.field("a")).isNull()
    assertThat(messageType.extensionField("p1.a")!!.packageName).isEqualTo("p1")
    assertThat(messageType.extensionField("p2.a")!!.packageName).isEqualTo("p2")
  }

  @Test
  fun extendEnumDisallowed() {
    try {
      RepoBuilder()
          .add("enum.proto", """
               |enum Enum {
               |  A = 1;
               |  B = 2;
               |}
               """.trimMargin()
          )
          .add("extend.proto", """
               |import "enum.proto";
               |extend Enum {
               |  optional string a = 2;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |expected a message but was Enum
            |  for extend (/source/extend.proto at 2:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun requiredExtendFieldDisallowed() {
    try {
      RepoBuilder()
          .add("message.proto", """
               |message Message {
               |}
               |extend Message {
               |  required string a = 1;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |extension fields cannot be required
            |  for field a (/source/message.proto at 4:3)
            |  in message Message (/source/message.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun oneOfLabelDisallowed() {
    try {
      RepoBuilder()
          .add("message.proto", """
               |message Message {
               |  oneof string s = 1;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage("Syntax error in /source/message.proto at 2:17: expected '{'")
    }
  }

  @Test
  fun duplicateEnumValueTagInScopeDisallowed() {
    try {
      RepoBuilder()
          .add("message.proto", """
               |message Message {
               |  enum Enum1 {
               |    VALUE = 1;
               |  }
               |  enum Enum2 {
               |    VALUE = 2;
               |  }
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |multiple enums share constant VALUE:
            |  1. Message.Enum1.VALUE (/source/message.proto at 3:5)
            |  2. Message.Enum2.VALUE (/source/message.proto at 6:5)
            |  for message Message (/source/message.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun duplicateEnumConstantTagWithoutAllowAliasDisallowed() {
    try {
      RepoBuilder()
          .add("message.proto", """
               |enum Enum {
               |  A = 1;
               |  B = 1;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |multiple enum constants share tag 1:
            |  1. A (/source/message.proto at 2:3)
            |  2. B (/source/message.proto at 3:3)
            |  for enum Enum (/source/message.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun duplicateEnumConstantTagWithAllowAliasFalseDisallowed() {
    try {
      RepoBuilder()
          .add("message.proto", """
               |enum Enum {
               |  option allow_alias = false;
               |  A = 1;
               |  B = 1;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |multiple enum constants share tag 1:
            |  1. A (/source/message.proto at 3:3)
            |  2. B (/source/message.proto at 4:3)
            |  for enum Enum (/source/message.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun duplicateEnumConstantTagWithAllowAliasTrueAllowed() {
    val schema = RepoBuilder()
        .add("message.proto", """
             |enum Enum {
             |  option allow_alias = true;
             |  A = 1;
             |  B = 1;
             |}
             """.trimMargin()
        )
        .schema()
    val enumType = schema.getType("Enum") as EnumType
    assertThat(enumType.constant("A")!!.tag).isEqualTo(1)
    assertThat(enumType.constant("B")!!.tag).isEqualTo(1)
  }

  @Test
  fun fieldTypeImported() {
    val schema = RepoBuilder()
        .add("a.proto", """
             |package pa;
             |import "b.proto";
             |message A {
             |  optional pb.B b = 1;
             |}
             """.trimMargin()
        )
        .add("b.proto", """
             |package pb;
             |message B {
             |}
             """.trimMargin()
        )
        .schema()
    val a = schema.getType("pa.A") as MessageType
    val b = schema.getType("pb.B") as MessageType
    assertThat(a.field("b")!!.type).isEqualTo(b.type)
  }

  @Test
  fun fieldMapTypeImported() {
    val schema = RepoBuilder()
        .add("a.proto", """
             |package pa;
             |import "b.proto";
             |message A {
             |  map<string, pb.B> b = 1;
             |}
             """.trimMargin()
        )
        .add("b.proto", """
             |package pb;
             |message B {
             |}
             """.trimMargin()
        )
        .schema()
    val a = schema.getType("pa.A") as MessageType
    val b = schema.getType("pb.B") as MessageType
    assertThat(a.field("b")!!.type!!.valueType).isEqualTo(b.type)
  }

  @Test
  fun fieldTypeNotImported() {
    try {
      RepoBuilder()
          .add("a.proto", """
               |package pa;
               |message A {
               |  optional pb.B b = 1;
               |}
               """.trimMargin()
          )
          .add("b.proto", """
               |package pb;
               |message B {
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected.message).isEqualTo("""
            |a.proto needs to import b.proto
            |  for field b (/source/a.proto at 3:3)
            |  in message pa.A (/source/a.proto at 2:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun fieldMapTypeNotImported() {
    try {
      RepoBuilder()
          .add("a.proto", """
               |package pa;
               |message A {
               |  map<string, pb.B> b = 1;
               |}
               """.trimMargin()
          )
          .add("b.proto", """
               |package pb;
               |message B {
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected.message).isEqualTo("""
            |a.proto needs to import b.proto
            |  for field b (/source/a.proto at 3:3)
            |  in message pa.A (/source/a.proto at 2:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun rpcTypeImported() {
    val schema = RepoBuilder()
        .add("a.proto", """
             |package pa;
             |import "b.proto";
             |service Service {
             |  rpc Call (pb.B) returns (pb.B);
             |}
             """.trimMargin()
        )
        .add("b.proto", """
             |package pb;
             |message B {
             |}
             """.trimMargin()
        )
        .schema()
    val service = schema.getService("pa.Service")!!
    val b = schema.getType("pb.B") as MessageType
    assertThat(service.rpcs()[0].requestType).isEqualTo(b.type)
    assertThat(service.rpcs()[0].responseType).isEqualTo(b.type)
  }

  @Test
  fun rpcTypeNotImported() {
    try {
      RepoBuilder()
          .add("a.proto", """
               |package pa;
               |service Service {
               |  rpc Call (pb.B) returns (pb.B);
               |}
               """.trimMargin()
          )
          .add("b.proto", """
               |package pb;
               |message B {
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected.message).isEqualTo("""
            |a.proto needs to import b.proto
            |  for rpc Call (/source/a.proto at 3:3)
            |  in service pa.Service (/source/a.proto at 2:1)
            |a.proto needs to import b.proto
            |  for rpc Call (/source/a.proto at 3:3)
            |  in service pa.Service (/source/a.proto at 2:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun extendTypeImported() {
    val schema = RepoBuilder()
        .add("a.proto", """
             |package pa;
             |import "b.proto";
             |extend pb.B {
             |  optional string a = 1;
             |}
             """.trimMargin()
        )
        .add("b.proto", """
             |package pb;
             |message B {
             |  extensions 1;
             |}
             """.trimMargin()
        )
        .schema()
    val extendB = schema.protoFiles[0].extendList[0]
    val b = schema.getType("pb.B") as MessageType
    assertThat(extendB.type).isEqualTo(b.type)
  }

  @Test
  fun extendTypeNotImported() {
    try {
      RepoBuilder()
          .add("a.proto", """
               |package pa;
               |extend pb.B {
               |  optional string a = 1;
               |}
               """.trimMargin()
          )
          .add("b.proto", """
               |package pb;
               |message B {
               |  extensions 1;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected.message).isEqualTo("""
            |a.proto needs to import b.proto
            |  for extend pb.B (/source/a.proto at 2:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun transitiveImportNotFollowed() {
    try {
      RepoBuilder()
          .add("a.proto", """
               |package pa;
               |import "b.proto";
               |message A {
               |  optional pc.C c = 1;
               |}
               """.trimMargin()
          )
          .add("b.proto", """
               |package pb;
               |import "c.proto";
               |message B {
               |}
               """.trimMargin()
          )
          .add("c.proto", """
               |package pc;
               |message C {
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected.message).isEqualTo("""
            |a.proto needs to import c.proto
            |  for field c (/source/a.proto at 4:3)
            |  in message pa.A (/source/a.proto at 3:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun transitivePublicImportFollowed() {
    val schema = RepoBuilder()
        .add("a.proto", """
             |package pa;
             |import "b.proto";
             |message A {
             |  optional pc.C c = 1;
             |}
             """.trimMargin()
        )
        .add("b.proto", """
             |package pb;
             |import public "c.proto";
             |message B {
             |}
             """.trimMargin()
        )
        .add("c.proto", """
             |package pc;
             |message C {
             |}
             """.trimMargin()
        )
        .schema()
    val a = schema.getType("pa.A") as MessageType
    val c = schema.getType("pc.C") as MessageType
    assertThat(a.field("c")!!.type).isEqualTo(c.type)
  }

  @Test
  fun importSamePackageDifferentFile() {
    val schema = RepoBuilder()
        .add("a_b_1.proto", """
             |package a.b;
             |
             |import "a_b_2.proto";
             |
             |message MessageB {
             |  optional .a.b.MessageC c1 = 1;
             |  optional a.b.MessageC c2 = 2;
             |  optional b.MessageC c3 = 3;
             |  optional MessageC c4 = 4;
             |}
             """.trimMargin()
        )
        .add("a_b_2.proto", """
             |package a.b;
             |
             |message MessageC {
             |}
             """.trimMargin()
        )
        .schema()
    val messageC = schema.getType("a.b.MessageB") as MessageType
    assertThat(messageC.field("c1")!!.type).isEqualTo(ProtoType.get("a.b.MessageC"))
    assertThat(messageC.field("c2")!!.type).isEqualTo(ProtoType.get("a.b.MessageC"))
    assertThat(messageC.field("c3")!!.type).isEqualTo(ProtoType.get("a.b.MessageC"))
    assertThat(messageC.field("c4")!!.type).isEqualTo(ProtoType.get("a.b.MessageC"))
  }

  @Test
  fun importResolvesEnclosingPackageSuffix() {
    val schema = RepoBuilder()
        .add("a_b.proto", """
             |package a.b;
             |
             |message MessageB {
             |}
             """.trimMargin()
        )
        .add("a_b_c.proto", """
             |package a.b.c;
             |
             |import "a_b.proto";
             |
             |message MessageC {
             |  optional b.MessageB message_b = 1;
             |}
             """.trimMargin()
        )
        .schema()
    val messageC = schema.getType("a.b.c.MessageC") as MessageType
    assertThat(messageC.field("message_b")!!.type).isEqualTo(ProtoType.get("a.b.MessageB"))
  }

  @Test
  fun importResolvesNestedPackageSuffix() {
    val schema = RepoBuilder()
        .add("a_b.proto", """
             |package a.b;
             |
             |import "a_b_c.proto";
             |
             |message MessageB {
             |  optional c.MessageC message_c = 1;
             |}
             """.trimMargin()
        )
        .add("a_b_c.proto", """
             |package a.b.c;
             |
             |message MessageC {
             |}
             """.trimMargin()
        )
        .schema()
    val messageC = schema.getType("a.b.MessageB") as MessageType
    assertThat(messageC.field("message_c")!!.type).isEqualTo(ProtoType.get("a.b.c.MessageC"))
  }

  @Test
  fun nestedPackagePreferredOverEnclosingPackage() {
    val schema = RepoBuilder()
        .add("a.proto", """
             |package a;
             |
             |message MessageA {
             |}
             """.trimMargin()
        )
        .add("a_b.proto", """
             |package a.b;
             |
             |import "a.proto";
             |import "a_b_a.proto";
             |
             |message MessageB {
             |  optional a.MessageA message_a = 1;
             |}
             """.trimMargin()
        )
        .add("a_b_a.proto", """
             |package a.b.a;
             |
             |message MessageA {
             |}
             """.trimMargin()
        )
        .schema()
    val messageC = schema.getType("a.b.MessageB") as MessageType
    assertThat(messageC.field("message_a")!!.type).isEqualTo(ProtoType.get("a.b.a.MessageA"))
  }

  @Test
  fun dotPrefixRefersToRootPackage() {
    val schema = RepoBuilder()
        .add("a.proto", """
             |package a;
             |
             |message MessageA {
             |}
             """.trimMargin()
        )
        .add("a_b.proto", """
             |package a.b;
             |
             |import "a.proto";
             |import "a_b_a.proto";
             |
             |message MessageB {
             |  optional .a.MessageA message_a = 1;
             |}
             """.trimMargin()
        )
        .add("a_b_a.proto", """
             |package a.b.a;
             |
             |message MessageA {
             |}
             """.trimMargin()
        )
        .schema()
    val messageC = schema.getType("a.b.MessageB") as MessageType
    assertThat(messageC.field("message_a")!!.type).isEqualTo(ProtoType.get("a.MessageA"))
  }

  @Test
  fun dotPrefixMustBeRoot() {
    try {
      RepoBuilder()
          .add("a_b.proto", """
               |package a.b;
               |
               |message MessageB {
               |}
               """.trimMargin()
          )
          .add("a_b_c.proto", """
               |package a.b.c;
               |
               |import "a_b.proto";
               |
               |message MessageC {
               |  optional .b.MessageB message_b = 1;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |unable to resolve .b.MessageB
            |  for field message_b (/source/a_b_c.proto at 6:3)
            |  in message a.b.c.MessageC (/source/a_b_c.proto at 5:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun groupsThrow() {
    try {
      RepoBuilder()
          .add("test.proto", """
               |message SearchResponse {
               |  repeated group Result = 1 {
               |    required string url = 2;
               |    optional string title = 3;
               |    repeated string snippets = 4;
               |  }
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage("/source/test.proto at 2:3: 'group' is not supported")
    }
  }

  @Test
  fun oneOfGroupsThrow() {
    try {
      RepoBuilder()
          .add("test.proto", """
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
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage("/source/test.proto at 5:5: 'group' is not supported")
    }
  }

  @Test
  fun reservedTagThrowsWhenUsed() {
    try {
      RepoBuilder()
          .add("test.proto", """
               |message Message {
               |  reserved 1;
               |  optional string name = 1;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |tag 1 is reserved (/source/test.proto at 2:3)
            |  for field name (/source/test.proto at 3:3)
            |  in message Message (/source/test.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun reservedTagRangeThrowsWhenUsed() {
    try {
      RepoBuilder()
          .add("test.proto", """
               |message Message {
               |  reserved 1 to 3;
               |  optional string name = 2;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |tag 2 is reserved (/source/test.proto at 2:3)
            |  for field name (/source/test.proto at 3:3)
            |  in message Message (/source/test.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun reservedNameThrowsWhenUsed() {
    try {
      RepoBuilder()
          .add("test.proto", """
               |message Message {
               |  reserved 'foo';
               |  optional string foo = 1;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |name 'foo' is reserved (/source/test.proto at 2:3)
            |  for field foo (/source/test.proto at 3:3)
            |  in message Message (/source/test.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun reservedTagAndNameBothReported() {
    try {
      RepoBuilder()
          .add("test.proto", """
               |message Message {
               |  reserved 'foo';
               |  reserved 1;
               |  optional string foo = 1;
               |}
               """.trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |name 'foo' is reserved (/source/test.proto at 2:3)
            |  for field foo (/source/test.proto at 4:3)
            |  in message Message (/source/test.proto at 1:1)
            |tag 1 is reserved (/source/test.proto at 3:3)
            |  for field foo (/source/test.proto at 4:3)
            |  in message Message (/source/test.proto at 1:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun proto3EnumShouldHaveZeroValueAtFirstPosition() {
    val schema = RepoBuilder()
        .add("period.proto", """
            |syntax = "proto3";
            |
            |enum Period {
            |  ZERO = 0;
            |  CRETACEOUS = 1;
            |  JURASSIC = 2;
            |  TRIASSIC = 3;
            |}
            |""".trimMargin())
        .schema()
    val enum = schema.getType("Period") as EnumType
    assertThat(enum.constant("ZERO")).isNotNull
    assertThat(enum.constant("CRETACEOUS")).isNotNull
    assertThat(enum.constant("JURASSIC")).isNotNull
    assertThat(enum.constant("TRIASSIC")).isNotNull
  }

  @Test
  fun proto3EnumMustHaveZeroValue() {
    try {
      RepoBuilder()
          .add("period.proto", """
              |syntax = "proto3";
              |
              |enum Period {
              |  CRETACEOUS = 1;
              |  JURASSIC = 2;
              |  TRIASSIC = 3;
              |}
              |""".trimMargin())
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
          |missing a zero value at the first element [proto3]
          |  for enum Period (/source/period.proto at 3:1)
          """.trimMargin()
      )
    }
  }

  @Test
  fun proto3EnumMustHaveZeroValueAtFirstPosition() {
    try {
      RepoBuilder()
          .add("period.proto", """
              |syntax = "proto3";
              |
              |enum Period {
              |  CRETACEOUS = 1;
              |  CHAOS = 0;
              |  JURASSIC = 2;
              |  TRIASSIC = 3;
              |}
              |""".trimMargin())
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
          |missing a zero value at the first element [proto3]
          |  for enum Period (/source/period.proto at 3:1)
          """.trimMargin()
      )
    }
  }

  @Test
  fun proto3EnumMustNotBeEmpty() {
    try {
      RepoBuilder()
          .add("period.proto", """
              |syntax = "proto3";
              |
              |enum Period {}
              |""".trimMargin())
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
          |missing a zero value at the first element [proto3]
          |  for enum Period (/source/period.proto at 3:1)
          """.trimMargin()
      )
    }
  }

  @Test
  fun proto2EnumNeedNotHaveZeroValue() {
    val schema = RepoBuilder()
        .add("period.proto", """
            |syntax = "proto2";
            |
            |enum Period {
            |  CRETACEOUS = 1;
            |  JURASSIC = 2;
            |  TRIASSIC = 3;
            |}
            |""".trimMargin())
        .schema()
    val enum = schema.getType("Period") as EnumType
    assertThat(enum.constant("ZERO")).isNull()
    assertThat(enum.constant("CRETACEOUS")).isNotNull
    assertThat(enum.constant("JURASSIC")).isNotNull
    assertThat(enum.constant("TRIASSIC")).isNotNull
  }

  @Test
  fun proto2EnumNeedNotHaveZeroValueWithoutSyntax() {
    val schema = RepoBuilder()
        .add("period.proto", """
            |enum Period {
            |  CRETACEOUS = 1;
            |  JURASSIC = 2;
            |  TRIASSIC = 3;
            |}
            |""".trimMargin())
        .schema()
    val enum = schema.getType("Period") as EnumType
    assertThat(enum.constant("ZERO")).isNull()
    assertThat(enum.constant("CRETACEOUS")).isNotNull
    assertThat(enum.constant("JURASSIC")).isNotNull
    assertThat(enum.constant("TRIASSIC")).isNotNull
  }

  @Test
  fun proto3CanExtendCustomOption() {
    val schema = RepoBuilder()
        .add("test.proto", """
            |syntax = "proto3";
            |import "google/protobuf/descriptor.proto";
            |
            |extend google.protobuf.FieldOptions {
            |  string a = 22101;
            |}
            |message Message {
            |  string title = 1 [(a) = "hello"];
            |}
            """.trimMargin())
        .schema()
    val fieldOptions = schema.getType("google.protobuf.FieldOptions") as MessageType
    assertThat(fieldOptions.extensionField("a")!!.type).isEqualTo(ProtoType.get("string"))
  }

  @Test
  fun proto3CannotExtendNonCustomOption() {
    try {
      RepoBuilder()
          .add("dinosaur.proto", """
              |syntax = "proto3";
              |
              |message Dinosaur {
              |  string name = 1;
              |}
              |
              |extend Dinosaur {
              |  bool scary = 2;
              |}
              |""".trimMargin())
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
          |extensions are not allowed [proto3]
          |  for extend Dinosaur (/source/dinosaur.proto at 7:1)
          """.trimMargin()
      )
    }
  }

  @Test
  fun proto3DoesNotAllowUserDefinedDefaultValue() {
    try {
      RepoBuilder()
          .add("dinosaur.proto", """
              |syntax = "proto3";
              |
              |message Dinosaur {
              |  string name = 1 [default = "T-Rex"];
              |}
              |""".trimMargin()
          )
          .schema()
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
            |user-defined default values are not permitted [proto3]
            |  for field name (/source/dinosaur.proto at 4:3)
            |  in message Dinosaur (/source/dinosaur.proto at 3:1)
            """.trimMargin()
      )
    }
  }

  @Test
  fun repeatedNumericScalarsShouldBePackedByDefaultForProto3() {
    val schema = RepoBuilder()
        .add("message.proto", """
            |syntax = "proto3";
            |
            |message Message {
            |  repeated OtherMessage a = 1;
            |  repeated bool b = 2;
            |  repeated bytes c = 3;
            |  repeated string d = 4;
            |
            |  repeated double e = 5;
            |  repeated float f = 6;
            |  repeated fixed32 g = 7;
            |  repeated fixed64 h = 8;
            |  repeated int32 i = 9;
            |  repeated int64 j = 10;
            |  repeated sfixed32 k = 11;
            |  repeated sfixed64 l = 12;
            |  repeated sint32 m = 13;
            |  repeated sint64 n = 14;
            |  repeated uint32 o = 15;
            |  repeated uint64 p = 16;
            |
            |  repeated int32 set_to_false = 17 [packed = false];
            |  repeated int32 set_to_true = 18 [packed = true];
            |}
            |
            |message OtherMessage {}
            |""".trimMargin()
        ).schema()

    val messageType = schema.getType("Message") as MessageType
    // Default to false.
    assertThat(messageType.field("a")!!.isPacked).isFalse()
    assertThat(messageType.field("b")!!.isPacked).isFalse()
    assertThat(messageType.field("c")!!.isPacked).isFalse()
    assertThat(messageType.field("d")!!.isPacked).isFalse()

    // Repeated numeric scalar default to true.
    assertThat(messageType.field("e")!!.isPacked).isTrue()
    assertThat(messageType.field("f")!!.isPacked).isTrue()
    assertThat(messageType.field("g")!!.isPacked).isTrue()
    assertThat(messageType.field("h")!!.isPacked).isTrue()
    assertThat(messageType.field("i")!!.isPacked).isTrue()
    assertThat(messageType.field("j")!!.isPacked).isTrue()
    assertThat(messageType.field("k")!!.isPacked).isTrue()
    assertThat(messageType.field("l")!!.isPacked).isTrue()
    assertThat(messageType.field("m")!!.isPacked).isTrue()
    assertThat(messageType.field("n")!!.isPacked).isTrue()
    assertThat(messageType.field("o")!!.isPacked).isTrue()
    assertThat(messageType.field("p")!!.isPacked).isTrue()

    // Don't override set packed.
    assertThat(messageType.field("set_to_false")!!.isPacked).isFalse()
    assertThat(messageType.field("set_to_true")!!.isPacked).isTrue()
  }

  @Test fun deprecatedOptionsForProto3() {
    val deprecatedOptionElement = OptionElement.create(name = "deprecated",
        kind = OptionElement.Kind.BOOLEAN, value = "true", isParenthesized = false)

    val schema = RepoBuilder()
        .add("message.proto", """
            |option deprecated = true;
            |
            |message Message {
            |  option deprecated = true;
            |  optional string s = 1 [deprecated = true];
            |}
            |
            |enum Enum {
            |  option deprecated = true;
            |  A = 1 [deprecated = true];
            |}
            |
            |service Service {
            |  option deprecated = true;
            |  rpc Call (Request) returns (Response) {
            |    option deprecated = true;
            |  };
            |}
            |message Request {}
            |message Response {}
            |""".trimMargin()
        ).schema()

    assertThat(schema.protoFile("message.proto")!!.options.elements).contains(deprecatedOptionElement)
    assertThat(schema.getType("Message")!!.options.elements).contains(deprecatedOptionElement)
    assertThat((schema.getType("Message")!! as MessageType).field("s")!!.options.elements).contains(deprecatedOptionElement)
    assertThat(schema.getType("Enum")!!.options.elements).contains(deprecatedOptionElement)
    assertThat((schema.getType("Enum")!! as EnumType).constant("A")!!.options.elements).contains(deprecatedOptionElement)
    assertThat(schema.getService("Service")!!.options().elements).contains(deprecatedOptionElement)
    assertThat(schema.getService("Service")!!.rpc("Call")!!.options.elements).contains(deprecatedOptionElement)
  }
}
