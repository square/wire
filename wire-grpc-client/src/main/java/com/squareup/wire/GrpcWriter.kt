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
package com.squareup.wire

import okio.Buffer
import okio.BufferedSink
import java.io.Closeable

/** Writes a sequence of GRPC messages as an HTTP/2 stream. */
internal class GrpcWriter<T> private constructor(
  private val sink: BufferedSink,
  private val messageAdapter: ProtoAdapter<T>
) : Closeable {
  companion object {
    /**
     * @param sink the HTTP/2 stream body.
     * @param messageAdapter a proto adapter for each message.
     */
    fun <T> get(
      sink: BufferedSink,
      messageAdapter: ProtoAdapter<T>
    ) = GrpcWriter(sink, messageAdapter)
  }

  fun writeMessage(message: T) {
    // TODO: support writing nontrivial encodings
    val compressedFlag = 0
    sink.writeByte(compressedFlag)

    val encodedMessage = Buffer()
    messageAdapter.encode(encodedMessage, message)

    // TODO: fail if the message size is less than MAX_INT
    sink.writeInt(encodedMessage.size.toInt())
    sink.writeAll(encodedMessage)
  }

  fun flush() {
    sink.flush()
  }

  override fun close() {
    sink.close()
  }
}