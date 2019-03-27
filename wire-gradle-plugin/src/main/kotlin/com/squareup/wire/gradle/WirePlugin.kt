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
import com.squareup.wire.schema.Target
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.internal.file.FileOrUriNotationConverter
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_DSL_NAME
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_JS_DSL_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.net.URI
import javax.inject.Inject

class WirePlugin @Inject constructor(
  private val sourceDirectorySetFactory: SourceDirectorySetFactory
) : Plugin<Project> {
  private var kotlin = false
  private var java = false
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

    val sourcePaths =
      if (extension.sourcePaths.isNotEmpty() || extension.sourceTrees.isNotEmpty()) {
        mergeDependencyPaths(project, extension.sourcePaths, extension.sourceTrees)
      } else {
        mergeDependencyPaths(project, setOf("src/main/proto"))
      }
    sourcePaths.forEach {
      sourceConfiguration.dependencies.add(project.dependencies.create(it))
    }

    val protoConfiguration = project.configurations.create("wireProtoDependencies")

    if (extension.protoPaths.isNotEmpty() || extension.protoTrees.isNotEmpty()) {
      val allPaths = mergeDependencyPaths(project, extension.protoPaths, extension.protoTrees)
      allPaths.forEach { path ->
        protoConfiguration.dependencies.add(project.dependencies.create(path))
      }
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
          javaInterop = target.javaInterop
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

  private fun mergeDependencyPaths(
    project: Project,
    dependencyPaths: Set<String>,
    dependencyTrees: Set<SourceDirectorySet> = emptySet()
  ): List<Any> {
    val allPaths = dependencyTrees.toMutableList<Any>()

    val parser = FileOrUriNotationConverter.parser()
    dependencyPaths.forEach { path ->
      val converted = parser.parseNotation(path)

      if (converted is File) {
        val file =
          if (!converted.isAbsolute) File(project.projectDir, converted.path) else converted
        if (!file.exists()) {
          throw IllegalArgumentException(
              "Invalid path string: \"$path\". Path does not exist."
          )
        }
        if (file.isDirectory) {
          allPaths += project.files(path) as Any
        } else {
          throw IllegalArgumentException(
              "Invalid path string: \"$path\". For individual files, use the closure syntax."
          )
        }
      } else if (converted is URI && isURL(converted)) {
        throw IllegalArgumentException(
            "Invalid path string: \"$path\". URL dependencies are not allowed."
        )
      } else {
        // assume it's a possible external dependency and let Gradle sort it out later...
        allPaths += path
      }
    }

    return allPaths
  }

  private fun isURL(uri: URI) =
    try {
      uri.toURL()
      true
    } catch (e: Exception) {
      false
    }
}