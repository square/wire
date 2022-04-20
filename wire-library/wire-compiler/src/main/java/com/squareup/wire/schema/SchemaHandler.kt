/*
 * Copyright 2022 Block Inc.
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

import com.squareup.wire.WireLogger
import com.squareup.wire.internal.Serializable
import okio.FileSystem
import okio.Path

/**
 * A [SchemaHandler] [handle]s [Schema]!
 * Implementations of this interface must have a no-arguments public constructor. TODO(Benoit) Is it still true?
 *
 * Consider using [AbstractSchemaHandler] for default logic around handling [Schema] and delegating
 * individual calls on [Type]s and [Service]s.
 */
interface SchemaHandler {
  /**
   * Entry point for the [SchemaHandler] to handle [schema]. It is the responsibility of the
   * [SchemaHandler] to respect the [context].
   *
   * This function is invoked at most once per [SchemaHandler] instance. When a single type can
   * handle multiple schemas, a new handler should be created for each schema.
   */
  fun handle(schema: Schema, context: Context)

  interface Factory : Serializable {
    fun create(): SchemaHandler
  }

  /**
   * A [Context] holds the information necessary for a [SchemaHandler] to do its job. It contains
   * both helping objects such as [logger], and constraining objects such as [emittingRules].
   */
  data class Context(
    /** To be used by the [SchemaHandler] for reading/writing operations on disk. */
    val fileSystem: FileSystem,
    /** Location on [fileSystem] where the [SchemaHandler] is to write files, if it needs to. */
    val outDirectory: Path,
    /** Event-listener like logger with which [SchemaHandler] can notify handled artifacts. */
    val logger: WireLogger,
    /**
     * Object to be used by the [SchemaHandler] to store errors. After all [SchemaHandler]s are
     * finished, Wire will throw an exception if any error are present inside the collector.
     */
    val errorCollector: ErrorCollector = ErrorCollector(),
    /**
     * Set of rules letting the [SchemaHandler] know what [ProtoType] to include or exclude in its
     * logic. This object represents the `includes` and `excludes` values which were associated
     * with its [Target].
     */
    val emittingRules: EmittingRules = EmittingRules(),
    /**
     * If set, the [SchemaHandler] is to handle only types which are not claimed yet, and claim
     * itself types it has handled. If null, the [SchemaHandler] is to handle all types.
     */
    val claimedDefinitions: ClaimedDefinitions? = null,
    /** If the [SchemaHandler] writes files, it is to claim [Path]s of files it created. */
    val claimedPaths: ClaimedPaths = ClaimedPaths(),
    /**
     * A [Module] dictates how the loaded types are partitioned and how they are to be handled.
     * If null, there are no partition and all types are to be handled.
     */
    val module: Module? = null,
    /**
     * Contains [Location.path] values of all `sourcePath` roots. The [SchemaHandler] is to ignore
     * [ProtoFile]s not part of this set; this verification can be executed via the [inSourcePath]
     * method.
     */
    val sourcePathPaths: Set<String>? = null,
    /**
     * To be used by the [SchemaHandler] if it supports [Profile] files. Please note that this API
     * is unstable and can change at anytime.
     */
    val profileLoader: ProfileLoader? = null,
  ) {
    /** True if this [protoFile] ia part of a `sourcePath` root. */
    fun inSourcePath(protoFile: ProtoFile): Boolean {
      return inSourcePath(protoFile.location)
    }

    /** True if this [location] ia part of a `sourcePath` root. */
    fun inSourcePath(location: Location): Boolean {
      return sourcePathPaths == null || location.path in sourcePathPaths
    }
  }

  /**
   * A [Module] dictates how the loaded types are to be partitioned and handled.
   */
  data class Module(
    /** The name of the [Module]. */
    val name: String,
    /** The types that this module is to handle. */
    val types: Set<ProtoType>,
    /** These are the types depended upon by [types] associated with their module name. */
    val upstreamTypes: Map<ProtoType, String> = mapOf(),
  )
}
