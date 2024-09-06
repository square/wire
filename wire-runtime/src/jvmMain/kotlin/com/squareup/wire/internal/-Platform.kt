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

import java.util.Collections

actual typealias Serializable = java.io.Serializable

actual typealias JvmDefaultWithCompatibility = kotlin.jvm.JvmDefaultWithCompatibility

actual typealias JvmField = kotlin.jvm.JvmField

actual typealias JvmSynthetic = kotlin.jvm.JvmSynthetic

actual typealias JvmStatic = kotlin.jvm.JvmStatic

actual typealias ObjectStreamException = java.io.ObjectStreamException

actual typealias ProtocolException = java.net.ProtocolException

@Suppress("NOTHING_TO_INLINE") // Syntactic sugar.
actual inline fun <T> MutableList<T>.toUnmodifiableList(): List<T> =
  Collections.unmodifiableList(this)

@Suppress("NOTHING_TO_INLINE") // Syntactic sugar.
actual inline fun <K, V> MutableMap<K, V>.toUnmodifiableMap(): Map<K, V> =
  Collections.unmodifiableMap(this)

actual fun camelCase(string: String, upperCamel: Boolean): String {
  return buildString(string.length) {
    var index = 0
    var uppercase = upperCamel
    while (index < string.length) {
      var codePoint = string.codePointAt(index)

      index += Character.charCount(codePoint)

      if (codePoint == '_'.code) {
        uppercase = true
        continue
      }
      if (uppercase) {
        if (codePoint in 'a'.code..'z'.code) codePoint += 'A' - 'a'
      }
      appendCodePoint(codePoint)
      uppercase = false
    }
  }
}
