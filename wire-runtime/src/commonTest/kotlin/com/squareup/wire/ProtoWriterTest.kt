/*
 * Copyright 2015 Square Inc.
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

import okio.Buffer
import okio.utf8Size
import kotlin.test.Test
import kotlin.test.assertEquals

class ProtoWriterTest {
  @Test fun utf8() {
    // 0 byte strings.
    assertUtf8("", "")

    // 1 byte code points.
    assertUtf8("\u0000", "00")
    assertUtf8("A", "41")

    // 2 byte code points.
    assertUtf8("\u0080", "c280")
    assertUtf8("\u07ff", "dfbf")

    // 3 byte code points.
    assertUtf8("\u0800", "e0a080")
    assertUtf8("\ud7ff", "ed9fbf")
    assertUtf8("\ue000", "ee8080")
    assertUtf8("\uffff", "efbfbf")

    // 4 byte code points, in Java as a high surrogate followed by low surrogate.
    assertUtf8("\ud800\udc00", "f0908080")

    // Malformed UTF-16.
    assertUtf8("\ud800", "3f") // Dangling high surrogate.
    assertUtf8("\ud800A", "3f41") // High surrogate followed by a 1 byte code point.
    assertUtf8("\ud800\ue000", "3fee8080") // High surrogate followed by a 3 byte code point.
    assertUtf8("\ud800\ud800\udc00", "3ff0908080") // High surrogate followed by surrogate pair.
    assertUtf8("\udc00A", "3f41") // Unexpected low surrogate.
    assertUtf8("\udc00", "3f") // Unexpected, dangling low surrogate.
  }

  private fun assertUtf8(string: String, expectedHex: String) {
    val buffer = Buffer()
    val writer = ProtoWriter(buffer)
    writer.writeString(string)
    assertEquals(expectedHex, buffer.readByteString().hex())
    assertEquals((expectedHex.length / 2).toLong(), string.utf8Size())
  }
}
