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
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import squareup.proto3.WellKnownRedact

/**
 * Duration, timestamp, empty, field mask, and wrapper values have nothing to clear, so redact
 * keeps them. The reflective adapter must match that. The Java module uses compact adapters, which
 * are reflective, so only the Kotlin module compares with generated redact code.
 */
class RuntimeMessageAdapterWellKnownRedactTest {
  private val reflective: ProtoAdapter<WellKnownRedact> =
    ProtoAdapter.newMessageAdapter(WellKnownRedact::class.java)

  private val duration: Duration = Duration.ofSeconds(5, 1)
  private val instant: Instant = Instant.ofEpochSecond(10, 2)
  private val mask = FieldMask(listOf("a.b", "c"))

  /** Checks that redacting keeps [message], reflectively and with the generated adapter. */
  private fun assertRedactKeeps(message: WellKnownRedact) {
    assertThat(reflective.redact(message)).isEqualTo(message)
    assertThat(WellKnownRedact.ADAPTER.redact(message)).isEqualTo(message)
  }

  @Test fun singularFields() {
    assertRedactKeeps(
      WellKnownRedact.Builder()
        .duration(duration)
        .timestamp(instant)
        .empty(Unit)
        .mask(mask)
        .string_value("secret")
        .int32_value(3)
        .build(),
    )
  }

  @Test fun repeatedFields() {
    assertRedactKeeps(
      WellKnownRedact.Builder()
        .rep_duration(listOf(duration, Duration.ZERO))
        .rep_timestamp(listOf(instant, Instant.EPOCH))
        .rep_mask(listOf(mask))
        .rep_string_value(listOf("secret", "other"))
        .build(),
    )
  }

  @Test fun mapValues() {
    assertRedactKeeps(
      WellKnownRedact.Builder()
        .map_int32_duration(mapOf(1 to duration))
        .map_int32_mask(mapOf(1 to mask))
        .map_int32_string_value(mapOf(1 to "secret"))
        .build(),
    )
  }

  @Test fun oneOfMembers() {
    val builders = listOf(
      WellKnownRedact.Builder().oneof_duration(duration),
      WellKnownRedact.Builder().oneof_timestamp(instant),
      WellKnownRedact.Builder().oneof_empty(Unit),
      WellKnownRedact.Builder().oneof_mask(mask),
      WellKnownRedact.Builder().oneof_string_value("secret"),
    )
    for (builder in builders) {
      assertRedactKeeps(builder.build())
    }
  }

  @Test fun absentFields() {
    assertRedactKeeps(WellKnownRedact.Builder().build())
  }
}
