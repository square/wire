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
package com.squareup.wire

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.message
import com.squareup.wire.MockRouteGuideService.Action.ReceiveCall
import io.grpc.ForwardingServerCall.SimpleForwardingServerCall
import io.grpc.Metadata
import io.grpc.Metadata.ASCII_STRING_MARSHALLER
import io.grpc.Metadata.Key
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerCall
import io.grpc.ServerCall.Listener
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.stub.StreamObserver
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import routeguide.RouteGuideGrpc
import routeguide.RouteGuideProto.Feature
import routeguide.RouteGuideProto.Point
import routeguide.RouteGuideProto.Rectangle
import routeguide.RouteGuideProto.RouteNote
import routeguide.RouteGuideProto.RouteSummary

/**
 * An assertive scriptable implementation of the [RouteGuideGrpc] gRPC service. Receiving and
 * sending actions can be added via the [MockRouteGuideService.enqueue] method.
 */
class MockRouteGuideService : RouteGuideGrpc.RouteGuideImplBase(), TestRule, ServerInterceptor {
  private lateinit var server: Server
  private lateinit var streamObserver: StreamObserver<Any>
  private var lastRequestHeaders: Metadata? = null
  private var nextResponseHeaders: Map<String, String> = mapOf()
  private val script = ArrayDeque<Action>()
  private val scriptEmpty = Throwable("script is empty")
  private val scriptResults = Channel<Throwable>(capacity = UNLIMITED)

  val url: HttpUrl
    get() = HttpUrl.Builder().scheme("http").host("127.0.0.1").port(server.port).build()

  fun enqueue(action: Action) {
    script.add(action)
  }

  fun enqueueReceiveNote(message: String) {
    enqueue(
      Action.ReceiveMessage(
        RouteNote.newBuilder()
          .setMessage(message)
          .build(),
      ),
    )
  }

  fun enqueueReceivePoint(latitude: Int, longitude: Int) {
    enqueue(
      Action.ReceiveMessage(
        Point.newBuilder()
          .setLatitude(latitude)
          .setLongitude(longitude)
          .build(),
      ),
    )
  }

  fun enqueueReceiveRectangle(lo: routeguide.Point, hi: routeguide.Point) {
    enqueue(
      Action.ReceiveMessage(
        Rectangle.newBuilder()
          .setLo(Point.newBuilder().setLatitude(lo.latitude!!).setLongitude(lo.longitude!!).build())
          .setHi(Point.newBuilder().setLatitude(hi.latitude!!).setLongitude(hi.longitude!!).build())
          .build(),
      ),
    )
  }

  fun enqueueSendFeature(name: String) {
    enqueue(
      Action.SendMessage(
        Feature.newBuilder()
          .setName(name)
          .build(),
      ),
    )
  }

  fun enqueueSendNote(message: String) {
    enqueue(
      Action.SendMessage(
        RouteNote.newBuilder()
          .setMessage(message)
          .build(),
      ),
    )
  }

  fun enqueueSendSummary(pointCount: Int) {
    enqueue(
      Action.SendMessage(
        RouteSummary.newBuilder()
          .setPointCount(pointCount)
          .build(),
      ),
    )
  }

  fun enqueueSendError(error: Throwable) {
    enqueue(Action.SendError(error))
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
          .intercept(this@MockRouteGuideService)
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
    @Suppress("UNCHECKED_CAST")
    streamObserver = responseObserver as StreamObserver<Any>
    assertNextActionAndProcessScript {
      assertThat(it).isEqualTo(
        ReceiveCall(
          path = "/routeguide.RouteGuide/GetFeature",
          requestHeaders = takeLastRequestHeaders(it),
        ),
      )
    }
    assertNextActionAndProcessScript {
      assertThat(it).isEqualTo(Action.ReceiveMessage(point))
    }
    assertNextActionAndProcessScript {
      assertThat(it).isEqualTo(Action.ReceiveComplete)
    }
  }

  override fun recordRoute(responseObserver: StreamObserver<RouteSummary>): StreamObserver<Point> {
    @Suppress("UNCHECKED_CAST")
    streamObserver = responseObserver as StreamObserver<Any>
    assertNextActionAndProcessScript {
      assertThat(it).isEqualTo(
        ReceiveCall(
          path = "/routeguide.RouteGuide/RecordRoute",
          requestHeaders = takeLastRequestHeaders(it),
        ),
      )
    }
    return createAssertingStreamObserver()
  }

