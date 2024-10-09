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
package com.squareup.wire.internal

import com.squareup.wire.GrpcException
import com.squareup.wire.GrpcStatus
import com.squareup.wire.ProtoAdapter
import java.io.Closeable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers.Companion.headersOf
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import okio.IOException

internal val APPLICATION_GRPC_MEDIA_TYPE: MediaType = "application/grpc".toMediaType()

/** Returns a new request body that writes [onlyMessage]. */
internal fun <S : Any> newRequestBody(
  minMessageToCompress: Long,
  requestAdapter: ProtoAdapter<S>,
  onlyMessage: S,
): RequestBody {
  return object : RequestBody() {
    override fun contentType() = APPLICATION_GRPC_MEDIA_TYPE

    override fun writeTo(sink: BufferedSink) {
      val grpcMessageSink = GrpcMessageSink(
        sink = sink,
        minMessageToCompress = minMessageToCompress,
        messageAdapter = requestAdapter,
        callForCancel = null,
        grpcEncoding = "gzip",
      )
      grpcMessageSink.use {
        it.write(onlyMessage)
      }
    }
  }
}

/**
 * Returns a new duplex request body that allows us to write request messages even after the
 * response status, headers, and body have been received.
 */
internal fun newDuplexRequestBody(): PipeDuplexRequestBody {
  return PipeDuplexRequestBody(APPLICATION_GRPC_MEDIA_TYPE, pipeMaxBufferSize = 1024 * 1024)
}

/** Writes messages to the request body. */
internal fun <S : Any> PipeDuplexRequestBody.messageSink(
  minMessageToCompress: Long,
  requestAdapter: ProtoAdapter<S>,
  callForCancel: Call,
) = GrpcMessageSink(
  sink = createSink(),
  minMessageToCompress = minMessageToCompress,
  messageAdapter = requestAdapter,
  callForCancel = callForCancel.toWireCall(),
  grpcEncoding = "gzip",
)

/** Sends the response messages to the channel. */
internal fun <R : Any> SendChannel<R>.readFromResponseBodyCallback(
  grpcCall: RealGrpcStreamingCall<*, R>,
  responseAdapter: ProtoAdapter<R>,
): Callback {
  return object : Callback {
    override fun onFailure(call: Call, e: IOException) {
      // Something broke. Kill the response channel.
      close(e)
    }

    override fun onResponse(call: Call, response: Response) {
      grpcCall.responseMetadata = response.headers.toMap()
      runBlocking {
        response.use {
          val messageSource = try {
            response.messageSource(responseAdapter)
          } catch (exception: IOException) {
            try {
              close(exception)
            } catch (e: CancellationException) {
              // If it's already canceled, there's nothing more to do.
            }
            return@use
          }

          messageSource.use { reader ->
            var exception: Exception? = null
            try {
              while (true) {
                val message = reader.read() ?: break
                send(message)
              }
              exception = response.grpcResponseToException()
            } catch (e: IOException) {
              exception = response.grpcResponseToException(e)
            } catch (e: Exception) {
              exception = e
            } finally {
              try {
                close(exception)
              } catch (e: CancellationException) {
                // If it's already canceled, there's nothing more to do.
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Stream messages from the request channel to the request body stream. This means:
 *
 * 1. read a message (non blocking, suspending code)
 * 2. write it to the stream (blocking)
 * 3. repeat. We also have to wait for all 2s to end before closing the writer
 *
 * If this fails reading a message from the channel, we need to call [MessageSink.cancel]. If it
 * fails writing a message to the network, we shouldn't call that method.
 *
 * If it fails either reading or writing we need to call [MessageSink.close] (via [Closeable.use])
 * and [ReceiveChannel.cancel].
 */
internal suspend fun <S : Any> ReceiveChannel<S>.writeToRequestBody(
  requestBody: PipeDuplexRequestBody,
  minMessageToCompress: Long,
  requestAdapter: ProtoAdapter<S>,
  callForCancel: Call,
) {
  val requestWriter = requestBody.messageSink(minMessageToCompress, requestAdapter, callForCancel)
  try {
    requestWriter.use {
      var channelReadFailed = true
      try {
        consumeEach { message ->
          channelReadFailed = false
          requestWriter.write(message)
          channelReadFailed = true
        }
        channelReadFailed = false
      } finally {
        if (channelReadFailed) requestWriter.cancel()
      }
    }
  } catch (e: Throwable) {
    cancel(CancellationException("Could not write message", e))
    if (e !is IOException && e !is CancellationException) {
      throw e
    }
  }
}

/** Reads messages from the response body. */
internal fun <R : Any> Response.messageSource(
  protoAdapter: ProtoAdapter<R>,
): GrpcMessageSource<R> {
  checkGrpcResponse()
  val grpcEncoding = header("grpc-encoding")
  val responseSource = body!!.source()
  return GrpcMessageSource(responseSource, protoAdapter, grpcEncoding)
}

/** Returns an exception if the response does not follow the protocol. */
private fun Response.checkGrpcResponse() {
  val contentType = body!!.contentType()
  if (code != 200 ||
    contentType == null ||
    contentType.type != "application" ||
    contentType.subtype != "grpc" && contentType.subtype != "grpc+proto"
  ) {
    throw IOException("expected gRPC but was HTTP status=$code, content-type=$contentType")
  }
}

/** Returns an exception if the gRPC call didn't have a grpc-status of 0. */
internal fun Response.grpcResponseToException(suppressed: IOException? = null): IOException? {
  var trailers = headersOf()
  var transportException = suppressed
  if (suppressed == null) {
    try {
      trailers = trailers()
    } catch (e: IOException) {
      transportException = e
    }
  }

  val grpcStatus = trailers["grpc-status"] ?: header("grpc-status")
  val grpcMessage = trailers["grpc-message"] ?: header("grpc-message")
  val url = request.url.toString()
  var grpcStatusDetailsBin: ByteString? = null

  grpcStatus?.toIntOrNull()?.takeIf { it != 0 }?.let { grpcStatusInt ->
    (trailers["grpc-status-details-bin"] ?: header("grpc-status-details-bin"))?.let {
      try {
        grpcStatusDetailsBin = it.decodeBase64()
      } catch (e: IllegalArgumentException) {
        throw IOException(
          "gRPC transport failure, invalid grpc-status-details-bin" +
            " (HTTP status=$code, grpc-status=$grpcStatus, grpc-message=$grpcMessage)",
          e,
        )
      }
    }

    return GrpcException(GrpcStatus.get(grpcStatusInt), grpcMessage, grpcStatusDetailsBin?.toByteArray(), url)
  }

  if (transportException != null || grpcStatus?.toIntOrNull() == null) {
    return IOException(
      "gRPC transport failure" +
        " (HTTP status=$code, grpc-status=$grpcStatus, grpc-message=$grpcMessage)",
      transportException,
    )
  }

  return null // Success.
}
