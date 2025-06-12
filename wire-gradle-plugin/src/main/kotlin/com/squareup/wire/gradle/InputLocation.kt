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

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input

internal data class InputLocation(
  /**
   * The collection of files to which the below includes and excludes apply. This collection may include
   * any valid gradle dependency type, including directories, jars, and project dependencies. Therefore,
   * it needs to be represented as a gradle Classpath to ensure cache-friendly behaviour.
   */
  @get:Classpath
  val configuration: FileCollection,

  /** Files to include, following `PatternFilterable` syntax. */
  @get:Input
  val includes: List<String>,

  /** Files to exclude, following `PatternFilterable` syntax. */
  @get:Input
  val excludes: List<String>,
)
