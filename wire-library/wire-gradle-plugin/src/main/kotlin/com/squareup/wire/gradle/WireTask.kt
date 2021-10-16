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
import com.squareup.wire.gradle.internal.GradleWireLogger
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.Target
import com.squareup.wire.schema.WireRun
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

@CacheableTask
abstract class WireTask @Inject constructor(objects: ObjectFactory) : SourceTask() {

  @get:OutputDirectories
  abstract val outputDirectories: ConfigurableFileCollection

  @get:Input
  val pluginVersion: Property<String> = objects.property(String::class.java)
    .convention(VERSION)

  @get:Internal
  internal abstract val sourceInput: ListProperty<Location>

  @get:Internal
  internal abstract val protoInput: ListProperty<Location>

  @get:Input
  abstract val roots: ListProperty<String>

  @get:Input
  abstract val prunes: ListProperty<String>

  @get:Input
  abstract val moves: ListProperty<Move>

  @get:Input
  @get:Optional
  abstract val sinceVersion: Property<String>

  @get:Input
  @get:Optional
  abstract val untilVersion: Property<String>

  @get:Input
  @get:Optional
  abstract val onlyVersion: Property<String>

  @get:Input
  @get:Optional
  abstract val rules: Property<String>

  @get:Input
  abstract val targets: ListProperty<Target>

  @get:Input
  val permitPackageCycles: Property<Boolean> = objects.property(Boolean::class.java)
    .convention(false)

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val inputFiles: ConfigurableFileCollection

  @get:Internal
  abstract val projectDirProperty: DirectoryProperty

  @get:Internal
  abstract val buildDirProperty: DirectoryProperty

  @TaskAction
  fun generateWireFiles() {
    val includes = mutableListOf<String>()
    val excludes = mutableListOf<String>()

    val projectDir = projectDirProperty.get()
    rules.orNull?.let {
      projectDir
        .file(it)
        .asFile
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
      logger.debug("roots: ${roots.orNull}")
      logger.debug("prunes: ${prunes.orNull}")
      logger.debug("rules: ${rules.orNull}")
      logger.debug("targets: ${targets.orNull}")
    }

    inputFiles.forEach { fileObj ->
      check(fileObj.exists()) {
        "Invalid path string: \"${fileObj.path}\". Path does not exist."
      }
    }

    val allTargets = targets.get()
    val wireRun = WireRun(
      sourcePath = sourceInput.get(),
      protoPath = protoInput.get(),
      treeShakingRoots = roots.get().ifEmpty { includes },
      treeShakingRubbish = prunes.get().ifEmpty { excludes },
      moves = moves.get().map { it.toTypeMoverMove() },
      sinceVersion = sinceVersion.orNull,
      untilVersion = untilVersion.orNull,
      onlyVersion = onlyVersion.orNull,
      targets = allTargets.map { target ->
        target.copyTarget(outDirectory = projectDir.file(target.outDirectory).asFile.path)
      },
      permitPackageCycles = permitPackageCycles.get()
    )

    val buildDir = buildDirProperty.get()
    for (target in allTargets) {
      if (projectDir.file(target.outDirectory).asFile.path.startsWith(buildDir.asFile.path)) {
        projectDir.file(target.outDirectory).asFile.deleteRecursively()
      }
    }
    wireRun.execute(logger = GradleWireLogger)
  }

  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  override fun getSource(): FileTree {
    return super.getSource()
  }
}
