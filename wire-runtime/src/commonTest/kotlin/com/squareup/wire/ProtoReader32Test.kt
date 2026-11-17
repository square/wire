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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.squareup.wire.ReverseProtoWriterTest.Person
import kotlin.test.Test
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import okio.EOFException
import okio.IOException

class ProtoReader32Test {
  @Test fun packedExposedAsRepeated() {
    val packedEncoded = "d20504d904bd05".decodeHex()
    val reader = ProtoReader32(packedEncoded)
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
      val reader = ProtoReader32(encoded)
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
    val reader = ProtoReader32(encoded)

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
      Person.ADAPTER.decode(data)
    }.isInstanceOf<IOException>().hasMessage("Wire recursion limit exceeded")
  }

  @Test fun defaultRecursionLimit() {
    assertThat(ProtoReader32(nestedMessages(100)).readNestingDepth()).isEqualTo(100)

    assertFailure {
      ProtoReader32(nestedMessages(101)).readNestingDepth()
    }.isInstanceOf<IOException>().hasMessage("Wire recursion limit exceeded")
  }

  @Test fun customRecursionLimit() {
    assertThat(ProtoReader32(nestedMessages(150), recursionLimit = 150).readNestingDepth())
      .isEqualTo(150)
    assertThat(ProtoReader32(nestedMessages(150).toByteString(), recursionLimit = 150).readNestingDepth())
      .isEqualTo(150)

    assertFailure {
      ProtoReader32(nestedMessages(151), recursionLimit = 150).readNestingDepth()
    }.isInstanceOf<IOException>().hasMessage("Wire recursion limit exceeded")

    assertFailure {
      ProtoReader32(nestedMessages(11), recursionLimit = 10).readNestingDepth()
    }.isInstanceOf<IOException>().hasMessage("Wire recursion limit exceeded")
  }

  @Test fun customRecursionLimitAppliesToGroups() {
    // A message with a group (field 20) nested 10 times. The outermost group doesn't count.
    val data = "a301".repeat(10).decodeHex().toByteArray() + "a401".repeat(10).decodeHex().toByteArray()

    assertThat(ProtoReader32(data, recursionLimit = 10).readNestingDepth()).isEqualTo(1)

    assertFailure {
      ProtoReader32(data, recursionLimit = 9).readNestingDepth()
    }.isInstanceOf<IOException>().hasMessage("Wire recursion limit exceeded")
  }

  @Test fun negativeRecursionLimitIsRejected() {
    assertFailure {
      ProtoReader32(ByteArray(0), recursionLimit = -1)
    }.isInstanceOf<IllegalArgumentException>()
      .hasMessage("recursionLimit=-1 must not be negative")
  }

  @Test fun asProtoReaderKeepsRecursionLimit() {
    assertFailure {
      ProtoReader32(nestedMessages(11), recursionLimit = 10).asProtoReader().readNestingDepth()
    }.isInstanceOf<IOException>().hasMessage("Wire recursion limit exceeded")
  }

  /** We had a bug where negative lengths in skipped groups crashed with runtime exceptions. */
  @Test fun testSkipGroupRejectsNegativeLength() {
    val data = "9b060a80ffffff0f9c06".decodeHex()

    assertFailure {
      Person.ADAPTER.decode(data)
    }.isInstanceOf<IOException>()
      .hasMessage("Negative length: -128. Reader position: 8. Last read tag: 1.")
  }

  /** We had a bug where positive lengths near Int.MAX_VALUE overflowed cursor math. */
  @Test fun lengthDelimitedRejectsPositiveLengthOverflow() {
    val knownString = "0affffffff07".decodeHex()
    val unknownBytes = "1affffffff07".decodeHex()
    val skippedGroup = "0b0affffffff070c".decodeHex()

    assertFailure {
      Person.ADAPTER.decode(knownString)
    }.isInstanceOf<IOException>()

    assertFailure {
      Person.ADAPTER.decode(unknownBytes)
    }.isInstanceOf<IOException>()

    assertFailure {
      Person.ADAPTER.decode(skippedGroup)
    }.isInstanceOf<IOException>()
  }

  @Test fun fixed32CannotReadPastLengthDelimitedLimit() {
    val encoded = (
      "02" + // varint32 length = 2
        "0d" + // 1: fixed32
        "05000000" // enough bytes in the source, but not in the current message
      ).decodeHex()
    val reader = ProtoReader32(encoded)

    assertThat(reader.nextLengthDelimited()).isEqualTo(2)
    reader.beginMessage()
    assertThat(reader.nextTag()).isEqualTo(1)
    assertFailure {
      reader.readFixed32()
    }.isInstanceOf<EOFException>()
  }

  // Consider pasting new tests into ProtoReaderTest.kt also.
}

/** Reads a message created by [nestedMessages] and returns its depth. */
private fun ProtoReader32.readNestingDepth(): Int {
  var childDepth = 0
  forEachTag { tag ->
    if (tag == 1) childDepth = readNestingDepth() else skip()
  }
  return childDepth + 1
}

private fun ProtoReader.readNestingDepth(): Int {
  var childDepth = 0
  forEachTag { tag ->
    if (tag == 1) childDepth = readNestingDepth() else skip()
  }
  return childDepth + 1
}
