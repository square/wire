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

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.util.ConfigureUtil
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
  fun sourcePath(closure: Closure<SourceDirectorySet>) {
    val sourceTree = sourceDirectorySetFactory.create("source-tree", "Source path tree")
    sourceTree.filter.include("**/*.proto")
    ConfigureUtil.configure<SourceDirectorySet>(closure, sourceTree)
    sourceTrees.add(sourceTree)
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
  fun protoPath(closure: Closure<SourceDirectorySet>) {
    val protoTree = sourceDirectorySetFactory.create("proto-tree", "Proto path tree")
    protoTree.filter.include("**/*.proto")
    ConfigureUtil.configure<SourceDirectorySet>(closure, protoTree)
    protoTrees.add(protoTree)
  }

  fun java(action: Action<JavaTarget>) {
    javaTarget = objectFactory.newInstance(JavaTarget::class.java)
    action.execute(javaTarget!!)
  }

  fun kotlin(action: Action<KotlinTarget>) {
    kotlinTarget = objectFactory.newInstance(KotlinTarget::class.java)
    action.execute(kotlinTarget!!)
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
  }
}
