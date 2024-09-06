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

  actual override fun load(path: String): ProtoFile {
    if (isWireRuntimeProto(path)) {
      resourceFileSystem.read("/".toPath() / path) {
        val data = readUtf8()
        val location = Location.get(path)
        val element = ProtoParser.parse(location, data)
        return ProtoFile.get(element)
      }
    }

    error("unexpected load: $path")
  }

  actual override fun withErrors(errors: ErrorCollector): Loader = this
}
