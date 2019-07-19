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

/**
 * Writes a sequence of gRPC messages as an HTTP/2 stream.
 *
 * @param sink the HTTP/2 stream body.
 * @param messageAdapter a proto adapter for each message.
 * @param grpcEncoding the content coding for the stream body.
 */
internal class GrpcMessageSink<T : Any> constructor(
  private val sink: BufferedSink,
  private val messageAdapter: ProtoAdapter<T>,
  private val grpcEncoding: String
) : MessageSink<T>, Closeable by sink {
  override fun write(message: T) {
    val messageEncoding = grpcEncoding.toGrpcEncoder()
    val encodingSink = messageEncoding.encode(sink)

    val compressedFlag = if (grpcEncoding == "identity") 0 else 1
    encodingSink.writeByte(compressedFlag)

    val encodedMessage = Buffer()
    messageAdapter.encode(encodedMessage, message)

    // TODO: fail if the message size is more than MAX_INT
    encodingSink.writeInt(encodedMessage.size.toInt())
    encodingSink.writeAll(encodedMessage)

    sink.flush()
  }
}
