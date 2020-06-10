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
import com.google.protobuf.NullValue.NULL_VALUE
import com.google.protobuf.Struct
import com.google.protobuf.Value
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Test

class StructTest {
  @Test fun nullValue() {
    val googleMessage = Value.newBuilder()
        .setNullValue(NullValue.NULL_VALUE)
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
}