  override fun listFeatures(rectangle: Rectangle, responseObserver: StreamObserver<Feature>) {
    @Suppress("UNCHECKED_CAST")
    streamObserver = responseObserver as StreamObserver<Any>
    assertNextActionAndProcessScript {
      assertThat(it).isEqualTo(
        ReceiveCall(
          path = "/routeguide.RouteGuide/ListFeatures",
          requestHeaders = takeLastRequestHeaders(it),
        ),
      )
    }
    assertNextActionAndProcessScript {
      assertThat(it).isEqualTo(Action.ReceiveMessage(rectangle))
    }
    assertNextActionAndProcessScript {
      assertThat(it).isEqualTo(Action.ReceiveComplete)
    }
  }

  override fun routeChat(responseObserver: StreamObserver<RouteNote>): StreamObserver<RouteNote> {
    @Suppress("UNCHECKED_CAST")
    streamObserver = responseObserver as StreamObserver<Any>
    assertNextActionAndProcessScript {
      assertThat(it).isEqualTo(
        ReceiveCall(
          path = "/routeguide.RouteGuide/RouteChat",
          requestHeaders = takeLastRequestHeaders(it),
        ),
      )
    }
    return createAssertingStreamObserver()
  }

  /** Use an interceptor to make assertions about request and response metadata. */
  override fun <ReqT : Any?, RespT : Any?> interceptCall(
    call: ServerCall<ReqT, RespT>,
    requestHeaders: Metadata,
    next: ServerCallHandler<ReqT, RespT>,
  ): Listener<ReqT> {
    lastRequestHeaders = requestHeaders
    return next.startCall(
      object : SimpleForwardingServerCall<ReqT, RespT>(call) {
        override fun sendHeaders(responseHeaders: Metadata) {
          for ((key, value) in nextResponseHeaders) {
            responseHeaders.put(key.toKey(), value)
          }
          nextResponseHeaders = mapOf()
          super.sendHeaders(responseHeaders)
        }
      },
      requestHeaders,
    )
  }

  private fun <T : com.google.protobuf.Message> createAssertingStreamObserver(): StreamObserver<T> {
    return object : StreamObserver<T> {
      override fun onNext(value: T) {
        assertNextActionAndProcessScript {
          assertThat(it).isEqualTo(Action.ReceiveMessage(value))
        }
      }

      override fun onError(t: Throwable?) {
        assertNextActionAndProcessScript {
          assertThat(it).isEqualTo(Action.ReceiveError)
        }
      }

      override fun onCompleted() {
        assertNextActionAndProcessScript {
          assertThat(it).isEqualTo(Action.ReceiveComplete)
        }
      }
    }
  }

  private fun assertNextActionAndProcessScript(nextActionAssert: (Action) -> Unit) {
    assertNextAction(nextActionAssert)
    processActions()
  }

  private fun assertNextAction(nextActionAssert: (Action) -> Unit) {
    try {
      val poll = script.poll()
      nextActionAssert(poll)
    } catch (e: Throwable) {
      runBlocking {
        scriptResults.send(e)
      }
    }
  }

  /** Perform any actions that are immediately executable. */
  private fun processActions() {
    try {
      while (true) {
        val action = script.peek()
        when {
          action == null -> {
            // No more actions in the queue.
            runBlocking {
              scriptResults.send(scriptEmpty)
            }
            return
          }
          action is Action.SendMessage -> {
            nextResponseHeaders = action.responseHeaders
            streamObserver.onNext(action.message)
          }
          action is Action.SendError -> {
            streamObserver.onError(action.throwable)
          }
          action is Action.SendCompleted -> {
            streamObserver.onCompleted()
          }
          action is Action.Delay -> {
            Thread.sleep(action.timeUnit.toMillis(action.duration))
          }
          else -> {
            return // We've run all the actions we can.
          }
        }
        script.removeFirst()
      }
    } catch (e: Throwable) {
      runBlocking {
        scriptResults.send(e)
      }
    }
  }

  private fun String.toKey() = Key.of(this, ASCII_STRING_MARSHALLER)

  private fun takeLastRequestHeaders(expectedAction: Action): Map<String, String> {
    val result = mutableMapOf<String, String>()
    if (expectedAction is ReceiveCall) {
      for (key in expectedAction.requestHeaders.keys) {
        val value = lastRequestHeaders?.get(key.toKey()) ?: continue
        result[key] = value
      }
    }
    lastRequestHeaders = null
    return result
  }

  sealed class Action {
    data class ReceiveCall(
      val path: String,
      val requestHeaders: Map<String, String> = mapOf(),
    ) : Action()
    data class ReceiveMessage(val message: com.google.protobuf.Message) : Action()
    object ReceiveError : Action()
    object ReceiveComplete : Action()
    data class SendMessage(
      val message: com.google.protobuf.Message,
      val responseHeaders: Map<String, String> = mapOf(),
    ) : Action()
    data class SendError(val throwable: Throwable) : Action()
    object SendCompleted : Action()
    data class Delay(val duration: Long, val timeUnit: TimeUnit) : Action()
  }
}
