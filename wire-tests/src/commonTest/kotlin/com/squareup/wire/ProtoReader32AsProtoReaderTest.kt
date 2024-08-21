/*
 * Copyright (C) 2024 Square, Inc.
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

import com.squareup.wire.TestAllTypesData.allTypes
import com.squareup.wire.protos.kotlin.alltypes.AllTypes
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * This test is similar to `ProtoReader32AdapterTest.kt` except it targets code generated without
 * the `emitProtoReader32` option.
 */
class ProtoReader32AsProtoReaderTest {

  @Test
  fun decodeProtoReader32ByteString() {
    val protoReader32 = ProtoReader32(allTypes.encodeByteString())
    assertEquals(allTypes, AllTypes.ADAPTER.decode(protoReader32))
  }

  @Test
  fun decodeProtoReader32ByteArray() {
    val protoReader32 = ProtoReader32(allTypes.encode())
    assertEquals(allTypes, AllTypes.ADAPTER.decode(protoReader32))
  }

  @Test
  fun decodeProtoReader32AsProtoReader() {
    val protoReader = ProtoReader32(allTypes.encode()).asProtoReader()
    assertEquals(allTypes, AllTypes.ADAPTER.decode(protoReader))
  }
}
