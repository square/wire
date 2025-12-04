/*
 * Copyright (C) 2023 Square, Inc.
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
import com.squareup.moshi.Moshi
import com.squareup.wire.json.assertJsonEquals
import com.squareup.wire.proto2.kotlin.Getters
import com.squareup.wire.proto2.person.java.Person as JavaPerson
import com.squareup.wire.proto2.person.javainteropkotlin.Person as JavaInteropKotlinPerson
import com.squareup.wire.proto2.person.kotlin.Person as KotlinPerson
import com.squareup.wire.proto2.person.kotlin.Person.PhoneNumber
import org.junit.Test

class MoshiPersonTest {
  /**
   * When we encounter an explicit null in the JSON, we discard it. As a consequence we won't
   * overwrite a non-null value with null which could happen in malformed JSON that repeats a value.
   */
  @Test
  fun javaClobberNonNullWithNull() {
    val personWithName = moshi.adapter(JavaPerson::class.java)
      .fromJson("""{"id":1,"name":"Jo","email":"foo@square.com"}""")
    assertThat(personWithName!!.email).isEqualTo("foo@square.com")

    val personWithNameClobberedWithNull = moshi.adapter(JavaPerson::class.java)
      .fromJson("""{"id":1,"name":"Jo","email":"foo@square.com","email":null}""")
    assertThat(personWithNameClobberedWithNull!!.email).isEqualTo("foo@square.com")
  }

  @Test
  fun kotlinClobberNonNullWithNull() {
    val personWithName = moshi.adapter(KotlinPerson::class.java)
      .fromJson("""{"id":1,"name":"Jo","email":"foo@square.com"}""")
    assertThat(personWithName!!.email).isEqualTo("foo@square.com")

    val personWithNameClobberedWithNull = moshi.adapter(KotlinPerson::class.java)
      .fromJson("""{"id":1,"name":"Jo","email":"foo@square.com","email":null}""")
    assertThat(personWithNameClobberedWithNull!!.email).isEqualTo("foo@square.com")
  }

  @Test
  fun kotlinWithoutBuilderFromJson() {
    val personWithName = moshi.adapter(KotlinPerson::class.java)
      .fromJson(
        """
        |{
        |  "id": 1,
        |  "name": "Jo",
        |  "email": "foo@square.com",
        |  "phone": [
        |    {"number": "555-555-5555"},
        |    {"number": "444-444-4444"}
        |  ],
        |  "favorite_numbers": [1, 2, 3],
        |  "area_numbers": {
        |    "519": "555-5555"
        |  },
        |  "is_canadian": true
        |}
        """.trimMargin(),
      )
    assertThat(personWithName)
      .isEqualTo(
        KotlinPerson(
          id = 1,
          name = "Jo",
          email = "foo@square.com",
          phone = listOf(PhoneNumber("555-555-5555"), PhoneNumber("444-444-4444")),
          favorite_numbers = listOf(1, 2, 3),
          area_numbers = mapOf(519 to "555-5555"),
          is_canadian = true,
        ),
      )
  }

  @Test
  fun kotlinWithoutBuilderToJson() {
    val json = moshi.adapter(KotlinPerson::class.java)
      .toJson(
        KotlinPerson(
          id = 1,
          name = "Jo",
          email = "foo@square.com",
          phone = listOf(PhoneNumber("555-555-5555"), PhoneNumber("444-444-4444")),
          favorite_numbers = listOf(1, 2, 3),
          area_numbers = mapOf(519 to "555-5555"),
          is_canadian = true,
        ),
      )
    assertJsonEquals(
      """
      |{
      |  "id": 1,
      |  "name": "Jo",
      |  "email": "foo@square.com",
      |  "phone": [
      |    {"number": "555-555-5555"},
      |    {"number": "444-444-4444"}
      |  ],
      |  "favorite_numbers": [1, 2, 3],
      |  "area_numbers": {
      |    "519": "555-5555"
      |  },
      |  "is_canadian": true
      |}
      """.trimMargin(),
      json,
    )
  }

  @Test
  fun javaInteropKotlinClobberNonNullWithNull() {
    val personWithName = moshi.adapter(JavaInteropKotlinPerson::class.java)
      .fromJson("{\"id\":1,\"name\":\"Jo\",\"email\":\"foo@square.com\"}")
    assertThat(personWithName!!.email).isEqualTo("foo@square.com")

    val personWithNameClobberedWithNull = moshi.adapter(JavaInteropKotlinPerson::class.java)
      .fromJson("{\"id\":1,\"name\":\"Jo\",\"email\":\"foo@square.com\",\"email\":null}")
    assertThat(personWithNameClobberedWithNull!!.email).isEqualTo("foo@square.com")
  }

  @Test
  fun kotlinGettersFromJson() {
    val getters = moshi.adapter(Getters::class.java)
      .fromJson("""{"isa":1,"isA":2,"is_a":3,"is32":32,"isb":true}""")
    assertThat(getters).isEqualTo(Getters(isa = 1, isA = 2, is_a = 3, is32 = 32, isb = true))
  }

  @Test
  fun kotlinGettersToJson() {
    val getters = moshi.adapter(Getters::class.java)
      .toJson(Getters(isa = 1, isA = 2, is_a = 3, is32 = 32, isb = true))
    assertThat(getters).isEqualTo("""{"isa":1,"isA":2,"is_a":3,"is32":32,"isb":true}""")
  }

  companion object {
    private val moshi = Moshi.Builder()
      .add(WireJsonAdapterFactory())
      .build()
  }
}
