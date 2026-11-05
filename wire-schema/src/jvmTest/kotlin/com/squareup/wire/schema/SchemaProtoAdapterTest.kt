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
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.squareup.wire.ProtoWriter
import com.squareup.wire.buildSchema
import com.squareup.wire.schema.internal.SchemaEncoder
import kotlin.test.Test
import kotlin.test.fail
import okio.Buffer
import okio.ByteString
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
  fun incompatibleMessageFieldEncodingsAreUnknown() {
    for (label in listOf("optional", "repeated")) {
      val schema = buildSchema {
        add(
          "message.proto".toPath(),
          """
              |syntax = "proto2";
              |message Child {}
              |message Message {
              |  $label Child child = 17;
              |}
          """.trimMargin(),
        )
      }
      val child = emptyMap<String, Any>()
      val valid = mapOf("child" to if (label == "repeated") listOf(child) else child)
      for ((hex, value) in listOf(
        "8d0107000000" to 7,
        "89010800000000000000" to 8L,
        "880109" to 9L,
      )) {
        for (includeUnknown in listOf(false, true)) {
          val adapter = schema.protoAdapter("Message", includeUnknown)
          val unknown = if (includeUnknown) mapOf("17" to listOf(value)) else emptyMap()
          for ((encodedHex, expected) in listOf(
            hex to unknown,
            "8a0100" to valid,
            hex + "8a0100" to unknown + valid,
            "8a0100" + hex to valid + unknown,
          )) {
            val encoded = encodedHex.decodeHex()
            assertThat(adapter.decode(encoded)).isEqualTo(expected)
            assertThat(adapter.decode(Buffer().write(encoded))).isEqualTo(expected)
          }
        }
      }
    }
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
      assertThat(expected).hasMessage("Unexpected end group. Reader position: 4. Last read tag: 3.")
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
      assertThat(expected).hasMessage("Unexpected end group. Reader position: 5. Last read tag: 3.")
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
  fun incompatibleScalarFieldEncodingsAreUnknown() {
    val adapter = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |syntax = "proto2";
            |message Message {
            |  optional int32 i32 = 1;
            |  optional int64 i64 = 2;
            |  optional fixed32 f32 = 7;
            |  optional bool flag = 13;
            |}
        """.trimMargin(),
      )
    }.protoAdapter("Message", true)

    for ((hex, tag, payload) in listOf(
      Triple("0a0107", "1", "07"),
      Triple("0a00", "1", ""),
      Triple("0a020102", "1", "0102"),
      Triple("120161", "2", "61"),
      Triple("3a0401020304", "7", "01020304"),
      Triple("6a0101", "13", "01"),
    )) {
      val encoded = hex.decodeHex()
      val expected = mapOf(tag to listOf(payload.decodeHex()))
      assertThat(adapter.decode(encoded)).isEqualTo(expected)
      assertThat(adapter.decode(Buffer().write(encoded))).isEqualTo(expected)
    }
  }

  @Test
  fun packedScalarEncodingsAreOnlyAcceptedForRepeatedFields() {
    val cases = listOf(
      Triple("int32", listOf("07", "08"), listOf(7, 8)),
      Triple("uint32", listOf("07", "08"), listOf(7, 8)),
      Triple("sint32", listOf("0e", "10"), listOf(7, 8)),
      Triple("fixed32", listOf("07000000", "08000000"), listOf(7, 8)),
      Triple("sfixed32", listOf("07000000", "08000000"), listOf(7, 8)),
      Triple("int64", listOf("07", "08"), listOf(7L, 8L)),
      Triple("uint64", listOf("07", "08"), listOf(7L, 8L)),
      Triple("sint64", listOf("0e", "10"), listOf(7L, 8L)),
      Triple("fixed64", listOf("0700000000000000", "0800000000000000"), listOf(7L, 8L)),
      Triple("sfixed64", listOf("0700000000000000", "0800000000000000"), listOf(7L, 8L)),
      Triple("bool", listOf("01", "00"), listOf(true, false)),
      Triple("float", listOf("0000803f", "00000040"), listOf(1.0f, 2.0f)),
      Triple("double", listOf("000000000000f03f", "0000000000000040"), listOf(1.0, 2.0)),
      Triple("Choice", listOf("01", "02"), listOf("A", "B")),
    )
    for ((type, values, expectedValues) in cases) {
      val schema = buildSchema {
        add(
          "message.proto".toPath(),
          """
              |syntax = "proto2";
              |message Message {
              |  enum Choice { A = 1; B = 2; }
              |  optional $type singular = 1;
              |  repeated $type unpacked = 2 [packed = false];
              |  repeated $type packed = 3 [packed = true];
              |}
          """.trimMargin(),
        )
      }
      val payload = values.joinToString("").decodeHex()
      val singular = Buffer().writeByte(0x0a).writeByte(payload.size).write(payload).readByteString()
      val adapter = schema.protoAdapter("Message", true)
      val adapterWithoutUnknown = schema.protoAdapter("Message", false)
      assertThat(adapter.decode(singular)).isEqualTo(mapOf("1" to listOf(payload)))
      assertThat(adapter.decode(Buffer().write(singular))).isEqualTo(mapOf("1" to listOf(payload)))
      assertThat(adapterWithoutUnknown.decode(singular)).isEqualTo(emptyMap<String, Any>())
      assertThat(adapterWithoutUnknown.decode(Buffer().write(singular))).isEqualTo(emptyMap<String, Any>())

      val fieldEncoding = when (type) {
        "fixed32", "sfixed32", "float" -> 5
        "fixed64", "sfixed64", "double" -> 1
        else -> 0
      }
      for ((tag, name) in listOf(2 to "unpacked", 3 to "packed")) {
        val packed = Buffer().writeByte((tag shl 3) or 2).writeByte(payload.size).write(payload).readByteString()
        val unpacked = Buffer().apply {
          for (value in values) {
            writeByte((tag shl 3) or fieldEncoding)
            write(value.decodeHex())
          }
        }.readByteString()
        val expected = mapOf(name to expectedValues)
        for (encoded in listOf(packed, unpacked)) {
          assertThat(adapter.decode(encoded)).isEqualTo(expected)
          assertThat(adapter.decode(Buffer().write(encoded))).isEqualTo(expected)
        }
      }
    }
  }

  @Test
  fun incompatibleNonScalarFieldEncodingsAreUnknown() {
    val adapter = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |syntax = "proto3";
            |message Message {
            |  message Child {}
            |  string text = 1;
            |  Child child = 2;
            |  map<int32, int32> entries = 3;
            |}
        """.trimMargin(),
      )
    }.protoAdapter("Message", true)
    for ((hex, tag) in listOf("0807" to "1", "1007" to "2", "1807" to "3")) {
      val expected = mapOf(tag to listOf(7L))
      assertThat(adapter.decode(hex.decodeHex())).isEqualTo(expected)
      assertThat(adapter.decode(Buffer().write(hex.decodeHex()))).isEqualTo(expected)
    }
  }

  @Test
  fun incompatibleScalarEncodingDoesNotOverwriteValidValue() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |message Message {
            |  optional int32 value = 1;
            |}
        """.trimMargin(),
      )
    }
    val adapter = schema.protoAdapter("Message", true)
    val adapterWithoutUnknown = schema.protoAdapter("Message", false)
    val unknown = "0102".decodeHex()
    for (hex in listOf("08070a020102", "0a0201020807")) {
      val expected = mapOf("value" to 7, "1" to listOf(unknown))
      assertThat(adapter.decode(hex.decodeHex())).isEqualTo(expected)
      assertThat(adapter.decode(Buffer().write(hex.decodeHex()))).isEqualTo(expected)
      assertThat(adapterWithoutUnknown.decode(hex.decodeHex())).isEqualTo(mapOf("value" to 7))
      assertThat(adapterWithoutUnknown.decode(Buffer().write(hex.decodeHex()))).isEqualTo(mapOf("value" to 7))
    }
    val emptyPackedThenValid = "0a000807".decodeHex()
    val expected = mapOf("value" to 7, "1" to listOf("".decodeHex()))
    assertThat(adapter.decode(emptyPackedThenValid)).isEqualTo(expected)
    assertThat(adapter.decode(Buffer().write(emptyPackedThenValid))).isEqualTo(expected)
    assertThat(adapterWithoutUnknown.decode(emptyPackedThenValid)).isEqualTo(mapOf("value" to 7))
    assertThat(adapterWithoutUnknown.decode(Buffer().write(emptyPackedThenValid))).isEqualTo(mapOf("value" to 7))
  }

  @Test
  fun emptyPackedRepeatedFields() {
    for (packed in listOf(false, true)) {
      val adapter = buildSchema {
        add(
          "message.proto".toPath(),
          """
              |message Message {
              |  repeated int32 values = 1 [packed = $packed];
              |  optional int32 after = 2;
              |}
          """.trimMargin(),
        )
      }.protoAdapter("Message")
      for ((hex, expected) in listOf(
        "0a00" to emptyMap<String, Any>(),
        "0a001007" to mapOf("after" to 7),
        "0a000a0201020a0008030a001007" to mapOf("values" to listOf(1, 2, 3), "after" to 7),
      )) {
        val bytes = hex.decodeHex()
        assertThat(adapter.decode(bytes)).isEqualTo(expected)
        assertThat(adapter.decode(Buffer().write(bytes))).isEqualTo(expected)
      }
    }
  }

  @Test
  fun emptyPackedRecordsForEveryScalarType() {
    for (type in listOf(
      "int32", "uint32", "sint32", "fixed32", "sfixed32",
      "int64", "uint64", "sint64", "fixed64", "sfixed64",
      "float", "double", "bool", "Choice",
    )) {
      for (packed in listOf(false, true)) {
        val adapter = buildSchema {
          add(
            "message.proto".toPath(),
            """
                |message Message {
                |  enum Choice { A = 1; B = 2; }
                |  repeated $type values = 1 [packed = $packed];
                |  optional int32 after = 2;
                |}
            """.trimMargin(),
          )
        }.protoAdapter("Message")
        for ((hex, expected) in listOf(
          "0a00" to emptyMap<String, Any>(),
          "0a001007" to mapOf("after" to 7),
          "10070a00" to mapOf("after" to 7),
        )) {
          val bytes = hex.decodeHex()
          assertThat(adapter.decode(bytes)).isEqualTo(expected)
          assertThat(adapter.decode(Buffer().write(bytes))).isEqualTo(expected)
        }
      }
    }
  }

  @Test
  fun emptyRepeatedLengthDelimitedValuesAreRetained() {
    val adapter = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |message Message {
            |  message Child {}
            |  repeated string strings = 1;
            |  repeated bytes blobs = 2;
            |  repeated Child children = 3;
            |}
        """.trimMargin(),
      )
    }.protoAdapter("Message")
    val expected = mapOf(
      "strings" to listOf(""),
      "blobs" to listOf("".decodeHex()),
      "children" to listOf(emptyMap<String, Any>()),
    )
    val bytes = "0a0012001a00".decodeHex()
    assertThat(adapter.decode(bytes)).isEqualTo(expected)
    assertThat(adapter.decode(Buffer().write(bytes))).isEqualTo(expected)
  }

  @Test
  fun encodeProto3RepeatedScalarPacked() {
    val adapter = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |syntax = "proto3";
            |message Message {
            |  repeated int32 values = 1;
            |}
        """.trimMargin(),
      )
    }.protoAdapter("Message", false)
    val value = mapOf("values" to listOf(1, 2, 3))
    // Golden data emitted by protoc.
    val encoded = "0a03010203".decodeHex()
    assertThat(adapter.encode(value).toByteString()).isEqualTo(encoded)
    assertThat(adapter.encodedSize(value)).isEqualTo(encoded.size)
    assertThat(adapter.decode(encoded)).isEqualTo(value)
  }

  @Test
  fun encodePackedAndUnpackedLikeProtobufJava() {
    val types = mapOf(
      "int32" to listOf(0, 1, -2),
      "uint32" to listOf(0, 1, -2),
      "sint32" to listOf(0, 1, -2),
      "fixed32" to listOf(0, 1, -2),
      "sfixed32" to listOf(0, 1, -2),
      "int64" to listOf(0L, 1L, -2L),
      "uint64" to listOf(0L, 1L, -2L),
      "sint64" to listOf(0L, 1L, -2L),
      "fixed64" to listOf(0L, 1L, -2L),
      "sfixed64" to listOf(0L, 1L, -2L),
      "float" to listOf(0f, 1.5f, -2f),
      "double" to listOf(0.0, 1.5, -2.0),
      "bool" to listOf(true, false, true),
      "Choice" to listOf("A", "B", "A"),
    )
    for (syntax in listOf("proto2", "proto3")) {
      for (packedOption in listOf("", " [packed = false]", " [packed = true]")) {
        for ((type, values) in types) {
          // Protobuf packs these by default in proto3. Wire does not, in generated code either.
          if (syntax == "proto3" && packedOption == "" && type in listOf("bool", "Choice")) continue
          val schema = buildSchema {
            add(
              "message.proto".toPath(),
              """
                  |syntax = "$syntax";
                  |message Message {
                  |  enum Choice { A = 0; B = 1; }
                  |  repeated $type values = 1$packedOption;
                  |  repeated int32 after = 2;
                  |}
              """.trimMargin(),
            )
          }
          val adapter = schema.protoAdapter("Message", false)
          val descriptor = protobufJavaDescriptor(schema, "message.proto", "Message")
          for (value in listOf(
            mapOf("values" to values, "after" to listOf(7)),
            mapOf("values" to values.take(1)),
            mapOf("values" to emptyList<Any>(), "after" to listOf(7)),
          )) {
            val expected = protobufJavaEncode(descriptor, value)
            val context = "$syntax repeated $type$packedOption $value"
            assertThat(adapter.encode(value).toByteString(), context).isEqualTo(expected)
            val buffer = Buffer()
            adapter.encode(ProtoWriter(buffer), value)
            assertThat(buffer.readByteString(), context).isEqualTo(expected)
            assertThat(adapter.encodedSize(value), context).isEqualTo(expected.size)
            val decodedValue = value.filterValues { it != emptyList<Any>() }
            assertThat(adapter.decode(expected), context).isEqualTo(decodedValue)
          }
        }
      }
    }
  }

  @Test
  fun encodePackedNegativeEnums() {
    val adapter = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |message Message {
            |  enum Choice { NEGATIVE = -1; ZERO = 0; }
            |  repeated Choice values = 1 [packed = true];
            |}
        """.trimMargin(),
      )
    }.protoAdapter("Message", false)
    val value = mapOf("values" to listOf("NEGATIVE", 1))
    // Generated Wire adapters write enum values as unsigned 32-bit varints.
    val encoded = "0a06ffffffff0f01".decodeHex()
    assertThat(adapter.encode(value).toByteString()).isEqualTo(encoded)
    val buffer = Buffer()
    adapter.encode(ProtoWriter(buffer), value)
    assertThat(buffer.readByteString()).isEqualTo(encoded)
    assertThat(adapter.encodedSize(value)).isEqualTo(encoded.size)
    assertThat(adapter.decode(encoded)).isEqualTo(value)
  }

  private fun protobufJavaDescriptor(
    schema: Schema,
    path: String,
    messageName: String,
  ): Descriptors.Descriptor {
    val encoded = SchemaEncoder(schema).encode(schema.protoFile(path)!!)
    val fileDescriptorProto = FileDescriptorProto.parseFrom(encoded.toByteArray())
    val fileDescriptor = Descriptors.FileDescriptor.buildFrom(fileDescriptorProto, arrayOf())
    return fileDescriptor.findMessageTypeByName(messageName)
  }

  private fun protobufJavaEncode(
    descriptor: Descriptors.Descriptor,
    value: Map<String, Any>,
  ): ByteString = protobufJavaMessage(descriptor, value).toByteArray().toByteString()

  private fun protobufJavaMessage(
    descriptor: Descriptors.Descriptor,
    value: Map<String, Any>,
  ): DynamicMessage {
    val builder = DynamicMessage.newBuilder(descriptor)
    for ((name, fieldValue) in value) {
      val field = descriptor.findFieldByName(name)
      when {
        field.isMapField -> {
          val keyField = field.messageType.findFieldByName("key")
          val valueField = field.messageType.findFieldByName("value")
          for ((entryKey, entryValue) in fieldValue as Map<*, *>) {
            val entry = DynamicMessage.newBuilder(field.messageType)
              .setField(keyField, protobufJavaValue(keyField, entryKey!!))
              .setField(valueField, protobufJavaValue(valueField, entryValue!!))
              .build()
            builder.addRepeatedField(field, entry)
          }
        }
        field.isRepeated -> {
          for (element in fieldValue as List<*>) {
            builder.addRepeatedField(field, protobufJavaValue(field, element!!))
          }
        }
        else -> builder.setField(field, protobufJavaValue(field, fieldValue))
      }
    }
    return builder.build()
  }

  @Suppress("UNCHECKED_CAST")
  private fun protobufJavaValue(field: Descriptors.FieldDescriptor, value: Any): Any = when (field.javaType) {
    Descriptors.FieldDescriptor.JavaType.ENUM -> field.enumType.findValueByName(value as String)
    Descriptors.FieldDescriptor.JavaType.MESSAGE -> protobufJavaMessage(field.messageType, value as Map<String, Any>)
    Descriptors.FieldDescriptor.JavaType.BYTE_STRING -> com.google.protobuf.ByteString.copyFrom((value as ByteString).toByteArray())
    else -> value
  }

  @Test
  fun encodeMapsLikeProtobufJava() {
    for (syntax in listOf("proto2", "proto3")) {
      val label = if (syntax == "proto2") "optional " else ""
      val schema = buildSchema {
        add(
          "message.proto".toPath(),
          """
              |syntax = "$syntax";
              |message Message {
              |  enum Choice { A = 0; B = 1; }
              |  message Child { ${label}int32 value = 1; }
              |  map<string, int64> strings = 1;
              |  map<int32, string> ints = 2;
              |  map<bool, Choice> bools = 3;
              |  map<sint64, Child> children = 4;
              |  map<fixed32, bytes> blobs = 5;
              |  map<string, double> doubles = 6;
              |}
          """.trimMargin(),
        )
      }
      val adapter = schema.protoAdapter("Message", false)
      val descriptor = protobufJavaDescriptor(schema, "message.proto", "Message")
      val value = mapOf(
        "strings" to mapOf("USD" to 44L, "EUR" to 33L),
        "ints" to mapOf(1 to "one", -2 to "minus two"),
        "bools" to mapOf(true to "B"),
        "children" to mapOf(-3L to mapOf("value" to 5), 4L to mapOf("value" to 6)),
        "blobs" to mapOf(7 to "cafe".decodeHex()),
        "doubles" to mapOf("pi" to 3.14),
      )
      val expected = protobufJavaEncode(descriptor, value)
      assertThat(adapter.encode(value).toByteString(), syntax).isEqualTo(expected)
      val buffer = Buffer()
      adapter.encode(ProtoWriter(buffer), value)
      assertThat(buffer.readByteString(), syntax).isEqualTo(expected)
      assertThat(adapter.encodedSize(value), syntax).isEqualTo(expected.size)
      assertThat(adapter.decode(expected), syntax).isEqualTo(value)
    }
  }

  @Test
  fun mapEntriesWithIdentityValues() {
    val adapter = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |syntax = "proto3";
            |message Message {
            |  map<string, int64> values = 1;
            |}
        """.trimMargin(),
      )
    }.protoAdapter("Message", false)
    val value = mapOf("values" to mapOf("" to 0L))
    // Golden data emitted by protoc. Map entries always write the key and the value.
    val encoded = "0a040a001000".decodeHex()
    assertThat(adapter.encode(value).toByteString()).isEqualTo(encoded)
    assertThat(adapter.encodedSize(value)).isEqualTo(encoded.size)
    assertThat(adapter.decode(encoded)).isEqualTo(value)
    // A missing key or value decodes to its identity value.
    assertThat(adapter.decode("0a00".decodeHex())).isEqualTo(value)
  }

  @Test
  fun mapEntriesWithoutEnumValue() {
    val adapter = buildSchema {
      add(
        "message.proto".toPath(),
        """
            |syntax = "proto3";
            |enum Choice { ZERO = 0; ONE = 1; }
            |message Message {
            |  map<bool, Choice> choices = 1;
            |}
        """.trimMargin(),
      )
    }.protoAdapter("Message", false)
    // A missing enum value decodes to the zero constant.
    assertThat(adapter.decode("0a020801".decodeHex())).isEqualTo(mapOf("choices" to mapOf(true to "ZERO")))
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
