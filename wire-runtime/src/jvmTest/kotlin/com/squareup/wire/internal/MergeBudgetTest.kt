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
