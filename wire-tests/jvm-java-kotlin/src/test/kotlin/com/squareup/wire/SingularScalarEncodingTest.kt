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

import com.squareup.wire.internal.createRuntimeMessageAdapter
import com.squareup.wire.protos.oneof.OneOfMessage
import com.squareup.wire.protos.simple.SimpleMessage
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
    val adapters = listOf(
      OuterMessage.ADAPTER,
      createRuntimeMessageAdapter(
        OuterMessage::class.java,
        null,
        Syntax.PROTO_2,
        OuterMessage::class.java.classLoader,
      ),
    )
    for (adapter in adapters) {
      for (hex in listOf("0a0107", "0a020102", "0a00")) {
        val bytes = hex.decodeHex()
        for (message in listOf(adapter.decode(bytes), adapter.decode(Buffer().write(bytes)))) {
          assertNull(message.outer_number_before)
          assertEquals(bytes, message.unknownFields)
          assertEquals(bytes, adapter.encodeByteString(message))
        }
      }
    }
  }

  @Test fun packedValueDoesNotSatisfyRequiredScalar() {
    val adapters = listOf(
      SimpleMessage.ADAPTER,
      createRuntimeMessageAdapter(
        SimpleMessage::class.java,
        null,
        Syntax.PROTO_2,
        SimpleMessage::class.java.classLoader,
      ),
    )
    for (adapter in adapters) {
      assertFailsWith<IllegalStateException> {
        adapter.decode("2a0107".decodeHex())
      }
    }
  }

  @Test fun incompatibleOneOfDoesNotClearPreviousChoice() {
    val bytes = "1a01610a0107".decodeHex()
    val adapters = listOf(
      OneOfMessage.ADAPTER,
      createRuntimeMessageAdapter(
        OneOfMessage::class.java,
        null,
        Syntax.PROTO_2,
        OneOfMessage::class.java.classLoader,
      ),
    )
    for (adapter in adapters) {
      val message = adapter.decode(bytes)
      assertNull(message.foo)
      assertEquals("a", message.bar)
      assertEquals("0a0107".decodeHex(), message.unknownFields)
    }
  }

  @Test fun repeatedScalarsAcceptPackedAndUnpackedValues() {
    val message = EmbeddedMessage.ADAPTER.decode("0a02010208031004".decodeHex())
    assertEquals(listOf(1, 2, 3), message.inner_repeated_number)
    assertEquals(4, message.inner_number_after)
  }
}
