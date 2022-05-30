/*
 * Copyright 2022 Block Inc.
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

import com.squareup.wire.schema.internal.CommonSchemaLoader
import com.squareup.wire.schema.internal.toOkioFileSystem
import okio.FileSystem
import java.nio.file.FileSystem as NioFileSystem

actual class SchemaLoader : Loader, ProfileLoader {
  private val delegate: CommonSchemaLoader

  constructor(fileSystem: NioFileSystem) : this(fileSystem.toOkioFileSystem())

  constructor(fileSystem: FileSystem) {
    delegate = CommonSchemaLoader(fileSystem)
  }

  private constructor(enclosing: CommonSchemaLoader, errors: ErrorCollector) {
    delegate = CommonSchemaLoader(enclosing, errors)
  }

  override fun withErrors(errors: ErrorCollector) = SchemaLoader(delegate, errors)

  /** Strict by default. Note that golang cannot build protos with package cycles. */
  actual var permitPackageCycles: Boolean
    get() = delegate.permitPackageCycles
    set(value) {
      delegate.permitPackageCycles = value
    }

  /**
   * If true, the schema loader will load the whole graph, including files and types not used by
   * anything in the source path.
   */
  actual var loadExhaustively: Boolean
    get() = delegate.loadExhaustively
    set(value) {
      delegate.loadExhaustively = value
    }

  /** Subset of the schema that was loaded from the source path. */
  actual val sourcePathFiles: List<ProtoFile>
    get() = delegate.sourcePathFiles

  /** Initialize the [WireRun.sourcePath] and [WireRun.protoPath] from which files are loaded. */
  actual fun initRoots(
    sourcePath: List<Location>,
    protoPath: List<Location>
  ) {
    delegate.initRoots(sourcePath, protoPath)
  }

  override fun loadProfile(name: String, schema: Schema) = delegate.loadProfile(name, schema)

  override fun load(path: String) = delegate.load(path)

  actual fun loadSchema(): Schema = delegate.loadSchema()
}
