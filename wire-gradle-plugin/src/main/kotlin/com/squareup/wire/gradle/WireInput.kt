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
package com.squareup.wire.gradle

import com.squareup.wire.gradle.WireExtension.ProtoRootSet
import com.squareup.wire.schema.Location
import java.io.EOFException
import java.io.File
import java.io.RandomAccessFile
import org.gradle.api.internal.file.FileOperations

internal val List<ProtoRootSet>.inputLocations: List<InputLocation>
  get() = flatMap { rootSet ->
    rootSet.roots.map { file ->
      rootSet.inputLocation(file)
    }
  }

private fun ProtoRootSet.inputLocation(file: File): InputLocation {
  val includes = when {
    includes.isEmpty() && excludes.isEmpty() -> listOf("**/*.proto")
    else -> includes
  }
  // We store [file] relative to the [project] in order to not invalidate the cache when we don't
  // have to.
  val path = when {
    file.toPath().startsWith(project.rootDir.toPath()) -> project.relativePath(file.path)
    else -> file.path
  }
  return InputLocation(path, includes, excludes)
}

/**
 * Expand a jar or directory to the specific `.proto` files we want to build, applying includes and
 * excludes.
 */
internal fun InputLocation.toLocations(
  fileOperations: FileOperations,
  projectDir: File,
): List<Location> {
  val base = when {
    File(path).isAbsolute -> File(path)
    else -> File(projectDir, path)
  }
  return buildList {
    val fileTree = when {
      base.isZip -> fileOperations.zipTree(base)
      base.isDirectory -> fileOperations.fileTree(base)
      else -> throw IllegalArgumentException(
        """
        |Invalid path string: "$path".
        |For individual files, use the following syntax:
        |wire {
        |  sourcePath {
        |    srcDir("dirPath")
        |    include("relativePath")
        |  }
        |}
        """.trimMargin(),
      )
    }

    fileTree
      .matching { pattern ->
        when {
          includes.isNotEmpty() || excludes.isNotEmpty() -> {
            pattern.include(*includes.toTypedArray())
            pattern.exclude(*excludes.toTypedArray())
          }

          else -> pattern.include("**/*.proto")
        }
      }
      .visit { entry ->
        if (!entry.isDirectory) add(Location(base.path, entry.path))
      }
  }
}

internal val File.isZip: Boolean
  get() {
    if (!exists() || !isFile) {
      return false
    }
    if (path.endsWith(".jar") || path.endsWith(".zip")) {
      return true
    }
    return try {
      // See "magic numbers": https://en.wikipedia.org/wiki/ZIP_(file_format)
      val signature = RandomAccessFile(this, "r").use { it.readInt() }
      signature == 0x504B0304 || signature == 0x504B0506 || signature == 0x504B0708
    } catch (_: EOFException) {
      false
    }
  }
