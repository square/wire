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
import okio.Path.Companion.toPath
import org.gradle.api.file.FileTree
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
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files

@CacheableTask
open class WireTask : SourceTask() {

  @get:OutputDirectories
  var outputDirectories: List<File>? = null

  @get:Input
  var pluginVersion: String = VERSION

  @get:Internal
  internal val sourceInput = project.objects.listProperty(Location::class.java)

  @get:Internal
  internal val protoInput = project.objects.listProperty(Location::class.java)

  @Input
  lateinit var roots: List<String>

  @Input
  lateinit var prunes: List<String>

  @Input
  lateinit var moves: List<Move>

  @Input
  @Optional
  var sinceVersion: String? = null

  @Input
  @Optional
  var untilVersion: String? = null

  @Input
  @Optional
  var onlyVersion: String? = null

  @Input
  @Optional
  var rules: String? = null

  @Input
  lateinit var targets: List<Target>

  @Input
  var permitPackageCycles: Boolean = false

  @Input
  lateinit var inputFiles: List<String>

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
      logger.debug("roots: $roots")
      logger.debug("prunes: $prunes")
      logger.debug("rules: $rules")
      logger.debug("targets: $targets")
    }

    inputFiles.forEach {
      val fileObj = project.file(it)
      check(fileObj.exists()) {
        "Invalid path string: \"${fileObj.path}\". Path does not exist."
      }
    }

    val wireRun = WireRun(
      sourcePath = sourceInput.map { expandJarGlobs(it) }.get(),
      protoPath = protoInput.get(),
      treeShakingRoots = if (roots.isEmpty()) includes else roots,
      treeShakingRubbish = if (prunes.isEmpty()) excludes else prunes,
      moves = moves.map { it.toTypeMoverMove() },
      sinceVersion = sinceVersion,
      untilVersion = untilVersion,
      onlyVersion = onlyVersion,
      targets = targets.map { target -> target.copyTarget(outDirectory = project.file(target.outDirectory).path) },
      permitPackageCycles = permitPackageCycles
    )

    for (target in targets) {
      if (project.file(target.outDirectory).path.startsWith(project.buildDir.path)) {
        project.file(target.outDirectory).deleteRecursively()
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

  /**
   * Expands all glob expressions occurring in [srcInputs] whose base location is a `.jar` file.
   *
   * Note that we cannot do it at configuration time because some `.jar` files might be remote.
   */
  private fun expandJarGlobs(srcInputs: List<Location>): List<Location> {
    val (jarInputs, nonJarInputs) = srcInputs.partition { it.base.endsWith(".jar") }

    val expandedJarInputs = mutableSetOf<Location>()

    val jarInputsByBase = jarInputs.groupBy { it.base }
    for ((jar, inputs) in jarInputsByBase) {
      val (globInputs, inlinedInputs) = inputs.partition { it.path.contains('*') }
      expandedJarInputs += inlinedInputs

      if (globInputs.isEmpty()) {
        continue
      }

      val jarPath = jar.toPath().toNioPath()
      FileSystems.newFileSystem(jarPath, null as ClassLoader?).use { jarFs ->
        val matchers = globInputs.map { jarFs.getPathMatcher("glob:${it.path}") }

        for (root in jarFs.rootDirectories) {
          Files.walk(root)
            .map { path ->
              // The ZipFileProvider implements glob syntax by transforming it to a regex, including
              // anchors at each end. The file-tree walk yields absolute paths, but the paths we use
              // in the Wire Gradle plugin are omitting the leading `/`. We thus need to strip the
              // leading '/' in order for our matchers to work.
              if (path.isAbsolute) {
                path.fileSystem.getPath(path.toString().substring(1))
              } else {
                path
              }
            }
            .filter { path -> matchers.any { matcher -> matcher.matches(path) } }
            .map { path -> Location.get(jar, path.toString()) }
            .forEach { expandedJarInputs += it }
        }
      }
    }

    return nonJarInputs + expandedJarInputs
  }
}
