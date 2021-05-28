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
import java.io.File
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
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.asSequence

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
  lateinit var inputFiles: List<File>

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
      check(it.exists()) {
        "Invalid path string: \"${it.path}\". Path does not exist."
      }
    }

    val wireRun = WireRun(
        sourcePath = sourceInput.map { expandSrcJarWildcards(it) }.get(),
        protoPath = protoInput.get(),
        treeShakingRoots = if (roots.isEmpty()) includes else roots,
        treeShakingRubbish = if (prunes.isEmpty()) excludes else prunes,
        moves = moves.map { it.toTypeMoverMove() },
        sinceVersion = sinceVersion,
        untilVersion = untilVersion,
        onlyVersion = onlyVersion,
        targets = targets,
        permitPackageCycles = permitPackageCycles
    )

    for (target in targets) {
      if (target.outDirectory.startsWith(project.buildDir.path)) {
        File(target.outDirectory).deleteRecursively()
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

  private fun expandSrcJarWildcards(srcInputs: List<Location>): List<Location> {
    val (inJar, notInJar) = srcInputs.partition { it.base.endsWith(".jar") }
    val inputsByJar = inJar.groupBy { it.base }

    val expandedJarInputs = mutableListOf<Location>()
    for ((jar, inputs) in inputsByJar) {
      if (inputs.none { it.path.contains('*') }) {
        expandedJarInputs += inputs
        continue
      }

      val (toExpand, expanded) = inputs.partition { it.path.contains('*') }

      val allInputsInJar = expanded.toMutableSet()
      val jarPath = Paths.get(jar)
      FileSystems.newFileSystem(jarPath, null as ClassLoader?).use { jarFs ->
        val matchers = toExpand.map { jarFs.getPathMatcher("glob:${it.path}") }
        val roots = jarFs.rootDirectories.toList()

        // The FS API allows for multiple root directories; I think in practice there's always one?
        for (root in roots) {
          Files.walk(root)
            .asSequence()
            .filter { path ->
              // A quirk of the ZipFileProvider is that it implements glob syntax by transforming it
              // to a regex, including anchors at each end.  The file-tree walk yields fully-rooted paths
              // (starting with '/'), but the include syntax we use in .gradle files is _not_ rooted.
              // We need to strip the leading in order for our matchers to make sense.
              val rootlessPath = path.unroot()
              matchers.any { matcher ->
                matcher.matches(rootlessPath)
              }
            }
            .map { Location.get(jar, it.toString().replace(Regex("^/"), "")) }
            .forEach { allInputsInJar += it }
        }
      }

      expandedJarInputs += allInputsInJar
    }

    return notInJar + expandedJarInputs
  }

  private fun Path.unroot(): Path {
    return if (isAbsolute) {
      fileSystem.getPath(toString().substring(1))
    } else {
      this
    }
  }
}
