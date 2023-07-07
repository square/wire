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

import com.squareup.wire.internal.Serializable

abstract class Target : Serializable {
  /**
   * Proto types to include generated sources for. Types listed here will be generated for this
   * target and not for subsequent targets in the task.
   *
   * This list should contain package names (suffixed with `.*`) and type names only. It should
   * not contain member names.
   */
  abstract val includes: List<String>

  /**
   * Proto types to excluded generated sources for. Types listed here will not be generated for this
   * target.
   *
   * This list should contain package names (suffixed with `.*`) and type names only. It should
   * not contain member names.
   */
  abstract val excludes: List<String>

  /**
   * True if types emitted for this target should not also be emitted for other targets. Use this
   * to cause multiple outputs to be emitted for the same input type.
   */
  abstract val exclusive: Boolean

  /**
   * Directory where this target will write its output.
   *
   * In Gradle, when this class is serialized, this is relative to the project to improve build
   * cacheability. Callers must use [copyTarget] to resolve it to real path prior to use.
   */
  abstract val outDirectory: String

  /**
   * Returns a new Target object that is a copy of this one, but with the given fields updated.
   */
  // TODO(Benoit) We're only ever copying outDirectory, maybe we can remove other fields and rename
  //  the method?
  abstract fun copyTarget(
    includes: List<String> = this.includes,
    excludes: List<String> = this.excludes,
    exclusive: Boolean = this.exclusive,
    outDirectory: String = this.outDirectory,
  ): Target

  abstract fun newHandler(): SchemaHandler
}
