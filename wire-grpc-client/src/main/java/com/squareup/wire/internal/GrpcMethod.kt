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
import com.squareup.wire.GrpcStreamingCall
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.WireRpc
import java.lang.reflect.Method

internal abstract class GrpcMethod<S : Any, R : Any>(
  val path: String,
  val requestAdapter: ProtoAdapter<S>,
  val responseAdapter: ProtoAdapter<R>
) {
  /** Handle a dynamic proxy method call to this. */
  abstract fun invoke(grpcClient: GrpcClient, args: Array<Any>): Any

  internal companion object {
    internal fun <S : Any, R : Any> Method.toGrpc(): GrpcMethod<S, R> {
      val wireRpc = getAnnotation(WireRpc::class.java)
      val requestAdapter = ProtoAdapter.get(wireRpc.requestAdapter) as ProtoAdapter<S>
      val responseAdapter = ProtoAdapter.get(wireRpc.responseAdapter) as ProtoAdapter<R>
      val returnType = genericReturnType

      if (returnType.rawType() == GrpcCall::class.java) {
        return object : GrpcMethod<S, R>(wireRpc.path, requestAdapter, responseAdapter) {
          override fun invoke(
            grpcClient: GrpcClient,
            args: Array<Any>
          ) = RealGrpcCall(grpcClient, this)
        }

      } else if (returnType.rawType() == GrpcStreamingCall::class.java) {
        return object : GrpcMethod<S, R>(wireRpc.path, requestAdapter, responseAdapter) {
          override fun invoke(
            grpcClient: GrpcClient,
            args: Array<Any>
          ) = RealGrpcStreamingCall(grpcClient, this)
        }
      }

      error("unexpected gRPC method: $this")
    }
  }
}
