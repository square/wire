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

import com.squareup.wire.MockRouteGuideService.Action.Delay
import com.squareup.wire.MockRouteGuideService.Action.ReceiveCall
import com.squareup.wire.MockRouteGuideService.Action.ReceiveComplete
import com.squareup.wire.MockRouteGuideService.Action.ReceiveMessage
import com.squareup.wire.MockRouteGuideService.Action.SendCompleted
import com.squareup.wire.MockRouteGuideService.Action.SendMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import routeguide.Feature
import routeguide.Point
import routeguide.Rectangle
import routeguide.RouteGuide
import routeguide.RouteGuideProto
import routeguide.RouteNote
import routeguide.RouteSummary
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class GrpcTest {
  @JvmField @Rule val mockService = MockRouteGuideService()
  @JvmField @Rule val timeout = Timeout(8, TimeUnit.SECONDS)

  private lateinit var grpcClient: GrpcClient
  private lateinit var routeGuideService: RouteGuide
  private var callReference = AtomicReference<Call>()

  @Before
  fun setUp() {
    grpcClient = GrpcClient.Builder()
        .client(OkHttpClient.Builder()
            .addInterceptor { chain ->
              callReference.set(chain.call())
              chain.proceed(chain.request())
            }
            .protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE))
            .readTimeout(Duration.ofMinutes(60))
            .writeTimeout(Duration.ofMinutes(60))
            .callTimeout(Duration.ofMinutes(60))
            .build())
        .baseUrl(mockService.url)
        .build()
    routeGuideService = grpcClient.create(RouteGuide::class)
  }

  @Test
  fun requestResponse() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueue(
        ReceiveMessage(RouteGuideProto.Point.newBuilder().setLatitude(5).setLongitude(6).build()))
    mockService.enqueue(ReceiveComplete)
    mockService.enqueue(
        SendMessage(RouteGuideProto.Feature.newBuilder().setName("tree at 5,6").build()))
    mockService.enqueue(SendCompleted)

    runBlocking {
      val feature = routeGuideService.GetFeature(Point(latitude = 5, longitude = 6))
      assertThat(feature).isEqualTo(Feature(name = "tree at 5,6"))
    }
  }

  @Test
  fun streamingRequest() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RecordRoute"))
    mockService.enqueue(
        ReceiveMessage(RouteGuideProto.Point.newBuilder().setLatitude(3).setLongitude(3).build()))
    mockService.enqueue(
        ReceiveMessage(RouteGuideProto.Point.newBuilder().setLatitude(9).setLongitude(6).build()))
    mockService.enqueue(
        SendMessage(RouteGuideProto.RouteSummary.newBuilder().setPointCount(2).build()))
    mockService.enqueue(SendCompleted)
    mockService.enqueue(ReceiveComplete)

    val (requestChannel, deferredResponse) = routeGuideService.RecordRoute()
    runBlocking {
      requestChannel.send(Point(3, 3))
      requestChannel.send(Point(9, 6))
      requestChannel.close()
      assertThat(deferredResponse.await()).isEqualTo(RouteSummary(point_count = 2))
    }
  }

  @Test
  fun streamingResponse() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/ListFeatures"))
    mockService.enqueue(
        ReceiveMessage(RouteGuideProto.Rectangle.newBuilder()
            .setLo(RouteGuideProto.Point.newBuilder().setLatitude(0).setLongitude(0).build())
            .setHi(RouteGuideProto.Point.newBuilder().setLatitude(4).setLongitude(5).build())
            .build()))
    mockService.enqueue(ReceiveComplete)
    mockService.enqueue(
        SendMessage(RouteGuideProto.Feature.newBuilder().setName("tree").build()))
    mockService.enqueue(
        SendMessage(RouteGuideProto.Feature.newBuilder().setName("house").build()))
    mockService.enqueue(SendCompleted)

    val responseChannel = routeGuideService.ListFeatures(
        Rectangle(lo = Point(0, 0), hi = Point(4, 5)))
    runBlocking {
      assertThat(responseChannel.receive()).isEqualTo(Feature(name = "tree"))
      assertThat(responseChannel.receive()).isEqualTo(Feature(name = "house"))
      assertThat(responseChannel.receiveOrNull()).isNull()
    }
  }

  @Test
  fun duplex_receiveDataAfterClosingRequest() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueue(
        ReceiveMessage(RouteGuideProto.RouteNote.newBuilder().setMessage("marco").build()))
    mockService.enqueue(
        SendMessage(RouteGuideProto.RouteNote.newBuilder().setMessage("polo").build()))
    mockService.enqueue(
        ReceiveMessage(RouteGuideProto.RouteNote.newBuilder().setMessage("rené").build()))
    mockService.enqueue(
        SendMessage(RouteGuideProto.RouteNote.newBuilder().setMessage("lacoste").build()))
    mockService.enqueue(ReceiveComplete)
    mockService.enqueue(SendCompleted)

    val (requestChannel, responseChannel) = routeGuideService.RouteChat()
    runBlocking {
      requestChannel.send(RouteNote(message = "marco"))
      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "polo"))
      requestChannel.send(RouteNote(message = "rené"))
      requestChannel.close()
      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "lacoste"))
      assertThat(responseChannel.receiveOrNull()).isNull()
    }
  }

  @Test
  fun duplex_receiveDataBeforeClosingRequest() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueue(
        ReceiveMessage(RouteGuideProto.RouteNote.newBuilder().setMessage("marco").build()))
    mockService.enqueue(
        SendMessage(RouteGuideProto.RouteNote.newBuilder().setMessage("polo").build()))
    mockService.enqueue(
        ReceiveMessage(RouteGuideProto.RouteNote.newBuilder().setMessage("rené").build()))
    mockService.enqueue(
        SendMessage(RouteGuideProto.RouteNote.newBuilder().setMessage("lacoste").build()))
    // We give time to the sender to read the response.
    mockService.enqueue(Delay(500, TimeUnit.MILLISECONDS))
    mockService.enqueue(SendCompleted)
    mockService.enqueue(ReceiveComplete)

    val (requestChannel, responseChannel) = routeGuideService.RouteChat()
    runBlocking {
      requestChannel.send(RouteNote(message = "marco"))
      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "polo"))
      requestChannel.send(RouteNote(message = "rené"))
      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "lacoste"))
      assertThat(responseChannel.receiveOrNull()).isNull()
      requestChannel.close()
    }
  }

  @Test
  fun cancelRequestResponse() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    mockService.enqueue(
        ReceiveMessage(RouteGuideProto.Point.newBuilder().setLatitude(5).setLongitude(6).build()))
    mockService.enqueue(ReceiveComplete)
    mockService.enqueue(Delay(500, TimeUnit.MILLISECONDS))
    mockService.enqueue(
        SendMessage(RouteGuideProto.Feature.newBuilder().setName("tree at 5,6").build()))
    mockService.enqueue(SendCompleted)

    runBlocking {
      val deferred = async {
        routeGuideService.GetFeature(Point(latitude = 5, longitude = 6))
        fail()
      }
      // We wait for the request to complete and cancel before the response is sent.
      delay(200)
      deferred.cancel()
      mockService.awaitSuccess()
      assertThat(callReference.get()?.isCanceled).isTrue()
    }
  }

  @Test
  fun cancelStreamingRequest() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RecordRoute"))

    val (_, responseDeferred) = routeGuideService.RecordRoute()
    runBlocking {
      // We wait for the request to proceed.
      delay(200)
      responseDeferred.cancel()
      mockService.awaitSuccess()
      assertThat(callReference.get()?.isCanceled).isTrue()
    }
  }

  // TODO(benoit) Maybe add backpressure test (tried and failed)

  @Test
  fun cancelStreamingResponse() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/ListFeatures"))
    mockService.enqueue(
        ReceiveMessage(RouteGuideProto.Rectangle.newBuilder()
            .setLo(RouteGuideProto.Point.newBuilder().setLatitude(0).setLongitude(0).build())
            .setHi(RouteGuideProto.Point.newBuilder().setLatitude(4).setLongitude(5).build())
            .build()))
    mockService.enqueue(ReceiveComplete)
    mockService.enqueue(
        SendMessage(RouteGuideProto.Feature.newBuilder().setName("tree").build()))
    mockService.enqueue(Delay(500, TimeUnit.MILLISECONDS))
    mockService.enqueue(
        SendMessage(RouteGuideProto.Feature.newBuilder().setName("house").build()))
    mockService.enqueue(SendCompleted)

    val receiveChannel =
        routeGuideService.ListFeatures(Rectangle(lo = Point(0, 0), hi = Point(4, 5)))
    runBlocking {
      receiveChannel.receive()
      receiveChannel.cancel()
      mockService.awaitSuccess()
      assertThat(callReference.get()?.isCanceled).isTrue()
    }
  }

  @Test
  fun cancelDuplexBeforeRequestCompletes() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))

    val (requestChannel, responseChannel) = routeGuideService.RouteChat()
    runBlocking {
      // We wait for mockService to process our actions.
      delay(500)
      responseChannel.cancel()
      assertThat((requestChannel as Channel).isClosedForReceive).isTrue()
      assertThat(requestChannel.isClosedForSend).isTrue()
      mockService.awaitSuccess()
      assertThat(callReference.get()?.isCanceled).isTrue()
    }
  }

  @Test
  fun cancelDuplexAfterRequestCompletes() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueue(
        ReceiveMessage(RouteGuideProto.RouteNote.newBuilder().setMessage("marco").build()))
    mockService.enqueue(ReceiveComplete)

    val (requestChannel, responseChannel) = routeGuideService.RouteChat()
    runBlocking {
      requestChannel.send(RouteNote(message = "marco"))
      requestChannel.close()
      // We wait for mockService to process our actions.
      delay(500)

      responseChannel.cancel()
      assertThat((requestChannel as Channel).isClosedForReceive).isTrue()
      assertThat(requestChannel.isClosedForSend).isTrue()
      mockService.awaitSuccess()
      assertThat(callReference.get()?.isCanceled).isTrue()
    }
  }

  @Test
  fun cancelDuplexBeforeResponseCompletes() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueue(
        ReceiveMessage(RouteGuideProto.RouteNote.newBuilder().setMessage("marco").build()))
    mockService.enqueue(
        SendMessage(RouteGuideProto.RouteNote.newBuilder().setMessage("polo").build()))
    mockService.enqueue(
        ReceiveMessage(RouteGuideProto.RouteNote.newBuilder().setMessage("rené").build()))
    mockService.enqueue(ReceiveComplete)

    val (requestChannel, responseChannel) = routeGuideService.RouteChat()
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
      assertThat(callReference.get()?.isCanceled).isTrue()
    }
  }

  @Test
  fun cancelDuplexAfterResponseCompletes() {
    mockService.enqueue(ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    mockService.enqueue(
        ReceiveMessage(RouteGuideProto.RouteNote.newBuilder().setMessage("marco").build()))
    mockService.enqueue(
        SendMessage(RouteGuideProto.RouteNote.newBuilder().setMessage("polo").build()))
    mockService.enqueue(
        ReceiveMessage(RouteGuideProto.RouteNote.newBuilder().setMessage("rené").build()))
    mockService.enqueue(
        SendMessage(RouteGuideProto.RouteNote.newBuilder().setMessage("lacoste").build()))
    mockService.enqueue(ReceiveComplete)
    mockService.enqueue(SendCompleted)

    val (requestChannel, responseChannel) = routeGuideService.RouteChat()
    runBlocking {
      requestChannel.send(RouteNote(message = "marco"))
      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "polo"))
      requestChannel.send(RouteNote(message = "rené"))
      assertThat(responseChannel.receive()).isEqualTo(RouteNote(message = "lacoste"))
      requestChannel.close()
      assertThat(responseChannel.receiveOrNull()).isNull()

      // We wait for mockService to process our actions.
      delay(500)

      responseChannel.cancel()
      assertThat((requestChannel as Channel).isClosedForReceive).isTrue()
      assertThat(requestChannel.isClosedForSend).isTrue()
      assertThat(callReference.get()?.isCanceled).isTrue()
    }
  }
}
