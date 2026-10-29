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

import com.squareup.wire.protos.empty_packed.EmptyPacked
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.EOFException

class EmptyPackedTest {
  private val adapters = listOf(EmptyPacked.ADAPTER, ProtoAdapter.newMessageAdapter(EmptyPacked::class.java))

  @Test fun emptyPackedScalarsAreSkipped() {
    for (adapter in adapters) {
      for (hex in listOf("0a0012001a0022002a0032005a0062006a007200", "0a005007", "50070a00")) {
        val expected = EmptyPacked.Builder().apply { if (hex.contains("5007")) after = 7 }.build()
        val bytes = hex.decodeHex()
        assertEquals(expected, adapter.decode(bytes))
        assertEquals(expected, adapter.decode(Buffer().write(bytes)))
      }
    }
  }

  @Test fun emptyPackedRecordsCanSurroundValues() {
    val expected = EmptyPacked.Builder().apply {
      numbers = listOf(1, 2, 3)
      flags = listOf(true, false)
      choices = listOf(EmptyPacked.Choice.A, EmptyPacked.Choice.B)
      packed_numbers = listOf(4, 5)
      fixed32s = listOf(1)
      fixed64s = listOf(2L)
      after = 7
    }.build()
    val bytes = "0a000a0201020a00080312001202010012001a001a0201021a002200220104200522002a002a04010000002a0032003208020000000000000032005007".decodeHex()
    for (adapter in adapters) {
      assertEquals(expected, adapter.decode(bytes))
      assertEquals(expected, adapter.decode(Buffer().write(bytes)))
    }
  }

  @Test fun emptyPackedEnumRecordsRetainUnknownValues() {
    val bytes = "1a001a030163021a005007".decodeHex()
    for (adapter in adapters) {
      for (message in listOf(adapter.decode(bytes), adapter.decode(Buffer().write(bytes)))) {
        assertEquals(listOf(EmptyPacked.Choice.A, EmptyPacked.Choice.B), message.choices)
        assertEquals(7, message.after)
        assertEquals("1863".decodeHex(), message.unknownFields)
      }
    }
  }

  @Test fun emptyLengthDelimitedValuesAreRetained() {
    val expected = EmptyPacked.Builder().apply {
      strings = listOf("")
      blobs = listOf(ByteString.EMPTY)
      children = listOf(EmptyPacked.Child.Builder().build())
      after = 7
    }.build()
    val bytes = "3a0042004a005007".decodeHex()
    for (adapter in adapters) {
      assertEquals(expected, adapter.decode(bytes))
      assertEquals(expected, adapter.decode(Buffer().write(bytes)))
    }
  }

  @Test fun truncatedPackedScalarStillFails() {
    for (adapter in adapters) {
      for (hex in listOf("0a0180", "2a03010203", "320701020304050607")) {
        val bytes = hex.decodeHex()
        assertFailsWith<EOFException> { adapter.decode(bytes) }
        assertFailsWith<EOFException> { adapter.decode(Buffer().write(bytes)) }
      }
    }
  }
}
