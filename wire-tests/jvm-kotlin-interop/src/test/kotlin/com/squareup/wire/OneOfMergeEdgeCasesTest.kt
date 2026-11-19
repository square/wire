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
import com.squareup.wire.protos.kotlin.oneofmergeboxed.BoxedCap
import com.squareup.wire.protos.kotlin.oneofmergeboxed.BoxedMerge
import com.squareup.wire.protos.kotlin.oneofmergeboxed.BoxedNames
import com.squareup.wire.protos.kotlin.oneofmergeboxed.Leaf as BoxedLeaf
import com.squareup.wire.protos.kotlin.oneofmergesealed.Leaf as SealedLeaf
import com.squareup.wire.protos.kotlin.oneofmergesealed.SealedCap
import com.squareup.wire.protos.kotlin.oneofmergesealed.SealedMerge
import com.squareup.wire.protos.kotlin.oneofmergesealed.SealedNames
import com.squareup.wire.protos.kotlin.oneofmergesealedbuilders.Leaf as BuildersLeaf
import com.squareup.wire.protos.kotlin.oneofmergesealedbuilders.SealedBuildersOnlyNames
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import org.junit.Test

/** Edge cases of merging repeated message members of boxed and sealed oneofs. */
class OneOfMergeEdgeCasesTest {
  /** A `google.protobuf.Value` member with tag 4 that holds the number 1.0. */
  private val numberValue = "220911000000000000f03f".decodeHex()

  /** A `google.protobuf.Value` member with tag 4 that holds `null_value`. */
  private val nullValue = "22020800".decodeHex()

  private fun concat(vararg parts: ByteString): ByteString {
    val buffer = Buffer()
    parts.forEach { buffer.write(it) }
    return buffer.readByteString()
  }

  private fun <T> encode(adapter: ProtoAdapter<T>, tag: Int, value: T): ByteString {
    val buffer = Buffer()
    adapter.encodeWithTag(ProtoWriter(buffer), tag, value)
    return buffer.readByteString()
  }

  private fun <T : Any> decodeAll(adapter: ProtoAdapter<T>, bytes: ByteString): List<T> = listOf(
    adapter.decode(bytes),
    adapter.decode(bytes.toByteArray()),
    adapter.decode(Buffer().write(bytes)),
  )

  private fun buildersLeaf(vararg values: Int) = BuildersLeaf.Builder().values(values.toList()).build()

  @Test fun boxedValueThatIsNullAfterNumberIsNull() {
    for (message in decodeAll(BoxedMerge.ADAPTER, concat(numberValue, nullValue))) {
      assertThat(message.choice).isEqualTo(OneOf(BoxedMerge.CHOICE_VALUE, null))
      assertThat(message.choice!!.value).isNull()
    }
  }

  @Test fun boxedNumberAfterNullValueIsNumber() {
    for (message in decodeAll(BoxedMerge.ADAPTER, concat(nullValue, numberValue))) {
      assertThat(message.choice).isEqualTo(OneOf(BoxedMerge.CHOICE_VALUE, 1.0))
    }
  }

  @Test fun sealedValueThatIsNullAfterNumberIsNull() {
    for (message in decodeAll(SealedMerge.ADAPTER, concat(numberValue, nullValue))) {
      assertThat(message.choice).isEqualTo(SealedMerge.Choice.Value(null))
    }
  }

  @Test fun sealedNumberAfterNullValueIsNumber() {
    for (message in decodeAll(SealedMerge.ADAPTER, concat(nullValue, numberValue))) {
      assertThat(message.choice).isEqualTo(SealedMerge.Choice.Value(1.0))
    }
  }

  @Test fun boxedValueFollowedByMessageMemberKeepsTheMessageMember() {
    val a = encode(BoxedLeaf.ADAPTER, 1, BoxedLeaf(values = listOf(7)))
    for (message in decodeAll(BoxedMerge.ADAPTER, concat(numberValue, nullValue, a))) {
      assertThat(message.choice).isEqualTo(OneOf(BoxedMerge.CHOICE_A, BoxedLeaf(values = listOf(7))))
    }
  }

  @Test fun sealedValueFollowedByMessageMemberKeepsTheMessageMember() {
    val a = encode(SealedLeaf.ADAPTER, 1, SealedLeaf(values = listOf(7)))
    for (message in decodeAll(SealedMerge.ADAPTER, concat(numberValue, nullValue, a))) {
      assertThat(message.choice).isEqualTo(SealedMerge.Choice.A(SealedLeaf(values = listOf(7))))
    }
  }

