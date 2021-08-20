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
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.FileOrUriNotationConverter
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import java.io.File
import java.net.URI

/**
 * Builds Wire's inputs (expressed as [Location] lists) from Gradle's objects (expressed as
 * directory trees, jars, and coordinates). This includes registering dependencies with the project
 * so they can be resolved for us.
 */
internal class WireInput(var configuration: Configuration) {
  val name: String
    get() = configuration.name

  private val dependencyFilters = mutableMapOf<Dependency, Collection<WireExtension.Filter>>()

  val dependencies: DependencySet
    get() = configuration.dependencies

  // Deferred dependency evaluation.
  val inputFiles = mutableSetOf<File>()

  fun addPaths(project: Project, paths: Set<String>) {
    for (path in paths) {
      val dependency = resolveDependency(project, path)
      configuration.dependencies.add(dependency)
    }
  }

  fun addJars(project: Project, jars: Set<ProtoRootSet>) {
    for (jar in jars) {
      jar.srcJar?.let { path ->
        val dependency = resolveDependency(project, path)
        dependencyFilters[dependency] = jar.filters
        configuration.dependencies.add(dependency)
      }
    }
  }

  fun addProjects(project: Project, projects: Set<ProtoRootSet>) {
    for (projectPath in projects) {
      if (projectPath.srcProject == null) continue

      val dependency = resolveDependency(project, projectPath.srcProject!!)
      dependencyFilters[dependency] = projectPath.filters
      configuration.dependencies.add(dependency)
    }
  }

  fun addTrees(project: Project, trees: Set<SourceDirectorySet>) {
    for (tree in trees) {
      tree.srcDirs.forEach {
        inputFiles.add(it)
      }
      val dependency = project.dependencies.create(tree)
      configuration.dependencies.add(dependency)
    }
  }

  private fun resolveDependency(project: Project, path: String): Dependency {
    val parser = FileOrUriNotationConverter.parser()

    val converted = parser.parseNotation(path)

    if (converted is File) {
      val file = if (!converted.isAbsolute) File(project.projectDir, converted.path) else converted
      if (!path.mayBeProject) inputFiles.add(file)

      return when {
        file.isDirectory -> project.dependencies.create(project.files(path))
        file.isJar -> project.dependencies.create(project.files(file.path))
        path.mayBeProject -> {
          // Keys can be either `path` or `configuration`.
          // Example: "[path: ':someProj', configuration: 'someConf']"
          return project.dependencies.project(mutableMapOf("path" to path))
        }
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
      val includes = dependencyFilters[dep] ?: listOf()
      logger.debug("dep: $dep -> $srcDirs")
      logger.debug("$name.files for dep: ${configuration.files(dep)}")
      logger.debug("$name.includes for dep: $includes")
    }
  }

  fun toLocations(project: Project): Provider<List<Location>> {
    // We create a provider to support lazily created locations which do not exist at
    // configuration time.
    return project.provider {
      configuration.dependencies.flatMap { dep ->
        configuration.files(dep).flatMap { file -> file.toLocations(project, dep) }
      }
    }
  }

  private fun File.toLocations(project: Project, dependency: Dependency) =
    if (dependency is FileCollectionDependency && dependency.files is SourceDirectorySet) {
      val srcDir = (dependency.files as SourceDirectorySet).srcDirs.first {
        startsWith(it.path)
      }
      listOf(
        Location.get(
          base = srcDir.path,
          path = relativeTo(srcDir).toString()
        )
      )
    } else if (isJar) {
      val filters = dependencyFilters.getOrDefault(dependency, listOf())

      mutableListOf<Location>().apply {
        project.zipTree(path)
          .matching { pattern -> filters.forEach { it.act(pattern) } }
          .visit { if (!it.isDirectory) add(Location.get(path, it.path)) }
      }
    } else listOf(Location.get(path))

  private val File.isJar
    get() = path.endsWith(".jar")

  private val String.mayBeProject: Boolean
    get() = startsWith(":")
}
