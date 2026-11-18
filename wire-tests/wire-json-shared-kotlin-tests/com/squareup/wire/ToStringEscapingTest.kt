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

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import org.junit.Test
import squareup.proto3.AllStructs
import squareup.proto3.AllWrappers
import squareup.proto3.MapTypes
import squareup.proto3.Pizza
import squareup.proto3.PizzaDelivery

/**
 * Tests meant to be executed against both Java generated and Kotlin generated code. The Java
 * module generates compact code, which prints messages with reflection.
 */
class ToStringEscapingTest {
  @Test fun stringFieldsAreEscaped() {
    val bytes = lengthDelimited(1, "a,\nb\u001b".encodeUtf8())
    val text = PizzaDelivery.ADAPTER.decode(bytes).toString()
    assertThat(text).contains("""phone_number=a\,\nb\u001b""")
    assertThat(text).doesNotContain("\n")
    assertThat(text).doesNotContain("\u001b")
  }

  @Test fun repeatedStringFieldsAreEscaped() {
    val bytes = concat(lengthDelimited(1, "x\ry".encodeUtf8()), lengthDelimited(1, "z, w".encodeUtf8()))
    val text = Pizza.ADAPTER.decode(bytes).toString()
    assertThat(text).contains("""toppings=[x\ry, z\, w]""")
    assertThat(text).doesNotContain("\r")
  }

  @Test fun mapKeysAndValuesAreEscaped() {
    val entry = concat(lengthDelimited(1, "k, forged=1\n".encodeUtf8()), lengthDelimited(2, "v}\r".encodeUtf8()))
    val bytes = lengthDelimited(1, entry)
    val text = MapTypes.ADAPTER.decode(bytes).toString()
    assertThat(text).contains("""map_string_string={k\, forged=1\n=v\}\r}""")
    assertThat(text).doesNotContain("\n")
    assertThat(text).doesNotContain("\r")
  }

  @Test fun wrappedStringsAreEscaped() {
    fun wrapped(value: String) = lengthDelimited(1, value.encodeUtf8())
    val bytes = concat(
      lengthDelimited(8, wrapped("a\nb")),
      lengthDelimited(108, wrapped("c\nd")),
      lengthDelimited(108, wrapped("e, f")),
    )
    val text = AllWrappers.ADAPTER.decode(bytes).toString()
    assertThat(text).contains("""string_value=a\nb""")
    assertThat(text).contains("""rep_string_value=[c\nd, e\, f]""")
    assertThat(text).doesNotContain("\n")
  }

  @Test fun structsAreEscaped() {
    val struct = mapOf("k\n1" to "v\n2", "list" to listOf("x\ry"))
    val bytes = concat(
      lengthDelimited(1, ProtoAdapter.STRUCT_MAP.encodeByteString(struct)),
      lengthDelimited(2, ProtoAdapter.STRUCT_LIST.encodeByteString(listOf("a\nb"))),
      lengthDelimited(4, ProtoAdapter.STRUCT_VALUE.encodeByteString("c\nd")),
    )
    val text = AllStructs.ADAPTER.decode(bytes).toString()
    assertThat(text).contains("""struct={k\n1=v\n2, list=[x\ry]}""")
    assertThat(text).contains("""list=[a\nb]""")
    assertThat(text).contains("""value_a=c\nd""")
    assertThat(text).doesNotContain("\n")
    assertThat(text).doesNotContain("\r")
  }

  private fun concat(vararg values: ByteString): ByteString {
    val buffer = Buffer()
    for (value in values) buffer.write(value)
    return buffer.readByteString()
  }

  /** Encodes a length-delimited field. All payloads here are shorter than 128 bytes. */
  private fun lengthDelimited(tag: Int, value: ByteString): ByteString {
    check(value.size < 128)
    val buffer = Buffer()
    var key = (tag shl 3) or 2
    while (key >= 0x80) {
      buffer.writeByte((key and 0x7f) or 0x80)
      key = key ushr 7
    }
    return buffer.writeByte(key).writeByte(value.size).write(value).readByteString()
  }
}
