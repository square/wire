/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.gradle

import com.squareup.wire.VERSION
import com.squareup.wire.schema.Target
import com.squareup.wire.schema.WireRun
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class WireTask : SourceTask() {
  @get:OutputDirectories
  var outputDirectories: List<File>? = null

  @get:Input
  var pluginVersion: String = VERSION

  @get:Internal
  internal lateinit var sourceInput: WireInput

  @get:Internal
  internal lateinit var protoInput: WireInput

  @Input
  lateinit var roots: List<String>

  @Input
  lateinit var prunes: List<String>

  @Input
  @Optional
  var sinceVersion: String? = null

  @Input
  @Optional
  var untilVersion: String? = null

  @Input
  @Optional
  var rules: String? = null

  @Input
  lateinit var targets: List<Target>

  @Input
  @Optional
  var proto3Preview: String? = null

  @TaskAction
  fun generateWireFiles() {
    val includes = mutableListOf<String>()
    val excludes = mutableListOf<String>()

    rules?.let {
      project.file(it)
          .forEachLine { line ->
            when (line.firstOrNull()) {
              '+' -> includes.add(line.substring(1))
              '-' -> excludes.add(line.substring(1))
              else -> Unit
            }
          }
    }

    if (includes.isNotEmpty()) {
      logger.info("INCLUDE:\n * ${includes.joinToString(separator = "\n * ")}")
    }
    if (excludes.isNotEmpty()) {
      logger.info("EXCLUDE:\n * ${excludes.joinToString(separator = "\n * ")}")
    }
    if (includes.isEmpty() && excludes.isEmpty()) logger.info("NO INCLUDES OR EXCLUDES")

    if (logger.isDebugEnabled) {
      sourceInput.debug(logger)
      protoInput.debug(logger)
      logger.debug("roots: $roots")
      logger.debug("prunes: $prunes")
      logger.debug("rules: $rules")
      logger.debug("targets: $targets")
    }

    val wireRun = WireRun(
        sourcePath = sourceInput.toLocations(),
        protoPath = protoInput.toLocations(),
        treeShakingRoots = if (roots.isEmpty()) includes else roots,
        treeShakingRubbish = if (prunes.isEmpty()) excludes else prunes,
        since = sinceVersion,
        until = untilVersion,
        targets = targets,
        proto3Preview = (proto3Preview == "UNSUPPORTED")
    )
    wireRun.execute()
  }
}
