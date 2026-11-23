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
import assertk.assertions.hasMessage
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import java.util.Locale.US
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.SECONDS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import okio.IOException
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class GrpcCallsTest {
  @Test
  fun execute() {
    val grpcCall = GrpcCall<String, String> { request ->
      request.uppercase(US)
    }
    runBlocking {
      assertThat(grpcCall.execute("hello")).isEqualTo("HELLO")
    }
  }

  @Test
  fun executeBlocking() {
    val grpcCall = GrpcCall<String, String> { request ->
      request.uppercase(US)
    }
    assertThat(grpcCall.executeBlocking("hello")).isEqualTo("HELLO")
  }

  @Test
  fun enqueue() {
    val grpcCall = GrpcCall<String, String> { request ->
      request.uppercase(US)
    }

    val log = LinkedBlockingQueue<String>()
    grpcCall.enqueue(
      "hello",
      object : GrpcCall.Callback<String, String> {
        override fun onFailure(call: GrpcCall<String, String>, exception: IOException) {
          log.add("failure: $exception")
        }

        override fun onSuccess(call: GrpcCall<String, String>, response: String) {
          log.add("success: $response")
        }
      },
    )

    assertThat(log.take()).isEqualTo("success: HELLO")
    assertThat(log).isEmpty()
  }

  @Test
  fun executeThrowsException() {
    val grpcCall = GrpcCall<String, String> { _ ->
      throw Exception("boom!")
    }
    runBlocking {
      try {
        grpcCall.execute("hello")
        fail()
      } catch (e: IOException) {
        assertThat(e).hasMessage("call failed: java.lang.Exception: boom!")
      }
    }
  }

  @Test
  fun executeBlockingThrowsException() {
    val grpcCall = GrpcCall<String, String> { _ ->
      throw Exception("boom!")
    }

    try {
      grpcCall.executeBlocking("hello")
      fail()
    } catch (e: IOException) {
      assertThat(e).hasMessage("call failed: java.lang.Exception: boom!")
    }
  }

  @Test
  fun enqueueThrowsException() {
    val grpcCall = GrpcCall<String, String> { _ ->
      throw Exception("boom!")
    }

    val log = LinkedBlockingQueue<String>()
    grpcCall.enqueue(
      "hello",
      object : GrpcCall.Callback<String, String> {
        override fun onFailure(call: GrpcCall<String, String>, exception: IOException) {
          log.add("failure: $exception")
        }

        override fun onSuccess(call: GrpcCall<String, String>, response: String) {
          log.add("success: $response")
        }
      },
    )

    assertThat(log.take())
      .isEqualTo("failure: java.io.IOException: call failed: java.lang.Exception: boom!")
    assertThat(log).isEmpty()
  }

  @Test
  fun executeAfterExecute() {
    val grpcCall = GrpcCall<String, String> { request ->
      request.uppercase(US)
    }
    assertThat(grpcCall.executeBlocking("hello")).isEqualTo("HELLO")

    runBlocking {
      try {
        grpcCall.execute("hello")
        fail()
      } catch (e: IllegalStateException) {
        assertThat(e).hasMessage("already executed")
      }
    }
  }

  @Test
  fun executeBlockingAfterExecute() {
    val grpcCall = GrpcCall<String, String> { request ->
      request.uppercase(US)
    }
    assertThat(grpcCall.executeBlocking("hello")).isEqualTo("HELLO")

    try {
      grpcCall.executeBlocking("hello")
      fail()
    } catch (e: IllegalStateException) {
      assertThat(e).hasMessage("already executed")
    }
  }

  @Test
  fun enqueueAfterExecute() {
    val grpcCall = GrpcCall<String, String> { request ->
      request.uppercase(US)
    }
    assertThat(grpcCall.executeBlocking("hello")).isEqualTo("HELLO")

    try {
      grpcCall.enqueue(
        "hello",
        object : GrpcCall.Callback<String, String> {
          override fun onFailure(call: GrpcCall<String, String>, exception: IOException) {
            error("unexpected call")
          }

          override fun onSuccess(call: GrpcCall<String, String>, response: String) {
            error("unexpected call")
          }
        },
      )
      fail()
    } catch (e: IllegalStateException) {
      assertThat(e).hasMessage("already executed")
    }
  }

  @Test
  fun executeCanceled() {
    val grpcCall = GrpcCall<String, String> { _ ->
      error("unexpected call")
    }
    grpcCall.cancel()

    runBlocking {
      try {
        grpcCall.execute("hello")
        fail()
      } catch (e: IOException) {
        assertThat(e).hasMessage("canceled")
      }
    }
  }

  @Test
  fun executeBlockingCanceled() {
    val grpcCall = GrpcCall<String, String> { _ ->
      error("unexpected call")
    }
    grpcCall.cancel()

    try {
      grpcCall.executeBlocking("hello")
      fail()
    } catch (e: IOException) {
      assertThat(e).hasMessage("canceled")
    }
  }

  @Test
  fun enqueueCanceled() {
    val grpcCall = GrpcCall<String, String> { _ ->
      error("unexpected call")
    }
    grpcCall.cancel()

    val log = LinkedBlockingQueue<String>()
    grpcCall.enqueue(
      "hello",
      object : GrpcCall.Callback<String, String> {
        override fun onFailure(call: GrpcCall<String, String>, exception: IOException) {
          log.add("failure: $exception")
        }

        override fun onSuccess(call: GrpcCall<String, String>, response: String) {
          log.add("success: $response")
        }
      },
    )

    assertThat(log.take()).isEqualTo("failure: java.io.IOException: canceled")
    assertThat(log).isEmpty()
  }

  @Test
  fun executeInRespectsScope() {
    val threadName = runOnSingleThreadScope { scope, onThread ->
      val grpcCall = GrpcStreamingCall<String, String> { requests, responses ->
        onThread(Thread.currentThread().name)
        requests.receive()
        responses.close()
      }
      val (send, receive) = grpcCall.executeIn(scope)
      runBlocking {
        send.send("hello")
        receive.receiveCatching()
      }
    }

    assertThat(threadName.startsWith("grpc-test-thread")).isTrue()
  }

  @Test
  fun clientStreamingExecuteInRespectsScope() {
    val threadName = runOnSingleThreadScope { scope, onThread ->
      val grpcCall = GrpcClientStreamingCall<String, String> {
        onThread(Thread.currentThread().name)
        receive()
      }
      val (send, response) = grpcCall.executeIn(scope)
      runBlocking {
        send.send("hello")
        response.await()
      }
    }

    assertThat(threadName.startsWith("grpc-test-thread")).isTrue()
  }

  @Test
  fun serverStreamingExecuteInRespectsScope() {
    val threadName = runOnSingleThreadScope { scope, onThread ->
      val grpcCall = GrpcServerStreamingCall<String, String> {
        onThread(Thread.currentThread().name)
        close()
      }
      runBlocking {
        grpcCall.executeIn(scope, "hello").receiveCatching()
      }
    }

    assertThat(threadName.startsWith("grpc-test-thread")).isTrue()
  }

  @Test
  fun executeBlockingRunsThreadBlockingFunctionsOnIoDispatcher() {
    assertRunsThreadBlockingFunctionsOnIoDispatcher { grpcCall ->
      val responses = grpcCall.executeBlocking().second
      return@assertRunsThreadBlockingFunctionsOnIoDispatcher { responses.read() }
    }
  }

  @Test
  fun executeInScopeWithoutDispatcherRunsThreadBlockingFunctionsOnIoDispatcher() {
    assertRunsThreadBlockingFunctionsOnIoDispatcher { grpcCall ->
      @Suppress("OPT_IN_USAGE")
      val responses = grpcCall.executeIn(GlobalScope).second
      return@assertRunsThreadBlockingFunctionsOnIoDispatcher { runBlocking { responses.receive() } }
    }
  }

  @Test
  fun executeRunsThreadBlockingFunctionsOnIoDispatcher() {
    assertRunsThreadBlockingFunctionsOnIoDispatcher { grpcCall ->
      @Suppress("DEPRECATION")
      val responses = grpcCall.execute().second
      return@assertRunsThreadBlockingFunctionsOnIoDispatcher { runBlocking { responses.receive() } }
    }
  }

  /** Runs [block] with a single-thread scope, and returns the thread name that [block] reports. */
  private fun runOnSingleThreadScope(block: (CoroutineScope, (String) -> Unit) -> Unit): String {
    val dispatcher = newSingleThreadContext("grpc-test-thread")
    try {
      var threadName: String? = null
      block(CoroutineScope(dispatcher)) { threadName = it }
      return threadName!!
    } finally {
      dispatcher.close()
    }
  }

  /**
   * Dispatchers.Default has about one thread per core, and Dispatchers.IO has at least 64.
   * This starts more blocking functions than Default has threads, and each one waits for all to start.
   * [start] executes the call and returns a function that reads one response.
   */
  private fun assertRunsThreadBlockingFunctionsOnIoDispatcher(
    start: (GrpcStreamingCall<String, String>) -> () -> String?,
  ) {
    val callCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(2) + 1
    assumeTrue(callCount <= 64)
    val started = CountDownLatch(callCount)

    val reads = List(callCount) {
      val grpcCall = GrpcStreamingCall<String, String> { _, responses ->
        started.countDown()
        val allStarted = started.await(5, SECONDS)
        responses.send(if (allStarted) "all started" else "starved")
        responses.close()
      }
      start(grpcCall)
    }

    for (read in reads) {
      assertThat(read()).isEqualTo("all started")
    }
  }

  @Test
  fun cloneIsIndependent() {
    val grpcCall = GrpcCall<String, String> { it.uppercase(US) }
    val requestMetadata = mutableMapOf("1" to "one")
    grpcCall.requestMetadata = requestMetadata
    assertThat(grpcCall.executeBlocking("hello")).isEqualTo("HELLO")
    grpcCall.cancel()

    val clonedGrpcCall = grpcCall.clone()
    requestMetadata["2"] = "two"
    assertThat(clonedGrpcCall.requestMetadata).isEqualTo(mapOf("1" to "one"))
    assertThat(clonedGrpcCall.executeBlocking("world")).isEqualTo("WORLD")
  }

  @Test
  fun propagatesGrpcExceptionsWithoutWrappingIOException() {
    val grpcCall = GrpcCall<String, String> { _ ->
      throw GrpcException(GrpcStatus.INTERNAL, "oops")
    }

    try {
      grpcCall.executeBlocking("hello")
      fail()
    } catch (e: GrpcException) {
      assertThat(e).hasMessage("grpc-status=13 grpc-status-name=INTERNAL grpc-message=oops")
    }
  }
}
