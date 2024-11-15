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
package com.squareup.wire

import assertk.assertions.message
import com.squareup.wire.ReverseProtoWriterTest.Person
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import okio.Buffer
import okio.ByteString.Companion.decodeHex
import okio.IOException

class ProtoReaderTest {
  @Test fun packedExposedAsRepeated() {
    val packedEncoded = "d20504d904bd05".decodeHex()
    val reader = ProtoReader(Buffer().write(packedEncoded))
    val token = reader.beginMessage()
    assertEquals(90, reader.nextTag())
    assertEquals(601, ProtoAdapter.INT32.decode(reader))
    assertEquals(90, reader.nextTag())
    assertEquals(701, ProtoAdapter.INT32.decode(reader))
    assertEquals(-1, reader.nextTag())
    reader.endMessageAndGetUnknownFields(token)
  }

  @Test fun lengthDelimited() {
    val encoded = (
      "02" + // varint32 length = 2
        "0802" + // 1: int32 = 2
        "06" + // varint32 length = 6
        "08ffffffff07" // 1: int32 = 2,147,483,647
      ).decodeHex()
    val reader = ProtoReader(Buffer().write(encoded))

    assertEquals(2, reader.nextLengthDelimited())

    val firstToken = reader.beginMessage()
    assertEquals(1, reader.nextTag())
    assertEquals(2, ProtoAdapter.INT32.decode(reader))
    assertEquals(-1, reader.nextTag())
    reader.endMessageAndGetUnknownFields(firstToken)

    assertEquals(6, reader.nextLengthDelimited())

    val secondToken = reader.beginMessage()
    assertEquals(1, reader.nextTag())
    assertEquals(Int.MAX_VALUE, ProtoAdapter.INT32.decode(reader))
    assertEquals(-1, reader.nextTag())
    reader.endMessageAndGetUnknownFields(secondToken)
  }

  /** We had a bug where we weren't enforcing recursion limits for groups. */
  @Test fun testSkipGroupNested() {
    val data = ByteArray(50000) {
      when {
        it % 2 == 0 -> 0xa3.toByte()
        else -> 0x01.toByte()
      }
    }

    val failed = assertFailsWith<IOException> {
      Person.ADAPTER.decode(Buffer().write(data))
    }
    assertEquals("Wire recursion limit exceeded", failed.message)
  }

  // Consider pasting new tests into ProtoReader32Test.kt also.
}
