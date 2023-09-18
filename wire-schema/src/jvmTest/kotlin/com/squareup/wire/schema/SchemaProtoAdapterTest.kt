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
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import com.squareup.wire.buildSchema
import kotlin.test.Test
import kotlin.test.fail
import okio.Buffer
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import okio.EOFException
import okio.IOException
import okio.Path.Companion.toPath
import okio.ProtocolException

class SchemaProtoAdapterTest {
  private val coffeeSchema = buildSchema {
    add(
      "coffee.proto".toPath(),
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
      """.trimMargin(),
    )
  }

  // Golden data emitted by protoc using the schema above.
  private val dansCoffee = mapOf(
    "customer_name" to "Dan",
    "shots" to listOf(mapOf("caffeine_level" to 0.5)),
    "size_ounces" to 16,
    "dairy" to mapOf("count" to 1),
  )

  private val dansCoffeeEncoded = "0a0344616e120911000000000000e03f70107a021001".decodeHex()

  private val jessesCoffee = mapOf(
    "customer_name" to "Jesse",
    "shots" to
      listOf(
        mapOf("bean_type" to "colombian", "caffeine_level" to 1.0),
        mapOf("bean_type" to "colombian", "caffeine_level" to 1.0),
      ),
    "foam" to "ZOMG_SO_FOAMY",
    "size_ounces" to 24,
  )

  private val jessesCoffeeEncoded = (
    "0a054a6573736512140a09636f6c" +
      "6f6d6269616e11000000000000f03f12140a09636f6c6f6d6269616e11000000000000f03f18037018"
    )
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
    assertThat(adapter.encode(dansCoffee).toByteString()).isEqualTo(dansCoffeeEncoded)
    assertThat(adapter.encode(jessesCoffee).toByteString()).isEqualTo(jessesCoffeeEncoded)
    assertThat(adapter.encodedSize(dansCoffee)).isEqualTo(dansCoffeeEncoded.size)
    assertThat(adapter.encodedSize(jessesCoffee)).isEqualTo(jessesCoffeeEncoded.size)
  }

  @Test
  @Throws(IOException::class)
  fun groupsIgnored() {
    val adapter = buildSchema {
      add(
        "message.proto".toPath(),
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
        """.trimMargin(),
      )
    }.protoAdapter("Message")
    val encoded = (
      "0a0161135a02080114135a02100214135a090803720568656c6c" +
        "6f141baa010208011c1baa010210021c1baa01090803720568656c6c6f1c220162"
      )
      .decodeHex()
    val expected = mapOf("a" to "a", "b" to "b")
    assertThat(adapter.decode(Buffer().write(encoded))).isEqualTo(expected)
  }

  @Test
  @Throws(IOException::class)
  fun startGroupWithoutEndGroup() {
    val adapter = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |message Message {
            |  optional string a = 1;
            |}
        """.trimMargin(),
      )
    }.protoAdapter("Message")
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
    val adapter = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |message Message {
            |  optional string a = 1;
            |}
        """.trimMargin(),
      )
    }.protoAdapter("Message")
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
    val adapter = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |message Message {
            |  optional string a = 1;
            |}
        """.trimMargin(),
      )
    }.protoAdapter("Message")
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
    val adapter = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |message Message {
            |  repeated int32 a = 90 [packed = false];
            |}
        """.trimMargin(),
      )
    }.protoAdapter("Message")
    val expected = mapOf("a" to listOf(601, 701))

    val packedEncoded = "d20504d904bd05".decodeHex()
    assertThat(adapter.decode(Buffer().write(packedEncoded))).isEqualTo(expected)

    val unpackedEncoded = "d005d904d005bd05".decodeHex()
    assertThat(adapter.decode(Buffer().write(unpackedEncoded))).isEqualTo(expected)
  }

  @Test
  @Throws(IOException::class)
  fun decodeToPacked() {
    val adapter = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |message Message {
            |  repeated int32 a = 90 [packed = true];
            |}
        """.trimMargin(),
      )
    }.protoAdapter("Message")
    val expected = mapOf("a" to listOf(601, 701))

    val unpackedEncoded = "d005d904d005bd05".decodeHex()
    assertThat(adapter.decode(Buffer().write(unpackedEncoded))).isEqualTo(expected)

    val packedEncoded = "d20504d904bd05".decodeHex()
    assertThat(adapter.decode(Buffer().write(packedEncoded))).isEqualTo(expected)
  }

  @Test
  @Throws(IOException::class)
  fun recursiveMessage() {
    val adapter = buildSchema {
      add(
        "tree.proto".toPath(),
        """
            |message BinaryTreeNode {
            |  optional BinaryTreeNode left = 1;
            |  optional BinaryTreeNode right = 2;
            |  optional string value = 3;
            |}
        """.trimMargin(),
      )
    }.protoAdapter("BinaryTreeNode")
    val value = mapOf(
      "value" to "D",
      "left" to mapOf(
        "value" to "B",
        "left" to mapOf("value" to "A"),
        "right" to mapOf("value" to "C"),
      ),
      "right" to mapOf(
        "value" to "F",
        "left" to mapOf("value" to "E"),
        "right" to mapOf("value" to "G"),
      ),
    )
    val encoded = "0a0d0a031a014112031a01431a0142120d0a031a014512031a01471a01461a0144".decodeHex()
    assertThat(adapter.encode(value).toByteString()).isEqualTo(encoded)
    assertThat(adapter.decode(Buffer().write(encoded))).isEqualTo(value)
  }

  @Test
  fun includeUnknowns() {
    val schema = buildSchema {
      add(
        "coffee.proto".toPath(),
        """
             |message CafeDrink {
             |  optional string customer_name = 1;
             |  optional int32 size_ounces = 14;
             |}
        """.trimMargin(),
      )
    }

    val dansCoffeeWithUnknowns = mapOf(
      "customer_name" to "Dan",
      "2" to listOf("11000000000000e03f".decodeHex()),
      "size_ounces" to 16,
      "15" to listOf("1001".decodeHex()),
    )

    val adapter = schema.protoAdapter("CafeDrink", true)
    assertThat(adapter.decode(Buffer().write(dansCoffeeEncoded)))
      .isEqualTo(dansCoffeeWithUnknowns)
  }

  @Test
  fun omitUnknowns() {
    val schema = buildSchema {
      add(
        "coffee.proto".toPath(),
        """
            |message CafeDrink {
            |  optional string customer_name = 1;
            |  optional int32 size_ounces = 14;
            |}
        """.trimMargin(),
      )
    }

    val dansCoffeeWithoutUnknowns = mapOf("customer_name" to "Dan", "size_ounces" to 16)

    val adapter = schema.protoAdapter("CafeDrink", false)
    assertThat(adapter.decode(Buffer().write(dansCoffeeEncoded)))
      .isEqualTo(dansCoffeeWithoutUnknowns)
  }
}
