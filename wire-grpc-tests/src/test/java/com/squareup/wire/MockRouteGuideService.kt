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

import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import routeguide.RouteGuideGrpc
import routeguide.RouteGuideProto
import routeguide.RouteGuideProto.Feature
import routeguide.RouteGuideProto.Point
import routeguide.RouteGuideProto.Rectangle
import routeguide.RouteGuideProto.RouteNote
import routeguide.RouteGuideProto.RouteSummary
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit

/**
 * An assertive scriptable implementation of the [RouteGuideGrpc] gRPC service. Receiving and
 * sending actions can be added via the [MockRouteGuideService.enqueue] method.
 */
class MockRouteGuideService : RouteGuideGrpc.RouteGuideImplBase(), TestRule {
  private lateinit var server: Server
  private lateinit var streamObserver: StreamObserver<Any>
  private val script = ArrayDeque<Action>()
  private val scriptEmpty = Throwable("script is empty")
  private val scriptResults = Channel<Throwable>(capacity = UNLIMITED)

  val url: HttpUrl
    get() = HttpUrl.Builder().scheme("http").host("127.0.0.1").port(server.port).build()

  fun enqueue(action: Action) {
    script.add(action)
  }

  fun enqueueReceiveNote(message: String) {
    enqueue(Action.ReceiveMessage(RouteNote.newBuilder()
        .setMessage(message)
        .build()))
  }

  fun enqueueReceivePoint(latitude: Int, longitude: Int) {
    enqueue(Action.ReceiveMessage(RouteGuideProto.Point.newBuilder()
        .setLatitude(latitude)
        .setLongitude(longitude)
        .build()))
  }

  fun enqueueReceiveRectangle(lo: routeguide.Point, hi: routeguide.Point) {
    enqueue(Action.ReceiveMessage(Rectangle.newBuilder()
        .setLo(Point.newBuilder().setLatitude(lo.latitude!!).setLongitude(lo.longitude!!).build())
        .setHi(Point.newBuilder().setLatitude(hi.latitude!!).setLongitude(hi.longitude!!).build())
        .build()))
  }

  fun enqueueSendFeature(name: String) {
    enqueue(Action.SendMessage(Feature.newBuilder()
        .setName(name)
        .build()))
  }

  fun enqueueSendNote(message: String) {
    enqueue(Action.SendMessage(RouteNote.newBuilder()
        .setMessage(message)
        .build()))
  }

  fun enqueueSendSummary(pointCount: Int) {
    enqueue(Action.SendMessage(RouteSummary.newBuilder()
        .setPointCount(pointCount)
        .build()))
  }

  suspend fun awaitSuccess() {
    try {
      withTimeout(3000L) {
        val result = scriptResults.receive()
        if (result != scriptEmpty) throw result
      }
    } catch (e: TimeoutCancellationException) {
      throw AssertionError("script had stuff left over: $script")
    }
  }

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        server = ServerBuilder.forPort(0)
            .addService(this@MockRouteGuideService)
            .build()
        server.start()
        try {
          base.evaluate()
        } finally {
          server.shutdown()
        }
        server.awaitTermination()
      }
    }
  }

  override fun getFeature(point: Point, responseObserver: StreamObserver<Feature>) {
    streamObserver = responseObserver as StreamObserver<Any>
    processScript {
      assertThat(it).isEqualTo(Action.ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    }
    processScript {
      assertThat(it).isEqualTo(Action.ReceiveMessage(point))
    }
    processScript {
      assertThat(it).isEqualTo(Action.ReceiveComplete)
    }
  }

  override fun recordRoute(responseObserver: StreamObserver<RouteSummary>): StreamObserver<Point> {
    streamObserver = responseObserver as StreamObserver<Any>
    processScript {
      assertThat(it).isEqualTo(Action.ReceiveCall("/routeguide.RouteGuide/RecordRoute"))
    }
    return createAssertingStreamObserver()
  }

  override fun listFeatures(rectangle: Rectangle, responseObserver: StreamObserver<Feature>) {
    streamObserver = responseObserver as StreamObserver<Any>
    processScript {
      assertThat(it).isEqualTo(Action.ReceiveCall("/routeguide.RouteGuide/ListFeatures"))
    }
    processScript {
      assertThat(it).isEqualTo(Action.ReceiveMessage(rectangle))
    }
    processScript {
      assertThat(it).isEqualTo(Action.ReceiveComplete)
    }
  }

  override fun routeChat(responseObserver: StreamObserver<RouteNote>): StreamObserver<RouteNote> {
    streamObserver = responseObserver as StreamObserver<Any>
    processScript {
      assertThat(it).isEqualTo(Action.ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    }
    return createAssertingStreamObserver()
  }

  private fun <T : com.google.protobuf.Message> createAssertingStreamObserver(): StreamObserver<T> {
    return object : StreamObserver<T> {
      override fun onNext(value: T) {
        processScript {
          assertThat(it).isEqualTo(Action.ReceiveMessage(value))
        }
      }

      override fun onError(t: Throwable?) {
        processScript {
          assertThat(it).isEqualTo(Action.ReceiveError)
        }
      }

      override fun onCompleted() {
        processScript {
          assertThat(it).isEqualTo(Action.ReceiveComplete)
        }
      }
    }
  }

  /** Execute actions that are immediately ready. */
  private fun processScript(nextActionAssert: (Action) -> Unit) {
    try {
      val poll = script.poll()
      nextActionAssert(poll)

      // If other actions are executable, execute 'em immediately.
      while (true) {
        val action = script.peek()
        if (action == null) {
          runBlocking {
            scriptResults.send(scriptEmpty)
          }
          return
        }
        when (action) {
          is Action.SendMessage -> {
            script.removeFirst()
            streamObserver.onNext(action.message)
          }
          is Action.SendError -> {
            script.removeFirst()
            streamObserver.onError(action.throwable)
          }
          is Action.SendCompleted -> {
            script.removeFirst()
            streamObserver.onCompleted()
          }
          is Action.Delay -> {
            script.removeFirst()
            Thread.sleep(action.timeUnit.toMillis(action.duration))
          }
          else -> return
        }
      }
    } catch (e: Throwable) {
      runBlocking {
        scriptResults.send(e)
      }
    }
  }

  sealed class Action {
    data class ReceiveCall(val path: String) : Action()
    data class ReceiveMessage(val message: com.google.protobuf.Message) : Action()
    object ReceiveError : Action()
    object ReceiveComplete : Action()
    data class SendMessage(val message: com.google.protobuf.Message) : Action()
    data class SendError(val throwable: Throwable) : Action()
    object SendCompleted : Action()
    data class Delay(val duration: Long, val timeUnit: TimeUnit) : Action()
  }
}
