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
import com.squareup.wire.protos.kotlin.flatredacted.FlatRedactedOneOfBuildersOnly
import com.squareup.wire.protos.kotlin.flatredacted.FlatRedactedOneOfInterop
import com.squareup.wire.protos.kotlin.flatredacted.FlatRedactedOneOfInteropBuildersOnly
import com.squareup.wire.protos.kotlin.flatredacted.FlatRedactedOneOfPlain
import com.squareup.wire.protos.kotlin.flatredacted.FlatRedactedSingleOneOfBuildersOnly
import com.squareup.wire.protos.kotlin.flatredacted.Payload
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import org.junit.Test

/**
 * The generated `redact` of a message with a flat oneof must change only the member that is set.
 * The setters of a `Builder` clear every other member of the oneof, so a style that uses a
 * `Builder` must not call a setter for a member that is not set.
 */
class FlatOneOfRedactTest {
  private val payload = Payload(label = "label", token = "token", unknownFields = "a00601".decodeHex())
  private val redactedPayload = Payload(label = "label")

  /** The members of the `variant` oneof, read from a message of any style. */
  private data class Variant(val payload: Payload?, val text: String?, val secret: String?)

  private class Style<M : Message<M, *>>(
    val name: String,
    val adapter: ProtoAdapter<M>,
    val reflective: ProtoAdapter<M>,
    val create: (Variant, ByteString) -> M,
    val read: (M) -> Variant,
  ) {
    fun redactAll(variant: Variant, unknownFields: ByteString): Triple<Variant, Variant, Variant> {
      val message = create(variant, unknownFields)
      return Triple(
        read(adapter.redact(message)),
        read(reflective.redact(message)),
        read(adapter.decode(adapter.redact(message).encode())),
      )
    }

    fun unknownFieldsSizeAfterRedact(unknownFields: ByteString): Int {
      val message = create(Variant(null, "text", null), unknownFields)
      return adapter.redact(message).unknownFields.size
    }
  }

  private val styles: List<Style<*>> = listOf(
    Style(
      name = "plain",
      adapter = FlatRedactedOneOfPlain.ADAPTER,
      reflective = ProtoAdapter.newMessageAdapter(FlatRedactedOneOfPlain::class.java),
      create = { v, u -> FlatRedactedOneOfPlain(v.payload, v.text, v.secret, u) },
      read = { Variant(it.payload, it.text, it.secret) },
    ),
    Style(
      name = "interop",
      adapter = FlatRedactedOneOfInterop.ADAPTER,
      reflective = ProtoAdapter.newMessageAdapter(FlatRedactedOneOfInterop::class.java),
      create = { v, u -> FlatRedactedOneOfInterop(v.payload, v.text, v.secret, u) },
      read = { Variant(it.payload, it.text, it.secret) },
    ),
    Style(
      name = "builders only",
      adapter = FlatRedactedOneOfBuildersOnly.ADAPTER,
      reflective = ProtoAdapter.newMessageAdapter(FlatRedactedOneOfBuildersOnly::class.java),
      create = { v, u ->
        FlatRedactedOneOfBuildersOnly.Builder().apply {
          // Setters clear the other members, so only call the one that is set.
          v.payload?.let(::payload)
          v.text?.let(::text)
          v.secret?.let(::secret)
          addUnknownFields(u)
        }.build()
      },
      read = { Variant(it.payload, it.text, it.secret) },
    ),
    Style(
      name = "interop and builders only",
      adapter = FlatRedactedOneOfInteropBuildersOnly.ADAPTER,
      reflective = ProtoAdapter.newMessageAdapter(FlatRedactedOneOfInteropBuildersOnly::class.java),
      create = { v, u ->
        FlatRedactedOneOfInteropBuildersOnly.Builder().apply {
          // Setters clear the other members, so only call the one that is set.
          v.payload?.let(::payload)
          v.text?.let(::text)
          v.secret?.let(::secret)
          addUnknownFields(u)
        }.build()
      },
      read = { Variant(it.payload, it.text, it.secret) },
    ),
  )

  private fun assertRedacts(variant: Variant, expected: Variant) {
    for (style in styles) {
      val (generated, reflective, decoded) = style.redactAll(variant, "a00603".decodeHex())
      assertThat(generated, name = "${style.name}: generated").isEqualTo(expected)
      assertThat(reflective, name = "${style.name}: reflective").isEqualTo(expected)
      assertThat(decoded, name = "${style.name}: decoded").isEqualTo(expected)
    }
  }

  @Test fun scalarMemberIsKeptWhenAnotherMemberNeedsRedaction() {
    assertRedacts(Variant(null, "text", null), Variant(null, "text", null))
  }

  @Test fun messageMemberIsRedactedAndKept() {
    assertRedacts(Variant(payload, null, null), Variant(redactedPayload, null, null))
  }

  @Test fun redactedScalarMemberIsCleared() {
    assertRedacts(Variant(null, null, "secret"), Variant(null, null, null))
  }

  @Test fun unsetOneOfStaysUnset() {
    assertRedacts(Variant(null, null, null), Variant(null, null, null))
  }

  @Test fun unknownFieldsAreCleared() {
    for (style in styles) {
      assertThat(style.unknownFieldsSizeAfterRedact("a00603".decodeHex()), name = style.name).isEqualTo(0)
    }
  }

  @Test fun singleMemberOneOf() {
    val adapter = FlatRedactedSingleOneOfBuildersOnly.ADAPTER
    val reflective = ProtoAdapter.newMessageAdapter(FlatRedactedSingleOneOfBuildersOnly::class.java)
    val message = FlatRedactedSingleOneOfBuildersOnly.Builder().payload(payload).build()
    assertThat(adapter.redact(message).payload).isEqualTo(redactedPayload)
    assertThat(reflective.redact(message).payload).isEqualTo(redactedPayload)
    assertThat(adapter.redact(FlatRedactedSingleOneOfBuildersOnly.Builder().build()).payload).isNull()
  }
}
