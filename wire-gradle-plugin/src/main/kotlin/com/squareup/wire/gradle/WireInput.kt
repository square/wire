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

import com.squareup.wire.gradle.WireExtension.ProtoRootSet
import com.squareup.wire.schema.Location
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.FileOrUriNotationConverter
import org.gradle.api.logging.Logger
import java.io.File
import java.net.URI

/**
 * Builds Wire's inputs (expressed as [Location] lists) from Gradle's objects (expressed as
 * directory trees, jars, and coordinates). This includes registering dependencies with the project
 * so they can be resolved for us.
 */
internal class WireInput(
  var project: Project,
  var configuration: Configuration
) {
  val name: String
    get() = configuration.name

  private val dependencyToIncludes = mutableMapOf<Dependency, List<String>>()

  fun addPaths(paths: Set<String>) {
    for (path in paths) {
      val dependency = resolveDependency(path)
      configuration.dependencies.add(dependency)
    }
  }

  fun addJars(jars: Set<ProtoRootSet>) {
    for (jar in jars) {
      jar.srcJar?.let { path ->
        val dependency = resolveDependency(path)
        dependencyToIncludes[dependency] = jar.includes
        configuration.dependencies.add(dependency)
      }
    }
  }

  fun addTrees(trees: Set<SourceDirectorySet>) {
    for (tree in trees) {
      // TODO: this eagerly resolves dependencies; fix this!
      tree.srcDirs.forEach {
        check(it.exists()) {
          "Invalid path string: \"${it.relativeTo(project.projectDir)}\". Path does not exist."
        }
      }
      val dependency = project.dependencies.create(tree)
      configuration.dependencies.add(dependency)
    }
  }

  private fun resolveDependency(path: String): Dependency {
    val parser = FileOrUriNotationConverter.parser()

    val converted = parser.parseNotation(path)

    if (converted is File) {
      val file = if (!converted.isAbsolute) File(project.projectDir, converted.path) else converted

      check(file.exists()) { "Invalid path string: \"$path\". Path does not exist." }

      return when {
        file.isDirectory -> project.dependencies.create(project.files(path))
        file.isJar -> project.dependencies.create(project.files(file.path))
        else -> throw IllegalArgumentException(
            """
            |Invalid path string: "$path".
            |For individual files, use the following syntax:
            |wire {
            |  sourcePath {
            |    srcDir 'dirPath'
            |    include 'relativePath'
            |  }
            |}
            """.trimMargin()
        )
      }
    } else if (converted is URI && isURL(converted)) {
      throw IllegalArgumentException(
          "Invalid path string: \"$path\". URL dependencies are not allowed."
      )
    } else {
      // Assume it's a possible external dependency and let Gradle sort it out later.
      return project.dependencies.create(path)
    }
  }

  private fun isURL(uri: URI) =
      try {
        uri.toURL()
        true
      } catch (e: Exception) {
        false
      }

  fun debug(logger: Logger) {
    configuration.dependencies.forEach { dep ->
      val srcDirs = ((dep as? FileCollectionDependency)?.files as? SourceDirectorySet)?.srcDirs
      val includes = dependencyToIncludes[dep] ?: listOf()
      logger.debug("dep: $dep -> $srcDirs")
      logger.debug("$name.files for dep: ${configuration.files(dep)}")
      logger.debug("$name.includes for dep: $includes")
    }
  }

  fun toLocations(): List<Location> {
    return configuration.dependencies
        .flatMap { dep ->
          configuration.files(dep)
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

    val includes = dependencyToIncludes[dependency] ?: listOf()

    if (includes.isEmpty()) {
      return listOf(Location.get(path))
    }

    return includes.map { include ->
      Location.get(base = path, path = include)
    }
  }

  private val File.isJar
    get() = path.endsWith(".jar")
}
