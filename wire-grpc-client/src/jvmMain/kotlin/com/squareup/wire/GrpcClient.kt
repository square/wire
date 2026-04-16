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
package com.squareup.wire

import com.squareup.wire.internal.DEFAULT_MAX_INBOUND_MESSAGE_SIZE
import com.squareup.wire.internal.RealGrpcCall
import com.squareup.wire.internal.RealGrpcServerStreamingCall
import com.squareup.wire.internal.RealGrpcStreamingCall
import com.squareup.wire.internal.asGrpcClientStreamingCall
import com.squareup.wire.internal.asGrpcStreamingCall
import com.squareup.wire.internal.remainingNanos
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol.H2_PRIOR_KNOWLEDGE
import okhttp3.Protocol.HTTP_2
import okio.Timeout

actual abstract class GrpcClient actual constructor() {
  actual abstract fun <S : Any, R : Any> newCall(method: GrpcMethod<S, R>): GrpcCall<S, R>

  actual abstract fun <S : Any, R : Any> newStreamingCall(method: GrpcMethod<S, R>): GrpcStreamingCall<S, R>

  actual abstract fun <S : Any, R : Any> newClientStreamingCall(method: GrpcMethod<S, R>): GrpcClientStreamingCall<S, R>

  actual abstract fun <S : Any, R : Any> newServerStreamingCall(method: GrpcMethod<S, R>): GrpcServerStreamingCall<S, R>

  /** Returns a [T] that makes gRPC calls using this client. */
  inline fun <reified T : Service> create(): T = create(T::class)

  /** Returns a [service] that makes gRPC calls using this client. */
  fun <T : Service> create(service: KClass<T>): T {
    // Use reflection to find the implementing class like "routeguide.GrpcRouteGuideClient" for a
    // generated interface like "routeguide.RouteGuideClient".
    try {
      val implementationClass = implementationClass(service)
      val onlyConstructor = implementationClass.declaredConstructors.single()
      val instance = onlyConstructor.newInstance(this)
      return service.java.cast(instance)
    } catch (_: Exception) {
      error("failed to create gRPC class for $service: is it a Wire-generated gRPC interface?")
    }
  }

  private fun <T : Service> implementationClass(service: KClass<T>): Class<*> {
    val interfaceName = service.qualifiedName!!
    val simpleNameOffset = interfaceName.lastIndexOf(".") + 1
    val packageName = interfaceName.substring(0, simpleNameOffset)
    val interfaceSimpleName = interfaceName.substring(simpleNameOffset)
    val implementationName = "${packageName}Grpc$interfaceSimpleName"
    return Class.forName(implementationName, false, service.java.classLoader)
  }

  fun newBuilder(): Builder {
    check(this is WireGrpcClient) { "newBuilder is not available for custom implementation of GrpcClient" }
    return Builder()
      .callFactory(client)
      .baseUrl(baseUrl)
      .minMessageToCompress(minMessageToCompress)
      .callTimeout(callTimeoutNanos, TimeUnit.NANOSECONDS)
      .maxInboundMessageSize(maxInboundMessageSize)
  }

  internal fun newCall(
    method: GrpcMethod<*, *>,
    requestMetadata: Map<String, String>,
    requestBody: GrpcRequestBody,
    timeout: Timeout,
  ): Call {
    check(this is WireGrpcClient) { "newCall is not available for custom implementation of GrpcClient" }
    return client.newCall(
      GrpcRequestBuilder()
        .url(baseUrl.resolve(method.path)!!)
        .addHeader("te", "trailers")
        .addHeader("grpc-trace-bin", "")
        .addHeader("grpc-accept-encoding", "gzip")
        .apply {
          if (minMessageToCompress < Long.MAX_VALUE) {
            addHeader("grpc-encoding", "gzip")
          }
          for ((key, value) in requestMetadata) {
            addHeader(key, value)
          }

          // Send the time left until the first of the timeout and the deadline expires.
          timeout.remainingNanos()?.let { remainingNanos ->
            addHeader("grpc-timeout", serializeTimeout(remainingNanos.coerceAtLeast(1L)))
          }
        }
        .tag(GrpcMethod::class.java, method)
        .method("POST", requestBody)
        .build(),
    )
  }

  /**
   * Return a string that represents the timeout in gRPC wire format. The Timeout value must be a
   * positive integer as an ASCII string of at most 8 digits. We will increase the TimeoutUnit to
   * fit the 8-digit limit.
   */
  private fun serializeTimeout(timeoutNanos: Long): String {
    val cutoff: Long = 100000000
    val unit = TimeUnit.NANOSECONDS
    return if (timeoutNanos < 0) {
      throw IllegalArgumentException("Timeout too small")
    } else if (timeoutNanos < cutoff) {
      timeoutNanos.toString() + "n"
    } else if (timeoutNanos < cutoff * 1000L) {
      unit.toMicros(timeoutNanos).toString() + "u"
    } else if (timeoutNanos < cutoff * 1000L * 1000L) {
      unit.toMillis(timeoutNanos).toString() + "m"
    } else if (timeoutNanos < cutoff * 1000L * 1000L * 1000L) {
      unit.toSeconds(timeoutNanos).toString() + "S"
    } else if (timeoutNanos < cutoff * 1000L * 1000L * 1000L * 60L) {
      unit.toMinutes(timeoutNanos).toString() + "M"
    } else {
      unit.toHours(timeoutNanos).toString() + "H"
    }
  }

  class Builder {
    private var client: Call.Factory? = null
    private var baseUrl: GrpcHttpUrl? = null
    private var minMessageToCompress: Long = 0L
    private var callTimeoutNanos: Long = 0L
    private var maxInboundMessageSize: Long = DEFAULT_MAX_INBOUND_MESSAGE_SIZE

    fun client(client: OkHttpClient): Builder {
      require(client.protocols.contains(HTTP_2) || client.protocols.contains(H2_PRIOR_KNOWLEDGE)) {
        "OkHttpClient is not configured with a HTTP/2 protocol which is required for gRPC connections."
      }
      return callFactory(client)
    }

    fun callFactory(client: Call.Factory): Builder = apply {
      this.client = client
    }

    fun baseUrl(baseUrl: String): Builder = apply {
      this.baseUrl = baseUrl.toHttpUrl()
    }

    fun baseUrl(url: GrpcHttpUrl): Builder = apply {
      this.baseUrl = url
    }

    /**
     * Sets the minimum outbound message size (in bytes) that will be compressed.
     *
     * Set this to 0 to enable compression for all outbound messages. Set to [Long.MAX_VALUE] to
     * disable compression.
     *
     * This is 0 by default.
     */
    fun minMessageToCompress(bytes: Long) = apply {
      require(bytes >= 0) {
        "minMessageToCompress must not be negative: $bytes"
      }
      this.minMessageToCompress = bytes
    }

    /**
     * Sets the default timeout of each call that this client makes. Like a gRPC deadline, the
     * timeout starts when the call executes, and it covers the full lifetime of the call. For a
     * streaming call, this includes all the messages, so configure a sufficiently long timeout for
     * long-running streams.
     *
     * The client sends the timeout to the server in the `grpc-timeout` header, and it also enforces
     * the timeout itself. An expired call fails with a [GrpcException] that has
     * [GrpcStatus.DEADLINE_EXCEEDED].
     *
     * A call can change this value before it executes with [GrpcCall.timeout] or
     * [GrpcStreamingCall.timeout]. Use `timeout.clearTimeout()` to remove it from one call.
     *
     * This is different from [OkHttpClient.Builder.callTimeout], which stops when a streaming call
     * receives its response headers.
     *
     * Set this to 0 for no timeout. This is 0 by default.
     */
    fun callTimeout(timeout: Long, unit: TimeUnit): Builder = apply {
      require(timeout >= 0) { "callTimeout must not be negative: $timeout" }
      this.callTimeoutNanos = unit.toNanos(timeout)
    }

    /**
     * Sets the largest response message (in bytes) that this client accepts.
     * A larger message fails the call with a [GrpcException] that has [GrpcStatus.RESOURCE_EXHAUSTED].
     *
     * The limit applies to each message, both as it is on the wire and after decompression.
     * This means that a compressed message is also rejected when its compressed frame is over the limit, like in grpc-java.
     * It does not limit the number of messages in a stream.
     *
     * This is 64 MiB by default.
     * Use a lower value if the server is not trusted.
     * Pass [Long.MAX_VALUE] to remove the limit and accept any message, as earlier versions did.
     * A frame is still at most 4 GiB.
     */
    fun maxInboundMessageSize(bytes: Long): Builder = apply {
      require(bytes >= 0) { "maxInboundMessageSize must not be negative: $bytes" }
      this.maxInboundMessageSize = bytes
    }

    fun build(): GrpcClient = WireGrpcClient(
      client = client ?: throw IllegalArgumentException("client is not set"),
      baseUrl = baseUrl ?: throw IllegalArgumentException("baseUrl is not set"),
      minMessageToCompress = minMessageToCompress,
      callTimeoutNanos = callTimeoutNanos,
      maxInboundMessageSize = maxInboundMessageSize,
    )
  }
}

internal class WireGrpcClient internal constructor(
  internal val client: Call.Factory,
  internal val baseUrl: GrpcHttpUrl,
  internal val minMessageToCompress: Long,
  internal val callTimeoutNanos: Long,
  internal val maxInboundMessageSize: Long,
) : GrpcClient() {
  override fun <S : Any, R : Any> newCall(method: GrpcMethod<S, R>): GrpcCall<S, R> = RealGrpcCall(this, method)

  override fun <S : Any, R : Any> newStreamingCall(method: GrpcMethod<S, R>): GrpcStreamingCall<S, R> = if (!method.requestStreaming && method.responseStreaming) {
    RealGrpcServerStreamingCall(this, method).asGrpcStreamingCall()
  } else {
    RealGrpcStreamingCall(this, method)
  }

  override fun <S : Any, R : Any> newClientStreamingCall(method: GrpcMethod<S, R>): GrpcClientStreamingCall<S, R> = RealGrpcStreamingCall(this, method).asGrpcClientStreamingCall()

  override fun <S : Any, R : Any> newServerStreamingCall(method: GrpcMethod<S, R>): GrpcServerStreamingCall<S, R> = RealGrpcServerStreamingCall(this, method)
}
