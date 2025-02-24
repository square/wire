/*
 * Copyright (C) 2025 Square, Inc.
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

import com.squareup.wire.GrpcMethod
import com.squareup.wire.GrpcServerStreamingCall
import com.squareup.wire.GrpcStreamingCall
import com.squareup.wire.MessageSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import okio.Timeout

internal class RealGrpcServerStreamingCall<S : Any, R : Any>(
  private val callDelegate: GrpcStreamingCall<S, R>,
  override val method: GrpcMethod<S, R>,
) : GrpcServerStreamingCall<S, R> {

  override val timeout: Timeout
    get() = callDelegate.timeout

  override var requestMetadata: Map<String, String>
    get() = callDelegate.requestMetadata
    set(value) { callDelegate.requestMetadata = value }

  override val responseMetadata: Map<String, String>?
    get() = callDelegate.responseMetadata

  override fun cancel() {
    callDelegate.cancel()
  }

  override fun isCanceled() = callDelegate.isCanceled()

  override fun isExecuted() = callDelegate.isExecuted()

  override fun clone() = RealGrpcServerStreamingCall(callDelegate.clone(), method)

  override suspend fun executeIn(scope: CoroutineScope, request: S): ReceiveChannel<R> {
    val (sendChannel, receiveChannel) = callDelegate.executeIn(scope)
    try {
      sendChannel.send(request)
    } finally {
      sendChannel.close()
    }
    return receiveChannel
  }

  override fun executeBlocking(request: S): MessageSource<R> {
    val (sink, source) = callDelegate.executeBlocking()
    sink.use { it.write(request) }
    return source
  }
}

internal fun <S : Any, R : Any>GrpcStreamingCall<S, R>.asGrpcServerStreamingCall() =
  RealGrpcServerStreamingCall(this, method)
