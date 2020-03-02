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
package com.squareup.wire.internal

import com.squareup.wire.GrpcCall
import com.squareup.wire.GrpcClient
import com.squareup.wire.GrpcMethod
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okio.Timeout
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class RealGrpcCall<S : Any, R : Any>(
  private val grpcClient: GrpcClient,
  private val grpcMethod: GrpcMethod<S, R>
) : GrpcCall<S, R> {
  /** Non-null until the call is executed. */
  private var call: Call? = null
  private var canceled = false

  override val timeout: Timeout = LateInitTimeout()

  override fun cancel() {
    canceled = true
    call?.cancel()
  }

  override fun isCanceled(): Boolean = canceled

  override suspend fun execute(request: S): R {
    val call = initCall(request)

    return suspendCancellableCoroutine { continuation ->
      continuation.invokeOnCancellation {
        cancel()
      }

      call.enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
          continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
          var message: R? = null
          var exception: IOException? = null
          try {
            message = response.messageSource(grpcMethod.responseAdapter).readExactlyOneAndClose()
            exception = response.grpcStatusToException()
          } catch (e: IOException) {
            exception = e
          }

          if (exception != null) {
            continuation.resumeWithException(exception)
          } else {
            continuation.resume(message!!)
          }
        }
      })
    }
  }

  override fun executeBlocking(request: S): R {
    val call = initCall(request)
    val response = call.execute()
    response.use {
      val message: R = response.messageSource(grpcMethod.responseAdapter).readExactlyOneAndClose()
      val exception: IOException? = response.grpcStatusToException()
      if (exception != null) throw exception
      return message
    }
  }

  override fun enqueue(request: S, callback: GrpcCall.Callback<S, R>) {
    val call = initCall(request)
    call.enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        callback.onFailure(this@RealGrpcCall, e)
      }

      override fun onResponse(call: Call, response: Response) {
        response.use {
          var message: R? = null
          var exception: IOException?
          try {
            message = response.messageSource(grpcMethod.responseAdapter).readExactlyOneAndClose()
            exception = response.grpcStatusToException()
          } catch (e: IOException) {
            exception = e
          }
          if (exception != null) {
            callback.onFailure(this@RealGrpcCall, exception)
          } else {
            callback.onSuccess(this@RealGrpcCall, message!!)
          }
        }
      }
    })
  }

  override fun isExecuted(): Boolean = call?.isExecuted() ?: false

  override fun clone(): GrpcCall<S, R> {
    val result = RealGrpcCall(grpcClient, grpcMethod)
    val oldTimeout = this.timeout
    result.timeout.also { newTimeout ->
      newTimeout.timeout(oldTimeout.timeoutNanos(), TimeUnit.NANOSECONDS)
      if (oldTimeout.hasDeadline()) newTimeout.deadlineNanoTime(oldTimeout.deadlineNanoTime())
    }
    return result
  }

  private fun initCall(request: S): Call {
    check(this.call == null) { "already executed" }

    val requestBody = newRequestBody(grpcMethod.requestAdapter, request)
    val result = grpcClient.newCall(grpcMethod.path, requestBody)
    this.call = result
    if (canceled) result.cancel()
    (timeout as LateInitTimeout).init(result.timeout())
    return result
  }
}
