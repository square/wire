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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isLessThan
import assertk.assertions.isTrue
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import okio.IOException
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import routeguide.Point
import routeguide.RouteGuideClient

/**
 * Proves that a timeout or a deadline set on a call's [GrpcCall.timeout] is enforced on the
 * client. The server in these tests receives each request and never responds. It ignores the
 * `grpc-timeout` request header, like any server or proxy that does not implement gRPC deadlines.
 * Only client-side enforcement can bound these calls.
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

  @Before
  fun setUp() {
    mockWebServer.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        dispatcherRelease.await(1, TimeUnit.MINUTES)
        return MockResponse().apply { socketPolicy = SocketPolicy.NO_RESPONSE }
      }
    }
    mockWebServer.protocols = listOf(Protocol.H2_PRIOR_KNOWLEDGE)

    val okhttpClient = OkHttpClient.Builder()
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
      }.isInstanceOf<IOException>()
    }

    assertThat(grpcCall.isCanceled()).isTrue()
    assertThat(elapsedMillis).isLessThan(5_000L)
  }

  @Test
  fun unaryCallDeadlineIsEnforcedOnClient() {
    val grpcCall = routeGuideService.GetFeature()
    grpcCall.timeout.deadline(500, TimeUnit.MILLISECONDS)

    val elapsedMillis = elapsedMillis {
      assertFailure {
        grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))
      }.isInstanceOf<IOException>()
    }

    assertThat(grpcCall.isCanceled()).isTrue()
    assertThat(elapsedMillis).isLessThan(5_000L)
  }

  @Test
  fun streamingCallTimeoutIsEnforcedOnClient() {
    val grpcCall = routeGuideService.RouteChat()
    grpcCall.timeout.timeout(500, TimeUnit.MILLISECONDS)

    val (requestSink, responseSource) = grpcCall.executeBlocking()
    val elapsedMillis = elapsedMillis {
      assertFailure {
        responseSource.read()
      }.isInstanceOf<IOException>()
    }

    assertThat(grpcCall.isCanceled()).isTrue()
    assertThat(elapsedMillis).isLessThan(5_000L)

    try {
      requestSink.close()
    } catch (_: IOException) {
      // Closing the request stream of a canceled call may fail. That is fine here.
    }
  }

  @Test
  fun streamingCallDeadlineIsEnforcedOnClient() {
    val grpcCall = routeGuideService.RouteChat()
    grpcCall.timeout.deadline(500, TimeUnit.MILLISECONDS)

    val (requestSink, responseSource) = grpcCall.executeBlocking()
    val elapsedMillis = elapsedMillis {
      assertFailure {
        responseSource.read()
      }.isInstanceOf<IOException>()
    }

    assertThat(grpcCall.isCanceled()).isTrue()
    assertThat(elapsedMillis).isLessThan(5_000L)

    try {
      requestSink.close()
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
