/*
 * Copyright (C) 2019 Square, Inc.
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
package com.squareup.wire.schema

import com.squareup.wire.schema.internal.parser.ProtoParser
import okio.buffer
import okio.source

/**
 * A loader that can only load built-in `.proto` files:
 *
 *  * Google's protobuf descriptor, which defines standard options like `default`, `deprecated`, and
 *    `java_package`.
 *
 *  * Wire's extensions, which defines since and until options.
 *
 * If the user has provided their own version of these protos, those are preferred.
 */
actual object CoreLoader : Loader {
  private const val ANY_PROTO = "google/protobuf/any.proto"
  private const val DESCRIPTOR_PROTO = "google/protobuf/descriptor.proto"
  private const val DURATION_PROTO = "google/protobuf/duration.proto"
  private const val EMPTY_PROTO = "google/protobuf/empty.proto"
  private const val STRUCT_PROTO = "google/protobuf/struct.proto"
  private const val TIMESTAMP_PROTO = "google/protobuf/timestamp.proto"
  private const val WRAPPERS_PROTO = "google/protobuf/wrappers.proto"
  private const val WIRE_EXTENSIONS_PROTO = "wire/extensions.proto"

  /** A special base directory used for Wire's built-in .proto files. */
  const val WIRE_RUNTIME_JAR = "wire-runtime.jar"

  override fun load(path: String): ProtoFile {
    if (isWireRuntimeProto(path)) {
      val resourceAsStream = CoreLoader::class.java.getResourceAsStream("/$path")
      resourceAsStream.source().buffer().use { source ->
        val data = source.readUtf8()
        val location = Location.get(path)
        val element = ProtoParser.parse(location, data)
        return ProtoFile.get(element)
      }
    }

    throw error("unexpected load: $path")
  }

  fun isWireRuntimeProto(location: Location): Boolean {
    return location.base == WIRE_RUNTIME_JAR && isWireRuntimeProto(location.path)
  }

  /** Returns true if [path] is bundled in the wire runtime. */
  fun isWireRuntimeProto(path: String): Boolean {
    return path == ANY_PROTO ||
        path == DESCRIPTOR_PROTO ||
        path == DURATION_PROTO ||
        path == EMPTY_PROTO ||
        path == STRUCT_PROTO ||
        path == TIMESTAMP_PROTO ||
        path == WRAPPERS_PROTO ||
        path == WIRE_EXTENSIONS_PROTO
  }

  override fun withErrors(errors: ErrorCollector) = this
}
