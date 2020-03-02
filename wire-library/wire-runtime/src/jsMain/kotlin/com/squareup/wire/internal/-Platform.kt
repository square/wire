/*
 * Copyright 2019 Square Inc.
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

import okio.IOException
import kotlin.reflect.KClass

actual interface Serializable

actual abstract class ObjectStreamException : IOException()

actual class ProtocolException actual constructor(host: String) : IOException(host)

@Suppress("NOTHING_TO_INLINE") // Syntactic sugar.
actual inline fun <T> MutableList<T>.toUnmodifiableList(): List<T> = this

@Suppress("NOTHING_TO_INLINE") // Syntactic sugar.
actual inline fun <K, V> MutableMap<K, V>.toUnmodifiableMap(): Map<K, V> = this
