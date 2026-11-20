/*
 * Copyright (C) 2026 Square, Inc.
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
package com.squareup.wire

import assertk.assertThat
import assertk.assertions.isEqualTo
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import org.junit.Test
import squareup.proto3.java.packed.Color as ColorJ
import squareup.proto3.java.packed.PackedByDefault as PackedByDefaultJ
import squareup.proto3.kotlin.packed.Color as ColorK
import squareup.proto3.kotlin.packed.PackedByDefault as PackedByDefaultK
import squareup.proto3.kotlin.packed.PackedByDefaultOuterClass.Color as ColorP
import squareup.proto3.kotlin.packed.PackedByDefaultOuterClass.PackedByDefault as PackedByDefaultP

class PackedByDefaultCompatibilityTest {
  // ┌─ 1: [true, false, true] (packed)
  // ├─ 2: [RED, GREEN] (packed)
  // ├─ 3: [LARGE, SMALL] (packed)
  // ├─ 4: true
  // ├─ 4: false
  // ╰─ 5: GREEN
  private val bytes = "0a03010001120201021a020201200120002802"

  private val protocMessage = PackedByDefaultP.newBuilder()
    .addAllRepBool(listOf(true, false, true))
    .addAllRepEnum(listOf(ColorP.RED, ColorP.GREEN))
    .addAllRepNestedEnum(listOf(PackedByDefaultP.Size.LARGE, PackedByDefaultP.Size.SMALL))
    .addAllUnpackedBool(listOf(true, false))
    .addAllUnpackedEnum(listOf(ColorP.GREEN))
    .build()

  private val kotlinMessage = PackedByDefaultK(
    rep_bool = listOf(true, false, true),
    rep_enum = listOf(ColorK.RED, ColorK.GREEN),
    rep_nested_enum = listOf(PackedByDefaultK.Size.LARGE, PackedByDefaultK.Size.SMALL),
    unpacked_bool = listOf(true, false),
    unpacked_enum = listOf(ColorK.GREEN),
  )

  private val javaMessage = PackedByDefaultJ.Builder()
    .rep_bool(listOf(true, false, true))
    .rep_enum(listOf(ColorJ.RED, ColorJ.GREEN))
    .rep_nested_enum(listOf(PackedByDefaultJ.Size.LARGE, PackedByDefaultJ.Size.SMALL))
    .unpacked_bool(listOf(true, false))
    .unpacked_enum(listOf(ColorJ.GREEN))
    .build()

  @Test fun protocPacksRepeatedBoolAndEnumByDefault() {
    assertThat(protocMessage.toByteArray().toByteString()).isEqualTo(bytes.decodeHex())
  }

  @Test fun kotlinEncodesLikeProtoc() {
    assertThat(kotlinMessage.encodeByteString()).isEqualTo(bytes.decodeHex())
    assertThat(PackedByDefaultK.ADAPTER.decode(protocMessage.toByteArray())).isEqualTo(kotlinMessage)
  }

  @Test fun javaEncodesLikeProtoc() {
    assertThat(javaMessage.encodeByteString()).isEqualTo(bytes.decodeHex())
    assertThat(PackedByDefaultJ.ADAPTER.decode(protocMessage.toByteArray())).isEqualTo(javaMessage)
  }

  @Test fun unknownEnumValueInPackedField() {
    // ╰─ 2: [RED, 99, GREEN] (packed)
    val bytes = "1203016302"

    val protocDecoded = PackedByDefaultP.parseFrom(bytes.decodeHex().toByteArray())
    assertThat(protocDecoded.repEnumValueList).isEqualTo(listOf(1, 99, 2))
    assertThat(protocDecoded.toByteArray().toByteString()).isEqualTo(bytes.decodeHex())

    // Kotlin keeps the unknown value in the list, so it encodes the same bytes as protoc.
    val kotlinDecoded = PackedByDefaultK.ADAPTER.decode(bytes.decodeHex())
    assertThat(kotlinDecoded.rep_enum).isEqualTo(listOf(ColorK.RED, ColorK.Unrecognized(99), ColorK.GREEN))
    assertThat(kotlinDecoded.encodeByteString()).isEqualTo(bytes.decodeHex())

    // Java keeps the unknown value in the unknown fields.
    // ┌─ 2: [RED, GREEN] (packed)
    // ╰─ 2: 99
    val javaDecoded = PackedByDefaultJ.ADAPTER.decode(bytes.decodeHex())
    assertThat(javaDecoded.rep_enum).isEqualTo(listOf(ColorJ.RED, ColorJ.GREEN))
    assertThat(javaDecoded.unknownFields).isEqualTo("1063".decodeHex())
    assertThat(javaDecoded.encodeByteString()).isEqualTo("120201021063".decodeHex())
  }

  @Test fun decodeUnpackedFormOfPackedFields() {
    // ┌─ 1: true
    // ├─ 1: false
    // ├─ 2: RED
    // ╰─ 3: LARGE
    val unpackedBytes = "0801080010011802"

    val protocDecoded = PackedByDefaultP.parseFrom(unpackedBytes.decodeHex().toByteArray())
    assertThat(protocDecoded.repBoolList).isEqualTo(listOf(true, false))

    val kotlinDecoded = PackedByDefaultK.ADAPTER.decode(unpackedBytes.decodeHex())
    assertThat(kotlinDecoded.rep_bool).isEqualTo(listOf(true, false))
    assertThat(kotlinDecoded.rep_enum).isEqualTo(listOf(ColorK.RED))
    assertThat(kotlinDecoded.rep_nested_enum).isEqualTo(listOf(PackedByDefaultK.Size.LARGE))

    val javaDecoded = PackedByDefaultJ.ADAPTER.decode(unpackedBytes.decodeHex())
    assertThat(javaDecoded.rep_bool).isEqualTo(listOf(true, false))
    assertThat(javaDecoded.rep_enum).isEqualTo(listOf(ColorJ.RED))
    assertThat(javaDecoded.rep_nested_enum).isEqualTo(listOf(PackedByDefaultJ.Size.LARGE))
  }
}
