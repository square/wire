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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.squareup.wire.ReverseProtoWriterTest.Person
import kotlin.test.Test
import okio.Buffer
import okio.ByteString.Companion.decodeHex
import okio.IOException

class ProtoReaderTest {
  @Test fun packedExposedAsRepeated() {
    val packedEncoded = "d20504d904bd05".decodeHex()
    val reader = ProtoReader(Buffer().write(packedEncoded))
    val token = reader.beginMessage()
    assertThat(reader.nextTag()).isEqualTo(90)
    assertThat(ProtoAdapter.INT32.decode(reader)).isEqualTo(601)
    assertThat(reader.nextTag()).isEqualTo(90)
    assertThat(ProtoAdapter.INT32.decode(reader)).isEqualTo(701)
    assertThat(reader.nextTag()).isEqualTo(-1)
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

    assertThat(reader.nextLengthDelimited()).isEqualTo(2)

    val firstToken = reader.beginMessage()
    assertThat(reader.nextTag()).isEqualTo(1)
    assertThat(ProtoAdapter.INT32.decode(reader)).isEqualTo(2)
    assertThat(reader.nextTag()).isEqualTo(-1)
    reader.endMessageAndGetUnknownFields(firstToken)

    assertThat(reader.nextLengthDelimited()).isEqualTo(6)

    val secondToken = reader.beginMessage()
    assertThat(reader.nextTag()).isEqualTo(1)
    assertThat(ProtoAdapter.INT32.decode(reader)).isEqualTo(Int.MAX_VALUE)
    assertThat(reader.nextTag()).isEqualTo(-1)
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

    assertFailure {
      Person.ADAPTER.decode(Buffer().write(data))
    }.isInstanceOf<IOException>().hasMessage("Wire recursion limit exceeded")
  }

  // Consider pasting new tests into ProtoReader32Test.kt also.
}
