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
import assertk.assertions.isNull
import com.squareup.wire.protos.kotlin.sealedredacted.Image
import com.squareup.wire.protos.kotlin.sealedredacted.Payload
import com.squareup.wire.protos.kotlin.sealedredacted.SealedRedactedOneOf
import com.squareup.wire.protos.kotlin.sealedredacted.SealedRedactedOneOfBuildersOnly
import com.squareup.wire.protos.kotlin.sealedredacted.State
import okio.ByteString.Companion.decodeHex
import org.junit.Test

class SealedOneOfRedactTest {
  private val payload = Payload(
    images = listOf(
      Image(
        light_url = "https://example.invalid/light",
        dark_url = "https://example.invalid/dark",
        caption = "caption",
        unknownFields = "a00601".decodeHex(),
      ),
    ),
    label = "label",
    unknownFields = "a00602".decodeHex(),
  )

  private val redactedPayload = Payload(images = listOf(Image(caption = "caption")), label = "label")

  @Test fun incompatibleEnumEncodingDoesNotClearPreviousChoice() {
    val bytes = "1201611a0101".decodeHex()
    val message = SealedRedactedOneOf.ADAPTER.decode(bytes)
    assertThat(message.variant).isEqualTo(SealedRedactedOneOf.Variant.Text("a"))
    assertThat(message.unknownFields).isEqualTo("1a0101".decodeHex())

    val buildersOnly = SealedRedactedOneOfBuildersOnly.ADAPTER.decode(bytes)
    assertThat(buildersOnly.variant).isEqualTo(SealedRedactedOneOfBuildersOnly.Variant.Text("a"))
    assertThat(buildersOnly.unknownFields).isEqualTo("1a0101".decodeHex())
  }

  @Test fun nestedMessageIsRedactedBeforeEncoding() {
    val message = SealedRedactedOneOf(
      variant = SealedRedactedOneOf.Variant.Payload(payload),
      sensitive = SealedRedactedOneOf.Sensitive.PublicText("public"),
      unknownFields = "a00603".decodeHex(),
    )
    val expected = SealedRedactedOneOf(
      variant = SealedRedactedOneOf.Variant.Payload(redactedPayload),
      sensitive = SealedRedactedOneOf.Sensitive.PublicText("public"),
    )

    assertThat(Payload.ADAPTER.redact(payload)).isEqualTo(redactedPayload)
    val redacted = SealedRedactedOneOf.ADAPTER.redact(message)
    val decoded = SealedRedactedOneOf.ADAPTER.decode(redacted.encode())
    val decodedImage = (decoded.variant as SealedRedactedOneOf.Variant.Payload).value.images.single()
    assertThat(decodedImage.light_url).isNull()
    assertThat(decodedImage.dark_url).isNull()
    assertThat(redacted).isEqualTo(expected)
    assertThat(decoded).isEqualTo(expected)
    assertThat(message.variant).isEqualTo(SealedRedactedOneOf.Variant.Payload(payload))
  }

  @Test fun scalarEnumAndAbsentBranchesArePreserved() {
    val messages = listOf(
      SealedRedactedOneOf(variant = SealedRedactedOneOf.Variant.Text("text")),
      SealedRedactedOneOf(variant = SealedRedactedOneOf.Variant.State(State.DONE)),
      SealedRedactedOneOf(),
    )
    for (message in messages) {
      assertThat(SealedRedactedOneOf.ADAPTER.redact(message)).isEqualTo(message)
    }
  }

  @Test fun explicitlyRedactedBranchesAreCleared() {
    val branches = listOf(
      SealedRedactedOneOf.Sensitive.Secret("secret"),
      SealedRedactedOneOf.Sensitive.WhollyRedacted(payload),
    )
    for (branch in branches) {
      val message = SealedRedactedOneOf(sensitive = branch)
      val redacted = SealedRedactedOneOf.ADAPTER.redact(message)
      assertThat(SealedRedactedOneOf.ADAPTER.decode(redacted.encode())).isEqualTo(SealedRedactedOneOf())
    }
  }

  @Test fun nestedMessageIsRedactedBeforeEncoding_buildersOnly() {
    val message = SealedRedactedOneOfBuildersOnly.Builder()
      .variant(SealedRedactedOneOfBuildersOnly.Variant.Payload(payload))
      .sensitive(SealedRedactedOneOfBuildersOnly.Sensitive.PublicText("public"))
      .addUnknownFields("a00603".decodeHex())
      .build()
    val expected = SealedRedactedOneOfBuildersOnly.Builder()
      .variant(SealedRedactedOneOfBuildersOnly.Variant.Payload(redactedPayload))
      .sensitive(SealedRedactedOneOfBuildersOnly.Sensitive.PublicText("public"))
      .build()

    val redacted = SealedRedactedOneOfBuildersOnly.ADAPTER.redact(message)
    val decoded = SealedRedactedOneOfBuildersOnly.ADAPTER.decode(redacted.encode())
    val decodedImage = (decoded.variant as SealedRedactedOneOfBuildersOnly.Variant.Payload).value.images.single()
    assertThat(decodedImage.light_url).isNull()
    assertThat(decodedImage.dark_url).isNull()
    assertThat(redacted).isEqualTo(expected)
    assertThat(decoded).isEqualTo(expected)
    assertThat(message.variant).isEqualTo(SealedRedactedOneOfBuildersOnly.Variant.Payload(payload))
  }

  @Test fun scalarEnumAndAbsentBranchesArePreserved_buildersOnly() {
    val branches = listOf(
      SealedRedactedOneOfBuildersOnly.Variant.Text("text"),
      SealedRedactedOneOfBuildersOnly.Variant.State(State.DONE),
      null,
    )
    for (branch in branches) {
      val message = SealedRedactedOneOfBuildersOnly.Builder().variant(branch).build()
      assertThat(SealedRedactedOneOfBuildersOnly.ADAPTER.redact(message)).isEqualTo(message)
    }
  }

  @Test fun explicitlyRedactedBranchesAreCleared_buildersOnly() {
    val branches = listOf(
      SealedRedactedOneOfBuildersOnly.Sensitive.Secret("secret"),
      SealedRedactedOneOfBuildersOnly.Sensitive.WhollyRedacted(payload),
    )
    for (branch in branches) {
      val message = SealedRedactedOneOfBuildersOnly.Builder().sensitive(branch).build()
      val redacted = SealedRedactedOneOfBuildersOnly.ADAPTER.redact(message)
      assertThat(SealedRedactedOneOfBuildersOnly.ADAPTER.decode(redacted.encode()))
        .isEqualTo(SealedRedactedOneOfBuildersOnly.Builder().build())
    }
  }
}
