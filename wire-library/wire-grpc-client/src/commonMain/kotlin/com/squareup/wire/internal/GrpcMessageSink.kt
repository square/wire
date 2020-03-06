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
internal class GrpcMessageSink<T : Any> constructor(
  private val sink: BufferedSink,
  private val messageAdapter: ProtoAdapter<T>,
  private val callForCancel: Call?,
  private val grpcEncoding: String
) : MessageSink<T> {
  private var closed = false
  override fun write(message: T) {
    check(!closed) { "closed" }

    val encodedMessage = Buffer()
    grpcEncoding.toGrpcEncoder().encode(encodedMessage).use(BufferedSink::close) { encodingSink ->
      messageAdapter.encode(encodingSink, message)
    }

    val compressedFlag = if (grpcEncoding == "identity") 0 else 1
    sink.writeByte(compressedFlag)
    // TODO: fail if the message size is more than MAX_INT
    sink.writeInt(encodedMessage.size.toInt())
    sink.writeAll(encodedMessage)
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
