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
package com.squareup.wire.schema.internal

internal actual fun Char.isDigit() = this in '0'..'9'

internal actual fun String.toEnglishLowerCase() = lowercase()

actual interface MutableQueue<T : Any> : MutableCollection<T> {
  actual fun poll(): T?
}

internal actual fun <T : Any> mutableQueueOf(): MutableQueue<T> {
  val queue = mutableListOf<T>()
  return object : MutableQueue<T>, MutableCollection<T> by queue {
    override fun poll() = firstOrNull()
  }
}
