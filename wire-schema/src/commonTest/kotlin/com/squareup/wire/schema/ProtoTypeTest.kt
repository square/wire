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
package com.squareup.wire.schema

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isSameAs
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.test.fail

class ProtoTypeTest {
  @Test
  fun get() {
    assertThat(ProtoType.get("int32")).isSameAs(ProtoType.INT32)
    assertThat(ProtoType.get("Person")).isEqualTo(ProtoType.get("Person"))
    assertThat(ProtoType.get("squareup.protos.person", "Person"))
      .isEqualTo(ProtoType.get("squareup.protos.person.Person"))
  }

  @Test
  fun simpleName() {
    val person = ProtoType.get("squareup.protos.person.Person")
    assertThat(person.simpleName).isEqualTo("Person")
  }

  @Test
  fun scalarToString() {
    assertThat(ProtoType.INT32.toString()).isEqualTo("int32")
    assertThat(ProtoType.STRING.toString()).isEqualTo("string")
    assertThat(ProtoType.BYTES.toString()).isEqualTo("bytes")
  }

  @Test
  fun nestedType() {
    assertThat(ProtoType.get("squareup.protos.person.Person").nestedType("PhoneType"))
      .isEqualTo(ProtoType.get("squareup.protos.person.Person.PhoneType"))
  }

  @Test
  fun primitivesCannotNest() {
    try {
      ProtoType.INT32.nestedType("PhoneType")
      fail()
    } catch (expected: IllegalStateException) {
    }
  }

  @Test
  fun mapsCannotNest() {
    try {
      ProtoType.get("map<string, string>").nestedType("PhoneType")
      fail()
    } catch (expected: IllegalStateException) {
    }
  }

  @Test
  fun mapFormat() {
    try {
      ProtoType.get("map<string>")
      fail()
    } catch (e: IllegalArgumentException) {
      assertThat(e).hasMessage("expected ',' in map type: map<string>")
    }
  }

  @Test
  fun mapKeyScalarType() {
    try {
      ProtoType.get("map<bytes, string>")
      fail()
    } catch (expected: IllegalArgumentException) {
    }

    try {
      ProtoType.get("map<double, string>")
      fail()
    } catch (expected: IllegalArgumentException) {
    }

    try {
      ProtoType.get("map<float, string>")
      fail()
    } catch (expected: IllegalArgumentException) {
    }

    try {
      ProtoType.get("map<some.Message, string>")
      fail()
    } catch (expected: IllegalArgumentException) {
    }
  }

  @Test
  fun messageToString() {
    val person = ProtoType.get("squareup.protos.person.Person")
    assertThat(person.toString()).isEqualTo("squareup.protos.person.Person")

    val phoneType = person.nestedType("PhoneType")
    assertThat(phoneType.toString()).isEqualTo("squareup.protos.person.Person.PhoneType")
  }

  @Test
  fun enclosingTypeOrPackage() {
    assertThat(ProtoType.STRING.enclosingTypeOrPackage).isNull()

    val person = ProtoType.get("squareup.protos.person.Person")
    assertThat(person.enclosingTypeOrPackage).isEqualTo("squareup.protos.person")

    val phoneType = person.nestedType("PhoneType")
    assertThat(phoneType.enclosingTypeOrPackage).isEqualTo("squareup.protos.person.Person")
  }

  @Test
  fun isScalar() {
    assertThat(ProtoType.INT32.isScalar).isTrue()
    assertThat(ProtoType.STRING.isScalar).isTrue()
    assertThat(ProtoType.BYTES.isScalar).isTrue()
    assertThat(ProtoType.get("squareup.protos.person.Person").isScalar).isFalse()
  }
}
