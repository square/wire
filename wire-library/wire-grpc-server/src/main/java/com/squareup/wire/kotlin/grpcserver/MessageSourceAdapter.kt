/*
 * Copyright (C) 2021 Square, Inc.
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
package com.squareup.wire.kotlin.grpcserver

import com.google.common.util.concurrent.Monitor
import com.squareup.wire.MessageSource
import io.grpc.stub.StreamObserver

// This is for adapting Google style grpc stubs to Wire style grpc stubs.
@Suppress("UnstableApiUsage")
class MessageSourceAdapter<T : Any> : MessageSource<T>, StreamObserver<T> {
  private var value: T? = null
  private var error: Throwable? = null
  private var completed = false
  private val monitor = Monitor()
  private val valuePresent = monitor.newGuard { value != null || error != null || completed }
  private val valueAbsent = monitor.newGuard { value == null && error == null && !completed }

  override fun onNext(next: T) {
    monitor.enterIf(valueAbsent)
    value = next
    monitor.leave()
  }

  override fun onError(t: Throwable) {
    monitor.enterIf(valueAbsent)
    error = t
    monitor.leave()
  }

  override fun onCompleted() {
    monitor.enterIf(valueAbsent)
    completed = true
    monitor.leave()
  }

  override fun close() {
    throw RuntimeException("client streams cannot be closed by the server")
  }

  override fun read(): T? {
    monitor.enterIf(valuePresent)
    return try {
      when {
        completed -> { null }
        error != null -> { throw error!! }
        else -> { value }
      }
    } finally {
      value = null
      error = null
      monitor.leave()
    }
  }
}
