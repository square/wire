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

/**
 * Bytes sent over the network to represent a message can contain fields, or values (enum constants)
 * that your application doesn't know about. Any values that `Wire` wasn't able to decode are stored
 * in the [Message.unknownFields] field. Moreover, in all cases, their value is bound to a field
 * tag. An [UnknownValueReader] will let you extract the value attached to a tag embedded inside
 * [Message.unknownFields].
 *
 * For instance, an enum has two constants `DEFAULT = 0`, and `BUNNY = 1` but the bytes from the
 * network contains another value (such as 2) that your application doesn't know about and cannot be
 * decoded at runtime. [UnknownValueReader] will extract this value for you and return the unknown
 * constant's value, which is 2.
 *
 * For single fields, use [SingleUnknownValueReader]. For repeated fields, use
 * [RepeatedUnknownValueReader].
 * For Kotlin, on the JVM, we offer extension function on `KProperty1`.
 * Say we have the following proto schema which has been generated into Kotlin.
 * ```protobuf
 * message Easter {
 *   EasterAnimal easter_animal = 1;
 *   repeated EasterAnimal easter_animals = 2;
 * }
 *
 * enum EasterAnimal {
 *   DEFAULT = 0;
 *   BUNNY = 1;
 * }
 * ```
 * For the single field `easter_animal`, you can get a reader with the following:
 * ```kotlin
 * val easter: Easter = ...
 * val reader = Easter::easter_animal.unknownValueReader()
 * // We read the unknown value.
 * val unknownValue = reader.read(easter)
 * ```
 * For the repeated field `easter_animals`, you can get a reader with the following:
 * ```kotlin
 * val easter: Easter = ...
 * val reader = Easter::easter_animals.unknownValuesReader()
 * // We read the unknown values.
 * val unknownValues = reader.read(easter)
 * ```
 */
interface UnknownValueReader<M : Message<M, *>, V> {
  fun read(message: M): V
}
