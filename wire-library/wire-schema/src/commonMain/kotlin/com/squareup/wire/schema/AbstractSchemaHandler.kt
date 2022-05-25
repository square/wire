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

import okio.Path

/**
 * [SchemaHandler] with default logic on handling [Schema]'s [ProtoFile]s respecting the
 * [SchemaHandler.Context].
 */
abstract class AbstractSchemaHandler : SchemaHandler {
  /**
   * This will handle all [ProtoFile]s which are part of the `sourcePath`. If a
   * [Module][SchemaHandler.Context.Module] is set in the [context], it will handle only [Type]s
   * and [Service]s the module defines.
   */
  override fun handle(schema: Schema, context: SchemaHandler.Context) {
    val moduleTypes = context.module?.types
    for (protoFile in schema.protoFiles) {
      if (!context.inSourcePath(protoFile)) continue

      // Remove types from the file which are not owned by this partition.
      val filteredProtoFile = protoFile.copy(
        types = protoFile.types.filter { if (moduleTypes != null) it.type in moduleTypes else true },
        services = protoFile.services.filter { if (moduleTypes != null) it.type in moduleTypes else true }
      )

      handle(filteredProtoFile, context)
    }
  }

  /**
   * This will handle all [Type]s and [Service]s of the [protoFile] in respect to the emitting
   * rules defined by the [context]. If exclusive, the handled [Type]s and [Service]s should be
   * added to the [ClaimedDefinitions]. Already consumed types and services themselves will be
   * omitted by this handler.
   */
  private fun handle(
    protoFile: ProtoFile,
    context: SchemaHandler.Context,
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
      }

      // We don't let other targets handle this one.
      claimedDefinitions?.claim(type)
    }

    val services = protoFile.services
      .filter { if (claimedDefinitions != null) it !in claimedDefinitions else true }
      .filter { emittingRules.includes(it.type) }

    for (service in services) {
      val generatedFilePaths = handle(service, context)

      for (generatedFilePath in generatedFilePaths) {
        claimedPaths.claim(generatedFilePath, service)
      }

      // We don't let other targets handle this one.
      claimedDefinitions?.claim(service)
    }

    // TODO(jwilson): extend emitting rules to support include/exclude of extension fields.
    protoFile.extendList
      .flatMap { extend -> extend.fields.map { field -> extend to field } }
      .filter { (extend, field) ->
        claimedDefinitions == null || extend.member(field) !in claimedDefinitions
      }
      .forEach { (extend, field) ->
        // TODO(Benoît) claim path.
        handle(extend, field, context)

        // We don't let other targets handle this one.
        claimedDefinitions?.claim(extend.member(field))
      }
  }

  /**
   * Returns the [Path] of the file which [type] will have been generated into. Null if nothing has
   * been generated.
   */
  abstract fun handle(type: Type, context: SchemaHandler.Context): Path?

  /**
   * Returns the [Path]s of the files which [service] will have been generated into. Null if
   * nothing has been generated.
   */
  abstract fun handle(service: Service, context: SchemaHandler.Context): List<Path>

  /**
   * Returns the [Path] of the files which [field] will have been generated into. Null if nothing
   * has been generated.
   */
  abstract fun handle(extend: Extend, field: Field, context: SchemaHandler.Context): Path?
}
