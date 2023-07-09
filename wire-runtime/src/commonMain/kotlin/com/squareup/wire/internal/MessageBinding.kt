/*
 * Copyright (C) 2021 Square, Inc.
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

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Syntax
import kotlin.reflect.KClass
import okio.ByteString

/**
 * A representation of a message and its builder class. Typically these are generated subtypes of
 * [com.squareup.wire.Message] and [com.squareup.wire.Message.Builder].
 */
interface MessageBinding<M : Any, B : Any> {
  val messageType: KClass<in M>

  val fields: Map<Int, FieldOrOneOfBinding<M, B>>

  val typeUrl: String?

  val syntax: Syntax

  fun unknownFields(message: M): ByteString

  fun getCachedSerializedSize(message: M): Int

  fun setCachedSerializedSize(message: M, size: Int)

  fun newBuilder(): B

  fun build(builder: B): M

  fun addUnknownField(builder: B, tag: Int, fieldEncoding: FieldEncoding, value: Any?)

  fun clearUnknownFields(builder: B)
}
