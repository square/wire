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

import com.squareup.wire.GrpcClient
import com.squareup.wire.GrpcMethod
import com.squareup.wire.GrpcStreamingCall
import com.squareup.wire.MessageSink
import com.squareup.wire.MessageSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import okio.Timeout
import java.util.concurrent.TimeUnit

internal class RealGrpcStreamingCall<S : Any, R : Any>(
  private val grpcClient: GrpcClient,
  private val grpcMethod: GrpcMethod<S, R>
) : GrpcStreamingCall<S, R> {
  private val requestBody = newDuplexRequestBody()
  private val call = grpcClient.newCall(grpcMethod.path, requestBody)

  override val timeout: Timeout
    get() = call.timeout()

  init {
    timeout.clearTimeout()
    timeout.clearDeadline()
  }

  override fun cancel() {
    call.cancel()
  }

  override fun isCanceled(): Boolean = call.isCanceled()

  override fun execute(): Pair<SendChannel<S>, ReceiveChannel<R>> {
    val requestChannel = Channel<S>(1)
    val responseChannel = Channel<R>(1)
    requestChannel.writeToRequestBody(requestBody, grpcMethod.requestAdapter, call)
    call.enqueue(responseChannel.readFromResponseBodyCallback(grpcMethod.responseAdapter))

    responseChannel.invokeOnClose {
      if (responseChannel.isClosedForReceive) {
        // Short-circuit the request stream if it's still active.
        call.cancel()
        requestChannel.cancel()
      }
    }

    return requestChannel to responseChannel
  }

  override fun executeBlocking(): Pair<MessageSink<S>, MessageSource<R>> {
    val messageSource = BlockingMessageSource(grpcMethod.responseAdapter, call)
    val messageSink = requestBody.messageSink(grpcMethod.requestAdapter, call)
    call.enqueue(messageSource.readFromResponseBodyCallback())

    return messageSink to messageSource
  }

  override fun isExecuted(): Boolean = call.isExecuted()

  override fun clone(): GrpcStreamingCall<S, R> {
    val result = RealGrpcStreamingCall(grpcClient, grpcMethod)
    val oldTimeout = this.timeout
    result.timeout.also { newTimeout ->
      newTimeout.timeout(oldTimeout.timeoutNanos(), TimeUnit.NANOSECONDS)
      if (oldTimeout.hasDeadline()) newTimeout.deadlineNanoTime(oldTimeout.deadlineNanoTime())
      else newTimeout.clearDeadline()
    }
    return result
  }
}
