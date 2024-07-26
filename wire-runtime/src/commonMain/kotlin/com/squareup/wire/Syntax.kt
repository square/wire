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

/** Syntax and edition version. */
sealed class Syntax(internal val string: String) {
  data object PROTO_2 : Syntax("proto2") {
    override fun toString(): String = string
  }
  data object PROTO_3 : Syntax("proto3") {
    override fun toString(): String = string
  }
  data class Edition(val value: String) : Syntax(value) {
    override fun toString(): String = value
  }

  // Created for backward capability with when Syntax used to be an enum.
  fun name(): String {
    return when (this) {
      // TODO(Benoit) Edition needs to return something like `Edition(<value>)`.
      is Edition,
      PROTO_2,
      -> "PROTO_2"
      PROTO_3 -> "PROTO_3"
    }
  }

  companion object {
    @Deprecated("Use get(string: String, edition: Boolean) instead", ReplaceWith("Syntax.get(string, edition)"))
    operator fun get(string: String): Syntax {
      if (string == PROTO_2.toString()) return PROTO_2
      if (string == PROTO_3.toString()) return PROTO_3
      throw IllegalArgumentException("unexpected syntax: $string")
    }

    fun get(string: String, edition: Boolean): Syntax {
      if (edition) {
        if (string in KNOWN_EDITIONS) return Edition(string)
        throw IllegalArgumentException("unknown edition: $string")
      } else {
        if (string == PROTO_2.toString()) return PROTO_2
        if (string == PROTO_3.toString()) return PROTO_3
        throw IllegalArgumentException("unexpected syntax: $string")
      }
    }

    private val KNOWN_EDITIONS = listOf(
      "2023",
      "2024",
    )
  }
}
