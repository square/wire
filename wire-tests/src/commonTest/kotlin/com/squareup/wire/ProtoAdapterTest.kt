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

import assertk.assertions.isTrue
import com.squareup.wire.proto3.kotlin.person.Person
import com.squareup.wire.protos.kotlin.bool.TrueBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import okio.ByteString.Companion.decodeHex
import squareup.protos.packed_encoding.EmbeddedMessage
import squareup.protos.packed_encoding.OuterMessage

class ProtoAdapterTest {
  @Test fun repeatedHelpersCacheInstances() {
    val adapter: ProtoAdapter<*> = ProtoAdapter.UINT64
    assertSame(adapter.asRepeated(), adapter.asRepeated())
    assertSame(adapter.asPacked(), adapter.asPacked())
  }

  /** https://github.com/square/wire/issues/541  */
  @Test fun embeddedEmptyPackedMessage() {
    val outerMessage = OuterMessage(
      outer_number_before = 2,
      embedded_message = EmbeddedMessage(inner_number_after = 1),
    )
    val outerMessagesAfterSerialisation = OuterMessage.ADAPTER
      .decode(OuterMessage.ADAPTER.encode(outerMessage))
    assertEquals(outerMessagesAfterSerialisation, outerMessage)
  }

  @Test fun getFromClassProto3() {
    val person = Person(
      name = "Somebody",
      phones = listOf(Person.PhoneNumber()),
    )
    val hexByteString = "0a08536f6d65626f64792200"
    assertEquals(hexByteString, Person.ADAPTER.encodeByteString(person).hex())
    assertEquals(person, Person.ADAPTER.decode(hexByteString.decodeHex()))
  }

  @Test fun lenientBooleanParsing() {
    // 0 is false, the rest is true.
    assertEquals(false, TrueBoolean.ADAPTER.decode("0800".decodeHex()).isTrue)
    assertEquals(true, TrueBoolean.ADAPTER.decode("0801".decodeHex()).isTrue)
    assertEquals(true, TrueBoolean.ADAPTER.decode("0802".decodeHex()).isTrue)
  }
}
