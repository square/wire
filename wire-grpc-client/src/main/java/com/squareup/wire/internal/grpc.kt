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

import com.squareup.wire.ProtoAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import java.io.IOException

internal val APPLICATION_GRPC_MEDIA_TYPE: MediaType = "application/grpc".toMediaType()

/** Returns a new request body that writes [onlyMessage]. */
internal fun <S : Any> newRequestBody(
  requestAdapter: ProtoAdapter<S>,
  onlyMessage: S
): RequestBody {
  return object : RequestBody() {
    override fun contentType() = APPLICATION_GRPC_MEDIA_TYPE

    override fun writeTo(sink: BufferedSink) {
      val grpcMessageSink = GrpcMessageSink(
          sink = sink,
          messageAdapter = requestAdapter,
          callForCancel = null,
          grpcEncoding = "gzip"
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
  requestAdapter: ProtoAdapter<S>,
  callForCancel: Call
) = GrpcMessageSink(
    sink = createSink(),
    messageAdapter = requestAdapter,
    callForCancel = callForCancel,
    grpcEncoding = "gzip"
)

/** Sends the response messages to the channel. */
internal fun <R : Any> SendChannel<R>.readFromResponseBodyCallback(
  responseAdapter: ProtoAdapter<R>
): Callback {
  return object : Callback {
    override fun onFailure(call: Call, e: IOException) {
      // Something broke. Kill the response channel.
      close(e)
    }

    override fun onResponse(call: Call, response: Response) {
      runBlocking {
        response.use {
          response.messageSource(responseAdapter).use { reader ->
            while (true) {
              val message = reader.read() ?: break
              send(message)
            }

            close(response.grpcStatusToException())
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
 */
internal fun <S : Any> ReceiveChannel<S>.writeToRequestBody(
  requestBody: PipeDuplexRequestBody,
  requestAdapter: ProtoAdapter<S>,
  callForCancel: Call
) {
  CoroutineScope(Dispatchers.IO).launch {
    requestBody.messageSink(requestAdapter, callForCancel).use { requestWriter ->
      var success = false
      try {
        consumeEach { message ->
          requestWriter.write(message)
        }
        success = true
      } finally {
        if (!success) requestWriter.cancel()
      }
    }
  }
}

/** Reads messages from the response body. */
internal fun <R : Any> Response.messageSource(
  protoAdapter: ProtoAdapter<R>
): GrpcMessageSource<R> {
  val grpcEncoding = header("grpc-encoding")
  val responseSource = body!!.source()
  return GrpcMessageSource(responseSource, protoAdapter, grpcEncoding)
}

/** Maps the response trailer to either success (null) or an exception. */
internal fun Response.grpcStatusToException(): IOException? {
  val grpcStatus = trailers().get("grpc-status") ?: header("grpc-status")
  return when (grpcStatus) {
    "0" -> null
    else -> {
      // also see https://github.com/grpc/grpc-go/blob/master/codes/codes.go#L31
      IOException("unexpected or absent grpc-status: $grpcStatus")
    }
  }
}
