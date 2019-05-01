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
package com.squareup.wire.schema

import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Test
import java.io.EOFException
import java.io.IOException
import java.net.ProtocolException

class SchemaProtoAdapterTest {
  private val coffeeSchema = RepoBuilder()
      .add(
          "coffee.proto",
          """
          |message CafeDrink {
          |  optional string customer_name = 1;
          |  repeated EspressoShot shots = 2;
          |  optional Foam foam = 3;
          |  optional int32 size_ounces = 14;
          |  optional Dairy dairy = 15;
          |
          |  enum Foam {
          |    NOT_FOAMY_AND_QUITE_BORING = 1;
          |    ZOMG_SO_FOAMY = 3;
          |  }
          |}
          |
          |message Dairy {
          |  optional int32 count = 2;
          |  optional string type = 1;
          |}
          |
          |message EspressoShot {
          |  optional string bean_type = 1;
          |  optional double caffeine_level = 2;
          |}
          """.trimMargin()
      )
      .schema()

  // Golden data emitted by protoc using the schema above.
  private val dansCoffee = mapOf(
      "customer_name" to "Dan",
      "shots" to listOf(mapOf("caffeine_level" to 0.5)),
      "size_ounces" to 16,
      "dairy" to mapOf("count" to 1)
  )

  private val dansCoffeeEncoded = "0a0344616e120911000000000000e03f70107a021001".decodeHex()

  private val jessesCoffee = mapOf(
      "customer_name" to "Jesse",
      "shots" to
          listOf(
              mapOf("bean_type" to "colombian", "caffeine_level" to 1.0),
              mapOf("bean_type" to "colombian", "caffeine_level" to 1.0)
          ),
      "foam" to "ZOMG_SO_FOAMY",
      "size_ounces" to 24
  )

  private val jessesCoffeeEncoded = ("0a054a6573736512140a09636f6c" +
      "6f6d6269616e11000000000000f03f12140a09636f6c6f6d6269616e11000000000000f03f18037018")
      .decodeHex()

  @Test
  fun decode() {
    val adapter = coffeeSchema.protoAdapter("CafeDrink", true)
    assertThat(adapter.decode(Buffer().write(dansCoffeeEncoded))).isEqualTo(dansCoffee)
    assertThat(adapter.decode(Buffer().write(jessesCoffeeEncoded))).isEqualTo(jessesCoffee)
  }

  @Test
  @Throws(IOException::class)
  fun encode() {
    val adapter = coffeeSchema.protoAdapter("CafeDrink", true)
    assertThat(ByteString.of(*adapter.encode(dansCoffee))).isEqualTo(dansCoffeeEncoded)
    assertThat(ByteString.of(*adapter.encode(jessesCoffee))).isEqualTo(jessesCoffeeEncoded)
  }

  @Test
  @Throws(IOException::class)
  fun groupsIgnored() {
    val adapter = RepoBuilder()
        .add(
            "message.proto",
            """
            |message Message {
            |  optional string a = 1;
            |  // repeated group Group1 = 2 {
            |  //   optional SomeMessage a = 11;
            |  // }
            |  // repeated group Group2 = 3 {
            |  //   optional SomeMessage b = 21;
            |  // }
            |  optional string b = 4;
            |}
            """.trimMargin()
        )
        .protoAdapter("Message")
    val encoded = ("0a0161135a02080114135a02100214135a090803720568656c6c" +
        "6f141baa010208011c1baa010210021c1baa01090803720568656c6c6f1c220162")
        .decodeHex()
    val expected = mapOf("a" to "a", "b" to "b")
    assertThat(adapter.decode(Buffer().write(encoded))).isEqualTo(expected)
  }

  @Test
  @Throws(IOException::class)
  fun startGroupWithoutEndGroup() {
    val adapter = RepoBuilder()
        .add(
            "message.proto",
            """
            |message Message {
            |  optional string a = 1;
            |}
            """.trimMargin()
        )
        .protoAdapter("Message")
    val encoded = "130a0161".decodeHex()
    try {
      adapter.decode(Buffer().write(encoded))
      fail()
    } catch (expected: EOFException) {
    }
  }

  @Test
  @Throws(IOException::class)
  fun unexpectedEndGroup() {
    val adapter = RepoBuilder()
        .add(
            "message.proto",
            """
            |message Message {
            |  optional string a = 1;
            |}
            """.trimMargin()
        )
        .protoAdapter("Message")
    val encoded = "0a01611c".decodeHex()
    try {
      adapter.decode(Buffer().write(encoded))
      fail()
    } catch (expected: ProtocolException) {
      assertThat(expected).hasMessage("Unexpected end group")
    }
  }

