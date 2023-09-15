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

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

actual object CoreLoader : Loader {
  @Deprecated("Instead use loadWireRuntimeProto.", replaceWith = ReplaceWith("loadWireRuntimeProto"))
  override fun load(path: String): ProtoFile {
    return loadWireRuntimeProto(path.toPath())
  }

  actual fun loadWireRuntimeProto(path: Path): ProtoFile {
    error("Wire cannot load $path on JavaScript. Please manually add it to the proto path.")
  }

  override fun load(path: Path, fileSystem: FileSystem) = commonLoad(path, fileSystem)

  override fun withErrors(errors: ErrorCollector) = this
}
