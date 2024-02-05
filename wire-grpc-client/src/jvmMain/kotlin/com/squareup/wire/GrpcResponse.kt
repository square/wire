/*
 * Copyright (C) 2020 Square, Inc.
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
package com.squareup.wire

import java.io.IOException
import okhttp3.Response
import okhttp3.ResponseBody

internal actual class GrpcResponse(private val response: Response) {

  @get:JvmName("body")
  actual val body: ResponseBody?
    get() = response.body

  fun header(name: String): String? = header(name, null)

  actual fun header(name: String, defaultValue: String?): String? =
    response.header(name, defaultValue)

  /**
   * Returns the trailers after the HTTP response, which may be empty. It is an error to call this
   * before the entire gRPC response body has been consumed.
   */
  @Throws(IOException::class)
  actual fun trailers(): GrpcHeaders = response.trailers()

  /**
   * Closes the response body. Equivalent to body().close().
   * It is an error to close a response that is not eligible for a body. This includes the
   * responses returned from cacheResponse, networkResponse, and priorResponse.
   */
  actual fun close() = response.close()
}
