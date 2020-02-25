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
import okio.ByteString.Companion.decodeHex
import kotlin.test.Test
import kotlin.test.assertEquals

class ProtoReaderTest {
  @Test fun packedExposedAsRepeated() {
    val packedEncoded = "d20504d904bd05".decodeHex()
    val reader = ProtoReader(Buffer().write(packedEncoded))
    val token = reader.beginMessage()
    assertEquals(90, reader.nextTag())
    assertEquals(601, ProtoAdapter.INT32.decode(reader))
    assertEquals(90, reader.nextTag())
    assertEquals(701, ProtoAdapter.INT32.decode(reader))
    assertEquals(-1, reader.nextTag())
    reader.endMessageAndGetUnknownFields(token)
  }
}
