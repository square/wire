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
package com.squareup.wire

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.squareup.wire.mockwebserver.GrpcDispatcher
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Headers.Companion.headersOf
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.GzipSink
import okio.buffer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import routeguide.Feature
import routeguide.Point
import routeguide.Rectangle
import routeguide.RouteGuideClient
import routeguide.RouteNote
import routeguide.RouteSummary

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class GrpcOnMockWebServerTest {
  @JvmField @Rule
  val mockWebServer = MockWebServer()

  @JvmField @Rule
  val timeout = Timeout(30, TimeUnit.SECONDS)

  private lateinit var okhttpClient: OkHttpClient
  private lateinit var grpcClient: GrpcClient
  private lateinit var routeGuideService: RouteGuideClient
  private var callReference = AtomicReference<Call>()
  private val fakeRouteGuide = FakeRouteGuide()

  /** This is a pass through interceptor that tests can replace without extra plumbing. */
  private var interceptor: Interceptor = object : Interceptor {
    override fun intercept(chain: Interceptor.Chain) = chain.proceed(chain.request())
  }

  @Before
  fun setUp() {
    mockWebServer.protocols = listOf(Protocol.H2_PRIOR_KNOWLEDGE)

    okhttpClient = OkHttpClient.Builder()
      .addInterceptor { chain ->
        callReference.set(chain.call())
        interceptor.intercept(chain)
      }
      .protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE))
      .build()
    grpcClient = GrpcClient.Builder()
      .client(okhttpClient)
      .baseUrl(mockWebServer.url("/"))
      .build()
    routeGuideService = grpcClient.create(RouteGuideClient::class)
  }

  @Test
  fun requestResponseSuspend() {
    mockWebServer.dispatcher = GrpcDispatcher(
      services = listOf(fakeRouteGuide),
      delegate = mockWebServer.dispatcher,
    )
    runBlocking {
      val grpcCall = routeGuideService.GetFeature()
      val feature = grpcCall.execute(Point(latitude = 5, longitude = 6))

      assertThat(feature).isEqualTo(Feature(name = "tree"))
      assertThat(fakeRouteGuide.recordedGetFeatureCalls)
        .containsExactly(Point(latitude = 5, longitude = 6))
    }
  }

  @Test
  fun rawResponse() {
    val bytes = "00ff800108120200fe".decodeHex()
    for (gzip in listOf(false, true)) {
      enqueueRawResponses(listOf(bytes), gzip)
      val call = grpcClient.newCall(rawResponseMethod)
      assertThat(call.executeBlocking(Point(latitude = 5, longitude = 6))).isEqualTo(bytes)
    }
  }

  @Test
  fun emptyRawResponse() {
    for (gzip in listOf(false, true)) {
      enqueueRawResponses(listOf(ByteString.EMPTY), gzip)
      val call = grpcClient.newCall(rawResponseMethod)
      assertThat(call.executeBlocking(Point(latitude = 5, longitude = 6))).isEqualTo(ByteString.EMPTY)
    }
  }

  @Test
  fun streamingRawResponses() {
    val messages = listOf("00ff800108120200fe".decodeHex(), ByteString.EMPTY, "ffff00".decodeHex())
    for (gzip in listOf(false, true)) {
      enqueueRawResponses(messages, gzip)
      val call = grpcClient.newStreamingCall(rawResponseMethod)
      val (requests, responses) = call.executeBlocking()
      requests.write(Point(latitude = 5, longitude = 6))
      requests.close()
      for (message in messages) {
        assertThat(responses.read()).isEqualTo(message)
      }
      assertThat(responses.read()).isNull()
      responses.close()
    }
  }

  private val rawResponseMethod = GrpcMethod(
    path = "/opaque.Service/Response",
    requestAdapter = Point.ADAPTER,
    responseAdapter = ProtoAdapter.BYTES,
  )

  private fun enqueueRawResponses(messages: List<ByteString>, gzip: Boolean) {
    val body = Buffer()
    for (message in messages) {
      val encoded = if (gzip) {
        val compressed = Buffer()
        GzipSink(compressed).buffer().use { it.write(message) }
        compressed.readByteString()
      } else {
        message
      }
      body.writeByte(if (gzip) 1 else 0)
      body.writeInt(encoded.size)
      body.write(encoded)
    }
    mockWebServer.enqueue(
      MockResponse()
        .setHeader("Content-Type", "application/grpc")
        .setHeader("grpc-encoding", if (gzip) "gzip" else "identity")
        .setTrailers(headersOf("grpc-status", "0"))
        .setBody(body),
    )
  }

  class FakeRouteGuide : RouteGuideClient {
    val recordedGetFeatureCalls = mutableListOf<Point>()

    override fun GetFeature() = GrpcCall<Point, Feature> { request ->
      recordedGetFeatureCalls += request
      return@GrpcCall Feature(name = "tree")
    }

    override fun ListFeatures(): GrpcServerStreamingCall<Rectangle, Feature> {
      TODO("Not yet implemented")
    }

    override fun RecordRoute(): GrpcClientStreamingCall<Point, RouteSummary> {
      TODO("Not yet implemented")
    }

    override fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote> {
      TODO("Not yet implemented")
    }
  }
}
