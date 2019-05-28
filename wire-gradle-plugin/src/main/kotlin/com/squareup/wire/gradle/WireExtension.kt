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
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import javax.inject.Inject

open class WireExtension(
  project: Project,
  private val sourceDirectorySetFactory: SourceDirectorySetFactory
) {
  private val objectFactory = project.objects

  internal val sourcePaths = mutableSetOf<String>()
  internal val protoPaths = mutableSetOf<String>()
  internal val sourceTrees = mutableSetOf<SourceDirectorySet>()
  internal val protoTrees = mutableSetOf<SourceDirectorySet>()
  internal val sourceJars = mutableSetOf<ProtoRootSet>()
  internal val protoJars = mutableSetOf<ProtoRootSet>()
  internal val roots = mutableSetOf<String>()
  internal val prunes = mutableSetOf<String>()

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

  /**
   * A user-provided file listing [roots] and [prunes]
   */
  @get:Input
  @get:Optional
  var rules: String? = null

  /**
   * Defines java-specific settings for [com.squareup.wire.schema.WireRun.targets]
   * Maps to [com.squareup.wire.schema.Target.JavaTarget]
   */
  @get:Input
  @get:Optional
  var javaTarget: JavaTarget? = null

  /**
   * Defines kotlin-specific settings for [com.squareup.wire.schema.WireRun.targets]
   * Maps to [com.squareup.wire.schema.Target.KotlinTarget]
   */
  @get:Input
  @get:Optional
  var kotlinTarget: KotlinTarget? = null

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
      val protoTree = sourceDirectorySetFactory.create(name)
      protoTree.srcDirs(protoRootSet.srcDirs)
      protoTree.filter.include("**/*.proto")
      protoTree.filter.include(protoRootSet.includes)
      sourceTrees.add(protoTree)
    }

    if (hasSrcJar) {
      sourceJars.add(protoRootSet)
    }
  }

  fun java(action: Action<JavaTarget>) {
    javaTarget = objectFactory.newInstance(JavaTarget::class.java)
    action.execute(javaTarget!!)
  }

  fun kotlin(action: Action<KotlinTarget>) {
    kotlinTarget = objectFactory.newInstance(KotlinTarget::class.java)
    action.execute(kotlinTarget!!)
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

  open class JavaTarget @Inject constructor() {
    var elements: List<String>? = null
    var out: String? = null
    var android: Boolean = false
    var androidAnnotations: Boolean = false
    var compact: Boolean = false
  }

  open class KotlinTarget @Inject constructor() {
    var out: String? = null
    var elements: List<String>? = null
    var android: Boolean = false
    var javaInterop: Boolean = false
    var blockingServices: Boolean = false
    var singleMethodServices: Boolean = false
  }
}
