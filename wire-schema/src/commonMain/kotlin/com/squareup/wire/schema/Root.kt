/*
 * Copyright (C) 2018 Square, Inc.
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

import com.squareup.wire.schema.internal.ProfileFileElement
import com.squareup.wire.schema.internal.ProfileParser
import com.squareup.wire.schema.internal.withUnixSlashes
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath

internal sealed class Root {
  abstract val base: String?

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
  baseToRoots: MutableMap<String, List<Root>> = mutableMapOf(),
): List<Root> {
  if (isWireRuntimeProto(this)) {
    // Handle descriptor.proto, etc. by returning a placeholder path.
    return listOf(ProtoFilePath(this, fs, path.toPath()))
  } else if (base.isNotEmpty()) {
    val roots = baseToRoots.getOrPut(base) {
      Location.get(base).roots(fs)
    }
    for (root in roots) {
      val resolved = root.resolve(path) ?: continue
      return listOf(resolved)
    }
    throw IllegalArgumentException("unable to resolve $this")
  } else {
    val path = path.toPath()
    return baseToRoots.getOrPut(this.path) {
      path.roots(fs, this)
    }
  }
}

internal expect fun Path.roots(fileSystem: FileSystem, location: Location): List<Root>

/**
 * Returns the parsed proto file and the path that should be used to import it.
 *
 * This is a path like `squareup/dinosaurs/Dinosaur.proto` for a file based on its package name
 * (like `squareup.dinosaurs`) and its file name (like `Dinosaur.proto`).
 */
internal expect fun ProtoFilePath.parse(): ProtoFile

/**
 * A logical location (the base location and path to the file), plus the physical path to load.
 * These will be different if the file is loaded from a .zip archive.
 */
internal class ProtoFilePath(
  val location: Location,
  val fileSystem: FileSystem,
  val path: Path,
) : Root() {
  override val base: String?
    get() = null

  override fun allProtoFiles() = setOf(this)

  override fun resolve(import: String): ProtoFilePath? {
    if (import == location.path) return this
    return null
  }

  fun parseProfile(): ProfileFileElement {
    try {
      fileSystem.read(path) {
        val data = readUtf8()
        return ProfileParser(location, data).read()
      }
    } catch (e: IOException) {
      throw IOException("Failed to load $path", e)
    }
  }

  override fun toString(): String = location.toString()
}

internal class DirectoryRoot(
  /** The location of either a directory or .zip file. */
  override val base: String,

  val fileSystem: FileSystem,

  /** The root to search. If this is a .zip file this is within its internal file system. */
  val rootDirectory: Path,
) : Root() {
  override fun allProtoFiles(): Set<ProtoFilePath> {
    return fileSystem.listRecursively(rootDirectory)
      .filter { it.toString().endsWith(".proto") }
      .map { descendant ->
        val location = Location.get(
          base = base,
          path = descendant.relativeTo(rootDirectory).withUnixSlashes().toString(),
        )
        ProtoFilePath(location, fileSystem, descendant)
      }
      .toSet()
  }

  override fun resolve(import: String): ProtoFilePath? {
    val resolved = rootDirectory / import
    if (!fileSystem.exists(resolved)) return null
    return ProtoFilePath(
      location = Location.get(base, import.toPath().withUnixSlashes().toString()),
      fileSystem = fileSystem,
      path = resolved,
    )
  }

  override fun toString(): String = base
}
