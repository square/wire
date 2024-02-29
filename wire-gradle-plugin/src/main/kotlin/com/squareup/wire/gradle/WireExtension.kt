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

import com.squareup.wire.schema.EventListener
import java.io.File
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.internal.catalog.DelegatingProjectDependency
import org.gradle.api.internal.file.FileOrUriNotationConverter
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderConvertible
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

open class WireExtension(
  private val project: Project,
) {
  private val objectFactory = project.objects

  internal val protoSourceProtoRootSets = mutableListOf<ProtoRootSet>()
  internal val protoPathProtoRootSets = mutableListOf<ProtoRootSet>()
  internal val roots = mutableSetOf<String>()
  internal val prunes = mutableSetOf<String>()
  internal val moves = mutableListOf<Move>()
  internal val opaques = mutableSetOf<String>()
  internal val eventListenerFactories = mutableSetOf<EventListener.Factory>()
  internal val eventListenerFactoryClasses = mutableSetOf<String>()
  internal var onlyVersion: String? = null
  internal var sinceVersion: String? = null
  internal var untilVersion: String? = null
  internal var permitPackageCycles: Boolean = false

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
  fun sinceVersion() = sinceVersion

  /**
   * See [com.squareup.wire.schema.WireRun.sinceVersion]
   */
  fun sinceVersion(sinceVersion: String) {
    this.sinceVersion = sinceVersion
  }

  @Input
  @Optional
  fun untilVersion() = untilVersion

  /**
   * See [com.squareup.wire.schema.WireRun.untilVersion]
   */
  fun untilVersion(untilVersion: String) {
    this.untilVersion = untilVersion
  }

  @Input
  @Optional
  fun onlyVersion() = onlyVersion

  /**
   * See [com.squareup.wire.schema.WireRun.onlyVersion].
   */
  fun onlyVersion(onlyVersion: String) {
    this.onlyVersion = onlyVersion
  }

  @Input
  fun permitPackageCycles() = permitPackageCycles

  /**
   * See [com.squareup.wire.schema.WireRun.permitPackageCycles]
   */
  fun permitPackageCycles(permitPackageCycles: Boolean) {
    this.permitPackageCycles = permitPackageCycles
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

  /**
   * True to emit `.proto` files into the output resources. Use this when your `.jar` file can be
   * used as a library for other proto or Wire projects.
   *
   * Note that only the `.proto` files used in the library will be included, and these files will
   * have tree-shaking applied.
   */
  @get:Input
  @get:Optional
  var protoLibrary = false

  /**
   * If true, Wire will fail if not all [roots] and [prunes] are used when tree-shaking the schema.
   * This can help discover incorrect configurations early and avoid misexpectations about the
   * built schema.
   * See [treeShakingRoots][com.squareup.wire.schema.WireRun.treeShakingRoots] and
   * [treeShakingRubbish][com.squareup.wire.schema.WireRun.treeShakingRubbish].
   *
   * If false, unused [roots] and [prunes] will be printed as warnings.
   */
  @get:Input
  @get:Optional
  var rejectUnusedRootsOrPrunes = true

  /**
   * True to not write generated types to disk, but emit the names of the source files that would
   * otherwise be generated.
   */
  @get:Input
  @get:Optional
  var dryRun = false

  /**
   * Source paths for local jars and directories, as well as remote binary dependencies
   */
  // TODO(Benoit) Delete this because it seems unused? I think the DSL only pass down ProtoRootSet.
  fun sourcePath(vararg sourcePaths: String) {
    val protoRootSet = addProtoSourceProtoRootSet()
    for (path in sourcePaths) {
      protoRootSet.srcJar(path)
    }
  }

  /**
   * Source paths for local file trees, backed by a [org.gradle.api.file.SourceDirectorySet]
   * Must provide at least a [org.gradle.api.file.SourceDirectorySet.srcDir]
   */
  fun sourcePath(action: Action<ProtoRootSet>) {
    action.execute(addProtoSourceProtoRootSet())
  }

  @Input
  fun eventListenerFactories() = eventListenerFactories.toSet()

  /** Add a [EventListener.Factory]. */
  fun eventListenerFactory(eventListenerFactory: EventListener.Factory) {
    this.eventListenerFactories.add(eventListenerFactory)
  }

  @Input
  fun eventListenerFactoryClasses() = eventListenerFactoryClasses.toSet()

  /** Add a [EventListener.Factory] by name. The referred class must have a no-arguments constructor. */
  fun eventListenerFactoryClass(eventListenerFactoryClass: String) {
    this.eventListenerFactoryClasses.add(eventListenerFactoryClass)
  }

  /**
   * Proto paths for local jars and directories, as well as remote binary dependencies
   */
  fun protoPath(vararg protoPaths: String) {
    val protoRootSet = addProtoPathProtoRootSet()
    for (path in protoPaths) {
      protoRootSet.srcJar(path)
    }
  }

  /**
   * Proto paths for local file trees, backed by a [org.gradle.api.file.SourceDirectorySet]
   * Must provide at least a [org.gradle.api.file.SourceDirectorySet.srcDir]
   */
  fun protoPath(action: Action<ProtoRootSet>) {
    val protoRootSet = addProtoPathProtoRootSet()
    action.execute(protoRootSet)
  }

  internal fun addProtoSourceProtoRootSet() =
    addProtoRootSet(protoSourceProtoRootSets, "protoSource")

  internal fun addProtoPathProtoRootSet() =
    addProtoRootSet(protoPathProtoRootSets, "protoPath")

  /** Creates a new uniquely-named [ProtoRootSet], adds it to [list], and returns it. */
  private fun addProtoRootSet(
    list: MutableList<ProtoRootSet>,
    namePrefix: String,
  ): ProtoRootSet {
    val result = ProtoRootSet(
      project = project,
      name = when {
        list.isEmpty() -> namePrefix
        else -> "$namePrefix${(list.size + 1)}"
      },
    )

    list += result

    return result
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

  @Input
  @Optional
  fun moves() = moves.toList()

  fun move(action: Action<Move>) {
    val move = objectFactory.newInstance(Move::class.java)
    action.execute(move)
    moves += move
  }

  @Input
  @Optional
  fun opaques() = opaques.toSet()

  fun opaque(vararg opaques: String) {
    this.opaques.addAll(opaques)
  }

  /**
   * Aggregates inputs for a run of Wire. Instances of this are either proto sources (these emit
   * code) or proto paths (these are only used to resolve references).
   *
   * This uses a Gradle configuration for the library and project dependencies that we need Gradle
   * to resolve. It uses a basic `MutableList<String>` for paths to files and .jar files.
   *
   * This also keeps track of [includes] and [excludes] that apply only to the referenced file
   * trees. A single run of Wire may use many [ProtoRootSet]s, each with their own sets of
   * includes and excludes.
   */
  class ProtoRootSet internal constructor(
    internal val project: Project,
    name: String,
  ) {
    private val configuration = project.configurations.create(name)
      .apply {
        isCanBeConsumed = false
        isTransitive = false
      }
    private val sourceDirectoriesAndLocalJars = mutableListOf<File>()

    internal val roots: Set<File>
      get() = configuration.files + sourceDirectoriesAndLocalJars

    internal val includes = mutableListOf<String>()
    internal val excludes = mutableListOf<String>()

    fun srcDir(dir: String) {
      sourceDirectoriesAndLocalJars += project.file(dir)
    }

    fun srcDirs(vararg dirs: String) {
      sourceDirectoriesAndLocalJars += dirs.map { project.file(it) }
    }

    fun srcJar(jar: String) {
      srcFileOrConfiguration(jar)
    }

    private fun srcFileOrConfiguration(jar: String) {
      val parser = FileOrUriNotationConverter.parser()
      val converted = parser.parseNotation(jar)
      when (converted) {
        is File -> sourceDirectoriesAndLocalJars += project.file(jar)
        else -> addDependency(jar)
      }
    }

    fun srcJar(provider: Provider<MinimalExternalModuleDependency>) {
      addDependency(provider)
    }

    fun srcJar(convertible: ProviderConvertible<MinimalExternalModuleDependency>) {
      addDependency(convertible.asProvider())
    }

    fun srcProject(projectPath: String) {
      addDependency(project.project(projectPath))
    }

    fun srcProject(project: DelegatingProjectDependency) {
      addDependency(project)
    }

    private fun addDependency(dependencyNotation: Any) {
      project.dependencies.add(configuration.name, dependencyNotation)
      project.dependencies.add("protoProjectDependenciesJvm", dependencyNotation)
    }

    fun include(vararg includePaths: String) {
      includes += includePaths
    }

    fun exclude(vararg excludePaths: String) {
      excludes += excludePaths
    }
  }
}
