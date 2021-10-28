package com.squareup.wire.internal

import com.squareup.wire.MessageSink
import com.squareup.wire.ProtoAdapter
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.launch
import okio.Buffer
import okio.BufferedSink

class GrpcMessageWriteChannel<T: Any>(
  private val channel: ByteWriteChannel,
  private val minMessageToCompress: Long,
  private val messageAdapter: ProtoAdapter<T>,
  private val grpcEncoding: String
) {
  private var closed = false

  suspend fun write(message: T) {
    check(!closed) { "closed" }

    val encodedMessage = Buffer()
    messageAdapter.encode(encodedMessage, message)

    if (grpcEncoding == "identity" || encodedMessage.size < minMessageToCompress) {
      channel.writeByte(0) // 0 = Not encoded.
      channel.writeInt(encodedMessage.size.toInt())
      channel.writeFully(encodedMessage.readByteArray())
    } else {
      val compressedMessage = Buffer()
      grpcEncoding.toGrpcEncoder().encode(compressedMessage).use(BufferedSink::close) { channel ->
        channel.writeAll(encodedMessage)
      }
      channel.writeByte(1) // 1 = Compressed.
      channel.writeInt(compressedMessage.size.toInt())
      channel.writeFully(compressedMessage.readByteArray())
    }

    // TODO: fail if the message size is more than MAX_INT
    channel.flush()
  }

  fun cancel() {
    check(!closed) { "closed" }
  }

  fun close() {
    if (closed) return
    closed = true
    channel.close()
  }
}
