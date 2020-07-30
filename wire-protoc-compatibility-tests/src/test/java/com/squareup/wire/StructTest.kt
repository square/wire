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
import com.google.protobuf.NullValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Test
import squareup.proto3.kotlin.alltypes.AllStructsOuterClass
import squareup.proto3.java.alltypes.AllStructs as AllStructsJ
import squareup.proto3.kotlin.alltypes.AllStructs as AllStructsK

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

  @Test fun nullMapAndListAsFields() {
    val protocAllStruct = AllStructsOuterClass.AllStructs.newBuilder().build()
    val wireAllStructJava = AllStructsJ.Builder().build()
    val wireAllStructKotlin = AllStructsK()

    val protocAllStructBytes = protocAllStruct.toByteArray()
    assertThat(AllStructsJ.ADAPTER.encode(wireAllStructJava)).isEqualTo(protocAllStructBytes)
    assertThat(AllStructsJ.ADAPTER.decode(protocAllStructBytes)).isEqualTo(wireAllStructJava)
    assertThat(AllStructsK.ADAPTER.encode(wireAllStructKotlin)).isEqualTo(protocAllStructBytes)
    assertThat(AllStructsK.ADAPTER.decode(protocAllStructBytes)).isEqualTo(wireAllStructKotlin)
  }

  @Test fun emptyMapAndListAsFields() {
    val protocAllStruct = AllStructsOuterClass.AllStructs.newBuilder()
        .setStruct(emptyStruct())
        .setList(emptyListValue())
        .build()
    val wireAllStructJava = AllStructsJ.Builder()
        .struct(emptyMap<String, Any?>())
        .list(emptyList<Any?>())
        .build()
    val wireAllStructKotlin = AllStructsK(
        struct = emptyMap<String, Any?>(),
        list = emptyList<Any?>())

    val protocAllStructBytes = protocAllStruct.toByteArray()
    assertThat(AllStructsJ.ADAPTER.encode(wireAllStructJava)).isEqualTo(protocAllStructBytes)
    assertThat(AllStructsJ.ADAPTER.decode(protocAllStructBytes)).isEqualTo(wireAllStructJava)
    assertThat(AllStructsK.ADAPTER.encode(wireAllStructKotlin)).isEqualTo(protocAllStructBytes)
    assertThat(AllStructsK.ADAPTER.decode(protocAllStructBytes)).isEqualTo(wireAllStructKotlin)
  }

  // Note: We are not testing nulls because while protoc emits `NULL_VALUE`s, Wire doesn't.
  @Test fun structRoundTripWithData() {
    val protocAllStruct = AllStructsOuterClass.AllStructs.newBuilder()
        .setStruct(mapOf("a" to 1.0).toStruct())
        .setList(listOf("a", 3.0).toListValue())
        .setNullValue(NullValue.NULL_VALUE)
        .setValueA("a".toValue())
        .setValueB(33.0.toValue())
        .setValueC(true.toValue())
        .setValueE(mapOf("a" to 1.0).toValue())
        .setValueF(listOf("a", 3.0).toValue())
        .build()
    val wireAllStructJava = AllStructsJ.Builder()
        .struct(mapOf("a" to 1.0))
        .list(listOf("a", 3.0))
        .null_value(null)
        .value_a("a")
        .value_b(33.0)
        .value_c(true)
        .value_e(mapOf("a" to 1.0))
        .value_f(listOf("a", 3.0))
        .build()
    val wireAllStructKotlin = AllStructsK(
        struct = mapOf("a" to 1.0),
        list = listOf("a", 3.0),
        null_value = null,
        value_a = "a",
        value_b = 33.0,
        value_c = true,
        value_e = mapOf("a" to 1.0),
        value_f = listOf("a", 3.0)
    )

    val protocAllStructBytes = protocAllStruct.toByteArray()
    assertThat(AllStructsJ.ADAPTER.encode(wireAllStructJava)).isEqualTo(protocAllStructBytes)
    assertThat(AllStructsJ.ADAPTER.decode(protocAllStructBytes)).isEqualTo(wireAllStructJava)
    assertThat(AllStructsK.ADAPTER.encode(wireAllStructKotlin)).isEqualTo(protocAllStructBytes)
    assertThat(AllStructsK.ADAPTER.decode(protocAllStructBytes)).isEqualTo(wireAllStructKotlin)
  }
}
