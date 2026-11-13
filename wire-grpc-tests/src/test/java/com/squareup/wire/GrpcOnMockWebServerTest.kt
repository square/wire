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
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.squareup.wire.mockwebserver.GrpcDispatcher
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
  fun serverStreamingDoesNotNeedDispatchersIoToSendRequest() {
    enqueueListFeaturesResponse()

    val received = runWhileDispatcherIsBusy(Dispatchers.IO, ioParallelism()) {
      runBlocking {
        val responses = routeGuideService.ListFeatures().executeIn(this, rectangle)
        val result = responses.receive()
        responses.cancel()
        result
      }
    }
    assertThat(received).isEqualTo(Feature(name = "peak"))
  }

  @Test
  fun legacyServerStreamingDoesNotNeedDispatchersIoToSendRequest() {
    enqueueListFeaturesResponse()

    val received = runWhileDispatcherIsBusy(Dispatchers.IO, ioParallelism()) {
      runBlocking {
        val (requests, responses) = newLegacyListFeaturesCall().executeIn(this)
        requests.send(rectangle)
        requests.close()
        val result = responses.receive()
        responses.cancel()
        result
      }
    }
    assertThat(received).isEqualTo(Feature(name = "peak"))
  }

  @Test
  fun legacyServerStreamingBlockingDoesNotNeedCoroutineDispatchers() {
    enqueueListFeaturesResponse()

    val received = runWhileDispatcherIsBusy(Dispatchers.Default, defaultParallelism()) {
      val (requests, responses) = newLegacyListFeaturesCall().executeBlocking()
      requests.write(rectangle)
      requests.close()
      val result = responses.read()
      responses.close()
      result
    }
    assertThat(received).isEqualTo(Feature(name = "peak"))
  }

  @Test
  fun legacyServerStreamingListFeatures() {
    enqueueListFeaturesResponse()

    val listFeatures = newLegacyListFeaturesCall()

    runBlocking {
      val (requests, responses) = listFeatures.executeIn(this)
      requests.send(rectangle)
      requests.close()
      assertThat(responses.receive()).isEqualTo(Feature(name = "peak"))
      assertThat(responses.receive()).isEqualTo(Feature(name = "valley"))
      assertThat(responses.receiveCatching().getOrNull()).isNull()
      assertThat(listFeatures.isCanceled()).isFalse()
    }
  }

  /** The caller's scope only sends the request. It must not wait for unread responses. */
  @Test
  fun legacyServerStreamingDoesNotKeepCallerScopeBusy() {
    enqueueListFeaturesResponse(List(4) { Feature(name = "feature $it") })

    runBlocking {
      val responses = withTimeout(5_000) {
        coroutineScope {
          val (requests, responses) = newLegacyListFeaturesCall().executeIn(this)
          requests.send(rectangle)
          requests.close()
          assertThat(responses.receive()).isEqualTo(Feature(name = "feature 0"))
          responses
        }
      }
      responses.cancel()
    }
  }

  private val rectangle = Rectangle(lo = Point(latitude = 1, longitude = 2), hi = Point(latitude = 3, longitude = 4))

  private fun newLegacyListFeaturesCall() = grpcClient.newStreamingCall(
    GrpcMethod(
      path = "/routeguide.RouteGuide/ListFeatures",
      requestAdapter = Rectangle.ADAPTER,
      responseAdapter = Feature.ADAPTER,
      responseStreaming = true,
    ),
  )

  private fun ioParallelism() = System.getProperty("kotlinx.coroutines.io.parallelism")?.toInt()
    ?: maxOf(64, Runtime.getRuntime().availableProcessors())

  private fun defaultParallelism() = System.getProperty("kotlinx.coroutines.scheduler.core.pool.size")?.toInt()
    ?: maxOf(2, Runtime.getRuntime().availableProcessors())

  /**
   * Keeps all [parallelism] threads of [dispatcher] busy, then runs [block] on a new thread.
   * Returns the result of [block], or the exception it throws.
   */
  private fun runWhileDispatcherIsBusy(
    dispatcher: CoroutineDispatcher,
    parallelism: Int,
    block: () -> Any?,
  ): Any? {
    val started = CountDownLatch(parallelism)
    val release = CountDownLatch(1)

    @Suppress("OPT_IN_USAGE")
    val blockers = List(parallelism) {
      GlobalScope.launch(dispatcher) {
        started.countDown()
        release.await()
      }
    }
    var callThread: Thread? = null

    try {
      assertThat(started.await(10, TimeUnit.SECONDS)).isTrue()

      val result = AtomicReference<Any?>()
      val done = CountDownLatch(1)
      callThread = Thread {
        try {
          result.set(block())
        } catch (e: Throwable) {
          result.set(e)
        } finally {
          done.countDown()
        }
      }
      callThread.start()

      assertThat(done.await(1, TimeUnit.SECONDS)).isTrue()
      return result.get()
    } finally {
      release.countDown()
      callThread?.join(10_000)
      runBlocking {
        blockers.joinAll()
      }
    }
  }

  private fun enqueueListFeaturesResponse(
    features: List<Feature> = listOf(Feature(name = "peak"), Feature(name = "valley")),
  ) {
    val responseBody = Buffer()
    for (feature in features) {
      val encoded = Feature.ADAPTER.encodeByteString(feature)
      responseBody.writeByte(0) // not compressed
      responseBody.writeInt(encoded.size)
      responseBody.write(encoded)
    }
    val grpcDispatcher = mockWebServer.dispatcher
    mockWebServer.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
      override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest): MockResponse {
        if (request.path == "/routeguide.RouteGuide/ListFeatures") {
          return MockResponse()
            .setHeader("Content-Type", "application/grpc")
            .setTrailers(headersOf("grpc-status", "0"))
            .setBody(responseBody)
        }
        return grpcDispatcher.dispatch(request)
      }
    }
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
