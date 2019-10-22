/*
 * Copyright 2015 Square Inc.
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

import com.squareup.wire.protos.kotlin.person.Person
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class ProtoAdapterTest {
  @Test fun fromClass() {
    val person = Person.Builder()
        .id(99)
        .name("Omar Little")
        .build()
    val encoded = "0a0b4f6d6172204c6974746c651063".decodeHex()
    val personAdapter = ProtoAdapter.get(Person::class.java)
    assertThat(personAdapter.encode(person).toByteString()).isEqualTo(encoded)
    assertThat(personAdapter.decode(encoded)).isEqualTo(person)
  }

  @Test fun fromInstanceSameAsFromClass() {
    val person = Person.Builder()
        .id(99)
        .name("Omar Little")
        .build()
    val instanceAdapter = ProtoAdapter.get(person)
    val classAdapter = ProtoAdapter.get(Person::class.java)
    assertThat(instanceAdapter).isSameAs(classAdapter)
  }
}
