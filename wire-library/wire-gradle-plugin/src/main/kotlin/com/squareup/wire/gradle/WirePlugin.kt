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
import com.squareup.wire.gradle.kotlin.sourceRoots
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet

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
      val libraryProtoSources = File("${buildDir}/wire/proto-sources")
      val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
      sourceSets.getByName("main") { main: SourceSet ->
        main.resources.srcDir(libraryProtoSources)
      }
      extension.proto { protoOutput ->
        protoOutput.out = libraryProtoSources.path
      }
    }

    val outputs = extension.outputs.toMutableList()
    if (outputs.isEmpty()) {
      outputs.add(JavaOutput())
    }

    val hasJavaOutput = outputs.any { it is JavaOutput }
    val hasKotlinOutput = outputs.any { it is KotlinOutput }
    check(!hasKotlinOutput || kotlin.get()) {
      "Wire Gradle plugin applied in " +
          "project '${project.path}' but no supported Kotlin plugin was found"
    }

    addWireRuntimeDependency(hasJavaOutput, hasKotlinOutput)

    for (output in outputs) {
      if (output.out == null) {
        output.out = "${project.buildDir}/generated/source/wire"
      } else {
        output.out = project.file(output.out!!).path
      }
    }
    val generatedSourcesDirectories = outputs.map { output -> File(output.out!!) }.toSet()
    // TODO(benoit) Do we want to delete all output directories? It creates problem if protos are
    //  generated in a shared folder BUT there would no be any forgotten code left over code when
    //  the proto schema shrinks.
    // for (generatedSourcesDirectory in generatedSourcesDirectories) {
    //   generatedSourcesDirectory.deleteRecursively()
    // }

    val sourceInput = WireInput(project.configurations.named("protoSource"))
    sourceInput.addTrees(project, extension.sourceTrees)
    sourceInput.addJars(project, extension.sourceJars)
    sourceInput.addProjects(project, extension.sourceProjects)
    sourceInput.addPaths(project, extension.sourcePaths)
    if (sourceInput.dependencies.isEmpty()) {
      sourceInput.addPaths(project, setOf("src/main/proto"))
    }

    val protoInput = WireInput(project.configurations.named("protoPath"))
    protoInput.addTrees(project, extension.protoTrees)
    protoInput.addJars(project, extension.protoJars)
    protoInput.addProjects(project, extension.protoProjects)
    protoInput.addPaths(project, extension.protoPaths)

    val inputFiles = mutableListOf<File>()
    inputFiles.addAll(sourceInput.inputFiles)
    inputFiles.addAll(protoInput.inputFiles)

    val targets = outputs.map { it.toTarget() }

    val projectDependencies = (sourceInput.dependencies + protoInput.dependencies)
      .filterIsInstance<ProjectDependency>()

    val common = sources.singleOrNull { it.type == KotlinPlatformType.common }
    for (generatedSourcesDirectory in generatedSourcesDirectories) {
      common?.sourceDirectorySet
        ?.srcDir(generatedSourcesDirectory.toRelativeString(project.projectDir))
    }
    sources.forEach { source ->
      if (common == null) {
        for (generatedSourcesDirectory in generatedSourcesDirectories) {
          source.sourceDirectorySet
            .srcDir(generatedSourcesDirectory.toRelativeString(project.projectDir))
        }
      }

      val taskName = "generate${source.name.capitalize()}Protos"
      val task = project.tasks.register(taskName, WireTask::class.java) { task: WireTask ->
        task.group = GROUP
        task.description = "Generate protobuf implementation for ${source.name}"
        task.source(sourceInput.configuration)

        if (task.logger.isDebugEnabled) {
          sourceInput.debug(task.logger)
          protoInput.debug(task.logger)
        }
        task.outputDirectories = outputs.map { output -> File(output.out!!) }
        task.sourceInput.set(sourceInput.toLocations())
        task.protoInput.set(protoInput.toLocations())
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
      for (output in outputs) {
        // TODO(Benoit) Why do I need this?
        if (output is JavaOutput && !android.get()) {
          (project.tasks.named("compileJava") as TaskProvider<JavaCompile>).configure {
            it.source(output.out!!)
          }
        }
      }
    }
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
        project.configurations.getByName(sourceSet.apiConfigurationName).dependencies.add(
            project.dependencies.create("com.squareup.wire:wire-runtime-multiplatform:$VERSION")
        )
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

  internal companion object {
    const val PARENT_TASK = "generateProtos"
    const val GROUP = "wire"
  }
}
