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

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.wire.proto2.kotlin.largefield.LargeFieldMessage as LargeFieldMessageK
import com.squareup.wire.proto2.kotlin.largefield.LargeFieldMessageOuterClass.LargeFieldMessage as LargeFieldMessageP
import org.junit.Test

class LargeFieldNumberInteropTest {

  @Test fun fieldNumberBelowBoundary() {
    val wireMessage = LargeFieldMessageK(
      below_boundary = "works",
    )

    val protocMessage = LargeFieldMessageP.newBuilder()
      .setBelowBoundary("works")
      .build()

    val wireDecodedFromProtoc = LargeFieldMessageK.ADAPTER.decode(protocMessage.toByteArray())
    assertThat(wireDecodedFromProtoc).isEqualTo(wireMessage)

    val protocDecodedFromWire = LargeFieldMessageP.parseFrom(wireMessage.encode())
    assertThat(protocDecodedFromWire).isEqualTo(protocMessage)
  }

  @Test fun fieldNumberAtAndAboveBoundary() {
    val wireMessage = LargeFieldMessageK(
      at_boundary = "fixed",
      max_field = "max",
    )

    val protocMessage = LargeFieldMessageP.newBuilder()
      .setAtBoundary("fixed")
      .setMaxField("max")
      .build()

    val wireDecodedFromProtoc = LargeFieldMessageK.ADAPTER.decode(protocMessage.toByteArray())
    assertThat(wireDecodedFromProtoc).isEqualTo(wireMessage)

    val protocDecodedFromWire = LargeFieldMessageP.parseFrom(wireMessage.encode())
    assertThat(protocDecodedFromWire).isEqualTo(protocMessage)
  }
}
