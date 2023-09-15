/*
 * Copyright (C) 2013 Square, Inc.
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
package com.squareup.wire

import com.squareup.wire.internal.Serializable
import com.squareup.wire.schema.ProtoType
import okio.Path

/**
 * Logger class used by [WireRun][com.squareup.wire.schema.WireRun] and
 * [SchemaHandlers][com.squareup.wire.schema.Target.SchemaHandler] to log information related to
 * processing the protobuf [Schema][com.squareup.wire.schema.Schema].
 */
interface WireLogger {
  /**
   * This is called when an artifact is handled by a
   * [SchemaHandler][com.squareup.wire.schema.Target.SchemaHandler].
   * @param outputPath is the path where the artifact is written on disk.
   * @param qualifiedName is the file path when generating a `.proto` file, the type or service
   *   name prefixed with its package name when generating a `.java` or `.kt` file, and the type
   *   name when generating a `.swift` file.
   * @param targetName is used to identify the concerned target. For
   * [JavaTarget][com.squareup.wire.schema.JavaTarget], the name will be "Java". For
   * [KotlinTarget][com.squareup.wire.schema.KotlinTarget], the name will be "Kotlin". For
   * [SwiftTarget][com.squareup.wire.schema.SwiftTarget], the name will be "Swift". For
   * [ProtoTarget][com.squareup.wire.schema.ProtoTarget], the name will be "Proto".
   */
  fun artifactHandled(outputPath: Path, qualifiedName: String, targetName: String)

  /**
   * This is called when an artifact has been passed down to a
   * [SchemaHandler][com.squareup.wire.schema.Target.SchemaHandler] but has been skipped. This is
   * useful for dry-runs.
   * @param type is the unique identifier for the skipped type.
   * @param targetName is used to identify the concerned target. For
   * [JavaTarget][com.squareup.wire.schema.JavaTarget], the name will be "Java". For
   * [KotlinTarget][com.squareup.wire.schema.KotlinTarget], the name will be "Kotlin". For
   * [SwiftTarget][com.squareup.wire.schema.SwiftTarget], the name will be "Swift". For
   * [ProtoTarget][com.squareup.wire.schema.ProtoTarget], the name will be "Proto".
   */
  fun artifactSkipped(type: ProtoType, targetName: String)

  /**
   * This is called if some `root` values have not been used when Wire pruned the schema model.
   * Note that `root` should contain package names (suffixed with `.*`), type names, and member
   * names only. It should not contain file paths. Unused roots can happen if the referenced type
   * or service isn't part of any `.proto` files defined in either
   * [sourcePath][com.squareup.wire.schema.WireRun.sourcePath] or
   * [protoPath][com.squareup.wire.schema.WireRun.protoPath], or if a broader root value is already
   * defined.
   */
  fun unusedRoots(unusedRoots: Set<String>)

  /**
   * This is called if some `prune` values have not been used when Wire pruned the schema model.
   * Note that `prune` should contain package names (suffixed with `.*`), type names, and member
   * names only. It should not contain file paths. Unused prunes can happen if the referenced type
   * or service isn't part of any `.proto` files defined in either
   * [sourcePath][com.squareup.wire.schema.WireRun.sourcePath] or
   * [protoPath][com.squareup.wire.schema.WireRun.protoPath], or if a broader prune value is
   * already defined.
   */
  fun unusedPrunes(unusedPrunes: Set<String>)

  /**
   * This is called if some `includes` values have not been used by the target they were defined in.
   * Note that `includes` should contain package names (suffixed with `.*`) and type names only. It
   * should not contain member names, nor file paths. Unused includes can happen if the referenced
   * type or service isn't part of the parsed and pruned schema model, or has already been consumed
   * by another preceding target.
   */
  // TODO(Benoit) We could pass the target name or something which makes it identifiable.
  fun unusedIncludesInTarget(unusedIncludes: Set<String>)

  /**
   * This is called if some `excludes` values have not been used by the target they were defined in.
   * Note that `excludes` should contain package names (suffixed with `.*`) and type names only. It
   * should not contain member names, nor file paths. Unused excludes can happen if the referenced
   * type or service isn't part of the parsed and pruned schema model, or has already been consumed
   * by another preceding target.
   */
  // TODO(Benoit) We could pass the target name or something which makes it identifiable.
  fun unusedExcludesInTarget(unusedExcludes: Set<String>)

  /** Implementations of this interface must have a no-arguments public constructor. */
  fun interface Factory : Serializable {
    fun create(): WireLogger
  }

  companion object {
    val NONE = object : WireLogger {
      override fun artifactHandled(outputPath: Path, qualifiedName: String, targetName: String) {}
      override fun artifactSkipped(type: ProtoType, targetName: String) {}
      override fun unusedRoots(unusedRoots: Set<String>) {}
      override fun unusedPrunes(unusedPrunes: Set<String>) {}
      override fun unusedIncludesInTarget(unusedIncludes: Set<String>) {}
      override fun unusedExcludesInTarget(unusedExcludes: Set<String>) {}
    }
  }
}