  @Test fun boxedAboveTheDeferralCapMergesEagerly() {
    fun leaf(tag: Int, value: Int) = encode(BoxedLeaf.ADAPTER, tag, BoxedLeaf(values = listOf(value)))
    val bytes = concat(leaf(1, 1), leaf(100, 2), leaf(1, 3), leaf(100, 4), leaf(65, 5), leaf(65, 6))
    for (message in decodeAll(BoxedCap.ADAPTER, bytes)) {
      assertThat(message.f1).isEqualTo(BoxedLeaf(values = listOf(1, 3)))
      assertThat(message.f65).isEqualTo(BoxedLeaf(values = listOf(5, 6)))
      assertThat(message.choice).isEqualTo(OneOf(BoxedCap.CHOICE_A, BoxedLeaf(values = listOf(2, 4))))
    }
  }

  @Test fun boxedAboveTheDeferralCapStillReplacesTheMember() {
    fun leaf(tag: Int, value: Int) = encode(BoxedLeaf.ADAPTER, tag, BoxedLeaf(values = listOf(value)))
    val bytes = concat(leaf(100, 1), leaf(101, 2), leaf(100, 3), leaf(100, 4))
    for (message in decodeAll(BoxedCap.ADAPTER, bytes)) {
      assertThat(message.choice).isEqualTo(OneOf(BoxedCap.CHOICE_A, BoxedLeaf(values = listOf(3, 4))))
    }
  }

  @Test fun largeSealedOneofKeepsTheLastMemberLikeItAlwaysDid() {
    fun leaf(tag: Int, value: Int) = encode(SealedLeaf.ADAPTER, tag, SealedLeaf(values = listOf(value)))
    val bytes = concat(leaf(1, 1), leaf(1, 2), leaf(22, 3), leaf(22, 4))
    // The reflective adapter merges here, so only the generated adapter is checked.
    val expected = SealedCap(choice = SealedCap.Choice.M22(SealedLeaf(values = listOf(4))))
    for (message in decodeAll(SealedCap.ADAPTER, bytes)) {
      assertThat(message).isEqualTo(expected)
    }
    val repeated = concat(leaf(1, 1), leaf(1, 2))
    for (message in decodeAll(SealedCap.ADAPTER, repeated)) {
      assertThat(message).isEqualTo(SealedCap(choice = SealedCap.Choice.M1(SealedLeaf(values = listOf(2)))))
    }
  }

  @Test fun boxedMembersNamedLikeLocalsOfTheDecodeFunction() {
    fun leaf(tag: Int, value: Int) = encode(BoxedLeaf.ADAPTER, tag, BoxedLeaf(values = listOf(value)))
    val bytes = concat(leaf(5, 1), leaf(1, 2), leaf(6, 3), leaf(5, 4), leaf(1, 5), leaf(6, 6))
    for (message in decodeAll(BoxedNames.ADAPTER, bytes)) {
      assertThat(message.existing).isEqualTo(BoxedLeaf(values = listOf(1, 4)))
      assertThat(message.it).isEqualTo(OneOf(BoxedNames.IT_X, BoxedLeaf(values = listOf(2, 5))))
      assertThat(message.reader_).isEqualTo(OneOf(BoxedNames.READER_Y, BoxedLeaf(values = listOf(3, 6))))
    }
  }

  @Test fun sealedMembersNamedLikeLocalsOfTheDecodeFunction() {
    fun leaf(tag: Int, value: Int) = encode(SealedLeaf.ADAPTER, tag, SealedLeaf(values = listOf(value)))
    val bytes = concat(leaf(5, 1), leaf(1, 2), leaf(2, 3), leaf(1, 4), leaf(5, 5), leaf(1, 6), leaf(6, 7), leaf(6, 8))
    for (message in decodeAll(SealedNames.ADAPTER, bytes)) {
      assertThat(message.existing).isEqualTo(SealedLeaf(values = listOf(1, 5)))
      assertThat(message.it).isEqualTo(SealedNames.It.X(SealedLeaf(values = listOf(4, 6))))
      assertThat(message.reader_).isEqualTo(SealedNames.Reader.Y(SealedLeaf(values = listOf(7, 8))))
    }
  }

  @Test fun sealedBuildersOnlyMembersNamedLikeLocalsOfTheDecodeFunction() {
    fun leaf(tag: Int, value: Int) = encode(BuildersLeaf.ADAPTER, tag, buildersLeaf(value))
    val bytes = concat(leaf(5, 1), leaf(1, 2), leaf(2, 3), leaf(1, 4), leaf(5, 5), leaf(1, 6), leaf(6, 7), leaf(6, 8))
    for (message in decodeAll(SealedBuildersOnlyNames.ADAPTER, bytes)) {
      assertThat(message.existing).isEqualTo(buildersLeaf(1, 5))
      assertThat(message.it).isEqualTo(SealedBuildersOnlyNames.It.X(buildersLeaf(4, 6)))
      assertThat(message.reader_).isEqualTo(SealedBuildersOnlyNames.Reader.Y(buildersLeaf(7, 8)))
    }
  }
}
