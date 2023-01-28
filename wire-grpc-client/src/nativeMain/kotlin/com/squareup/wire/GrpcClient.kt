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

actual class GrpcClient {
  actual fun <S : Any, R : Any> newCall(method: GrpcMethod<S, R>): GrpcCall<S, R> {
    throw UnsupportedOperationException("wire-grpc-client doesn't support Native yet.")
  }

  actual fun <S : Any, R : Any> newStreamingCall(
    method: GrpcMethod<S, R>
  ): GrpcStreamingCall<S, R> {
    throw UnsupportedOperationException("wire-grpc-client doesn't support Native yet.")
  }
}
