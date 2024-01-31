/*
 * Copyright (C) 2024 Square, Inc.
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
package com.squareup.wire

import java.util.Base64
import okhttp3.Headers.Companion.headersOf
import okio.IOException

/** Returns an exception if the gRPC call didn't have a grpc-status of 0. */
fun GrpcResponse.grpcResponseToException(suppressed: IOException? = null): IOException? {
  var trailers = headersOf()
  var transportException = suppressed
  try {
    trailers = trailers()
  } catch (e: IOException) {
    if (transportException == null) transportException = e
  }

  val grpcStatus = trailers["grpc-status"] ?: header("grpc-status")
  val grpcMessage = trailers["grpc-message"] ?: header("grpc-message")
  var grpcStatusDetailsBin: ByteArray? = null

  grpcStatus?.toIntOrNull()?.takeIf { it != 0 }?.let { grpcStatusInt ->
    (trailers["grpc-status-details-bin"] ?: header("grpc-status-details-bin"))?.let {
      try {
        grpcStatusDetailsBin = Base64.getDecoder().decode(it)
      } catch (e: IllegalArgumentException) {
        throw IOException(
          "gRPC transport failure, invalid grpc-status-details-bin" +
            " (HTTP status=$code, grpc-status=$grpcStatus, grpc-message=$grpcMessage)",
          e,
        )
      }
    }

    return GrpcException(GrpcStatus.get(grpcStatusInt), grpcMessage, grpcStatusDetailsBin)
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
