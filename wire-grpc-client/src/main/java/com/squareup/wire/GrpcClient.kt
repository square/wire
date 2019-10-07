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

import com.squareup.wire.internal.GrpcMethod
import com.squareup.wire.internal.GrpcMethod.Companion.toGrpc
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

class GrpcClient private constructor(
  internal val client: OkHttpClient,
  internal val baseUrl: HttpUrl
) {
  fun <T : Service> create(service: KClass<T>): T {
    val methodToService: Map<Method, GrpcMethod<*, *>> =
        service.java.methods.associate { method -> method to method.toGrpc<Any, Any>() }

    return Proxy.newProxyInstance(
        service.java.classLoader,
        arrayOf<Class<*>>(service.java),
        object : InvocationHandler {
          override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any {
            val self: InvocationHandler = this
            val args = args ?: emptyArray()

            if (method.declaringClass == Object::class.java) {
              return method.invoke(self, *args)
            }

            val grpcMethod = methodToService[method] as GrpcMethod<*, *>
            return grpcMethod.invoke(this@GrpcClient, args)
          }
        }
    ) as T
  }

  internal fun newCall(path: String, requestBody: RequestBody): Call {
    return client.newCall(Request.Builder()
        .url(baseUrl.resolve(path)!!)
        .addHeader("te", "trailers")
        .addHeader("grpc-trace-bin", "")
        .addHeader("grpc-accept-encoding", "gzip")
        .addHeader("grpc-encoding", "gzip")
        .method("POST", requestBody)
        .build())
  }

  class Builder {
    private var client: OkHttpClient? = null
    private var baseUrl: HttpUrl? = null

    fun client(client: OkHttpClient): Builder {
      this.client = client
      return this
    }

    fun baseUrl(baseUrl: String): Builder {
      this.baseUrl = baseUrl.toHttpUrlOrNull()
      return this
    }

    fun baseUrl(url: HttpUrl): Builder {
      this.baseUrl = url
      return this
    }

    fun build(): GrpcClient {
      return GrpcClient(client = client!!, baseUrl = baseUrl!!)
    }
  }
}
