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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
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
    val sourceInput = WireInput(project, project.configurations.create("wireSourceDependencies"))
    if (extension.sourcePaths.isNotEmpty() ||
        extension.sourceTrees.isNotEmpty() ||
        extension.sourceJars.isNotEmpty()) {
      sourceInput.addTrees(extension.sourceTrees)
      sourceInput.addJars(extension.sourceJars)
      sourceInput.addPaths(extension.sourcePaths)
    } else {
      sourceInput.addPaths(setOf("src/main/proto"))
    }

    val protoInput = WireInput(project, project.configurations.create("wireProtoDependencies"))
    if (extension.protoPaths.isNotEmpty() ||
        extension.protoTrees.isNotEmpty() ||
        extension.protoJars.isNotEmpty()) {
      protoInput.addTrees(extension.protoTrees)
      protoInput.addJars(extension.protoJars)
      protoInput.addPaths(extension.protoPaths)
    }

    // At this point, all source and proto file references should be set up for Gradle to resolve.

    val outputs = extension.outputs.toMutableList()
    if (outputs.isEmpty()) {
      outputs.add(JavaOutput())
    }

    for (output in outputs) {
      if (output.out == null) {
        output.out = "${project.buildDir}/generated/source/wire"
      } else {
        output.out = project.file(output.out!!).path
      }
    }

    val targets = outputs.map { it.toTarget() }

    val wireTask = project.tasks.register("generateProtos", WireTask::class.java) { task ->
      task.source(sourceInput.configuration)
      task.sourceInput = sourceInput
      task.protoInput = protoInput
      task.roots = extension.roots.toList()
      task.prunes = extension.prunes.toList()
      task.rules = extension.rules
      task.generateOnBuild = extension.generateOnBuild
      task.targets = targets
      task.group = "wire"
      task.description = "Generate Wire protocol buffer implementation for .proto files"
    }

    for (output in outputs) {
      output.applyToProject(project, wireTask, kotlin)
    }
  }
}
