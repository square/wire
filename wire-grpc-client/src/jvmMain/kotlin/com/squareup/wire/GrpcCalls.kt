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
@file:JvmName("GrpcCalls")

package com.squareup.wire

import com.squareup.wire.internal.asGrpcClientStreamingCall
import com.squareup.wire.internal.asGrpcServerStreamingCall
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.IOException
import okio.Timeout

/**
 * Returns a new instance of [GrpcCall] that can be used for a single call to
 * [execute][GrpcCall.execute], [executeBlocking][GrpcCall.executeBlocking], or
 * [enqueue][GrpcCall.enqueue].
 *
 * The returned instance executes [function] synchronously on the calling thread, regardless of
 * which blocking mode is used. If [function] throws, the thrown exception will be wrapped in an
 * [IOException].
 *
 * This method is useful when implementing the interfaces that are generated by Wire:
 *
 * ```
 *   override fun GetFeature(): GrpcCall<Point, Feature> {
 *     return GrpcCall<Point, Feature> { request ->
 *       return@GrpcCall lookupNearestFeature(request.latitude, request.longitude)
 *     }
 *   }
 * ```
 *
 * It is succinct when used in an expression function:
 *
 * ```
 *   override fun GetFeature() = GrpcCall<Point, Feature> { request ->
 *     return@GrpcCall lookupNearestFeature(request.latitude, request.longitude)
 *   }
 * ```
 */
@JvmName("grpcCall")
fun <S : Any, R : Any> GrpcCall(function: (S) -> R): GrpcCall<S, R> {
  return object : GrpcCall<S, R> {
    private var canceled = AtomicBoolean()
    private var executed = AtomicBoolean()
    override var requestMetadata: Map<String, String> = mapOf()
    override val responseMetadata: Map<String, String>? = null

    @Suppress("UNCHECKED_CAST")
    override val method: GrpcMethod<S, R>
      get() = GrpcMethod(
        path = "/wire/AnonymousEndpoint",
        requestAdapter = ProtoAdapter.BYTES,
        responseAdapter = ProtoAdapter.BYTES,
      ) as GrpcMethod<S, R>

    override val timeout: Timeout = Timeout.NONE

    override fun cancel() {
      canceled.set(true)
    }

    override fun isCanceled() = canceled.get()

    override fun isExecuted() = executed.get()

    override fun enqueue(request: S, callback: GrpcCall.Callback<S, R>) {
      val response = try {
        executeBlocking(request)
      } catch (exception: IOException) {
        callback.onFailure(this, exception)
        return
      }
      callback.onSuccess(this, response)
    }

    override suspend fun execute(request: S): R = executeBlocking(request)

    override fun executeBlocking(request: S): R {
      check(executed.compareAndSet(false, true)) { "already executed" }
      if (canceled.get()) throw IOException("canceled")
      try {
        return function(request)
      } catch (e: IOException) {
        throw e
      } catch (e: Exception) {
        throw IOException("call failed: $e", e)
      }
    }

    override fun clone() = GrpcCall(function).also { it.requestMetadata += requestMetadata }
  }
}

/**
 * Returns a new instance of [GrpcStreamingCall] that can be used for a single call to
 * [executeIn][GrpcStreamingCall.executeIn] or [executeBlocking][GrpcStreamingCall.executeBlocking].
 *
 * The returned instance launches [function] on [Dispatchers.IO]. The function must close the
 * [SendChannel] when it has no more messages to transmit. If [function] throws, both channels will
 * be closed using the thrown exception as a cause.
 *
 * This method is useful when implementing the interfaces that are generated by Wire:
 *
 * ```
 *   override fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote> {
 *     return GrpcStreamingCall { requests, responses ->
 *       requests.consumeEach { note ->
 *         responses.send(translateNote(note))
 *       }
 *       responses.close()
 *     }
 *   }
 * ```
 *
 * It is succinct when used in an expression function:
 *
 * ```
 *  override fun RouteChat() = GrpcStreamingCall<RouteNote, RouteNote> { requests, responses ->
 *    requests.consumeEach { note ->
 *      responses.send(translateNote(note))
 *    }
 *    responses.close()
 *  }
 * ```
 */
