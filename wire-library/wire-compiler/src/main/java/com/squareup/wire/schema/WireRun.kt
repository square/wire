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

import com.squareup.wire.ConsoleWireLogger
import com.squareup.wire.WireLogger
import com.squareup.wire.schema.PartitionedSchema.Partition
import com.squareup.wire.schema.internal.DagChecker
import com.squareup.wire.schema.internal.TypeMover
import okio.FileSystem
import okio.Path

/**
 * An invocation of the Wire compiler. Each invocation performs the following operations:
 *
 *  1. Read source `.proto` files directly from the file system or from archive files (ie. `.jar`
 *     and `.zip` files). This will also load imported `.proto` files from either the [sourcePath]
 *     or [protoPath]. The collection of loaded type declarations is called a schema.
 *
 *  2. Validate the schema and resolve references between types.
 *
 *  3. Optionally refactor the schema. This builds a new schema that is a subset of the original.
 *     The new schema contains only types that are both transitively reachable from
 *     [treeShakingRoots] and not in [treeShakingRubbish]. Types are moved to different files as
 *     specified by [moves].
 *
 *  4. Call each target. It will generate sources for protos in the [sourcePath] that are in its
 *     [Target.includes], that are not in its [Target.excludes], and that haven't already been
 *     emitted by an earlier target.
 *
 *
 * Source Directories and Archives
 * -------------------------------
 *
 * The [sourcePath] and [protoPath] lists contain locations that are of the following forms:
 *
 *  * Locations of `.proto` files.
 *
 *  * Locations of directories that contain a tree of `.proto` files. Typically this is a directory
 *    ending in `src/main/proto`.
 *
 *  * Locations of `.zip` and `.jar` archives that contain a tree of `.proto` files. Typically this
 *    is a `.jar` file from a Maven repository.
 *
 * When one `.proto` message imports another, the import is resolved from the base of each location
 * and archive. If the build is in the unfortunate situation where an import could be resolved by
 * multiple files, whichever was listed first takes precedence.
 *
 * Although the content and structure of [sourcePath] and [protoPath] are the same, only types
 * defined in [sourcePath] are used to generate sources.
 *
 *
 * Matching Packages, Types, and Members
 * -------------------------------------
 *
 * The [treeShakingRoots], [treeShakingRubbish], [Target.includes] and [Target.excludes] lists
 * contain strings that select proto types and members. Strings in these lists are in one of these
 * forms:
 *
 *  * Package names followed by `.*`, like `squareup.dinosaurs.*`. This matches types defined in the
 *    package and its descendant packages. A lone asterisk `*` matches all packages.
 *
 *  * Fully-qualified type names like `squareup.dinosaurs.Dinosaur`. Types may be messages, enums,
 *    or services.
 *
 *  * Fully-qualified member names like `squareup.dinosaurs.Dinosaur#name`. These are type names
 *    followed by `#` followed by a member name. Members may be message fields, enum constants, or
 *    service RPCs.
 *
 * It is an error to specify mutually-redundant values in any of these lists. For example, the list
 * `[squareup.dinosaurs, squareup.dinosaurs.Dinosaur]` is invalid because the second element is
 * already matched by the first.
 *
 * Every element in each lists must apply to at least one declaration. Otherwise that option is
 * unnecessary and a possible typo.
 *
 *
 * Composability
 * -------------
 *
 * There are many moving parts in this system! For most applications it is safe to use [sourcePath]
 * and [targets] only. The other options are for the benefit of large and modular applications.
 *
 * ### Use [protoPath] when one proto module depends on another proto module.
 *
 * These `.proto` files are used for checking dependencies only. It is assumed that the sources for
 * these protos are generated elsewhere.
 *
 * ### Use tree shaking to remove unwanted types.
 *
 * [Tree shaking](https://en.wikipedia.org/wiki/Tree_shaking) can be used to create a
 * small-as-possible generated footprint even if the source declarations are large. This works like
 * [ProGuard](https://en.wikipedia.org/wiki/ProGuard_(software)) and other code shrinking compilers:
 * it allows you to benefit from a shared codebase without creating a large artifact.
 *
 * ### Use multiple targets to split generated code across multiple programming languages.
 *
 * If your project is already using generated Java, it’s difficult to switch to generated Kotlin.
 * Instead of switching everything over at once you can use multiple targets to switch over
 * incrementally. Targets consume their types; subsequent targets get whatever types are left over.
 */
