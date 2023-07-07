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

/** Syntax version. */
enum class Syntax(private val string: String) {
  PROTO_2("proto2"),
  PROTO_3("proto3"),
  ;

  override fun toString(): String = string

  companion object {
    operator fun get(string: String): Syntax {
      for (syntax in values()) {
        if (syntax.string == string) return syntax
      }
      throw IllegalArgumentException("unexpected syntax: $string")
    }
  }
}
