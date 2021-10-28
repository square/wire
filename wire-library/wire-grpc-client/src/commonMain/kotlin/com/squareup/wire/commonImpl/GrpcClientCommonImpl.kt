package com.squareup.wire.commonImpl

import com.squareup.wire.GrpcCall
import com.squareup.wire.GrpcClient
import com.squareup.wire.GrpcMethod
import com.squareup.wire.GrpcStreamingCall
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.call
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.HttpStatement
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.takeFrom
import io.ktor.util.AttributeKey

class GrpcClientCommonImpl private constructor(
  internal val ktorClient: HttpClient,
  internal val baseUrlBuilder: URLBuilder,
  internal val minMessageToCompress: Long
) : GrpcClient {

  override fun <S : Any, R : Any> newCall(method: GrpcMethod<S, R>): GrpcCall<S, R> {
    return GrpcCallCommonImpl(this, method)
  }

  override fun <S : Any, R : Any> newStreamingCall(method: GrpcMethod<S, R>): GrpcStreamingCall<S, R> {
    throw UnsupportedOperationException("common wire-grpc-client doesn't support streaming yet.")
  }

  internal fun newCall(
    method: GrpcMethod<*, *>,
    requestMetaData: Map<String, String>,
    requestData: OutgoingContent
  ): HttpStatement =
    HttpStatement(HttpRequestBuilder().apply {
      url(baseUrlBuilder.takeFrom(method.path).build())
      header("te", "trailers")
      header("grpc-trace-bin", "")
      header("grpc-accept-encoding", "gzip")
      if (minMessageToCompress < Long.MAX_VALUE) {
        header("grpc-encoding", "gzip")
      }
      for ((key, value) in requestMetaData) {
        header(key, value)
      }
      this.method = HttpMethod.Post
      this.body = requestData
      setAttributes {
        this.put(AttributeKey<GrpcMethod<*, *>>("tag"), method)
      }
    }, ktorClient)
}