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

import com.squareup.wire.protos.kotlin.oneofmergeboxed.BoxedMerge
import com.squareup.wire.protos.kotlin.oneofmergeboxed.BoxedNames
import com.squareup.wire.protos.kotlin.oneofmergeboxed.Leaf as BoxedLeaf
import com.squareup.wire.protos.kotlin.oneofmergesealed.Leaf as SealedLeaf
import com.squareup.wire.protos.kotlin.oneofmergesealed.SealedMerge
import com.squareup.wire.protos.kotlin.oneofmergesealed.SealedNames
import kotlin.test.Test
import kotlin.test.assertEquals
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex

private operator fun ByteString.plus(other: ByteString): ByteString = Buffer().write(this).write(other).readByteString()

/** Boxed and sealed oneofs merge repeated message members when they decode from a [ProtoReader32]. */
class ProtoReader32OneOfMergeTest {
  private val numberValue = "220911000000000000f03f".decodeHex()
  private val nullValue = "22020800".decodeHex()

  private fun <T> encode(adapter: ProtoAdapter<T>, tag: Int, value: T): ByteString {
    val buffer = Buffer()
    adapter.encodeWithTag(ProtoWriter(buffer), tag, value)
    return buffer.readByteString()
  }

  private fun boxedLeaf(tag: Int, value: Int) = encode(BoxedLeaf.ADAPTER, tag, BoxedLeaf(values = listOf(value)))

  private fun sealedLeaf(tag: Int, value: Int) = encode(SealedLeaf.ADAPTER, tag, SealedLeaf(values = listOf(value)))

  private fun <T : Any> ProtoAdapter<T>.decodeBoth(bytes: ByteString): List<T> = listOf(decode(ProtoReader32(bytes.toByteArray())), decode(Buffer().write(bytes)))

  @Test fun boxedMergesRepeatedMemberAndKeepsLastMember() {
    val bytes = boxedLeaf(1, 1) + boxedLeaf(1, 2) + boxedLeaf(2, 3) + boxedLeaf(1, 4) + boxedLeaf(1, 5)
    for (message in BoxedMerge.ADAPTER.decodeBoth(bytes)) {
      assertEquals(OneOf(BoxedMerge.CHOICE_A, BoxedLeaf(values = listOf(4, 5))), message.choice)
    }
  }

  @Test fun sealedMergesRepeatedMemberAndKeepsLastMember() {
    val bytes = sealedLeaf(1, 1) + sealedLeaf(1, 2) + sealedLeaf(2, 3) + sealedLeaf(1, 4) + sealedLeaf(1, 5)
    for (message in SealedMerge.ADAPTER.decodeBoth(bytes)) {
      assertEquals(SealedMerge.Choice.A(SealedLeaf(values = listOf(4, 5))), message.choice)
    }
  }

  @Test fun boxedNullValueAfterNumberIsNull() {
    for (message in BoxedMerge.ADAPTER.decodeBoth(numberValue + nullValue)) {
      assertEquals(OneOf(BoxedMerge.CHOICE_VALUE, null), message.choice)
    }
    for (message in BoxedMerge.ADAPTER.decodeBoth(nullValue + numberValue)) {
      assertEquals(OneOf(BoxedMerge.CHOICE_VALUE, 1.0), message.choice)
    }
  }

  @Test fun sealedNullValueAfterNumberIsNull() {
    for (message in SealedMerge.ADAPTER.decodeBoth(numberValue + nullValue)) {
      assertEquals(SealedMerge.Choice.Value(null), message.choice)
    }
    for (message in SealedMerge.ADAPTER.decodeBoth(nullValue + numberValue)) {
      assertEquals(SealedMerge.Choice.Value(1.0), message.choice)
    }
  }

  @Test fun membersNamedLikeLocalsOfTheDecodeFunction() {
    val boxed = boxedLeaf(5, 1) + boxedLeaf(1, 2) + boxedLeaf(5, 3) + boxedLeaf(1, 4)
    for (message in BoxedNames.ADAPTER.decodeBoth(boxed)) {
      assertEquals(BoxedLeaf(values = listOf(1, 3)), message.existing)
      assertEquals(OneOf(BoxedNames.IT_X, BoxedLeaf(values = listOf(2, 4))), message.it)
    }
    val sealed = sealedLeaf(5, 1) + sealedLeaf(1, 2) + sealedLeaf(5, 3) + sealedLeaf(1, 4)
    for (message in SealedNames.ADAPTER.decodeBoth(sealed)) {
      assertEquals(SealedLeaf(values = listOf(1, 3)), message.existing)
      assertEquals(SealedNames.It.X(SealedLeaf(values = listOf(2, 4))), message.it)
    }
  }
}
