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
import com.squareup.wire.schema.internal.parser.ProtoParser
import okio.buffer
import okio.source
import java.io.IOException
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.ProviderNotFoundException
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

internal sealed class Root {
  /** Returns all proto files within this root. */
  abstract fun allProtoFiles(): Set<ProtoFilePath>

  /** Returns the proto file if it's in this root, or null if it isn't. */
  abstract fun resolve(import: String): ProtoFilePath?
}

/**
 * Returns this location's roots.
 *
 * @param baseToRoots cached roots to avoid opening the same .zip multiple times.
 */
// TODO(jwilson): rework this API to combine baseToRoots and Closer.
internal fun Location.roots(
  fs: FileSystem,
  closer: Closer,
  baseToRoots: MutableMap<String, List<Root>> = mutableMapOf()
): List<Root> {
  if (base.isNotEmpty()) {
    val roots = baseToRoots.computeIfAbsent(base) {
      Location.get(it).roots(fs, closer)
    }
    for (root in roots) {
      val resolved = root.resolve(path) ?: continue
      return listOf(resolved)
    }
    throw IllegalArgumentException("unable to resolve $this")
  } else {
    val path = fs.getPath(path)
    return path.roots(closer, this)
  }
}

/** Returns this path's roots. */
private fun Path.roots(closer: Closer, location: Location): List<Root> {
  return when {
    Files.isDirectory(this) -> {
      check(location.base.isEmpty())
      listOf(DirectoryRoot(location.path, this))
    }

    endsWithDotProto() -> listOf(ProtoFilePath(location, this))

    // Handle a .zip or .jar file by adding all .proto files within.
    else -> {
      check(location.base.isEmpty())
      try {
        val sourceFs = FileSystems.newFileSystem(this, javaClass.classLoader)
        closer.register(sourceFs)
        sourceFs.rootDirectories.map { DirectoryRoot(location.path, it) }
      } catch (e: ProviderNotFoundException) {
        throw IllegalArgumentException(
            "expected a directory, archive (.zip / .jar / etc.), or .proto: $this")
      }
    }
  }
}

/**
 * A logical location (the base location and path to the file), plus the physical path to load.
 * These will be different if the file is loaded from a .zip archive.
 */
internal class ProtoFilePath(val location: Location, val path: Path) : Root() {
  init {
    check(path.endsWithDotProto()) { "expected a .proto suffix for $location" }
  }

  override fun allProtoFiles() = setOf(this)

  override fun resolve(import: String): ProtoFilePath? {
    if (import == location.path) return this
    return null
  }

  /**
   * Returns the parsed proto file and the path that should be used to import it.
   *
   * This is a path like `squareup/dinosaurs/Dinosaur.proto` for a file based on its package name
   * (like `squareup.dinosaurs`) and its file name (like `Dinosaur.proto`).
   */
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

internal class DirectoryRoot(
  /** The location of either a directory or .zip file. */
  val base: String,

  /** The root to search. If this is a .zip file this is within its internal file system. */
  val rootDirectory: Path
) : Root() {
  override fun allProtoFiles(): Set<ProtoFilePath> {
    val result = mutableSetOf<ProtoFilePath>()
    Files.walkFileTree(rootDirectory, object : SimpleFileVisitor<Path>() {
      override fun visitFile(descendant: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (descendant.endsWithDotProto()) {
          val location = Location.get(base, "${rootDirectory.relativize(descendant)}")
          result.add(ProtoFilePath(location, descendant))
        }
        return FileVisitResult.CONTINUE
      }
    })
    return result
  }

  override fun resolve(import: String): ProtoFilePath? {
    val resolved = rootDirectory.resolve(import)
    if (!Files.exists(resolved)) return null
    return ProtoFilePath(Location.get(base, import), resolved)
  }
}

private fun Path.endsWithDotProto() = fileName.toString().endsWith(".proto")