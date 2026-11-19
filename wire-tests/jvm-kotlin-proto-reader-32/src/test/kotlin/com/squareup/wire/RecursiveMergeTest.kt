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

import com.squareup.wire.protos.kotlin.edgecases.Recursive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import okio.Buffer
import okio.IOException

class RecursiveMergeTest {
  @Test
  fun repeatedMessageFieldsAreMerged() {
    val first = Recursive(value_ = 1, recursive = Recursive(value_ = 10))
    val second = Recursive(recursive = Recursive(value_ = 20))
    val buffer = Buffer()
    Recursive.ADAPTER.encodeWithTag(ProtoWriter(buffer), 2, first)
    Recursive.ADAPTER.encodeWithTag(ProtoWriter(buffer), 2, second)
    val bytes = buffer.readByteArray()

    val expected = Recursive(recursive = Recursive(value_ = 1, recursive = Recursive(value_ = 20)))
    assertEquals(expected, Recursive.ADAPTER.decode(bytes))
    assertEquals(expected, Recursive.ADAPTER.decode(Buffer().write(bytes)))
  }

  @Test
  fun repeatedMessageFieldsShareTheRecursionLimit() {
    // Each level is an empty message followed by a message with the next level.
    var current = Buffer()
    repeat(5_000) {
      val next = Buffer()
      ProtoAdapter.BYTES.encodeWithTag(ProtoWriter(next), 2, okio.ByteString.EMPTY)
      ProtoAdapter.BYTES.encodeWithTag(ProtoWriter(next), 2, current.readByteString())
      current = next
    }
    val bytes = current.readByteArray()

    val bytesFailure = assertFailsWith<IOException> { Recursive.ADAPTER.decode(bytes) }
    assertEquals("Wire recursion limit exceeded", bytesFailure.message)
    val sourceFailure = assertFailsWith<IOException> { Recursive.ADAPTER.decode(Buffer().write(bytes)) }
    assertEquals("Wire recursion limit exceeded", sourceFailure.message)
  }
}
