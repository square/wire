/*
 * Copyright (C) 2014 Square, Inc.
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

import com.squareup.wire.protos.kotlin.redacted.NotRedacted
import com.squareup.wire.protos.kotlin.redacted.RedactedChild
import com.squareup.wire.protos.kotlin.redacted.RedactedCycleA
import com.squareup.wire.protos.kotlin.redacted.RedactedCycleB
import com.squareup.wire.protos.kotlin.redacted.RedactedExtension
import com.squareup.wire.protos.kotlin.redacted.RedactedFields
import com.squareup.wire.protos.kotlin.redacted.RedactedRepeated
import com.squareup.wire.protos.kotlin.redacted.RedactedRequired
import com.squareup.wire.protos.kotlin.redacted.buildersonly.NotRedacted as NotRedactedBuildersOnly
import com.squareup.wire.protos.kotlin.redacted.buildersonly.RedactedChild as RedactedChildBuildersOnly
import com.squareup.wire.protos.kotlin.redacted.buildersonly.RedactedCycleA as RedactedCycleABuildersOnly
import com.squareup.wire.protos.kotlin.redacted.buildersonly.RedactedCycleB as RedactedCycleBBuildersOnly
import com.squareup.wire.protos.kotlin.redacted.buildersonly.RedactedExtension as RedactedExtensionBuildersOnly
import com.squareup.wire.protos.kotlin.redacted.buildersonly.RedactedFields as RedactedFieldsBuildersOnly
import com.squareup.wire.protos.kotlin.redacted.buildersonly.RedactedRepeated as RedactedRepeatedBuildersOnly
import com.squareup.wire.protos.kotlin.redacted.buildersonly.RedactedRequired as RedactedRequiredBuildersOnly
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class RedactTest {
  @Test fun string() {
    val redacted = RedactedFields(a = "a", b = "b", c = "c")
    assertEquals("RedactedFields{a=██, b=b, c=c}", redacted.toString())
    val redactedRepeated = RedactedRepeated(
      a = listOf("a", "b"),
      b = listOf(RedactedFields("a", "b", "c", null), RedactedFields("d", "e", "f", null)),
    )
    assertEquals(
      "RedactedRepeated{a=██, b=[RedactedFields{a=██, b=b, c=c}, " +
        "RedactedFields{a=██, b=e, c=f}]}",
      redactedRepeated.toString(),
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
      c = NotRedacted(a = "a", b = "b"),
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
    var message = RedactedCycleA()
    message = message.copy(b = RedactedCycleB(message))
    assertEquals(message, RedactedCycleA.ADAPTER.redact(message))
  }

  @Test fun repeatedField() {
    val message = RedactedRepeated(
      a = listOf("a", "b"),
      b = listOf(RedactedFields("a", "b", "c", null), RedactedFields("d", "e", "f", null)),
    )
    val expected = RedactedRepeated(
      b = listOf(RedactedFields(null, "b", "c", null), RedactedFields(null, "e", "f", null)),
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

  @Test fun string_buildersOnly() {
    val redacted = RedactedFieldsBuildersOnly.Builder().a("a").b("b").c("c").build()
    assertEquals("RedactedFields{a=██, b=b, c=c}", redacted.toString())
    val redactedRepeated = RedactedRepeatedBuildersOnly.Builder()
      .a(listOf("a", "b"))
      .b(listOf(RedactedFieldsBuildersOnly.Builder().a("a").b("b").c("c").extension(null).build(), RedactedFieldsBuildersOnly.Builder().a("d").b("e").c("f").extension(null).build()))
      .build()
    assertEquals(
      "RedactedRepeated{a=██, b=[RedactedFields{a=██, b=b, c=c}, " +
        "RedactedFields{a=██, b=e, c=f}]}",
      redactedRepeated.toString(),
    )
  }

  @Test fun message_buildersOnly() {
    val message = RedactedFieldsBuildersOnly.Builder().a("a").b("b").c("c").build()
    val expected = message.newBuilder().a(null).build()
    assertEquals(expected, RedactedFieldsBuildersOnly.ADAPTER.redact(message))
  }

  @Test fun messageWithNoRedactions_buildersOnly() {
    val message = NotRedactedBuildersOnly.Builder().a("a").b("b").build()
    assertEquals(message, NotRedactedBuildersOnly.ADAPTER.redact(message))
  }

  @Test fun nestedRedactions_buildersOnly() {
    val message = RedactedChildBuildersOnly.Builder()
      .a("a")
      .b(RedactedFieldsBuildersOnly.Builder().a("a").b("b").c("c").build())
      .c(NotRedactedBuildersOnly.Builder().a("a").b("b").build())
      .build()
    val expected = message.newBuilder().b(message.b!!.newBuilder().a(null).build()).build()
    assertEquals(expected, RedactedChildBuildersOnly.ADAPTER.redact(message))
  }

  @Test fun redactedExtensions_buildersOnly() {
    val message = RedactedFieldsBuildersOnly.Builder().extension(RedactedExtensionBuildersOnly.Builder().d("d").e("e").build()).build()
    val expected = RedactedFieldsBuildersOnly.Builder().extension(RedactedExtensionBuildersOnly.Builder().e("e").build()).build()
    assertEquals(expected, RedactedFieldsBuildersOnly.ADAPTER.redact(message))
  }

  @Test fun messageCycle_buildersOnly() {
    var message = RedactedCycleABuildersOnly.Builder().build()
    message = message.newBuilder().b(RedactedCycleBBuildersOnly.Builder().a(message).build()).build()
    assertEquals(message, RedactedCycleABuildersOnly.ADAPTER.redact(message))
  }

  @Test fun repeatedField_buildersOnly() {
    val message = RedactedRepeatedBuildersOnly.Builder()
      .a(listOf("a", "b"))
      .b(listOf(RedactedFieldsBuildersOnly.Builder().a("a").b("b").c("c").extension(null).build(), RedactedFieldsBuildersOnly.Builder().a("d").b("e").c("f").extension(null).build()))
      .build()
    val expected = RedactedRepeatedBuildersOnly.Builder()
      .b(listOf(RedactedFieldsBuildersOnly.Builder().a(null).b("b").c("c").extension(null).build(), RedactedFieldsBuildersOnly.Builder().a(null).b("e").c("f").extension(null).build()))
      .build()
    val actual = RedactedRepeatedBuildersOnly.ADAPTER.redact(message)
    assertEquals(expected, actual)
  }

  @Test fun requiredRedactedFieldThrowsRedacting_buildersOnly() {
    val adapter = RedactedRequiredBuildersOnly.ADAPTER
    try {
      adapter.redact(RedactedRequiredBuildersOnly.Builder().a("a").build())
      fail()
    } catch (e: UnsupportedOperationException) {
      assertEquals("Field 'a' is required and cannot be redacted.", e.message)
    }
  }

  @Test fun requiredRedactedFieldToString_buildersOnly() {
    val adapter = RedactedRequiredBuildersOnly.ADAPTER
    assertEquals("RedactedRequired{a=██}", adapter.toString(RedactedRequiredBuildersOnly.Builder().a("a").build()))
  }
}
