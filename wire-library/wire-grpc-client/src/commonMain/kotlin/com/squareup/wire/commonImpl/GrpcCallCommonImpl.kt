package com.squareup.wire.commonImpl

import com.squareup.wire.GrpcCall
import com.squareup.wire.GrpcCall.Callback
import com.squareup.wire.GrpcMethod
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.internal.GrpcMessageReadChannel
import com.squareup.wire.internal.GrpcMessageWriteChannel
import com.squareup.wire.use
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.OutgoingContent.WriteChannelContent
import io.ktor.http.contentType
import io.ktor.util.flattenEntries
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.CoroutineScope
import okio.IOException
import okio.Timeout

class GrpcCallCommonImpl<S: Any, R: Any>(
  private val grpcClient: GrpcClientCommonImpl,
  override val method: GrpcMethod<S, R>
): GrpcCall<S, R> {
  /** Non-null once this is executed. */
  private var httpStatement: HttpStatement? = null
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
    val statement = initCall(request)
    val response = statement.execute()
    responseMetadata = response.headers.flattenEntries().toMap()
    val contentType = response.contentType()
    val statusCode = response.status.value
    if (statusCode != 200 ||
      contentType == null ||
      contentType.contentType != "application" ||
      contentType.contentSubtype != "grpc" && contentType.contentSubtype != "grpc+proto") {
      throw IOException("expected gRPC but was HTTP status=$statusCode, content-type=$contentType")
    }
    val grpcEncoding = response.headers["grpc-encoding"]
    val responseChannel = response.content
    val messageSource = GrpcMessageReadChannel(responseChannel, method.responseAdapter, grpcEncoding)

    return messageSource.read()!!
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

  private fun initCall(request: S): HttpStatement {
    check(this.httpStatement == null) { "already executed" }

    val requestBody = newRequestBody(
      minMessageToCompress = grpcClient.minMessageToCompress,
      requestAdapter = method.requestAdapter,
      onlyMessage = request
    )
    val result = grpcClient.newCall(method, requestMetadata, requestBody)
    this.httpStatement = result
    return result
  }
}

/** Returns a new request body that writes [onlyMessage]. */
internal fun <S : Any> newRequestBody(
  minMessageToCompress: Long,
  requestAdapter: ProtoAdapter<S>,
  onlyMessage: S
): OutgoingContent {
  return object : WriteChannelContent() {
    override val contentType = ContentType.parse("application/grpc")

    override suspend fun writeTo(channel: ByteWriteChannel) {
      val grpcMessageWriteChannel = GrpcMessageWriteChannel(
        channel = channel,
        minMessageToCompress = minMessageToCompress,
        messageAdapter = requestAdapter,
        grpcEncoding = "gzip",
      )
      grpcMessageWriteChannel.write(onlyMessage)
      grpcMessageWriteChannel.close()
    }
  }
}
