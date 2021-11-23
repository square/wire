/*
 * Copyright 2013 Square Inc.
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
package com.squareup.wire

import com.squareup.wire.schema.ProtoType
import okio.Path

/**
 * Logger class used by [WireRun][com.squareup.wire.schema.WireRun] and
 * [SchemaHandlers][com.squareup.wire.schema.Target.SchemaHandler] to log information related to
 * processing the protobuf [Schema][com.squareup.wire.schema.Schema].
 */
interface WireLogger {
  // TODO(Benoit) I think we should remove this one.
  fun setQuiet(quiet: Boolean)
  /**
   * This is called when an artifact is handled by a
   * [SchemaHandler][com.squareup.wire.schema.Target.SchemaHandler].
   * @param outputPath is the path where the artifact is written on disk.
   * @param qualifiedName is the file path when generating a `.proto` file, the type or service
   *   name prefixed with its package name when generating a `.java` or `.kt` file, and the type
   *   name when generating a `.swift` file.
   */
  fun artifactHandled(outputPath: Path, qualifiedName: String)

  /**
   * This is called when an artifact has been passed down to a
   * [SchemaHandler][com.squareup.wire.schema.Target.SchemaHandler] but has been skipped. This is
   * useful for dry-runs.
   * @param type is the unique identifier for the skipped type.
   */
  fun artifactSkipped(type: ProtoType)

  /**
   * This is called when [WireRun][com.squareup.wire.schema.WireRun] found unusual configurations.
   * For instance when types marked as roots or prunes are not used.
   */
  fun warn(message: String)
}
