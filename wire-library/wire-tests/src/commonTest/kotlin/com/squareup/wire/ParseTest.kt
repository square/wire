/*
 * Copyright 2014 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire

import com.squareup.wire.internal.ProtocolException
import com.squareup.wire.protos.kotlin.edgecases.OneBytesField
import com.squareup.wire.protos.kotlin.edgecases.OneField
import com.squareup.wire.protos.kotlin.edgecases.Recursive
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.EOFException
import okio.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.fail

class ParseTest {
  @Test
  fun unknownTagIgnored() {
    // tag 1 / type 0: 456
    // tag 2 / type 0: 789
    val data = "08c803109506".decodeHex()
    val oneField = OneField.ADAPTER.decode(data.toByteArray())
    val expected = OneField(opt_int32 = 456)
    assertNotEquals(expected, oneField)
    assertEquals(expected, oneField.copy(unknownFields = ByteString.EMPTY))
  }

  @Test
  fun unknownTypeThrowsIOException() {
    // tag 1 / type 0: 456
    // tag 2 / type 7: 789
    val data = "08c803179506".decodeHex()
    try {
      OneField.ADAPTER.decode(data.toByteArray())
      fail()
    } catch (expected: ProtocolException) {
      assertEquals("Unexpected field encoding: 7", expected.message)
    }
  }

  @Test
  fun truncatedMessageThrowsEOFException() {
    // tag 1 / 4-byte length delimited string: 0x000000 (3 bytes)
    val data = "0a04000000".decodeHex()
    try {
      OneBytesField.ADAPTER.decode(data.toByteArray())
      fail()
    } catch (expected: EOFException) {
    }
  }

  @Test
  fun lastValueWinsForRepeatedValueOfNonrepeatedField() {
    // tag 1 / type 0: 456
    // tag 1 / type 0: 789
    val data = "08c803089506".decodeHex()
    val oneField = OneField.ADAPTER.decode(data.toByteArray())
    assertEquals(oneField, OneField(opt_int32 = 789))
  }

  @Test
  fun upToRecursionLimit() {
    // tag 2: nested message (64 times)
    // tag 1: signed varint32 456
    val data = ("127e127c127a12781276127412721270126e126c126a12681266126"
        + "412621260125e125c125a12581256125412521250124e124c124a12481246124412421240123e123c123a123"
        + "81236123412321230122e122c122a12281226122412221220121e121c121a12181216121412121210120e120"
        + "c120a1208120612041202120008c803").decodeHex()
    val recursive = Recursive.ADAPTER.decode(data.toByteArray())
    assertEquals(456, recursive.value!!.toInt())
  }

  @Test
  fun overRecursionLimitThrowsIOException() {
    // tag 2: nested message (65 times)
    // tag 1: signed varint32 456
    val data = ("128001127e127c127a12781276127412721270126e126c126a12681"
        + "266126412621260125e125c125a12581256125412521250124e124c124a12481246124412421240123e123c1"
        + "23a12381236123412321230122e122c122a12281226122412221220121e121c121a121812161214121212101"
        + "20e120c120a1208120612041202120008c803").decodeHex()
    try {
      Recursive.ADAPTER.decode(data.toByteArray())
      fail()
    } catch (expected: IOException) {
      assertEquals("Wire recursion limit exceeded", expected.message)
    }
  }
}
