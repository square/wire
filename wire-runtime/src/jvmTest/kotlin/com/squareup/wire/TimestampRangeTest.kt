/*
 * Copyright (C) 2025 Square, Inc.
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
import assertk.assertions.isNotNull
import java.net.ProtocolException
import java.time.DateTimeException
import kotlin.test.Test
import okio.Buffer
import okio.ByteString.Companion.decodeHex

class TimestampRangeTest {
  @Test fun timestampBeyondInstantRangeThrowsProtocolException() {
    // Seconds are Long.MAX_VALUE and Long.MIN_VALUE.
    for (hex in listOf("08ffffffffffffffff7f1000", "08808080808080808080011000")) {
      val encoded = hex.decodeHex()
      for (decode in listOf({ ProtoAdapter.INSTANT.decode(encoded) }, { ProtoAdapter.INSTANT.decode(Buffer().write(encoded)) })) {
        assertFailure { decode() }
          .isInstanceOf<ProtocolException>()
          .transform { it.cause }
          .isNotNull()
          .isInstanceOf<DateTimeException>()
      }
    }
  }

  @Test fun durationWithMaximumSecondsDecodes() {
    val encoded = "08ffffffffffffffff7f1000".decodeHex()
    assertThat(ProtoAdapter.DURATION.decode(encoded).seconds).isEqualTo(Long.MAX_VALUE)
  }
}
