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
package com.squareup.wire.internal

import com.squareup.wire.GrpcResponse
import java.lang.reflect.Method
import okio.IOException
import okio.Sink
import okio.Source
import okio.gzip

internal actual interface Call {
  actual fun cancel()

  @Throws(IOException::class)
  actual fun execute(): GrpcResponse
}

internal fun okhttp3.Call.toWireCall(): Call {
  return object : Call {
    override fun cancel() = this@toWireCall.cancel()

    override fun execute(): GrpcResponse = GrpcResponse(this@toWireCall.execute())
  }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Sink.asGzip(): Sink = gzip()

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Source.asGzip(): Source = gzip()

internal actual fun Throwable.addSuppressed(other: Throwable) {
  AddSuppressedMethod?.invoke(this, other)
}

private val AddSuppressedMethod: Method? by lazy {
  try {
    Throwable::class.java.getMethod("addSuppressed", Throwable::class.java)
  } catch (t: Throwable) {
    null
  }
}
