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
package com.squareup.wire.internal

import com.squareup.wire.GrpcCall
import com.squareup.wire.GrpcMethod
import com.squareup.wire.WireGrpcClient
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.ForwardingTimeout
import okio.IOException
import okio.Timeout

internal class RealGrpcCall<S : Any, R : Any>(
  private val grpcClient: WireGrpcClient,
  override val method: GrpcMethod<S, R>,
) : GrpcCall<S, R> {
  /** Non-null once this is executed. */
  private var call: okhttp3.Call? = null
  private var canceled = false

  override val timeout: Timeout = ForwardingTimeout(Timeout())

  override var requestMetadata: Map<String, String> = mapOf()

  override var responseMetadata: Map<String, String>? = null
    private set

  override fun cancel() {
    canceled = true
    call?.cancel()
  }

  override fun isCanceled(): Boolean = canceled || call?.isCanceled() == true

  override suspend fun execute(request: S): R {
    val call = initCall(request)

    return suspendCancellableCoroutine { continuation ->
      continuation.invokeOnCancellation {
        cancel()
      }

      call.enqueue(object : okhttp3.Callback {
        override fun onFailure(call: okhttp3.Call, e: IOException) {
          continuation.resumeWithException(e)
        }

        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
          try {
            responseMetadata = response.headers.toMap()
            val message = response.readExactlyOneAndClose()
            continuation.resume(message)
          } catch (e: IOException) {
            continuation.resumeWithException(e)
          }
        }
      })
    }
  }

  override fun executeBlocking(request: S): R {
    val call = initCall(request)
    val response = call.execute()
    responseMetadata = response.headers.toMap()
    return response.readExactlyOneAndClose()
  }

  override fun enqueue(request: S, callback: GrpcCall.Callback<S, R>) {
    val call = initCall(request)
    call.enqueue(object : okhttp3.Callback {
      override fun onFailure(call: okhttp3.Call, e: IOException) {
        callback.onFailure(this@RealGrpcCall, e)
      }

      override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
        try {
          responseMetadata = response.headers.toMap()
          val message = response.readExactlyOneAndClose()
          callback.onSuccess(this@RealGrpcCall, message)
        } catch (e: IOException) {
          callback.onFailure(this@RealGrpcCall, e)
        }
      }
    })
  }

  private fun okhttp3.Response.readExactlyOneAndClose(): R {
    use {
      messageSource(method.responseAdapter).use { reader ->
        val result = try {
          reader.readExactlyOneAndClose()
        } catch (e: IOException) {
          throw grpcResponseToException(e)!!
        }
        val exception = grpcResponseToException()
        if (exception != null) throw exception
        return result
      }
    }
  }

  override fun isExecuted(): Boolean = call?.isExecuted() ?: false

  override fun clone(): GrpcCall<S, R> {
    val result = RealGrpcCall(grpcClient, method)
    val oldTimeout = this.timeout
    result.timeout.also { newTimeout ->
      newTimeout.timeout(oldTimeout.timeoutNanos(), TimeUnit.NANOSECONDS)
      if (oldTimeout.hasDeadline()) newTimeout.deadlineNanoTime(oldTimeout.deadlineNanoTime())
    }
    result.requestMetadata += this.requestMetadata
    return result
  }

  private fun initCall(request: S): okhttp3.Call {
    check(this.call == null) { "already executed" }

    val requestBody = newRequestBody(
      minMessageToCompress = grpcClient.minMessageToCompress,
      requestAdapter = method.requestAdapter,
      onlyMessage = request,
    )
    val result = grpcClient.newCall(method, requestMetadata, requestBody, timeout)
    this.call = result
    if (canceled) result.cancel()
    // If the timeout doesn't have a deadline or timeout, then the user
    // didn't set the timeout on this Call manually.
    if (!timeout.hasDeadline() && (timeout.timeoutNanos() == 0L)) {
      (timeout as ForwardingTimeout).setDelegate(result.timeout())
    }
    return result
  }
}
