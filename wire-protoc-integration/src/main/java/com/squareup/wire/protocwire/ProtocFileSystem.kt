/*
 * Copyright 2023 Block Inc.
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
package com.squareup.wire.protocwire

import okio.Buffer
import okio.ForwardingFileSystem
import okio.Path
import okio.Sink
import okio.Timeout

/**
 * The primary responsibility of the protoc plugin is to do some memory
 * manipulation and deferring to protoc to manage the creation of the generated
 * files through the [Plugin.Response].
 *
 * The [ProtocFileSystem] helps with replacing Wire's file writing behavior with
 * one that defers to protoc to handle generated files.
 */
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
