/*
 * Copyright 2020 Square Inc.
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
@file:JvmName("GrpcResponseCloseable")

package com.squareup.wire

import com.squareup.wire.internal.Throws
import com.squareup.wire.internal.addSuppressed
import okio.IOException
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

expect class GrpcResponse {
  @get:JvmName("body") val body: GrpcResponseBody?

  @JvmOverloads fun header(name: String, defaultValue: String? = null): String?

  @Throws(IOException::class)
  fun trailers(): GrpcHeaders

  fun close()
}

internal inline fun <T : GrpcResponse?, R> T.use(block: (T) -> R): R {
  var exception: Throwable? = null
  try {
    return block(this)
  } catch (e: Throwable) {
    exception = e
    throw e
  } finally {
    closeFinally(exception)
  }
}

private fun GrpcResponse?.closeFinally(cause: Throwable?) = when {
  this == null -> {
  }
  cause == null -> close()
  else ->
    try {
      close()
    } catch (closeException: Throwable) {
      cause.addSuppressed(closeException)
    }
}
