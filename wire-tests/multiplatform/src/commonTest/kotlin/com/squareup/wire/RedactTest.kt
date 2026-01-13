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

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
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
import kotlin.test.fail

class RedactTest {
  @Test fun string() {
    val redacted = RedactedFields(a = "a", b = "b", c = "c")
    assertThat(redacted.toString()).isEqualTo("RedactedFields{a=██, b=b, c=c}")
    val redactedRepeated = RedactedRepeated(
      a = listOf("a", "b"),
      b = listOf(RedactedFields("a", "b", "c", null), RedactedFields("d", "e", "f", null)),
    )
    assertThat(redactedRepeated.toString()).isEqualTo(
      "RedactedRepeated{a=██, b=[RedactedFields{a=██, b=b, c=c}, " +
        "RedactedFields{a=██, b=e, c=f}]}",
    )
  }

  @Test fun message() {
    val message = RedactedFields(a = "a", b = "b", c = "c")
    val expected = message.copy(a = null)
    assertThat(RedactedFields.ADAPTER.redact(message)).isEqualTo(expected)
  }

  @Test fun messageWithNoRedactions() {
    val message = NotRedacted(a = "a", b = "b")
    assertThat(NotRedacted.ADAPTER.redact(message)).isEqualTo(message)
  }

  @Test fun nestedRedactions() {
    val message = RedactedChild(
      a = "a",
      b = RedactedFields(a = "a", b = "b", c = "c"),
      c = NotRedacted(a = "a", b = "b"),
    )
    val expected = message.copy(b = message.b!!.copy(a = null))
    assertThat(RedactedChild.ADAPTER.redact(message)).isEqualTo(expected)
  }

  @Test fun redactedExtensions() {
    val message = RedactedFields(extension = RedactedExtension(d = "d", e = "e"))
    val expected = RedactedFields(extension = RedactedExtension(e = "e"))
    assertThat(RedactedFields.ADAPTER.redact(message)).isEqualTo(expected)
  }

  @Test fun messageCycle() {
    var message = RedactedCycleA()
    message = message.copy(b = RedactedCycleB(message))
    assertThat(RedactedCycleA.ADAPTER.redact(message)).isEqualTo(message)
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
    assertThat(actual).isEqualTo(expected)
  }

  @Test fun requiredRedactedFieldThrowsRedacting() {
    val adapter = RedactedRequired.ADAPTER
    try {
      adapter.redact(RedactedRequired("a"))
      fail()
    } catch (e: UnsupportedOperationException) {
      assertThat(e).hasMessage("Field 'a' is required and cannot be redacted.")
    }
  }

  @Test fun requiredRedactedFieldToString() {
    val adapter = RedactedRequired.ADAPTER
    assertThat(adapter.toString(RedactedRequired("a"))).isEqualTo("RedactedRequired{a=██}")
  }

  @Test fun string_buildersOnly() {
    val redacted = RedactedFieldsBuildersOnly.Builder().a("a").b("b").c("c").build()
    assertThat(redacted.toString()).isEqualTo("RedactedFields{a=██, b=b, c=c}")
    val redactedRepeated = RedactedRepeatedBuildersOnly.Builder()
      .a(listOf("a", "b"))
      .b(
        listOf(
          RedactedFieldsBuildersOnly.Builder().a("a").b("b").c("c").extension(null).build(),
          RedactedFieldsBuildersOnly.Builder().a("d").b("e").c("f").extension(null).build(),
        ),
      )
      .build()
    assertThat(redactedRepeated.toString()).isEqualTo(
      "RedactedRepeated{a=██, b=[RedactedFields{a=██, b=b, c=c}, " +
        "RedactedFields{a=██, b=e, c=f}]}",
    )
  }

  @Test fun message_buildersOnly() {
    val message = RedactedFieldsBuildersOnly.Builder().a("a").b("b").c("c").build()
    val expected = message.newBuilder().a(null).build()
    assertThat(RedactedFieldsBuildersOnly.ADAPTER.redact(message)).isEqualTo(expected)
  }

  @Test fun messageWithNoRedactions_buildersOnly() {
    val message = NotRedactedBuildersOnly.Builder().a("a").b("b").build()
    assertThat(NotRedactedBuildersOnly.ADAPTER.redact(message)).isEqualTo(message)
  }

  @Test fun nestedRedactions_buildersOnly() {
    val message = RedactedChildBuildersOnly.Builder()
      .a("a")
      .b(RedactedFieldsBuildersOnly.Builder().a("a").b("b").c("c").build())
      .c(NotRedactedBuildersOnly.Builder().a("a").b("b").build())
      .build()
    val expected = message.newBuilder().b(message.b!!.newBuilder().a(null).build()).build()
    assertThat(RedactedChildBuildersOnly.ADAPTER.redact(message)).isEqualTo(expected)
  }

  @Test fun redactedExtensions_buildersOnly() {
    val message = RedactedFieldsBuildersOnly.Builder()
      .extension(RedactedExtensionBuildersOnly.Builder().d("d").e("e").build()).build()
    val expected = RedactedFieldsBuildersOnly.Builder()
      .extension(RedactedExtensionBuildersOnly.Builder().e("e").build()).build()
    assertThat(RedactedFieldsBuildersOnly.ADAPTER.redact(message)).isEqualTo(expected)
  }

  @Test fun messageCycle_buildersOnly() {
    var message = RedactedCycleABuildersOnly.Builder().build()
    message =
      message.newBuilder().b(RedactedCycleBBuildersOnly.Builder().a(message).build()).build()
    assertThat(RedactedCycleABuildersOnly.ADAPTER.redact(message)).isEqualTo(message)
  }

  @Test fun repeatedField_buildersOnly() {
    val message = RedactedRepeatedBuildersOnly.Builder()
      .a(listOf("a", "b"))
      .b(
        listOf(
          RedactedFieldsBuildersOnly.Builder().a("a").b("b").c("c").extension(null).build(),
          RedactedFieldsBuildersOnly.Builder().a("d").b("e").c("f").extension(null).build(),
        ),
      )
      .build()
    val expected = RedactedRepeatedBuildersOnly.Builder()
      .b(
        listOf(
          RedactedFieldsBuildersOnly.Builder().a(null).b("b").c("c").extension(null).build(),
          RedactedFieldsBuildersOnly.Builder().a(null).b("e").c("f").extension(null).build(),
        ),
      )
      .build()
    val actual = RedactedRepeatedBuildersOnly.ADAPTER.redact(message)
    assertThat(actual).isEqualTo(expected)
  }

  @Test fun requiredRedactedFieldThrowsRedacting_buildersOnly() {
    val adapter = RedactedRequiredBuildersOnly.ADAPTER
    try {
      adapter.redact(RedactedRequiredBuildersOnly.Builder().a("a").build())
      fail()
    } catch (e: UnsupportedOperationException) {
      assertThat(e).hasMessage("Field 'a' is required and cannot be redacted.")
    }
  }

  @Test fun requiredRedactedFieldToString_buildersOnly() {
    val adapter = RedactedRequiredBuildersOnly.ADAPTER
    assertThat(
      adapter.toString(
        RedactedRequiredBuildersOnly.Builder().a("a").build(),
      ),
    ).isEqualTo("RedactedRequired{a=██}")
  }
}