  @Test
  @Throws(IOException::class)
  fun endGroupDoesntMatchStartGroup() {
    val adapter = RepoBuilder()
        .add(
            "message.proto",
            """
            |message Message {
            |  optional string a = 1;
            |}
            """.trimMargin()
        )
        .protoAdapter("Message")
    val encoded = "130a01611c".decodeHex()
    try {
      adapter.decode(Buffer().write(encoded))
      fail()
    } catch (expected: ProtocolException) {
      assertThat(expected).hasMessage("Unexpected end group")
    }
  }

  @Test
  @Throws(IOException::class)
  fun decodeToUnpacked() {
    val adapter = RepoBuilder()
        .add(
            "message.proto",
            """
            |message Message {
            |  repeated int32 a = 90 [packed = false];
            |}
            """.trimMargin()
        )
        .protoAdapter("Message")
    val expected = mapOf("a" to listOf(601, 701))

    val packedEncoded = "d20504d904bd05".decodeHex()
    assertThat(adapter.decode(Buffer().write(packedEncoded))).isEqualTo(expected)

    val unpackedEncoded = "d005d904d005bd05".decodeHex()
    assertThat(adapter.decode(Buffer().write(unpackedEncoded))).isEqualTo(expected)
  }

  @Test
  @Throws(IOException::class)
  fun decodeToPacked() {
    val adapter = RepoBuilder()
        .add(
            "message.proto",
            """
            |message Message {
            |  repeated int32 a = 90 [packed = true];
            |}
            """.trimMargin()
        )
        .protoAdapter("Message")
    val expected = mapOf("a" to listOf(601, 701))

    val unpackedEncoded = "d005d904d005bd05".decodeHex()
    assertThat(adapter.decode(Buffer().write(unpackedEncoded))).isEqualTo(expected)

    val packedEncoded = "d20504d904bd05".decodeHex()
    assertThat(adapter.decode(Buffer().write(packedEncoded))).isEqualTo(expected)
  }

  @Test
  @Throws(IOException::class)
  fun recursiveMessage() {
    val adapter = RepoBuilder()
        .add(
            "tree.proto",
            """
            |message BinaryTreeNode {
            |  optional BinaryTreeNode left = 1;
            |  optional BinaryTreeNode right = 2;
            |  optional string value = 3;
            |}
            """.trimMargin()
        )
        .protoAdapter("BinaryTreeNode")
    val value = mapOf(
        "value" to "D",
        "left" to mapOf(
            "value" to "B",
            "left" to mapOf("value" to "A"),
            "right" to mapOf("value" to "C")
        ),
        "right" to mapOf(
            "value" to "F",
            "left" to mapOf("value" to "E"),
            "right" to mapOf("value" to "G")
        )
    )
    val encoded = "1a01440a0d1a01420a031a014112031a0143120d1a01460a031a014512031a0147".decodeHex()
    assertThat(ByteString.of(*adapter.encode(value))).isEqualTo(encoded)
    assertThat(adapter.decode(Buffer().write(encoded))).isEqualTo(value)
  }

  @Test
  fun includeUnknowns() {
    val schema = RepoBuilder()
        .add(
            "coffee.proto", """
             |message CafeDrink {
             |  optional string customer_name = 1;
             |  optional int32 size_ounces = 14;
             |}
             """.trimMargin()
        )
        .schema()

    val dansCoffeeWithUnknowns = mapOf(
        "customer_name" to "Dan",
        "2" to listOf("11000000000000e03f".decodeHex()),
        "size_ounces" to 16,
        "15" to listOf("1001".decodeHex())
    )

    val adapter = schema.protoAdapter("CafeDrink", true)
    assertThat(adapter.decode(Buffer().write(dansCoffeeEncoded)))
        .isEqualTo(dansCoffeeWithUnknowns)
  }

  @Test
  fun omitUnknowns() {
    val schema = RepoBuilder()
        .add(
            "coffee.proto",
            """
            |message CafeDrink {
            |  optional string customer_name = 1;
            |  optional int32 size_ounces = 14;
            |}
            """.trimMargin()
        )
        .schema()

    val dansCoffeeWithoutUnknowns = mapOf("customer_name" to "Dan", "size_ounces" to 16)

    val adapter = schema.protoAdapter("CafeDrink", false)
    assertThat(adapter.decode(Buffer().write(dansCoffeeEncoded)))
        .isEqualTo(dansCoffeeWithoutUnknowns)
  }
}
