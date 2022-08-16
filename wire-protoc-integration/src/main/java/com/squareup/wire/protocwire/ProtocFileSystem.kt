package com.squareup.wire.protocwire

import okio.Buffer
import okio.ForwardingFileSystem
import okio.Path
import okio.Sink
import okio.Timeout

internal class ProtocFileSystem(private val response: Plugin.Response): ForwardingFileSystem(SYSTEM) {
  /**
   * Returns a single sink per file from protoc.
   *
   * It isn't guaranteed that flush() is called prior to close().
   * The behavior intends to address this issue by calling flush()
   * on close() if it hasn't been called yet. The flush() method
   * is the canonical way to write to file.
   */
  override fun sink(file: Path, mustCreate: Boolean): Sink {
    return object: Sink {
      private val buffer = Buffer()
      private var isFlushed = false

      override fun close() {
        flush()
        buffer.close()
      }

      override fun flush() {
        if (isFlushed) {
          return
        }
        isFlushed = true
        response.addFile(file.toString(), buffer.readUtf8())
      }

      override fun timeout(): Timeout {
        return Timeout()
      }

      override fun write(source: Buffer, byteCount: Long) {
        source.read(buffer, byteCount)
      }
    }
  }
}
