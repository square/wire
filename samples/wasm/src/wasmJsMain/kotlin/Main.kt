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
import human.Person

fun main() {
  val phoneNumber = Person.PhoneNumber.Builder()
    .area("519")
    .number("5550202")
    .build()
  val person = Person.Builder().name("Jacques").phone_number(phoneNumber).build()
  println("Hello, Kotlin/Native! Here is ${person.name} and their number: ${person.phone_number}")
}
