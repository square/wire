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
@file:Suppress(
  "CANNOT_OVERRIDE_INVISIBLE_MEMBER",
  "INVISIBLE_MEMBER",
  "INVISIBLE_REFERENCE",
)
// Above is a hack to use GrpcMessageSink and GrpcMessageSource from wire-grpc-client.

package com.squareup.wire.mockwebserver

import com.squareup.wire.GrpcCall
import com.squareup.wire.GrpcClient
import com.squareup.wire.GrpcMethod
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.Service
import com.squareup.wire.internal.GrpcMessageSink
import com.squareup.wire.internal.GrpcMessageSource
import java.lang.reflect.Method
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers.Companion.headersOf
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import okio.Timeout

/**
 * Serves gRPC calls using MockWebServer over HTTP/2.
 *
 * Use this instead of MockWebServer's default dispatcher. It can delegate to that dispatcher for
 * non-gRPC calls.
 *
 * ```
 *   @JvmField @Rule val mockWebServer = MockWebServer()
 *
 *   val myFakeService = MyFakeService()
 *
 *   @Before
 *   fun setUp() {
 *     mockWebServer.dispatcher = GrpcDispatcher(
 *       services = listOf(myFakeService),
 *       delegate = mockWebServer.dispatcher
 *     )
 *   }
 * ```
 *
 * **WARNING**: This class does not currently support streaming calls.
 */
class GrpcDispatcher(
  services: List<Service>,
  val delegate: Dispatcher,
) : Dispatcher() {
  private val endpoints: Map<String, Endpoint<*, *>> = run {
    val map = mutableMapOf<String, Endpoint<*, *>>()
    for (service in services) {
      map += service.endpoints()
    }
    return@run map
  }

  /** Returns endpoints indexed by path. */
  private fun Service.endpoints(): Map<String, Endpoint<*, *>> {
    // TODO(oldergod): find a better way to get the GrpcCall objects or a stub GrpcClient.

    val result = mutableMapOf<String, Endpoint<*, *>>()
    val grpcInterfaces = mutableSetOf<Class<out Service>>()
    collectGrpcInterfaces(grpcInterfaces, this::class.java)

    for (grpcInterface in grpcInterfaces) {
      val grpcClientClass = nullGrpcClient.create(grpcInterface.kotlin)

      for (javaMethod in grpcInterface.methods) {
        val grpcCall = javaMethod.invoke(grpcClientClass) as? GrpcCall<*, *> ?: continue
        val grpcMethod = grpcCall.method
        // TODO(oldergod): recover gracefully if multiple services define the same endpoint.
        result[grpcMethod.path] = Endpoint(grpcMethod, javaMethod, this)
      }
    }
    return result
  }

  /**
   * Given an arbitrary instance of a Wire-generated gRPC service, collect the Wire-generated
   * interface types. For example, if [type] is a `MyRouteGuide` instance, this would put
   * `RouteGuide::class.java` into [sink].
   */
  private fun collectGrpcInterfaces(sink: MutableSet<Class<out Service>>, type: Class<*>) {
    val interfaces = type.interfaces

    if (type.isInterface &&
      interfaces.size == 1 &&
      interfaces[0] == Service::class.java
    ) {
      @Suppress("UNCHECKED_CAST") // Checked reflectively above.
      sink += type as Class<out Service>
      return
    }

    for (parentInterface in interfaces) {
      collectGrpcInterfaces(sink, parentInterface)
    }

    val superclass = type.superclass
    if (superclass != null) {
      collectGrpcInterfaces(sink, superclass)
    }
  }

  override fun dispatch(request: RecordedRequest): MockResponse {
    val endpoint = endpoints[request.path] ?: return delegate.dispatch(request)

    if (request.headers["content-type"] != "application/grpc" ||
      request.method != "POST"
    ) {
      return delegate.dispatch(request)
    }

    return dispatchGrpc(endpoint, request)
  }

  private fun <S : Any, R : Any> dispatchGrpc(
    endpoint: Endpoint<S, R>,
    recordedRequest: RecordedRequest,
  ): MockResponse {
    // TODO(oldergod): recover gracefully if the parameters don't decode to the expected types.

    val request = decodeRequest(recordedRequest, endpoint.grpcMethod.requestAdapter)

    val grpcCall = endpoint.newGrpcCall()

    // TODO(oldergod): send a non-0 grpc-status if this throws an exception.
    val response = grpcCall.executeBlocking(request)

    val responseBody = encodeResponse(response, endpoint.grpcMethod.responseAdapter)

    return MockResponse()
      .setHeader("grpc-encoding", "identity")
      .setHeader("grpc-accept-encoding", "gzip")
      .setHeader("Content-Type", "application/grpc")
      .setTrailers(headersOf("grpc-status", "0"))
      .setBody(responseBody)
  }

  private fun <S : Any> decodeRequest(
    request: RecordedRequest,
    protoAdapter: ProtoAdapter<S>,
  ): S {
    val source = GrpcMessageSource(
      source = request.body,
      messageAdapter = protoAdapter,
      grpcEncoding = request.headers["grpc-encoding"],
    )
    return source.readExactlyOneAndClose()
  }

  private fun <R : Any> encodeResponse(
    response: R,
    protoAdapter: ProtoAdapter<R>,
  ): Buffer {
    val result = Buffer()
    GrpcMessageSink(
      sink = result,
      minMessageToCompress = 0L,
      messageAdapter = protoAdapter,
      callForCancel = null,
      grpcEncoding = "identity",
    ).use {
      it.write(response)
    }
    return result
  }

  private class Endpoint<S : Any, R : Any>(
    val grpcMethod: GrpcMethod<S, R>,
    val javaMethod: Method,
    val service: Service,
  ) {
    @Suppress("UNCHECKED_CAST") // The GrpcCall type and GrpcMethod type always align.
    fun newGrpcCall(): GrpcCall<S, R> = javaMethod.invoke(service) as GrpcCall<S, R>
  }

  companion object {
    object NullCall : Call {
      override fun cancel() = error("unexpected call")
      override fun clone() = error("unexpected call")
      override fun enqueue(responseCallback: Callback) = error("unexpected call")
      override fun execute() = error("unexpected call")
      override fun isCanceled() = error("unexpected call")
      override fun isExecuted() = error("unexpected call")
      override fun request() = error("unexpected call")
      override fun timeout() = Timeout.NONE
    }

    /**
     * We need a [GrpcClient] to create instances to inspect metadata. We never execute the
     * corresponding calls.
     */
    private val nullGrpcClient = GrpcClient.Builder()
      .callFactory { NullCall }
      .baseUrl("https://localhost/")
      .build()
  }
}
