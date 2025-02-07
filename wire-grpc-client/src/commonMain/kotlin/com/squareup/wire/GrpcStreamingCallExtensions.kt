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
package com.squareup.wire

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

/**
 * Executes a Client Streaming RPC call This encapsulates the Streaming request, single response use case for wire's
 * [GrpcStreamingCall] In this use case, all outbound request messages are sent before the inbound response message.
 *
 * @param scope The [CoroutineScope] to use for the call. This will be used to create coroutines that will write the
 *   requests to the client message sync. It will also be the context for calling the [block] function
 * @param block A function that will be called with a [SendChannel] that the caller can use to send requests to the
 *   server. The channel will automatically be closed when the block completes.
 * @return The optional single response from the server
 */
suspend inline fun <S : Any, R : Any> GrpcClientStreamingCall<S, R>.clientStream(
  scope: CoroutineScope,
  crossinline block: suspend SendChannel<S>.() -> Unit,
): R {
  val (requestChannel, responseProvider) = executeIn(scope)
  try {
    block(requestChannel)
  } finally {
    requestChannel.close()
  }
  return responseProvider.await()
}

/**
 * Executes a blocking Client Streaming RPC call This encapsulates the Streaming request, single response use case for
 * wire's [GrpcStreamingCall] In this use case, all outbound request messages are sent before the inbound response
 * message.
 *
 * @param block A function that will be called with a [MessageSink] that the caller can use to send requests to the
 *   server. The channel will automatically be closed when the block completes.
 * @return The optional single response from the server
 */
inline fun <S : Any, R : Any> GrpcClientStreamingCall<S, R>.clientStreamBlocking(
  crossinline block: MessageSink<S>.() -> Unit,
): R {
  val (sink, responseProvider) = executeBlocking()
  try {
    block(sink)
  } finally {
    sink.close()
  }
  return responseProvider.get()
}

/**
 * Executes a Bidirectional Streaming RPC call This encapsulates the Streaming request, streaming response use case for
 * wire's [GrpcStreamingCall] In this use case, you are free to interleave request and response messages. The caller can
 * optionally send and receive messages in the supplied block or after the block completes in the returned
 * [ReceiveChannel].
 *
 * @param scope The [CoroutineScope] to use for the call. This will be used to create coroutines that will write the
 *   requests to the client message sync. It will also be the context for calling the [block] function.
 * @param block A function that will be called with a [SendChannel] and [ReceiveChannel] that the caller can use to send
 *   requests to the server and consume responses from the server. The send channel will automatically be closed when
 *   the block completes.
 * @return The [ReceiveChannel] that will receive the server's streaming response. The caller can optionally consume
 *   server responses from this channel.
 */
suspend inline fun <S : Any, R : Any> GrpcStreamingCall<S, R>.bidirectionalStream(
  scope: CoroutineScope,
  crossinline block: suspend (SendChannel<S>, ReceiveChannel<R>) -> Unit,
): ReceiveChannel<R> {
  val (requestChannel, responseChannel) = executeIn(scope)
  try {
    block(requestChannel, responseChannel)
  } finally {
    requestChannel.close()
  }
  return responseChannel
}

/**
 * Executes a blocking Bidirectional Streaming RPC call This encapsulates the Streaming request, streaming response use
 * case for wire's [GrpcStreamingCall] In this use case, you are free to interleave request and response messages. The
 * caller can optionally send and receive messages in the supplied block or after the block completes in the returned
 * [MessageSource].
 *
 * @param block A function that will be called with a [MessageSink] and [MessageSource] that the caller can use to send
 *   requests to the server and consume responses from the server. The send channel will automatically be closed when
 *   the block completes.
 * @return The [MessageSource] that will receive the server's streaming response. The caller can optionally consume
 *   server responses from this channel.
 */
inline fun <S : Any, R : Any> GrpcStreamingCall<S, R>.bidirectionalStreamBlocking(
  crossinline block: (MessageSink<S>, MessageSource<R>) -> Unit,
): MessageSource<R> {
  val (sink, source) = executeBlocking()
  try {
    block(sink, source)
  } finally {
    sink.close()
  }
  return source
}
