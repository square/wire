/*
 * Copyright (C) 2020 Square, Inc.
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
package com.squareup.wire.schema

/**
 * A specific [Loader] which loads Wire's runtime proto files such as
 * `google/protobuf/descriptor.proto` or `wire/extensions.proto`.
 */
expect object CoreLoader : Loader {
  override fun load(path: String): ProtoFile

  /** Returns a new loader that reports failures to [errors]. */
  override fun withErrors(errors: ErrorCollector): Loader
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

internal const val DESCRIPTOR_PROTO = "google/protobuf/descriptor.proto"
internal const val WIRE_EXTENSIONS_PROTO = "wire/extensions.proto"

private const val ANY_PROTO = "google/protobuf/any.proto"
private const val DURATION_PROTO = "google/protobuf/duration.proto"
private const val EMPTY_PROTO = "google/protobuf/empty.proto"
private const val STRUCT_PROTO = "google/protobuf/struct.proto"
private const val TIMESTAMP_PROTO = "google/protobuf/timestamp.proto"
private const val WRAPPERS_PROTO = "google/protobuf/wrappers.proto"

/** A special base directory used for Wire's built-in .proto files. */
const val WIRE_RUNTIME_JAR = "wire-runtime.jar"
