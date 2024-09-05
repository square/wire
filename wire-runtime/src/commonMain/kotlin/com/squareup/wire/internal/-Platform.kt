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

expect interface Serializable

@OptionalExpectation
@OptIn(ExperimentalMultiplatform::class)
expect annotation class JvmDefaultWithCompatibility()

expect annotation class JvmField()

expect annotation class JvmSynthetic()

expect annotation class JvmStatic()

expect abstract class ObjectStreamException : IOException

expect class ProtocolException(host: String) : IOException

expect fun <T> MutableList<T>.toUnmodifiableList(): List<T>

expect fun <K, V> MutableMap<K, V>.toUnmodifiableMap(): Map<K, V>

/**
 * Convert [string], from snake case to camel case.
 *
 * When [upperCamel] is true, this should mirror the logic of `fieldNameToJsonName` in
 * `com/google/protobuf/Descriptors.java`, except it lower cases the first character as well (protoc
 * seems to do this step earlier).
 *
 * See https://github.com/protocolbuffers/protobuf/blob/master/java/core/src/main/java/com/google/protobuf/Descriptors.java
 *
 * @param upperCamel true to uppercase the first character.
 */
expect fun camelCase(string: String, upperCamel: Boolean = false): String
