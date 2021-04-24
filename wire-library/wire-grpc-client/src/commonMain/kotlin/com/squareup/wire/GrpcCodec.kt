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
package com.squareup.wire

import com.squareup.wire.internal.asGzip
import okio.BufferedSink
import okio.BufferedSource
import okio.Source
import okio.buffer

sealed class GrpcCodec(val name: String) {
    /** Returns a stream that decodes `source`. */
    abstract fun decode(source: BufferedSource): Source

    /** Returns a stream that encodes `source`. */
    abstract fun encode(sink: BufferedSink): BufferedSink

    object IdentityGrpcCodec : GrpcCodec("identity") {
        override fun decode(source: BufferedSource) = source
        override fun encode(sink: BufferedSink): BufferedSink = sink
    }

    object GzipGrpcCodec : GrpcCodec("gzip") {
        override fun decode(source: BufferedSource) = source.asGzip()
        override fun encode(sink: BufferedSink): BufferedSink = sink.asGzip().buffer()
    }
}
