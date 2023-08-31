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
@file:OptIn(ExperimentalForeignApi::class)

package com.squareup.wire

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.posix.memcpy

/**
 * Read an encoded message from `data`.
 *
 * Note: this method is marked with [ExperimentalUnsignedTypes] annotation and requires an opt-in
 * (e.g. through `@OptIn(ExperimentalUnsignedTypes::class)` to be used.
 *
 * @throws IllegalArgumentException if `data.length` is larger than [Int.MAX_VALUE].
 */
@ExperimentalUnsignedTypes
fun <E> ProtoAdapter<E>.decode(data: NSData): E {
  require(data.length <= Int.MAX_VALUE.toULong()) {
    "Can't decode data with length ${data.length}, maximum length is ${Int.MAX_VALUE}."
  }
  val bytes = ByteArray(data.length.toInt()).apply {
    usePinned { pinned ->
      memcpy(pinned.addressOf(0), data.bytes, data.length)
    }
  }
  return decode(bytes)
}
