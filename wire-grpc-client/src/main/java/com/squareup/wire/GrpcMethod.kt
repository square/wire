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
package com.squareup.wire

import com.squareup.wire.internal.BlockingMessageSource
import com.squareup.wire.internal.genericParameterType
import com.squareup.wire.internal.invokeSuspending
import com.squareup.wire.internal.messageSink
import com.squareup.wire.internal.newDuplexRequestBody
import com.squareup.wire.internal.newRequestBody
import com.squareup.wire.internal.rawType
import com.squareup.wire.internal.readFromResponseBodyCallback
import com.squareup.wire.internal.unconfinedCoroutineScope
import com.squareup.wire.internal.writeToRequestBody
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.toChannel
import okhttp3.Call
import okhttp3.RequestBody
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.net.ProtocolException
import kotlin.coroutines.Continuation

internal sealed class GrpcMethod<S : Any, R : Any>(
  val path: String,
  val requestAdapter: ProtoAdapter<S>,
  val responseAdapter: ProtoAdapter<R>
) {
  /** Handle a dynamic proxy method call to this. */
  abstract fun invoke(grpcClient: GrpcClient, args: Array<Any>): Any

  /**
   * Cancellation of the [Call] is always tied to the response. To cancel the call, either cancel
   * the response channel, deferred, or suspending function call.
   */
  private fun callWithChannels(grpcClient: GrpcClient): Pair<Channel<S>, Channel<R>> {
    val requestChannel = Channel<S>(1)
    val responseChannel = Channel<R>(1)

    val requestBody = newDuplexRequestBody()
    val call = grpcClient.newCall(path, requestBody)
    requestChannel.writeToRequestBody(requestBody, requestAdapter)
    call.enqueue(responseChannel.readFromResponseBodyCallback(responseAdapter))

    responseChannel.invokeOnClose { cause ->
      call.cancel()
      requestChannel.cancel()
      responseChannel.cancel()
    }

    return requestChannel to responseChannel
  }

  private fun callBlocking(
    grpcClient: GrpcClient,
    requestBody: RequestBody
  ): MessageSource<R> {
    val call = grpcClient.newCall(path, requestBody)
    val messageSource = BlockingMessageSource(call, responseAdapter)
    call.enqueue(messageSource.readFromResponseBodyCallback())
    return messageSource
  }

  /** Single request, single response. */
  class RequestResponse<S : Any, R : Any>(
    path: String,
    requestAdapter: ProtoAdapter<S>,
    responseAdapter: ProtoAdapter<R>
  ) : GrpcMethod<S, R>(path, requestAdapter, responseAdapter) {
    override fun invoke(grpcClient: GrpcClient, args: Array<Any>): Any {
      return (args.last() as Continuation<Any>).invokeSuspending {
        invoke(grpcClient, parameter = args[0])
      }
    }

    suspend fun invoke(
      grpcClient: GrpcClient,
      parameter: Any
    ): R {
      val (requestChannel, responseChannel) = super.callWithChannels(grpcClient)

      responseChannel.consume {
        requestChannel.send(parameter as S)
        requestChannel.close()
        return receive()
      }
    }
  }

  /** Single request, single response. */
  class BlockingRequestResponse<S : Any, R : Any>(
    path: String,
    requestAdapter: ProtoAdapter<S>,
    responseAdapter: ProtoAdapter<R>
  ) : GrpcMethod<S, R>(path, requestAdapter, responseAdapter) {
    override fun invoke(grpcClient: GrpcClient, args: Array<Any>): Any {
      val requestBody = newRequestBody(requestAdapter, args[0] as S)
      val messageSource = super.callBlocking(grpcClient, requestBody)

      messageSource.use {
        return messageSource.read() ?: throw ProtocolException("required message not sent")
      }
    }
  }

  /** Request is streaming, with one single response. */
  class StreamingRequest<S : Any, R : Any>(
    path: String,
    requestAdapter: ProtoAdapter<S>,
    responseAdapter: ProtoAdapter<R>
  ) : GrpcMethod<S, R>(path, requestAdapter, responseAdapter) {
    override fun invoke(
      grpcClient: GrpcClient,
      args: Array<Any>
    ): Pair<SendChannel<S>, Deferred<R>> {
      val (requestChannel, responseChannel) = super.callWithChannels(grpcClient)

      return Pair(
          requestChannel,
          // We use ATOMIC to guarantee the coroutine doesn't start after potential cancellations
          // happen.
          unconfinedCoroutineScope.async(start = CoroutineStart.ATOMIC) {
            responseChannel.consume { receive() }
          }
      )
    }
  }

  /** Single request, and response is streaming. */
  class StreamingResponse<S : Any, R : Any>(
    path: String,
    requestAdapter: ProtoAdapter<S>,
    responseAdapter: ProtoAdapter<R>
  ) : GrpcMethod<S, R>(path, requestAdapter, responseAdapter) {
    override fun invoke(grpcClient: GrpcClient, args: Array<Any>): Any {
      val (requestChannel, responseChannel) = super.callWithChannels(grpcClient)

      // TODO(benoit) Remove the cancellation handling once this ships:
      //     https://github.com/Kotlin/kotlinx.coroutines/issues/845
      unconfinedCoroutineScope.coroutineContext[Job]!!.invokeOnCompletion {
        responseChannel.cancel()
      }
      return unconfinedCoroutineScope.produce<Any> {
        requestChannel.send(args[0] as S)
        requestChannel.close()
        responseChannel.toChannel(channel)
      }
    }
  }

  /** Single request, and response is streaming. */
  class BlockingStreamingResponse<S : Any, R : Any>(
    path: String,
    requestAdapter: ProtoAdapter<S>,
    responseAdapter: ProtoAdapter<R>
  ) : GrpcMethod<S, R>(path, requestAdapter, responseAdapter) {
    override fun invoke(grpcClient: GrpcClient, args: Array<Any>): Any {
      val requestBody = newRequestBody(requestAdapter, args[0] as S)
      return super.callBlocking(grpcClient, requestBody)
    }
  }

  /** Request and response are both streaming. */
  class FullDuplex<S : Any, R : Any>(
    path: String,
    requestAdapter: ProtoAdapter<S>,
    responseAdapter: ProtoAdapter<R>
  ) : GrpcMethod<S, R>(path, requestAdapter, responseAdapter) {
    override fun invoke(
      grpcClient: GrpcClient,
      args: Array<Any>
    ): Pair<SendChannel<S>, ReceiveChannel<R>> = super.callWithChannels(grpcClient)
  }

  /** Request and response are both streaming. */
  class BlockingFullDuplex<S : Any, R : Any>(
    path: String,
    requestAdapter: ProtoAdapter<S>,
    responseAdapter: ProtoAdapter<R>
  ) : GrpcMethod<S, R>(path, requestAdapter, responseAdapter) {
    override fun invoke(
      grpcClient: GrpcClient, args: Array<Any>
    ): Pair<MessageSink<S>, MessageSource<R>> {
      val requestBody = newDuplexRequestBody()
      val messageSink = requestBody.messageSink(requestAdapter)
      val messageSource = super.callBlocking(grpcClient, requestBody)
      return messageSink to messageSource
    }
  }

  internal companion object {
    internal fun <S : Any, R : Any> Method.toGrpc(): GrpcMethod<S, R> {
      val wireRpc = getAnnotation(WireRpc::class.java)
      val requestAdapter = ProtoAdapter.get(wireRpc.requestAdapter) as ProtoAdapter<S>
      val responseAdapter = ProtoAdapter.get(wireRpc.responseAdapter) as ProtoAdapter<R>
      val parameterTypes = genericParameterTypes
      val returnType = genericReturnType

      if (parameterTypes.size == 2) {
        // Coroutines request-response.
        // Object methodName(RequestType, Continuation)
        if (parameterTypes[1].rawType() == Continuation::class.java) {
          return RequestResponse(wireRpc.path, requestAdapter, responseAdapter)
        }

      } else if (parameterTypes.size == 1) {
        // Coroutines streaming response.
        // ReceiveChannel<ResponseType> methodName(RequestType)
        if (returnType.rawType() == ReceiveChannel::class.java) {
          return StreamingResponse(wireRpc.path, requestAdapter, responseAdapter)
        }

        // Blocking streaming response.
        // MessageSource<ResponseType> methodName(RequestType)
        if (returnType.rawType() == MessageSource::class.java) {
          return BlockingStreamingResponse(wireRpc.path, requestAdapter, responseAdapter)
        }

        // Blocking request-response.
        // ResponseType methodName(RequestType)
        return BlockingRequestResponse(wireRpc.path, requestAdapter, responseAdapter)

      } else if (parameterTypes.isEmpty()) {
        if (returnType.rawType() == Pair::class.java) {
          val pairType = returnType as ParameterizedType
          val requestType = pairType.genericParameterType(index = 0).rawType()
          val responseType = pairType.genericParameterType(index = 1).rawType()

          // Coroutines full duplex.
          // Pair<SendChannel<RequestType>, ReceiveChannel<ResponseType>> methodName()
          if (requestType == SendChannel::class.java
              && responseType == ReceiveChannel::class.java) {
            return FullDuplex(wireRpc.path, requestAdapter, responseAdapter)
          }

          // Coroutines streaming request.
          // Pair<SendChannel<RequestType>, Deferred<ResponseType>> methodName()
          if (requestType == SendChannel::class.java
              && responseType == Deferred::class.java) {
            return StreamingRequest(wireRpc.path, requestAdapter, responseAdapter)
          }

          // Blocking full duplex OR streaming request. (single response could be Future instead?)
          // Pair<MessageSink<RequestType>, MessageSource<ResponseType>> methodName()
          if (requestType == MessageSink::class.java
              && responseType == MessageSource::class.java) {
            return BlockingFullDuplex(wireRpc.path, requestAdapter, responseAdapter)
          }
        }
      }

      error("unexpected gRPC method: $this")
    }
  }
}