data class WireRun(
  /**
   * Source `.proto` files for this task to generate from.
   */
  val sourcePath: List<Location>,

  /**
   * Sources `.proto` files for this task to use when resolving references.
   */
  val protoPath: List<Location> = listOf(),

  /**
   * The roots of the schema model. Wire will prune the schema model to only include types in this
   * list and the types transitively required by them.
   *
   * If a member is included in this list then the enclosing type is included but its other members
   * are not. For example, if `squareup.dinosaurs.Dinosaur#name` is in this list then the emitted
   * source of the `Dinosaur` message will have the `name` field, but not the `length_meters` or
   * `mass_kilograms` fields.
   */
  val treeShakingRoots: List<String> = listOf("*"),

  /**
   * Types and members that will be stripped from the schema model. Wire will remove the elements
   * themselves and also all references to them.
   */
  val treeShakingRubbish: List<String> = listOf(),

  /**
   * Types to move before generating code or producing other output. Use this with [ProtoTarget] to
   * refactor proto schemas safely.
   */
  val moves: List<TypeMover.Move> = listOf(),

  /**
   * The exclusive lower bound of the version range. Fields with `until` values greater than this
   * are retained.
   */
  val sinceVersion: String? = null,

  /**
   * The inclusive upper bound of the version range. Fields with `since` values less than or equal
   * to this are retained.
   */
  val untilVersion: String? = null,

  /**
   * The only version of the version range. Fields with `until` values greater than this, as well as
   * fields with `since` values less than or equal to this, are retained. This field is mutually
   * exclusive with `sinceVersion` and `untilVersion`.
   */
  val onlyVersion: String? = null,

  /**
   * Action to take with the loaded, resolved, and possibly-pruned schema.
   */
  val targets: List<Target>,

  /**
   * A map from module dir to module info which dictates how the loaded types are partitioned and
   * generated.
   *
   * When empty everything is generated in the root output directory.
   * If desired, multiple modules can be specified along with dependencies between them. Types
   * which appear in dependencies will not be re-generated.
   */
  val modules: Map<String, Module> = emptyMap(),

  /**
   * If true, no validation will be executed to check package cycles.
   */
  val permitPackageCycles: Boolean = false,
) {
  data class Module(
    val dependencies: Set<String> = emptySet(),
    val pruningRules: PruningRules? = null
  )

  init {
    val dagChecker = DagChecker(modules.keys) { moduleName ->
      modules.getValue(moduleName).dependencies
    }
    val cycles = dagChecker.check()
    require(cycles.isEmpty()) {
      buildString {
        append("ERROR: Modules contain dependency cycle(s):\n")
        for (cycle in cycles) {
          append(" - ")
          append(cycle)
          append('\n')
        }
      }
    }
  }

  fun execute(fs: FileSystem = FileSystem.SYSTEM, logger: WireLogger = ConsoleWireLogger()) {
    return SchemaLoader(fs).use { schemaLoader ->
      execute(fs, logger, schemaLoader)
    }
  }

  private fun execute(fs: FileSystem, logger: WireLogger, schemaLoader: SchemaLoader) {
    schemaLoader.permitPackageCycles = permitPackageCycles
    schemaLoader.initRoots(sourcePath, protoPath)

    // Validate the schema and resolve references
    val fullSchema = schemaLoader.loadSchema()
    val sourceLocationPaths = schemaLoader.sourcePathFiles.map { it.location.path }
    val moveTargetPaths = moves.map { it.targetPath }

    // Refactor the schema.
    val schema = refactorSchema(fullSchema, logger)

    val targetToEmittingRules = targets.associateWith {
      EmittingRules.Builder()
          .include(it.includes)
          .exclude(it.excludes)
          .build()
    }
    val targetsExclusiveLast = targets.sortedBy { it.exclusive }

    val partitions = if (modules.isNotEmpty()) {
      val partitionedSchema = schema.partition(modules)
      // TODO handle errors and warnings
      partitionedSchema.partitions
    } else {
      // Synthesize a single partition that includes everything from the schema.
      mapOf(null to Partition(schema))
    }

    val skippedForSyntax = mutableSetOf<Location>()
    val claimedPaths = mutableMapOf<Path, String>()
    val errorCollector = ErrorCollector()
    for ((moduleName, partition) in partitions) {
      val targetToSchemaHandler = targets.associateWith {
        it.newHandler(
            partition.schema, moduleName, partition.transitiveUpstreamTypes, fs, logger,
            schemaLoader, errorCollector
        )
      }

      // Call each target.
      for (protoFile in partition.schema.protoFiles) {
        if (protoFile.location.path !in sourceLocationPaths &&
            protoFile.location.path !in moveTargetPaths) {
          continue
        }

        // Remove types from the file which are not owned by this partition.
        val filteredProtoFile = protoFile.copy(
            types = protoFile.types.filter { it.type in partition.types },
            services = protoFile.services.filter { it.type in partition.types }
        )

        val claimedDefinitions = ClaimedDefinitions()
        claimedDefinitions.claim(ProtoType.ANY)

        for (target in targetsExclusiveLast) {
          val schemaHandler = targetToSchemaHandler.getValue(target)
          schemaHandler.handle(
              filteredProtoFile,
              targetToEmittingRules.getValue(target),
              claimedDefinitions,
              claimedPaths,
              isExclusive = target.exclusive)
        }
      }
    }

    errorCollector.throwIfNonEmpty()

    for (emittingRules in targetToEmittingRules.values) {
      if (emittingRules.unusedIncludes().isNotEmpty()) {
        logger.warn("""Unused includes in targets:
            |  ${emittingRules.unusedIncludes().joinToString(separator = "\n  ")}
            """.trimMargin())
      }

      if (emittingRules.unusedExcludes().isNotEmpty()) {
        logger.warn("""Unused excludes in targets:
            |  ${emittingRules.unusedExcludes().joinToString(separator = "\n  ")}
            """.trimMargin())
      }
    }

    if (skippedForSyntax.isNotEmpty()) {
      logger.warn("""Skipped .proto files with unsupported syntax. Add this line to fix:
          |  syntax = "proto2";
          |  ${skippedForSyntax.joinToString(separator = "\n  ") { it.toString() }}
          """.trimMargin())
    }
  }

  /** Returns a transformed schema with unwanted elements removed and moves applied. */
  private fun refactorSchema(schema: Schema, logger: WireLogger): Schema {
    if (treeShakingRoots == listOf("*") &&
        treeShakingRubbish.isEmpty() &&
        sinceVersion == null &&
        untilVersion == null &&
        moves.isEmpty()) {
      return schema
    }

    val pruningRules = PruningRules.Builder()
        .addRoot(treeShakingRoots)
        .prune(treeShakingRubbish)
        .since(sinceVersion)
        .until(untilVersion)
        .only(onlyVersion)
        .build()

    val prunedSchema = schema.prune(pruningRules)

    for (rule in pruningRules.unusedRoots()) {
      logger.warn("Unused element in treeShakingRoots: $rule")
    }

    for (rule in pruningRules.unusedPrunes()) {
      logger.warn("Unused element in treeShakingRubbish: $rule")
    }

    return TypeMover(prunedSchema, moves).move()
  }
}
