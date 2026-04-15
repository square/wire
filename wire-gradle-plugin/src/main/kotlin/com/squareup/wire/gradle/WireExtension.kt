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
import java.net.URI
import java.net.URISyntaxException
import kotlin.LazyThreadSafetyMode.NONE
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.internal.catalog.DelegatingProjectDependency
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderConvertible

open class WireExtension(
  private val project: Project,
) {
  private val objectFactory = project.objects

  internal val protoSourceProtoRootSets = mutableListOf<ProtoRootSet>()
  internal val protoPathProtoRootSets = mutableListOf<ProtoRootSet>()
  internal val roots = objectFactory.setProperty(String::class.java)
  internal val prunes = objectFactory.setProperty(String::class.java)
  internal val moves = objectFactory.listProperty(Move::class.java)
  internal val opaques = objectFactory.setProperty(String::class.java)
  internal val eventListenerFactories = objectFactory.setProperty(EventListener.Factory::class.java)
  internal val eventListenerFactoryClasses = objectFactory.setProperty(String::class.java)
  internal val onlyVersion = objectFactory.property(String::class.java)
  internal val sinceVersion = objectFactory.property(String::class.java)
  internal val untilVersion = objectFactory.property(String::class.java)
  internal val permitPackageCycles = objectFactory.property(Boolean::class.java).convention(false)
  internal val loadExhaustively = objectFactory.property(Boolean::class.java).convention(false)

  fun roots() = roots.get().toSet()

  /**
   * See [com.squareup.wire.schema.WireRun.treeShakingRoots].
   */
  fun root(vararg roots: String) {
    this.roots.addAll(roots.toList())
  }

  fun prunes() = prunes.get().toSet()

  /**
   * See [com.squareup.wire.schema.WireRun.treeShakingRubbish].
   */
  fun prune(vararg prunes: String) {
    this.prunes.addAll(prunes.toList())
  }

  fun sinceVersion() = sinceVersion.orNull

  /**
   * See [com.squareup.wire.schema.WireRun.sinceVersion].
   */
  fun sinceVersion(sinceVersion: String) {
    this.sinceVersion.set(sinceVersion)
  }

  fun untilVersion() = untilVersion.orNull

  /**
   * See [com.squareup.wire.schema.WireRun.untilVersion].
   */
  fun untilVersion(untilVersion: String) {
    this.untilVersion.set(untilVersion)
  }

  fun onlyVersion() = onlyVersion.orNull

  /**
   * See [com.squareup.wire.schema.WireRun.onlyVersion].
   */
  fun onlyVersion(onlyVersion: String) {
    this.onlyVersion.set(onlyVersion)
  }

  fun permitPackageCycles() = permitPackageCycles.get()

  /**
   * See [com.squareup.wire.schema.WireRun.permitPackageCycles].
   */
  fun permitPackageCycles(permitPackageCycles: Boolean) {
    this.permitPackageCycles.set(permitPackageCycles)
  }

  fun loadExhaustively() = loadExhaustively.get()

  /**
   * See [com.squareup.wire.schema.WireRun.loadExhaustively].
   */
  fun loadExhaustively(loadExhaustively: Boolean) {
    this.loadExhaustively.set(loadExhaustively)
  }

  /**
   * A user-provided file listing [roots] and [prunes].
   */
  val rules: Property<String> = objectFactory.property(String::class.java)

  /** For Groovy DSL assignment: `rules = "path/to/rules.txt"`. */
  fun setRules(value: String?) {
    rules.set(value)
  }

  /** Specified what types to output where. Maps to [com.squareup.wire.schema.Target] */
  val outputs: ListProperty<WireOutput> = objectFactory.listProperty(WireOutput::class.java)

  /**
   * True to emit `.proto` files into the output resources. Use this when your `.jar` file can be
   * used as a library for other proto or Wire projects.
   *
   * Note that only the `.proto` files used in the library will be included, and these files will
   * have tree-shaking applied.
   */
  val protoLibrary: Property<Boolean> =
    objectFactory.property(Boolean::class.java).convention(false)

  /** For Groovy DSL assignment: `protoLibrary = true`. */
  fun setProtoLibrary(value: Boolean) {
    protoLibrary.set(value)
  }

  /**
   * If true, Wire will fail if not all [roots] and [prunes] are used when tree-shaking the schema.
   * This can help discover incorrect configurations early and avoid misexpectations about the
   * built schema.
   * See [treeShakingRoots][com.squareup.wire.schema.WireRun.treeShakingRoots] and
   * [treeShakingRubbish][com.squareup.wire.schema.WireRun.treeShakingRubbish].
   *
   * If false, unused [roots] and [prunes] will be printed as warnings.
   */
  val rejectUnusedRootsOrPrunes: Property<Boolean> =
    objectFactory.property(Boolean::class.java).convention(true)

  /** For Groovy DSL assignment: `rejectUnusedRootsOrPrunes = false`. */
  fun setRejectUnusedRootsOrPrunes(value: Boolean) {
    rejectUnusedRootsOrPrunes.set(value)
  }

  /**
   * True to not write generated types to disk, but emit the names of the source files that would
   * otherwise be generated.
   */
  val dryRun: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)

  /** For Groovy DSL assignment: `dryRun = true`. */
  fun setDryRun(value: Boolean) {
    dryRun.set(value)
  }

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
   * Source paths for local file trees, backed by a [org.gradle.api.file.SourceDirectorySet].
   * Must provide at least a [org.gradle.api.file.SourceDirectorySet.srcDir].
   */
  fun sourcePath(action: Action<ProtoRootSet>) {
    action.execute(addProtoSourceProtoRootSet())
  }

  fun eventListenerFactories() = eventListenerFactories.get().toSet()

  /** Add a [EventListener.Factory]. */
  fun eventListenerFactory(eventListenerFactory: EventListener.Factory) {
    this.eventListenerFactories.add(eventListenerFactory)
  }

  fun eventListenerFactoryClasses() = eventListenerFactoryClasses.get().toSet()

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
   * Proto paths for local file trees, backed by a [org.gradle.api.file.SourceDirectorySet].
   * Must provide at least a [org.gradle.api.file.SourceDirectorySet.srcDir].
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

  /**
   * Defines a Java target. See [com.squareup.wire.schema.JavaTarget].
   */
  fun java(action: Action<JavaOutput>) {
    val javaOutput = objectFactory.newInstance(JavaOutput::class.java)
    action.execute(javaOutput)
    outputs.add(javaOutput)
  }

  /**
   * Defines a Kotlin target. See [com.squareup.wire.schema.KotlinTarget].
   */
  fun kotlin(action: Action<KotlinOutput>) {
    val kotlinOutput = objectFactory.newInstance(KotlinOutput::class.java)
    action.execute(kotlinOutput)
    outputs.add(kotlinOutput)
  }

  /**
   * Defines a Proto target. See [com.squareup.wire.schema.ProtoTarget].
   */
  fun proto(action: Action<ProtoOutput>) {
    val protoOutput = objectFactory.newInstance(ProtoOutput::class.java)
    action.execute(protoOutput)
    outputs.add(protoOutput)
  }

  /**
   * Defines a Custom target. See [com.squareup.wire.schema.CustomTarget].
   */
  fun custom(action: Action<CustomOutput>) {
    val customOutput = objectFactory.newInstance(CustomOutput::class.java)
    action.execute(customOutput)
    outputs.add(customOutput)
  }

  fun moves() = moves.get().toList()

  /**
   * See [com.squareup.wire.schema.WireRun.moves].
   */
  fun move(action: Action<Move>) {
    val move = objectFactory.newInstance(Move::class.java)
    action.execute(move)
    moves.add(move)
  }

  fun opaques() = opaques.get().toSet()

  /**
   * See [com.squareup.wire.schema.WireRun.opaqueTypes].
   */
  fun opaque(vararg opaques: String) {
    this.opaques.addAll(opaques.toList())
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
    /** Calling this doesn't resolve the configuration. */
    internal var isEmpty = true

    internal val configuration = project.configurations.create(name)
      .apply {
        isCanBeConsumed = false
        isTransitive = false
      }

    /** Calling this will resolve the configuration. */
    internal val roots: Set<File>
      get() = configuration.files
    private val files: ConfigurableFileCollection by lazy(NONE) {
      val files = project.files()
      project.dependencies.add(configuration.name, files)
      files
    }

    internal val includes = project.objects.listProperty(String::class.java)
    internal val excludes = project.objects.listProperty(String::class.java)

    /**
     * Adds a set of source. The given paths are evaluated as per [Project.files][org.gradle.api.Project.files].
     */
    fun srcDir(fileCollection: Any) {
      isEmpty = false
      files.from(fileCollection)
    }

    /** Sets a directory. Example: "src/main/proto". */
    fun srcDir(dir: String) {
      srcDir(project.file(dir))
    }

    /** Sets one or more directories. */
    fun srcDirs(vararg dirs: String) {
      srcDir(dirs.map { project.file(it) })
    }

    /** Sets a local or a remote jar. Examples: "libs/protos.jar", or "com.example:protos:1.0.0". */
    fun srcJar(jar: String) {
      try {
        val uri = URI.create(jar)
        if (uri.scheme != null) {
          // It's likely a URI notation.
          addDependency(jar)
          return
        }
      } catch (e: URISyntaxException) {}

      val fileOrNull = try {
        project.file(jar)
      } catch (e: Exception) {
        null // Probably a dependency string like "com.example:protos:1.0.0".
      }

      if (fileOrNull != null) {
        addDependency(project.files(fileOrNull))
      } else {
        addDependency(jar)
      }
    }

    /** Sets a local or a remote jar. */
    fun srcJar(provider: Provider<MinimalExternalModuleDependency>) {
      addDependency(provider)
    }

    /** Sets a local or a remote jar. */
    fun srcJar(convertible: ProviderConvertible<MinimalExternalModuleDependency>) {
      addDependency(convertible.asProvider())
    }

    /** Sets a project. Example: ":protos". */
    fun srcProject(projectPath: String) {
      addDependency(project.project(projectPath))
    }

    /** Sets a project. */
    @Deprecated(
      message = "Use srcProject(ProjectDependency) instead. This method will be removed in a future version of Wire.",
      level = DeprecationLevel.HIDDEN,
    )
    fun srcProject(project: DelegatingProjectDependency) {
      addDependency(project)
    }

    /** Sets a project. */
    fun srcProject(project: ProjectDependency) {
      addDependency(project)
    }

    private fun addDependency(dependencyNotation: Any) {
      isEmpty = false
      project.dependencies.add(configuration.name, dependencyNotation)
      project.dependencies.add("protoProjectDependenciesJvm", dependencyNotation)
    }

    /**
     * If set, only the files defined as included will be processed.
     * Example: "com/example/important.proto".
     */
    fun include(vararg includePaths: String) {
      includes.addAll(includePaths.toList())
    }

    /**
     * If set, all the files defined as excluded will be ignored.
     * Example: "com/example/irrelevant.proto".
     */
    fun exclude(vararg excludePaths: String) {
      excludes.addAll(excludePaths.toList())
    }
  }
}
