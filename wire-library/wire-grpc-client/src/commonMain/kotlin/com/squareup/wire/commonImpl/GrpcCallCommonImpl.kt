package com.squareup.wire.commonImpl

import com.squareup.wire.GrpcCall
import com.squareup.wire.GrpcCall.Callback
import com.squareup.wire.GrpcClient
import com.squareup.wire.GrpcMethod
import okio.Timeout

class GrpcCallCommonImpl<S: Any, R: Any>(
  private val grpcClient: GrpcClientCommonImpl,
  override val method: GrpcMethod<S, R>
): GrpcCall<S, R> {
  private var canceled = false

  override val timeout: Timeout = Timeout.NONE

  override var requestMetadata: Map<String, String> = mapOf()
  override var responseMetadata: Map<String, String>? = null
    private set

  override fun cancel() {
    canceled = true
  }

  override fun isCanceled(): Boolean = canceled

  override suspend fun execute(request: S): R {
    TODO("Not yet implemented")
  }

  override fun executeBlocking(request: S): R {
    TODO("Not yet implemented")
  }

  override fun enqueue(request: S, callback: Callback<S, R>) {
    TODO("Not yet implemented")
  }

  override fun isExecuted(): Boolean {
    TODO("Not yet implemented")
  }

  override fun clone(): GrpcCall<S, R> {
    TODO("Not yet implemented")
  }
}