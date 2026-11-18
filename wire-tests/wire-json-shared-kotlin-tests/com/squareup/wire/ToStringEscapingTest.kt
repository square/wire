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

  private fun concat(vararg values: ByteString): ByteString {
    val buffer = Buffer()
    for (value in values) buffer.write(value)
    return buffer.readByteString()
  }

  /** Encodes a length-delimited field. All payloads here are shorter than 128 bytes. */
  private fun lengthDelimited(tag: Int, value: ByteString): ByteString {
    check(value.size < 128)
    return Buffer()
      .writeByte((tag shl 3) or 2)
      .writeByte(value.size)
      .write(value)
      .readByteString()
  }
}
