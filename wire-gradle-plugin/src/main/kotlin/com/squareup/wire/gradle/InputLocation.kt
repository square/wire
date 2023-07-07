/*
 * Copyright (C) 2023 Square, Inc.
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

import com.squareup.wire.internal.Serializable
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import org.gradle.api.Project

class InputLocation private constructor(
  /** The base of this location; typically a directory or .jar file. */
  val base: String,

  /** The path to this location relative to [base]. */
  val path: String,
) : Serializable {
  companion object {
    @JvmStatic
    fun get(project: Project, path: String): InputLocation {
      // We store [path] relative to the [project] in order to not invalidate the cache when we
      // don't have to.
      return InputLocation("", project.relativePath(path))
    }

    @JvmStatic
    fun get(
      project: Project,
      base: String,
      path: String,
    ): InputLocation {
      val basePath = base.toPath()

      // On Windows, a dependency could live on another drive. If that's a case,
      // `project.relativePath` will throw so we don't try to optimize its reference.
      @Suppress("NAME_SHADOWING")
      val base = if (basePath.isAbsolute && project.buildDir.toOkioPath().root != basePath.root) {
        base
      } else {
        // We store [base] relative to the [project] in order to not invalidate the cache when we
        // don't have to.
        project.relativePath(base)
      }

      return InputLocation(base.trimEnd('/'), path)
    }
  }
}
