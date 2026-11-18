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
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.prop
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import okhttp3.Headers.Companion.headersOf
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.IOException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import routeguide.Feature
import routeguide.Point

/**
 * Proves that every kind of call completes with an [IOException] when the response is unusable,
 * and that it never hangs. A hung call would also kill an Android app, because OkHttp rethrows an
 * unchecked exception on its dispatcher thread.
 */
@Suppress("OPT_IN_USAGE")
class GrpcResponseFailureTest {
  @JvmField @Rule
  val mockWebServer = MockWebServer()

  @JvmField @Rule
  val testTimeout = Timeout(30, TimeUnit.SECONDS)

  private lateinit var grpcClient: GrpcClient

  /** A method that fails to decode like a generated adapter does when a required field is absent. */
  private val undecodableMethod = GrpcMethod(
    path = "/routeguide.RouteGuide/GetFeature",
    requestAdapter = Point.ADAPTER,
    responseAdapter = object : ProtoAdapter<Feature>(
      FieldEncoding.LENGTH_DELIMITED,
      Feature::class,
      null,
      Syntax.PROTO_2,
    ) {
      override fun redact(value: Feature) = value
      override fun encodedSize(value: Feature) = Feature.ADAPTER.encodedSize(value)
      override fun encode(writer: ProtoWriter, value: Feature) = Feature.ADAPTER.encode(writer, value)
      override fun decode(reader: ProtoReader): Feature = throw IllegalStateException("Required field not set")
    },
  )

  private val method = GrpcMethod(
    path = "/routeguide.RouteGuide/GetFeature",
    requestAdapter = Point.ADAPTER,
    responseAdapter = Feature.ADAPTER,
  )

  @Before
  fun setUp() {
    mockWebServer.protocols = listOf(Protocol.H2_PRIOR_KNOWLEDGE)
    val okhttpClient = OkHttpClient.Builder()
      .protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE))
      .build()
    grpcClient = GrpcClient.Builder()
      .client(okhttpClient)
      .baseUrl(mockWebServer.url("/"))
      .build()
  }

  @Test
  fun negativeGrpcStatusBlocking() {
    enqueue(grpcStatus = "-1")
    assertFailure {
      grpcClient.newCall(method).executeBlocking(Point())
    }.isUnknownStatus(-1)
  }

  @Test
  fun negativeGrpcStatusSuspend() {
    enqueue(grpcStatus = "-1")
    assertFailure {
      runBlocking { grpcClient.newCall(method).execute(Point()) }
    }.isUnknownStatus(-1)
  }

  @Test
  fun negativeGrpcStatusCallback() {
    enqueue(grpcStatus = "-1")
    assertFailure {
      awaitCallback(grpcClient.newCall(method))
    }.isUnknownStatus(-1)
  }

  @Test
  fun negativeGrpcStatusStreamingBlocking() {
    enqueue(grpcStatus = "-1")
    val (sink, source) = grpcClient.newStreamingCall(method).executeBlocking()
    sink.close()
    assertThat(source.read()).isEqualTo(Feature())
    assertFailure { source.read() }.isUnknownStatus(-1)
  }

  @Test
  fun negativeGrpcStatusStreamingSuspend() {
    enqueue(grpcStatus = "-1")
    val (requests, responses) = grpcClient.newStreamingCall(method).executeIn(GlobalScope)
    requests.close()
    runBlocking {
      assertThat(responses.receive()).isEqualTo(Feature())
      assertFailure { responses.receive() }.isUnknownStatus(-1)
    }
  }

  @Test
  fun negativeGrpcStatusServerStreamingBlocking() {
    enqueue(grpcStatus = "-1")
    val source = grpcClient.newServerStreamingCall(method).executeBlocking(Point())
    assertThat(source.read()).isEqualTo(Feature())
    assertFailure { source.read() }.isUnknownStatus(-1)
  }

  @Test
  fun grpcStatusAcceptsAnyCode() {
    assertThat(GrpcStatus.get(-1).code).isEqualTo(-1)
    assertThat(GrpcStatus.get(Int.MIN_VALUE).code).isEqualTo(Int.MIN_VALUE)
    assertThat(GrpcStatus.get(99).name).isEqualTo("STATUS_99")
  }

  @Test
  fun undecodableResponseBlocking() {
    enqueue(grpcStatus = "0")
    assertFailure {
      grpcClient.newCall(undecodableMethod).executeBlocking(Point())
    }.isDecodeFailure()
  }

  @Test
  fun undecodableResponseSuspend() {
    enqueue(grpcStatus = "0")
    assertFailure {
      runBlocking { grpcClient.newCall(undecodableMethod).execute(Point()) }
    }.isDecodeFailure()
  }

  @Test
  fun undecodableResponseCallback() {
    enqueue(grpcStatus = "0")
    assertFailure {
      awaitCallback(grpcClient.newCall(undecodableMethod))
    }.isDecodeFailure()
  }

  @Test
  fun undecodableResponseStreamingBlocking() {
    enqueue(grpcStatus = "0")
    val (sink, source) = grpcClient.newStreamingCall(undecodableMethod).executeBlocking()
    sink.close()
    assertFailure { source.read() }.isDecodeFailure()
  }

  @Test
  fun undecodableResponseStreamingSuspend() {
    enqueue(grpcStatus = "0")
    val (requests, responses) = grpcClient.newStreamingCall(undecodableMethod).executeIn(GlobalScope)
    requests.close()
    assertFailure {
      runBlocking { responses.receive() }
    }.isDecodeFailure()
  }

  @Test
  fun undecodableResponseClientStreamingBlocking() {
    enqueue(grpcStatus = "0")
    val (sink, response) = grpcClient.newClientStreamingCall(undecodableMethod).executeBlocking()
    sink.close()
    assertFailure { response.get() }.isDecodeFailure()
  }

  /** Enqueues one empty message followed by the trailers. */
  private fun enqueue(grpcStatus: String) {
    mockWebServer.enqueue(
      MockResponse()
        .setHeader("Content-Type", "application/grpc")
        .setBody(Buffer().writeByte(0).writeInt(0))
        .setTrailers(headersOf("grpc-status", grpcStatus, "grpc-message", "nope")),
    )
  }

  private fun awaitCallback(call: GrpcCall<Point, Feature>): Feature {
    val result = CompletableFuture<Feature>()
    call.enqueue(
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
    try {
      return result.get(10, TimeUnit.SECONDS)
    } catch (e: java.util.concurrent.ExecutionException) {
      throw e.cause!!
    }
  }

  private fun Assert<Throwable>.isUnknownStatus(code: Int) {
    isInstanceOf<GrpcException>()
      .prop(GrpcException::grpcStatus)
      .prop(GrpcStatus::code)
      .isEqualTo(code)
  }

  private fun Assert<Throwable>.isDecodeFailure() {
    isInstanceOf<IOException>()
    isNotInstanceOf<GrpcException>()
    transform("causes") { generateSequence<Throwable>(it) { e -> e.cause }.toList() }
      .transform("has IllegalStateException") { causes -> causes.any { it is IllegalStateException } }
      .isTrue()
  }
}
