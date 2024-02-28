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

internal data class InputLocation(
  /** The path to the directory or .jar. This might not exist until the [WireTask] runs! */
  val path: String,

  /** Files to include, following `PatternFilterable` syntax. */
  val includes: List<String>,

  /** Files to exclude, following `PatternFilterable` syntax. */
  val excludes: List<String>,
) : Serializable
