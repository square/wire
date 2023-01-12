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

import com.squareup.wire.MessageSink
import io.grpc.stub.StreamObserver

// This is for adapting Google style grpc stubs to Wire style grpc stubs.
class MessageSinkAdapter<T : Any> constructor(private val responseObserver: StreamObserver<T>) :
  MessageSink<T> {
  override fun cancel() {
    TODO("not implemented")
  }

  override fun close() {
    responseObserver.onCompleted()
  }

  override fun write(message: T) {
    responseObserver.onNext(message)
  }
}
