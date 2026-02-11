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
@file:Suppress("UsePropertyAccessSyntax")

package com.squareup.wire.schema

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasMessage
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.message
import com.squareup.wire.buildSchema
import com.squareup.wire.schema.Options.Companion.FIELD_OPTIONS
import com.squareup.wire.schema.Options.Companion.ONEOF_OPTIONS
import com.squareup.wire.schema.internal.isValidTag
import com.squareup.wire.schema.internal.parser.OptionElement
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.fail
import okio.Path.Companion.toPath

class SchemaTest {
  @Test
  fun linkService() {
    val schema = buildSchema {
      add(
        "service.proto".toPath(),
        """
        |import "request.proto";
        |import "response.proto";
        |service Service {
        |  rpc Call (Request) returns (Response);
        |}
        """.trimMargin(),
      )
      add(
        "request.proto".toPath(),
        """
        |message Request {
        |}
        """.trimMargin(),
      )
      add(
        "response.proto".toPath(),
        """
        |message Response {
        |}
        """.trimMargin(),
      )
    }

    val service = schema.getService("Service")!!
    val call = service.rpc("Call")!!
    assertThat(call.requestType).isEqualTo(schema.getType("Request")!!.type)
    assertThat(call.responseType).isEqualTo(schema.getType("Response")!!.type)
  }

