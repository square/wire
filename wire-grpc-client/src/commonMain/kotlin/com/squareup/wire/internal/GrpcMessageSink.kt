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

import com.squareup.wire.MessageSink
import com.squareup.wire.ProtoAdapter
import okio.Buffer
import okio.BufferedSink

/**
 * Writes a sequence of gRPC messages as an HTTP/2 stream.
 *
 * @param sink the HTTP/2 stream body.
 * @param messageAdapter a proto adapter for each message.
 * @param callForCancel the HTTP call that can be canceled to signal abnormal termination.
 * @param grpcEncoding the content coding for the stream body.
 */
internal class GrpcMessageSink<T : Any>(
  private val sink: BufferedSink,
  private val minMessageToCompress: Long,
  private val messageAdapter: ProtoAdapter<T>,
  private val callForCancel: Call?,
  private val grpcEncoding: String,
) : MessageSink<T> {
  private var closed = false
  override fun write(message: T) {
    check(!closed) { "closed" }

    val encodedMessage = Buffer()
    messageAdapter.encode(encodedMessage, message)

    if (grpcEncoding == "identity" || encodedMessage.size < minMessageToCompress) {
      sink.writeByte(0) // 0 = Not encoded.
      sink.writeInt(encodedMessage.size.toInt())
      sink.writeAll(encodedMessage)
    } else {
      val compressedMessage = Buffer()
      grpcEncoding.toGrpcEncoder().encode(compressedMessage).use(BufferedSink::close) { sink ->
        sink.writeAll(encodedMessage)
      }
      sink.writeByte(1) // 1 = Compressed.
      sink.writeInt(compressedMessage.size.toInt())
      sink.writeAll(compressedMessage)
    }

    // TODO: fail if the message size is more than MAX_INT
    sink.flush()
  }

  override fun cancel() {
    check(!closed) { "closed" }
    callForCancel?.cancel()
  }

  override fun close() {
    if (closed) return
    closed = true
    sink.close()
  }
}
