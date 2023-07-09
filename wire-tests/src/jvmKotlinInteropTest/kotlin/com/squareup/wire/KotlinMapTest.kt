/*
 * Copyright (C) 2018 Square, Inc.
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
import com.squareup.wire.protos.kotlin.map.Mappy
import com.squareup.wire.protos.kotlin.map.Thing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import okio.ByteString
import okio.ByteString.Companion.decodeHex

class KotlinMapTest {
  private val adapter = createRuntimeMessageAdapter(
    Mappy::class.java,
    "square.github.io/wire/unknown",
    Syntax.PROTO_2,
    KotlinMapTest::class.java.classLoader,
  )

  @Test fun serialize() {
    assertEquals(BYTES, ByteString.of(*adapter.encode(THREE)))

    assertEquals(0, adapter.encode(EMPTY).size)
  }

  @Test fun deserialize() {
    assertEquals(THREE, adapter.decode(BYTES))

    val empty = adapter.decode(ByteArray(0))
    assertNotNull(empty.things)
  }

  companion object {
    private val BYTES =
      "0a0c0a036f6e6512050a034f6e650a0c0a0374776f12050a0354776f0a100a05746872656512070a055468726565".decodeHex()
    private val EMPTY = Mappy(things = emptyMap())
    private val THREE = Mappy(
      things = mapOf(
        "one" to Thing("One"),
        "two" to Thing("Two"),
        "three" to Thing("Three"),
      ),
    )
  }
}
