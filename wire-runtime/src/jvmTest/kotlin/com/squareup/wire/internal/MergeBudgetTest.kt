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

/** These inputs are too large for the JavaScript and Wasm test runners, so they only run on the JVM. */
class MergeBudgetTest {
  @Test fun manyOccurrencesOfOneFieldAreRejected() {
    val buffer = Buffer()
    val occurrence = lengthDelimited(1, int32(2, 1) + int32(2, 1))
    repeat(20_000) { buffer.write(occurrence) }
    val bytes = buffer.readByteString()
    val adapter = nodeAdapters.first()
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
    val adapter = nodeAdapters.first()
    assertThat(adapter.decode(bytes.toByteArray()).child!!.values.size).isEqualTo(count * valuesPerOccurrence)
  }
}
