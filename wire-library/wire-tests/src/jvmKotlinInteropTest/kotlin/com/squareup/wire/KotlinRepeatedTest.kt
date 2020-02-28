/*
* Copyright 2019 Square Inc.
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

import com.squareup.wire.internal.RuntimeMessageAdapter
import com.squareup.wire.protos.kotlin.repeated.Repeated
import com.squareup.wire.protos.kotlin.repeated.Thing
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KotlinRepeatedTest {
  private val adapter = RuntimeMessageAdapter.create(Repeated::class.java, "square.github.io/wire/unknown")

  @Test fun serialize() {
    assertEquals(BYTES, adapter.encode(THREE).toByteString())

    assertEquals(0, adapter.encode(EMPTY).size)
  }

  @Test fun deserialize() {
    assertEquals(THREE, adapter.decode(BYTES))

    val empty = adapter.decode(ByteArray(0))
    assertNotNull(empty.things)
  }

  companion object {
    private val BYTES = "0a050a034f6e650a050a0354776f0a070a055468726565".decodeHex()
    private val EMPTY = Repeated(things = emptyList())
    private val THREE = Repeated(
        things = listOf(
            Thing("One"),
            Thing("Two"),
            Thing("Three")
        )
    )
  }
}
