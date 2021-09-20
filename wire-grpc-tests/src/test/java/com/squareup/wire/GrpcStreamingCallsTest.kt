/*
 * Copyright 2020 Square Inc.
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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.runBlocking
import okio.IOException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Test

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class GrpcStreamingCallsTest {
  @Test
  fun execute() {
    val grpcCall = GrpcStreamingCall<String, String> { requests, responses ->
      requests.consumeEach { responses.send(it.toUpperCase()) }
      responses.close()
    }
    runBlocking {
      val (requests, responses) = grpcCall.executeIn(this)
      requests.send("hello")
      requests.close()
      assertThat(responses.receive()).isEqualTo("HELLO")
      assertThat(responses.receiveCatching().getOrNull()).isNull()
    }
  }

  @Test
  fun executeBlocking() {
    val grpcCall = GrpcStreamingCall<String, String> { requests, responses ->
      requests.consumeEach { responses.send(it.toUpperCase()) }
      responses.close()
    }
    val (requests, responses) = grpcCall.executeBlocking()
    requests.write("hello")
    requests.close()
    assertThat(responses.read()).isEqualTo("HELLO")
    assertThat(responses.read()).isNull()
  }

  @Test
  fun executeThrowsException() {
    val grpcCall = GrpcStreamingCall<String, String> { requests, responses ->
      throw Exception("boom!")
    }
    runBlocking {
      val (requests, responses) = grpcCall.executeIn(this)
      try {
        responses.receive()
        fail()
      } catch (e: Exception) {
        assertThat(e).hasMessage("boom!")
      }
      try {
        requests.send("hello")
        fail()
      } catch (e: Exception) {
        assertThat(e).hasMessage("boom!")
      }
    }
  }

  @Test
  fun executeBlockingThrowsException() {
    val grpcCall = GrpcStreamingCall<String, String> { requests, responses ->
      throw Exception("boom!")
    }

    val (requests, responses) = grpcCall.executeBlocking()
    try {
      responses.read()
      fail()
    } catch (e: IOException) {
      assertThat(e).hasMessage("java.lang.Exception: boom!")
    }
    try {
      requests.write("hello")
      fail()
    } catch (e: IOException) {
      assertThat(e).hasMessage("java.lang.Exception: boom!")
    }
  }

  @Test
  fun executeAfterExecute() {
    val grpcCall = GrpcStreamingCall<String, String> { requests, responses ->
      requests.consumeEach { responses.send(it.toUpperCase()) }
      responses.close()
    }

    runBlocking {
      val (requests, responses) = grpcCall.executeIn(this)
      requests.close()
      responses.cancel()

      try {
        grpcCall.executeIn(this)
        fail()
      } catch (e: IllegalStateException) {
        assertThat(e).hasMessage("already executed")
      }
    }
  }

  @Test
  fun executeBlockingAfterExecute() {
    val grpcCall = GrpcStreamingCall<String, String> { requests, responses ->
      requests.consumeEach { responses.send(it.toUpperCase()) }
      responses.close()
    }

    runBlocking {
      val (requests, responses) = grpcCall.executeIn(this)
      requests.close()
      responses.cancel()
    }

    try {
      grpcCall.executeBlocking()
      fail()
    } catch (e: IllegalStateException) {
      assertThat(e).hasMessage("already executed")
    }
  }

  @Test
  fun executeCanceled() {
    val grpcCall = GrpcStreamingCall<String, String> { requests, responses ->
      error("unexpected call")
    }
    grpcCall.cancel()

    runBlocking {
      val (requests, responses) = grpcCall.executeIn(this)
      try {
        responses.receive()
        fail()
      } catch (e: CancellationException) {
      }
      try {
        requests.send("hello")
        fail()
      } catch (e: CancellationException) {
      }
    }
  }

  @Test
  fun executeBlockingCanceled() {
    val grpcCall = GrpcStreamingCall<String, String> { requests, responses ->
      error("unexpected call")
    }
    grpcCall.cancel()

    val (requests, responses) = grpcCall.executeBlocking()
    try {
      responses.read()
      fail()
    } catch (e: IOException) {
    }
    try {
      requests.write("hello")
      fail()
    } catch (e: IOException) {
    }
  }

  @Test
  fun cloneIsIndependent() {
    val grpcCall = GrpcStreamingCall<String, String> { requests, responses ->
      requests.consumeEach { responses.send(it.toUpperCase()) }
      responses.close()
    }

    runBlocking {
      val (requests1, responses1) = grpcCall.executeIn(this)
      requests1.close()
      responses1.cancel()

      val (requests2, responses2) = grpcCall.clone().executeIn(this)
      requests2.send("hello")
      requests2.close()
      assertThat(responses2.receive()).isEqualTo("HELLO")
      assertThat(responses2.receiveCatching().getOrNull()).isNull()
    }
  }

  /**
   * Confirm that if the scope that we called `execute()` with is canceled, the channels are
   * canceled also.
   */
  @Test
  fun executeIsScoped() {
    val grpcCall = GrpcStreamingCall<String, String> { requests, responses ->
      requests.consumeEach { responses.send(it.toUpperCase()) }
      responses.close()
    }

    var requests: SendChannel<String>? = null
    var responses: ReceiveChannel<String>? = null
    try {
      runBlocking {
        val execute = grpcCall.executeIn(this)
        requests = execute.first
        responses = execute.second
        cancel()
      }
      fail()
    } catch (expected: CancellationException) {
    }

    try {
      runBlocking {
        requests!!.send("hello")
      }
      fail()
    } catch (expected: CancellationException) {
    }

    try {
      runBlocking {
        responses!!.receive()
      }
      fail()
    } catch (expected: CancellationException) {
    }
  }
}
