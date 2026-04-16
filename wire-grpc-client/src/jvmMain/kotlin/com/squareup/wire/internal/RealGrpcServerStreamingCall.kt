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
import com.squareup.wire.MessageSink
import com.squareup.wire.MessageSource
import com.squareup.wire.WireGrpcClient
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.ForwardingTimeout
import okio.IOException
import okio.Timeout

/**
 * A [GrpcServerStreamingCall] that sends a single non-duplex request and reads a streaming
 * response. Using a non-duplex request body ensures the complete request (including END_STREAM) is
 * sent to the server before responses are read, avoiding delays on servers that wait for the
 * client's half-close before starting to stream responses.
 */
internal class RealGrpcServerStreamingCall<S : Any, R : Any>(
  private val grpcClient: WireGrpcClient,
  override val method: GrpcMethod<S, R>,
) : GrpcServerStreamingCall<S, R> {

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

  override fun isExecuted(): Boolean = call?.isExecuted() ?: false

  override fun clone(): GrpcServerStreamingCall<S, R> {
    val result = RealGrpcServerStreamingCall(grpcClient, method)
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

  override suspend fun executeIn(scope: CoroutineScope, request: S): ReceiveChannel<R> {
    val responseChannel = Channel<R>(1)
    val call = initCall(request)

    responseChannel.invokeOnClose { cause ->
      if (cause != null) {
        call.cancel()
      }
    }

    call.enqueue(
      responseChannel.readFromResponseBodyCallback(
        onResponseMetadata = { this.responseMetadata = it },
        responseAdapter = method.responseAdapter,
      ),
    )

    return responseChannel
  }

  override fun executeBlocking(request: S): MessageSource<R> {
    val call = initCall(request)
    val messageSource = BlockingMessageSource(
      onResponseMetadata = { this.responseMetadata = it },
      responseAdapter = method.responseAdapter,
      call = call,
    )
    call.enqueue(messageSource.readFromResponseBodyCallback())
    return messageSource
  }

  private fun initCall(request: S): okhttp3.Call {
    check(this.call == null) { "already executed" }
    val requestBody = newRequestBody(
      minMessageToCompress = grpcClient.minMessageToCompress,
      requestAdapter = method.requestAdapter,
      onlyMessage = request,
    )
    val result = grpcClient.newCall(method, requestMetadata, requestBody, timeout)
    this.call = result
    if (canceled) result.cancel()
    (timeout as ForwardingTimeout).setDelegate(result.timeout())
    return result
  }
}

/**
 * Wraps a [GrpcStreamingCall] as a [GrpcServerStreamingCall]. Used for test doubles created via
 * [com.squareup.wire.GrpcServerStreamingCall] factory functions in GrpcCalls.
 */
internal class GrpcStreamingCallServerStreamingAdapter<S : Any, R : Any>(
  private val callDelegate: GrpcStreamingCall<S, R>,
  override val method: GrpcMethod<S, R>,
) : GrpcServerStreamingCall<S, R> {

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

  override fun isExecuted() = callDelegate.isExecuted()

  override fun clone() = GrpcStreamingCallServerStreamingAdapter(callDelegate.clone(), method)

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

internal fun <S : Any, R : Any> GrpcStreamingCall<S, R>.asGrpcServerStreamingCall() = GrpcStreamingCallServerStreamingAdapter(this, method)

/**
 * Wraps a [GrpcServerStreamingCall] as the legacy [GrpcStreamingCall] API. This is used by
 * generated clients when explicit streaming call types are disabled.
 */
internal class GrpcServerStreamingCallStreamingAdapter<S : Any, R : Any>(
  private val callDelegate: GrpcServerStreamingCall<S, R>,
  override val method: GrpcMethod<S, R>,
) : GrpcStreamingCall<S, R> {
  private var executed = false

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

  @Suppress("OPT_IN_USAGE", "OVERRIDE_DEPRECATION")
  override fun execute(): Pair<SendChannel<S>, ReceiveChannel<R>> {
    return executeIn(GlobalScope)
  }

  override fun executeIn(scope: CoroutineScope): Pair<SendChannel<S>, ReceiveChannel<R>> {
    return executeWithChannels(scope)
  }

  @Suppress("OPT_IN_USAGE")
  override fun executeBlocking(): Pair<MessageSink<S>, MessageSource<R>> {
    val (requestChannel, responseChannel) = executeWithChannels(GlobalScope)
    return requestChannel.toMessageSink() to responseChannel.toMessageSource()
  }

  override fun isExecuted() = executed || callDelegate.isExecuted()

  override fun clone() = GrpcServerStreamingCallStreamingAdapter(callDelegate.clone(), method)

  private fun executeWithChannels(scope: CoroutineScope): Pair<Channel<S>, Channel<R>> {
    check(!executed) { "already executed" }
    executed = true

    val requestChannel = Channel<S>(1)
    val responseChannel = Channel<R>(1)
    var delegateResponseChannel: ReceiveChannel<R>? = null

    responseChannel.invokeOnClose { cause ->
      if (cause != null) {
        requestChannel.cancel()
        delegateResponseChannel?.cancel()
        callDelegate.cancel()
      }
    }

    scope.launch {
      try {
        val requestResult = requestChannel.receiveCatching()
        requestResult.exceptionOrNull()?.let { throw it }
        val request = requestResult.getOrNull()
          ?: throw ProtocolException("expected 1 message but got none")
        requestChannel.close()
        val responses = callDelegate.executeIn(scope, request)
        delegateResponseChannel = responses
        for (response in responses) {
          responseChannel.send(response)
        }
        responseChannel.close()
      } catch (e: Throwable) {
        responseChannel.close(e)
      }
    }

    return requestChannel to responseChannel
  }
}

internal fun <S : Any, R : Any> GrpcServerStreamingCall<S, R>.asGrpcStreamingCall() = GrpcServerStreamingCallStreamingAdapter(this, method)

private fun <E : Any> Channel<E>.toMessageSource() = object : MessageSource<E> {
  override fun read(): E? = runBlocking {
    try {
      val result = receiveCatching()
      result.exceptionOrNull()?.let { throw it }
      result.getOrNull()
    } catch (e: Throwable) {
      throw e.toIOException()
    }
  }

  override fun close() {
    cancel()
  }
}

private fun <E : Any> Channel<E>.toMessageSink() = object : MessageSink<E> {
  override fun write(message: E) {
    runBlocking {
      try {
        send(message)
      } catch (e: Throwable) {
        throw e.toIOException()
      }
    }
  }

  override fun cancel() {
    this@toMessageSink.cancel()
  }

  override fun close() {
    this@toMessageSink.close()
  }
}

private fun Throwable.toIOException() = this as? IOException ?: IOException(this)
