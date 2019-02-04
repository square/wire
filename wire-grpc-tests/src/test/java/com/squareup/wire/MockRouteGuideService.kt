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
import okhttp3.HttpUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import routeguide.RouteGuideGrpc
import routeguide.RouteGuideProto.Feature
import routeguide.RouteGuideProto.Point
import routeguide.RouteGuideProto.Rectangle
import routeguide.RouteGuideProto.RouteNote
import routeguide.RouteGuideProto.RouteSummary
import java.util.ArrayDeque

/**
 * An assertive scriptable implementation of the [RouteGuideGrpc] gRPC service. Receiving and
 * sending actions can be added via the [MockRouteGuideService.enqueue] method.
 */
class MockRouteGuideService : RouteGuideGrpc.RouteGuideImplBase(), TestRule {
  private lateinit var server: Server
  private lateinit var streamObserver: StreamObserver<Any>
  private val script = ArrayDeque<Action>()

  val url: HttpUrl
    get() = HttpUrl.Builder().scheme("http").host("127.0.0.1").port(server.port).build()

  fun enqueue(action: Action) {
    script.add(action)
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
          server.awaitTermination()
        }
      }
    }
  }

  override fun getFeature(point: Point, responseObserver: StreamObserver<Feature>) {
    assertThat(script.removeFirst())
        .isEqualTo(Action.ReceiveCall("/routeguide.RouteGuide/GetFeature"))
    assertThat(script.removeFirst()).isEqualTo(Action.ReceiveMessage(point))
    assertThat(script.removeFirst()).isEqualTo(Action.ReceiveComplete)
    streamObserver = responseObserver as StreamObserver<Any>
    processScript()
  }

  override fun recordRoute(responseObserver: StreamObserver<RouteSummary>): StreamObserver<Point> {
    assertThat(script.removeFirst())
        .isEqualTo(Action.ReceiveCall("/routeguide.RouteGuide/RecordRoute"))

    streamObserver = responseObserver as StreamObserver<Any>
    processScript()

    return createAssertingStreamObserver()
  }

  override fun listFeatures(rectangle: Rectangle, responseObserver: StreamObserver<Feature>) {
    assertThat(script.removeFirst())
        .isEqualTo(Action.ReceiveCall("/routeguide.RouteGuide/ListFeatures"))
    assertThat(script.removeFirst()).isEqualTo(Action.ReceiveMessage(rectangle))
    assertThat(script.removeFirst()).isEqualTo(Action.ReceiveComplete)

    streamObserver = responseObserver as StreamObserver<Any>
    processScript()
  }

  override fun routeChat(responseObserver: StreamObserver<RouteNote>): StreamObserver<RouteNote> {
    assertThat(script.removeFirst())
        .isEqualTo(Action.ReceiveCall("/routeguide.RouteGuide/RouteChat"))
    streamObserver = responseObserver as StreamObserver<Any>
    processScript()


    return createAssertingStreamObserver()
  }

  private fun <T : com.google.protobuf.Message> createAssertingStreamObserver(): StreamObserver<T> {
    return object : StreamObserver<T> {
      override fun onNext(value: T) {
        assertThat(script.removeFirst()).isEqualTo(Action.ReceiveMessage(value))
        processScript()
      }

      override fun onError(t: Throwable?) {
        assertThat(script.removeFirst()).isEqualTo(Action.ReceiveError)
        processScript()
      }

      override fun onCompleted() {
        assertThat(script.removeFirst()).isEqualTo(Action.ReceiveComplete)
        processScript()
      }
    }
  }

  /** Execute actions that are immediately ready. */
  private fun processScript() {
    while (true) {
      val action = script.peek() ?: return
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
        else -> return
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
  }
}