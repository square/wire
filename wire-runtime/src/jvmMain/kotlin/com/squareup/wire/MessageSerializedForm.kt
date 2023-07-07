/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.wire

import java.io.IOException
import java.io.ObjectStreamException
import java.io.Serializable
import java.io.StreamCorruptedException

internal class MessageSerializedForm<M : Message<M, B>, B : Message.Builder<M, B>>(
  private val bytes: ByteArray,
  private val messageClass: Class<M>,
) : Serializable {

  @Throws(ObjectStreamException::class)
  fun readResolve(): Any {
    val adapter = ProtoAdapter.get(messageClass)
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
