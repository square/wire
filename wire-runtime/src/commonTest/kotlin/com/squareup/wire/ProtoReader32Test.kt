/*
 * Copyright (C) 2024 Square, Inc.
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

import kotlin.test.Test
import kotlin.test.assertEquals
import okio.ByteString.Companion.decodeHex

class ProtoReader32Test {
  @Test fun packedExposedAsRepeated() {
    val packedEncoded = "d20504d904bd05".decodeHex()
    val reader = ProtoReader32(packedEncoded)
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
    val reader = ProtoReader32(encoded)

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

  // Consider pasting new tests into ProtoReaderTest.kt also.
}
