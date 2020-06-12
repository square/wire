/*
 * Copyright 2020 Square Inc.
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
package com.squareup.wire

import com.google.protobuf.ListValue
import com.google.protobuf.NullValue.NULL_VALUE
import com.google.protobuf.Struct
import com.google.protobuf.Value
import com.google.protobuf.util.JsonFormat
import com.squareup.moshi.Moshi
import com.squareup.wire.json.assertJsonEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Test
import squareup.proto3.alltypes.AllStructs
import squareup.proto3.alltypes.AllStructsOuterClass

class StructTest {
  @Test fun nullValue() {
    val googleMessage = Value.newBuilder()
        .setNullValue(NULL_VALUE)
        .build()

    val wireMessage = null

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_VALUE.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_VALUE.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun doubleValue() {
    val googleMessage = Value.newBuilder()
        .setNumberValue(0.25)
        .build()

    val wireMessage = 0.25

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_VALUE.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_VALUE.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun specialDoubleValues() {
    val googleMessage = ListValue.newBuilder()
        .addValues(Value.newBuilder().setNumberValue(Double.NEGATIVE_INFINITY).build())
        .addValues(Value.newBuilder().setNumberValue(-0.0).build())
        .addValues(Value.newBuilder().setNumberValue(0.0).build())
        .addValues(Value.newBuilder().setNumberValue(Double.POSITIVE_INFINITY).build())
        .addValues(Value.newBuilder().setNumberValue(Double.NaN).build())
        .build()

    val wireMessage = listOf(
        Double.NEGATIVE_INFINITY,
        -0.0,
        0.0,
        Double.POSITIVE_INFINITY,
        Double.NaN
    )

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_LIST.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_LIST.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun booleanTrue() {
    val googleMessage = Value.newBuilder()
        .setBoolValue(true)
        .build()

    val wireMessage = true

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_VALUE.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_VALUE.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun booleanFalse() {
    val googleMessage = Value.newBuilder()
        .setBoolValue(false)
        .build()

    val wireMessage = false

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_VALUE.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_VALUE.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun stringValue() {
    val googleMessage = Value.newBuilder()
        .setStringValue("Cash App!")
        .build()

    val wireMessage = "Cash App!"

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_VALUE.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_VALUE.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun emptyStringValue() {
    val googleMessage = Value.newBuilder()
        .setStringValue("")
        .build()

    val wireMessage = ""

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_VALUE.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_VALUE.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun utf8StringValue() {
    val googleMessage = Value.newBuilder()
        .setStringValue("На берегу пустынных волн")
        .build()

    val wireMessage = "На берегу пустынных волн"

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_VALUE.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_VALUE.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun emptyMap() {
    val googleMessage = Struct.newBuilder().build()

    val wireMessage = mapOf<String, Any?>()

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_MAP.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_MAP.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun map() {
    val googleMessage = Struct.newBuilder()
        .putFields("a", Value.newBuilder().setStringValue("android").build())
        .putFields("c", Value.newBuilder().setStringValue("cash").build())
        .build()

    val wireMessage = mapOf("a" to "android", "c" to "cash")

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_MAP.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_MAP.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun mapOfAllTypes() {
    val googleMessage = Struct.newBuilder()
        .putFields("a", Value.newBuilder().setNullValue(NULL_VALUE).build())
        .putFields("b", Value.newBuilder().setNumberValue(0.5).build())
        .putFields("c", Value.newBuilder().setBoolValue(true).build())
        .putFields("d", Value.newBuilder().setStringValue("cash").build())
        .putFields("e", Value.newBuilder()
            .setListValue(ListValue.newBuilder()
                .addValues(Value.newBuilder().setStringValue("g").build())
                .addValues(Value.newBuilder().setStringValue("h").build())
                .build())
            .build())
        .putFields("f", Value.newBuilder()
            .setStructValue(Struct.newBuilder()
                .putFields("i", Value.newBuilder().setStringValue("j").build())
                .putFields("k", Value.newBuilder().setStringValue("l").build())
                .build())
            .build())
        .build()

    val wireMessage = mapOf(
        "a" to null,
        "b" to 0.5,
        "c" to true,
        "d" to "cash",
        "e" to listOf("g", "h"),
        "f" to mapOf("i" to "j", "k" to "l")
    )

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_MAP.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_MAP.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun unsupportedKeyType() {
    @Suppress("UNCHECKED_CAST") // Totally unsafe.
    val wireMessage = mapOf(5 to "android") as Map<String, Any?>

    try {
      ProtoAdapter.STRUCT_MAP.encode(wireMessage)
      fail()
    } catch (_: ClassCastException) {
    }

    try {
      ProtoAdapter.STRUCT_MAP.encodedSize(wireMessage)
      fail()
    } catch (_: ClassCastException) {
    }

    try {
      ProtoAdapter.STRUCT_MAP.redact(wireMessage)
      fail()
    } catch (_: IllegalArgumentException) {
    }
  }

  @Test fun unsupportedValueType() {
    val wireMessage = mapOf("a" to StringBuilder("android"))

    try {
      ProtoAdapter.STRUCT_MAP.encode(wireMessage)
      fail()
    } catch (_: IllegalArgumentException) {
    }

    try {
      ProtoAdapter.STRUCT_MAP.encodedSize(wireMessage)
      fail()
    } catch (_: IllegalArgumentException) {
    }

    try {
      ProtoAdapter.STRUCT_MAP.redact(wireMessage)
      fail()
    } catch (_: IllegalArgumentException) {
    }
  }

  @Test fun list() {
    val googleMessage = ListValue.newBuilder()
        .addValues(Value.newBuilder().setStringValue("android").build())
        .addValues(Value.newBuilder().setStringValue("cash").build())
        .build()

    val wireMessage = listOf("android", "cash")

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_LIST.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_LIST.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun listOfAllTypes() {
    val googleMessage = ListValue.newBuilder()
        .addValues(Value.newBuilder().setNullValue(NULL_VALUE).build())
        .addValues(Value.newBuilder().setNumberValue(0.5).build())
        .addValues(Value.newBuilder().setBoolValue(true).build())
        .addValues(Value.newBuilder().setStringValue("cash").build())
        .addValues(Value.newBuilder()
            .setListValue(ListValue.newBuilder()
                .addValues(Value.newBuilder().setStringValue("a").build())
                .addValues(Value.newBuilder().setStringValue("b").build())
                .build())
            .build())
        .addValues(Value.newBuilder()
            .setStructValue(Struct.newBuilder()
                .putFields("c", Value.newBuilder().setStringValue("d").build())
                .putFields("e", Value.newBuilder().setStringValue("f").build())
                .build())
            .build())
        .build()

    val wireMessage = listOf(
        null,
        0.5,
        true,
        "cash",
        listOf("a", "b"),
        mapOf("c" to "d", "e" to "f")
    )

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_LIST.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_LIST.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun emptyList() {
    val googleMessage = ListValue.newBuilder()
        .build()

    val wireMessage = listOf<Any?>()

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_LIST.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_LIST.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun unsupportedListElement() {
    val wireMessage = listOf(StringBuilder())

    try {
      ProtoAdapter.STRUCT_LIST.encode(wireMessage)
      fail()
    } catch (_: IllegalArgumentException) {
    }

    try {
      ProtoAdapter.STRUCT_LIST.encodedSize(wireMessage)
      fail()
    } catch (_: IllegalArgumentException) {
    }

    try {
      ProtoAdapter.STRUCT_LIST.redact(wireMessage)
      fail()
    } catch (_: IllegalArgumentException) {
    }
  }

  @Test fun structJsonRoundTrip() {
    val json = """{
         |  "struct": {"a": 1.0},
         |  "list": ["a", 3.0],
         |  "nullValue": null,
         |  "valueA": "a",
         |  "valueB": 33.0,
         |  "valueC": true,
         |  "valueD": null,
         |  "valueE": {"a": 1.0},
         |  "valueF": ["a", 3.0]
         |}""".trimMargin()

    val wireAllStruct = AllStructs(
        struct = mapOf("a" to 1.0),
        list = listOf("a", 3.0),
        null_value = null,
        value_a = "a",
        value_b = 33.0,
        value_c = true,
        value_d = null,
        value_e = mapOf("a" to 1.0),
        value_f = listOf("a", 3.0)
    )

    val moshi = Moshi.Builder().add(WireJsonAdapterFactory()).build()
    val allStructAdapter = moshi.adapter(AllStructs::class.java)
    assertJsonEquals(allStructAdapter.toJson(wireAllStruct), json)
    assertThat(allStructAdapter.fromJson(json)).isEqualTo(wireAllStruct)
  }

  @Test fun structJsonRoundTripWithEmptyOrNestedMapAndList() {
    val json = """{
         |  "struct": {"a": null},
         |  "list": [],
         |  "valueA": {
         |    "a": [
         |      "b",
         |      2.0,
         |      {"c": false}
         |    ]
         |  },
         |  "valueB": [{"d": null, "e": "trois"}],
         |  "valueC": [],
         |  "valueD": {},
         |  "valueE": null,
         |  "valueF": null
         |}""".trimMargin()
    // Wire prints null value members while protoc doesn't.
    val jsonWithNullValue = """{"nullValue": null, ${json.substring(1)}"""

    val protocAllStruct = AllStructsOuterClass.AllStructs.newBuilder()
        .setStruct(
            Struct.newBuilder().putFields("a", Value.newBuilder().setNullValue(NULL_VALUE).build())
                .build())
        .setList(ListValue.newBuilder().build())
        .setValueA(Value.newBuilder().setStructValue(Struct.newBuilder().putFields("a",
            Value.newBuilder().setListValue(
                ListValue.newBuilder().addValues(Value.newBuilder().setStringValue("b").build())
                    .addValues(Value.newBuilder().setNumberValue(2.0).build()).addValues(
                    Value.newBuilder().setStructValue(Struct.newBuilder()
                        .putFields("c", Value.newBuilder().setBoolValue(false).build()).build())
                        .build()).build()).build())).build())
        .setValueB(Value.newBuilder().setListValue(ListValue.newBuilder().addValues(
            Value.newBuilder().setStructValue(Struct.newBuilder()
                .putFields("d", Value.newBuilder().setNullValue(NULL_VALUE).build())
                .putFields("e", Value.newBuilder().setStringValue("trois").build()).build())
                .build()).build()).build())
        .setValueC(Value.newBuilder().setListValue(ListValue.newBuilder().build()).build())
        .setValueD(Value.newBuilder().setStructValue(Struct.newBuilder().build()).build())
        .setValueE(Value.newBuilder().setNullValue(NULL_VALUE).build())
        .setValueF(Value.newBuilder().setNullValue(NULL_VALUE).build())
        .build()

    val jsonPrinter = JsonFormat.printer()
    assertJsonEquals(jsonPrinter.print(protocAllStruct), json)
    val jsonParser = JsonFormat.parser()
    val protocParsed = AllStructsOuterClass.AllStructs.newBuilder()
        .apply { jsonParser.merge(json, this) }
        .build()
    assertThat(protocParsed).isEqualTo(protocAllStruct)

    val wireAllStruct = AllStructs(
        struct = mapOf("a" to null),
        list = emptyList<Any>(),
        value_a = mapOf("a" to listOf("b", 2.0, mapOf("c" to false))),
        value_b = listOf(mapOf("d" to null, "e" to "trois")),
        value_c = emptyList<Any>(),
        value_d = emptyMap<String, Any>()
    )

    val moshi = Moshi.Builder().add(WireJsonAdapterFactory()).build()
    val allStructAdapter = moshi.adapter(AllStructs::class.java)
    assertJsonEquals(allStructAdapter.toJson(wireAllStruct), jsonWithNullValue)
    assertThat(allStructAdapter.fromJson(json)).isEqualTo(wireAllStruct)
    assertThat(allStructAdapter.fromJson(jsonWithNullValue)).isEqualTo(wireAllStruct)
  }
}
