/*
 * Copyright (C) 2019 Square, Inc.
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
import kotlin.test.Test

class ProtoAdapterTest {
  @Test fun repeatedRepeatedProtoAdapterForbidden() {
    assertFailure {
      ProtoAdapter.BOOL.asRepeated().asRepeated()
    }.isInstanceOf<UnsupportedOperationException>()
  }

  @Test fun packedPackedProtoAdapterForbidden() {
    // Unable to pack a length-delimited type.
    assertFailure {
      ProtoAdapter.BOOL.asPacked().asPacked()
    }.isInstanceOf<IllegalArgumentException>()
  }

  @Test fun repeatedPackedProtoAdapterForbidden() {
    assertFailure {
      ProtoAdapter.BOOL.asRepeated().asPacked()
    }.isInstanceOf<UnsupportedOperationException>()
  }

  @Test fun packedRepeatedProtoAdapterForbidden() {
    assertFailure {
      ProtoAdapter.BOOL.asPacked().asRepeated()
    }.isInstanceOf<UnsupportedOperationException>()
  }

  @Test fun instantEncodeValidMinBoundary() {
    // 0001-01-01T00:00:00Z.
    val instant = ofEpochSecond(-62135596800L, 0L)
    val bytes = ProtoAdapter.INSTANT.encode(instant)
    assertThat(ProtoAdapter.INSTANT.decode(bytes).getEpochSecond()).isEqualTo(-62135596800L)
  }

  @Test fun instantEncodeValidMaxBoundary() {
    // 9999-12-31T23:59:59Z.
    val instant = ofEpochSecond(253402300799L, 999_999_999L)
    val bytes = ProtoAdapter.INSTANT.encode(instant)
    val decoded = ProtoAdapter.INSTANT.decode(bytes)
    assertThat(decoded.getEpochSecond()).isEqualTo(253402300799L)
    assertThat(decoded.getNano()).isEqualTo(999_999_999)
  }

  @Test fun instantEncodeRejectsSecondsBelowMin() {
    // 0001-01-01T00:00:00Z - 1 second.
    val instant = ofEpochSecond(-62135596801L, 0L)
    assertFailure {
      ProtoAdapter.INSTANT.encode(instant)
    }.isInstanceOf<IllegalArgumentException>()
  }

  @Test fun instantEncodeRejectsSecondsAboveMax() {
    // 9999-12-31T23:59:59Z + 1 second.
    val instant = ofEpochSecond(253402300800L, 0L)
    assertFailure {
      ProtoAdapter.INSTANT.encode(instant)
    }.isInstanceOf<IllegalArgumentException>()
  }

  @Test fun instantEncodedSizeRejectsOutOfRange() {
    // 0001-01-01T00:00:00Z - 1 second.
    val instant = ofEpochSecond(-62135596801L, 0L)
    assertFailure {
      ProtoAdapter.INSTANT.encodedSize(instant)
    }.isInstanceOf<IllegalArgumentException>()
  }
}
