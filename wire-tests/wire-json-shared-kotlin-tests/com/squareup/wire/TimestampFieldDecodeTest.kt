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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.squareup.wire.internal.ProtocolException
import okio.Buffer
import okio.ByteString.Companion.decodeHex
import org.junit.Test
import squareup.proto3.PizzaDelivery

class TimestampFieldDecodeTest {
  /** Field 7 is `ordered_at`, a message field that holds a Timestamp. */
  private val orderedAtOneSecondPastMax = "3a07088083d1ffaf07"
  private val orderedAtAtMax = "3a0708ff82d1ffaf07"

  @Test fun timestampFieldOutsideRangeThrowsProtocolException() {
    val encoded = orderedAtOneSecondPastMax.decodeHex()
    assertFailure { PizzaDelivery.ADAPTER.decode(encoded) }.isInstanceOf<ProtocolException>()
    assertFailure { PizzaDelivery.ADAPTER.decode(Buffer().write(encoded)) }
      .isInstanceOf<ProtocolException>()
  }

  @Test fun repeatedTimestampFieldOutsideRangeThrowsProtocolException() {
    // The decoder keeps the later occurrence as bytes and merges it into the first one afterwards.
    val inputs = listOf(
      orderedAtOneSecondPastMax + orderedAtAtMax,
      orderedAtAtMax + orderedAtOneSecondPastMax,
      orderedAtOneSecondPastMax + orderedAtOneSecondPastMax,
    )
    for (hex in inputs) {
      val encoded = hex.decodeHex()
      assertFailure { PizzaDelivery.ADAPTER.decode(encoded) }.isInstanceOf<ProtocolException>()
      assertFailure { PizzaDelivery.ADAPTER.decode(Buffer().write(encoded)) }
        .isInstanceOf<ProtocolException>()
    }
  }

  @Test fun repeatedTimestampFieldInsideRangeMergesAndEncodes() {
    val encoded = (orderedAtAtMax + orderedAtAtMax).decodeHex()
    val decoded = PizzaDelivery.ADAPTER.decode(encoded)
    assertThat(decoded.ordered_at!!.getEpochSecond()).isEqualTo(253402300799L)
    assertThat(PizzaDelivery.ADAPTER.decode(PizzaDelivery.ADAPTER.encode(decoded))).isEqualTo(decoded)
  }
}
