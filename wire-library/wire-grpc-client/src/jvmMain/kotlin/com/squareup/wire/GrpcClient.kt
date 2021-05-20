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

import com.squareup.wire.internal.RealGrpcCall
import com.squareup.wire.internal.RealGrpcStreamingCall
import kotlin.reflect.KClass
import okhttp3.Call
import okhttp3.OkHttpClient

actual class GrpcClient private constructor(
  internal val client: Call.Factory,
  internal val baseUrl: GrpcHttpUrl,
  internal val minMessageToCompress: Long
) {
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
    return Class.forName(implementationName)
  }

  actual fun <S : Any, R : Any> newCall(method: GrpcMethod<S, R>): GrpcCall<S, R> {
    return RealGrpcCall(this, method)
  }

  actual fun <S : Any, R : Any> newStreamingCall(
    method: GrpcMethod<S, R>
  ): GrpcStreamingCall<S, R> {
    return RealGrpcStreamingCall(this, method)
  }

  fun newBuilder(): Builder {
    return Builder()
      .callFactory(client)
      .baseUrl(baseUrl)
      .minMessageToCompress(minMessageToCompress)
  }

  internal fun newCall(
    method: GrpcMethod<*, *>,
    requestMetadata: Map<String, String>,
    requestBody: GrpcRequestBody
  ): Call {
    return client.newCall(GrpcRequestBuilder()
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
      }
      .tag(GrpcMethod::class.java, method)
      .method("POST", requestBody)
      .build())
  }

  class Builder {
    private var client: Call.Factory? = null
    private var baseUrl: GrpcHttpUrl? = null
    private var minMessageToCompress: Long = 0L

    fun client(client: OkHttpClient): Builder = callFactory(client)

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

    fun build(): GrpcClient = GrpcClient(
      client = client ?: throw IllegalArgumentException("client is not set"),
      baseUrl = baseUrl ?: throw IllegalArgumentException("baseUrl is not set"),
      minMessageToCompress = minMessageToCompress
    )
  }
}
