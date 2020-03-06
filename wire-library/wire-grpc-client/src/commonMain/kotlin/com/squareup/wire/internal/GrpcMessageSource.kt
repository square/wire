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

import com.squareup.wire.MessageSource
import com.squareup.wire.ProtoAdapter
import okio.Buffer
import okio.BufferedSource
import okio.buffer

/**
 * Reads an HTTP/2 stream as a sequence of gRPC messages.
 *
 * @param source the HTTP/2 stream body.
 * @param messageAdapter a proto adapter for each message.
 * @param grpcEncoding the "grpc-encoding" header, or null if it is absent.
 */
internal class GrpcMessageSource<T : Any>(
  private val source: BufferedSource,
  private val messageAdapter: ProtoAdapter<T>,
  private val grpcEncoding: String? = null
) : MessageSource<T> {
  override fun read(): T? {
    if (source.exhausted()) return null

    // Length-Prefixed-Message → Compressed-Flag Message-Length Message
    //         Compressed-Flag → 0 / 1 # encoded as 1 byte unsigned integer
    //          Message-Length → {length of Message} # encoded as 4 byte unsigned integer
    //                 Message → *{binary octet}

    val compressedFlag = source.readByte()
    val messageDecoding: GrpcDecoder = when {
      compressedFlag.toInt() == 0 -> GrpcDecoder.IdentityGrpcDecoder
      compressedFlag.toInt() == 1 -> {
        grpcEncoding?.toGrpcDecoding() ?: throw ProtocolException(
            "message is encoded but message-encoding header was omitted")
      }
      else -> throw ProtocolException("unexpected compressed-flag: $compressedFlag")
    }

    val encodedLength = source.readInt().toLong() and 0xffffffffL

    val encodedMessage = Buffer().write(source, encodedLength)

    return messageDecoding.decode(encodedMessage).buffer().use(BufferedSource::close) {
      messageAdapter.decode(it)
    }
  }

  fun readExactlyOneAndClose(): T {
    use(GrpcMessageSource<T>::close) { reader ->
      val result = reader.read() ?: throw ProtocolException("expected 1 message but got none")
      val end = reader.read()
      if (end != null) throw ProtocolException("expected 1 message but got multiple")
      return result
    }
  }

  override fun close() = source.close()
}
