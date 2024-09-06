/*
 * Copyright (C) 2018 Square, Inc.
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

import okio.FileSystem
import okio.IOException

/**
 * Load proto files and their transitive dependencies and parse them. Keep track of which files were
 * loaded from where so that we can use that information later when deciding what to generate.
 */
expect class SchemaLoader(fileSystem: FileSystem) : Loader, ProfileLoader {
  override fun load(path: String): ProtoFile

  /** Returns a new loader that reports failures to [errors]. */
  override fun withErrors(errors: ErrorCollector): Loader

  override fun loadProfile(name: String, schema: Schema): Profile

  /** Strict by default. Note that golang cannot build protos with package cycles. */
  var permitPackageCycles: Boolean

  /**
   * All qualified named Protobuf types in [opaqueTypes] will be evaluated as being of type `bytes`.
   * On code generation, the fields of such types will be using the platform equivalent of `bytes`,
   * like [okio.ByteString] for the JVM. Note that scalar types cannot be opaqued.
   */
  var opaqueTypes: List<ProtoType>

  /**
   * If true, the schema loader will load the whole graph, including files and types not used by
   * anything in the source path.
   */
  var loadExhaustively: Boolean

  /** Subset of the schema that was loaded from the source path. */
  val sourcePathFiles: List<ProtoFile>

  /** Initialize the [WireRun.sourcePath] and [WireRun.protoPath] from which files are loaded. */
  fun initRoots(
    sourcePath: List<Location>,
    protoPath: List<Location> = listOf(),
  )

  @Throws(IOException::class)
  fun loadSchema(): Schema
}
