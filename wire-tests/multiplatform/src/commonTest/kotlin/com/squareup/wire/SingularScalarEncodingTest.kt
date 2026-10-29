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

import com.squareup.wire.protos.kotlin.BoxedOneOfs
import com.squareup.wire.protos.kotlin.OneOfMessage
import com.squareup.wire.protos.kotlin.simple.SimpleMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import okio.Buffer
import okio.ByteString.Companion.decodeHex
import squareup.protos.packed_encoding.EmbeddedMessage
import squareup.protos.packed_encoding.OuterMessage

class SingularScalarEncodingTest {
  @Test fun lengthDelimitedScalarIsUnknown() {
    for (hex in listOf("0a0107", "0a020102", "0a00")) {
      val bytes = hex.decodeHex()
      for (message in listOf(OuterMessage.ADAPTER.decode(bytes), OuterMessage.ADAPTER.decode(Buffer().write(bytes)))) {
        assertNull(message.outer_number_before)
        assertEquals(bytes, message.unknownFields)
        assertEquals(bytes, message.encodeByteString())
      }
    }
  }

  @Test fun packedValueDoesNotSatisfyRequiredScalar() {
    assertFailsWith<IllegalStateException> {
      SimpleMessage.ADAPTER.decode("2a0107".decodeHex())
    }
  }

  @Test fun incompatibleOneOfDoesNotClearPreviousChoice() {
    val message = OneOfMessage.ADAPTER.decode("1a01610a0107".decodeHex())
    assertNull(message.foo)
    assertEquals("a", message.bar)
    assertEquals("0a0107".decodeHex(), message.unknownFields)
  }

  @Test fun incompatibleBoxedOneOfDoesNotClearPreviousChoice() {
    val message = BoxedOneOfs.ADAPTER.decode("0a01615a0107".decodeHex())
    assertEquals("a", message.OneOf!!.value)
    assertEquals("5a0107".decodeHex(), message.unknownFields)
  }

  @Test fun repeatedScalarsAcceptPackedAndUnpackedValues() {
    val message = EmbeddedMessage.ADAPTER.decode("0a02010208031004".decodeHex())
    assertEquals(listOf(1, 2, 3), message.inner_repeated_number)
    assertEquals(4, message.inner_number_after)
  }
}
