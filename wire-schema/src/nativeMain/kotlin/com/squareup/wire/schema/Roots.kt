/*
 * Copyright (C) 2023 Square, Inc.
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
import okio.IOException
import okio.Path

internal actual fun Path.roots(
  fileSystem: FileSystem,
  location: Location,
): List<Root> {
  val symlinkTarget = fileSystem.metadataOrNull(this)?.symlinkTarget
  val path = symlinkTarget ?: this
  return when {
    fileSystem.metadataOrNull(path)?.isDirectory == true -> {
      check(location.base.isEmpty())
      listOf(DirectoryRoot(location.path, fileSystem, path))
    }

    path.toString().endsWith(".proto") -> listOf(ProtoFilePath(location, fileSystem, path))

    // Handle a .zip or .jar file by adding all .proto files within.
    else -> throw IllegalArgumentException(
      """
        |expected a directory, or .proto: $path
        |archive (.zip / .jar / etc.) are not supported in native.
      """.trimMargin(),
    )
  }
}

/**
 * Returns the parsed proto file and the path that should be used to import it.
 *
 * This is a path like `squareup/dinosaurs/Dinosaur.proto` for a file based on its package name
 * (like `squareup.dinosaurs`) and its file name (like `Dinosaur.proto`).
 */
internal actual fun ProtoFilePath.parse(): ProtoFile {
  try {
    fileSystem.read(path) {
      val data = readUtf8()
      val element = ProtoParser.parse(location, data)
      return ProtoFile.get(element)
    }
  } catch (e: IOException) {
    throw IOException("Failed to load $path", e)
  }
}
