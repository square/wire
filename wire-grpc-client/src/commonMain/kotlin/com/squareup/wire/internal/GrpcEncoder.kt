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

import okio.BufferedSink
import okio.buffer

internal sealed class GrpcEncoder(val name: String) {
  /** Returns a stream that decodes `source`. */
  abstract fun encode(sink: BufferedSink): BufferedSink

  internal object IdentityGrpcEncoder : GrpcEncoder("identity") {
    override fun encode(sink: BufferedSink) = sink
  }

  internal object GzipGrpcEncoder : GrpcEncoder("gzip") {
    override fun encode(sink: BufferedSink) = sink.asGzip().buffer()
  }
}

internal fun String.toGrpcEncoder(): GrpcEncoder {
  return when (this) {
    "identity" -> GrpcEncoder.IdentityGrpcEncoder
    "gzip" -> GrpcEncoder.GzipGrpcEncoder
    "deflate" -> throw ProtocolException("deflate not yet supported")
    "snappy" -> throw ProtocolException("snappy not yet supported")
    else -> throw ProtocolException("unsupported grpc-encoding: $this")
  }
}
