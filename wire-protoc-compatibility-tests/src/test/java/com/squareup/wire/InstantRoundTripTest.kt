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
import com.google.protobuf.Timestamp
import org.junit.Test

class InstantRoundTripTest {
  @Test fun `positive values`() {
    val googleMessage = Timestamp.newBuilder()
      .setSeconds(1L)
      .setNanos(200_000_000)
      .build()

    val wireMessage = ofEpochSecond(1L, 200_000_000L)

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.INSTANT.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.INSTANT.decode(googleMessageBytes)).isEqualTo(wireMessage)
    assertThat(ProtoAdapter.INSTANT.encodedSize(wireMessage)).isEqualTo(googleMessageBytes.size)
  }

  @Test fun `zero`() {
    val googleMessage = Timestamp.newBuilder()
      .setSeconds(0L)
      .setNanos(0)
      .build()

    val wireMessage = ofEpochSecond(0L, 0L)

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.INSTANT.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.INSTANT.decode(googleMessageBytes)).isEqualTo(wireMessage)
    assertThat(ProtoAdapter.INSTANT.encodedSize(wireMessage)).isEqualTo(googleMessageBytes.size)
  }

  @Test fun `negative near zero`() {
    val googleMessage = Timestamp.newBuilder()
      .setSeconds(-1L)
      .setNanos(800_000_000)
      .build()

    val wireMessage = ofEpochSecond(0L, -200_000_000L)

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.INSTANT.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.INSTANT.decode(googleMessageBytes)).isEqualTo(wireMessage)
    assertThat(ProtoAdapter.INSTANT.encodedSize(wireMessage)).isEqualTo(googleMessageBytes.size)
  }

  @Test fun `negative values`() {
    val googleMessage = Timestamp.newBuilder()
      .setSeconds(-2L)
      .setNanos(800_000_000)
      .build()

    val wireMessage = ofEpochSecond(-1L, -200_000_000L)

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.INSTANT.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.INSTANT.decode(googleMessageBytes)).isEqualTo(wireMessage)
    assertThat(ProtoAdapter.INSTANT.encodedSize(wireMessage)).isEqualTo(googleMessageBytes.size)
  }

  @Test fun `decode proto with nanos too high`() {
    val googleMessage = Timestamp.newBuilder()
      .setSeconds(2L)
      .setNanos(2_000_000_000)
      .build()

    val wireMessage = ofEpochSecond(4L, 0L)

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.INSTANT.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun `decode proto with nanos too low`() {
    val googleMessage = Timestamp.newBuilder()
      .setSeconds(2L)
      .setNanos(-1)
      .build()

    val wireMessage = ofEpochSecond(1L, 999_999_999L)

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.INSTANT.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }
}
