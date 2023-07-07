/*
 * Copyright (C) 2022 Square, Inc.
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
import com.squareup.wire.schema.internal.readBomAsCharset
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.openZip

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
    else -> {
      try {
        check(location.base.isEmpty())
        val sourceFs = fileSystem.openZip(path)
        listOf(DirectoryRoot(location.path, sourceFs, "/".toPath()))
      } catch (_: IOException) {
        throw IllegalArgumentException(
          "expected a directory, archive (.zip / .jar / etc.), or .proto: $path",
        )
      }
    }
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
      val charset = readBomAsCharset()
      val data = readString(charset)
      val element = ProtoParser.parse(location, data)
      return ProtoFile.get(element)
    }
  } catch (e: IOException) {
    throw IOException("Failed to load $path", e)
  }
}
