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

import com.squareup.wire.gradle.WireExtension.JavaTarget
import com.squareup.wire.gradle.WireExtension.ProtoRootSet
import com.squareup.wire.gradle.WirePlugin.DependencyType.Directory
import com.squareup.wire.gradle.WirePlugin.DependencyType.Jar
import com.squareup.wire.gradle.WirePlugin.DependencyType.Path
import com.squareup.wire.schema.Target
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.FileOrUriNotationConverter
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.internal.typeconversion.NotationParser
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.net.URI
import javax.inject.Inject

class WirePlugin @Inject constructor(
  private val sourceDirectorySetFactory: SourceDirectorySetFactory
) : Plugin<Project> {
  private var kotlin = false
  private var java = false
  private val dependencyToIncludes = mutableMapOf<Dependency, List<String>>()

  private lateinit var sourceSetContainer: SourceSetContainer

  override fun apply(project: Project) {
    val logger = project.logger

    val extension = project.extensions.create(
        "wire", WireExtension::class.java, project, sourceDirectorySetFactory
    )

    project.plugins.all {
      logger.debug("plugin: $it")
      when (it) {
        is JavaBasePlugin -> {
          java = true
        }
        is KotlinBasePluginWrapper -> {
          kotlin = true
        }
      }
    }

    project.afterEvaluate {
      if (logger.isDebugEnabled) {
        sourceSetContainer = project.property("sourceSets") as SourceSetContainer
        sourceSetContainer.forEach {
          logger.debug("source set: ${it.name}")
        }
      }

      when {
        kotlin || java -> applyJvm(it, extension)
        else ->
          throw IllegalArgumentException(
              "Either the Java or Kotlin plugin must be applied before the Wire Gradle plugin."
          )
      }
    }
  }

  private fun applyJvm(
    project: Project,
    extension: WireExtension
  ) {
    val sourceConfiguration = project.configurations.create("wireSourceDependencies")

    val sourcePathDependencies =
        if (extension.sourcePaths.isNotEmpty() || extension.sourceTrees.isNotEmpty() || extension.sourceJars.isNotEmpty()) {
          mergeDependencyPaths(
              project, extension.sourcePaths, extension.sourceTrees, extension.sourceJars
          )
        } else {
          mergeDependencyPaths(project, setOf("src/main/proto"))
        }
    sourceConfiguration.dependencies.addAll(sourcePathDependencies)

    val protoConfiguration = project.configurations.create("wireProtoDependencies")

    if (extension.protoPaths.isNotEmpty() || extension.protoTrees.isNotEmpty() || extension.protoJars.isNotEmpty()) {
      val protoPathDependencies = mergeDependencyPaths(
          project, extension.protoPaths, extension.protoTrees, extension.protoJars
      )
      protoConfiguration.dependencies.addAll(protoPathDependencies)
    } else {
      protoConfiguration.dependencies.addAll(sourceConfiguration.dependencies)
    }

    // at this point, all source and proto file references should be set up for Gradle to resolve.

    val targets = mutableListOf<Target>()
    val defaultBuildDirectory = "${project.buildDir}/generated/src/main/java"
    val javaOutDirs = mutableListOf<String>()
    val kotlinOutDirs = mutableListOf<String>()

    val kotlinTarget = extension.kotlinTarget
    val javaTarget = extension.javaTarget ?: if (kotlinTarget != null) null else JavaTarget()

    javaTarget?.let { target ->
      val javaOut = target.out ?: defaultBuildDirectory
      javaOutDirs += javaOut
      targets += Target.JavaTarget(
          elements = target.elements ?: listOf("*"),
          outDirectory = javaOut,
          android = target.android,
          androidAnnotations = target.androidAnnotations,
          compact = target.compact
      )
    }
    kotlinTarget?.let { target ->
      val kotlinOut = target.out ?: defaultBuildDirectory
      kotlinOutDirs += kotlinOut
      targets += Target.KotlinTarget(
          elements = target.elements ?: listOf("*"),
          outDirectory = kotlinOut,
          android = target.android,
          javaInterop = target.javaInterop,
          blockingServices = target.blockingServices,
          singleMethodServices = target.singleMethodServices
      )
    }

    val wireTask = project.tasks.register("generateProtos", WireTask::class.java) { task ->
      task.source(sourceConfiguration)
      task.sourceConfiguration = sourceConfiguration
      task.protoConfiguration = protoConfiguration
      task.roots = extension.roots.toList()
      task.prunes = extension.prunes.toList()
      task.rules = extension.rules
      task.targets = targets
      task.group = "wire"
      task.dependencyToIncludes = dependencyToIncludes
      task.description = "Generate Wire protocol buffer implementation for .proto files"
    }

    javaTarget?.let {
      val compileJavaTask = project.tasks.named("compileJava") as TaskProvider<JavaCompile>
      compileJavaTask.configure {
        it.source(javaOutDirs)
        it.dependsOn(wireTask)
      }
      if (kotlin) {
        sourceSetContainer = project.property("sourceSets") as SourceSetContainer
        val mainSourceSet = sourceSetContainer.getByName("main") as SourceSet
        mainSourceSet.java.srcDirs(javaOutDirs)

        val compileKotlinTask = project.tasks.named("compileKotlin") as TaskProvider<KotlinCompile>
        compileKotlinTask.configure {
          it.dependsOn(wireTask)
        }
      }
    }

    kotlinTarget?.let {
      val compileKotlinTasks = project.tasks.withType(KotlinCompile::class.java)
      if (compileKotlinTasks.isEmpty()) {
        throw IllegalStateException("To generate Kotlin protos, please apply a Kotlin plugin.")
      }
      compileKotlinTasks.configureEach {
        it.source(kotlinOutDirs)
        it.dependsOn(wireTask)
      }
    }
  }

  /** Returns a list of dependencies for the build's paths, trees, and .jars. */
  private fun mergeDependencyPaths(
    project: Project,
    dependencyPaths: Set<String>,
    dependencyTrees: Set<SourceDirectorySet> = emptySet(),
    dependencyJars: Set<ProtoRootSet> = emptySet()
  ): List<Dependency> {
    val result = mutableListOf<Dependency>()

    for (dependencyTree in dependencyTrees) {
      result += project.dependencies.create(dependencyTree)
    }

    val parser = FileOrUriNotationConverter.parser()

    dependencyJars.forEach { depJar ->
      depJar.srcJar?.let { path ->
        val depPath = mapDependencyPath(parser, path, project)
        val dependency = project.dependencies.create(depPath.dependency)
        dependencyToIncludes[dependency] = depJar.includes
        result += dependency
      }
    }

    dependencyPaths.forEach { path ->
      val depPath = mapDependencyPath(parser, path, project)
      result += project.dependencies.create(depPath.dependency)
    }

    return result
  }

  private fun mapDependencyPath(
    parser: NotationParser<Any, Any>,
    path: String,
    project: Project
  ): DependencyType {
    val converted = parser.parseNotation(path)

    if (converted is File) {
      val file = if (!converted.isAbsolute) File(project.projectDir, converted.path) else converted

      check(file.exists()) { "Invalid path string: \"$path\". Path does not exist." }

      return when {
        file.isDirectory -> Directory(project, path)
        file.isJar -> Jar(project, file.path)
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
      // assume it's a possible external dependency and let Gradle sort it out later...
      return Path(project, path)
    }
  }

  private fun isURL(uri: URI) =
      try {
        uri.toURL()
        true
      } catch (e: Exception) {
        false
      }

  sealed class DependencyType(val project: Project) {
    abstract val path: String
    abstract val dependency: Any

    class Directory(project: Project, override val path: String) : DependencyType(project) {
      override val dependency: Any
        get() = project.files(path)
    }

    class Jar(project: Project, override val path: String) : DependencyType(project) {
      override val dependency: Any
        get() = project.files(path)
    }

    class Path(project: Project, override val path: String) : DependencyType(project) {
      override val dependency: Any
        get() = path
    }
  }
}

private val File.isJar
  get() = path.endsWith(".jar")