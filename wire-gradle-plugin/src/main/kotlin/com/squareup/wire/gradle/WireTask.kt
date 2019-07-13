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
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.Target
import com.squareup.wire.schema.WireRun
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class WireTask : SourceTask() {
  @Input
  fun pluginVersion() = VERSION

  @Internal
  lateinit var sourceConfiguration: Configuration

  @Internal
  lateinit var protoConfiguration: Configuration

  @Input
  lateinit var roots: List<String>

  @Input
  lateinit var prunes: List<String>

  @Input
  @Optional
  var rules: String? = null

  @Input
  lateinit var targets: List<Target>

  @Internal
  lateinit var dependencyToIncludes: Map<Dependency, List<String>>

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
      sourceConfiguration.dependencies.forEach {
        logger.debug(
            "dep: $it -> " + ((it as? FileCollectionDependency)?.files as? SourceDirectorySet)?.srcDirs
        )
        logger.debug("sourceConfiguration.files for dep: " + sourceConfiguration.files(it))
      }
      protoConfiguration.dependencies.forEach {
        logger.debug(
            "dep: $it -> " + ((it as? FileCollectionDependency)?.files as? SourceDirectorySet)?.srcDirs
        )
        logger.debug("protoConfiguration.files for dep: " + protoConfiguration.files(it))
      }
      logger.debug("roots: $roots")
      logger.debug("prunes: $prunes")
      logger.debug("rules: $rules")
      logger.debug("targets: $targets")
    }

    val wireRun = WireRun(
        sourcePath = sourceConfiguration.toLocations(),
        protoPath = protoConfiguration.toLocations(),
        treeShakingRoots = if (roots.isEmpty()) includes else roots,
        treeShakingRubbish = if (prunes.isEmpty()) excludes else prunes,
        targets = targets
    )
    wireRun.execute()
  }

  private fun Configuration.toLocations(): List<Location> {
    return dependencies
        .flatMap { dep ->
          files(dep)
              .flatMap { file ->
                file.toLocations(dep)
              }
        }
  }

  private fun File.toLocations(dependency: Dependency): List<Location> {
    if (dependency is FileCollectionDependency && dependency.files is SourceDirectorySet) {
      val srcDir = (dependency.files as SourceDirectorySet).srcDirs.first {
        path.startsWith(it.path + "/")
      }
      return listOf(Location.get(
          base = srcDir.path,
          path = path.substring(srcDir.path.length + 1)
      ))
    }

    val includes = dependencyToIncludes[dependency]

    if (includes == null || includes.isEmpty()) {
      return listOf(Location.get(path))
    }

    return includes.map { include ->
      Location.get(base = path, path = include)
    }
  }
}
