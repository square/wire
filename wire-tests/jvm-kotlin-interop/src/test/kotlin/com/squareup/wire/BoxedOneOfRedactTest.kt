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
import com.squareup.wire.protos.kotlin.boxedredacted.BoxedRedactedOneOf
import com.squareup.wire.protos.kotlin.boxedredacted.BoxedRedactedOneOfBuildersOnly
import com.squareup.wire.protos.kotlin.boxedredacted.Image
import com.squareup.wire.protos.kotlin.boxedredacted.Payload
import com.squareup.wire.protos.kotlin.boxedredacted.State
import okio.ByteString.Companion.decodeHex
import org.junit.Test

class BoxedOneOfRedactTest {
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

  @Test fun nestedMessageIsRedactedBeforeEncoding() {
    val message = BoxedRedactedOneOf(
      variant = OneOf(BoxedRedactedOneOf.VARIANT_PAYLOAD, payload),
      sensitive = OneOf(BoxedRedactedOneOf.SENSITIVE_PUBLIC_TEXT, "public"),
      unknownFields = "a00603".decodeHex(),
    )
    val expected = BoxedRedactedOneOf(
      variant = OneOf(BoxedRedactedOneOf.VARIANT_PAYLOAD, redactedPayload),
      sensitive = OneOf(BoxedRedactedOneOf.SENSITIVE_PUBLIC_TEXT, "public"),
    )

    val redacted = BoxedRedactedOneOf.ADAPTER.redact(message)
    val decoded = BoxedRedactedOneOf.ADAPTER.decode(redacted.encode())
    val decodedImage = decoded.variant!!.getOrNull(BoxedRedactedOneOf.VARIANT_PAYLOAD)!!.images.single()
    assertThat(decodedImage.light_url).isNull()
    assertThat(decodedImage.dark_url).isNull()
    assertThat(redacted).isEqualTo(expected)
    assertThat(decoded).isEqualTo(expected)
    assertThat(message.variant).isEqualTo(OneOf(BoxedRedactedOneOf.VARIANT_PAYLOAD, payload))
  }

  @Test fun scalarEnumAndAbsentBranchesArePreserved() {
    val messages = listOf(
      BoxedRedactedOneOf(variant = OneOf(BoxedRedactedOneOf.VARIANT_TEXT, "text")),
      BoxedRedactedOneOf(variant = OneOf(BoxedRedactedOneOf.VARIANT_STATE, State.DONE)),
      BoxedRedactedOneOf(),
    )
    for (message in messages) {
      assertThat(BoxedRedactedOneOf.ADAPTER.redact(message)).isEqualTo(message)
    }
  }

  @Test fun explicitlyRedactedBranchesAreCleared() {
    val branches = listOf(
      OneOf(BoxedRedactedOneOf.SENSITIVE_SECRET, "secret"),
      OneOf(BoxedRedactedOneOf.SENSITIVE_WHOLLY_REDACTED, payload),
    )
    for (branch in branches) {
      val message = BoxedRedactedOneOf(sensitive = branch)
      val redacted = BoxedRedactedOneOf.ADAPTER.redact(message)
      assertThat(BoxedRedactedOneOf.ADAPTER.decode(redacted.encode())).isEqualTo(BoxedRedactedOneOf())
    }
  }

  @Test fun nestedMessageIsRedactedBeforeEncoding_buildersOnly() {
    val message = BoxedRedactedOneOfBuildersOnly.Builder()
      .variant(OneOf(BoxedRedactedOneOfBuildersOnly.VARIANT_PAYLOAD, payload))
      .sensitive(OneOf(BoxedRedactedOneOfBuildersOnly.SENSITIVE_PUBLIC_TEXT, "public"))
      .addUnknownFields("a00603".decodeHex())
      .build()
    val expected = BoxedRedactedOneOfBuildersOnly.Builder()
      .variant(OneOf(BoxedRedactedOneOfBuildersOnly.VARIANT_PAYLOAD, redactedPayload))
      .sensitive(OneOf(BoxedRedactedOneOfBuildersOnly.SENSITIVE_PUBLIC_TEXT, "public"))
      .build()

    val redacted = BoxedRedactedOneOfBuildersOnly.ADAPTER.redact(message)
    val decoded = BoxedRedactedOneOfBuildersOnly.ADAPTER.decode(redacted.encode())
    val decodedImage = decoded.variant!!.getOrNull(BoxedRedactedOneOfBuildersOnly.VARIANT_PAYLOAD)!!.images.single()
    assertThat(decodedImage.light_url).isNull()
    assertThat(decodedImage.dark_url).isNull()
    assertThat(redacted).isEqualTo(expected)
    assertThat(decoded).isEqualTo(expected)
    assertThat(message.variant).isEqualTo(OneOf(BoxedRedactedOneOfBuildersOnly.VARIANT_PAYLOAD, payload))
  }

  @Test fun explicitlyRedactedBranchesAreCleared_buildersOnly() {
    val branches = listOf(
      OneOf(BoxedRedactedOneOfBuildersOnly.SENSITIVE_SECRET, "secret"),
      OneOf(BoxedRedactedOneOfBuildersOnly.SENSITIVE_WHOLLY_REDACTED, payload),
    )
    for (branch in branches) {
      val message = BoxedRedactedOneOfBuildersOnly.Builder().sensitive(branch).build()
      val redacted = BoxedRedactedOneOfBuildersOnly.ADAPTER.redact(message)
      assertThat(BoxedRedactedOneOfBuildersOnly.ADAPTER.decode(redacted.encode()))
        .isEqualTo(BoxedRedactedOneOfBuildersOnly.Builder().build())
    }
  }
}
