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
@file:Suppress("UsePropertyAccessSyntax")

package com.squareup.wire

import assertk.all
import assertk.assertThat
import assertk.assertions.cause
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isIn
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNotZero
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.message
import assertk.assertions.messageContains
import assertk.assertions.startsWith
import com.google.common.util.concurrent.SettableFuture
import com.squareup.wire.MockRouteGuideService.Action.Delay
import com.squareup.wire.MockRouteGuideService.Action.ReceiveCall
import com.squareup.wire.MockRouteGuideService.Action.ReceiveComplete
import com.squareup.wire.MockRouteGuideService.Action.ReceiveError
import com.squareup.wire.MockRouteGuideService.Action.SendCompleted
import com.squareup.wire.MockRouteGuideService.Action.SendMessage
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol.H2_PRIOR_KNOWLEDGE
import okhttp3.Protocol.HTTP_1_1
import okhttp3.Protocol.HTTP_2
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.ByteString
import okio.ForwardingSource
import okio.IOException
import okio.buffer
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import routeguide.Feature
import routeguide.Point
import routeguide.Rectangle
import routeguide.RouteGuideClient
import routeguide.RouteGuideProto
import routeguide.RouteNote
import routeguide.RouteSummary

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class GrpcClientTest {
  @JvmField @Rule
  val mockService = MockRouteGuideService()

  @JvmField @Rule
  val timeout = Timeout(30, TimeUnit.SECONDS)

  private lateinit var okhttpClient: OkHttpClient
  private lateinit var grpcClient: GrpcClient
  private lateinit var routeGuideService: RouteGuideClient
  private lateinit var incompatibleRouteGuideService: IncompatibleRouteGuideClient
  private var callReference = AtomicReference<Call>()

  // Just set to a large enough value we won't timeout in tests
  private val okHttpClientTimeout = Duration.ofSeconds(117)

  /** This is a pass through interceptor that tests can replace without extra plumbing. */
  private var interceptor: Interceptor = object : Interceptor {
    override fun intercept(chain: Chain) = chain.proceed(chain.request())
  }

  private var networkInterceptor: Interceptor = object : Interceptor {
    override fun intercept(chain: Chain) = chain.proceed(chain.request())
  }

  @Before
  fun setUp() {
    okhttpClient = OkHttpClient.Builder()
      .addInterceptor { chain ->
        callReference.set(chain.call())
        interceptor.intercept(chain)
      }
      .addNetworkInterceptor { chain ->
        networkInterceptor.intercept(chain)
      }
      .protocols(listOf(H2_PRIOR_KNOWLEDGE))
      .callTimeout(okHttpClientTimeout)
      .build()
    grpcClient = GrpcClient.Builder()
      .client(okhttpClient)
      .baseUrl(mockService.url)
      .build()
    routeGuideService = grpcClient.create(RouteGuideClient::class)
    incompatibleRouteGuideService = IncompatibleRouteGuideClient(grpcClient)
  }

  @After
  fun tearDown() {
    okhttpClient.dispatcher.executorService.shutdown()
  }

  @Test
  fun grpcClientBuilderThrowsIfHttp2ProtocolMissing() {
    val okHttpClient = OkHttpClient.Builder()
      .protocols(listOf(HTTP_1_1))
      .build()
    val exception = assertThrows(IllegalArgumentException::class.java) {
      GrpcClient.Builder().client(okHttpClient)
    }
    assertThat(exception.message)
      .isEqualTo(
        "OkHttpClient is not configured with a HTTP/2 protocol which is required for gRPC connections.",
      )
  }

  @Test
  fun grpcClientBuilderDoesNotThrowIfHttp2ProtocolIsSet() {
    val okHttpClient = OkHttpClient.Builder()
      .protocols(listOf(HTTP_1_1, HTTP_2))
      .build()
    GrpcClient.Builder()
      .client(okHttpClient)
      .baseUrl("https://square.github.io/wire/")
      .build()
  }

  @Test
  fun grpcClientBuilderDoesNotThrowIfH2PriorKnowledgeProtocolIsSet() {
    val okHttpClient = OkHttpClient.Builder()
      .protocols(listOf(H2_PRIOR_KNOWLEDGE))
      .build()
    GrpcClient.Builder()
      .client(okHttpClient)
      .baseUrl("https://square.github.io/wire/")
      .build()
  }

  @Suppress("ReplaceCallWithBinaryOperator") // We are explicitly testing this behavior.
  @Test
  fun objectMethodsStillWork() {
    assertThat(routeGuideService.hashCode()).isNotZero()
    assertThat(routeGuideService.equals(this)).isFalse()
    assertThat(routeGuideService.toString()).isNotEmpty()
  }

  @Test
  fun invalidBaseUrlThrows() {
    val builder = GrpcClient.Builder()
    try {
      builder.baseUrl("mailto:bob@example.com")
      fail()
    } catch (_: IllegalArgumentException) {
    }
  }

  @Test
  fun requestResponseSuspend() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree at 5,6")
    mockService.enqueue(SendCompleted)

    runBlocking {
      val grpcCall = routeGuideService.GetFeature()
      val feature = grpcCall.execute(Point(latitude = 5, longitude = 6))

      assertThat(feature).isEqualTo(Feature(name = "tree at 5,6"))
    }
  }

  @Test
  fun requestResponseBlocking() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree at 5,6")
    mockService.enqueue(SendCompleted)

    val grpcCall = routeGuideService.GetFeature()
    val feature = grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))

    assertThat(feature).isEqualTo(Feature(name = "tree at 5,6"))
  }

  @Test
  fun requestResponseCallback() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree at 5,6")
    mockService.enqueue(SendCompleted)

    val grpcCall = routeGuideService.GetFeature()
    var feature: Feature? = null
    val latch = CountDownLatch(1)
    grpcCall.enqueue(
      Point(latitude = 5, longitude = 6),
      object : GrpcCall.Callback<Point, Feature> {
        override fun onFailure(call: GrpcCall<Point, Feature>, exception: IOException) {
          throw AssertionError()
        }

        override fun onSuccess(call: GrpcCall<Point, Feature>, response: Feature) {
          feature = response
          latch.countDown()
        }
      },
    )

    mockService.awaitSuccessBlocking()
    latch.await()
    assertThat(feature).isEqualTo(Feature(name = "tree at 5,6"))
  }

  @Test @Ignore
  fun cancelRequestResponseSuspending() {
    TODO()
  }

  @Test @Ignore
  fun cancelRequestResponseBlocking() {
    TODO()
  }

  @Test @Ignore
  fun cancelRequestResponseCallback() {
    TODO()
  }

  @Test
  fun streamingRequestSuspend() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RecordRoute"))
    mockService.enqueueReceivePoint(latitude = 3, longitude = 3)
    mockService.enqueueReceivePoint(latitude = 9, longitude = 6)
    mockService.enqueueSendSummary(pointCount = 2)
    mockService.enqueue(SendCompleted)
    mockService.enqueue(ReceiveComplete)

    val (requestChannel, responseChannel) = routeGuideService.RecordRoute().execute()
    runBlocking {
      requestChannel.send(Point(3, 3))
      requestChannel.send(Point(9, 6))
      requestChannel.close()
      assertThat(responseChannel.receive()).isEqualTo(RouteSummary(point_count = 2))
    }
  }

  @Test
  fun streamingRequestBlocking() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RecordRoute"))
    mockService.enqueueReceivePoint(latitude = 3, longitude = 3)
    mockService.enqueueReceivePoint(latitude = 9, longitude = 6)
    mockService.enqueueSendSummary(pointCount = 2)
    mockService.enqueue(SendCompleted)
    mockService.enqueue(ReceiveComplete)

    val (requestChannel, deferredResponse) = routeGuideService.RecordRoute().executeBlocking()
    requestChannel.write(Point(3, 3))
    requestChannel.write(Point(9, 6))
    requestChannel.close()
    assertThat(deferredResponse.read()).isEqualTo(RouteSummary(point_count = 2))
  }

  /**
   * This is a regression test for a crash that occurred when OkHttp attempted to retry sending the
   * request body of a gRPC streaming connection. This failed because our `PipeDuplexRequestBody`
   * is a one shot request body, but didn't override `RequestBody.isOneShot()`.
   *
   * `java.lang.IllegalStateException: sink already folded`
   */
  @Test
  fun streamingConnectionFailsAfterReusedConnection() {
    // Make a basic request/response call to seed the connection pool.
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree at 5,6")
    mockService.enqueue(SendCompleted)
    val grpcCall = routeGuideService.GetFeature()
    val feature = grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))
    assertThat(feature).isEqualTo(Feature(name = "tree at 5,6"))

    // Give the mock server invalid actions (SendCompleted with no response message), which will
    // cause it to reset the socket connection.
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RecordRoute"))
    mockService.enqueue(SendCompleted)

    // Confirm we get an IOException from the broken connection and not an IllegalStateException
    // from incorrectly attempting to recover.
    val (requestChannel, deferredResponse) = routeGuideService.RecordRoute().executeBlocking()
    val e = assertFailsWith<IOException> {
      requestChannel.close()
      deferredResponse.read()
    }
    assertThat(e).hasMessage("stream was reset: CANCEL")
    assertThat(e.cause).isNull()
  }

  @Test
  fun cancelStreamingRequestSuspend() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RecordRoute"))

    val (_, responseChannel) = routeGuideService.RecordRoute().execute()
    runBlocking {
      // TODO(benoit) Fix it so we don't have to wait.
      // We wait for the request to proceed.
      delay(200)
      responseChannel.cancel()
      mockService.awaitSuccess()
      assertThat(callReference.get()!!.isCanceled()).isTrue()
    }
  }

  @Test
  fun cancelStreamingRequestBlocking() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RecordRoute"))

    val (_, responseDeferred) = routeGuideService.RecordRoute().executeBlocking()
    // We wait for the request to proceed.
    Thread.sleep(200)
    responseDeferred.close()
    mockService.awaitSuccessBlocking()
    assertThat(callReference.get()!!.isCanceled()).isTrue()
  }

  @Test
  fun streamingResponseSuspend() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/ListFeatures"))
    mockService.enqueueReceiveRectangle(lo = Point(0, 0), hi = Point(4, 5))
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree")
    mockService.enqueueSendFeature(name = "house")
    mockService.enqueue(SendCompleted)

    val (requestChannel, responseChannel) = routeGuideService.ListFeatures().execute()
    runBlocking {
      requestChannel.send(Rectangle(lo = Point(0, 0), hi = Point(4, 5)))
      requestChannel.close()
      assertThat(responseChannel.receive()).isEqualTo(Feature(name = "tree"))
      assertThat(responseChannel.receive()).isEqualTo(Feature(name = "house"))
      assertThat(responseChannel.receiveCatching().getOrNull()).isNull()
    }
  }

  @Test
  fun streamingResponseBlocking() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/ListFeatures"))
    mockService.enqueueReceiveRectangle(lo = Point(0, 0), hi = Point(4, 5))
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree")
    mockService.enqueueSendFeature(name = "house")
    mockService.enqueue(SendCompleted)

    val (requestChannel, responseChannel) = routeGuideService.ListFeatures().executeBlocking()
    requestChannel.write(Rectangle(lo = Point(0, 0), hi = Point(4, 5)))
    requestChannel.close()
    assertThat(responseChannel.read()).isEqualTo(Feature(name = "tree"))
    assertThat(responseChannel.read()).isEqualTo(Feature(name = "house"))
    assertThat(responseChannel.read()).isNull()
  }

  @Test
  fun cancelStreamingResponseSuspend() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/ListFeatures"))
    mockService.enqueueReceiveRectangle(lo = Point(0, 0), hi = Point(4, 5))
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree")
    mockService.enqueue(Delay(500, TimeUnit.MILLISECONDS))
    mockService.enqueueSendFeature(name = "house")
    mockService.enqueue(SendCompleted)

    val (requestChannel, responseChannel) = routeGuideService.ListFeatures().execute()
    runBlocking {
      requestChannel.send(Rectangle(lo = Point(0, 0), hi = Point(4, 5)))
      requestChannel.close()
      assertThat(responseChannel.receive()).isEqualTo(Feature(name = "tree"))
      responseChannel.cancel()
      mockService.awaitSuccess()
      assertThat(callReference.get()!!.isCanceled()).isTrue()
    }
  }

  @Test
  fun cancelStreamingResponseBlocking() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/ListFeatures"))
    mockService.enqueueReceiveRectangle(lo = Point(0, 0), hi = Point(4, 5))
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree")
    mockService.enqueue(Delay(500, TimeUnit.MILLISECONDS))
    mockService.enqueueSendFeature(name = "house")
    mockService.enqueue(SendCompleted)

    val (requestChannel, responseChannel) = routeGuideService.ListFeatures().executeBlocking()
    requestChannel.write(Rectangle(lo = Point(0, 0), hi = Point(4, 5)))
    requestChannel.close()
    assertThat(responseChannel.read()).isEqualTo(Feature(name = "tree"))
    responseChannel.close()
    mockService.awaitSuccessBlocking()
    assertThat(callReference.get()!!.isCanceled()).isTrue()
  }

  @Test
  fun duplexSuspend_receiveDataAfterClosingRequest() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueReceiveNote(message = "marco")
    mockService.enqueueSendNote(message = "polo")
    mockService.enqueueReceiveNote(message = "rené")
    mockService.enqueueSendNote(message = "lacoste")
    mockService.enqueue(ReceiveComplete)
    mockService.enqueue(SendCompleted)

    val (requestChannel, responseChannel) = routeGuideService.RouteChat().execute()
    runBlocking {
      requestChannel.send(RouteNote(message = "marco"))
      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "polo"))
      requestChannel.send(RouteNote(message = "rené"))
      requestChannel.close()
      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "lacoste"))
      assertThat(responseChannel.receiveCatching().getOrNull()).isNull()
    }
  }

  @Test
  fun duplexSuspend_requestChannelThrowsWhenResponseChannelExhausted() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueReceiveNote(message = "marco")
    mockService.enqueue(SendCompleted)

    runBlocking {
      val (requestChannel, _) = routeGuideService.RouteChat().executeIn(this)

      val latch = Mutex(locked = true)
      requestChannel.invokeOnClose { expected ->
        assertThat(expected!!).isInstanceOf<CancellationException>()
        latch.unlock()
      }

      requestChannel.send(RouteNote(message = "marco"))

      latch.withLock {
        // Wait for requestChannel to be closed.
      }

      try {
        requestChannel.send(RouteNote(message = "léo"))
        fail()
      } catch (expected: Throwable) {
        assertThat(expected).isInstanceOf<CancellationException>()
      }
    }
  }

  @Test
  fun duplexSuspend_requestChannelThrowsWhenWhenCanceledByServer() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueReceiveNote(message = "marco")
    mockService.enqueueSendError(StatusException(Status.FAILED_PRECONDITION))

    runBlocking {
      val (requestChannel, _) = routeGuideService.RouteChat().executeIn(this)

      val latch = Mutex(locked = true)
      requestChannel.invokeOnClose { expected ->
        assertThat(expected!!).isInstanceOf<CancellationException>()
        latch.unlock()
      }

      requestChannel.send(RouteNote(message = "marco"))

      latch.withLock {
        // Wait for requestChannel to be closed.
      }

      try {
        requestChannel.send(RouteNote(message = "léo"))
        fail()
      } catch (expected: Throwable) {
        assertThat(expected).isInstanceOf<CancellationException>()
      }
    }
  }

  @Test
  fun duplexSuspend_requestChannelThrowsWhenCallCanceledByClient() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueSendNote(message = "marco")
    mockService.enqueueReceiveNote(message = "léo")

    runBlocking {
      val (requestChannel, responseChannel) = routeGuideService.RouteChat().executeIn(this)

      val latch = Mutex(locked = true)
      requestChannel.invokeOnClose { expected ->
        assertThat(expected!!).isInstanceOf<CancellationException>()
        latch.unlock()
      }

      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "marco"))
      requestChannel.send(RouteNote(message = "léo"))

      callReference.get().cancel()

      latch.withLock {
        // Wait for requestChannel to be closed.
      }

      try {
        requestChannel.send(RouteNote(message = "rené"))
        fail()
      } catch (expected: Throwable) {
        assertThat(expected).isInstanceOf<CancellationException>()
      }
    }
  }

  // Note that this test is asserting that we can actually catch the exception within our try/catch.
  @Test
  fun duplexSuspend_responseChannelThrowsWhenCanceledByServer() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueSendNote(message = "marco")
    mockService.enqueueSendError(StatusException(Status.FAILED_PRECONDITION))

    runBlocking {
      val (_, responseChannel) = routeGuideService.RouteChat().executeIn(this)

      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "marco"))

      try {
        responseChannel.receive()
        fail()
      } catch (expected: GrpcException) {
        assertThat(expected.grpcStatus).isEqualTo(GrpcStatus.FAILED_PRECONDITION)
      }

      assertThat(responseChannel.isClosedForReceive).isTrue()
    }
  }

  /**
   * Force an IOException in the gRPC response body stream, and confirm that Wire fails with the
   * right exception (IOException). We've had bugs where we incorrectly attempt to read trailers
   * while handling an exception.
   */
  @Test
  fun duplexSuspend_responseBodyThrows() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueSendNote(message = "marco")

    // This interceptor throws an exception at the start of the stream instead of returning -1L.
    networkInterceptor = object : Interceptor {
      override fun intercept(chain: Chain): Response {
        val response = chain.proceed(chain.request())
        return response.newBuilder()
          .body(object : ResponseBody() {
            override fun contentLength() = response.body.contentLength()
            override fun contentType() = response.body.contentType()
            override fun source() = object : ForwardingSource(response.body.source()) {
              override fun read(sink: Buffer, byteCount: Long) = throw IOException("boom!")
            }.buffer()
          })
          .build()
      }
    }

    runBlocking {
      val (_, responseChannel) = routeGuideService.RouteChat().executeIn(this)

      val expected = assertFailsWith<IOException> {
        responseChannel.receive()
      }
      assertThat(expected).all {
        hasMessage("gRPC transport failure (HTTP status=200, grpc-status=null, grpc-message=null)")
        // Synthetic exception added by coroutines in StackTraceRecovery.kt.
        cause()
          .isNotNull()
          .cause()
          .isNotNull()
          .hasMessage("boom!")
      }

      assertThat(responseChannel.isClosedForReceive).isTrue()
    }
  }

  @Test
  fun duplexSuspend_responseChannelThrowsWhenCallCanceledByClient() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueSendNote(message = "marco")

    runBlocking {
      val (_, responseChannel) = routeGuideService.RouteChat().executeIn(this)

      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "marco"))

      callReference.get().cancel()

      try {
        responseChannel.receive()
        fail()
      } catch (expected: IOException) {
        assertThat(expected.message!!).startsWith("gRPC transport failure")
      }

      assertThat(responseChannel.isClosedForReceive).isTrue()
    }
  }

  @Test
  fun duplexBlocking_receiveDataAfterClosingRequest() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueReceiveNote(message = "marco")
    mockService.enqueueSendNote(message = "polo")
    mockService.enqueueReceiveNote(message = "rené")
    mockService.enqueueSendNote(message = "lacoste")
    mockService.enqueue(ReceiveComplete)
    mockService.enqueue(SendCompleted)

    val (requestSink, responseSource) = routeGuideService.RouteChat().executeBlocking()
    requestSink.write(RouteNote(message = "marco"))
    assertThat(responseSource.read()).isEqualTo(RouteNote(message = "polo"))
    requestSink.write(RouteNote(message = "rené"))
    requestSink.close()
    assertThat(responseSource.read()).isEqualTo(RouteNote(message = "lacoste"))
    assertThat(responseSource.read()).isNull()
  }

  @Test
  fun duplexSuspend_receiveDataBeforeClosingRequest() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueReceiveNote(message = "marco")
    mockService.enqueueSendNote(message = "polo")
    mockService.enqueueReceiveNote(message = "rené")
    mockService.enqueueSendNote(message = "lacoste")
    // We give time to the sender to read the response.
    mockService.enqueue(Delay(500, TimeUnit.MILLISECONDS))
    mockService.enqueue(SendCompleted)
    mockService.enqueue(ReceiveComplete)

    val (requestChannel, responseChannel) = routeGuideService.RouteChat().execute()
    runBlocking {
      requestChannel.send(RouteNote(message = "marco"))
      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "polo"))
      requestChannel.send(RouteNote(message = "rené"))
      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "lacoste"))
      assertThat(responseChannel.receiveCatching().getOrNull()).isNull()
      requestChannel.close()
    }
  }

  @Test
  fun duplexBlocking_receiveDataBeforeClosingRequest() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueReceiveNote(message = "marco")
    mockService.enqueueSendNote(message = "polo")
    mockService.enqueueReceiveNote(message = "rené")
    mockService.enqueueSendNote(message = "lacoste")
    // We give time to the sender to read the response.
    mockService.enqueue(Delay(500, TimeUnit.MILLISECONDS))
    mockService.enqueue(SendCompleted)
    mockService.enqueue(ReceiveComplete)

    val (requestChannel, responseChannel) = routeGuideService.RouteChat().executeBlocking()
    requestChannel.write(RouteNote(message = "marco"))
    assertThat(responseChannel.read()).isEqualTo(RouteNote(message = "polo"))
    requestChannel.write(RouteNote(message = "rené"))
    assertThat(responseChannel.read()).isEqualTo(RouteNote(message = "lacoste"))
    assertThat(responseChannel.read()).isNull()
    requestChannel.close()
  }

  @Test
  fun cancelDuplexSuspendBeforeRequestCompletes() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))

    val (requestChannel, responseChannel) = routeGuideService.RouteChat().execute()
    runBlocking {
      // We wait for mockService to process our actions.
      delay(500)
      responseChannel.cancel()
      assertThat((requestChannel as Channel).isClosedForReceive).isTrue()
      assertThat(requestChannel.isClosedForSend).isTrue()
      mockService.awaitSuccess()
      assertThat(callReference.get()!!.isCanceled()).isTrue()
    }
  }

  @Test
  fun cancelDuplexSuspendAfterRequestCompletes() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueReceiveNote(message = "marco")
    mockService.enqueue(ReceiveComplete)

    val (requestChannel, responseChannel) = routeGuideService.RouteChat().execute()
    runBlocking {
      requestChannel.send(RouteNote(message = "marco"))
      requestChannel.close()
      // We wait for mockService to process our actions.
      delay(500)

      responseChannel.cancel()
      assertThat((requestChannel as Channel).isClosedForReceive).isTrue()
      assertThat(requestChannel.isClosedForSend).isTrue()
      mockService.awaitSuccess()
      assertThat(callReference.get()!!.isCanceled()).isTrue()
    }
  }

  @Test
  fun cancelDuplexSuspendBeforeResponseCompletes() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueReceiveNote(message = "marco")
    mockService.enqueueSendNote(message = "polo")
    mockService.enqueueReceiveNote(message = "rené")
    mockService.enqueue(ReceiveComplete)

    val (requestChannel, responseChannel) = routeGuideService.RouteChat().execute()
    runBlocking {
      requestChannel.send(RouteNote(message = "marco"))
      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "polo"))
      requestChannel.send(RouteNote(message = "rené"))
      requestChannel.close()

      // We wait for mockService to process our actions.
      delay(500)

      responseChannel.cancel()
      assertThat((requestChannel as Channel).isClosedForReceive).isTrue()
      assertThat(requestChannel.isClosedForSend).isTrue()
      mockService.awaitSuccess()
      assertThat(callReference.get()!!.isCanceled()).isTrue()
    }
  }

  @Test
  fun cancelDuplexSuspendAfterResponseCompletes() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueReceiveNote(message = "marco")
    mockService.enqueueSendNote(message = "polo")
    mockService.enqueueReceiveNote(message = "rené")
    mockService.enqueueSendNote(message = "lacoste")
    mockService.enqueue(ReceiveComplete)
    mockService.enqueue(SendCompleted)

    val (requestChannel, responseChannel) = routeGuideService.RouteChat().execute()
    runBlocking {
      requestChannel.send(RouteNote(message = "marco"))
      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "polo"))
      requestChannel.send(RouteNote(message = "rené"))
      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "lacoste"))
      requestChannel.close()
      assertThat(responseChannel.receiveCatching().getOrNull()).isNull()

      // We wait for mockService to process our actions.
      delay(500)

      responseChannel.cancel()
      assertThat((requestChannel as Channel).isClosedForReceive).isTrue()
      assertThat(requestChannel.isClosedForSend).isTrue()
      assertThat(callReference.get()!!.isCanceled()).isTrue()
    }
  }

  @Test fun cancelDuplexBlockingBeforeRequestCompletes() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))

    val (_, responseChannel) = routeGuideService.RouteChat().executeBlocking()
    // We wait for mockService to process our actions.
    Thread.sleep(500)
    responseChannel.close()
    mockService.awaitSuccessBlocking()
    assertThat(callReference.get()!!.isCanceled()).isTrue()
  }

  @Test fun cancelDuplexBlockingAfterRequestCompletes() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueReceiveNote(message = "marco")
    mockService.enqueue(ReceiveComplete)

    val (requestChannel, responseChannel) = routeGuideService.RouteChat().executeBlocking()
    requestChannel.write(RouteNote(message = "marco"))
    requestChannel.close()
    // We wait for mockService to process our actions.
    Thread.sleep(500)

    responseChannel.close()
    mockService.awaitSuccessBlocking()
    assertThat(callReference.get()!!.isCanceled()).isTrue()
  }

  @Test fun cancelDuplexBlockingBeforeResponseCompletes() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueReceiveNote(message = "marco")
    mockService.enqueueSendNote(message = "polo")
    mockService.enqueueReceiveNote(message = "rené")
    mockService.enqueue(ReceiveComplete)

    val (requestChannel, responseChannel) = routeGuideService.RouteChat().executeBlocking()
    requestChannel.write(RouteNote(message = "marco"))
    assertThat(responseChannel.read()).isEqualTo(RouteNote(message = "polo"))
    requestChannel.write(RouteNote(message = "rené"))
    requestChannel.close()

    // We wait for mockService to process our actions.
    Thread.sleep(500)

    responseChannel.close()
    mockService.awaitSuccessBlocking()
    assertThat(callReference.get()!!.isCanceled()).isTrue()
  }

  @Test fun cancelDuplexBlockingAfterResponseCompletes() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueReceiveNote(message = "marco")
    mockService.enqueueSendNote(message = "polo")
    mockService.enqueueReceiveNote(message = "rené")
    mockService.enqueueSendNote(message = "lacoste")
    mockService.enqueue(ReceiveComplete)
    mockService.enqueue(SendCompleted)

    val (requestChannel, responseChannel) = routeGuideService.RouteChat().executeBlocking()
    requestChannel.write(RouteNote(message = "marco"))
    assertThat(responseChannel.read()).isEqualTo(RouteNote(message = "polo"))
    requestChannel.write(RouteNote(message = "rené"))
    assertThat(responseChannel.read()).isEqualTo(RouteNote(message = "lacoste"))
    requestChannel.close()
    assertThat(responseChannel.read()).isNull()

    // We wait for mockService to process our actions.
    Thread.sleep(500)

    responseChannel.close()
    assertThat(callReference.get()!!.isCanceled()).isTrue()
  }

  @Test
  fun duplexSuspendReceiveOnly() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueSendNote(message = "welcome")
    mockService.enqueue(SendCompleted)
    mockService.enqueue(ReceiveComplete)

    val (requestChannel, responseChannel) = routeGuideService.RouteChat().execute()
    runBlocking {
      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "welcome"))
      requestChannel.close()
      assertThat(responseChannel.receiveCatching().getOrNull()).isNull()
    }
  }

  @Test
  fun duplexBlockingReceiveOnly() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueSendNote(message = "welcome")
    mockService.enqueueSendNote(message = "polo")
    mockService.enqueue(SendCompleted)
    mockService.enqueue(ReceiveComplete)

    val (requestChannel, responseChannel) = routeGuideService.RouteChat().executeBlocking()
    assertThat(responseChannel.read()).isEqualTo(RouteNote(message = "welcome"))
    assertThat(responseChannel.read()).isEqualTo(RouteNote(message = "polo"))
    requestChannel.close()
    assertThat(responseChannel.read()).isNull()
  }

  /**
   * This test is flaky. The root cause is OkHttp may send the cancel frame after the EOF frame,
   * which is incorrect. https://github.com/square/okhttp/issues/5388
   */
  @Test
  fun cancelOutboundStream() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueSendNote(message = "welcome")
    mockService.enqueue(ReceiveError)

    val (requestChannel, responseChannel) = routeGuideService.RouteChat().execute()
    runBlocking {
      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "welcome"))
      requestChannel.close(IOException("boom!"))
      mockService.awaitSuccess()
    }
  }

  @Test
  fun grpcCallIsCanceledWhenItShouldBe() {
    val grpcCall = routeGuideService.GetFeature()
    assertThat(grpcCall.isCanceled()).isFalse()

    grpcCall.cancel()
    assertThat(grpcCall.isCanceled()).isTrue()
  }

  @Test
  fun grpcStreamingCallIsCanceledWhenItShouldBe() {
    val grpcStreamingCall = routeGuideService.RouteChat()
    assertThat(grpcStreamingCall.isCanceled()).isFalse()

    grpcStreamingCall.cancel()
    assertThat(grpcStreamingCall.isCanceled()).isTrue()
  }

  @Test
  fun grpcCallIsExecutedAfterExecute() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree at 5,6")
    mockService.enqueue(SendCompleted)

    runBlocking {
      val grpcCall = routeGuideService.GetFeature()
      assertThat(grpcCall.isExecuted()).isFalse()

      val feature = grpcCall.execute(Point(latitude = 5, longitude = 6))
      assertThat(grpcCall.isExecuted()).isTrue()
      assertThat(feature).isEqualTo(Feature(name = "tree at 5,6"))
    }
  }

  @Test
  fun grpcCallIsExecutedAfterExecuteBlocking() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree at 5,6")
    mockService.enqueue(SendCompleted)

    val grpcCall = routeGuideService.GetFeature()
    assertThat(grpcCall.isExecuted()).isFalse()

    val feature = grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))
    assertThat(grpcCall.isExecuted()).isTrue()

    assertThat(feature).isEqualTo(Feature(name = "tree at 5,6"))
  }

  @Test
  fun grpcCallIsExecutedAfterEnqueue() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree at 5,6")
    mockService.enqueue(SendCompleted)

    val grpcCall = routeGuideService.GetFeature()
    assertThat(grpcCall.isExecuted()).isFalse()

    var feature: Feature? = null
    val latch = CountDownLatch(1)
    grpcCall.enqueue(
      Point(latitude = 5, longitude = 6),
      object : GrpcCall.Callback<Point, Feature> {
        override fun onFailure(call: GrpcCall<Point, Feature>, exception: IOException) {
          throw AssertionError()
        }

        override fun onSuccess(call: GrpcCall<Point, Feature>, response: Feature) {
          feature = response
          latch.countDown()
        }
      },
    )
    assertThat(grpcCall.isExecuted()).isTrue()

    mockService.awaitSuccessBlocking()
    latch.await()
    assertThat(feature).isEqualTo(Feature(name = "tree at 5,6"))
  }

  @Test
  fun grpcStreamingCallIsExecutedAfterExecute() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueSendNote(message = "welcome")
    mockService.enqueue(SendCompleted)
    mockService.enqueue(ReceiveComplete)

    val grpcStreamingCall = routeGuideService.RouteChat()
    assertThat(grpcStreamingCall.isExecuted()).isFalse()
    val (requestChannel, responseChannel) = grpcStreamingCall.execute()
    assertThat(grpcStreamingCall.isExecuted()).isTrue()

    runBlocking {
      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "welcome"))
      requestChannel.close()
      assertThat(responseChannel.receiveCatching().getOrNull()).isNull()
    }
  }

  @Test
  fun grpcStreamingCallIsExecutedAfterExecuteBlocking() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueSendNote(message = "welcome")
    mockService.enqueueSendNote(message = "polo")
    mockService.enqueue(SendCompleted)
    mockService.enqueue(ReceiveComplete)

    val grpcStreamingCall = routeGuideService.RouteChat()
    assertThat(grpcStreamingCall.isExecuted()).isFalse()
    val (requestChannel, responseChannel) = grpcStreamingCall.executeBlocking()
    assertThat(grpcStreamingCall.isExecuted()).isTrue()

    assertThat(responseChannel.read()).isEqualTo(RouteNote(message = "welcome"))
    assertThat(responseChannel.read()).isEqualTo(RouteNote(message = "polo"))
    requestChannel.close()
    assertThat(responseChannel.read()).isNull()
  }

  @Test
  fun requestResponseSuspendServerOmitsGrpcStatus() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree at 5,6")
    mockService.enqueue(SendCompleted)
    interceptor = removeGrpcStatusInterceptor()

    runBlocking {
      val grpcCall = routeGuideService.GetFeature()
      try {
        grpcCall.execute(Point(latitude = 5, longitude = 6))
        fail()
      } catch (expected: IOException) {
        assertThat(expected).hasMessage(
          "gRPC transport failure (HTTP status=200, grpc-status=null, grpc-message=null)",
        )
      }
    }
  }

  @Test
  fun requestResponseBlockingServerOmitsGrpcStatus() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree at 5,6")
    mockService.enqueue(SendCompleted)
    interceptor = removeGrpcStatusInterceptor()

    val grpcCall = routeGuideService.GetFeature()
    try {
      grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))
      fail()
    } catch (expected: IOException) {
      assertThat(expected).hasMessage(
        "gRPC transport failure (HTTP status=200, grpc-status=null, grpc-message=null)",
      )
    }
  }

  @Test
  fun requestResponseCallbackServerOmitsGrpcStatus() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree at 5,6")
    mockService.enqueue(SendCompleted)
    interceptor = removeGrpcStatusInterceptor()

    val grpcCall = routeGuideService.GetFeature()
    val latch = CountDownLatch(1)
    grpcCall.enqueue(
      Point(latitude = 5, longitude = 6),
      object : GrpcCall.Callback<Point, Feature> {
        override fun onFailure(call: GrpcCall<Point, Feature>, exception: IOException) {
          assertThat(exception).hasMessage(
            "gRPC transport failure (HTTP status=200, grpc-status=null, grpc-message=null)",
          )
          latch.countDown()
        }

        override fun onSuccess(call: GrpcCall<Point, Feature>, response: Feature) {
          throw AssertionError()
        }
      },
    )

    mockService.awaitSuccessBlocking()
    latch.await()
  }

  @Test
  fun responseStatusIsNot200() {
    interceptor = object : Interceptor {
      override fun intercept(chain: Chain): Response {
        return Response.Builder()
          .request(chain.request())
          .protocol(HTTP_2)
          .code(500)
          .message("internal server error")
          .body(ByteString.EMPTY.toResponseBody("application/grpc".toMediaType()))
          .build()
      }
    }

    runBlocking {
      val grpcCall = routeGuideService.GetFeature()
      try {
        grpcCall.execute(Point(latitude = 5, longitude = 6))
        fail()
      } catch (expected: IOException) {
        assertThat(expected).hasMessage(
          "expected gRPC but was HTTP status=500, content-type=application/grpc",
        )
      }
    }
  }

  @Test
  fun responseContentTypeIsNotGrpc() {
    interceptor = object : Interceptor {
      override fun intercept(chain: Chain): Response {
        return Response.Builder()
          .request(chain.request())
          .protocol(HTTP_2)
          .code(200)
          .message("ok")
          .body(ByteString.EMPTY.toResponseBody("text/plain".toMediaType()))
          .build()
      }
    }

    runBlocking {
      val grpcCall = routeGuideService.GetFeature()
      try {
        grpcCall.execute(Point(latitude = 5, longitude = 6))
        fail()
      } catch (expected: IOException) {
        assertThat(expected).hasMessage(
          "expected gRPC but was HTTP status=200, content-type=text/plain",
        )
      }
    }
  }

  /** Confirm the response content-type "application/grpc" is accepted. */
  @Test
  fun contentTypeApplicationGrpc() {
    interceptor = object : Interceptor {
      override fun intercept(chain: Chain): Response {
        val response = chain.proceed(chain.request())
        return response.newBuilder()
          .body(object : ResponseBody() {
            override fun contentLength() = response.body!!.contentLength()
            override fun source() = response.body!!.source()
            override fun contentType() = "application/grpc".toMediaType()
          })
          .build()
      }
    }

    requestResponseBlocking()
  }

  /** Confirm the response content-type "application/grpc+proto" is accepted. */
  @Test
  fun contentTypeApplicationGrpcPlusProto() {
    interceptor = object : Interceptor {
      override fun intercept(chain: Chain): Response {
        val response = chain.proceed(chain.request())
        return response.newBuilder()
          .body(object : ResponseBody() {
            override fun contentLength() = response.body!!.contentLength()
            override fun source() = response.body!!.source()
            override fun contentType() = "application/grpc+proto".toMediaType()
          })
          .build()
      }
    }

    requestResponseBlocking()
  }

  @Test
  fun requestEarlyFailure() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendError(Exception("boom"))
    mockService.enqueue(SendCompleted)

    val grpcCall = routeGuideService.GetFeature()
    try {
      grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))
      fail()
    } catch (expected: GrpcException) {
      assertThat(expected.grpcStatus).isEqualTo(GrpcStatus.UNKNOWN)
      assertThat(expected.grpcMessage).isNull()
    }
  }

  @Test
  fun requestEarlyFailureWithDescription() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendError(Status.INTERNAL.withDescription("boom").asRuntimeException())
    mockService.enqueue(SendCompleted)

    val grpcCall = routeGuideService.GetFeature()
    try {
      grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))
      fail()
    } catch (expected: GrpcException) {
      assertThat(expected.grpcStatus).isEqualTo(GrpcStatus.INTERNAL)
      assertThat(expected.grpcMessage).isEqualTo("boom")
    }
  }

  /** Return a value, then send an error. This relies on trailers being read. */
  @Test
  fun requestLateFailureWithDescription() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree at 5,6")
    mockService.enqueueSendError(Status.INTERNAL.withDescription("boom").asRuntimeException())
    mockService.enqueue(SendCompleted)

    val grpcCall = routeGuideService.GetFeature()
    try {
      grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))
      fail()
    } catch (expected: GrpcException) {
      assertThat(expected.grpcStatus).isEqualTo(GrpcStatus.INTERNAL)
      assertThat(expected).messageContains(
        "grpc-status=13 grpc-status-name=INTERNAL grpc-message=boom url=" +
          mockService.url + "routeguide.RouteGuide/GetFeature",
      )
    }
  }

  /** Violate the server's API contract, causing the stream to be canceled. */
  @Test
  fun serverCrashDueToTooManyResponses() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree at 5,6")
    mockService.enqueueSendFeature(name = "tree at 7,8") // Invalid! Will cause a failure response.
    mockService.enqueue(SendCompleted)

    val grpcCall = routeGuideService.GetFeature()
    try {
      grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))
      fail()
    } catch (expected: IOException) {
      assertThat(expected).hasMessage(
        "gRPC transport failure (HTTP status=200, grpc-status=null, grpc-message=null)",
      )
      assertThat(expected.cause!!).hasMessage("stream was reset: CANCEL")
    }
  }

  /** The server is streaming multiple responses, but the client expects 1 response. */
  @Test
  fun serverSendsTooManyResponseMessages() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueReceiveNote(message = "marco")
    mockService.enqueueSendNote(message = "polo")
    mockService.enqueueSendNote(message = "rené")
    mockService.enqueueSendNote(message = "lacoste")
    mockService.enqueue(ReceiveComplete)
    mockService.enqueue(SendCompleted)

    val grpcCall = incompatibleRouteGuideService.RouteChat()
    try {
      grpcCall.executeBlocking(RouteNote(message = "marco"))
      fail()
    } catch (expected: IOException) {
      // It's racy whether we receive trailers first or close the response stream first.
      assertThat(expected.message).isIn(
        "gRPC transport failure (HTTP status=200, grpc-status=0, grpc-message=null)",
        "gRPC transport failure (HTTP status=200, grpc-status=null, grpc-message=null)",
      )
      assertThat(expected.cause!!).hasMessage("expected 1 message but got multiple")
    }
  }

  /** The server is streaming zero responses, but the client expects 1 response. */
  @Test
  fun serverSendsTooFewResponseMessages() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueReceiveNote(message = "marco")
    mockService.enqueue(ReceiveComplete)
    mockService.enqueue(SendCompleted)

    val grpcCall = incompatibleRouteGuideService.RouteChat()
    try {
      grpcCall.executeBlocking(RouteNote(message = "marco"))
      fail()
    } catch (expected: IOException) {
      assertThat(expected).hasMessage(
        "gRPC transport failure (HTTP status=200, grpc-status=0, grpc-message=null)",
      )
      assertThat(expected.cause!!).hasMessage("expected 1 message but got none")
    }
  }

  @Test
  fun grpcMethodTagIsPresent() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree at 5,6")
    mockService.enqueue(SendCompleted)
    interceptor = object : Interceptor {
      override fun intercept(chain: Chain): Response {
        val grpcMethod = chain.request().tag(GrpcMethod::class.java)
        assertThat(grpcMethod?.path).isEqualTo("/routeguide.RouteGuide/GetFeature")
        return chain.proceed(chain.request())
      }
    }

    val grpcCall = routeGuideService.GetFeature()
    val feature = grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))

    assertThat(feature).isEqualTo(Feature(name = "tree at 5,6"))
  }

  @Test
  fun messageLargerThanMinimumSizeIsCompressed() {
    val requestBodyBuffer = Buffer()
    interceptor = object : Interceptor {
      override fun intercept(chain: Chain): Response {
        assertThat(chain.request().header("grpc-encoding")).isEqualTo("gzip")
        chain.request().body!!.writeTo(requestBodyBuffer)
        return chain.proceed(chain.request())
      }
    }

    grpcClient = grpcClient.newBuilder()
      .minMessageToCompress(1L) // Smaller than Point(5, 6).
      .build()
    routeGuideService = grpcClient.create(RouteGuideClient::class)

    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree at 5,6")
    mockService.enqueue(SendCompleted)

    val grpcCall = routeGuideService.GetFeature()
    val feature = grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))

    assertThat(feature).isEqualTo(Feature(name = "tree at 5,6"))
    assertThat(requestBodyBuffer.size).isEqualTo(29L) // 5-byte frame + 24-byte gzip data.
  }

  @Test
  fun messageSmallerThanMinimumSizeIsNotCompressed() {
    val requestBodyBuffer = Buffer()
    interceptor = object : Interceptor {
      override fun intercept(chain: Chain): Response {
        // The grpc-encoding header is sent if gzip is enabled, independent of message size.
        assertThat(chain.request().header("grpc-encoding")).isEqualTo("gzip")
        chain.request().body!!.writeTo(requestBodyBuffer)
        return chain.proceed(chain.request())
      }
    }

    grpcClient = grpcClient.newBuilder()
      .minMessageToCompress(10L) // Larger than Point(5, 6).
      .build()
    routeGuideService = grpcClient.create(RouteGuideClient::class)

    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree at 5,6")
    mockService.enqueue(SendCompleted)

    val grpcCall = routeGuideService.GetFeature()
    val feature = grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))

    assertThat(feature).isEqualTo(Feature(name = "tree at 5,6"))
    assertThat(requestBodyBuffer.size).isEqualTo(9L) // 5-byte frame + 4-byte data.
  }

  @Test
  fun maxMessageSizeToCompressDisablesCompression() {
    val requestBodyBuffer = Buffer()
    interceptor = object : Interceptor {
      override fun intercept(chain: Chain): Response {
        assertThat(chain.request().header("grpc-encoding")).isNull()
        chain.request().body!!.writeTo(requestBodyBuffer)
        return chain.proceed(chain.request())
      }
    }

    grpcClient = grpcClient.newBuilder()
      .minMessageToCompress(Long.MAX_VALUE)
      .build()
    routeGuideService = grpcClient.create(RouteGuideClient::class)

    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree at 5,6")
    mockService.enqueue(SendCompleted)

    val grpcCall = routeGuideService.GetFeature()
    val feature = grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))

    assertThat(feature).isEqualTo(Feature(name = "tree at 5,6"))
    assertThat(requestBodyBuffer.size).isEqualTo(9L) // 5-byte frame + 4-byte body.
  }

  @Test
  fun requestResponseMetadataOnSuccessfulCall() {
    mockService.enqueue(
      ReceiveCall(
        path = "/routeguide.RouteGuide/GetFeature",
        requestHeaders = mapOf("request-lucky-number" to "twenty-two"),
      ),
    )
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueue(
      SendMessage(
        message = RouteGuideProto.Feature.newBuilder()
          .setName("tree at 5,6")
          .build(),
        responseHeaders = mapOf("response-lucky-animal" to "horse"),
      ),
    )
    mockService.enqueue(SendCompleted)

    val grpcCall = routeGuideService.GetFeature()
    grpcCall.requestMetadata = mapOf("request-lucky-number" to "twenty-two")

    grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))
    assertThat(grpcCall.responseMetadata!!["response-lucky-animal"]).isEqualTo("horse")

    mockService.awaitSuccessBlocking()
  }

  @Test
  fun requestResponseMetadataOnSuccessfulClonedCall() {
    // First call.
    mockService.enqueue(
      ReceiveCall(
        path = "/routeguide.RouteGuide/GetFeature",
        requestHeaders = mapOf("request-lucky-number" to "twenty-two"),
      ),
    )
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueue(
      SendMessage(
        message = RouteGuideProto.Feature.newBuilder()
          .setName("tree at 5,6")
          .build(),
        responseHeaders = mapOf("response-lucky-animal" to "horse"),
      ),
    )
    mockService.enqueue(SendCompleted)
    // Cloned call.
    mockService.enqueue(
      ReceiveCall(
        path = "/routeguide.RouteGuide/GetFeature",
        requestHeaders = mapOf("request-lucky-number" to "twenty-two", "all-in" to "true"),
      ),
    )
    mockService.enqueueReceivePoint(latitude = 15, longitude = 16)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueue(
      SendMessage(
        message = RouteGuideProto.Feature.newBuilder()
          .setName("tree at 15,16")
          .build(),
        responseHeaders = mapOf("response-lucky-animal" to "emu"),
      ),
    )
    mockService.enqueue(SendCompleted)

    val grpcCall = routeGuideService.GetFeature()
    val callMetadata = mutableMapOf("request-lucky-number" to "twenty-two")
    grpcCall.requestMetadata = callMetadata

    // First call.
    grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))
    assertThat(grpcCall.responseMetadata!!["response-lucky-animal"]).isEqualTo("horse")

    // Cloned call.
    val clonedCall = grpcCall.clone()
    // Modifying the original call's metadata should not affect the already cloned `clonedCall`.
    callMetadata["request-lucky-number"] = "one"
    clonedCall.requestMetadata += mapOf("all-in" to "true")
    clonedCall.executeBlocking(Point(latitude = 15, longitude = 16))
    assertThat(clonedCall.responseMetadata!!["response-lucky-animal"]).isEqualTo("emu")

    mockService.awaitSuccessBlocking()
  }

  @Test
  fun requestResponseMetadataOnSuccessfulStreamingCall() {
    mockService.enqueue(
      ReceiveCall(
        path = "/routeguide.RouteGuide/ListFeatures",
        requestHeaders = mapOf("request-lucky-number" to "twenty-two"),
      ),
    )
    mockService.enqueueReceiveRectangle(lo = Point(0, 0), hi = Point(4, 5))
    mockService.enqueue(ReceiveComplete)
    mockService.enqueue(
      SendMessage(
        message = RouteGuideProto.Feature.newBuilder()
          .setName("tree")
          .build(),
        responseHeaders = mapOf("response-lucky-animal" to "horse"),
      ),
    )
    mockService.enqueueSendFeature(name = "house")
    mockService.enqueue(SendCompleted)

    val grpcCall = routeGuideService.ListFeatures()
    grpcCall.requestMetadata = mapOf("request-lucky-number" to "twenty-two")

    val (requestChannel, responseChannel) = grpcCall.executeBlocking()
    requestChannel.write(Rectangle(lo = Point(0, 0), hi = Point(4, 5)))
    requestChannel.close()
    assertThat(responseChannel.read()).isEqualTo(Feature(name = "tree"))
    assertThat(responseChannel.read()).isEqualTo(Feature(name = "house"))
    assertThat(responseChannel.read()).isNull()
    assertThat(grpcCall.responseMetadata!!["response-lucky-animal"]).isEqualTo("horse")

    mockService.awaitSuccessBlocking()
  }

  @Test
  fun requestResponseMetadataOnSuccessfulClonedStreamingCall() {
    // First call.
    mockService.enqueue(
      ReceiveCall(
        path = "/routeguide.RouteGuide/ListFeatures",
        requestHeaders = mapOf("request-lucky-number" to "twenty-two"),
      ),
    )
    mockService.enqueueReceiveRectangle(lo = Point(0, 0), hi = Point(4, 5))
    mockService.enqueue(ReceiveComplete)
    mockService.enqueue(
      SendMessage(
        message = RouteGuideProto.Feature.newBuilder()
          .setName("tree")
          .build(),
        responseHeaders = mapOf("response-lucky-animal" to "horse"),
      ),
    )
    mockService.enqueueSendFeature(name = "house")
    mockService.enqueue(SendCompleted)
    // Clone call.
    mockService.enqueue(
      ReceiveCall(
        path = "/routeguide.RouteGuide/ListFeatures",
        requestHeaders = mapOf("request-lucky-number" to "twenty-two", "all-in" to "true"),
      ),
    )
    mockService.enqueueReceiveRectangle(lo = Point(0, 0), hi = Point(14, 15))
    mockService.enqueue(ReceiveComplete)
    mockService.enqueue(
      SendMessage(
        message = RouteGuideProto.Feature.newBuilder()
          .setName("forest")
          .build(),
        responseHeaders = mapOf("response-lucky-animal" to "emu"),
      ),
    )
    mockService.enqueueSendFeature(name = "cabane")
    mockService.enqueue(SendCompleted)

    val grpcCall = routeGuideService.ListFeatures()
    val callMetadata = mutableMapOf("request-lucky-number" to "twenty-two")
    grpcCall.requestMetadata = callMetadata

    // First call.
    val (requestChannel, responseChannel) = grpcCall.executeBlocking()
    requestChannel.write(Rectangle(lo = Point(0, 0), hi = Point(4, 5)))
    requestChannel.close()
    assertThat(responseChannel.read()).isEqualTo(Feature(name = "tree"))
    assertThat(responseChannel.read()).isEqualTo(Feature(name = "house"))
    assertThat(responseChannel.read()).isNull()
    assertThat(grpcCall.responseMetadata!!["response-lucky-animal"]).isEqualTo("horse")

    // Cloned call.
    val clonedCall = grpcCall.clone()
    // Modifying the original call's metadata should not affect the already cloned `clonedCall`.
    callMetadata["request-lucky-number"] = "one"
    clonedCall.requestMetadata += mapOf("all-in" to "true")
    val (cloneRequestChannel, cloneResponseChannel) = clonedCall.executeBlocking()
    cloneRequestChannel.write(Rectangle(lo = Point(0, 0), hi = Point(14, 15)))
    cloneRequestChannel.close()
    assertThat(cloneResponseChannel.read()).isEqualTo(Feature(name = "forest"))
    assertThat(cloneResponseChannel.read()).isEqualTo(Feature(name = "cabane"))
    assertThat(cloneResponseChannel.read()).isNull()
    assertThat(clonedCall.responseMetadata!!["response-lucky-animal"]).isEqualTo("emu")
  }

  @Test
  fun requestResponseCanceledInHttpCall() {
    interceptor = object : Interceptor {
      override fun intercept(chain: Chain): Response {
        chain.call().cancel()
        throw IOException("boom")
      }
    }

    val grpcCall = routeGuideService.GetFeature()
    runBlocking {
      try {
        grpcCall.execute(Point(latitude = 5, longitude = 6))
        fail()
      } catch (expected: Throwable) {
        assertThat(expected).isInstanceOf<IOException>()
        assertThat(grpcCall.isCanceled()).isTrue()
      }
    }
  }

  @Test
  fun requestResponseStreamingCanceledInHttpCall() {
    interceptor = object : Interceptor {
      override fun intercept(chain: Chain): Response {
        chain.call().cancel()
        throw IOException("boom")
      }
    }

    val grpcCall = routeGuideService.RouteChat()
    runBlocking {
      val (_, receive) = grpcCall.executeIn(this@runBlocking)
      try {
        receive.receive()
        fail()
      } catch (expected: Throwable) {
        assertThat(expected).isInstanceOf<IOException>()
        assertThat(grpcCall.isCanceled()).isTrue()
      }
    }
  }

  @Test
  fun requestFailureWithDetailsMessage() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature("tree 5,6")
    val metadata = Metadata().apply {
      put(
        Metadata.Key.of("grpc-status-details-bin", Metadata.BINARY_BYTE_MARSHALLER),
        RouteNote(message = "marco").encode(),
      )
    }
    mockService.enqueueSendError(Status.INTERNAL.withDescription("boom").asRuntimeException(metadata))
    mockService.enqueue(SendCompleted)

    val grpcCall = routeGuideService.GetFeature()
    try {
      grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))
      fail()
    } catch (expected: GrpcException) {
      assertThat(expected.grpcStatus).isEqualTo(GrpcStatus.INTERNAL)
      assertThat(expected).messageContains(
        "grpc-status=13 grpc-status-name=INTERNAL grpc-message=boom url=" +
          mockService.url + "routeguide.RouteGuide/GetFeature",
      )
      assertThat(RouteNote.ADAPTER.decode(expected.grpcStatusDetails!!).message).isEqualTo("marco")
    }
  }

  @Test
  fun streamingRequestFailureWithUnexpectedHttpCode() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueueReceiveNote(message = "marco")
    mockService.enqueueSendError(StatusException(Status.FAILED_PRECONDITION))
    interceptor = Interceptor { chain ->
      chain
        .proceed(chain.request())
        .newBuilder()
        .code(520)
        .build()
    }

    val call = routeGuideService.RouteChat()
    runBlocking {
      val (requests, responses) = call.executeIn(this@runBlocking)
      try {
        requests.send(RouteNote(message = "marco"))
        responses.receive()
        fail()
      } catch (expected: IOException) {
        assertThat(expected).isInstanceOf<IOException>()
        assertThat(expected.message).isEqualTo("expected gRPC but was HTTP status=520, content-type=application/grpc")
        assertThat(call.isCanceled()).isTrue()
      }
    }
  }

  @Test
  fun grpcCallCallTimeoutIsPreservedFromHttpClient() {
    val call = routeGuideService.GetFeature()

    val callbackCallFuture = SettableFuture.create<GrpcCall<Point, Feature>>()

    call.enqueue(
      Point(1, 1),
      object : GrpcCall.Callback<Point, Feature> {
        override fun onFailure(
          call: GrpcCall<Point, Feature>,
          exception: IOException,
        ) {
          callbackCallFuture.set(call)
        }

        override fun onSuccess(
          call: GrpcCall<Point, Feature>,
          response: Feature,
        ) {
          callbackCallFuture.set(call)
        }
      },
    )

    assertThat(callbackCallFuture.get().timeout.timeoutNanos()).isEqualTo(okHttpClientTimeout.toNanos())
  }

  @Test
  fun grpcCallCallTimeoutCanBeUpdatedFromTheClientDefault() {
    val callbackCallFuture = SettableFuture.create<GrpcCall<Point, Feature>>()

    routeGuideService.GetFeature()
      .apply {
        timeout.timeout(okHttpClientTimeout.toMillis() * 2, TimeUnit.MILLISECONDS)
      }
      .enqueue(
        Point(1, 1),
        object : GrpcCall.Callback<Point, Feature> {
          override fun onFailure(
            call: GrpcCall<Point, Feature>,
            exception: IOException,
          ) {
            callbackCallFuture.set(call)
          }

          override fun onSuccess(
            call: GrpcCall<Point, Feature>,
            response: Feature,
          ) {
            callbackCallFuture.set(call)
          }
        },
      )

    assertThat(callbackCallFuture.get().timeout.timeoutNanos()).isEqualTo(okHttpClientTimeout.toNanos() * 2)
  }

  @Test
  fun grpcCallCallTimeoutIsPropagatedInRequestMetadata() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature", requestHeaders = mapOf("grpc-timeout" to "1000000u")))
    mockService.enqueueReceivePoint(latitude = 5, longitude = 6)
    mockService.enqueue(ReceiveComplete)
    mockService.enqueueSendFeature(name = "tree at 5,6")
    mockService.enqueue(SendCompleted)

    val grpcCall = routeGuideService.GetFeature()
    grpcCall.timeout.timeout(1, TimeUnit.SECONDS)

    val feature = grpcCall.executeBlocking(Point(latitude = 5, longitude = 6))

    assertThat(feature).isEqualTo(Feature(name = "tree at 5,6"))

    mockService.awaitSuccessBlocking()
  }

  private fun removeGrpcStatusInterceptor(): Interceptor {
    val noTrailersResponse = noTrailersResponse()
    assertThat(noTrailersResponse.trailers().size).isEqualTo(0)

    return object : Interceptor {
      override fun intercept(chain: Chain): Response {
        val response = chain.proceed(chain.request())
        return noTrailersResponse.newBuilder()
          .request(response.request)
          .code(response.code)
          .protocol(response.protocol)
          .message(response.message)
          .headers(response.headers)
          .removeHeader("grpc-status")
          .body(response.body)
          .build()
      }
    }
  }

  /**
   * OkHttp really tries to make it hard for us to strip out the trailers on a response. We
   * accomplish it by taking a completely unrelated response and building upon it. This is ugly.
   *
   * https://github.com/square/okhttp/issues/5527
   */
  private fun noTrailersResponse(): Response {
    val request = Request.Builder()
      .url(mockService.url)
      .build()
    val response = okhttpClient.newCall(request).execute()
    response.use {
      response.body!!.bytes()
    }
    return response
  }

  @ExperimentalCoroutinesApi
  private fun MockRouteGuideService.awaitSuccessBlocking() {
    runBlocking {
      awaitSuccess()
    }
  }

  class IncompatibleRouteGuideClient(
    private val client: GrpcClient,
  ) {
    fun RouteChat(): GrpcCall<RouteNote, RouteNote> =
      client.newCall(
        GrpcMethod(
          path = "/routeguide.RouteGuide/RouteChat",
          requestAdapter = RouteNote.ADAPTER,
          responseAdapter = RouteNote.ADAPTER,
        ),
      )
  }
}
