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

import kotlin.coroutines.cancellation.CancellationException
import okio.IOException
import okio.Timeout

/**
 * A single call to a remote server. This call sends a single request value and receives a single
 * response value. A gRPC call cannot be executed twice.
 *
 * gRPC calls can be [suspending][execute], [blocking][executeBlocking], or
 * [asynchronous][enqueue]. Use whichever mechanism works at your call site: the bytes transmitted
 * on the network are the same.
 */
interface GrpcCall<S : Any, R : Any> {
  /** The method invoked by this call. */
  val method: GrpcMethod<S, R>

  /** Configures how long the call can take to complete before it is automatically canceled. */
  val timeout: Timeout

  /**
   * A map containing request metadata. This is initially empty; it can be assigned to any other map
   * of metadata before the call is executed. It is an error to set this value after the call is
   * executed.
   */
  var requestMetadata: Map<String, String>

  /**
   * A map containing response metadata. This is null until the call has executed, at which point
   * it will be non-null if the call completed successfully. It may also be non-null in failure
   * cases if the failure was not a problem of connectivity. For example, if the gRPC call fails
   * with an HTTP 503 error, response metadata will be present.
   */
  val responseMetadata: Map<String, String>?

  /**
   * Attempts to cancel the call. This function is safe to call concurrently with execution. When
   * canceled, execution fails with an immediate [IOException] rather than waiting to complete
   * normally.
   */
  fun cancel()

  /** True if [cancel] was called. */
  fun isCanceled(): Boolean

  /** Invokes the call immediately and suspends until its response is received. */
  @Throws(IOException::class, CancellationException::class)
  suspend fun execute(request: S): R

  /** Invokes the call immediately and blocks until its response is received. */
  @Throws(IOException::class)
  fun executeBlocking(request: S): R

  /**
   * Enqueues this call for asynchronous execution. The [callback] will be invoked on the client's
   * dispatcher thread when the call completes.
   */
  fun enqueue(request: S, callback: Callback<S, R>)

  /**
   * Returns true if [execute], [executeBlocking], or [enqueue] was called. It is an error to
   * execute or enqueue a call more than once.
   */
  fun isExecuted(): Boolean

  /**
   * Create a new, identical gRPC call to this one which can be enqueued or executed even if this
   * call has already been.
   */
  fun clone(): GrpcCall<S, R>

  interface Callback<S : Any, R : Any> {
    fun onFailure(call: GrpcCall<S, R>, exception: IOException)
    fun onSuccess(call: GrpcCall<S, R>, response: R)
  }
}
