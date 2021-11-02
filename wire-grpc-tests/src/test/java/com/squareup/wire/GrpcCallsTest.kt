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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
import okio.IOException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class GrpcCallsTest {
  @Test
  fun execute() {
    val grpcCall = GrpcCall<String, String> { request ->
      request.toUpperCase()
    }
    runBlocking {
      assertThat(grpcCall.execute("hello")).isEqualTo("HELLO")
    }
  }

  @Test
  fun executeBlocking() {
    val grpcCall = GrpcCall<String, String> { request ->
      request.toUpperCase()
    }
    assertThat(grpcCall.executeBlocking("hello")).isEqualTo("HELLO")
  }

  @Test
  fun enqueue() {
    val grpcCall = GrpcCall<String, String> { request ->
      request.toUpperCase()
    }

    val log = LinkedBlockingQueue<String>()
    grpcCall.enqueue("hello", object:  GrpcCall.Callback<String, String> {
      override fun onFailure(call: GrpcCall<String, String>, exception: IOException) {
        log.add("failure: $exception")
      }

      override fun onSuccess(call: GrpcCall<String, String>, response: String) {
        log.add("success: $response")
      }
    })

    assertThat(log.take()).isEqualTo("success: HELLO")
    assertThat(log).isEmpty()
  }

  @Test
  fun executeThrowsException() {
    val grpcCall = GrpcCall<String, String> { request ->
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
    val grpcCall = GrpcCall<String, String> { request ->
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
    val grpcCall = GrpcCall<String, String> { request ->
      throw Exception("boom!")
    }

    val log = LinkedBlockingQueue<String>()
    grpcCall.enqueue("hello", object:  GrpcCall.Callback<String, String> {
      override fun onFailure(call: GrpcCall<String, String>, exception: IOException) {
        log.add("failure: $exception")
      }

      override fun onSuccess(call: GrpcCall<String, String>, response: String) {
        log.add("success: $response")
      }
    })

    assertThat(log.take())
        .isEqualTo("failure: java.io.IOException: call failed: java.lang.Exception: boom!")
    assertThat(log).isEmpty()
  }

  @Test
  fun executeAfterExecute() {
    val grpcCall = GrpcCall<String, String> { request ->
      request.toUpperCase()
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
      request.toUpperCase()
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
      request.toUpperCase()
    }
    assertThat(grpcCall.executeBlocking("hello")).isEqualTo("HELLO")

    try {
      grpcCall.enqueue("hello", object:  GrpcCall.Callback<String, String> {
        override fun onFailure(call: GrpcCall<String, String>, exception: IOException) {
          error("unexpected call")
        }

        override fun onSuccess(call: GrpcCall<String, String>, response: String) {
          error("unexpected call")
        }
      })
      fail()
    } catch (e: IllegalStateException) {
      assertThat(e).hasMessage("already executed")
    }
  }

  @Test
  fun executeCanceled() {
    val grpcCall = GrpcCall<String, String> { request ->
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
    val grpcCall = GrpcCall<String, String> { request ->
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
    val grpcCall = GrpcCall<String, String> { request ->
      error("unexpected call")
    }
    grpcCall.cancel()

    val log = LinkedBlockingQueue<String>()
    grpcCall.enqueue("hello", object:  GrpcCall.Callback<String, String> {
      override fun onFailure(call: GrpcCall<String, String>, exception: IOException) {
        log.add("failure: $exception")
      }

      override fun onSuccess(call: GrpcCall<String, String>, response: String) {
        log.add("success: $response")
      }
    })

    assertThat(log.take()).isEqualTo("failure: java.io.IOException: canceled")
    assertThat(log).isEmpty()
  }

  @Test
  fun cloneIsIndependent() {
    val grpcCall = GrpcCall(String::toUpperCase)
    val requestMetadata = mutableMapOf("1" to "one")
    grpcCall.requestMetadata = requestMetadata
    assertThat(grpcCall.executeBlocking("hello")).isEqualTo("HELLO")
    grpcCall.cancel()

    val clonedGrpcCall = grpcCall.clone()
    requestMetadata["2"] = "two"
    assertThat(clonedGrpcCall.requestMetadata).isEqualTo(mapOf("1" to "one"))
    assertThat(clonedGrpcCall.executeBlocking("world")).isEqualTo("WORLD")
  }
}
