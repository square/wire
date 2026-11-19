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
import com.squareup.wire.protos.kotlin.boxedredacted.BoxedRedactedOneOf
import com.squareup.wire.protos.kotlin.boxedredacted.BoxedRedactedOneOfBuildersOnly
import com.squareup.wire.protos.kotlin.boxedredacted.Image as BoxedImage
import com.squareup.wire.protos.kotlin.boxedredacted.Payload as BoxedPayload
import com.squareup.wire.protos.kotlin.sealedredacted.Image as SealedImage
import com.squareup.wire.protos.kotlin.sealedredacted.Payload as SealedPayload
import com.squareup.wire.protos.kotlin.sealedredacted.SealedRedactedOneOf
import com.squareup.wire.protos.kotlin.sealedredacted.SealedRedactedOneOfBuildersOnly
import okio.Buffer
import okio.ByteString
import org.junit.Test

/**
 * A repeated occurrence of a message member of a boxed or sealed oneof merges into the first one.
 * Another member replaces it, so the last member on the wire wins.
 */
class OneOfMergeTest {
  private fun concat(vararg parts: ByteString): ByteString {
    val buffer = Buffer()
    parts.forEach { buffer.write(it) }
    return buffer.readByteString()
  }

  private fun <T : Any> assertDecodes(expected: T, bytes: ByteString, vararg adapters: ProtoAdapter<T>) {
    for (adapter in adapters) {
      assertThat(adapter.decode(bytes)).isEqualTo(expected)
      assertThat(adapter.decode(bytes.toByteArray())).isEqualTo(expected)
      assertThat(adapter.decode(Buffer().write(bytes))).isEqualTo(expected)
    }
  }

  private fun boxedPayload(vararg captions: String) = BoxedPayload(images = captions.map { BoxedImage(caption = it) })

  private fun sealedPayload(vararg captions: String) = SealedPayload(images = captions.map { SealedImage(caption = it) })

  @Test fun boxedMergesRepeatedMemberAndKeepsLastMember() {
    val adapter = BoxedRedactedOneOf.ADAPTER
    fun payload(vararg captions: String) = adapter.encodeByteString(
      BoxedRedactedOneOf(variant = OneOf(BoxedRedactedOneOf.VARIANT_PAYLOAD, boxedPayload(*captions))),
    )
    val text = adapter.encodeByteString(BoxedRedactedOneOf(variant = OneOf(BoxedRedactedOneOf.VARIANT_TEXT, "t")))
    val reflective = ProtoAdapter.newMessageAdapter(BoxedRedactedOneOf::class.java)

    assertDecodes(
      BoxedRedactedOneOf(variant = OneOf(BoxedRedactedOneOf.VARIANT_PAYLOAD, boxedPayload("1", "2"))),
      concat(payload("1"), payload("2")),
      adapter,
      reflective,
    )
    assertDecodes(
      BoxedRedactedOneOf(variant = OneOf(BoxedRedactedOneOf.VARIANT_PAYLOAD, boxedPayload("3", "4"))),
      concat(payload("1"), payload("2"), text, payload("3"), payload("4")),
      adapter,
      reflective,
    )
    assertDecodes(
      BoxedRedactedOneOf(variant = OneOf(BoxedRedactedOneOf.VARIANT_TEXT, "t")),
      concat(payload("1"), payload("2"), text),
      adapter,
      reflective,
    )
  }

  @Test fun boxedBuildersOnlyMergesRepeatedMemberAndKeepsLastMember() {
    val adapter = BoxedRedactedOneOfBuildersOnly.ADAPTER
    fun message(variant: OneOf<*, *>) = BoxedRedactedOneOfBuildersOnly.Builder().variant(variant as OneOf<BoxedRedactedOneOfBuildersOnly.Variant<*>, *>).build()
    fun payload(vararg captions: String) = adapter.encodeByteString(
      message(OneOf(BoxedRedactedOneOfBuildersOnly.VARIANT_PAYLOAD, boxedPayload(*captions))),
    )
    val text = adapter.encodeByteString(message(OneOf(BoxedRedactedOneOfBuildersOnly.VARIANT_TEXT, "t")))
    val reflective = ProtoAdapter.newMessageAdapter(BoxedRedactedOneOfBuildersOnly::class.java)

    assertDecodes(
      message(OneOf(BoxedRedactedOneOfBuildersOnly.VARIANT_PAYLOAD, boxedPayload("3", "4"))),
      concat(payload("1"), payload("2"), text, payload("3"), payload("4")),
      adapter,
      reflective,
    )
    assertDecodes(
      message(OneOf(BoxedRedactedOneOfBuildersOnly.VARIANT_PAYLOAD, boxedPayload("1", "2"))),
      concat(payload("1"), payload("2")),
      adapter,
      reflective,
    )
  }

  @Test fun sealedMergesRepeatedMemberAndKeepsLastMember() {
    val adapter = SealedRedactedOneOf.ADAPTER
    fun payload(vararg captions: String) = adapter.encodeByteString(
      SealedRedactedOneOf(variant = SealedRedactedOneOf.Variant.Payload(sealedPayload(*captions))),
    )
    val text = adapter.encodeByteString(SealedRedactedOneOf(variant = SealedRedactedOneOf.Variant.Text("t")))
    val reflective = ProtoAdapter.newMessageAdapter(SealedRedactedOneOf::class.java)

    assertDecodes(
      SealedRedactedOneOf(variant = SealedRedactedOneOf.Variant.Payload(sealedPayload("3", "4"))),
      concat(payload("1"), payload("2"), text, payload("3"), payload("4")),
      adapter,
      reflective,
    )
    assertDecodes(
      SealedRedactedOneOf(variant = SealedRedactedOneOf.Variant.Payload(sealedPayload("1", "2"))),
      concat(payload("1"), payload("2")),
      adapter,
      reflective,
    )
    assertDecodes(
      SealedRedactedOneOf(variant = SealedRedactedOneOf.Variant.Text("t")),
      concat(payload("1"), payload("2"), text),
      adapter,
      reflective,
    )
  }

  @Test fun sealedBuildersOnlyMergesRepeatedMemberAndKeepsLastMember() {
    val adapter = SealedRedactedOneOfBuildersOnly.ADAPTER
    fun message(variant: SealedRedactedOneOfBuildersOnly.Variant) = SealedRedactedOneOfBuildersOnly.Builder().variant(variant).build()
    fun payload(vararg captions: String) = adapter.encodeByteString(
      message(SealedRedactedOneOfBuildersOnly.Variant.Payload(sealedPayload(*captions))),
    )
    val text = adapter.encodeByteString(message(SealedRedactedOneOfBuildersOnly.Variant.Text("t")))
    val reflective = ProtoAdapter.newMessageAdapter(SealedRedactedOneOfBuildersOnly::class.java)

    assertDecodes(
      message(SealedRedactedOneOfBuildersOnly.Variant.Payload(sealedPayload("3", "4"))),
      concat(payload("1"), payload("2"), text, payload("3"), payload("4")),
      adapter,
      reflective,
    )
    assertDecodes(
      message(SealedRedactedOneOfBuildersOnly.Variant.Payload(sealedPayload("1", "2"))),
      concat(payload("1"), payload("2")),
      adapter,
      reflective,
    )
  }
}
