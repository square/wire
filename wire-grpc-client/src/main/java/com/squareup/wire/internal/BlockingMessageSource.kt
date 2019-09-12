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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.LinkedBlockingDeque

/**
 * This message source uses a [LinkedBlockingDeque] to connect a reading source with a writing
 * response callback.
 *
 * It uses two sentinel types:
 *
 *  * Failure: enqueued when there's an exception on the stream.
 *  * Complete: enqueued when the stream completes normally.
 */
internal class BlockingMessageSource<R : Any>(
  val responseAdapter: ProtoAdapter<R>,
  val call: Call
) : MessageSource<R> {
  private val queue = LinkedBlockingDeque<Any>(1)

  override fun read(): R? {
    return when (val result = queue.take()) {
      is Complete -> {
        queue.put(result) // Replace it.
        null
      }
      is Failure -> {
        queue.put(result) // Replace it.
        throw result.e
      }
      else -> result as R
    }
  }

  override fun close() {
    call.cancel() // Short-circuit the request stream if it's still active.
  }

  /** Read messages from the response body and write them to the deque. */
  fun readFromResponseBodyCallback(): Callback {
    return object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        queue.put(Failure(e))
      }

      override fun onResponse(call: Call, response: Response) {
        try {
          response.use {
            response.messageSource(responseAdapter).use { reader ->
              while (true) {
                val message = reader.read() ?: break
                queue.put(message)
              }

              val exception = response.grpcStatusToException()
              if (exception != null) throw exception
            }
          }
          queue.put(Complete)
        } catch (e: IOException) {
          call.cancel() // Break the request stream if the response stream breaks.
          queue.put(Failure(e))
        }
      }
    }
  }

  private object Complete
  private class Failure(val e: IOException)
}
