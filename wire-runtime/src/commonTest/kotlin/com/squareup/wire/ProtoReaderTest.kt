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
import com.squareup.wire.internal.DoubleArrayList
import com.squareup.wire.internal.FloatArrayList
import com.squareup.wire.internal.IntArrayList
import com.squareup.wire.internal.LongArrayList
import kotlin.test.Test
import okio.Buffer
import okio.ByteString.Companion.decodeHex
import okio.EOFException
import okio.ForwardingSource
import okio.IOException
import okio.buffer

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

  @Test fun messageRejectsIncompatibleFieldEncoding() {
    for ((hex, encoding) in listOf(
      "8d0100000000" to 5,
      "89010000000000000000" to 1,
      "880100" to 0,
    )) {
      val encoded = hex.decodeHex()
      val reader = ProtoReader(Buffer().write(encoded))
      reader.beginMessage()
      assertThat(reader.nextTag()).isEqualTo(17)

      assertFailure {
        reader.beginMessage()
      }.isInstanceOf<IOException>()
        .hasMessage("Expected LENGTH_DELIMITED but was $encoding. Reader position: 2. Last read tag: 17.")
    }
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

  /** We had a bug where negative lengths in skipped groups crashed with runtime exceptions. */
  @Test fun testSkipGroupRejectsNegativeLength() {
    val data = "9b060a80ffffff0f9c06".decodeHex()

    assertFailure {
      Person.ADAPTER.decode(Buffer().write(data))
    }.isInstanceOf<IOException>()
      .hasMessage("Negative length: -128. Reader position: 8. Last read tag: 1.")
  }

  @Test fun fixed32CannotReadPastLengthDelimitedLimit() {
    val encoded = (
      "02" + // varint32 length = 2
        "0d" + // 1: fixed32
        "05000000" // enough bytes in the source, but not in the current message
      ).decodeHex()
    val reader = ProtoReader(Buffer().write(encoded))

    assertThat(reader.nextLengthDelimited()).isEqualTo(2)
    reader.beginMessage()
    assertThat(reader.nextTag()).isEqualTo(1)
    assertFailure {
      reader.readFixed32()
    }.isInstanceOf<EOFException>()
  }

  @Test fun packedAllocationHintDoesNotTrustDeclaredLength() {
    for (length in listOf(0x0fffffff, Int.MAX_VALUE)) {
      val source = Buffer().writeByte(0x12)
      ProtoWriter(source).writeVarint32(length)
      val reader = ProtoReader(source)
      reader.beginMessage()
      assertThat(reader.nextTag()).isEqualTo(2)

      // Check the hint before allocating, so a regression cannot exhaust the test process heap.
      val hint = reader.nextFieldMinLengthInBytes()
      assertThat(hint).isEqualTo(0L)
      val values = ArrayList<Int>(hint.toInt())
      assertFailure {
        ProtoAdapter.INT32.tryDecode(reader, values)
      }.isInstanceOf<EOFException>()
    }
  }

  @Test fun packedPrimitiveArrayAllocationHintDoesNotTrustDeclaredLength() {
    val source = Buffer().write("12ffffffff07".decodeHex())
    val reader = ProtoReader(source)
    reader.beginMessage()
    assertThat(reader.nextTag()).isEqualTo(2)
    val hint = reader.nextFieldMinLengthInBytes()
    assertThat(hint).isEqualTo(0L)
    assertThat(IntArrayList.forDecoding(hint, 1).toArray().size).isEqualTo(0)
    assertThat(LongArrayList.forDecoding(hint, 1).toArray().size).isEqualTo(0)
    assertThat(FloatArrayList.forDecoding(hint, 4).toArray().size).isEqualTo(0)
    assertThat(DoubleArrayList.forDecoding(hint, 8).toArray().size).isEqualTo(0)
    assertFailure {
      reader.readFixed64()
    }.isInstanceOf<EOFException>()
  }

  @Test fun packedAllocationHintRespectsBufferedBytesAndFieldLimit() {
    val truncated = ProtoReader(Buffer().write("12ffffffff070102".decodeHex()))
    truncated.beginMessage()
    assertThat(truncated.nextTag()).isEqualTo(2)
    assertThat(truncated.nextFieldMinLengthInBytes()).isEqualTo(2L)

    val complete = ProtoReader(Buffer().write("120201021807".decodeHex()))
    val token = complete.beginMessage()
    assertThat(complete.nextTag()).isEqualTo(2)
    assertThat(complete.nextFieldMinLengthInBytes()).isEqualTo(2L)
    assertThat(complete.readVarint32()).isEqualTo(1)
    assertThat(complete.nextTag()).isEqualTo(2)
    assertThat(complete.readVarint32()).isEqualTo(2)
    assertThat(complete.nextTag()).isEqualTo(3)
    assertThat(complete.readVarint32()).isEqualTo(7)
    assertThat(complete.nextTag()).isEqualTo(-1)
    complete.endMessageAndGetUnknownFields(token)
  }

  @Test fun packedAllocationHintDoesNotReadAhead() {
    var reads = 0
    val source = object : ForwardingSource(Buffer().write("1204010203041807".decodeHex())) {
      override fun read(sink: Buffer, byteCount: Long): Long {
        reads++
        return super.read(sink, minOf(byteCount, 1L))
      }
    }.buffer()
    val reader = ProtoReader(source)
    val token = reader.beginMessage()
    assertThat(reader.nextTag()).isEqualTo(2)
    assertThat(reads).isEqualTo(2)
    assertThat(reader.nextFieldMinLengthInBytes()).isEqualTo(0L)
    assertThat(reads).isEqualTo(2)

    // A zero hint must still allow a packed list to grow as streaming bytes arrive.
    val values = ArrayList<Int>(0)
    for (value in 1..4) {
      ProtoAdapter.INT32.tryDecode(reader, values)
      assertThat(reader.nextTag()).isEqualTo(if (value < 4) 2 else 3)
    }
    assertThat(values).isEqualTo(listOf(1, 2, 3, 4))
    assertThat(reader.readVarint32()).isEqualTo(7)
    assertThat(reader.nextTag()).isEqualTo(-1)
    reader.endMessageAndGetUnknownFields(token)
  }

  // Consider pasting new tests into ProtoReader32Test.kt also.
}
