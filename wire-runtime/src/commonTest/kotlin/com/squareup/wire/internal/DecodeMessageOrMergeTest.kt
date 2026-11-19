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
package com.squareup.wire.internal

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoReader32
import kotlin.test.Test
import okio.Buffer
import okio.ByteString
import okio.IOException

class DecodeMessageOrMergeTest {
  private fun decodeAll(adapter: ProtoAdapter<Node>, bytes: ByteString, block: (Node) -> Unit) {
    block(adapter.decode(bytes.toByteArray()))
    block(adapter.decode(Buffer().write(bytes)))
  }

  @Test fun twoOccurrencesAreMerged() {
    val first = int32(2, 1) + lengthDelimited(1, int32(2, 10))
    val second = int32(2, 2) + lengthDelimited(1, int32(2, 20))
    val bytes = lengthDelimited(1, first) + lengthDelimited(1, second)
    for (adapter in nodeAdapters) {
      decodeAll(adapter, bytes) { node ->
        assertThat(node.toString()).isEqualTo(
          "Node(child=Node(child=Node(child=null, values=[10, 20]), values=[1, 2]), values=[])",
        )
      }
    }
  }

  @Test fun someHundredsOfOccurrencesAreMerged() {
    val buffer = Buffer()
    repeat(100) { occurrence ->
      val node = Buffer()
      repeat(10) { node.write(int32(2, occurrence)) }
      buffer.write(lengthDelimited(1, node.readByteString()))
    }
    val bytes = buffer.readByteString()
    for (adapter in nodeAdapters) {
      decodeAll(adapter, bytes) { node ->
        assertThat(node.child!!.values.size).isEqualTo(1_000)
      }
    }
  }

  @Test fun mergedMessagesShareTheRecursionLimit() {
    for (adapter in nodeAdapters) {
      for (levels in listOf(150, 400)) {
        val bytes = nestedMerges(levels)
        assertFailure { adapter.decode(bytes.toByteArray()) }
          .isInstanceOf<IOException>().hasMessage("Wire recursion limit exceeded")
        assertFailure { adapter.decode(Buffer().write(bytes)) }
          .isInstanceOf<IOException>().hasMessage("Wire recursion limit exceeded")
      }
    }
  }

  @Test fun mergedMessagesWithinTheRecursionLimitAreDecoded() {
    val bytes = nestedMerges(50)
    for (adapter in nodeAdapters) {
      decodeAll(adapter, bytes) { node ->
        var depth = 0
        var current: Node? = node
        while (current != null) {
          depth++
          current = current.child
        }
        assertThat(depth).isEqualTo(51)
      }
    }
  }

  @Test fun mergedMessagesShareACustomRecursionLimit() {
    val bytes = nestedMerges(20)
    for (adapter in nodeAdapters) {
      assertFailure { adapter.decode(ProtoReader32(bytes, recursionLimit = 10)) }
        .isInstanceOf<IOException>().hasMessage("Wire recursion limit exceeded")
      assertFailure { adapter.decode(ProtoReader(Buffer().write(bytes), 10)) }
        .isInstanceOf<IOException>().hasMessage("Wire recursion limit exceeded")
      adapter.decode(ProtoReader32(bytes, recursionLimit = 30))
      adapter.decode(ProtoReader(Buffer().write(bytes), 30))
    }
  }

  @Test fun customReaderMergesShareTheRecursionLimit() {
    for (adapter in protoReader32NodeAdapters) {
      assertFailure { adapter.decode(CustomProtoReader32(ProtoReader32(nestedMerges(150)))) }
        .isInstanceOf<IOException>().hasMessage("Wire recursion limit exceeded")
      assertFailure { adapter.decode(CustomProtoReader32(ProtoReader32(nestedMerges(20), recursionLimit = 10))) }
        .isInstanceOf<IOException>().hasMessage("Wire recursion limit exceeded")
      adapter.decode(CustomProtoReader32(ProtoReader32(nestedMerges(20), recursionLimit = 30)))
    }
  }

  @Test fun customReaderMergesOccurrences() {
    val first = int32(2, 1) + lengthDelimited(1, int32(2, 10))
    val second = int32(2, 2) + lengthDelimited(1, int32(2, 20))
    val bytes = lengthDelimited(1, first) + lengthDelimited(1, second)
    for (adapter in protoReader32NodeAdapters) {
      val node = adapter.decode(CustomProtoReader32(ProtoReader32(bytes)))
      assertThat(node.toString()).isEqualTo(
        "Node(child=Node(child=Node(child=null, values=[10, 20]), values=[1, 2]), values=[])",
      )
    }
  }

  @Test fun customReaderThatCannotReturnAProtoReaderStillMerges() {
    val first = int32(2, 1) + lengthDelimited(1, int32(2, 10))
    val second = int32(2, 2) + lengthDelimited(1, int32(2, 20))
    val bytes = lengthDelimited(1, first) + lengthDelimited(1, second)
    val failures = listOf<() -> Nothing>(
      { throw UnsupportedOperationException() },
      { TODO() },
    )
    for (adapter in protoReader32NodeAdapters) {
      for (failure in failures) {
        val node = adapter.decode(CustomProtoReader32WithoutProtoReader(ProtoReader32(bytes), failure))
        assertThat(node.toString()).isEqualTo(
          "Node(child=Node(child=Node(child=null, values=[10, 20]), values=[1, 2]), values=[])",
        )
      }
    }
  }
}
