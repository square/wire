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
import com.squareup.wire.java.Profile
import com.squareup.wire.java.internal.ProfileFileElement
import com.squareup.wire.schema.CoreLoader.isWireRuntimeProto
import com.squareup.wire.schema.internal.parser.ProtoFileElement
import java.io.Closeable
import java.io.IOException
import java.nio.file.FileSystem
import java.util.ArrayDeque

/**
 * Load proto files and their transitive dependencies and parse them. Keep track of which files were
 * loaded from where so that we can use that information later when deciding what to generate.
 */
class SchemaLoader : Closeable, Loader, ProfileLoader {
  private val fileSystem: FileSystem

  private val closer: Closer

  /** Errors accumulated by this load. */
  private val errors: ErrorCollector

  /** Source path roots that need to be closed */
  private var sourcePathRoots: List<Root>?

  /** Proto path roots that need to be closed */
  private var protoPathRoots: List<Root>?

  /** Subset of the schema that was loaded from the source path. */
  var sourcePathFiles: List<ProtoFile>
    private set

  /** Keys are a [Location.base]; values are the roots that those locations loaded from. */
  private val baseToRoots: MutableMap<String, List<Root>>

  constructor(fileSystem: FileSystem) {
    this.fileSystem = fileSystem
    this.closer = Closer.create()
    this.errors = ErrorCollector()
    this.sourcePathRoots = null
    this.protoPathRoots = null
    this.sourcePathFiles = listOf()
    this.baseToRoots = mutableMapOf()
  }

  private constructor(enclosing: SchemaLoader, errors: ErrorCollector) {
    this.fileSystem = enclosing.fileSystem
    this.closer = enclosing.closer
    this.errors = errors
    this.sourcePathRoots = enclosing.sourcePathRoots
    this.protoPathRoots = enclosing.protoPathRoots
    this.sourcePathFiles = enclosing.sourcePathFiles
    this.baseToRoots = enclosing.baseToRoots
  }

  override fun withErrors(errors: ErrorCollector) = SchemaLoader(this, errors)

  /** Initialize the [WireRun.sourcePath] and [WireRun.protoPath] from which files are loaded. */
  fun initRoots(
    sourcePath: List<Location>,
    protoPath: List<Location> = listOf()
  ) {
    check(sourcePathRoots == null && protoPathRoots == null)
    sourcePathRoots = allRoots(closer, sourcePath)
    protoPathRoots = allRoots(closer, protoPath)
  }

  @Throws(IOException::class)
  fun loadSchema(permitPackageCycles: Boolean = false): Schema {
    sourcePathFiles = loadSourcePathFiles()
    val linker = Linker(this, errors, permitPackageCycles)
    val result = linker.link(sourcePathFiles)
    errors.throwIfNonEmpty()
    return result
  }

  /** Returns the files in the source path. */
  @Throws(IOException::class)
  internal fun loadSourcePathFiles(): List<ProtoFile> {
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

    errors.throwIfNonEmpty()

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

    if (loadFrom != null) {
      return load(loadFrom)
    }

    if (isWireRuntimeProto(path)) {
      return CoreLoader.load(path)
    }

    errors += """
          |unable to find $path
          |  searching ${protoPathRoots!!.size} proto paths:
          |    ${protoPathRoots!!.joinToString(separator = "\n    ")}
          """.trimMargin()
    return ProtoFile.get(ProtoFileElement.empty(path))
  }

  private fun load(protoFilePath: ProtoFilePath): ProtoFile {
    if (isWireRuntimeProto(protoFilePath.location)) {
      return CoreLoader.load(protoFilePath.location.path)
    }

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
    for (location in locations) {
      try {
        result += location.roots(fileSystem, closer, baseToRoots)
      } catch (e: IllegalArgumentException) {
        errors += e.message!!
      }
    }
    return result
  }

  internal fun reportLoadingErrors() {
    errors.throwIfNonEmpty()
  }

  override fun close() {
    return closer.close()
  }

  override fun loadProfile(name: String, schema: Schema): Profile {
    val allLocations = schema.protoFiles.map { it.location }
    val locationsToCheck = locationsToCheck(name, allLocations)

    val profileElements = mutableListOf<ProfileFileElement>()
    for (location in locationsToCheck) {
      val roots = baseToRoots[location.base] ?: continue
      for (root in roots) {
        val resolved = root.resolve(location.path) ?: continue
        profileElements += resolved.parseProfile()
      }
    }

    val profile = Profile(profileElements)
    validate(schema, profileElements)
    return profile
  }

  /** Confirms that `protoFiles` link correctly against `schema`.  */
  private fun validate(schema: Schema, profileFiles: List<ProfileFileElement>) {
    for (profileFile in profileFiles) {
      for (typeConfig in profileFile.typeConfigs) {
        val type = importedType(ProtoType.get(typeConfig.type)) ?: continue

        val resolvedType = schema.getType(type)
        if (resolvedType == null) {
          // This type is either absent from .proto files, or merely not loaded because our schema
          // is incomplete. Unfortunately we can't tell the difference! Assume that this type is
          // just absent from the schema-as-loaded and therefore irrelevant to the current project.
          // Ignore it!
          //
          // (A fancier implementation would load the schema and profile in one step and they would
          // be mutually complete. We aren't bothering with this correctness at this phase.)
          continue
        }

        val requiredImport = resolvedType.location.path
        if (!profileFile.imports.contains(requiredImport)) {
          errors += "${typeConfig.location.path} needs to import $requiredImport " +
              "(${typeConfig.location})"
        }
      }
    }

    errors.throwIfNonEmpty()
  }

  /** Returns the type to import for `type`.  */
  private fun importedType(type: ProtoType): ProtoType? {
    var type = type
    // Map key type is always scalar.
    if (type.isMap) type = type.valueType!!
    return if (type.isScalar) null else type
  }

  /**
   * Returns a list of locations to check for profile files. This is the profile file name (like
   * "java.wire") in the same directory, and in all parent directories up to the base.
   */
  internal fun locationsToCheck(name: String, input: List<Location>): Set<Location> {
    val queue = ArrayDeque(input)

    val result = mutableSetOf<Location>()
    while (true) {
      val protoLocation = queue.poll() ?: break
      val lastSlash = protoLocation.path.lastIndexOf("/")
      val parentPath = protoLocation.path.substring(0, lastSlash + 1)
      val profileLocation = protoLocation.copy(path = "$parentPath$name.wire")

      if (!result.add(profileLocation)) continue // Already added.
      if (!parentPath.isNotEmpty()) continue // No more parents to enqueue.
      queue += protoLocation.copy(path = parentPath.dropLast(1)) // Drop trailing '/'.
    }
    return result
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
