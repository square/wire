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

import com.squareup.wire.protos.kotlin.OneOfMessage
import com.squareup.wire.protos.kotlin.alltypes.AllTypes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import okio.ByteString.Companion.decodeHex

class ProtoReader32SingularScalarEncodingTest {
  @Test fun lengthDelimitedScalarIsUnknown() {
    for (hex in listOf("0a0107", "0a020102", "0a00")) {
      val bytes = hex.decodeHex()
      val message = AllTypes.NestedMessage.ADAPTER.decode(ProtoReader32(bytes.toByteArray()))
      assertNull(message.a)
      assertEquals(bytes, message.unknownFields)
      assertEquals(bytes, message.encodeByteString())
    }
  }

  @Test fun incompatibleOneOfDoesNotClearPreviousChoice() {
    val bytes = "1a01610a0107".decodeHex()
    val message = OneOfMessage.ADAPTER.decode(ProtoReader32(bytes.toByteArray()))
    assertNull(message.foo)
    assertEquals("a", message.bar)
    assertEquals("0a0107".decodeHex(), message.unknownFields)
  }
}
