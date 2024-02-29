/*
 * Copyright (C) 2019 Square, Inc.
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
import com.squareup.wire.GrpcStreamingCall
import com.squareup.wire.MessageSink
import com.squareup.wire.MessageSource
import com.squareup.wire.WireGrpcClient
import java.util.concurrent.TimeUnit
import kotlin.DeprecationLevel.WARNING
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import okio.ForwardingTimeout
import okio.Timeout

internal class RealGrpcStreamingCall<S : Any, R : Any>(
  private val grpcClient: WireGrpcClient,
  override val method: GrpcMethod<S, R>,
) : GrpcStreamingCall<S, R> {
  private val requestBody = newDuplexRequestBody()

  /** Non-null once this is executed. */
  private var call: okhttp3.Call? = null
  private var canceled = false

  override val timeout: Timeout = ForwardingTimeout(Timeout())

  init {
    timeout.clearTimeout()
    timeout.clearDeadline()
  }

  override var requestMetadata: Map<String, String> = mapOf()

  override var responseMetadata: Map<String, String>? = null
    internal set

  override fun cancel() {
    canceled = true
    call?.cancel()
  }

  override fun isCanceled(): Boolean = canceled || call?.isCanceled() == true

  @Deprecated(
    "Provide a scope, preferably not GlobalScope",
    replaceWith = ReplaceWith("executeIn(GlobalScope)", "kotlinx.coroutines.GlobalScope"),
    level = WARNING,
  )
  @Suppress("OPT_IN_USAGE")
  override fun execute(): Pair<SendChannel<S>, ReceiveChannel<R>> = executeIn(GlobalScope)

  override fun executeIn(scope: CoroutineScope): Pair<SendChannel<S>, ReceiveChannel<R>> {
    val requestChannel = Channel<S>(1)
    val responseChannel = Channel<R>(1)
    val call = initCall()

    responseChannel.invokeOnClose {
      if (responseChannel.isClosedForReceive) {
        // Short-circuit the request stream if it's still active.
        call.cancel()
        requestChannel.cancel()
      }
    }

    scope.launch(Dispatchers.IO) {
      requestChannel.writeToRequestBody(
        requestBody = requestBody,
        minMessageToCompress = grpcClient.minMessageToCompress,
        requestAdapter = method.requestAdapter,
        callForCancel = call,
      )
    }
    call.enqueue(responseChannel.readFromResponseBodyCallback(this, method.responseAdapter))

    return requestChannel to responseChannel
  }

  override fun executeBlocking(): Pair<MessageSink<S>, MessageSource<R>> {
    val call = initCall()
    val messageSource = BlockingMessageSource(this, method.responseAdapter, call)
    val messageSink = requestBody.messageSink(
      minMessageToCompress = grpcClient.minMessageToCompress,
      requestAdapter = method.requestAdapter,
      callForCancel = call,
    )
    call.enqueue(messageSource.readFromResponseBodyCallback())

    return messageSink to messageSource
  }

  override fun isExecuted(): Boolean = call?.isExecuted() ?: false

  override fun clone(): GrpcStreamingCall<S, R> {
    val result = RealGrpcStreamingCall(grpcClient, method)
    val oldTimeout = this.timeout
    result.timeout.also { newTimeout ->
      newTimeout.timeout(oldTimeout.timeoutNanos(), TimeUnit.NANOSECONDS)
      if (oldTimeout.hasDeadline()) {
        newTimeout.deadlineNanoTime(oldTimeout.deadlineNanoTime())
      } else {
        newTimeout.clearDeadline()
      }
    }
    result.requestMetadata += this.requestMetadata
    return result
  }

  private fun initCall(): okhttp3.Call {
    check(this.call == null) { "already executed" }

    val result = grpcClient.newCall(method, requestMetadata, requestBody, timeout)
    this.call = result
    if (canceled) result.cancel()
    (timeout as ForwardingTimeout).setDelegate(result.timeout())
    return result
  }
}
