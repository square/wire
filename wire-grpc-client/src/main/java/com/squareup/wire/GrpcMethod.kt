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

import com.squareup.wire.internal.genericParameterType
import com.squareup.wire.internal.rawType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.toChannel
import java.lang.reflect.Method
import java.lang.reflect.WildcardType
import kotlin.coroutines.CoroutineContext

internal sealed class GrpcMethod<S, R>(
  val path: String,
  val requestAdapter: ProtoAdapter<S>,
  val responseAdapter: ProtoAdapter<R>
) {
  companion object {
    internal fun <S, R> Method.toGrpc(): GrpcMethod<S, R> {
      val wireRpc = getAnnotation(WireRpc::class.java)
      val requestAdapter = ProtoAdapter.get(wireRpc.requestAdapter) as ProtoAdapter<S>
      val responseAdapter = ProtoAdapter.get(wireRpc.responseAdapter) as ProtoAdapter<R>

      // TODO(oldergod) Clean up those reflection calls into util methods.
      if (genericParameterTypes.size == 1) {
        // Request is streaming.
        val continuation = genericParameterTypes[0]
        val pairReturnType =
            (continuation.genericParameterType() as WildcardType).lowerBounds[0]

        val responseStreaming =
            pairReturnType.genericParameterType(1).rawType() == ReceiveChannel::class.java
        return if (responseStreaming) {
          FullDuplex(wireRpc.path, requestAdapter, responseAdapter)
        } else {
          StreamingRequest(wireRpc.path, requestAdapter, responseAdapter)
        }
      } else {
        val responseStreaming =
            (genericParameterTypes.last().genericParameterType() as WildcardType).lowerBounds[0].rawType() == ReceiveChannel::class.java
        return if (responseStreaming) {
          StreamingResponse(wireRpc.path, requestAdapter, responseAdapter)
        } else {
          RequestResponse(wireRpc.path, requestAdapter, responseAdapter)
        }
      }
    }
  }

  /** Single request, single response. */
  class RequestResponse<S, R>(
    path: String,
    requestAdapter: ProtoAdapter<S>,
    responseAdapter: ProtoAdapter<R>
  ) : GrpcMethod<S, R>(path, requestAdapter, responseAdapter) {
    suspend fun invoke(
      context: CoroutineContext,
      grpcClient: GrpcClient,
      parameter: Any
    ): Any {
      val requestChannel = Channel<S>(0)
      val responseChannel = Channel<R>(0)

      grpcClient.call(this, context, requestChannel, responseChannel)

      requestChannel.send(parameter as S)
      requestChannel.close()
      return responseChannel.consume { responseChannel.receive() } as Any
    }
  }

  /** Request is streaming, with one single response. */
  class StreamingRequest<S, R>(
    path: String,
    requestAdapter: ProtoAdapter<S>,
    responseAdapter: ProtoAdapter<R>
  ) : GrpcMethod<S, R>(path, requestAdapter, responseAdapter) {
    fun invoke(
      context: CoroutineContext,
      grpcClient: GrpcClient
    ): Any {
      val requestChannel = Channel<S>(0)
      val responseChannel = Channel<R>(0)

      grpcClient.call(this, context, requestChannel, responseChannel)

      return Pair(
          requestChannel,
          CoroutineScope(context).async { responseChannel.consume { responseChannel.receive() } }
      )
    }
  }

  /** Single request, and response is streaming. */
  class StreamingResponse<S, R>(
    path: String,
    requestAdapter: ProtoAdapter<S>,
    responseAdapter: ProtoAdapter<R>
  ) : GrpcMethod<S, R>(path, requestAdapter, responseAdapter) {
    fun invoke(
      context: CoroutineContext,
      grpcClient: GrpcClient,
      parameter: Any
    ): Any {
      val requestChannel = Channel<S>(0)
      val responseChannel = Channel<R>(0)

      grpcClient.call(this, context, requestChannel, responseChannel)

      return CoroutineScope(context).produce<Any> {
        requestChannel.consume { requestChannel.send(parameter as S) }
        (responseChannel as Channel<Any>).toChannel(channel)
      }
    }
  }

  /** Request and response are both streaming. */
  class FullDuplex<S, R>(
    path: String,
    requestAdapter: ProtoAdapter<S>,
    responseAdapter: ProtoAdapter<R>
  ) : GrpcMethod<S, R>(path, requestAdapter, responseAdapter) {
    fun invoke(
      context: CoroutineContext,
      grpcClient: GrpcClient
    ): Any {
      val requestChannel = Channel<S>(0)
      val responseChannel = Channel<R>(0)
      grpcClient.call(this, context, requestChannel, responseChannel)
      return requestChannel to responseChannel
    }
  }
}