/*
 * Copyright (C) 2026 Square, Inc.
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

import assertk.Assert
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.matches
import assertk.assertions.prop
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.Headers.Companion.headersOf
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import okio.IOException
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import routeguide.Feature
import routeguide.Point
import routeguide.RouteGuideClient
import routeguide.RouteNote

/**
 * Proves that a timeout or a deadline set on a call's [GrpcCall.timeout] is enforced on the
 * client. By default, the server in these tests receives each request and never responds. It
 * ignores the `grpc-timeout` request header, like any server or proxy that does not implement gRPC
 * deadlines. Only client-side enforcement can bound these calls.
 *
 * The OkHttpClient has no call timeout, and its read and write timeouts are far above the bounds
 * that these tests assert. When a call fails fast, the per-call timeout did it.
 */
class GrpcClientTimeoutTest {
  @JvmField @Rule
  val mockWebServer = MockWebServer()

  @JvmField @Rule
  val testTimeout = Timeout(30, TimeUnit.SECONDS)

  private lateinit var routeGuideService: RouteGuideClient

  /** Held until the test ends. The dispatcher blocks on it, so responses never go out. */
  private val dispatcherRelease = CountDownLatch(1)

  /** Returns the response for each request. Tests replace it to send a response. */
  private var respond: (RecordedRequest) -> MockResponse = {
    dispatcherRelease.await(1, TimeUnit.MINUTES)
    MockResponse().apply { socketPolicy = SocketPolicy.NO_RESPONSE }
  }

  /** Thrown by an OkHttp interceptor when non-null. */
  private var interceptorFailure: RuntimeException? = null

  @Before
  fun setUp() {
    mockWebServer.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse = respond(request)
    }
    mockWebServer.protocols = listOf(Protocol.H2_PRIOR_KNOWLEDGE)

