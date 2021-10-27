package com.squareup.wire.commonImpl

import com.squareup.wire.GrpcCall
import com.squareup.wire.GrpcClient
import com.squareup.wire.GrpcMethod
import com.squareup.wire.GrpcStreamingCall
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.request.request

class GrpcClientCommonImpl private constructor(
  internal val ktorClient: HttpClient
): GrpcClient {

  init {
    ktorClient.request<> {  }
  }

  override fun <S : Any, R : Any> newCall(method: GrpcMethod<S, R>): GrpcCall<S, R> {
    throw UnsupportedOperationException("common wire-grpc-client doesn't support calls yet.")
  }

  override fun <S : Any, R : Any> newStreamingCall(method: GrpcMethod<S, R>): GrpcStreamingCall<S, R> {
    throw UnsupportedOperationException("common wire-grpc-client doesn't support streaming yet.")
  }


}