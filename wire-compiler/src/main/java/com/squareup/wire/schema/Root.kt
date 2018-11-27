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

import com.google.common.io.Closer
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.ProviderNotFoundException
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

internal sealed class Root {
  /** Returns all proto files within this root. */
  abstract fun allProtoFiles(): Set<LocationAndPath>

  /** Returns the proto file if it's in this root, or null if it isn't. */
  abstract fun resolve(import: String): LocationAndPath?
}

internal fun Path.roots(closer: Closer): List<Root> {
  return when {
    Files.isDirectory(this) -> listOf(DirectoryRoot(this, this))

    endsWithDotProto() -> listOf(StandaloneFileRoot(this))

    // Handle a .zip or .jar file by adding all .proto files within.
    else -> {
      try {
        val sourceFs = FileSystems.newFileSystem(this, javaClass.classLoader)
        closer.register(sourceFs)
        sourceFs.rootDirectories.map { inZipDirectory -> DirectoryRoot(this, inZipDirectory) }
      } catch (e: ProviderNotFoundException) {
        throw IllegalArgumentException(
            "expected a directory, archive (.zip / .jar / etc.), or .proto: $this")
      }
    }
  }
}

internal class StandaloneFileRoot(path: Path) : Root() {
  private val locationAndPath = LocationAndPath(Location.get("$path"), path)

  override fun allProtoFiles() = setOf(locationAndPath)

  override fun resolve(import: String): LocationAndPath? = null
}

internal class DirectoryRoot(
  /** The path to either a directory or .zip file. */
  val path: Path,

  /** The root to search. If this is a .zip file this is within its internal file system. */
  val rootDirectory: Path
) : Root() {
  override fun allProtoFiles(): Set<LocationAndPath> {
    val result = mutableSetOf<LocationAndPath>()
    Files.walkFileTree(rootDirectory, object : SimpleFileVisitor<Path>() {
      override fun visitFile(descendant: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (descendant.endsWithDotProto()) {
          val location = Location.get("$path", "${rootDirectory.relativize(descendant)}")
          result.add(LocationAndPath(location, descendant))
        }
        return FileVisitResult.CONTINUE
      }
    })
    return result
  }

  override fun resolve(import: String): LocationAndPath? {
    val resolved = rootDirectory.resolve(import)
    if (!Files.exists(resolved)) return null
    return LocationAndPath(Location.get("$path", import), resolved)
  }
}

private fun Path.endsWithDotProto() = fileName.toString().endsWith(".proto")