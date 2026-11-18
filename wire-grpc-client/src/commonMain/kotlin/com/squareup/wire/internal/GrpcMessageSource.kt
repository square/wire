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

import com.squareup.wire.GrpcException
import com.squareup.wire.GrpcStatus
import com.squareup.wire.MessageSource
import com.squareup.wire.ProtoAdapter
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer

/** The largest inbound message that a client accepts unless it is configured otherwise. */
internal const val DEFAULT_MAX_INBOUND_MESSAGE_SIZE: Long = 64L * 1024 * 1024

/**
 * Reads an HTTP/2 stream as a sequence of gRPC messages.
 *
 * @param source the HTTP/2 stream body.
 * @param messageAdapter a proto adapter for each message.
 * @param grpcEncoding the "grpc-encoding" header, or null if it is absent.
 * @param maxMessageSize the largest message to accept, in bytes. This applies to the message as it
 *     is on the wire, and to the message after decompression. A larger message fails with a
 *     [GrpcException] that has [GrpcStatus.RESOURCE_EXHAUSTED].
 */
internal class GrpcMessageSource<T : Any>(
  private val source: BufferedSource,
  private val messageAdapter: ProtoAdapter<T>,
  private val grpcEncoding: String? = null,
  private val maxMessageSize: Long = DEFAULT_MAX_INBOUND_MESSAGE_SIZE,
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
          "message is encoded but message-encoding header was omitted",
        )
      }
      else -> throw ProtocolException("unexpected compressed-flag: $compressedFlag")
    }

    val encodedLength = source.readInt().toLong() and 0xffffffffL
    if (encodedLength > maxMessageSize) {
      throw messageTooLarge("gRPC message exceeds maximum size $maxMessageSize: $encodedLength")
    }

    val encodedMessage = Buffer().write(source, encodedLength)

    return messageDecoding.decode(encodedMessage).limited().buffer().use(BufferedSource::close) {
      messageAdapter.decode(it)
    }
  }

  /** Returns a source that fails when it yields more than [maxMessageSize] bytes. */
  private fun Source.limited(): Source = object : ForwardingSource(this) {
    private var totalBytesRead = 0L

    override fun read(sink: Buffer, byteCount: Long): Long {
      // Ask for one byte more than we allow, so that we notice a message that is too large.
      val room = maxMessageSize - totalBytesRead
      val toRead = if (room < Long.MAX_VALUE) minOf(byteCount, room + 1) else byteCount
      val result = super.read(sink, toRead)
      if (result == -1L) return -1L
      totalBytesRead += result
      if (totalBytesRead > maxMessageSize) {
        throw messageTooLarge("Decompressed gRPC message exceeds maximum size $maxMessageSize")
      }
      return result
    }
  }

  private fun messageTooLarge(message: String) = GrpcException(GrpcStatus.RESOURCE_EXHAUSTED, message)

  fun isEmptyBody(): Boolean = source.exhausted()

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
