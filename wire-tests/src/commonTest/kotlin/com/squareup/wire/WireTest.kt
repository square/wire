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

import com.squareup.wire.protos.kotlin.person.Person
import com.squareup.wire.protos.kotlin.simple.ExternalMessage
import com.squareup.wire.protos.kotlin.simple.SimpleMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WireTest {
  @Test fun simpleMessage() {
    var msg = SimpleMessage(required_int32 = 456)
    assertNull(msg.optional_int32)
    assertNull(msg.optional_nested_msg)
    assertNull(msg.optional_external_msg)
    assertNull(msg.default_nested_enum)
    assertEquals(456, msg.required_int32)
    assertNotNull(msg.repeated_double)
    assertEquals(0, msg.repeated_double.size)

    val doubles = listOf(1.0, 2.0, 3.0)
    msg = msg.copy(
      optional_int32 = 789,
      optional_nested_msg = SimpleMessage.NestedMessage(bb = 2),
      optional_external_msg = ExternalMessage(f = 99.9f),
      default_nested_enum = SimpleMessage.NestedEnum.BAR,
      required_int32 = 456,
      repeated_double = doubles,
    )

    assertEquals(789, msg.optional_int32)
    assertEquals(2, msg.optional_nested_msg!!.bb)
    assertFloatEquals(99.9f, msg.optional_external_msg!!.f!!)
    assertEquals(SimpleMessage.NestedEnum.BAR, msg.default_nested_enum)
    assertEquals(456, msg.required_int32)
    assertEquals(doubles, msg.repeated_double)

    // Rebuilding will use the new list

    msg = msg.copy()
    assertEquals(doubles, msg.repeated_double)

    val adapter = SimpleMessage.ADAPTER
    val result = adapter.encode(msg)
    assertEquals(46, result.size)
    val newMsg = adapter.decode(result)
    assertEquals(789, newMsg.optional_int32)
    assertEquals(2, newMsg.optional_nested_msg!!.bb)
    assertFloatEquals(99.9f, newMsg.optional_external_msg!!.f!!)
    assertEquals(SimpleMessage.NestedEnum.BAR, newMsg.default_nested_enum)
    assertEquals(456, newMsg.required_int32)
    assertEquals(doubles, msg.repeated_double)
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
    assertEquals(expected, person.toString())
  }
}
