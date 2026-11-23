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
import assertk.assertions.contains
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.prop
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import okhttp3.Headers.Companion.headersOf
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.GzipSink
import okio.IOException
import okio.buffer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import routeguide.Feature
import routeguide.Point

@Suppress("OPT_IN_USAGE")
class GrpcMaxInboundMessageSizeTest {
  @JvmField @Rule
  val mockWebServer = MockWebServer()

  @JvmField @Rule
  val testTimeout = Timeout(30, TimeUnit.SECONDS)

  private lateinit var okhttpClient: OkHttpClient

  private val method = GrpcMethod(
    path = "/routeguide.RouteGuide/GetFeature",
    requestAdapter = Point.ADAPTER,
    responseAdapter = Feature.ADAPTER,
  )

  private val serverStreamingMethod = GrpcMethod(
    path = "/routeguide.RouteGuide/ListFeatures",
    requestAdapter = Point.ADAPTER,
    responseAdapter = Feature.ADAPTER,
    responseStreaming = true,
  )

  @Before
  fun setUp() {
    mockWebServer.protocols = listOf(Protocol.H2_PRIOR_KNOWLEDGE)
    okhttpClient = OkHttpClient.Builder()
      .protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE))
      .build()
  }

  @Test
  fun defaultLimitAllowsLargeMessage() {
    val feature = featureOfSize(5 * 1024 * 1024)
    enqueue(frame(Feature.ADAPTER.encode(feature)))

    assertThat(client().newCall(method).executeBlocking(Point())).isEqualTo(feature)
  }

  @Test
  fun messageAtTheLimitIsAccepted() {
    val message = Feature.ADAPTER.encode(featureOfSize(1000))
    for (gzip in listOf(false, true)) {
      enqueue(frame(message, gzip), gzip)

      val feature = client(maxInboundMessageSize = message.size.toLong())
        .newCall(method)
        .executeBlocking(Point())

      assertThat(Feature.ADAPTER.encode(feature).size).isEqualTo(message.size)
    }
  }

  @Test
  fun messageOverTheLimitFails() {
    val message = Feature.ADAPTER.encode(featureOfSize(1000))
    enqueue(frame(message))

    assertFailure {
      client(maxInboundMessageSize = message.size - 1L).newCall(method).executeBlocking(Point())
    }.isMessageTooLarge()
  }

  @Test
  fun compressedMessageOverTheLimitAfterInflatingFails() {
    // The frame is small on the wire, under the limit, and the message is large once inflated.
    val message = Feature.ADAPTER.encode(featureOfSize(16 * 1024 * 1024))
    val framed = frame(message, gzip = true)
    assertThat(framed.size < 64 * 1024).isEqualTo(true)
    enqueue(framed, gzip = true)

    assertFailure {
      client(maxInboundMessageSize = 1024 * 1024).newCall(method).executeBlocking(Point())
    }.isMessageTooLarge(messageContains = "Decompressed gRPC message exceeds maximum size")
  }

  @Test
  fun lengthPrefixOverTheLimitFailsWithoutReadingTheMessage() {
    // The prefix claims 4 GiB - 1, and the body ends right after it.
    enqueue(Buffer().writeByte(0).writeInt(-1).write(ByteArray(10)).readByteArray())

    assertFailure {
      client().newCall(method).executeBlocking(Point())
    }.isMessageTooLarge()
  }

  @Test
  fun suspendCallFails() {
    enqueue(frame(Feature.ADAPTER.encode(featureOfSize(1000))))

    assertFailure {
      runBlocking { client(maxInboundMessageSize = 100).newCall(method).execute(Point()) }
    }.isMessageTooLarge()
  }

  @Test
  fun callbackCallFails() {
    enqueue(frame(Feature.ADAPTER.encode(featureOfSize(1000))))
    val result = CompletableFuture<Feature>()

    client(maxInboundMessageSize = 100).newCall(method).enqueue(
      Point(),
      object : GrpcCall.Callback<Point, Feature> {
        override fun onFailure(call: GrpcCall<Point, Feature>, exception: IOException) {
          result.completeExceptionally(exception)
        }

        override fun onSuccess(call: GrpcCall<Point, Feature>, response: Feature) {
          result.complete(response)
        }
      },
    )

    assertFailure {
      try {
        result.get(10, TimeUnit.SECONDS)
      } catch (e: ExecutionException) {
        throw e.cause!!
      }
    }.isMessageTooLarge()
  }

  @Test
  fun blockingStreamingCallFailsOnTheLargeMessageOnly() {
    val small = featureOfSize(10)
    enqueue(
      frame(Feature.ADAPTER.encode(small)) +
        frame(Feature.ADAPTER.encode(featureOfSize(1000))),
    )

    val (sink, source) = client(maxInboundMessageSize = 100).newStreamingCall(method).executeBlocking()
    sink.close()

    assertThat(source.read()).isEqualTo(small)
    assertFailure { source.read() }.isMessageTooLarge()
  }

  @Test
  fun suspendStreamingCallFailsOnTheLargeMessageOnly() {
    val small = featureOfSize(10)
    enqueue(
      frame(Feature.ADAPTER.encode(small)) +
        frame(Feature.ADAPTER.encode(featureOfSize(1000))),
    )

    val (requests, responses) = client(maxInboundMessageSize = 100).newStreamingCall(method).executeIn(GlobalScope)
    requests.close()

    runBlocking {
      assertThat(responses.receive()).isEqualTo(small)
      assertFailure { responses.receive() }.isMessageTooLarge()
    }
  }

  @Test
  fun blockingServerStreamingCallFailsOnTheLargeMessageOnly() {
    val small = featureOfSize(10)
    enqueue(
      frame(Feature.ADAPTER.encode(small)) +
        frame(Feature.ADAPTER.encode(featureOfSize(1000))),
    )

    val source = client(maxInboundMessageSize = 100).newServerStreamingCall(serverStreamingMethod).executeBlocking(Point())

    assertThat(source.read()).isEqualTo(small)
    assertFailure { source.read() }.isMessageTooLarge()
  }

  @Test
  fun suspendServerStreamingCallFailsOnTheLargeMessageOnly() {
    val small = featureOfSize(10)
    enqueue(
      frame(Feature.ADAPTER.encode(small)) +
        frame(Feature.ADAPTER.encode(featureOfSize(1000))),
    )

    runBlocking {
      val responses = client(maxInboundMessageSize = 100).newServerStreamingCall(serverStreamingMethod).executeIn(this, Point())
      assertThat(responses.receive()).isEqualTo(small)
      assertFailure { responses.receive() }.isMessageTooLarge()
    }
  }

  @Test
  fun legacyServerStreamingCallFailsOnTheLargeMessageOnly() {
    val small = featureOfSize(10)
    enqueue(
      frame(Feature.ADAPTER.encode(small)) +
        frame(Feature.ADAPTER.encode(featureOfSize(1000))),
    )

    val (sink, source) = client(maxInboundMessageSize = 100).newStreamingCall(serverStreamingMethod).executeBlocking()
    sink.write(Point())
    sink.close()

    assertThat(source.read()).isEqualTo(small)
    assertFailure { source.read() }.isMessageTooLarge()
  }

  @Test
  fun newBuilderKeepsTheLimit() {
    val message = Feature.ADAPTER.encode(featureOfSize(1000))
    enqueue(frame(message))

    val copy = client(maxInboundMessageSize = 100).newBuilder().build()

    assertFailure {
      copy.newCall(method).executeBlocking(Point())
    }.isMessageTooLarge()
  }

  @Test
  fun negativeLimitIsRejected() {
    assertFailure {
      GrpcClient.Builder().maxInboundMessageSize(-1)
    }.isInstanceOf<IllegalArgumentException>()
      .hasMessage("maxInboundMessageSize must not be negative: -1")
  }

  private fun client(maxInboundMessageSize: Long? = null): GrpcClient = GrpcClient.Builder()
    .client(okhttpClient)
    .baseUrl(mockWebServer.url("/"))
    .apply { if (maxInboundMessageSize != null) maxInboundMessageSize(maxInboundMessageSize) }
    .build()

  private fun featureOfSize(size: Int) = Feature(name = "a".repeat(size))

  /** Returns [message] in a gRPC length-prefixed frame. */
  private fun frame(message: ByteArray, gzip: Boolean = false): ByteArray {
    val payload = if (gzip) {
      Buffer().also { compressed ->
        GzipSink(compressed).buffer().use { it.write(message) }
      }.readByteArray()
    } else {
      message
    }
    return Buffer()
      .writeByte(if (gzip) 1 else 0)
      .writeInt(payload.size)
      .write(payload)
      .readByteArray()
  }

  private fun enqueue(body: ByteArray, gzip: Boolean = false) {
    mockWebServer.enqueue(
      MockResponse()
        .setHeader("Content-Type", "application/grpc")
        .setHeader("grpc-encoding", if (gzip) "gzip" else "identity")
        .setTrailers(headersOf("grpc-status", "0"))
        .setBody(Buffer().write(body)),
    )
  }

  private fun Assert<Throwable>.isMessageTooLarge(messageContains: String = "exceeds maximum size") {
    val exception = isInstanceOf<GrpcException>()
    exception.prop(GrpcException::grpcStatus).isEqualTo(GrpcStatus.RESOURCE_EXHAUSTED)
    exception.prop(GrpcException::grpcMessage).isNotNull().contains(messageContains)
  }
}
