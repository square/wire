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
import com.squareup.wire.schema.internal.parser.ProtoFileElement
import java.io.Closeable
import java.io.IOException
import java.nio.file.FileSystem
import java.util.TreeSet

/**
 * Load proto files and their transitive dependencies and parse them. Keep track of which files were
 * loaded from where so that we can use that information later when deciding what to generate.
 */
class NewSchemaLoader(
  private val fs: FileSystem
) : Closeable, Loader {
  private val closer = Closer.create()

  /** Errors accumulated by this load. */
  private val errors = mutableListOf<String>()

  /** Paths accumulated that we failed to load. */
  private val missingImports = TreeSet<String>()

  /** Source path roots that need to be closed */
  private var sourcePathRoots: List<Root>? = null

  /** Proto path roots that need to be closed */
  private var protoPathRoots: List<Root>? = null

  /** Initialize the [WireRun.sourcePath] and [WireRun.protoPath] from which files are loaded. */
  fun initRoots(
    sourcePath: List<Location>,
    protoPath: List<Location> = listOf()
  ) {
    check(sourcePathRoots == null && protoPathRoots == null)
    sourcePathRoots = allRoots(closer, sourcePath)
    protoPathRoots = allRoots(closer, protoPath)
  }

  /** Returns the files in the source path. */
  @Throws(IOException::class)
  fun loadSourcePathFiles(): List<ProtoFile> {
    check(sourcePathRoots != null && protoPathRoots != null) {
      "call initRoots() before calling loadSourcePathFiles()"
    }

    val result = mutableListOf<ProtoFile>()
    for (sourceRoot in sourcePathRoots!!) {
      for (locationAndPath in sourceRoot.allProtoFiles()) {
        result += load(locationAndPath)
      }
    }

    if (result.isEmpty()) {
      errors += "no sources"
    }

    checkForErrors()

    return result
  }

  override fun load(path: String): ProtoFile {
    // Traverse roots in search of the one that has this path.
    var loadFrom: ProtoFilePath? = null
    for (protoPathRoot in protoPathRoots!!) {
      val locationAndPath: ProtoFilePath = protoPathRoot.resolve(path) ?: continue
      if (loadFrom != null) {
        errors += "$path is ambiguous:\n  $locationAndPath\n  $loadFrom"
        continue
      }
      loadFrom = locationAndPath
    }

    if (loadFrom == null) {
      if (path == CoreLoader.DESCRIPTOR_PROTO) {
        return CoreLoader.load(path)
      }
      missingImports += path
      return ProtoFile.get(ProtoFileElement.empty(path))
    } else {
      return load(loadFrom)
    }
  }

  private fun load(protoFilePath: ProtoFilePath): ProtoFile {
    val protoFile = protoFilePath.parse()
    val importPath = protoFile.importPath(protoFilePath.location)

    // If the .proto was specified as a full path without a separate base directory that it's
    // relative to, confirm that the import path and file system path agree.
    if (protoFilePath.location.base.isEmpty()
        && protoFilePath.location.path != importPath
        && !protoFilePath.location.path.endsWith("/$importPath")) {
      errors += "expected ${protoFilePath.location.path} to have a path ending with $importPath"
    }

    return protoFile
  }

  /** Convert `pathStrings` into roots that can be searched. */
  private fun allRoots(closer: Closer, locations: List<Location>): List<Root> {
    val result = mutableListOf<Root>()
    val baseToRoots = mutableMapOf<String, List<Root>>()

    for (location in locations) {
      try {
        result += location.roots(fs, closer, baseToRoots)
      } catch (e: IllegalArgumentException) {
        errors += e.message!!
      }
    }
    return result
  }

  fun reportLoadingErrors() {
    if (missingImports.isNotEmpty()) {
      errors += """
          |unable to resolve ${missingImports.size} imports:
          |  ${missingImports.joinToString(separator = "\n  ")}
          |searching ${protoPathRoots!!.size} proto paths:
          |  ${protoPathRoots!!.joinToString(separator = "\n  ")}
          """.trimMargin()
    }
    checkForErrors()
  }

  private fun checkForErrors() {
    require(errors.isEmpty()) { errors.joinToString(separator = "\n") }
  }

  override fun close() {
    return closer.close()
  }
}

internal fun ProtoFile.importPath(location: Location): String {
  return when {
    location.base.isEmpty() -> canonicalImportPath(location)
    else -> location.path
  }
}

internal fun ProtoFile.canonicalImportPath(location: Location): String {
  val filename = location.path.substringAfterLast('/')
  return packageName!!.replace('.', '/') + "/" + filename
}