@JvmName("grpcStreamingCall")
fun <S : Any, R : Any> GrpcStreamingCall(
  function: suspend (ReceiveChannel<S>, SendChannel<R>) -> Unit,
): GrpcStreamingCall<S, R> {
  return object : GrpcStreamingCall<S, R> {
    @Suppress("UNCHECKED_CAST")
    override val method: GrpcMethod<S, R>
      get() = GrpcMethod(
        path = "/wire/AnonymousEndpoint",
        requestAdapter = ProtoAdapter.BYTES,
        responseAdapter = ProtoAdapter.BYTES,
      ) as GrpcMethod<S, R>

    private var canceled = AtomicBoolean()
    private var executed = AtomicBoolean()
    override var requestMetadata: Map<String, String> = mapOf()
    override val responseMetadata: Map<String, String>? = null
    private val requestChannel = Channel<S>(1)
    private val responseChannel = Channel<R>(1)

    override val timeout: Timeout = Timeout.NONE

    override fun cancel() {
      if (canceled.compareAndSet(false, true)) {
        requestChannel.cancel()
        responseChannel.cancel()
      }
    }

    override fun isCanceled() = canceled.get()

    override fun isExecuted() = executed.get()

    @Deprecated(
      "Provide a scope, preferably not GlobalScope",
      replaceWith = ReplaceWith("executeIn(GlobalScope)", "kotlinx.coroutines.GlobalScope"),
      level = DeprecationLevel.WARNING,
    )
    @Suppress("OPT_IN_USAGE")
    override fun execute(): Pair<SendChannel<S>, ReceiveChannel<R>> = executeIn(GlobalScope)

    override fun executeIn(scope: CoroutineScope): Pair<SendChannel<S>, ReceiveChannel<R>> {
      check(executed.compareAndSet(false, true)) { "already executed" }

      val job = scope.launch(Dispatchers.IO) {
        try {
          function(requestChannel, responseChannel)
        } catch (e: Exception) {
          requestChannel.close(e)
          responseChannel.close(e)
        }
      }

      job.invokeOnCompletion { cause ->
        requestChannel.close(cause)
        responseChannel.close(cause)
      }

      return requestChannel to responseChannel
    }

    @Suppress("OPT_IN_USAGE")
    override fun executeBlocking(): Pair<MessageSink<S>, MessageSource<R>> {
      // TODO(Benoit) Could we use a better scope here?
      executeIn(GlobalScope)
      return requestChannel.toMessageSink() to responseChannel.toMessageSource()
    }

    override fun clone() =
      GrpcStreamingCall(function).also { it.requestMetadata += requestMetadata }
  }
}

@JvmName("grpcClientStreamingCall")
fun <S : Any, R : Any> GrpcClientStreamingCall(
  function: suspend ReceiveChannel<S>.() -> R,
): GrpcClientStreamingCall<S, R> =
  GrpcStreamingCall { requests, responses ->
    val response = requests.function()
    if (response != Unit) {
      responses.send(response)
    }
  }.asGrpcClientStreamingCall()

@JvmName("grpcServerStreamingCall")
fun <S : Any, R : Any> GrpcServerStreamingCall(
  function: suspend SendChannel<R>.(S) -> Unit,
): GrpcServerStreamingCall<S, R> =
  GrpcStreamingCall { requests, responses ->
    function(responses, requests.receive())
  }.asGrpcServerStreamingCall()

internal fun <E : Any> Channel<E>.toMessageSource() = object : MessageSource<E> {
  override fun read(): E? {
    return runBlocking {
      try {
        receiveCatching()
          .onClosed { if (it != null) throw it }
          .getOrNull()
      } catch (e: Exception) {
        throw IOException(e)
      }
    }
  }

  override fun close() {
    cancel()
  }
}

internal fun <E : Any> Channel<E>.toMessageSink() = object : MessageSink<E> {
  override fun write(message: E) {
    runBlocking {
      try {
        send(message)
      } catch (e: Exception) {
        throw IOException(e)
      }
    }
  }

  override fun cancel() {
    (this@toMessageSink as Channel<*>).cancel()
  }

  override fun close() {
    this@toMessageSink.close()
  }
}
