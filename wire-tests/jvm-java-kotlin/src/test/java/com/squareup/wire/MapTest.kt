/*
 * Copyright (C) 2016 Square, Inc.
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

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.squareup.wire.internal.createRuntimeMessageAdapter
import com.squareup.wire.map.Mappy
import com.squareup.wire.map.MappyTwo
import com.squareup.wire.map.MappyTwo.ValueEnum
import com.squareup.wire.map.Thing
import java.io.IOException
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class MapTest {
  @Parameter(0)
  lateinit var name: String

  @Parameter(1)
  lateinit var adapter: ProtoAdapter<Mappy>

  @Parameter(2)
  lateinit var adapterTwo: ProtoAdapter<MappyTwo>

  @Test
  fun serialize() {
    assertThat(ByteString.of(*adapter.encode(THREE))).isEqualTo(BYTES)

    assertThat(adapter.encode(EMPTY)).isEmpty()
  }

  @Test
  @Throws(IOException::class)
  fun deserialize() {
    assertThat(adapter.decode(BYTES)).isEqualTo(THREE)

    val empty = adapter.decode(ByteArray(0))
    assertThat(empty.things).isNotNull()
  }

  @Test
  fun mapEntryWithUnknownTagIsSkipped() {
    // Entry with key "k", value "n", and an unknown varint field 3.
    val varint = "0a0a0a016b12030a016e1800".decodeHex()
    assertThat(adapter.decode(varint).things).isEqualTo(mapOf("k" to Thing("n")))
    assertThat(adapter.decode(Buffer().write(varint)).things).isEqualTo(mapOf("k" to Thing("n")))

    // Same entry with unknown length-delimited, fixed32, and fixed64 fields.
    val others = "0a1a0a016b12030a016e220200aa2d00000000310000000000000000".decodeHex()
    assertThat(adapter.decode(others).things).isEqualTo(mapOf("k" to Thing("n")))
    assertThat(adapter.decode(Buffer().write(others)).things).isEqualTo(mapOf("k" to Thing("n")))
  }

  @Test
  fun mapEntryWithoutValueHasDefaultValue() {
    // Entry with key "k" and no value.
    val noValue = "0a030a016b".decodeHex()
    assertThat(adapter.decode(noValue).things).isEqualTo(mapOf("k" to Thing.Builder().build()))
    assertThat(adapter.decode(Buffer().write(noValue)).things)
      .isEqualTo(mapOf("k" to Thing.Builder().build()))

    // Entry with neither key nor value.
    val empty = "0a00".decodeHex()
    assertThat(adapter.decode(empty).things).isEqualTo(mapOf("" to Thing.Builder().build()))

    // Entry with only an unknown field.
    val onlyUnknown = "0a021800".decodeHex()
    assertThat(adapter.decode(onlyUnknown).things).isEqualTo(mapOf("" to Thing.Builder().build()))
  }

  @Test
  fun mapEntryWithUnknownEnumValueIsKeptAsUnknownField() {
    // Entries "a" to FOO, "k" to 77 (unknown), "b" to BAR.
    val bytes = ("0a050a01611001" + "0a050a016b104d" + "0a050a01621002").decodeHex()
    for (message in listOf(adapterTwo.decode(bytes), adapterTwo.decode(Buffer().write(bytes)))) {
      assertThat(message.string_enums).isEqualTo(mapOf("a" to ValueEnum.FOO, "b" to ValueEnum.BAR))
      assertThat(message.unknownFields).isEqualTo("0a050a016b104d".decodeHex())
      // The unknown entry survives a round trip.
      assertThat(ByteString.of(*adapterTwo.encode(message)))
        .isEqualTo(("0a050a01611001" + "0a050a01621002" + "0a050a016b104d").decodeHex())
    }
  }

  @Test
  fun mapEntryWithoutEnumValueHasDefaultValue() {
    val message = adapterTwo.decode("0a030a016b".decodeHex())
    assertThat(message.string_enums).isEqualTo(mapOf("k" to ValueEnum.DEFAULT))
  }

  companion object {
    private val BYTES =
      "0a0c0a036f6e6512050a034f6e650a0c0a0374776f12050a0354776f0a100a05746872656512070a055468726565"
        .decodeHex()
    private val EMPTY = Mappy.Builder()
      .build()
    private val THREE = Mappy.Builder()
      .things(mapOf("one" to Thing("One"), "two" to Thing("Two"), "three" to Thing("Three")))
      .build()

    @Parameters(name = "{0}")
    @JvmStatic
    fun parameters() = listOf(
      arrayOf("Generated", Mappy.ADAPTER, MappyTwo.ADAPTER),
      arrayOf(
        "Runtime",
        createRuntimeMessageAdapter(
          Mappy::class.java,
          "square.github.io/wire/unknown",
          Syntax.PROTO_2,
          Mappy::class.java.classLoader,
        ),
        createRuntimeMessageAdapter(
          MappyTwo::class.java,
          "square.github.io/wire/unknown",
          Syntax.PROTO_2,
          MappyTwo::class.java.classLoader,
        ),
      ),
    )
  }
}
