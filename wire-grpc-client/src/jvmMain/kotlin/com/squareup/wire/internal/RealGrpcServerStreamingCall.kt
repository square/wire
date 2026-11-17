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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import okio.ForwardingTimeout
import okio.IOException
import okio.Timeout

/**
 * A [GrpcServerStreamingCall] that sends a single request and reads a streaming response.
 *
 * The request body writes the complete request and END_STREAM before OkHttp reads the response.
 * This prevents delays on servers that wait for the client's half-close before they stream responses.
 * The body is duplex, so OkHttp's call timeout stops after the response headers.
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

  override fun clone(): RealGrpcServerStreamingCall<S, R> {
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

    responseChannel.invokeOnClose { cause ->
      if (cause != null) {
        cancel()
      }
    }

    enqueue(request, responseChannel)

    return responseChannel
  }

  /** Starts the call and sends the response messages to [responseChannel] from OkHttp's thread. */
  internal fun enqueue(request: S, responseChannel: SendChannel<R>) {
    val call = initCall(request)
    call.enqueue(
      responseChannel.readFromResponseBodyCallback(
        onResponseMetadata = { this.responseMetadata = it },
        responseAdapter = method.responseAdapter,
      ),
    )
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
    val requestBody = newSingleMessageDuplexRequestBody(
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
 * Exposes a [RealGrpcServerStreamingCall] through the legacy [GrpcStreamingCall] API.
 * Generated clients use it for server streaming methods when explicit streaming call types are disabled.
 *
 * The call starts when the request is available.
 * OkHttp's thread sends the response messages, so the caller's scope only reads the request.
 */
internal class GrpcServerStreamingCallStreamingAdapter<S : Any, R : Any>(
  private val callDelegate: RealGrpcServerStreamingCall<S, R>,
) : GrpcStreamingCall<S, R> {
  private var executed = false

  /** Fails the pending response stream. Null until this call executes. */
  @Volatile private var failResponses: (() -> Unit)? = null

  override val method: GrpcMethod<S, R>
    get() = callDelegate.method

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
    // The HTTP call does not exist before the request arrives.
    failResponses?.invoke()
  }

  override fun isCanceled() = callDelegate.isCanceled()

  @Suppress("OPT_IN_USAGE", "OVERRIDE_DEPRECATION")
  override fun execute(): Pair<SendChannel<S>, ReceiveChannel<R>> = executeIn(GlobalScope)

  override fun executeIn(scope: CoroutineScope): Pair<SendChannel<S>, ReceiveChannel<R>> {
    markExecuted()

    val requestChannel = Channel<S>(1)
    val responseChannel = Channel<R>(1)

    responseChannel.invokeOnClose { cause ->
      if (cause != null) callDelegate.cancel()
      // The request is not necessary after the response ends.
      requestChannel.cancel()
    }

    scope.launch {
      try {
        sendRequest(requestChannel, responseChannel)
      } catch (e: Throwable) {
        responseChannel.close(e)
      }
    }.invokeOnCompletion { cause ->
      // The scope can be canceled before the coroutine starts.
      if (cause != null) responseChannel.close(cause)
    }

    onExecuted { responseChannel.close(IOException("canceled")) }

    return requestChannel to responseChannel
  }

  private suspend fun sendRequest(requestChannel: ReceiveChannel<S>, responseChannel: SendChannel<R>) {
    val first = requestChannel.receiveCatching()
    val request = first.getOrNull()
    if (request == null) {
      val cause = first.exceptionOrNull()
      responseChannel.close(
        if (cause != null) {
          CancellationException("request canceled", cause)
        } else {
          ProtocolException("expected 1 message but got none")
        },
      )
      return
    }

    callDelegate.enqueue(request, responseChannel)

    // Continue to read the request channel.
    // If the caller closes it with a cause, cancel the call.
    val next = requestChannel.receiveCatching()
    val cause = next.exceptionOrNull()
    when {
      next.isSuccess -> responseChannel.close(ProtocolException("expected 1 message but got multiple"))
      cause != null -> responseChannel.close(CancellationException("request canceled", cause))
    }
  }

  override fun executeBlocking(): Pair<MessageSink<S>, MessageSource<R>> {
    markExecuted()

    val responses = DeferredMessageSource<R>(cancelCall = callDelegate::cancel)
    val requests = object : MessageSink<S> {
      private var request: S? = null
      private var closed = false

      override fun write(message: S) {
        check(!closed) { "closed" }
        if (request != null) throw ProtocolException("expected 1 message but got multiple")
        request = message
        // Start now: callers can read responses before they close this sink.
        responses.start { callDelegate.executeBlocking(message) }
      }

      override fun cancel() {
        callDelegate.cancel()
        responses.fail(IOException("canceled"))
      }

      override fun close() {
        if (closed) return
        closed = true
        if (request == null) {
          responses.fail(ProtocolException("expected 1 message but got none"))
        }
      }
    }

    onExecuted { responses.fail(IOException("canceled")) }

    return requests to responses
  }

  override fun isExecuted() = executed || callDelegate.isExecuted()

  override fun clone() = GrpcServerStreamingCallStreamingAdapter(callDelegate.clone())

  private fun markExecuted() {
    check(!executed) { "already executed" }
    executed = true
  }

  /** Registers [failResponses] for [cancel], and runs it now if the call is already canceled. */
  private fun onExecuted(failResponses: () -> Unit) {
    this.failResponses = failResponses
    if (callDelegate.isCanceled()) failResponses()
  }
}

internal fun <S : Any, R : Any> RealGrpcServerStreamingCall<S, R>.asGrpcStreamingCall() = GrpcServerStreamingCallStreamingAdapter(this)

/**
 * A [MessageSource] for a call that starts when the caller writes the request.
 * [read] waits until the call starts or fails.
 */
private class DeferredMessageSource<R : Any>(
  private val cancelCall: () -> Unit,
) : MessageSource<R> {
  private val settled = CountDownLatch(1)

  /** Guarded by this. */
  private var delegate: MessageSource<R>? = null

  /** Guarded by this. */
  private var failure: IOException? = null

  fun start(openSource: () -> MessageSource<R>) {
    synchronized(this) {
      if (settled.count == 0L) return
      try {
        delegate = openSource()
      } catch (e: Throwable) {
        failure = e as? IOException ?: IOException(e)
        throw e
      } finally {
        settled.countDown()
      }
    }
  }

  fun fail(e: IOException) {
    synchronized(this) {
      if (settled.count == 0L) return
      failure = e
      settled.countDown()
    }
  }

  override fun read(): R? {
    settled.await()
    val delegate = synchronized(this) {
      failure?.let { throw it }
      delegate!!
    }
    return delegate.read()
  }

  override fun close() {
    fail(IOException("closed"))
    val delegate = synchronized(this) { delegate }
    if (delegate != null) delegate.close() else cancelCall()
  }
}
