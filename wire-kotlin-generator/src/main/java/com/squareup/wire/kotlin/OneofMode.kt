/*
 * Copyright (C) 2026 Square, Inc.
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
package com.squareup.wire.kotlin

/** Defines how protobuf oneof types are generated. */
enum class OneofMode {
  /**
   * Each oneof field is generated as a separate nullable property on the message class. If
   * [KotlinTarget.boxOneOfsMinSize] is set, it'll be honored.
   */
  FLAT,

  /**
   * Oneof fields are generated as boxed types. Effectively as if [KotlinTarget.boxOneOfsMinSize]
   * was set to 1.
   */
  BOXED,

  /**
   * Oneof is generated as a nested sealed class with a data class subtype per field. The message
   * holds a single nullable property of the sealed class type.
   *
   * Example for a oneof named `method` with fields `card_id`, `bank_account`, `cash_balance_cents`:
   * ```kotlin
   * sealed class Method {
   *   data class CardId(val value: String) : Method()
   *   data class BankAccount(val value: BankAccount) : Method()
   *   data class CashBalanceCents(val value: Int) : Method()
   * }
   * val method: Method? = null
   * ```
   */
  SEALED_CLASS,
}
