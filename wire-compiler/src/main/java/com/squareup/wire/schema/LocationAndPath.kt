/*
 * Copyright 2018 Square Inc.
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
import java.io.IOException
import java.nio.file.Path

/**
 * A logical location (the base location and path to the file), plus the physical path to load.
 * These will be different if the file is loaded from a .zip archive.
 */
internal data class LocationAndPath(val location: Location, val path: Path) {
  fun parse(): ProtoFile {
    try {
      path.source().buffer().use { source ->
        val data = source.readUtf8()
        val element = ProtoParser.parse(location, data)
        return ProtoFile.get(element)
      }
    } catch (e: IOException) {
      throw IOException("Failed to load $path", e)
    }
  }
}