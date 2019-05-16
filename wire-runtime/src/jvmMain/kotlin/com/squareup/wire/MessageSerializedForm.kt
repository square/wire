/*
 * Copyright 2015 Square Inc.
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
package com.squareup.wire

import java.io.IOException
import java.io.ObjectStreamException
import java.io.Serializable
import java.io.StreamCorruptedException
import kotlin.reflect.KClass

internal actual class MessageSerializedForm<M : Message<M, B>, B : Message.Builder<M, B>>
actual constructor(
  private val bytes: ByteArray,
  messageKClass: KClass<M>
) : Serializable {
  private val messageClass = messageKClass.javaObjectType

  @Throws(ObjectStreamException::class)
  actual fun readResolve(): Any {
    val adapter = ProtoAdapterJvm.get(messageClass)
    try {
      // Extensions will be decoded as unknown values.
      return adapter.decode(bytes)
    } catch (e: IOException) {
      throw StreamCorruptedException(e.message)
    }
  }

  companion object {
    private const val serialVersionUID = 0L
  }
}
