/*
 * Copyright (C) 2025 Square, Inc.
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

import com.squareup.wire.protos.kotlin.map.Mappy
import com.squareup.wire.protos.kotlin.map.MappyTwo
import com.squareup.wire.protos.kotlin.map.Thing
import java.net.ProtocolException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex

/** Decodes each input with both reader types. */
class ProtoReader32MapTest {
  private fun decodeMappy(bytes: ByteString): List<Mappy> = listOf(
    Mappy.ADAPTER.decode(bytes.toByteArray()),
    Mappy.ADAPTER.decode(Buffer().write(bytes)),
  )

  private fun decodeMappyTwo(bytes: ByteString): List<MappyTwo> = listOf(
    MappyTwo.ADAPTER.decode(bytes.toByteArray()),
    MappyTwo.ADAPTER.decode(Buffer().write(bytes)),
  )

  @Test
  fun mapEntryWithUnknownTagIsSkipped() {
    // Entry with key "k", value "n", and an unknown varint field 3.
    for (mappy in decodeMappy("0a0a0a016b12030a016e1800".decodeHex())) {
      assertEquals(mapOf("k" to Thing("n")), mappy.things)
    }
    // Entry with unknown length-delimited, fixed32, and fixed64 fields.
    for (mappy in decodeMappy("0a1a0a016b12030a016e220200aa2d00000000310000000000000000".decodeHex())) {
      assertEquals(mapOf("k" to Thing("n")), mappy.things)
    }
  }

  @Test
  fun mapEntryWithoutValueHasDefaultValue() {
    for (mappy in decodeMappy("0a030a016b".decodeHex())) {
      assertEquals(mapOf("k" to Thing()), mappy.things)
    }
    for (mappy in decodeMappy("0a00".decodeHex())) {
      assertEquals(mapOf("" to Thing()), mappy.things)
    }
    for (mappy in decodeMappy("0a021800".decodeHex())) {
      assertEquals(mapOf("" to Thing()), mappy.things)
    }
  }

  @Test
  fun mapEntryWithUnknownEnumValueIsKeptAsUnknownField() {
    // Entries "a" to FOO, "k" to 77 (unknown), "b" to BAR.
    val bytes = ("0a050a01611001" + "0a050a016b104d" + "0a050a01621002").decodeHex()
    for (message in decodeMappyTwo(bytes)) {
      assertEquals(
        mapOf("a" to MappyTwo.ValueEnum.FOO, "b" to MappyTwo.ValueEnum.BAR),
        message.string_enums,
      )
      assertEquals("0a050a016b104d".decodeHex(), message.unknownFields)
      // The unknown entry survives a round trip.
      assertEquals(
        ("0a050a01611001" + "0a050a01621002" + "0a050a016b104d").decodeHex(),
        ByteString.of(*MappyTwo.ADAPTER.encode(message)),
      )
    }
  }

  @Test
  fun mapEntryWithoutEnumValueHasZeroValueAsBefore() {
    for (message in decodeMappyTwo("0a030a016b".decodeHex())) {
      assertEquals(mapOf("k" to MappyTwo.ValueEnum.DEFAULT), message.string_enums)
    }
  }

  @Test
  fun rootMapEntryWithUnknownEnumValueFails() {
    // There is no parent message to hold the unknown field.
    val mapAdapter = ProtoAdapter.newMapAdapter(ProtoAdapter.STRING, MappyTwo.ValueEnum.ADAPTER)
    val entry = "0a016b104d".decodeHex()
    assertFailsWith<ProtocolException> { mapAdapter.decode(entry.toByteArray()) }
    assertFailsWith<ProtocolException> { mapAdapter.decode(Buffer().write(entry)) }
  }
}
