/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.gradle

import com.squareup.wire.DryRunFileSystem
import com.squareup.wire.VERSION
import com.squareup.wire.gradle.internal.GradleWireLogger
import com.squareup.wire.schema.EventListener
import com.squareup.wire.schema.Target
import com.squareup.wire.schema.WireRun
import javax.inject.Inject
import okio.FileSystem
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class WireTask @Inject constructor(
  objects: ObjectFactory,
  private val fileOperations: FileOperations,
) : SourceTask() {

  @get:OutputDirectories
  abstract val outputDirectories: ConfigurableFileCollection

  /** This input only exists to signal task dependencies. The files are read via [source]. */
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val protoSourceConfiguration: ConfigurableFileCollection

  /** Same as above: files are read via [source]. */
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val protoPathConfiguration: ConfigurableFileCollection

  /** Same as above: files are read via [source]. */
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val projectDependenciesJvmConfiguration: ConfigurableFileCollection

  @get:Optional
  @get:OutputDirectory
  abstract val protoLibraryOutput: DirectoryProperty

  @get:Input
  val pluginVersion: Property<String> = objects.property(String::class.java)
    .convention(VERSION)

  @get:Input
  internal abstract val sourceInput: ListProperty<InputLocation>

  @get:Input
  internal abstract val protoInput: ListProperty<InputLocation>

  @get:Input
  abstract val roots: ListProperty<String>

  @get:Input
  abstract val prunes: ListProperty<String>

  @get:Input
  abstract val moves: ListProperty<Move>

  @get:Input
  abstract val opaques: ListProperty<String>

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

  @get:Input
  val loadExhaustively: Property<Boolean> = objects.property(Boolean::class.java)
    .convention(false)

  @get:Internal
  abstract val projectDirProperty: DirectoryProperty

  @get:Internal
  abstract val buildDirProperty: DirectoryProperty

  @get:Input
  val dryRun: Property<Boolean> = objects.property(Boolean::class.java)
    .convention(false)

  @get:Input
  val rejectUnusedRootsOrPrunes: Property<Boolean> = objects.property(Boolean::class.java)
    .convention(false)

  @get:Input
  abstract val eventListenerFactories: ListProperty<EventListener.Factory>

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

    val projectDirAsFile = projectDir.asFile
    val allTargets = targets.get()
    val wireRun = WireRun(
      sourcePath = sourceInput.get().flatMap { it.toLocations(fileOperations, projectDirAsFile) },
      protoPath = protoInput.get().flatMap { it.toLocations(fileOperations, projectDirAsFile) },
      treeShakingRoots = roots.get().ifEmpty { includes },
      treeShakingRubbish = prunes.get().ifEmpty { excludes },
      moves = moves.get().map { it.toTypeMoverMove() },
      sinceVersion = sinceVersion.orNull,
      untilVersion = untilVersion.orNull,
      onlyVersion = onlyVersion.orNull,
      targets = allTargets.map { target ->
        target.copyTarget(outDirectory = projectDir.file(target.outDirectory).asFile.path)
      },
      permitPackageCycles = permitPackageCycles.get(),
      loadExhaustively = loadExhaustively.get(),
      rejectUnusedRootsOrPrunes = rejectUnusedRootsOrPrunes.get(),
      eventListeners = eventListenerFactories.get().map(EventListener.Factory::create),
      opaqueTypes = opaques.get(),
    )

    val buildDir = buildDirProperty.get()
    for (target in allTargets) {
      if (projectDir.file(target.outDirectory).asFile.path.startsWith(buildDir.asFile.path)) {
        projectDir.file(target.outDirectory).asFile.deleteRecursively()
      }
    }
    wireRun.execute(
      fs = if (dryRun.get()) DryRunFileSystem(FileSystem.SYSTEM) else FileSystem.SYSTEM,
      logger = GradleWireLogger,
    )
  }

  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  override fun getSource(): FileTree {
    return super.getSource()
  }
}
