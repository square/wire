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
package com.squareup.wire.kotlin

/** Defines how an protobuf enum type is to be generated. */
enum class EnumMode {
  /** The protobuf enum type will be generated as a Kotlin enum class. */
  ENUM_CLASS,

  /**
   * The protobuf enum type will be generated as a Kotlin sealed class, generated each constant of
   * the enum type as data objects. On top of those constants, the sealed class will contain a
   * `Unrecognized` data class which will contain the real decoded value for this enum if the
   * runtime didn't have any constant matching it.
   */
  SEALED_CLASS,
}
