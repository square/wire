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

import com.squareup.wire.internal.*
import com.squareup.wire.GrpcCodec
import com.squareup.wire.internal.RealGrpcCall
import com.squareup.wire.internal.RealGrpcStreamingCall
import okhttp3.Call
import okhttp3.OkHttpClient
import kotlin.reflect.KClass

actual class GrpcClient private constructor(
  internal val client: Call.Factory,
  internal val baseUrl: GrpcHttpUrl,
  internal val codec: GrpcCodec
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

  internal fun newCall(method: GrpcMethod<*, *>, requestBody: GrpcRequestBody): Call {
    return client.newCall(GrpcRequestBuilder()
        .url(baseUrl.resolve(method.path)!!)
        .addHeader("te", "trailers")
        .addHeader("grpc-trace-bin", "")
        .apply {
          if (codec !is GrpcCodec.IdentityGrpcCodec) {
            addHeader("grpc-accept-encoding", codec.name)
            addHeader("grpc-encoding", codec.name)
          } else {
            addHeader("grpc-accept-encoding", "gzip")
          }
        }
        .tag(GrpcMethod::class.java, method)
        .method("POST", requestBody)
        .build())
  }

  class Builder {
    private var client: Call.Factory? = null
    private var baseUrl: GrpcHttpUrl? = null
    private var codec: GrpcCodec? = null

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

    fun encoding(codec: GrpcCodec) : Builder = apply {
      this.codec = codec
    }

    fun build(): GrpcClient = GrpcClient(
            client = client!!,
            baseUrl = baseUrl!!,
            codec = codec ?: GrpcCodec.IdentityGrpcCodec
    )
  }
}
