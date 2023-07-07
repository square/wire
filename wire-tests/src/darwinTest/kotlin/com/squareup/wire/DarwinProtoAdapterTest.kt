/*
 * Copyright (C) 2020 Square, Inc.
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
import com.squareup.wire.protos.kotlin.person.Person.PhoneNumber
import com.squareup.wire.protos.kotlin.person.Person.PhoneType.WORK
import kotlin.test.Test
import kotlin.test.assertEquals
import platform.Foundation.NSData
import platform.Foundation.create

class DarwinProtoAdapterTest {
  @Test fun decodeNSData() {
    val expected = Person(
      name = "Alice",
      id = 1,
      phone = listOf(PhoneNumber(number = "+15551234567", type = WORK)),
      email = "alice@squareup.com",
    )
    val data = NSData.create(
      base64Encoding = "CgVBbGljZRABGhJhbGljZUBzcXVhcmV1cC5jb20iEAoMKzE1NTUxMjM0NTY3EAI=",
    )!!
    assertEquals(expected, Person.ADAPTER.decode(data))
  }
}