  @Test
  fun linkMessage() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |import "foo.proto";
        |message Message {
        |  optional foo_package.Foo field = 1;
        |  map<string, foo_package.Bar> bars = 2;
        |}
        """.trimMargin(),
      )
      add(
        "foo.proto".toPath(),
        """
        |package foo_package;
        |message Foo {
        |}
        |message Bar {
        |}
        """.trimMargin(),
      )
    }

    val message = schema.getType("Message") as MessageType
    val field = message.field("field")!!
    assertThat(field.type).isEqualTo(schema.getType("foo_package.Foo")!!.type)
    val bars = message.field("bars")!!.type
    assertThat(bars!!.keyType).isEqualTo(ProtoType.STRING)
    assertThat(bars.valueType).isEqualTo(schema.getType("foo_package.Bar")!!.type)
  }

  @Ignore // Resolution happens from the root not from inside Outer and so this fails.
  @Test
  fun linkExtendTypeInOuterMessage() {
    val schema = buildSchema {
      add(
        "foo.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }

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
      buildSchema {
        add(
          "message.proto".toPath(),
          """
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
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected.message).isEqualTo(
        """
        |tag is out of range: 0
        |  for field a (/sourcePath/message.proto:2:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        |tag is out of range: 19000
        |  for field d (/sourcePath/message.proto:5:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        |tag is out of range: 19999
        |  for field e (/sourcePath/message.proto:6:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        |tag is out of range: 536870912
        |  for field h (/sourcePath/message.proto:9:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun extensionsInvalidTag() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {
          |  extensions 0;
          |  extensions 1;
          |  extensions 18999;
          |  extensions 19000, 19001 to 19998, 19999;
          |  extensions 20000;
          |  extensions 536870911;
          |  extensions 536870912;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |tags are out of range: 0
        |  for extensions (/sourcePath/message.proto:2:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        |tags are out of range: 19000, 19001 to 19998, 19999
        |  for extensions (/sourcePath/message.proto:5:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        |tags are out of range: 536870912
        |  for extensions (/sourcePath/message.proto:8:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun scalarFieldIsPacked() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |message Message {
        |  repeated int32 a = 1;
        |  repeated int32 b = 2 [packed=false];
        |  repeated int32 c = 3 [packed=true];
        |}
        """.trimMargin(),
      )
    }

    val message = schema.getType("Message") as MessageType
    assertThat(message.field("a")!!.isPacked).isFalse()
    assertThat(message.field("b")!!.isPacked).isFalse()
    assertThat(message.field("c")!!.isPacked).isTrue()
  }

  @Test
  fun enumFieldIsPacked() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |message Message {
        |  repeated HabitablePlanet home_planet = 1 [packed=true];
        |  enum HabitablePlanet {
        |    EARTH = 1;
        |  }
        |}
        """.trimMargin(),
      )
    }
    val message = schema.getType("Message") as MessageType
    assertThat(message.field("home_planet")!!.isPacked).isTrue()
  }

  @Test
  fun fieldIsPackedButShouldntBe() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
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
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |packed=true not permitted on bytes
        |  for field b (/sourcePath/message.proto:3:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        |packed=true not permitted on string
        |  for field d (/sourcePath/message.proto:5:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        |packed=true not permitted on Message
        |  for field f (/sourcePath/message.proto:7:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        |packed=true not permitted on bytes
        |  for field h (/sourcePath/message.proto:11:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun fieldUsesUseArrayButShouldntBe() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |import "wire/extensions.proto";
          |
          |message Message {
          |  repeated bytes a = 1 [wire.use_array=true];
          |  repeated Message b = 2 [wire.use_array=true];
          |  repeated float c = 3 [wire.use_array=true];
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |wire.use_array=true only permitted on packed fields
        |  for field a (/sourcePath/message.proto:4:3)
        |  in message Message (/sourcePath/message.proto:3:1)
        |wire.use_array=true only permitted on packed fields
        |  for field b (/sourcePath/message.proto:5:3)
        |  in message Message (/sourcePath/message.proto:3:1)
        |wire.use_array=true only permitted on scalar fields
        |  for field b (/sourcePath/message.proto:5:3)
        |  in message Message (/sourcePath/message.proto:3:1)
        |wire.use_array=true only permitted on packed fields
        |  for field c (/sourcePath/message.proto:6:3)
        |  in message Message (/sourcePath/message.proto:3:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun fieldIsDeprecated() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |message Message {
        |  optional int32 a = 1;
        |  optional int32 b = 2 [deprecated=false];
        |  optional int32 c = 3 [deprecated=true];
        |}
        """.trimMargin(),
      )
    }

    val message = schema.getType("Message") as MessageType
    assertThat(message.field("a")!!.isDeprecated).isFalse()
    assertThat(message.field("b")!!.isDeprecated).isFalse()
    assertThat(message.field("c")!!.isDeprecated).isTrue()
  }

  @Test
  fun fieldDefault() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
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
        """.trimMargin(),
      )
    }

    val message = schema.getType("Message") as MessageType
    assertThat(message.field("a")!!.default).isNull()
    assertThat(message.field("b")!!.default).isEqualTo("5")
    assertThat(message.field("c")!!.default).isEqualTo("true")
    assertThat(message.field("d")!!.default).isEqualTo("foo")
    assertThat(message.field("e")!!.default).isEqualTo("PAPER")
  }

  @Test
  fun fieldOptions() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |import "google/protobuf/descriptor.proto";
        |message Message {
        |  optional int32 a = 1;
        |  optional int32 b = 2 [color=red, deprecated=true, packed=true];
        |}
        |extend google.protobuf.FieldOptions {
        |  optional string color = 60001;
        |}
        """.trimMargin(),
      )
    }
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
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |import "google/protobuf/descriptor.proto";
          |message Message {
          |  optional int32 a = 1 [color=red, color=blue];
          |}
          |extend google.protobuf.FieldOptions {
          |  optional string color = 60001;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |conflicting options: red, blue
        |  for field a (/sourcePath/message.proto:3:3)
        |  in message Message (/sourcePath/message.proto:2:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun messageFieldTypeUnknown() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {
          |  optional foo_package.Foo unknown = 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |unable to resolve foo_package.Foo
        |  for field unknown (/sourcePath/message.proto:2:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun oneOfFieldTypeUnknown() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {
          |  oneof selection {
          |    int32 known = 1;
          |    foo_package.Foo unknown = 2;
          |  }
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |unable to resolve foo_package.Foo
        |  for field unknown (/sourcePath/message.proto:4:5)
        |  in message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun serviceTypesMustBeNamed() {
    try {
      buildSchema {
        add(
          "service.proto".toPath(),
          """
          |service Service {
          |  rpc Call (string) returns (Response);
          |}
          |message Response {
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |expected a message but was string
        |  for rpc Call (/sourcePath/service.proto:2:3)
        |  in service Service (/sourcePath/service.proto:1:1)
        """.trimMargin(),
      )
    }

    try {
      buildSchema {
        add(
          "service.proto".toPath(),
          """
          |service Service {
          |  rpc Call (Request) returns (string);
          |}
          |message Request {
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |expected a message but was string
        |  for rpc Call (/sourcePath/service.proto:2:3)
        |  in service Service (/sourcePath/service.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun serviceTypesUnknown() {
    try {
      buildSchema {
        add(
          "service.proto".toPath(),
          """
          |service Service {
          |  rpc Call (foo_package.Foo) returns (Response);
          |}
          |message Response {
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |unable to resolve foo_package.Foo
        |  for rpc Call (/sourcePath/service.proto:2:3)
        |  in service Service (/sourcePath/service.proto:1:1)
        """.trimMargin(),
      )
    }

    try {
      buildSchema {
        add(
          "service.proto".toPath(),
          """
          |service Service {
          |  rpc Call (Request) returns (foo_package.Foo);
          |}
          |message Request {
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |unable to resolve foo_package.Foo
        |  for rpc Call (/sourcePath/service.proto:2:3)
        |  in service Service (/sourcePath/service.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun extendedTypeUnknown() {
    try {
      buildSchema {
        add(
          "extend.proto".toPath(),
          """
          |extend foo_package.Foo {
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |unable to resolve foo_package.Foo
        |  for extend (/sourcePath/extend.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun extendedTypeMustBeNamed() {
    try {
      buildSchema {
        add(
          "extend.proto".toPath(),
          """
          |extend string {
          |  optional Value value = 1000;
          |}
          |message Value {
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |expected a message but was string
        |  for extend (/sourcePath/extend.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun extendFieldTypeUnknown() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {
          |}
          |extend Message {
          |  optional foo_package.Foo unknown = 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |unable to resolve foo_package.Foo
        |  for field unknown (/sourcePath/message.proto:4:3)
        |  in extend Message (/sourcePath/message.proto:3:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun multipleErrors() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {
          |  optional foo_package.Foo unknown = 1;
          |  optional foo_package.Foo also_unknown = 2;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |unable to resolve foo_package.Foo
        |  for field unknown (/sourcePath/message.proto:2:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        |unable to resolve foo_package.Foo
        |  for field also_unknown (/sourcePath/message.proto:3:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun duplicateMessageTagDisallowed() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {
          |  required string name1 = 1;
          |  required string name2 = 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |multiple fields share tag 1:
        |  1. name1 (/sourcePath/message.proto:2:3)
        |  2. name2 (/sourcePath/message.proto:3:3)
        |  for message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun duplicateTagValueDisallowedInOneOf() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {
          |  required string name1 = 1;
          |  oneof selection {
          |    string name2 = 1;
          |  }
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |multiple fields share tag 1:
        |  1. name1 (/sourcePath/message.proto:2:3)
        |  2. name2 (/sourcePath/message.proto:4:5)
        |  for message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun duplicateExtendTagDisallowed() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {
          |}
          |extend Message {
          |  optional string name1 = 1;
          |  optional string name2 = 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |multiple fields share tag 1:
        |  1. name1 (/sourcePath/message.proto:4:3)
        |  2. name2 (/sourcePath/message.proto:5:3)
        |  for message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun messageNameCollisionDisallowed() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {
          |  optional string a = 1;
          |  optional string a = 2;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |multiple fields share name a:
        |  1. a (/sourcePath/message.proto:2:3)
        |  2. a (/sourcePath/message.proto:3:3)
        |  for message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun messageAndExtensionNameCollision() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |message Message {
        |  optional string a = 1;
        |}
        """.trimMargin(),
      )
      add(
        "extend.proto".toPath(),
        """
        |package p;
        |import "message.proto";
        |extend Message {
        |  optional string a = 2;
        |}
        """.trimMargin(),
      )
    }
    val messageType = schema.getType("Message") as MessageType

    assertThat(messageType.field("a")!!.tag).isEqualTo(1)
    assertThat(messageType.extensionField("p.a")!!.tag).isEqualTo(2)
  }

  @Test
  fun extendNameCollisionInSamePackageDisallowed() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {
          |}
          """.trimMargin(),
        )
        add(
          "extend1.proto".toPath(),
          """
          |import "message.proto";
          |extend Message {
          |  optional string a = 1;
          |}
          """.trimMargin(),
        )
        add(
          "extend2.proto".toPath(),
          """
          |import "message.proto";
          |extend Message {
          |  optional string a = 2;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |multiple fields share name a:
        |  1. a (/sourcePath/extend1.proto:3:3)
        |  2. a (/sourcePath/extend2.proto:3:3)
        |  for message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun extendNameCollisionInDifferentPackagesAllowed() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |message Message {
        |}
        """.trimMargin(),
      )
      add(
        "extend1.proto".toPath(),
        """
        |package p1;
        |import "message.proto";
        |extend Message {
        |  optional string a = 1;
        |}
        """.trimMargin(),
      )
      add(
        "extend2.proto".toPath(),
        """
        |package p2;
        |import "message.proto";
        |extend Message {
        |  optional string a = 2;
        |}
        """.trimMargin(),
      )
    }
    val messageType = schema.getType("Message") as MessageType

    assertThat(messageType.field("a")).isNull()
    assertThat(messageType.extensionField("p1.a")!!.namespaces).isEqualTo(listOf("p1"))
    assertThat(messageType.extensionField("p2.a")!!.namespaces).isEqualTo(listOf("p2"))
  }

  @Test
  fun extendEnumDisallowed() {
    try {
      buildSchema {
        add(
          "enum.proto".toPath(),
          """
          |enum Enum {
          |  A = 1;
          |  B = 2;
          |}
          """.trimMargin(),
        )
        add(
          "extend.proto".toPath(),
          """
          |import "enum.proto";
          |extend Enum {
          |  optional string a = 2;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |expected a message but was Enum
        |  for extend (/sourcePath/extend.proto:2:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun requiredExtendFieldDisallowed() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {
          |}
          |extend Message {
          |  required string a = 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |extension fields cannot be required
        |  for field a (/sourcePath/message.proto:4:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun oneOfLabelDisallowed() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {
          |  oneof string s = 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage("Syntax error in /sourcePath/message.proto:2:17: expected '{' but was 's'")
    }
  }

  @Test
  fun duplicateEnumValueTagInScopeDisallowed() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {
          |  enum Enum1 {
          |    VALUE = 1;
          |  }
          |  enum Enum2 {
          |    VALUE = 2;
          |  }
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |multiple enums share constant VALUE:
        |  1. Message.Enum1.VALUE (/sourcePath/message.proto:3:5)
        |  2. Message.Enum2.VALUE (/sourcePath/message.proto:6:5)
        |  for message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun duplicateEnumConstantTagWithoutAllowAliasDisallowed() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |enum Enum {
          |  A = 1;
          |  B = 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |multiple enum constants share tag 1:
        |  1. A (/sourcePath/message.proto:2:3)
        |  2. B (/sourcePath/message.proto:3:3)
        |  for enum Enum (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun duplicateEnumConstantTagWithAllowAliasFalseDisallowed() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |enum Enum {
          |  option allow_alias = false;
          |  A = 1;
          |  B = 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |multiple enum constants share tag 1:
        |  1. A (/sourcePath/message.proto:3:3)
        |  2. B (/sourcePath/message.proto:4:3)
        |  for enum Enum (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun duplicateEnumConstantTagWithAllowAliasTrueAllowed() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |enum Enum {
        |  option allow_alias = true;
        |  A = 1;
        |  B = 1;
        |}
        """.trimMargin(),
      )
    }
    val enumType = schema.getType("Enum") as EnumType
    assertThat(enumType.constant("A")!!.tag).isEqualTo(1)
    assertThat(enumType.constant("B")!!.tag).isEqualTo(1)
  }

  @Test
  fun fieldTypeImported() {
    val schema = buildSchema {
      add(
        "a.proto".toPath(),
        """
        |package pa;
        |import "b.proto";
        |message A {
        |  optional pb.B b = 1;
        |}
        """.trimMargin(),
      )
      add(
        "b.proto".toPath(),
        """
        |package pb;
        |message B {
        |}
        """.trimMargin(),
      )
    }
    val a = schema.getType("pa.A") as MessageType
    val b = schema.getType("pb.B") as MessageType
    assertThat(a.field("b")!!.type).isEqualTo(b.type)
  }

  @Test
  fun fieldMapTypeImported() {
    val schema = buildSchema {
      add(
        "a.proto".toPath(),
        """
        |package pa;
        |import "b.proto";
        |message A {
        |  map<string, pb.B> b = 1;
        |}
        """.trimMargin(),
      )
      add(
        "b.proto".toPath(),
        """
        |package pb;
        |message B {
        |}
        """.trimMargin(),
      )
    }
    val a = schema.getType("pa.A") as MessageType
    val b = schema.getType("pb.B") as MessageType
    assertThat(a.field("b")!!.type!!.valueType).isEqualTo(b.type)
  }

  @Test
  fun fieldTypeNotImported() {
    try {
      buildSchema {
        add(
          "a.proto".toPath(),
          """
          |package pa;
          |message A {
          |  optional pb.B b = 1;
          |}
          """.trimMargin(),
        )
        add(
          "b.proto".toPath(),
          """
          |package pb;
          |message B {
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected.message).isEqualTo(
        """
        |a.proto needs to import b.proto
        |  for field b (/sourcePath/a.proto:3:3)
        |  in message pa.A (/sourcePath/a.proto:2:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun fieldMapTypeNotImported() {
    try {
      buildSchema {
        add(
          "a.proto".toPath(),
          """
          |package pa;
          |message A {
          |  map<string, pb.B> b = 1;
          |}
          """.trimMargin(),
        )
        add(
          "b.proto".toPath(),
          """
          |package pb;
          |message B {
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected.message).isEqualTo(
        """
        |a.proto needs to import b.proto
        |  for field b (/sourcePath/a.proto:3:3)
        |  in message pa.A (/sourcePath/a.proto:2:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun rpcTypeImported() {
    val schema = buildSchema {
      add(
        "a.proto".toPath(),
        """
        |package pa;
        |import "b.proto";
        |service Service {
        |  rpc Call (pb.B) returns (pb.B);
        |}
        """.trimMargin(),
      )
      add(
        "b.proto".toPath(),
        """
        |package pb;
        |message B {
        |}
        """.trimMargin(),
      )
    }
    val service = schema.getService("pa.Service")!!
    val b = schema.getType("pb.B") as MessageType
    assertThat(service.rpcs[0].requestType).isEqualTo(b.type)
    assertThat(service.rpcs[0].responseType).isEqualTo(b.type)
  }

  @Test
  fun rpcTypeNotImported() {
    try {
      buildSchema {
        add(
          "a.proto".toPath(),
          """
          |package pa;
          |service Service {
          |  rpc Call (pb.B) returns (pb.B);
          |}
          """.trimMargin(),
        )
        add(
          "b.proto".toPath(),
          """
          |package pb;
          |message B {
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected.message).isEqualTo(
        """
        |a.proto needs to import b.proto
        |  for rpc Call (/sourcePath/a.proto:3:3)
        |  in service pa.Service (/sourcePath/a.proto:2:1)
        |a.proto needs to import b.proto
        |  for rpc Call (/sourcePath/a.proto:3:3)
        |  in service pa.Service (/sourcePath/a.proto:2:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun extendTypeImported() {
    val schema = buildSchema {
      add(
        "a.proto".toPath(),
        """
        |package pa;
        |import "b.proto";
        |extend pb.B {
        |  optional string a = 1;
        |}
        """.trimMargin(),
      )
      add(
        "b.proto".toPath(),
        """
        |package pb;
        |message B {
        |  extensions 1;
        |}
        """.trimMargin(),
      )
    }
    val extendB = schema.protoFiles[0].extendList[0]
    val b = schema.getType("pb.B") as MessageType
    assertThat(extendB.type).isEqualTo(b.type)
  }

  @Test
  fun extendTypeNotImported() {
    try {
      buildSchema {
        add(
          "a.proto".toPath(),
          """
          |package pa;
          |extend pb.B {
          |  optional string a = 1;
          |}
          """.trimMargin(),
        )
        add(
          "b.proto".toPath(),
          """
          |package pb;
          |message B {
          |  extensions 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected.message).isEqualTo(
        """
        |a.proto needs to import b.proto
        |  for extend pb.B (/sourcePath/a.proto:2:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun transitiveImportNotFollowed() {
    try {
      buildSchema {
        add(
          "a.proto".toPath(),
          """
          |package pa;
          |import "b.proto";
          |message A {
          |  optional pc.C c = 1;
          |}
          """.trimMargin(),
        )
        add(
          "b.proto".toPath(),
          """
          |package pb;
          |import "c.proto";
          |message B {
          |}
          """.trimMargin(),
        )
        add(
          "c.proto".toPath(),
          """
          |package pc;
          |message C {
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected.message).isEqualTo(
        """
        |a.proto needs to import c.proto
        |  for field c (/sourcePath/a.proto:4:3)
        |  in message pa.A (/sourcePath/a.proto:3:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun transitivePublicImportFollowed() {
    val schema = buildSchema {
      add(
        "a.proto".toPath(),
        """
        |package pa;
        |import "b.proto";
        |message A {
        |  optional pc.C c = 1;
        |}
        """.trimMargin(),
      )
      add(
        "b.proto".toPath(),
        """
        |package pb;
        |import public "c.proto";
        |message B {
        |}
        """.trimMargin(),
      )
      add(
        "c.proto".toPath(),
        """
        |package pc;
        |message C {
        |}
        """.trimMargin(),
      )
    }
    val a = schema.getType("pa.A") as MessageType
    val c = schema.getType("pc.C") as MessageType
    assertThat(a.field("c")!!.type).isEqualTo(c.type)
  }

  @Test
  fun importSamePackageDifferentFile() {
    val schema = buildSchema {
      add(
        "a_b_1.proto".toPath(),
        """
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
        """.trimMargin(),
      )
      add(
        "a_b_2.proto".toPath(),
        """
        |package a.b;
        |
        |message MessageC {
        |}
        """.trimMargin(),
      )
    }
    val messageC = schema.getType("a.b.MessageB") as MessageType
    assertThat(messageC.field("c1")!!.type).isEqualTo(ProtoType.get("a.b.MessageC"))
    assertThat(messageC.field("c2")!!.type).isEqualTo(ProtoType.get("a.b.MessageC"))
    assertThat(messageC.field("c3")!!.type).isEqualTo(ProtoType.get("a.b.MessageC"))
    assertThat(messageC.field("c4")!!.type).isEqualTo(ProtoType.get("a.b.MessageC"))
  }

  @Test
  fun importResolvesEnclosingPackageSuffix() {
    val schema = buildSchema {
      add(
        "a_b.proto".toPath(),
        """
        |package a.b;
        |
        |message MessageB {
        |}
        """.trimMargin(),
      )
      add(
        "a_b_c.proto".toPath(),
        """
        |package a.b.c;
        |
        |import "a_b.proto";
        |
        |message MessageC {
        |  optional b.MessageB message_b = 1;
        |}
        """.trimMargin(),
      )
    }
    val messageC = schema.getType("a.b.c.MessageC") as MessageType
    assertThat(messageC.field("message_b")!!.type).isEqualTo(ProtoType.get("a.b.MessageB"))
  }

  @Test
  fun importResolvesNestedPackageSuffix() {
    val schema = buildSchema {
      add(
        "a_b.proto".toPath(),
        """
        |package a.b;
        |
        |import "a_b_c.proto";
        |
        |message MessageB {
        |  optional c.MessageC message_c = 1;
        |}
        """.trimMargin(),
      )
      add(
        "a_b_c.proto".toPath(),
        """
        |package a.b.c;
        |
        |message MessageC {
        |}
        """.trimMargin(),
      )
    }
    val messageC = schema.getType("a.b.MessageB") as MessageType
    assertThat(messageC.field("message_c")!!.type).isEqualTo(ProtoType.get("a.b.c.MessageC"))
  }

  @Test
  fun nestedPackagePreferredOverEnclosingPackage() {
    val schema = buildSchema {
      add(
        "a.proto".toPath(),
        """
        |package a;
        |
        |message MessageA {
        |}
        """.trimMargin(),
      )
      add(
        "a_b.proto".toPath(),
        """
        |package a.b;
        |
        |import "a.proto";
        |import "a_b_a.proto";
        |
        |message MessageB {
        |  optional a.MessageA message_a = 1;
        |}
        """.trimMargin(),
      )
      add(
        "a_b_a.proto".toPath(),
        """
        |package a.b.a;
        |
        |message MessageA {
        |}
        """.trimMargin(),
      )
    }
    val messageC = schema.getType("a.b.MessageB") as MessageType
    assertThat(messageC.field("message_a")!!.type).isEqualTo(ProtoType.get("a.b.a.MessageA"))
  }

  @Test
  fun dotPrefixRefersToRootPackage() {
    val schema = buildSchema {
      add(
        "a.proto".toPath(),
        """
        |package a;
        |
        |message MessageA {
        |}
        """.trimMargin(),
      )
      add(
        "a_b.proto".toPath(),
        """
        |package a.b;
        |
        |import "a.proto";
        |import "a_b_a.proto";
        |
        |message MessageB {
        |  optional .a.MessageA message_a = 1;
        |}
        """.trimMargin(),
      )
      add(
        "a_b_a.proto".toPath(),
        """
        |package a.b.a;
        |
        |message MessageA {
        |}
        """.trimMargin(),
      )
    }
    val messageC = schema.getType("a.b.MessageB") as MessageType
    assertThat(messageC.field("message_a")!!.type).isEqualTo(ProtoType.get("a.MessageA"))
  }

  @Test
  fun dotPrefixMustBeRoot() {
    try {
      buildSchema {
        add(
          "a_b.proto".toPath(),
          """
          |package a.b;
          |
          |message MessageB {
          |}
          """.trimMargin(),
        )
        add(
          "a_b_c.proto".toPath(),
          """
          |package a.b.c;
          |
          |import "a_b.proto";
          |
          |message MessageC {
          |  optional .b.MessageB message_b = 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |unable to resolve .b.MessageB
        |  for field message_b (/sourcePath/a_b_c.proto:6:3)
        |  in message a.b.c.MessageC (/sourcePath/a_b_c.proto:5:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun groupsThrow() {
    try {
      buildSchema {
        add(
          "test.proto".toPath(),
          """
          |message SearchResponse {
          |  repeated group Result = 1 {
          |    required string url = 2;
          |    optional string title = 3;
          |    repeated string snippets = 4;
          |  }
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage("/sourcePath/test.proto:2:3: 'group' is not supported")
    }
  }

  @Test
  fun oneOfGroupsThrow() {
    try {
      buildSchema {
        add(
          "test.proto".toPath(),
          """
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
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage("/sourcePath/test.proto:5:5: 'group' is not supported")
    }
  }

  @Test
  fun reservedTagThrowsWhenUsed() {
    try {
      buildSchema {
        add(
          "test.proto".toPath(),
          """
          |message Message {
          |  reserved 1;
          |  optional string name = 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |tag 1 is reserved (/sourcePath/test.proto:2:3)
        |  for field name (/sourcePath/test.proto:3:3)
        |  in message Message (/sourcePath/test.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun reservedTagThrowsWhenUsedForMessageWithMax() {
    try {
      buildSchema {
        add(
          "test.proto".toPath(),
          """
          |message Message {
          |  reserved 1 to max;
          |  optional string name = 3;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |tag 3 is reserved (/sourcePath/test.proto:2:3)
        |  for field name (/sourcePath/test.proto:3:3)
        |  in message Message (/sourcePath/test.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun reservedTagThrowsWhenUsedForEnums() {
    try {
      buildSchema {
        add(
          "test.proto".toPath(),
          """
          |enum Enum {
          |  reserved 3 to max, 'FOO';
          |  FOO = 2;
          |  NAME = 4;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |name 'FOO' is reserved (/sourcePath/test.proto:2:3)
        |  for constant FOO (/sourcePath/test.proto:3:3)
        |  in enum Enum (/sourcePath/test.proto:1:1)
        |tag 4 is reserved (/sourcePath/test.proto:2:3)
        |  for constant NAME (/sourcePath/test.proto:4:3)
        |  in enum Enum (/sourcePath/test.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun reservedTagRangeThrowsWhenUsed() {
    try {
      buildSchema {
        add(
          "test.proto".toPath(),
          """
          |message Message {
          |  reserved 1 to 3;
          |  optional string name = 2;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |tag 2 is reserved (/sourcePath/test.proto:2:3)
        |  for field name (/sourcePath/test.proto:3:3)
        |  in message Message (/sourcePath/test.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun reservedNameThrowsWhenUsed() {
    try {
      buildSchema {
        add(
          "test.proto".toPath(),
          """
          |message Message {
          |  reserved 'foo';
          |  optional string foo = 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |name 'foo' is reserved (/sourcePath/test.proto:2:3)
        |  for field foo (/sourcePath/test.proto:3:3)
        |  in message Message (/sourcePath/test.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun reservedTagAndNameBothReported() {
    try {
      buildSchema {
        add(
          "test.proto".toPath(),
          """
          |message Message {
          |  reserved 'foo';
          |  reserved 1;
          |  optional string foo = 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |name 'foo' is reserved (/sourcePath/test.proto:2:3)
        |  for field foo (/sourcePath/test.proto:4:3)
        |  in message Message (/sourcePath/test.proto:1:1)
        |tag 1 is reserved (/sourcePath/test.proto:3:3)
        |  for field foo (/sourcePath/test.proto:4:3)
        |  in message Message (/sourcePath/test.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun proto3EnumShouldHaveZeroValueAtFirstPosition() {
    val schema = buildSchema {
      add(
        "period.proto".toPath(),
        """
        |syntax = "proto3";
        |
        |enum Period {
        |  ZERO = 0;
        |  CRETACEOUS = 1;
        |  JURASSIC = 2;
        |  TRIASSIC = 3;
        |}
        |
        """.trimMargin(),
      )
    }
    val enum = schema.getType("Period") as EnumType
    assertThat(enum.constant("ZERO")).isNotNull()
    assertThat(enum.constant("CRETACEOUS")).isNotNull()
    assertThat(enum.constant("JURASSIC")).isNotNull()
    assertThat(enum.constant("TRIASSIC")).isNotNull()
  }

  @Test
  fun proto3EnumMustHaveZeroValue() {
    try {
      buildSchema {
        add(
          "period.proto".toPath(),
          """
         |syntax = "proto3";
         |
         |enum Period {
         |  CRETACEOUS = 1;
         |  JURASSIC = 2;
         |  TRIASSIC = 3;
         |}
         |
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
          |missing a zero value at the first element in proto3
          |  for enum Period (/sourcePath/period.proto:3:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun proto3EnumMustHaveZeroValueAtFirstPosition() {
    try {
      buildSchema {
        add(
          "period.proto".toPath(),
          """
         |syntax = "proto3";
         |
         |enum Period {
         |  CRETACEOUS = 1;
         |  CHAOS = 0;
         |  JURASSIC = 2;
         |  TRIASSIC = 3;
         |}
         |
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
          |missing a zero value at the first element in proto3
          |  for enum Period (/sourcePath/period.proto:3:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun proto3EnumMustNotBeEmpty() {
    try {
      buildSchema {
        add(
          "period.proto".toPath(),
          """
         |syntax = "proto3";
         |
         |enum Period {}
         |
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
          |missing a zero value at the first element in proto3
          |  for enum Period (/sourcePath/period.proto:3:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun proto2EnumNeedNotHaveZeroValue() {
    val schema = buildSchema {
      add(
        "period.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |enum Period {
        |  CRETACEOUS = 1;
        |  JURASSIC = 2;
        |  TRIASSIC = 3;
        |}
        |
        """.trimMargin(),
      )
    }
    val enum = schema.getType("Period") as EnumType
    assertThat(enum.constant("ZERO")).isNull()
    assertThat(enum.constant("CRETACEOUS")).isNotNull()
    assertThat(enum.constant("JURASSIC")).isNotNull()
    assertThat(enum.constant("TRIASSIC")).isNotNull()
  }

  @Test
  fun proto2EnumNeedNotHaveZeroValueWithoutSyntax() {
    val schema = buildSchema {
      add(
        "period.proto".toPath(),
        """
        |enum Period {
        |  CRETACEOUS = 1;
        |  JURASSIC = 2;
        |  TRIASSIC = 3;
        |}
        |
        """.trimMargin(),
      )
    }
    val enum = schema.getType("Period") as EnumType
    assertThat(enum.constant("ZERO")).isNull()
    assertThat(enum.constant("CRETACEOUS")).isNotNull()
    assertThat(enum.constant("JURASSIC")).isNotNull()
    assertThat(enum.constant("TRIASSIC")).isNotNull()
  }

  @Test
  fun proto3CanExtendCustomOption() {
    val schema = buildSchema {
      add(
        "test.proto".toPath(),
        """
        |syntax = "proto3";
        |import "google/protobuf/descriptor.proto";
        |
        |extend google.protobuf.FieldOptions {
        |  string a = 22101;
        |}
        |message Message {
        |  string title = 1 [(a) = "hello"];
        |}
        """.trimMargin(),
      )
    }
    val fieldOptions = schema.getType("google.protobuf.FieldOptions") as MessageType
    assertThat(fieldOptions.extensionField("a")!!.type).isEqualTo(ProtoType.get("string"))
  }

  @Test
  fun proto3CanExtendCustomOptionWithLeadingDot() {
    val schema = buildSchema {
      add(
        "test.proto".toPath(),
        """
        |syntax = "proto3";
        |import "google/protobuf/descriptor.proto";
        |
        |extend .google.protobuf.FieldOptions {
        |  string a = 22101;
        |}
        |message Message {
        |  string title = 1 [(a) = "hello"];
        |}
        """.trimMargin(),
      )
    }
    val fieldOptions = schema.getType("google.protobuf.FieldOptions") as MessageType
    assertThat(fieldOptions.extensionField("a")!!.type).isEqualTo(ProtoType.get("string"))
  }

  @Test
  fun oneofOption() {
    val schema = buildSchema {
      add(
        "test.proto".toPath(),
        """
        |syntax = "proto3";
        |import "google/protobuf/descriptor.proto";
        |
        |extend google.protobuf.OneofOptions {
        |  string my_oneof_option = 22101;
        |}
        |message Message {
        |  oneof choice {
        |    option (my_oneof_option) = "Well done";
        |
        |    string one = 1;
        |    string two = 2;
        |  }
        |}
        """.trimMargin(),
      )
    }
    val fieldOptions = schema.getType("google.protobuf.OneofOptions") as MessageType
    assertThat(fieldOptions.extensionField("my_oneof_option")!!.type).isEqualTo(
      ProtoType.get("string"),
    )
    val choiceOneOf = (schema.getType("Message") as MessageType).oneOfs[0]
    assertThat(choiceOneOf.name).isEqualTo("choice")
    assertThat(choiceOneOf.options.get(ProtoMember.get(ONEOF_OPTIONS, "my_oneof_option")))
      .isEqualTo("Well done")
  }

  @Test
  fun proto3CannotExtendNonCustomOption() {
    try {
      buildSchema {
        add(
          "dinosaur.proto".toPath(),
          """
         |syntax = "proto3";
         |
         |message Dinosaur {
         |  string name = 1;
         |}
         |
         |extend Dinosaur {
         |  bool scary = 2;
         |}
         |
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
          |extensions are not allowed in proto3
          |  for extend Dinosaur (/sourcePath/dinosaur.proto:7:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun proto3DoesNotAllowUserDefinedDefaultValue() {
    try {
      buildSchema {
        add(
          "dinosaur.proto".toPath(),
          """
         |syntax = "proto3";
         |
         |message Dinosaur {
         |  string name = 1 [default = "T-Rex"];
         |}
         |
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |user-defined default values are not permitted in proto3
        |  for field name (/sourcePath/dinosaur.proto:4:3)
        |  in message Dinosaur (/sourcePath/dinosaur.proto:3:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun repeatedNumericScalarsShouldBePackedByDefaultForProto3() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
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
        |
        """.trimMargin(),
      )
    }

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
    val deprecatedOptionElement = OptionElement.create(
      name = "deprecated",
      kind = OptionElement.Kind.BOOLEAN,
      value = "true",
      isParenthesized = false,
    )

    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
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
        |
        """.trimMargin(),
      )
    }

    assertThat(schema.protoFile("message.proto")!!.options.elements)
      .contains(deprecatedOptionElement)
    assertThat(schema.getType("Message")!!.options.elements)
      .contains(deprecatedOptionElement)
    assertThat((schema.getType("Message")!! as MessageType).field("s")!!.options.elements)
      .contains(deprecatedOptionElement)
    assertThat(schema.getType("Enum")!!.options.elements)
      .contains(deprecatedOptionElement)
    assertThat((schema.getType("Enum")!! as EnumType).constant("A")!!.options.elements)
      .contains(deprecatedOptionElement)
    assertThat(schema.getService("Service")!!.options.elements)
      .contains(deprecatedOptionElement)
    assertThat(schema.getService("Service")!!.rpc("Call")!!.options.elements)
      .contains(deprecatedOptionElement)
  }

  @Test
  fun forbidConflictingCamelCasedNamesInProto3() {
    try {
      buildSchema {
        add(
          "dinosaur.proto".toPath(),
          """
         |syntax = "proto3";
         |
         |message Dinosaur {
         |  string myName = 1;
         |  string my_name = 2;
         |}
         |
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |multiple fields share same JSON name 'myName':
        |  1. myName (/sourcePath/dinosaur.proto:4:3)
        |  2. my_name (/sourcePath/dinosaur.proto:5:3)
        |  for message Dinosaur (/sourcePath/dinosaur.proto:3:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun noConflictWhenJsonNameResolvesItInProto3() {
    // Both fields' camel-cased name would conflict but since `json_name` takes precedence, there
    // shouldn't be any conflict here.
    val schema = buildSchema {
      add(
        "dinosaur.proto".toPath(),
        """
         |syntax = "proto3";
         |
         |message Dinosaur {
         |  string myName = 1 [json_name = "one"];
         |  string my_name = 2 [json_name = "two"];
         |}
         |
        """.trimMargin(),
      )
    }
    assertThat(schema).isNotNull()
  }

  @Test
  fun forbidConflictingJsonNames() {
    try {
      buildSchema {
        add(
          "dinosaur.proto".toPath(),
          """
         |message Dinosaur {
         |  optional string myName = 1 [json_name = "JsonName"];
         |  optional string my_name = 2 [json_name = "JsonName"];
         |}
         |
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |multiple fields share same JSON name 'JsonName':
        |  1. myName (/sourcePath/dinosaur.proto:2:3)
        |  2. my_name (/sourcePath/dinosaur.proto:3:3)
        |  for message Dinosaur (/sourcePath/dinosaur.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun allowConflictingCamelCasedNamesInProto2() {
    val schema = buildSchema {
      add(
        "dinosaur.proto".toPath(),
        """
         |syntax = "proto2";
         |
         |message Dinosaur {
         |  optional string myName = 1;
         |  optional string my_name = 2;
         |}
         |
        """.trimMargin(),
      )
    }
    assertThat(schema.getType("Dinosaur")).isNotNull()
  }

  @Test
  fun nestedOptionSetting() {
    val schema = buildSchema {
      add(
        "dinosaur.proto".toPath(),
        """
         |package wire;
         |import 'google/protobuf/descriptor.proto';
         |extend google.protobuf.FieldOptions {
         |  optional Foo my_field_option = 60004;
         |}
         |message Foo {
         |  optional double a = 1 [(wire.my_field_option).baz.value = "b"];
         |  optional Nested baz = 2;
         |}
         |message Nested {
         |  optional string value = 1;
         |}
         |
        """.trimMargin(),
      )
    }
    assertThat(schema.getType("wire.Foo")).isNotNull()
  }

  @Test
  fun unresolvedFieldOption() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {
          |  optional string name = 1 [(unicorn) = true];
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |unable to resolve option unicorn
        |  for field name (/sourcePath/message.proto:2:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun unimportedOptionShouldBeUnresolved() {
    try {
      buildSchema {
        add(
          "cashapp/pii.proto".toPath(),
          """
         |package cashapp;
         |import 'google/protobuf/descriptor.proto';
         |extend google.protobuf.FieldOptions {
         |  optional bool friday = 60004;
         |}
         |
          """.trimMargin(),
        )
        add(
          "message.proto".toPath(),
          """
         |message Message {
         |  optional string name = 1 [(cashapp.friday) = true];
         |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |message.proto needs to import cashapp/pii.proto
        |  for field friday (/sourcePath/cashapp/pii.proto:4:3)
        |  in field name (/sourcePath/message.proto:2:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun unresolvedEnumValueOption() {
    try {
      buildSchema {
        add(
          "enum.proto".toPath(),
          """
          |enum Enum {
          |  A = 1 [(unicorn) = true];
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |unable to resolve option unicorn
        |  for constant A (/sourcePath/enum.proto:2:3)
        |  in enum Enum (/sourcePath/enum.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun unresolvedMessageOption() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {
          |  option (unicorn) = true;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |unable to resolve option unicorn
        |  for message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun unresolvedFileOption() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |
          |option (unicorn) = true;
          |message Message {}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |unable to resolve option unicorn
        |  for file /sourcePath/message.proto
        """.trimMargin(),
      )
    }
  }

  @Test
  fun resolveOptionsWithRelativePath() {
    val schema = buildSchema {
      add(
        "squareup/common/options.proto".toPath(),
        """
        |syntax = "proto2";
        |package squareup.common;
        |import "google/protobuf/descriptor.proto";
        |
        |extend google.protobuf.FileOptions {
        |  optional string file_status = 60000;
        |}
        """.trimMargin(),
      )
      add(
        "squareup/domain/message.proto".toPath(),
        """
        |syntax = "proto2";
        |package squareup.domain;
        |import "squareup/common/options.proto";
        |option (common.file_status) = "INTERNAL";
        |
        |message Message{}
        """.trimMargin(),
      )
    }
    assertThat(schema.protoFile("squareup/domain/message.proto")).isNotNull()
  }

  @Test
  fun extensionInProtoPathDontLoad() {
    val schema = buildSchema {
      add(
        "a/original.proto".toPath(),
        """
        |syntax = "proto2";
        |package a;
        |message A {
        |  optional string one = 1;
        |}
        """.trimMargin(),
      )
      addProtoPath(
        "b/extension.proto".toPath(),
        """
        |syntax = "proto2";
        |package b;
        |import "a/original.proto";
        |extend a.A {
        |  optional string two = 2;
        |}
        """.trimMargin(),
      )
    }
    val fields = (schema.getType("a.A") as MessageType).fields
    assertThat(fields).hasSize(1)
    with(fields[0]) {
      assertThat(namespaces).isEqualTo(listOf("a", "A"))
      assertThat(name).isEqualTo("one")
      assertThat(isExtension).isEqualTo(false)
    }
  }

  @Test
  fun extensionInSourceLoadsEvenIfParentMessageIsInProtoPath() {
    val schema = buildSchema {
      addProtoPath(
        "a/original.proto".toPath(),
        """
        |syntax = "proto2";
        |package a;
        |message A {
        |  optional string one = 1;
        |}
        """.trimMargin(),
      )
      add(
        "b/extension.proto".toPath(),
        """
        |syntax = "proto2";
        |package b;
        |import "a/original.proto";
        |extend a.A {
        |  optional string two = 2;
        |}
        """.trimMargin(),
      )
    }
    val fields = (schema.getType("a.A") as MessageType).fields
    assertThat(fields).hasSize(1)
    with(fields[0]) {
      assertThat(namespaces).isEqualTo(listOf("b"))
      assertThat(name).isEqualTo("two")
      assertThat(isExtension).isEqualTo(true)
    }
  }

  @Test
  fun optionsWithRelativePathUsedInExtensions() {
    val schema = buildSchema {
      add(
        "squareup/domain/message.proto".toPath(),
        """
        |syntax = "proto2";
        |package squareup.domain;
        |
        |message Message{}
        """.trimMargin(),
      )
      add(
        "squareup/common/options.proto".toPath(),
        """
        |syntax = "proto2";
        |package squareup.common;
        |
        |import "google/protobuf/descriptor.proto";
        |import "squareup/domain/message.proto";
        |
        |extend squareup.domain.Message {
        |  optional string type = 12000 [(maps_to) = "sup"];
        |}
        |
        |extend google.protobuf.FieldOptions {
        |  optional string maps_to = 123301;
        |}
        """.trimMargin(),
      )
    }
    assertThat(schema.protoFile("squareup/domain/message.proto")).isNotNull()
  }

  @Test
  fun optionsWithRelativePathUsedInExtensionsUnresolvable() {
    try {
      buildSchema {
        add(
          "squareup/domain/message.proto".toPath(),
          """
          |syntax = "proto2";
          |package squareup.domain;
          |
          |message Message{}
          """.trimMargin(),
        )
        add(
          "squareup/common/options.proto".toPath(),
          """
          |syntax = "proto2";
          |package squareup.common;
          |
          |import "squareup/domain/message.proto";
          |import "squareup/options1/special.proto";
          |import "squareup/options2/special.proto";
          |
          |extend squareup.domain.Message {
          |  optional string type = 12000 [(maps_to) = "sup"]; // missing package qualifier
          |}
          """.trimMargin(),
        )
        add(
          "squareup/options1/special.proto".toPath(),
          """
          |syntax = "proto2";
          |package squareup.options1;
          |
          |import "google/protobuf/descriptor.proto";
          |
          |extend google.protobuf.FieldOptions {
          |  optional string maps_to = 123301;
          |}
          """.trimMargin(),
        )
        add(
          "squareup/options2/special.proto".toPath(),
          """
          |syntax = "proto2";
          |package squareup.options2;
          |
          |import "google/protobuf/descriptor.proto";
          |
          |extend google.protobuf.FieldOptions {
          |  optional string maps_to = 123302;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |unable to resolve option maps_to
        |  for field type (/sourcePath/squareup/common/options.proto:9:3)
        |  in extend squareup.domain.Message (/sourcePath/squareup/common/options.proto:8:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun optionsWithRelativePathUsedInExtensionsShouldUseClosest() {
    buildSchema {
      add(
        "squareup/domain/message.proto".toPath(),
        """
        |syntax = "proto2";
        |package squareup.domain;
        |
        |message Message{}
        """.trimMargin(),
      )
      add(
        "squareup/common/options.proto".toPath(),
        """
        |syntax = "proto2";
        |package squareup.common;
        |
        |import "squareup/domain/message.proto";
        |import "squareup/options/special.proto";
        |import "google/protobuf/descriptor.proto";
        |
        |extend squareup.domain.Message {
        |  optional string type = 12000 [(maps_to) = "sup"];
        |}
        |
        |extend google.protobuf.FieldOptions {
        |  optional string maps_to = 123301;
        |}
        """.trimMargin(),
      )
      add(
        "squareup/options/special.proto".toPath(),
        """
        |syntax = "proto2";
        |package squareup.options;
        |
        |import "google/protobuf/descriptor.proto";
        |
        |extend google.protobuf.FieldOptions {
        |  optional string maps_to = 123302;
        |}
        """.trimMargin(),
      )
    }
  }

  @Test
  fun optionsWithRelativePathUsedInExtensionsResolvable() {
    val schema = buildSchema {
      add(
        "squareup/domain/message.proto".toPath(),
        """
        |syntax = "proto2";
        |package squareup.domain;
        |
        |message Message{}
        """.trimMargin(),
      )
      add(
        "squareup/common/options.proto".toPath(),
        """
        |syntax = "proto2";
        |package squareup.common;
        |
        |import "squareup/domain/message.proto";
        |import "squareup/options1/special.proto";
        |import "squareup/options2/special.proto";
        |
        |extend squareup.domain.Message {
        |  optional string type = 12000 [(options1.maps_to) = "sup"];
        |}
        """.trimMargin(),
      )
      add(
        "squareup/options1/special.proto".toPath(),
        """
        |syntax = "proto2";
        |package squareup.options1;
        |
        |import "google/protobuf/descriptor.proto";
        |
        |extend google.protobuf.FieldOptions {
        |  optional string maps_to = 123301;
        |}
        """.trimMargin(),
      )
      add(
        "squareup/options2/special.proto".toPath(),
        """
        |syntax = "proto2";
        |package squareup.options2;
        |
        |import "google/protobuf/descriptor.proto";
        |
        |extend google.protobuf.FieldOptions {
        |  optional string maps_to = 123302;
        |}
        """.trimMargin(),
      )
    }
    assertThat(schema.protoFile("squareup/domain/message.proto")).isNotNull()
  }

  @Test
  fun mapsCannotBeExtensions() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {}
          |extend Message {
          |  map<int32, int32> map_int_int = 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |extension fields cannot be a map
        |  for field map_int_int (/sourcePath/message.proto:3:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun missingZeroTagAtFirstPositionInMapValue() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {
          |  map<int32, Enum> map = 1;
          |}
          |enum Enum {
          |  ONE = 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |enum value in map must define 0 as the first value
        |  for field map (/sourcePath/message.proto:2:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun zeroNotFirstConstantInMapValue() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {
          |  map<int32, Enum> map = 1;
          |}
          |enum Enum {
          |  ONE = 1;
          |  ZERO = 0;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
        |enum value in map must define 0 as the first value
        |  for field map (/sourcePath/message.proto:2:3)
        |  in message Message (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun duplicateMessagesWithMembers() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |message Message {
          |  optional string name = 1;
          |}
          |message Message {
          |  optional string title = 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (exception: IllegalStateException) {
      assertThat(exception).hasMessage(
        "Message (/sourcePath/message.proto:4:1) is already defined at /sourcePath/message.proto:1:1",
      )
    }
  }

  @Test
  fun duplicateServicesWithRpcs() {
    try {
      buildSchema {
        add(
          "service.proto".toPath(),
          """
          |service Service {
          |  rpc Send (Data) returns (Data) {}
          |}
          |service Service {
          |  rpc Receive (Data) returns (Data) {}
          |}
          |message Data {}
          """.trimMargin(),
        )
      }
      fail()
    } catch (exception: IllegalStateException) {
      assertThat(exception).hasMessage(
        "Service (/sourcePath/service.proto:4:1) is already defined at /sourcePath/service.proto:1:1",
      )
    }
  }

  @Test
  fun duplicateRpcsInSameService() {
    try {
      buildSchema {
        add(
          "service.proto".toPath(),
          """
          |service Service {
          |  rpc Send (Data) returns (Data) {}
          |  rpc Send (Data) returns (Data) {}
          |}
          |message Data {}
          """.trimMargin(),
        )
      }
      fail()
    } catch (exception: SchemaException) {
      assertThat(exception).hasMessage(
        """
        |mutable rpcs share name Send:
        |  1. Send (/sourcePath/service.proto:2:3)
        |  2. Send (/sourcePath/service.proto:3:3)
        |  for service Service (/sourcePath/service.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test
  fun cannotUseProto2EnumsInProto3Message() {
    try {
      buildSchema {
        add(
          "proto2.proto".toPath(),
          """
          |syntax = "proto2";
          |enum Bit {
          |  ZERO = 0;
          |  ONE = 1;
          |}
          """.trimMargin(),
        )
        add(
          "proto3.proto".toPath(),
          """
          |syntax = "proto3";
          |import "proto2.proto";
          |message Joint {
          |  Bit bit = 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (exception: SchemaException) {
      assertThat(exception).hasMessage(
        """
        |Proto2 enums cannot be referenced in a proto3 message
        |  for field bit (/sourcePath/proto3.proto:4:3)
        |  in message Joint (/sourcePath/proto3.proto:3:1)
        """.trimMargin(),
      )
    }
  }

  @Test fun ambiguousEnumConstants() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |enum Foo {
          |  ZERO = 0;
          |  zero = 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (e: SchemaException) {
      assertThat(e).hasMessage(
        """|Ambiguous constant names (if you are using allow_alias, use the same value for these constants):
        |  ZERO:0 (/sourcePath/message.proto:2:3)
        |  zero:1 (/sourcePath/message.proto:3:3)
        |  for enum Foo (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }

  @Test fun typeAliasAllowsAmbiguousEnumConstantsIfSameTag() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |enum Foo {
        |  option allow_alias = true;
        |  ZERO = 0;
        |  zero = 0;
        |}
        """.trimMargin(),
      )
    }

    val enumType = schema.getType("Foo") as EnumType
    assertThat(enumType.constant("ZERO")!!.tag).isEqualTo(0)
    assertThat(enumType.constant("zero")!!.tag).isEqualTo(0)
  }

  @Test fun typeAliasDoesNotAllowAmbiguousEnumConstantsIfDifferentTag() {
    try {
      buildSchema {
        add(
          "message.proto".toPath(),
          """
          |enum Foo {
          |  option allow_alias = true;
          |  ZERO = 0;
          |  zero = 1;
          |}
          """.trimMargin(),
        )
      }
      fail()
    } catch (e: SchemaException) {
      assertThat(e).hasMessage(
        """|Ambiguous constant names (if you are using allow_alias, use the same value for these constants):
        |  ZERO:0 (/sourcePath/message.proto:3:3)
        |  zero:1 (/sourcePath/message.proto:4:3)
        |  for enum Foo (/sourcePath/message.proto:1:1)
        """.trimMargin(),
      )
    }
  }
}
