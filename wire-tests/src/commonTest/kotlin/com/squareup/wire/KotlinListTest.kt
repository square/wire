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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class KotlinListTest {
  @Test fun listsAreImmutable() {
    val list = mutableListOf("a", "b")

    val person = Person(id = 0, name = "name", aliases = list)
    if (person.aliases is MutableList<*>) {
      try {
        (person.aliases as MutableList<*>).clear()
        fail()
      } catch (_: UnsupportedOperationException) {
        // Mutation failed as expected.
      }
    }

    // Mutate the values used to create the map. Wire should have defensive copies.
    list.clear()
    assertEquals(listOf("a", "b"), person.aliases)
  }
}
