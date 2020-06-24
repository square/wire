/*
 * Copyright 2020 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire

import com.squareup.wire.InstantJsonAdapter.instantToString
import com.squareup.wire.InstantJsonAdapter.stringToInstant
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class InstantJsonAdapterTest {
  @Test fun `instant to string`() {
    assertThat(instantToString(ofEpochSecond(0L, 250_000_000L)))
        .isEqualTo("1970-01-01T00:00:00.250Z")
    assertThat(instantToString(ofEpochSecond(0L, 250_000L)))
        .isEqualTo("1970-01-01T00:00:00.000250Z")
    assertThat(instantToString(ofEpochSecond(0L, 250L)))
        .isEqualTo("1970-01-01T00:00:00.000000250Z")
    assertThat(instantToString(ofEpochSecond(0L, 1L)))
        .isEqualTo("1970-01-01T00:00:00.000000001Z")
  }

  @Test fun `string to instant`() {
    assertThat(stringToInstant("0001-01-01T00:00:00Z"))
        .isEqualTo(ofEpochSecond(-62_135_596_800L, 0L))
    assertThat(stringToInstant("9999-12-31T23:59:59.999999999Z"))
        .isEqualTo(ofEpochSecond(253_402_300_799, 999_999_999L))
    assertThat(stringToInstant("1970-01-01T00:00:00.250Z"))
        .isEqualTo(ofEpochSecond(0L, 250_000_000L))
    assertThat(stringToInstant("1970-01-01T00:00:00.000250Z"))
        .isEqualTo(ofEpochSecond(0L, 250_000L))
    assertThat(stringToInstant("1970-01-01T00:00:00.000000250Z"))
        .isEqualTo(ofEpochSecond(0L, 250L))
    assertThat(stringToInstant("1970-01-01T00:00:00.000000001Z"))
        .isEqualTo(ofEpochSecond(0L, 1L))
    assertThat(stringToInstant("1970-01-01T01:00:00+02:00"))
        .isEqualTo(ofEpochSecond(-3_600L, 0L))
    assertThat(stringToInstant("1970-01-01T01:00:00-02:00"))
        .isEqualTo(ofEpochSecond(10_800L, 0L))
  }
}
