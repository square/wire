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
package com.squareup.wire.schema

import com.squareup.wire.schema.internal.parser.ProtoParser
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.asResourceFileSystem

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
  private val resourceFileSystem by lazy {
    CoreLoader::class.java.classLoader.asResourceFileSystem()
  }

  @Deprecated("Instead use loadWireRuntimeProto.", replaceWith = ReplaceWith("loadWireRuntimeProto"))
  override fun load(path: String): ProtoFile {
    return loadWireRuntimeProto(path.toPath())
  }

  actual fun loadWireRuntimeProto(path: Path): ProtoFile {
    if (isWireRuntimeProto(path)) {
      resourceFileSystem.read("/".toPath() / path) {
        val data = readUtf8()
        val location = Location.get(path.toString())
        val element = ProtoParser.parse(location, data)
        return ProtoFile.get(element)
      }
    }

    error("Unexpected load: $path. It is missing from Wire's resources.")
  }

  override fun load(path: Path, fileSystem: FileSystem) = commonLoad(path, fileSystem)

  override fun withErrors(errors: ErrorCollector) = this
}
