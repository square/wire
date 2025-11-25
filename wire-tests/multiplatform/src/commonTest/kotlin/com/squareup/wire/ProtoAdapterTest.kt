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
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import com.squareup.wire.proto3.kotlin.person.Person
import com.squareup.wire.protos.kotlin.bool.TrueBoolean
import kotlin.test.Test
import okio.ByteString.Companion.decodeHex
import squareup.protos.packed_encoding.EmbeddedMessage
import squareup.protos.packed_encoding.OuterMessage

class ProtoAdapterTest {
  @Test fun repeatedHelpersCacheInstances() {
    val adapter: ProtoAdapter<*> = ProtoAdapter.UINT64
    assertThat(adapter.asRepeated()).isSameInstanceAs(adapter.asRepeated())
    assertThat(adapter.asPacked()).isSameInstanceAs(adapter.asPacked())
  }

  /** https://github.com/square/wire/issues/541  */
  @Test fun embeddedEmptyPackedMessage() {
    val outerMessage = OuterMessage(
      outer_number_before = 2,
      embedded_message = EmbeddedMessage(inner_number_after = 1),
    )
    val outerMessagesAfterSerialisation = OuterMessage.ADAPTER
      .decode(OuterMessage.ADAPTER.encode(outerMessage))
    assertThat(outerMessage).isEqualTo(outerMessagesAfterSerialisation)
  }

  @Test fun getFromClassProto3() {
    val person = Person(
      name = "Somebody",
      phones = listOf(Person.PhoneNumber()),
    )
    val hexByteString = "0a08536f6d65626f64792200"
    assertThat(Person.ADAPTER.encodeByteString(person).hex()).isEqualTo(hexByteString)
    assertThat(Person.ADAPTER.decode(hexByteString.decodeHex())).isEqualTo(person)
  }

  @Test fun lenientBooleanParsing() {
    // 0 is false, the rest is true.
    assertThat(TrueBoolean.ADAPTER.decode("0800".decodeHex()).isTrue).isEqualTo(false)
    assertThat(TrueBoolean.ADAPTER.decode("0801".decodeHex()).isTrue).isEqualTo(true)
    assertThat(TrueBoolean.ADAPTER.decode("0802".decodeHex()).isTrue).isEqualTo(true)
  }
}
