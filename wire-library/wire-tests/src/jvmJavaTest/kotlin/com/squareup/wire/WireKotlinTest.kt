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

import com.squareup.wire.protos.custom_options.FooBar
import com.squareup.wire.protos.custom_options.MessageWithOptions
import com.squareup.wire.protos.custom_options.MyFieldOptionOne
import com.squareup.wire.protos.custom_options.MyFieldOptionThree
import com.squareup.wire.protos.custom_options.MyFieldOptionTwo
import com.squareup.wire.protos.custom_options.MyMessageOptionFour
import com.squareup.wire.protos.custom_options.MyMessageOptionTwo
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

  @Test fun optionsOnMessageType() {
    val myMessageOptionTwo = MessageWithOptions::class.annotations
        .filterIsInstance<MyMessageOptionTwo>()
        .first()
    assertThat(myMessageOptionTwo.value).isEqualTo(91011.0f)
    val myMessageOptionFour = MessageWithOptions::class.annotations
        .filterIsInstance<MyMessageOptionFour>()
        .first()
    assertThat(myMessageOptionFour.value).isEqualTo(FooBar.FooBarBazEnum.FOO)
  }

  @Test fun optionsOnField() {
    val myFieldOptionOne = FooBar::class.members.first { it.name == "foo" }.annotations
        .filterIsInstance<MyFieldOptionOne>()
        .first()
    assertThat(myFieldOptionOne.value).isEqualTo(17)
    val myFieldOptionTwo = FooBar::class.members.first { it.name == "bar" }.annotations
        .filterIsInstance<MyFieldOptionTwo>()
        .first()
    assertThat(myFieldOptionTwo.value).isEqualTo(33.5f)
    val myFieldOptionThree = FooBar::class.members.first { it.name == "baz" }.annotations
        .filterIsInstance<MyFieldOptionThree>()
        .first()
    assertThat(myFieldOptionThree.value).isEqualTo(FooBar.FooBarBazEnum.BAR)
  }
}
