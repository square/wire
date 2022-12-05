/*
 * Copyright 2020 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.internal

/**
 * Executes the given [block] function on this resource and then closes it down correctly using
 * [close] whether an exception is thrown or not.
 *
 * Differs from the JVM `use` in that it can be called on any type provided that you supply a
 * [close] function.
 *
 * @return the result of [block] function invoked on this resource.
 */
internal inline fun <C, R> C.use(close: (C) -> Unit, block: (C) -> R): R {
  var closed = false

  return try {
    block(this)
  } catch (first: Throwable) {
    try {
      closed = true
      close(this)
    } catch (second: Throwable) {
      first.addSuppressed(second)
    }
    throw first
  } finally {
    if (!closed) {
      close(this)
    }
  }
}