    val okhttpClient = OkHttpClient.Builder()
      .addInterceptor { chain ->
        interceptorFailure?.let { throw it }
        chain.proceed(chain.request())
      }
      .protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE))
      .readTimeout(Duration.ofSeconds(10))
      .writeTimeout(Duration.ofSeconds(10))
      .build()
    val grpcClient = GrpcClient.Builder()
      .client(okhttpClient)
      .baseUrl(mockWebServer.url("/"))
      .build()
    routeGuideService = grpcClient.create(RouteGuideClient::class)
  }

  @After
  fun tearDown() {
    dispatcherRelease.countDown()
  }

  @Test
  fun unaryCallTimeoutIsEnforcedOnClient() {
    val grpcCall = routeGuideService.GetFeature()
    grpcCall.timeout.timeout(500, TimeUnit.MILLISECONDS)

    val elapsedMillis = elapsedMillis {
      assertFailure {
        grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))
      }.isDeadlineExceeded()
    }

    assertThat(grpcCall.isCanceled()).isTrue()
    assertThat(elapsedMillis).isBetween(400L, 5_000L)
  }

  @Test
  fun unaryCallDeadlineIsEnforcedOnClient() {
    val grpcCall = routeGuideService.GetFeature()
    grpcCall.timeout.deadline(500, TimeUnit.MILLISECONDS)

    val elapsedMillis = elapsedMillis {
      assertFailure {
        grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))
      }.isDeadlineExceeded()
    }

    assertThat(grpcCall.isCanceled()).isTrue()
    assertThat(elapsedMillis).isBetween(400L, 5_000L)
  }

  @Test
  fun unaryCallTimeoutIsEnforcedOnClientWhenSuspending() {
    val grpcCall = routeGuideService.GetFeature()
    grpcCall.timeout.timeout(500, TimeUnit.MILLISECONDS)

    val elapsedMillis = elapsedMillis {
      assertFailure {
        runBlocking {
          grpcCall.execute(Point(latitude = 5, longitude = 6))
        }
      }.isDeadlineExceeded()
    }

    assertThat(grpcCall.isCanceled()).isTrue()
    assertThat(elapsedMillis).isBetween(400L, 5_000L)
  }

  @Test
  fun unaryCallWithDeadlineSucceedsWhenServerRespondsInTime() {
    respond = { grpcResponse(Feature.ADAPTER.encode(Feature(name = "tree"))) }
    val grpcCall = routeGuideService.GetFeature()
    grpcCall.timeout.deadline(5, TimeUnit.SECONDS)

    val feature = grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))

    assertThat(feature).isEqualTo(Feature(name = "tree"))
  }

  @Test
  fun timerStopsWhenCallFailsWithUncheckedException() {
    interceptorFailure = IllegalStateException("boom")
    val grpcCall = routeGuideService.GetFeature()
    grpcCall.timeout.timeout(200, TimeUnit.MILLISECONDS)

    assertFailure {
      grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))
    }.isInstanceOf<IllegalStateException>()

    // A timer that is still running cancels the finished call when it fires.
    Thread.sleep(600)
    assertThat(grpcCall.isCanceled()).isFalse()
  }

  @Test
  fun deadlineIsSentAsRemainingTimeInHeader() {
    respond = { grpcResponse(Feature.ADAPTER.encode(Feature(name = "tree"))) }
    val grpcCall = routeGuideService.GetFeature()
    grpcCall.timeout.deadline(2, TimeUnit.SECONDS)

    grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))

    assertThat(mockWebServer.takeRequest().grpcTimeoutMicros()).isBetween(1_000_000L, 2_000_000L)
  }

  @Test
  fun earlierOfTimeoutAndDeadlineIsSentInOneHeader() {
    respond = { grpcResponse(Feature.ADAPTER.encode(Feature(name = "tree"))) }
    val grpcCall = routeGuideService.GetFeature()
    grpcCall.timeout.timeout(10, TimeUnit.SECONDS)
    grpcCall.timeout.deadline(2, TimeUnit.SECONDS)

    grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))

    assertThat(mockWebServer.takeRequest().grpcTimeoutMicros()).isBetween(1_000_000L, 2_000_000L)
  }

  @Test
  fun streamingCallTimeoutIsEnforcedOnClient() {
    val grpcCall = routeGuideService.RouteChat()
    grpcCall.timeout.timeout(500, TimeUnit.MILLISECONDS)

    val (requestSink, responseSource) = grpcCall.executeBlocking()
    val elapsedMillis = elapsedMillis {
      assertFailure {
        responseSource.read()
      }.isDeadlineExceeded()
    }

    assertThat(grpcCall.isCanceled()).isTrue()
    assertThat(elapsedMillis).isBetween(400L, 5_000L)

    requestSink.closeQuietly()
  }

  @Test
  fun streamingCallDeadlineIsEnforcedOnClient() {
    val grpcCall = routeGuideService.RouteChat()
    grpcCall.timeout.deadline(500, TimeUnit.MILLISECONDS)

    val (requestSink, responseSource) = grpcCall.executeBlocking()
    val elapsedMillis = elapsedMillis {
      assertFailure {
        responseSource.read()
      }.isDeadlineExceeded()
    }

    assertThat(grpcCall.isCanceled()).isTrue()
    assertThat(elapsedMillis).isBetween(400L, 5_000L)

    requestSink.closeQuietly()
  }

  @Test
  fun streamingCallTimeoutIsEnforcedOnClientWhenSuspending() {
    val grpcCall = routeGuideService.RouteChat()
    grpcCall.timeout.timeout(500, TimeUnit.MILLISECONDS)

    val elapsedMillis = elapsedMillis {
      runBlocking {
        val (requestChannel, responseChannel) = grpcCall.executeIn(this)
        assertFailure {
          responseChannel.receive()
        }.isDeadlineExceeded()
        requestChannel.close()
      }
    }

    assertThat(grpcCall.isCanceled()).isTrue()
    assertThat(elapsedMillis).isBetween(400L, 5_000L)
  }

  @Test
  fun streamingCallTimeoutIsEnforcedWhenServerStallsMidStream() {
    // The server sends the headers and one message, then stalls for 4 seconds.
    val firstNote = grpcFrame(RouteNote.ADAPTER.encode(RouteNote(message = "first")))
    val secondNote = grpcFrame(RouteNote.ADAPTER.encode(RouteNote(message = "second")))
    respond = {
      grpcResponse()
        .setBody(Buffer().write(firstNote).write(secondNote))
        .throttleBody(firstNote.size.toLong(), 4, TimeUnit.SECONDS)
    }
    val grpcCall = routeGuideService.RouteChat()
    grpcCall.timeout.timeout(500, TimeUnit.MILLISECONDS)

    val elapsedMillis = elapsedMillis {
      val (requestSink, responseSource) = grpcCall.executeBlocking()
      // MockWebServer reads the whole request before it responds.
      requestSink.close()
      assertThat(responseSource.read()).isEqualTo(RouteNote(message = "first"))
      assertFailure {
        responseSource.read()
      }.isDeadlineExceeded()
    }

    assertThat(grpcCall.isCanceled()).isTrue()
    assertThat(elapsedMillis).isBetween(400L, 3_000L)
  }

  private fun Assert<Throwable>.isDeadlineExceeded() {
    isInstanceOf<GrpcException>()
      .prop(GrpcException::grpcStatus)
      .isEqualTo(GrpcStatus.DEADLINE_EXCEEDED)
  }

  /** Returns the only `grpc-timeout` header of this request in microseconds. */
  private fun RecordedRequest.grpcTimeoutMicros(): Long {
    val values = headers.values("grpc-timeout")
    assertThat(values).hasSize(1)
    assertThat(values.single()).matches(Regex("[0-9]+u"))
    return values.single().removeSuffix("u").toLong()
  }

  /** Returns a successful gRPC response with [messages] as its body. */
  private fun grpcResponse(vararg messages: ByteArray): MockResponse {
    val body = Buffer()
    for (message in messages) body.write(grpcFrame(message))
    return MockResponse()
      .setHeader("Content-Type", "application/grpc")
      .setHeader("grpc-encoding", "identity")
      .setTrailers(headersOf("grpc-status", "0"))
      .setBody(body)
  }

  /** Returns [message] in an uncompressed gRPC length-prefixed frame. */
  private fun grpcFrame(message: ByteArray): ByteArray = Buffer()
    .writeByte(0)
    .writeInt(message.size)
    .write(message)
    .readByteArray()

  private fun MessageSink<*>.closeQuietly() {
    try {
      close()
    } catch (_: IOException) {
      // Closing the request stream of a canceled call may fail. That is fine here.
    }
  }

  private inline fun elapsedMillis(block: () -> Unit): Long {
    val startNanos = System.nanoTime()
    block()
    return (System.nanoTime() - startNanos) / 1_000_000L
  }
}
