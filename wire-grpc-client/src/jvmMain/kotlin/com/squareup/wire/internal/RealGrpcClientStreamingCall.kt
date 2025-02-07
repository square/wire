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

import com.squareup.wire.GrpcClientStreamingCall
import com.squareup.wire.GrpcDeferredResponse
import com.squareup.wire.GrpcMethod
import com.squareup.wire.GrpcStreamingCall
import com.squareup.wire.MessageSink
import java.util.concurrent.locks.ReentrantLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.SendChannel
import okio.Timeout
import okio.withLock

internal class RealGrpcClientStreamingCall<S : Any, R : Any>(
  private val callDelegate: GrpcStreamingCall<S, R>,
  override val method: GrpcMethod<S, R>,
) : GrpcClientStreamingCall<S, R> {
  override val timeout: Timeout
    get() = callDelegate.timeout
  override var requestMetadata: Map<String, String>
    get() = callDelegate.requestMetadata
    set(value) {
      callDelegate.requestMetadata = value
    }
  override val responseMetadata: Map<String, String>?
    get() = callDelegate.responseMetadata

  override fun cancel() {
    callDelegate.cancel()
  }

  override fun isCanceled() = callDelegate.isCanceled()

  override fun executeIn(scope: CoroutineScope): Pair<SendChannel<S>, Deferred<R>> {
    val (sendChannel, receiveChannel) = callDelegate.executeIn(scope)
    return sendChannel to scope.async(Dispatchers.Unconfined, start = LAZY) {
      sendChannel.close()
      try {
        receiveChannel.receive()
      } catch (e: ClosedReceiveChannelException) {
        throw IllegalStateException("missing expected response", e)
      }
    }.apply {
      invokeOnCompletion { t ->
        if (t is CancellationException) {
          callDelegate.cancel()
        }
      }
    }
  }

  override fun executeBlocking(): Pair<MessageSink<S>, GrpcDeferredResponse<R>> {
    val (sink, source) = callDelegate.executeBlocking()
    return sink to object : GrpcDeferredResponse<R> {
      private val lock = ReentrantLock()
      private var response: R? = null

      override fun get(): R {
        sink.close()
        return response ?: lock.withLock {
          checkNotNull(response ?: source.read()?.also { response = it }) {
            "expecting a single response"
          }
        }
      }

      override fun close() {
        source.close()
      }
    }
  }

  override fun isExecuted() = callDelegate.isExecuted()

  override fun clone() = RealGrpcClientStreamingCall(callDelegate.clone(), method)
}

internal fun <S : Any, R : Any> GrpcStreamingCall<S, R>.asGrpcClientStreamingCall() =
  RealGrpcClientStreamingCall(this, method)
