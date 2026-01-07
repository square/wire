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
package com.squareup.wire.internal

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.wire.internal.InstantJsonFormatter.fromString
import com.squareup.wire.internal.InstantJsonFormatter.toStringOrNumber
import com.squareup.wire.ofEpochSecond
import org.junit.Test

class InstantJsonFormatterTest {
  @Test fun `instant to string`() {
    assertThat(toStringOrNumber(ofEpochSecond(0L, 250_000_000L)))
      .isEqualTo("1970-01-01T00:00:00.250Z")
    assertThat(toStringOrNumber(ofEpochSecond(0L, 250_000L)))
      .isEqualTo("1970-01-01T00:00:00.000250Z")
    assertThat(toStringOrNumber(ofEpochSecond(0L, 250L)))
      .isEqualTo("1970-01-01T00:00:00.000000250Z")
    assertThat(toStringOrNumber(ofEpochSecond(0L, 1L)))
      .isEqualTo("1970-01-01T00:00:00.000000001Z")
  }

  @Test fun `string to instant`() {
    assertThat(fromString("0001-01-01T00:00:00Z"))
      .isEqualTo(ofEpochSecond(-62_135_596_800L, 0L))
    assertThat(fromString("9999-12-31T23:59:59.999999999Z"))
      .isEqualTo(ofEpochSecond(253_402_300_799, 999_999_999L))
    assertThat(fromString("1970-01-01T00:00:00.250Z"))
      .isEqualTo(ofEpochSecond(0L, 250_000_000L))
    assertThat(fromString("1970-01-01T00:00:00.000250Z"))
      .isEqualTo(ofEpochSecond(0L, 250_000L))
    assertThat(fromString("1970-01-01T00:00:00.000000250Z"))
      .isEqualTo(ofEpochSecond(0L, 250L))
    assertThat(fromString("1970-01-01T00:00:00.000000001Z"))
      .isEqualTo(ofEpochSecond(0L, 1L))
    assertThat(fromString("1970-01-01T01:00:00+02:00"))
      .isEqualTo(ofEpochSecond(-3_600L, 0L))
    assertThat(fromString("1970-01-01T01:00:00-02:00"))
      .isEqualTo(ofEpochSecond(10_800L, 0L))
  }
}
