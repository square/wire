/*
 * Copyright (C) 2019 Square, Inc.
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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import okio.Buffer
import okio.ByteString.Companion.decodeHex

class ProtoAdapterTest {
  @Test fun singularScalarWrappersIgnorePackedValues() {
    for ((adapter, payload, expected) in listOf<Triple<ProtoAdapter<*>, String, Any>>(
      Triple(ProtoAdapter.DOUBLE_VALUE, "000000000000f03f", 0.0),
      Triple(ProtoAdapter.FLOAT_VALUE, "0000803f", 0.0f),
      Triple(ProtoAdapter.INT64_VALUE, "07", 0L),
      Triple(ProtoAdapter.UINT64_VALUE, "07", 0L),
      Triple(ProtoAdapter.INT32_VALUE, "07", 0),
      Triple(ProtoAdapter.UINT32_VALUE, "07", 0),
      Triple(ProtoAdapter.BOOL_VALUE, "01", false),
    )) {
      val value = payload.decodeHex()
      val encoded = Buffer().writeByte(0x0a).writeByte(value.size).write(value).readByteString()
      assertThat(adapter.decode(encoded)).isEqualTo(expected)
      assertThat(adapter.decode(Buffer().write(encoded))).isEqualTo(expected)
      assertThat(adapter.decode("0a00".decodeHex())).isEqualTo(expected)
      assertThat(adapter.decode(Buffer().write("0a00".decodeHex()))).isEqualTo(expected)
    }
    assertThat(ProtoAdapter.STRING_VALUE.decode("0a0161".decodeHex())).isEqualTo("a")
    assertThat(ProtoAdapter.BYTES_VALUE.decode("0a0161".decodeHex())).isEqualTo("61".decodeHex())
  }

  @Test fun singularWrapperValueSurvivesPackedOccurrence() {
    for (hex in listOf("08090a020102", "0a0201020809", "0a000809")) {
      val encoded = hex.decodeHex()
      assertThat(ProtoAdapter.INT32_VALUE.decode(encoded)).isEqualTo(9)
      assertThat(ProtoAdapter.INT32_VALUE.decode(Buffer().write(encoded))).isEqualTo(9)
    }
  }

  @Test fun durationAndTimestampIgnorePackedSecondsAndNanos() {
    for (hex in listOf("0a0107120108", "0a001200")) {
      val encoded = hex.decodeHex()
      assertThat(ProtoAdapter.DURATION.decode(encoded)).isEqualTo(durationOfSeconds(0, 0))
      assertThat(ProtoAdapter.DURATION.decode(Buffer().write(encoded))).isEqualTo(durationOfSeconds(0, 0))
      assertThat(ProtoAdapter.INSTANT.decode(encoded)).isEqualTo(ofEpochSecond(0, 0))
      assertThat(ProtoAdapter.INSTANT.decode(Buffer().write(encoded))).isEqualTo(ofEpochSecond(0, 0))
    }
    for (hex in listOf("0809100a0a0107120108", "0a01071201080809100a")) {
      val encoded = hex.decodeHex()
      assertThat(ProtoAdapter.DURATION.decode(encoded)).isEqualTo(durationOfSeconds(9, 10))
      assertThat(ProtoAdapter.DURATION.decode(Buffer().write(encoded))).isEqualTo(durationOfSeconds(9, 10))
      assertThat(ProtoAdapter.INSTANT.decode(encoded)).isEqualTo(ofEpochSecond(9, 10))
      assertThat(ProtoAdapter.INSTANT.decode(Buffer().write(encoded))).isEqualTo(ofEpochSecond(9, 10))
    }
  }

  @Test fun structScalarOneOfIgnoresPackedOccurrence() {
    for (invalid in listOf("0a0100", "1208000000000000f03f", "220101", "0a00", "1200", "2200")) {
      for (hex in listOf("1a0161$invalid", "${invalid}1a0161")) {
        val encoded = hex.decodeHex()
        assertThat(ProtoAdapter.STRUCT_VALUE.decode(encoded)).isEqualTo("a")
        assertThat(ProtoAdapter.STRUCT_VALUE.decode(Buffer().write(encoded))).isEqualTo("a")
      }
    }
  }

  @Test fun mapEntryScalarsIgnorePackedOccurrence() {
    val adapter = ProtoAdapter.newMapAdapter(ProtoAdapter.INT32, ProtoAdapter.INT32)
    for (hex in listOf("0a0107120108", "0a001200")) {
      val encoded = hex.decodeHex()
      assertThat(adapter.decode(encoded)).isEqualTo(mapOf(0 to 0))
      assertThat(adapter.decode(Buffer().write(encoded))).isEqualTo(mapOf(0 to 0))
    }
    for (hex in listOf("080110020a0107120108", "0a010712010808011002")) {
      val encoded = hex.decodeHex()
      assertThat(adapter.decode(encoded)).isEqualTo(mapOf(1 to 2))
      assertThat(adapter.decode(Buffer().write(encoded))).isEqualTo(mapOf(1 to 2))
    }
  }

  @Test fun repeatedRepeatedProtoAdapterForbidden() {
    assertFailure {
      ProtoAdapter.BOOL.asRepeated().asRepeated()
    }.isInstanceOf<UnsupportedOperationException>()
  }

  @Test fun packedPackedProtoAdapterForbidden() {
    // Unable to pack a length-delimited type.
    assertFailure {
      ProtoAdapter.BOOL.asPacked().asPacked()
    }.isInstanceOf<IllegalArgumentException>()
  }

  @Test fun repeatedPackedProtoAdapterForbidden() {
    assertFailure {
      ProtoAdapter.BOOL.asRepeated().asPacked()
    }.isInstanceOf<UnsupportedOperationException>()
  }

  @Test fun packedRepeatedProtoAdapterForbidden() {
    assertFailure {
      ProtoAdapter.BOOL.asPacked().asRepeated()
    }.isInstanceOf<UnsupportedOperationException>()
  }

  @Test fun instantEncodeValidMinBoundary() {
    // 0001-01-01T00:00:00Z.
    val instant = ofEpochSecond(-62135596800L, 0L)
    val bytes = ProtoAdapter.INSTANT.encode(instant)
    assertThat(ProtoAdapter.INSTANT.decode(bytes).getEpochSecond()).isEqualTo(-62135596800L)
  }

  @Test fun instantEncodeValidMaxBoundary() {
    // 9999-12-31T23:59:59Z.
    val instant = ofEpochSecond(253402300799L, 999_999_999L)
    val bytes = ProtoAdapter.INSTANT.encode(instant)
    val decoded = ProtoAdapter.INSTANT.decode(bytes)
    assertThat(decoded.getEpochSecond()).isEqualTo(253402300799L)
    assertThat(decoded.getNano()).isEqualTo(999_999_999)
  }

  @Test fun instantEncodeRejectsSecondsBelowMin() {
    // 0001-01-01T00:00:00Z - 1 second.
    val instant = ofEpochSecond(-62135596801L, 0L)
    assertFailure {
      ProtoAdapter.INSTANT.encode(instant)
    }.isInstanceOf<IllegalArgumentException>()
  }

  @Test fun instantEncodeRejectsSecondsAboveMax() {
    // 9999-12-31T23:59:59Z + 1 second.
    val instant = ofEpochSecond(253402300800L, 0L)
    assertFailure {
      ProtoAdapter.INSTANT.encode(instant)
    }.isInstanceOf<IllegalArgumentException>()
  }

  @Test fun instantEncodedSizeRejectsOutOfRange() {
    // 0001-01-01T00:00:00Z - 1 second.
    val instant = ofEpochSecond(-62135596801L, 0L)
    assertFailure {
      ProtoAdapter.INSTANT.encodedSize(instant)
    }.isInstanceOf<IllegalArgumentException>()
  }
}
