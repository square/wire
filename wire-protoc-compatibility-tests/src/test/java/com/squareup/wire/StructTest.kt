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
    val googleMessage = null.toValue()

    val wireMessage = null

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_VALUE.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_VALUE.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun doubleValue() {
    val googleMessage = 0.25.toValue()

    val wireMessage = 0.25

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_VALUE.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_VALUE.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun specialDoubleValues() {
    val googleMessage = listOf(
        Double.NEGATIVE_INFINITY,
        -0.0,
        0.0,
        Double.POSITIVE_INFINITY,
        Double.NaN
    ).toListValue()

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
    val googleMessage = true.toValue()

    val wireMessage = true

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_VALUE.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_VALUE.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun booleanFalse() {
    val googleMessage = false.toValue()

    val wireMessage = false

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_VALUE.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_VALUE.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun stringValue() {
    val googleMessage = "Cash App!".toValue()

    val wireMessage = "Cash App!"

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_VALUE.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_VALUE.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun emptyStringValue() {
    val googleMessage = "".toValue()

    val wireMessage = ""

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_VALUE.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_VALUE.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun utf8StringValue() {
    val googleMessage = "На берегу пустынных волн".toValue()

    val wireMessage = "На берегу пустынных волн"

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_VALUE.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_VALUE.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun map() {
    val googleMessage = mapOf("a" to "android", "c" to "cash").toStruct()

    val wireMessage = mapOf("a" to "android", "c" to "cash")

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_MAP.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_MAP.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun mapOfAllTypes() {
    val googleMessage = mapOf(
        "a" to null,
        "b" to 0.5,
        "c" to true,
        "d" to "cash",
        "e" to listOf("g", "h"),
        "f" to mapOf("i" to "j", "k" to "l")
    ).toStruct()

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

  @Test fun mapWithoutEntries() {
    val googleMessage = emptyStruct()

    val wireMessage = mapOf<String, Any?>()

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
    val googleMessage = listOf("android", "cash").toListValue()

    val wireMessage = listOf("android", "cash")

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_LIST.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_LIST.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun listOfAllTypes() {
    val googleMessage = listOf(
        null,
        0.5,
        true,
        "cash",
        listOf("a", "b"),
        mapOf("c" to "d", "e" to "f")
    ).toListValue()

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

  @Test fun listValueWithoutElements() {
    val googleMessage = ListValue.newBuilder().build()

    val wireMessage = listOf<Any?>()

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.STRUCT_LIST.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.STRUCT_LIST.decode(googleMessageBytes)).isEqualTo(wireMessage)
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
      |  "valueF": ["a", 3.0],
      |  "repStruct": [],
      |  "repList": [],
      |  "repValueA": [],
      |  "repNullValue": [],
      |  "mapInt32Struct": {},
      |  "mapInt32List": {},
      |  "mapInt32ValueA": {},
      |  "mapInt32NullValue": {},
      |  "oneofStruct": null,
      |  "oneofList": null,
      |  "oneofValueA": null,
      |  "oneofNullValue": null
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
    val protocJson = """{
      |  "struct": {"a": null},
      |  "list": [],
      |  "valueA": {"a": ["b", 2.0, {"c": false}]},
      |  "valueB": [{"d": null, "e": "trois"}],
      |  "valueC": [],
      |  "valueD": {},
      |  "valueE": null,
      |  "valueF": null
      |}""".trimMargin()
    // Protoc doesn't print those unless explicitly set.
    val moshiJson = """{
      |  "nullValue": null,
      |  "repStruct": [],
      |  "repList": [],
      |  "repValueA": [],
      |  "repNullValue": [],
      |  "mapInt32Struct": {},
      |  "mapInt32List": {},
      |  "mapInt32ValueA": {},
      |  "mapInt32NullValue": {},
      |  "oneofStruct": null,
      |  "oneofList": null,
      |  "oneofValueA": null,
      |  "oneofNullValue": null,
      |${protocJson.substring(1)}""".trimMargin()

    val protocAllStruct = AllStructsOuterClass.AllStructs.newBuilder()
        .setStruct(mapOf("a" to null).toStruct())
        .setList(emptyListValue())
        .setValueA(mapOf("a" to listOf("b", 2.0, mapOf("c" to false))).toValue())
        .setValueB(listOf(mapOf("d" to null, "e" to "trois")).toValue())
        .setValueC(emptyList<Any>().toValue())
        .setValueD(emptyMap<String, Any>().toValue())
        .setValueE(null.toValue())
        .setValueF(null.toValue())
        .build()

    val jsonPrinter = JsonFormat.printer()
    assertJsonEquals(jsonPrinter.print(protocAllStruct), protocJson)
    val jsonParser = JsonFormat.parser()
    val protocParsed = AllStructsOuterClass.AllStructs.newBuilder()
        .apply { jsonParser.merge(protocJson, this) }
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
    assertJsonEquals(allStructAdapter.toJson(wireAllStruct), moshiJson)
    assertThat(allStructAdapter.fromJson(protocJson)).isEqualTo(wireAllStruct)
    assertThat(allStructAdapter.fromJson(moshiJson)).isEqualTo(wireAllStruct)
  }
}
