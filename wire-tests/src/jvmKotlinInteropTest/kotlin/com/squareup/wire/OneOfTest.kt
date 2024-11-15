/*
 * Copyright (C) 2015 Square, Inc.
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
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.message
import com.squareup.wire.protos.kotlin.Form
import com.squareup.wire.protos.kotlin.OneOfMessage
import kotlin.test.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class OneOfTest {
  private val INITIAL_BYTES = byteArrayOf()

  // (Tag #1 << 3 | VARINT) = 8.
  private val FOO_BYTES = byteArrayOf(8, 17)

  // (Tag #3 << 3 | LENGTH_DELIMITED) = 26, string length = 6.
  private val BAR_BYTES = byteArrayOf(
    26,
    6,
    'b'.toByte(),
    'a'.toByte(),
    'r'.toByte(),
    'b'.toByte(),
    'a'.toByte(),
    'r'.toByte(),
  )

  private val adapter = OneOfMessage.ADAPTER

  @Test
  fun testOneOf() {
    val builder = OneOfMessage.Builder()
    builder.validate(
      expectedFoo = null,
      expectedBar = null,
      expectedBytes = INITIAL_BYTES,
    )

    builder.foo(17)
    builder.validate(
      expectedFoo = 17,
      expectedBar = null,
      expectedBytes = FOO_BYTES,
    )

    builder.bar("barbar")
    builder.validate(
      expectedFoo = null,
      expectedBar = "barbar",
      expectedBytes = BAR_BYTES,
    )

    builder.foo(17)
    builder.validate(
      expectedFoo = 17,
      expectedBar = null,
      expectedBytes = FOO_BYTES,
    )
  }

  @Test
  fun buildFailsWhenBothFieldsAreNonNull() {
    val builder = OneOfMessage.Builder()
    builder.foo = 1
    builder.bar = "two"
    try {
      builder.build()
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("At most one of foo, bar, baz may be non-null")
    }
  }

  @Test
  fun usingBoxOneOfs() {
    val viaBuilder = Form.Builder()
      .choice(OneOf(Form.CHOICE_TEXT_ELEMENT, Form.TextElement("Hello!")))
      .decision(OneOf(Form.DECISION_D, "Hi!"))
      .build()
    val viaConstructor = Form(
      choice = OneOf(Form.CHOICE_TEXT_ELEMENT, Form.TextElement("Hello!")),
      decision = OneOf(Form.DECISION_D, "Hi!"),
    )

    assertEquals(viaBuilder, viaConstructor)
    assertEquals(viaBuilder.toString(), viaConstructor.toString())

    assertThat(viaBuilder.choice!!.getOrNull(Form.CHOICE_ADDRESS_ELEMENT)).isNull()
    assertThat(viaBuilder.choice.getOrNull(Form.CHOICE_TEXT_ELEMENT))
      .isEqualTo(Form.TextElement("Hello!"))
    assertThat(viaBuilder.decision!!.getOrNull(Form.DECISION_A)).isNull()
    assertThat(viaBuilder.decision.getOrNull(Form.DECISION_D)).isEqualTo("Hi!")
  }

  private fun OneOfMessage.Builder.validate(
    expectedFoo: Int?,
    expectedBar: String?,
    expectedBytes: ByteArray,
  ) {
    // Check builder fields
    assertThat(foo).isEqualTo(expectedFoo)
    assertThat(bar).isEqualTo(expectedBar)

    // Check message fields.
    val message = build()
    assertThat(message.foo).isEqualTo(expectedFoo)
    assertThat(message.bar).isEqualTo(expectedBar)

    // Check serialized bytes.
    val bytes = adapter.encode(message)
    assertThat(expectedBytes).isEqualTo(bytes)

    // Check result of deserialization.
    val newMessage = adapter.decode(bytes)
    assertThat(newMessage.foo).isEqualTo(expectedFoo)
    assertThat(newMessage.bar).isEqualTo(expectedBar)
  }
}
