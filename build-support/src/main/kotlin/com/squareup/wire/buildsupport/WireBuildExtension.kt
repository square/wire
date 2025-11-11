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
package com.squareup.wire.buildsupport

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

interface WireBuildExtension {

  /**
   * Enable artifact publishing and Dokka documentation generation.
   *
   * The published `artifactId` will be set to the project name.
   */
  fun publishing()

  /**
   * Registers a new compilation and test task, and sets it as a task dependency of `jvmTest`.
   */
  fun createKotlinJvmTestTask(
    taskName: String,
    block: KotlinSourceSet.() -> Unit,
  )

  /**
   * Registers a new source set and test task, and sets it as a task dependency of `jvmTest`.
   */
  fun createJavaTestTask(
    taskName: String,
    block: JavaTestTaskScope.() -> Unit,
  )
}
