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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional

open class WireExtension(project: Project) {
  private val objectFactory = project.objects

  internal val sourcePaths = mutableSetOf<String>()
  internal val protoPaths = mutableSetOf<String>()
  internal val sourceTrees = mutableSetOf<SourceDirectorySet>()
  internal val protoTrees = mutableSetOf<SourceDirectorySet>()
  internal val sourceJars = mutableSetOf<ProtoRootSet>()
  internal val protoJars = mutableSetOf<ProtoRootSet>()
  internal val roots = mutableSetOf<String>()
  internal val prunes = mutableSetOf<String>()
  internal var since: String? = null
  internal var until: String? = null

  @Input
  @Optional
  fun roots() = roots.toSet()

  /**
   * See [com.squareup.wire.schema.WireRun.treeShakingRoots]
   */
  fun root(vararg roots: String) {
    this.roots.addAll(roots)
  }

  @Input
  @Optional
  fun prunes() = prunes.toSet()

  /**
   * See [com.squareup.wire.schema.WireRun.treeShakingRubbish]
   */
  fun prune(vararg prunes: String) {
    this.prunes.addAll(prunes)
  }

  @Input
  @Optional
  fun sinceVersion() = since

  /**
   * See [com.squareup.wire.schema.WireRun.since]
   */
  fun sinceVersion(since: String) {
    this.since = since
  }

  @Input
  @Optional
  fun untilVersion() = until

  /**
   * See [com.squareup.wire.schema.WireRun.until]
   */
  fun untilVersion(until: String) {
    this.until = until
  }

  /**
   * Sets since and until to the same version.
   */
  fun version(version: String) {
    this.since = version
    this.until = version
  }

  /**
   * A user-provided file listing [roots] and [prunes]
   */
  @get:Input
  @get:Optional
  var rules: String? = null

  /** Specified what types to output where. Maps to [com.squareup.wire.schema.Target] */
  @get:Input
  val outputs = mutableListOf<WireOutput>()

  /** Not supported, do not use. */
  @get:Input
  @get:Optional
  var proto3Preview: String? = null

  @InputFiles
  @Optional
  fun getSourcePaths() = sourcePaths.toSet()

  @InputFiles
  @Optional
  fun getSourceTrees() = sourceTrees.toSet()

  @InputFiles
  @Optional
  fun getSourceJars() = sourceJars.toSet()

  /**
   * Source paths for local jars and directories, as well as remote binary dependencies
   */
  fun sourcePath(vararg sourcePaths: String) {
    this.sourcePaths.addAll(sourcePaths)
  }

  /**
   * Source paths for local file trees, backed by a [org.gradle.api.file.SourceDirectorySet]
   * Must provide at least a [org.gradle.api.file.SourceDirectorySet.srcDir]
   */
  fun sourcePath(action: Action<ProtoRootSet>) {
    populateRootSets(action, sourceTrees, sourceJars, "source-tree")
  }

  @InputFiles
  @Optional
  fun getProtoPaths(): Set<String> {
    return protoPaths
  }

  @InputFiles
  @Optional
  fun getProtoTrees(): Set<SourceDirectorySet> {
    return protoTrees
  }

  @InputFiles
  @Optional
  fun getProtoJars(): Set<ProtoRootSet> {
    return protoJars
  }

  /**
   * Proto paths for local jars and directories, as well as remote binary dependencies
   */
  fun protoPath(vararg protoPaths: String) {
    this.protoPaths.addAll(protoPaths)
  }

  /**
   * Proto paths for local file trees, backed by a [org.gradle.api.file.SourceDirectorySet]
   * Must provide at least a [org.gradle.api.file.SourceDirectorySet.srcDir]
   */
  fun protoPath(action: Action<ProtoRootSet>) {
    populateRootSets(action, protoTrees, protoJars, "proto-tree")
  }

  private fun populateRootSets(
    action: Action<ProtoRootSet>,
    sourceTrees: MutableSet<SourceDirectorySet>,
    sourceJars: MutableSet<ProtoRootSet>,
    name: String
  ) {
    val protoRootSet = objectFactory.newInstance(ProtoRootSet::class.java)
    action.execute(protoRootSet)

    val hasSrcDirs = protoRootSet.srcDirs.isNotEmpty()
    val hasSrcJar = protoRootSet.srcJar != null

    check(!hasSrcDirs || !hasSrcJar) {
      "Cannot set both srcDirs and srcJars in the same protoPath closure"
    }

    if (hasSrcDirs) {
      // map to SourceDirectorySet which does the work for us!
      val protoTree = objectFactory.sourceDirectorySet(name, "Wire proto sources for $name.")
      protoTree.srcDirs(protoRootSet.srcDirs)
      protoTree.filter.include("**/*.proto")
      protoTree.filter.include(protoRootSet.includes)
      sourceTrees.add(protoTree)
    }

    if (hasSrcJar) {
      sourceJars.add(protoRootSet)
    }
  }

  fun java(action: Action<JavaOutput>) {
    val javaOutput = objectFactory.newInstance(JavaOutput::class.java)
    action.execute(javaOutput)
    outputs += javaOutput
  }

  fun kotlin(action: Action<KotlinOutput>) {
    val kotlinOutput = objectFactory.newInstance(KotlinOutput::class.java)
    action.execute(kotlinOutput)
    outputs += kotlinOutput
  }

  fun proto(action: Action<ProtoOutput>) {
    val protoOutput = objectFactory.newInstance(ProtoOutput::class.java)
    action.execute(protoOutput)
    outputs += protoOutput
  }

  fun custom(action: Action<CustomOutput>) {
    val customOutput = objectFactory.newInstance(CustomOutput::class.java)
    action.execute(customOutput)
    outputs += customOutput
  }

  open class ProtoRootSet {
    val srcDirs = mutableListOf<String>()
    var srcJar: String? = null
    val includes = mutableListOf<String>()

    fun srcDir(dir: String) {
      srcDirs += dir
    }

    fun srcDirs(vararg dirs: String) {
      srcDirs += dirs
    }

    fun srcJar(jar: String) {
      srcJar = jar
    }

    fun include(vararg includePaths: String) {
      includes += includePaths
    }
  }
}
