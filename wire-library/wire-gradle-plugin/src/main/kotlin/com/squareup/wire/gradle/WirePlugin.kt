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
import com.squareup.wire.gradle.internal.libraryProtoOutputPath
import com.squareup.wire.gradle.internal.targetDefaultOutputPath
import com.squareup.wire.gradle.kotlin.Source
import com.squareup.wire.gradle.kotlin.sourceRoots
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.internal.file.FileOrUriNotationConverter
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class WirePlugin : Plugin<Project> {
  private val android = AtomicBoolean(false)
  private val java = AtomicBoolean(false)
  private val kotlin = AtomicBoolean(false)

  private lateinit var extension: WireExtension
  internal lateinit var project: Project

  private val sources by lazy { this.sourceRoots(kotlin = kotlin.get(), java = java.get()) }

  override fun apply(project: Project) {
    this.extension = project.extensions.create("wire", WireExtension::class.java, project)
    this.project = project

    project.configurations.create("protoSource")
      .also {
        it.isCanBeConsumed = false
        it.isTransitive = false
      }
    project.configurations.create("protoPath")
      .also {
        it.isCanBeConsumed = false
        it.isTransitive = false
      }

    val androidPluginHandler = { _: Plugin<*> ->
      android.set(true)
      project.afterEvaluate {
        project.setupWireTasks(afterAndroid = true)
      }
    }
    project.plugins.withId("com.android.application", androidPluginHandler)
    project.plugins.withId("com.android.library", androidPluginHandler)
    project.plugins.withId("com.android.instantapp", androidPluginHandler)
    project.plugins.withId("com.android.feature", androidPluginHandler)
    project.plugins.withId("com.android.dynamic-feature", androidPluginHandler)

    val kotlinPluginHandler = { _: Plugin<*> -> kotlin.set(true) }
    project.plugins.withId("org.jetbrains.kotlin.multiplatform", kotlinPluginHandler)
    project.plugins.withId("org.jetbrains.kotlin.android", kotlinPluginHandler)
    project.plugins.withId("org.jetbrains.kotlin.jvm", kotlinPluginHandler)
    project.plugins.withId("kotlin2js", kotlinPluginHandler)

    val javaPluginHandler = { _: Plugin<*> -> java.set(true) }
    project.plugins.withId("java", javaPluginHandler)
    project.plugins.withId("java-library", javaPluginHandler)

    project.afterEvaluate {
      project.setupWireTasks(afterAndroid = false)
    }
  }

  private fun Project.setupWireTasks(afterAndroid: Boolean) {
    if (android.get() && !afterAndroid) return

    check(android.get() || java.get() || kotlin.get()) {
      "Missing either the Java, Kotlin, or Android plugin"
    }

    project.tasks.register(PARENT_TASK) {
      it.group = GROUP
      it.description = "Aggregation task which runs every generation task for every given source"
    }

    if (extension.protoLibrary) {
      val libraryProtoSources = File(project.libraryProtoOutputPath())
      val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
      sourceSets.getByName("main") { main: SourceSet ->
        main.resources.srcDir(libraryProtoSources)
      }
      extension.proto { protoOutput ->
        protoOutput.out = libraryProtoSources.path
      }
    }

    val outputs = extension.outputs.ifEmpty { listOf(JavaOutput()) }
    val hasJavaOutput = outputs.any { it is JavaOutput }
    val hasKotlinOutput = outputs.any { it is KotlinOutput }
    check(!hasKotlinOutput || kotlin.get()) {
      "Wire Gradle plugin applied in " +
          "project '${project.path}' but no supported Kotlin plugin was found"
    }

    addWireRuntimeDependency(hasJavaOutput, hasKotlinOutput)

    val protoPathInput = WireInput(project.configurations.getByName("protoPath"))
    protoPathInput.addTrees(project, extension.protoTrees)
    protoPathInput.addJars(project, extension.protoJars)
    protoPathInput.addProjects(project, extension.protoProjects)
    protoPathInput.addPaths(project, extension.protoPaths)

    sources.forEach { source ->
      val protoSourceInput = WireInput(project.configurations.getByName("protoSource").copy())
      protoSourceInput.addTrees(project, extension.sourceTrees)
      protoSourceInput.addJars(project, extension.sourceJars)
      protoSourceInput.addProjects(project, extension.sourceProjects)
      protoSourceInput.addPaths(project, extension.sourcePaths)
      // TODO(Benoit) Should we add our default source folders everytime? Right now, someone could
      //  not combine a custom protoSource with our default using variants.
      if (protoSourceInput.dependencies.isEmpty()) {
        protoSourceInput.addPaths(project, defaultSourceFolders(source))
      }

      val inputFiles = mutableListOf<File>()
      inputFiles.addAll(protoSourceInput.inputFiles)
      inputFiles.addAll(protoPathInput.inputFiles)

      val projectDependencies = (protoSourceInput.dependencies + protoPathInput.dependencies)
        .filterIsInstance<ProjectDependency>()

      val targets = outputs.map { output ->
        output.toTarget(
          if (output.out == null) {
            source.outputDir(project).path
          } else {
            project.file(output.out!!).path
          }
        )
      }
      val generatedSourcesDirectories = targets.map { target -> File(target.outDirectory) }.toSet()

      // Both the JavaCompile and KotlinCompile tasks might already have been configured by now.
      // Even though we add the Wire output directories into the corresponding sourceSets, the
      // compilation tasks won't know about them so we fix that here.
      if (hasJavaOutput) {
        project.tasks.matching { it.name == "compileJava" }.configureEach {
          (it as JavaCompile).source(generatedSourcesDirectories)
        }
      }
      if (hasJavaOutput || hasKotlinOutput) {
        project.tasks.matching {
          it.name == "compileKotlin" || it.name == "compile${source.name.capitalize()}Kotlin"
        }.configureEach {
          // Note that [KotlinCompile.source] will process files but will ignore strings.
          (it as KotlinCompile).source(generatedSourcesDirectories)
        }
      }

      // TODO: pair up generatedSourceDirectories with their targets so we can be precise.
      for (generatedSourcesDirectory in generatedSourcesDirectories) {
        val relativePath = generatedSourcesDirectory.toRelativeString(project.projectDir)
        if (hasJavaOutput) {
          source.javaSourceDirectorySet?.srcDir(relativePath)
        }
        if (hasKotlinOutput) {
          source.kotlinSourceDirectorySet?.srcDir(relativePath)
        }
      }

      val taskName = "generate${source.name.capitalize()}Protos"
      val task = project.tasks.register(taskName, WireTask::class.java) { task: WireTask ->
        task.group = GROUP
        task.description = "Generate protobuf implementation for ${source.name}"
        task.source(protoSourceInput.configuration)

        if (task.logger.isDebugEnabled) {
          protoSourceInput.debug(task.logger)
          protoPathInput.debug(task.logger)
        }
        task.outputDirectories = targets.map { target -> File(target.outDirectory) }
        task.sourceInput.set(protoSourceInput.toLocations(project.providers))
        task.protoInput.set(protoPathInput.toLocations(project.providers))
        task.roots = extension.roots.toList()
        task.prunes = extension.prunes.toList()
        task.moves = extension.moves.toList()
        task.sinceVersion = extension.sinceVersion
        task.untilVersion = extension.untilVersion
        task.onlyVersion = extension.onlyVersion
        task.rules = extension.rules
        task.targets = targets
        task.permitPackageCycles = extension.permitPackageCycles

        task.inputFiles = inputFiles

        for (projectDependency in projectDependencies) {
          task.dependsOn(projectDependency)
        }
      }

      project.tasks.named(PARENT_TASK).configure {
        it.dependsOn(task)
      }
      if (extension.protoLibrary) {
        project.tasks.named("processResources").configure {
          it.dependsOn(task)
        }
      }

      source.registerTaskDependency(task)
    }
  }

  private fun Source.outputDir(project: Project): File {
    return if (sources.size > 1) File(project.targetDefaultOutputPath(), name)
    else File(project.targetDefaultOutputPath())
  }

  private fun Project.addWireRuntimeDependency(
    hasJavaOutput: Boolean,
    hasKotlinOutput: Boolean
  ) {
    val isMultiplatform = project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
    // Only add the dependency for Java and Kotlin.
    if (hasJavaOutput || hasKotlinOutput) {
      if (isMultiplatform) {
        val sourceSets =
          project.extensions.getByType(KotlinMultiplatformExtension::class.java).sourceSets
        val sourceSet = (sourceSets.getByName("commonMain") as DefaultKotlinSourceSet)
        project.configurations.getByName(sourceSet.apiConfigurationName).dependencies
          .add(project.dependencies.create("com.squareup.wire:wire-runtime-multiplatform:$VERSION"))
      } else {
        try {
          project.configurations.getByName("api").dependencies
            .add(project.dependencies.create("com.squareup.wire:wire-runtime:$VERSION"))
        } catch (_: UnknownConfigurationException) {
          // No `api` configuration on Java applications.
          project.configurations.getByName("implementation").dependencies
            .add(project.dependencies.create("com.squareup.wire:wire-runtime:$VERSION"))
        }
      }
    }
  }

  private fun defaultSourceFolders(source: Source): Set<String> {
    val parser = FileOrUriNotationConverter.parser()
    return source.sourceSets
      .map { "src/$it/proto" }
      .filter { path ->
        val converted = parser.parseNotation(path) as File
        val file =
          if (!converted.isAbsolute) File(project.projectDir, converted.path) else converted
        return@filter file.exists()
      }
      .toSet()
  }

  internal companion object {
    const val PARENT_TASK = "generateProtos"
    const val GROUP = "wire"
  }
}
