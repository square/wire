/*
 * Copyright 2019 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire

import com.squareup.wire.protos.person.Person
import com.squareup.wire.protos.person.Person.PhoneNumber
import com.squareup.wire.protos.person.Person.PhoneType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Test

class WireKotlinTest {
  @Test fun unmodifiedMutableListReusesImmutableInstance() {
    val phone = PhoneNumber.Builder().number("555-1212").type(PhoneType.WORK).build()
    val personWithPhone = Person.Builder()
        .id(1)
        .name("Joe Schmoe")
        .phone(listOf(phone))
        .build()
    val personNoPhone = Person.Builder()
        .id(1)
        .name("Joe Schmoe")
        .build()
    try {
      personWithPhone.phone[0] = null
      fail()
    } catch (expected: UnsupportedOperationException) {
    }

    assertThat(personNoPhone.phone).isSameAs(emptyList<Any>())

    // Round-trip these instances through the builder and ensure the lists are the same instances.
    assertThat(personWithPhone.newBuilder().build().phone).isSameAs(personWithPhone.phone)
    assertThat(personNoPhone.newBuilder().build().phone).isSameAs(personNoPhone.phone)
  }
}
