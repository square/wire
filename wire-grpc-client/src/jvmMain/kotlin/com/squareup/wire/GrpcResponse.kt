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

actual class GrpcResponse(private val response: Response) {

  actual val body: ResponseBody?
    get() = response.body

  actual fun header(name: String, defaultValue: String?): String? = response.header(name, defaultValue)

  @Throws(IOException::class)
  actual fun trailers(): GrpcHeaders = response.trailers()

  actual fun close() = response.close()
}
