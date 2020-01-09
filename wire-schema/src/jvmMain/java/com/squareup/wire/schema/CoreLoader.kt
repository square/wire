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
object CoreLoader : Loader {
  const val DESCRIPTOR_PROTO = "google/protobuf/descriptor.proto"
  const val WIRE_EXTENSIONS_PROTO = "wire/extensions.proto"

  override fun load(path: String): ProtoFile {
    if (path == DESCRIPTOR_PROTO || path == WIRE_EXTENSIONS_PROTO) {
      val resourceAsStream = SchemaLoader::class.java.getResourceAsStream("/$path")
      resourceAsStream.source().buffer().use { source ->
        val data = source.readUtf8()
        val location = Location.get(path)
        val element = ProtoParser.parse(location, data)
        return ProtoFile.get(element)
      }
    }

    throw error("unexpected load: $path")
  }
}
