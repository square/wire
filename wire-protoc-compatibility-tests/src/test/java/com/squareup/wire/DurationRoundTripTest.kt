/*
 * Copyright (C) 2020 Square, Inc.
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
import com.google.protobuf.Duration
import org.junit.Test

class DurationRoundTripTest {
  @Test fun `positive values`() {
    val googleMessage = Duration.newBuilder()
      .setSeconds(1L)
      .setNanos(200_000_000)
      .build()

    val wireMessage = durationOfSeconds(1L, 200_000_000L)

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.DURATION.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.DURATION.decode(googleMessageBytes)).isEqualTo(wireMessage)
    assertThat(ProtoAdapter.DURATION.encodedSize(wireMessage)).isEqualTo(googleMessageBytes.size)
  }

  @Test fun `zero`() {
    val googleMessage = Duration.newBuilder()
      .setSeconds(0L)
      .setNanos(0)
      .build()

    val wireMessage = durationOfSeconds(0L, 0L)

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.DURATION.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.DURATION.decode(googleMessageBytes)).isEqualTo(wireMessage)
    assertThat(ProtoAdapter.DURATION.encodedSize(wireMessage)).isEqualTo(googleMessageBytes.size)
  }

  @Test fun `negative near zero`() {
    val googleMessage = Duration.newBuilder()
      .setSeconds(0L)
      .setNanos(-200_000_000)
      .build()

    val wireMessage = durationOfSeconds(0L, -200_000_000L)

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.DURATION.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.DURATION.decode(googleMessageBytes)).isEqualTo(wireMessage)
    assertThat(ProtoAdapter.DURATION.encodedSize(wireMessage)).isEqualTo(googleMessageBytes.size)
  }

  @Test fun `negative values`() {
    val googleMessage = Duration.newBuilder()
      .setSeconds(-1L)
      .setNanos(-200_000_000)
      .build()

    val wireMessage = durationOfSeconds(-1L, -200_000_000L)

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.DURATION.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.DURATION.decode(googleMessageBytes)).isEqualTo(wireMessage)
    assertThat(ProtoAdapter.DURATION.encodedSize(wireMessage)).isEqualTo(googleMessageBytes.size)
  }
}
