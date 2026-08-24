/*
 * Copyright (C) 2015 Square, Inc.
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
import assertk.assertions.hasMessage
import com.squareup.wire.protos.kotlin.OneOfMessage
import com.squareup.wire.protos.kotlin.OneOfMessageWithMessages
import com.squareup.wire.protos.kotlin.OneOfMessageWithMessages.Inner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class OneOfTest {
  @Test
  fun constructorFailsWhenBothFieldsAreNonNull() {
    try {
      OneOfMessage(foo = 1, bar = "two", baz = null)
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("At most one of foo, bar, baz may be non-null")
    }
  }

  @Test
  fun repeatedOccurrencesOfTheSameOneOfFieldMerge() {
    val bytes = OneOfMessageWithMessages(first = Inner(a = "a")).encode() +
      OneOfMessageWithMessages(first = Inner(b = "b")).encode()

    val decoded = OneOfMessageWithMessages.ADAPTER.decode(bytes)

    assertEquals(OneOfMessageWithMessages(first = Inner(a = "a", b = "b")), decoded)
  }

  @Test
  fun lastOneOfFieldWins() {
    val bytes = OneOfMessageWithMessages(first = Inner(a = "a")).encode() +
      OneOfMessageWithMessages(second = Inner(b = "b")).encode()

    val decoded = OneOfMessageWithMessages.ADAPTER.decode(bytes)

    assertEquals(OneOfMessageWithMessages(second = Inner(b = "b")), decoded)
  }

  @Test
  fun lastOneOfScalarFieldWins() {
    val bytes = OneOfMessageWithMessages(first = Inner(a = "a")).encode() +
      OneOfMessageWithMessages(note = "note").encode()

    val decoded = OneOfMessageWithMessages.ADAPTER.decode(bytes)

    assertEquals(OneOfMessageWithMessages(note = "note"), decoded)
  }

  @Test
  fun oneOfFieldDoesNotMergeAcrossAnOccurrenceOfAnotherField() {
    val bytes = OneOfMessageWithMessages(first = Inner(a = "a")).encode() +
      OneOfMessageWithMessages(second = Inner(a = "x")).encode() +
      OneOfMessageWithMessages(first = Inner(b = "b")).encode()

    val decoded = OneOfMessageWithMessages.ADAPTER.decode(bytes)

    assertEquals(OneOfMessageWithMessages(first = Inner(b = "b")), decoded)
  }
}
