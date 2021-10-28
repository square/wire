package com.squareup.wire.internal

import com.squareup.wire.ProtoAdapter
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyAndClose
import io.ktor.utils.io.readAvailable
import okio.Buffer
import okio.BufferedSource
import okio.buffer

class GrpcMessageReadChannel<T : Any>(
  private val channel: ByteReadChannel,
  private val messageAdapter: ProtoAdapter<T>,
  private val grpcEncoding: String? = null
) {
  suspend fun read(): T? {
    if (channel.isClosedForRead) return null

    // Length-Prefixed-Message → Compressed-Flag Message-Length Message
    //         Compressed-Flag → 0 / 1 # encoded as 1 byte unsigned integer
    //          Message-Length → {length of Message} # encoded as 4 byte unsigned integer
    //                 Message → *{binary octet}

    val compressedFlag = channel.readByte()
    val messageDecoding: GrpcDecoder = when {
      compressedFlag.toInt() == 0 -> GrpcDecoder.IdentityGrpcDecoder
      compressedFlag.toInt() == 1 -> {
        grpcEncoding?.toGrpcDecoding() ?: throw ProtocolException(
          "message is encoded but message-encoding header was omitted")
      }
      else -> throw ProtocolException("unexpected compressed-flag: $compressedFlag")
    }

    val encodedLength = channel.readInt().toLong() and 0xffffffffL

    val byteArray = ByteArray(encodedLength.toInt())
    channel.readAvailable(byteArray)
    val encodedMessage = Buffer().write(byteArray)

    return messageDecoding.decode(encodedMessage).buffer().use(BufferedSource::close) {
      messageAdapter.decode(it)
    }
  }
}