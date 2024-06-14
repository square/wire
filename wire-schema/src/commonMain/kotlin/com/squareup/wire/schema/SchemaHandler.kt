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

import com.squareup.wire.WireLogger
import com.squareup.wire.internal.JvmDefaultWithCompatibility
import com.squareup.wire.internal.Serializable
import okio.FileSystem
import okio.Path

/** A [SchemaHandler] [handle]s [Schema]! */
abstract class SchemaHandler {
  /**
   * This will handle all [ProtoFile]s which are part of the `sourcePath`. If a [Module] is set in
   * the [context], it will handle only [Type]s and [Service]s the module defines respecting the
   * [context] rules. Override this method if you have specific needs the default implementation
   * doesn't address.
   */
  open fun handle(schema: Schema, context: Context) {
    val moduleTypes = context.module?.types
    for (protoFile in schema.protoFiles) {
      if (!context.inSourcePath(protoFile)) continue

      // Remove types from the file which are not owned by this partition.
      val filteredProtoFile = protoFile.copy(
        types = protoFile.types.filter { if (moduleTypes != null) it.type in moduleTypes else true },
        services = protoFile.services.filter { if (moduleTypes != null) it.type in moduleTypes else true },
      )

      handle(filteredProtoFile, context)
    }
  }

  /**
   * Returns the [Path] of the file which [type] will have been generated into. Null if nothing has
   * been generated.
   */
  abstract fun handle(type: Type, context: Context): Path?

  /**
   * Returns the [Path]s of the files which [service] will have been generated into. Null if
   * nothing has been generated.
   */
  abstract fun handle(service: Service, context: Context): List<Path>

  /**
   * Returns the [Path] of the files which [field] will have been generated into. Null if nothing
   * has been generated.
   */
  abstract fun handle(extend: Extend, field: Field, context: Context): Path?

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

  /**
   * This will handle all [Type]s and [Service]s of the [protoFile] in respect to the emitting
   * rules defined by the [context]. If exclusive, the handled [Type]s and [Service]s should be
   * added to the [ClaimedDefinitions]. Already consumed types and services themselves will be
   * omitted by this handler.
   */
  protected fun handle(
    protoFile: ProtoFile,
    context: Context,
  ) {
    val claimedDefinitions = context.claimedDefinitions
    val emittingRules = context.emittingRules
    val claimedPaths = context.claimedPaths
    val types = protoFile.types
      .filter { if (claimedDefinitions != null) it !in claimedDefinitions else true }
      .filter { emittingRules.includes(it.type) }

    for (type in types) {
      val generatedFilePath = handle(type, context)

      if (generatedFilePath != null) {
        claimedPaths.claim(generatedFilePath, type)
        // We don't let other targets handle this one.
        claimedDefinitions?.claim(type)
      }
    }

    val services = protoFile.services
      .filter { if (claimedDefinitions != null) it !in claimedDefinitions else true }
      .filter { emittingRules.includes(it.type) }

    for (service in services) {
      val generatedFilePaths = handle(service, context)

      if (generatedFilePaths.isNotEmpty()) {
        for (generatedFilePath in generatedFilePaths) {
          claimedPaths.claim(generatedFilePath, service)
        }
        // We don't let other targets handle this one.
        claimedDefinitions?.claim(service)
      }
    }

    protoFile.extendList
      .flatMap { extend -> extend.fields.map { field -> extend to field } }
      .filter { (extend, field) ->
        claimedDefinitions == null || extend.member(field) !in claimedDefinitions
      }
      .filter { (_, field) ->
        // We append `.*` to the field's package name so that it matches rules defined as
        // `package.*`.
        emittingRules.includes(ProtoType.get(field.packageName + ".*"))
      }
      .forEach { (extend, field) ->
        val generatedFilePath = handle(extend, field, context)

        if (generatedFilePath != null) {
          claimedPaths.claim(generatedFilePath, extend)
          // We don't let other targets handle this one.
          claimedDefinitions?.claim(extend.member(field))
        }
      }
  }

  /** Implementations of this interface must have a no-arguments public constructor. */
  @JvmDefaultWithCompatibility
  interface Factory : Serializable {
    fun create(
      /** Set of rules letting the [SchemaHandler] know what [ProtoType] to include in its logic. */
      includes: List<String>,
      /** Set of rules letting the [SchemaHandler] know what [ProtoType] to exclude in its logic. */
      excludes: List<String>,
      /**
       * If true, the schema handler is to [claim][SchemaHandler.Context.claimedDefinitions] types
       * and services it handled.
       */
      exclusive: Boolean,
      /** Location on the fileSystem where the schema handler is to write files, if it needs to. */
      outDirectory: String,
      /**
       * Arbitrary list of options to be used for the caller to pass extract data to the Schema
       * Handler if needed.
       * */
      options: Map<String, String>,
    ): SchemaHandler
  }
}
