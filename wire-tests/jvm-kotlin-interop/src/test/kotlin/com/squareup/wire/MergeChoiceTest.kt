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

import com.squareup.wire.protos.kotlin.edgecases.MergeChoice
import com.squareup.wire.protos.kotlin.edgecases.MergeChoiceBuildersOnly
import com.squareup.wire.protos.kotlin.edgecases.MergeLeaf
import kotlin.test.Test
import kotlin.test.assertEquals
import okio.Buffer

/** Repeated message fields are merged, and a oneof keeps only its last member. */
class MergeChoiceTest {
  private val a = 1
  private val b = 2
  private val c = 3

  private val reflectiveAdapter = ProtoAdapter.newMessageAdapter(MergeChoice::class.java)
  private val reflectiveBuildersOnlyAdapter =
    ProtoAdapter.newMessageAdapter(MergeChoiceBuildersOnly::class.java)

  private fun encode(vararg occurrences: Pair<Int, Int>): ByteArray {
    val buffer = Buffer()
    for ((tag, value) in occurrences) {
      MergeLeaf.ADAPTER.encodeWithTag(ProtoWriter(buffer), tag, MergeLeaf(values = listOf(value)))
    }
    return buffer.readByteArray()
  }

  private fun assertDecodes(expected: String, vararg occurrences: Pair<Int, Int>) {
    val bytes = encode(*occurrences)
    assertEquals(expected, describe(MergeChoice.ADAPTER.decode(bytes)))
    assertEquals(expected, describe(MergeChoice.ADAPTER.decode(Buffer().write(bytes))))
    assertEquals(expected, describe(MergeChoiceBuildersOnly.ADAPTER.decode(bytes)))
    assertEquals(expected, describe(MergeChoiceBuildersOnly.ADAPTER.decode(Buffer().write(bytes))))
    assertEquals(expected, describe(reflectiveAdapter.decode(bytes)))
    assertEquals(expected, describe(reflectiveAdapter.decode(Buffer().write(bytes))))
    assertEquals(expected, describe(reflectiveBuildersOnlyAdapter.decode(bytes)))
    assertEquals(expected, describe(reflectiveBuildersOnlyAdapter.decode(Buffer().write(bytes))))
  }

  private fun describe(message: MergeChoice) = "a=${message.a?.values} b=${message.b?.values} c=${message.c?.values}"

  private fun describe(message: MergeChoiceBuildersOnly) = "a=${message.a?.values} b=${message.b?.values} c=${message.c?.values}"

  @Test fun memberThenRepeatedUnrelatedField() = assertDecodes("a=null b=[1] c=[2, 3]", b to 1, c to 2, c to 3)

  @Test fun sameMemberTwice() = assertDecodes("a=[1, 2] b=null c=null", a to 1, a to 2)

  @Test fun memberRepeatedThenReplaced() = assertDecodes("a=null b=[3] c=null", a to 1, a to 2, b to 3)

  @Test fun memberReplacedThenSetAgain() = assertDecodes("a=[3] b=null c=null", a to 1, b to 2, a to 3)

  @Test fun repeatedMembersAreMergedAfterBeingSetAgain() = assertDecodes("a=[4, 5] b=null c=null", a to 1, a to 2, b to 3, a to 4, a to 5)

  @Test fun repeatedMemberAmongOtherFields() = assertDecodes("a=[1, 3] b=null c=[2, 4]", a to 1, c to 2, a to 3, c to 4)

  @Test fun lastMemberWins() = assertDecodes("a=null b=[5] c=null", b to 1, b to 2, a to 3, a to 4, b to 5)
}
