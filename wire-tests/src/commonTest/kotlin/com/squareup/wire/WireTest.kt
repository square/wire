/*
 * Copyright (C) 2019 Square, Inc.
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
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.squareup.wire.protos.kotlin.person.Person
import com.squareup.wire.protos.kotlin.simple.ExternalMessage
import com.squareup.wire.protos.kotlin.simple.SimpleMessage
import kotlin.test.Test

class WireTest {
  @Test fun simpleMessage() {
    var msg = SimpleMessage(required_int32 = 456)
    assertThat(msg.optional_int32).isNull()
    assertThat(msg.optional_nested_msg).isNull()
    assertThat(msg.optional_external_msg).isNull()
    assertThat(msg.default_nested_enum).isNull()
    assertThat(msg.required_int32).isEqualTo(456)
    assertThat(msg.repeated_double).isNotNull()
    assertThat(msg.repeated_double.size).isEqualTo(0)

    val doubles = listOf(1.0, 2.0, 3.0)
    msg = msg.copy(
      optional_int32 = 789,
      optional_nested_msg = SimpleMessage.NestedMessage(bb = 2),
      optional_external_msg = ExternalMessage(f = 99.9f),
      default_nested_enum = SimpleMessage.NestedEnum.BAR,
      required_int32 = 456,
      repeated_double = doubles,
    )

    assertThat(msg.optional_int32).isEqualTo(789)
    assertThat(msg.optional_nested_msg!!.bb).isEqualTo(2)
    assertFloatEquals(99.9f, msg.optional_external_msg!!.f!!)
    assertThat(msg.default_nested_enum).isEqualTo(SimpleMessage.NestedEnum.BAR)
    assertThat(msg.required_int32).isEqualTo(456)
    assertThat(msg.repeated_double).isEqualTo(doubles)

    // Rebuilding will use the new list

    msg = msg.copy()
    assertThat(msg.repeated_double).isEqualTo(doubles)

    val adapter = SimpleMessage.ADAPTER
    val result = adapter.encode(msg)
    assertThat(result.size).isEqualTo(46)
    val newMsg = adapter.decode(result)
    assertThat(newMsg.optional_int32).isEqualTo(789)
    assertThat(newMsg.optional_nested_msg!!.bb).isEqualTo(2)
    assertFloatEquals(99.9f, newMsg.optional_external_msg!!.f!!)
    assertThat(newMsg.default_nested_enum).isEqualTo(SimpleMessage.NestedEnum.BAR)
    assertThat(newMsg.required_int32).isEqualTo(456)
    assertThat(msg.repeated_double).isEqualTo(doubles)
  }

  @Test fun sanitizedToString() {
    val person = Person(
      id = 1,
      name = "Such, I mean it, such [a] {funny} name.",
      phone = listOf(Person.PhoneNumber(number = "123,456,789")),
      aliases = listOf("B-lo,ved", "D{esperado}"),
    )
    val expected = """Person{
          |id=1
          |, name=Such\, I mean it\, such \[a\] \{funny\} name.
          |, phone=[PhoneNumber{number=123\,456\,789}]
          |, aliases=[B-lo\,ved, D\{esperado\}]
          |}
    """.trimMargin().replace("\n", "")
    assertThat(person.toString()).isEqualTo(expected)
  }
}
