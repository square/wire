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

import com.squareup.wire.GrpcEncoding.Companion.toGrpcEncoding
import com.squareup.wire.internal.genericParameterType
import com.squareup.wire.internal.invokeSuspending
import com.squareup.wire.internal.rawType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.toChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.lang.reflect.WildcardType
import kotlin.coroutines.Continuation
import kotlin.reflect.KClass

internal val APPLICATION_GRPC_MEDIA_TYPE: MediaType = MediaType.get("application/grpc")

class GrpcClient private constructor(
  private val client: OkHttpClient,
  private val baseUrl: HttpUrl
) {
  fun <T : Service> create(service: KClass<T>): T {
    val methodToService: Map<Method, GrpcMethod<*, *>> =
        service.java.methods.associate { method -> method to method.toGrpc<Any, Any>() }

    return Proxy.newProxyInstance(service.java.classLoader, arrayOf<Class<*>>(service.java),
        object : InvocationHandler {
          override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any? {
            val continuation = args.last() as Continuation<Any>

            return when (val grpcMethod = methodToService[method] as GrpcMethod<*, *>) {
              is GrpcMethod.RequestResponse -> {
                continuation.invokeSuspending {
                  grpcMethod.invoke(continuation, this@GrpcClient, parameter = args[0])
                }
              }
              is GrpcMethod.StreamingResponse -> {
                grpcMethod.invoke(continuation, this@GrpcClient, parameter = args[0])
              }
              is GrpcMethod.StreamingRequest -> {
                grpcMethod.invoke(continuation, this@GrpcClient)
              }
              is GrpcMethod.FullDuplex -> {
                grpcMethod.invoke(continuation, this@GrpcClient)
              }
            }
          }
        }) as T
  }

  class Builder {
    private var client: OkHttpClient? = null
    private var baseUrl: HttpUrl? = null

    fun client(client: OkHttpClient): Builder {
      this.client = client
      return this
    }

    fun baseUrl(baseUrl: String): Builder {
      this.baseUrl = HttpUrl.parse(baseUrl)
      return this
    }

    fun build(): GrpcClient {
      return GrpcClient(client = client!!, baseUrl = baseUrl!!)
    }
  }

  internal fun <S, R> call(
    grpcMethod: GrpcMethod<S, R>,
    requestChannel: ReceiveChannel<S>,
    responseChannel: SendChannel<R>
  ): Call {
    // Create a duplex request body. It allows us to write request messages even after the response
    // status, headers, and body have been received.
    val requestBody =
        PipeDuplexRequestBody(APPLICATION_GRPC_MEDIA_TYPE, pipeMaxBufferSize = 1024 * 1024)

    // Make the HTTP call.
    val call = client.newCall(Request.Builder()
        .url(baseUrl.resolve(grpcMethod.path)!!)
        .addHeader("te", "trailers")
        .addHeader("grpc-trace-bin", "")
        .addHeader("grpc-accept-encoding", "gzip")
        .method("POST", requestBody)
        .build())

    // Stream messages from the request channel to the request body stream. This means:
    // 1. read a message (non blocking, suspending code)
    // 2. write it to the stream (blocking)
    // 3. repeat. We also have to wait for all 2s to end before closing the writer
    CoroutineScope(Dispatchers.IO).launch {
      val requestWriter = GrpcWriter.get(requestBody.createSink(), grpcMethod.requestAdapter)
      requestWriter.use {
        for (message in requestChannel) {
          requestWriter.writeMessage(message)
          requestWriter.flush()
        }
      }
    }

    call.enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        // Something broke. Kill the response channel.
        responseChannel.close(e)
      }

      override fun onResponse(call: Call, response: Response) {
        runBlocking {
          response.use {
            // Stream messages from the response body to the response channel.
            val grpcEncoding = response.header("grpc-encoding")
            val responseSource = response.body()!!.source()
            val responseReader = GrpcReader(
                responseSource, grpcMethod.responseAdapter, grpcEncoding?.toGrpcEncoding())
            responseReader.use {
              while (true) {
                val message = it.readMessage() ?: break
                responseChannel.send(message)
              }

              val grpcStatus =
                  response.trailers().get("grpc-status") ?: response.header("grpc-status")
              when (grpcStatus) {
                "0" -> responseChannel.close()
                else -> {
                  // also see https://github.com/grpc/grpc-go/blob/master/codes/codes.go#L31
                  responseChannel.close(
                      IOException("unexpected or absent grpc-status: $grpcStatus"))
                }
              }
            }
          }
        }
      }
    })
    return call
  }

  internal sealed class GrpcMethod<S, R>(
    val path: String,
    val requestAdapter: ProtoAdapter<S>,
    val responseAdapter: ProtoAdapter<R>
  ) {
    private fun invoke(
      continuation: Continuation<Any>,
      grpcClient: GrpcClient
    ): Pair<Channel<S>, Channel<R>> {
      val requestChannel = Channel<S>(0)
      val responseChannel = Channel<R>(0)

      val call = grpcClient.call(this, requestChannel, responseChannel)

      continuation.context[Job]!!.invokeOnCompletion { cause: Throwable? ->
        if (cause is CancellationException) {
          call.cancel()
          requestChannel.cancel()
          responseChannel.cancel()
        }
      }

      return requestChannel to responseChannel
    }

    /** Single request, single response. */
    class RequestResponse<S, R>(
      path: String,
      requestAdapter: ProtoAdapter<S>,
      responseAdapter: ProtoAdapter<R>
    ) : GrpcMethod<S, R>(path, requestAdapter, responseAdapter) {
      suspend fun invoke(
        continuation: Continuation<Any>,
        grpcClient: GrpcClient,
        parameter: Any
      ): Any {
        val (requestChannel, responseChannel) = super.invoke(continuation, grpcClient)

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
        continuation: Continuation<Any>,
        grpcClient: GrpcClient
      ): Any {
        val (requestChannel, responseChannel) = super.invoke(continuation, grpcClient)

        val coroutineScope = CoroutineScope(continuation.context)
        return Pair(
            requestChannel,
            coroutineScope.async {
              responseChannel.consume { responseChannel.receive() }
            }
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
        continuation: Continuation<Any>,
        grpcClient: GrpcClient,
        parameter: Any
      ): Any {
        val (requestChannel, responseChannel) = super.invoke(continuation, grpcClient)

        val coroutineScope = CoroutineScope(continuation.context)
        return coroutineScope.produce<Any> {
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
        continuation: Continuation<Any>,
        grpcClient: GrpcClient
      ): Any {
        return super.invoke(continuation, grpcClient)
      }
    }
  }

  companion object {
    internal fun <S, R> Method.toGrpc(): GrpcMethod<S, R> {
      val wireRpc = getAnnotation(WireRpc::class.java)
      val requestAdapter = ProtoAdapter.get(wireRpc.requestAdapter) as ProtoAdapter<S>
      val responseAdapter = ProtoAdapter.get(wireRpc.responseAdapter) as ProtoAdapter<R>

      if (genericParameterTypes.size == 1) {
        // Request is streaming.
        val continuation = genericParameterTypes[0]
        val pairReturnType =
            (continuation.genericParameterType() as WildcardType).lowerBounds[0]

        val responseStreaming =
            pairReturnType.genericParameterType(1).rawType() == ReceiveChannel::class.java
        return if (responseStreaming) {
          GrpcMethod.FullDuplex(wireRpc.path, requestAdapter, responseAdapter)
        } else {
          GrpcMethod.StreamingRequest(wireRpc.path, requestAdapter, responseAdapter)
        }
      } else {
        val responseStreaming =
            genericParameterTypes.last().genericParameterType().rawType() == ReceiveChannel::class.java
        return if (responseStreaming) {
          GrpcMethod.StreamingResponse(wireRpc.path, requestAdapter, responseAdapter)
        } else {
          GrpcMethod.RequestResponse(wireRpc.path, requestAdapter, responseAdapter)
        }
      }
    }
  }
}