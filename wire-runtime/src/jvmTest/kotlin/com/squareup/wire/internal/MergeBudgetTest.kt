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
package com.squareup.wire.internal

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.squareup.wire.ByteArrayProtoReader32
import com.squareup.wire.MERGE_ALLOWANCE_BYTES
import com.squareup.wire.ProtoReader
import com.squareup.wire.forEachTag
import com.squareup.wire.mergeContext
import kotlin.test.Test
import okio.Buffer
import okio.ByteString

/** These inputs are too large for the JavaScript and Wasm test runners, so they only run on the JVM. */
class MergeBudgetTest {
  @Test fun manyOccurrencesOfOneFieldAreRejected() {
    val buffer = Buffer()
    val occurrence = lengthDelimited(1, int32(2, 1) + int32(2, 1))
    repeat(20_000) { buffer.write(occurrence) }
    val bytes = buffer.readByteString()
    val adapter = nodeAdapters.first { !it.defersMerges }
    assertFailure { adapter.decode(bytes.toByteArray()) }.isInstanceOf<ProtocolException>()
  }

  @Test fun manyOccurrencesOfOneFieldAreMergedOnce() {
    val buffer = Buffer()
    val occurrence = lengthDelimited(1, int32(2, 1) + int32(2, 1))
    repeat(200_000) { buffer.write(occurrence) }
    val bytes = buffer.readByteString()
    for (adapter in nodeAdapters.filter { it.defersMerges }) {
      assertThat(adapter.decode(bytes.toByteArray()).child!!.values.size).isEqualTo(400_000)
      assertThat(adapter.decode(Buffer().write(bytes)).child!!.values.size).isEqualTo(400_000)
    }
  }

  /**
   * Every level holds the next level followed by a short chain, so merging at each level decodes
   * the levels below it again. Merging each field once is not enough to make this linear, so the
   * merges are limited too.
   */
  @Test fun mergesAtEveryLevelAreRejected() {
    val levels = 90
    val values = Buffer()
    // Large values take 6 bytes each on the wire and cost less to decode per byte.
    repeat(35_000) { values.write(int32(2, Int.MAX_VALUE)) }
    var current = values.readByteString()
    for (level in levels - 1 downTo 0) {
      var stub = ByteString.EMPTY
      repeat(levels - level - 1) { stub = lengthDelimited(1, stub) }
      current = lengthDelimited(1, current) + lengthDelimited(1, stub)
    }
    val bytes = current
    val adapter = nodeAdapters.first { it.defersMerges }
    assertFailure { adapter.decode(bytes.toByteArray()) }.isInstanceOf<ProtocolException>()
  }

  /** Legitimate inputs that repeat a message field are far below the allowance. */
  @Test fun threeHundredOccurrencesOfTenKilobytesAreMerged() {
    assertMergesOccurrences(count = 300, valuesPerOccurrence = 5_000)
  }

  @Test fun oneThousandOccurrencesOfOneKilobyteAreMerged() {
    assertMergesOccurrences(count = 1_000, valuesPerOccurrence = 500)
  }

  /**
   * A deferred merge is charged against the bytes consumed when it finishes, not when its first
   * occurrence was kept. Here the budget has 1 MiB left of the fixed allowance when the first
   * occurrence arrives, and the 100 KB that follow add 6 MB to the allowance.
   */
  @Test fun deferredMergeUsesTheBytesConsumedWhenItFinishes() {
    val bytes = twoOccurrencesFollowedBy(padding = 100_000)
    assertThat(mergeAfterSpending(bytes, readsByteArray = false)).isEqualTo(listOf(1, 2))
    assertThat(mergeAfterSpending(bytes, readsByteArray = true)).isEqualTo(listOf(1, 2))
  }

  @Test fun deferredMergeIsStillLimitedWhenItFinishes() {
    val bytes = twoOccurrencesFollowedBy(padding = 0)
    assertFailure { mergeAfterSpending(bytes, readsByteArray = false) }.isInstanceOf<ProtocolException>()
    assertFailure { mergeAfterSpending(bytes, readsByteArray = true) }.isInstanceOf<ProtocolException>()
  }

  private fun twoOccurrencesFollowedBy(padding: Int): ByteString {
    val first = lengthDelimited(1, int32(2, 1))
    val second = lengthDelimited(1, int32(2, 2))
    val unknown = if (padding > 0) lengthDelimited(3, ByteString.of(*ByteArray(padding))) else ByteString.EMPTY
    return first + second + unknown
  }

  /** Decodes like generated code, and spends all but 1 MiB of the fixed allowance before the merge. */
  private fun mergeAfterSpending(bytes: ByteString, readsByteArray: Boolean): List<Int> {
    val adapter = NodeAdapter(readsProtoReader32 = readsByteArray, defersMerges = true)
    val spent = MERGE_ALLOWANCE_BYTES + 1024 * 1024
    var child: Node? = null
    if (readsByteArray) {
      val reader = ByteArrayProtoReader32(bytes.toByteArray())
      reader.forEachTag { tag ->
        when (tag) {
          1 -> child = decodeMessageOrDefer(adapter, reader, 1, child)
          else -> reader.readUnknownField(tag)
        }
      }
      assertThat(hasMerges(reader)).isTrue()
      reader.mergeContext().budget.charge(spent, Long.MAX_VALUE)
      child = finishMerge(reader, 1, adapter, child)
    } else {
      val reader = ProtoReader(Buffer().write(bytes))
      reader.forEachTag { tag ->
        when (tag) {
          1 -> child = decodeMessageOrDefer(adapter, reader, 1, child)
          else -> reader.readUnknownField(tag)
        }
      }
      assertThat(hasMerges(reader)).isTrue()
      reader.mergeContext().budget.charge(spent, Long.MAX_VALUE)
      child = finishMerge(reader, 1, adapter, child)
    }
    return child!!.values
  }

  private fun assertMergesOccurrences(count: Int, valuesPerOccurrence: Int) {
    // Each value takes 2 bytes on the wire, so an occurrence of 5,000 values is 10 KB.
    val values = Buffer()
    repeat(valuesPerOccurrence) { values.write(int32(2, 1)) }
    val occurrence = lengthDelimited(1, values.readByteString())
    val buffer = Buffer()
    repeat(count) { buffer.write(occurrence) }
    val bytes = buffer.readByteString()
    val adapter = nodeAdapters.first { !it.defersMerges }
    assertThat(adapter.decode(bytes.toByteArray()).child!!.values.size).isEqualTo(count * valuesPerOccurrence)
  }
}
