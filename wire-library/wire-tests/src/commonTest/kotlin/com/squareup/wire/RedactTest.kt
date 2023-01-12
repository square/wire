/*
 * Copyright 2014 Square Inc.
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

import com.squareup.wire.protos.kotlin.redacted.NotRedacted
import com.squareup.wire.protos.kotlin.redacted.RedactedChild
import com.squareup.wire.protos.kotlin.redacted.RedactedCycleA
import com.squareup.wire.protos.kotlin.redacted.RedactedExtension
import com.squareup.wire.protos.kotlin.redacted.RedactedFields
import com.squareup.wire.protos.kotlin.redacted.RedactedRepeated
import com.squareup.wire.protos.kotlin.redacted.RedactedRequired
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class RedactTest {
  @Test fun string() {
    val redacted = RedactedFields(a = "a", b = "b", c = "c")
    assertEquals("RedactedFields{a=██, b=b, c=c}", redacted.toString())
    val redactedRepeated = RedactedRepeated(
      a = listOf("a", "b"),
      b = listOf(RedactedFields("a", "b", "c", null), RedactedFields("d", "e", "f", null))
    )
    assertEquals(
      "RedactedRepeated{a=██, b=[RedactedFields{a=██, b=b, c=c}, " +
        "RedactedFields{a=██, b=e, c=f}]}",
      redactedRepeated.toString()
    )
  }

  @Test fun message() {
    val message = RedactedFields(a = "a", b = "b", c = "c")
    val expected = message.copy(a = null)
    assertEquals(expected, RedactedFields.ADAPTER.redact(message))
  }

  @Test fun messageWithNoRedactions() {
    val message = NotRedacted(a = "a", b = "b")
    assertEquals(message, NotRedacted.ADAPTER.redact(message))
  }

  @Test fun nestedRedactions() {
    val message = RedactedChild(
      a = "a",
      b = RedactedFields(a = "a", b = "b", c = "c"),
      c = NotRedacted(a = "a", b = "b")
    )
    val expected = message.copy(b = message.b!!.copy(a = null))
    assertEquals(expected, RedactedChild.ADAPTER.redact(message))
  }

  @Test fun redactedExtensions() {
    val message = RedactedFields(extension = RedactedExtension(d = "d", e = "e"))
    val expected = RedactedFields(extension = RedactedExtension(e = "e"))
    assertEquals(expected, RedactedFields.ADAPTER.redact(message))
  }

  @Test fun messageCycle() {
    val message = RedactedCycleA()
    assertEquals(message, RedactedCycleA.ADAPTER.redact(message))
  }

  @Test fun repeatedField() {
    val message = RedactedRepeated(
      a = listOf("a", "b"),
      b = listOf(RedactedFields("a", "b", "c", null), RedactedFields("d", "e", "f", null))
    )
    val expected = RedactedRepeated(
      b = listOf(RedactedFields(null, "b", "c", null), RedactedFields(null, "e", "f", null))
    )
    val actual = RedactedRepeated.ADAPTER.redact(message)
    assertEquals(expected, actual)
  }

  @Test fun requiredRedactedFieldThrowsRedacting() {
    val adapter = RedactedRequired.ADAPTER
    try {
      adapter.redact(RedactedRequired("a"))
      fail()
    } catch (e: UnsupportedOperationException) {
      assertEquals("Field 'a' is required and cannot be redacted.", e.message)
    }
  }

  @Test fun requiredRedactedFieldToString() {
    val adapter = RedactedRequired.ADAPTER
    assertEquals("RedactedRequired{a=██}", adapter.toString(RedactedRequired("a")))
  }
}
