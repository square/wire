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

import java.util.Collections

actual typealias Serializable = java.io.Serializable

actual typealias Throws = kotlin.jvm.Throws

actual typealias ObjectStreamException = java.io.ObjectStreamException

actual typealias ProtocolException = java.net.ProtocolException

actual fun <T> MutableList<T>.toUnmodifiableList(): List<T> = Collections.unmodifiableList(this)

actual fun <K, V> MutableMap<K, V>.toUnmodifiableMap(): Map<K, V> =
    Collections.unmodifiableMap(this)

internal actual fun String.format(vararg args: Any?): String = String.format(this, args)

internal actual fun Int.intBitsToFloat(): Float = java.lang.Float.intBitsToFloat(this)

internal actual fun Long.longBitsToDouble(): Double = java.lang.Double.longBitsToDouble(this)
