/*
 * Copyright (C) 2019 Square, Inc.
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
package com.squareup.wire.internal

import okio.IOException

actual interface Serializable

/** This annotation is an empty placeholder. */
actual annotation class JvmField

/** This annotation is an empty placeholder. */
actual annotation class JvmSynthetic

/** This annotation is an empty placeholder. */
actual annotation class JvmStatic

actual abstract class ObjectStreamException : IOException()

actual class ProtocolException actual constructor(host: String) : IOException(host)

@Suppress("NOTHING_TO_INLINE") // Syntactic sugar.
actual inline fun <T> MutableList<T>.toUnmodifiableList(): List<T> = this

@Suppress("NOTHING_TO_INLINE") // Syntactic sugar.
actual inline fun <K, V> MutableMap<K, V>.toUnmodifiableMap(): Map<K, V> = this

// TODO: Use code points to process each char.
actual fun camelCase(string: String, upperCamel: Boolean): String {
  return buildString(string.length) {
    var index = 0
    var uppercase = upperCamel
    while (index < string.length) {
      var char = string[index]

      index++

      if (char == '_') {
        uppercase = true
        continue
      }
      if (uppercase) {
        if (char in 'a'..'z') char += 'A' - 'a'
      }
      append(char)
      uppercase = false
    }
  }
}
