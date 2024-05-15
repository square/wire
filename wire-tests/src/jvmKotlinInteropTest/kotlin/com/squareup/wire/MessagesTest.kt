/*
 * Copyright (C) 2024 Square, Inc.
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

import okio.ByteString.Companion.decodeHex
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import squareup.Easter
import squareup.EasterAnimal.BUNNY
import squareup.EasterAnimal.EASTER_ANIMAL_DEFAULT
import squareup.EasterAnimal.HEN

class MessagesTest {
  private val optionalEasterAnimalReader = Easter::optional_easter_animal.unknownValueReader()
  private val identityEasterAnimalReader = Easter::identity_easter_animal.unknownValueReader()
  private val easterAnimalsReader = Easter::easter_animals.unknownValuesReader()
  private val easterAnimalsPackedReader = Easter::easter_animals_packed.unknownValuesReader()

  @Test fun readUnknownEnumTagReturnsValuesIfValueIsUnknown() {
    // ┌─ 2: 5
    // ├─ 3: 6
    // ├─ 4: 7
    // ├─ 4: 2
    // ├─ 4: 8
    // ├─ 5: 9
    // ╰- 5: 4
    val bytes = "2a02090410051806200720022008"
    val wireMessage = Easter.ADAPTER.decode(bytes.decodeHex())
    assertThat(wireMessage).isEqualTo(
      Easter(
        optional_easter_animal = null,
        identity_easter_animal = EASTER_ANIMAL_DEFAULT,
        // Only value from the list that we know.
        easter_animals = listOf(HEN),
        easter_animals_packed = listOf(),
        unknownFields = wireMessage.unknownFields,
      ),
    )

    assertThat(optionalEasterAnimalReader.read(wireMessage)).isEqualTo(5)
    assertThat(identityEasterAnimalReader.read(wireMessage)).isEqualTo(6)
    assertThat(easterAnimalsReader.read(wireMessage)).isEqualTo(listOf(7, 8))
    assertThat(easterAnimalsPackedReader.read(wireMessage)).isEqualTo(listOf(9, 4))
  }

  @Test fun readUnknownEnumTagReturnsNullIfValueAbsent() {
    val bytes = ""
    val wireMessage = Easter.ADAPTER.decode(bytes.decodeHex())
    assertThat(wireMessage).isEqualTo(
      Easter(
        optional_easter_animal = null,
        identity_easter_animal = EASTER_ANIMAL_DEFAULT,
        easter_animals = listOf(),
        easter_animals_packed = listOf(),
      ),
    )

    assertThat(optionalEasterAnimalReader.read(wireMessage)).isNull()
    assertThat(identityEasterAnimalReader.read(wireMessage)).isNull()
    assertThat(easterAnimalsReader.read(wireMessage)).isEmpty()
    assertThat(easterAnimalsPackedReader.read(wireMessage)).isEmpty()
  }

  @Test fun readUnknownEnumTagReturnsNullIfValueIsKnown() {
    // ┌─ 2: 2
    // ├─ 3: 1
    // ├─ 4: 2
    // ├─ 4: 1
    // ├─ 4: 2
    // ├─ 5: 1
    // ├─ 5: 2
    // ╰- 5: 1
    val bytes = "100218012002200120022a03010201"
    val wireMessage = Easter.ADAPTER.decode(bytes.decodeHex())
    assertThat(wireMessage).isEqualTo(
      Easter(
        optional_easter_animal = HEN,
        identity_easter_animal = BUNNY,
        easter_animals = listOf(HEN, BUNNY, HEN),
        easter_animals_packed = listOf(BUNNY, HEN, BUNNY),
      ),
    )

    assertThat(optionalEasterAnimalReader.read(wireMessage)).isNull()
    assertThat(identityEasterAnimalReader.read(wireMessage)).isNull()
    assertThat(easterAnimalsReader.read(wireMessage)).isEmpty()
    assertThat(easterAnimalsPackedReader.read(wireMessage)).isEmpty()
  }
}
