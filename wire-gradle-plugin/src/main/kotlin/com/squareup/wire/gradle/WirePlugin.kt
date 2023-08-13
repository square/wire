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
@file:OptIn(ExperimentalStdlibApi::class)

package com.squareup.wire.gradle

import com.squareup.wire.VERSION
import com.squareup.wire.gradle.internal.libraryProtoOutputPath
import com.squareup.wire.gradle.internal.targetDefaultOutputPath
import com.squareup.wire.gradle.kotlin.Source
import com.squareup.wire.gradle.kotlin.WireSourceDirectorySet
import com.squareup.wire.gradle.kotlin.sourceRoots
import com.squareup.wire.schema.ProtoTarget
import com.squareup.wire.schema.Target
import com.squareup.wire.schema.newEventListenerFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileOrUriNotationConverter
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import java.lang.reflect.Array as JavaArray

class WirePlugin @Inject internal constructor(
  private val objects: ObjectFactory,
) : Plugin<Project> {
  private val android = AtomicBoolean(false)
  private val java = AtomicBoolean(false)
  private val kotlin = AtomicBoolean(false)

  private lateinit var extension: WireExtension
  internal lateinit var project: Project

  private val sources by lazy { this.sourceRoots(kotlin = kotlin.get(), java = java.get()) }

  override fun apply(project: Project) {
    this.extension = project.extensions.create("wire", WireExtension::class.java, project)
    this.project = project

    project.configurations.create("protoSource").also {
      it.isCanBeConsumed = false
      it.isTransitive = false
    }
    project.configurations.create("protoPath").also {
      it.isCanBeConsumed = false
      it.isTransitive = false
    }

    val androidPluginHandler = { _: Plugin<*> ->
      android.set(true)
      project.afterEvaluate {
        project.setupWireTasks(afterAndroid = true)
      }
    }
    project.plugins.withId("com.android.application", androidPluginHandler)
    project.plugins.withId("com.android.library", androidPluginHandler)
    project.plugins.withId("com.android.instantapp", androidPluginHandler)
    project.plugins.withId("com.android.feature", androidPluginHandler)
    project.plugins.withId("com.android.dynamic-feature", androidPluginHandler)

    val kotlinPluginHandler = { _: Plugin<*> -> kotlin.set(true) }
    project.plugins.withId("org.jetbrains.kotlin.multiplatform", kotlinPluginHandler)
    project.plugins.withId("org.jetbrains.kotlin.android", kotlinPluginHandler)
    project.plugins.withId("org.jetbrains.kotlin.jvm", kotlinPluginHandler)
    project.plugins.withId("org.jetbrains.kotlin.js", kotlinPluginHandler)
    project.plugins.withId("kotlin2js", kotlinPluginHandler)

    val javaPluginHandler = { _: Plugin<*> -> java.set(true) }
    project.plugins.withId("java", javaPluginHandler)
    project.plugins.withId("java-library", javaPluginHandler)

    project.afterEvaluate {
      project.setupWireTasks(afterAndroid = false)
    }
  }

  private fun Project.setupWireTasks(afterAndroid: Boolean) {
    if (android.get() && !afterAndroid) return

    check(android.get() || java.get() || kotlin.get()) {
      "Wire Gradle plugin applied in " + "project '${project.path}' but unable to find either the Java, Kotlin, or Android plugin"
    }

    project.tasks.register<Task>(ROOT_TASK) {
      group = GROUP
      description = "Aggregation task which runs every generation task for every given source"

      doFirst {
        val extension = project.extensions.getByType<WireExtension>()
        check(extension.outputs.get().isNotEmpty()) {
          "At least one target must be provided for project '${project.path}\n" +
                  "See our documentation for details: https://square.github.io/wire/wire_compiler/#customizing-output"
        }
      }
    }

    if (extension.protoLibrary) {
      extension.proto { protoOutput ->
        protoOutput.out = File(project.libraryProtoOutputPath()).path
      }
    }

    val outputs = extension.outputs

    val hasJavaOutput = outputs.map { list -> list.any { it is JavaOutput } }
    val hasKotlinOutput = outputs.map { list -> list.any { it is KotlinOutput } }
    val hasJavaOrKotlinOutput = hasJavaOutput.zip(hasKotlinOutput) { java, kotlin -> java || kotlin }

    addWireRuntimeDependency(hasJavaOrKotlinOutput)

    val protoPathInput = WireInput(project.configurations.getByName("protoPath"))
    protoPathInput.addTrees(project, extension.protoTrees)
    protoPathInput.addJars(project, extension.protoJars)
    protoPathInput.addPaths(project, extension.protoPaths)

    sources.forEach { source ->
      val protoSourceInput = WireInput(project.configurations.getByName("protoSource").copy())
      protoSourceInput.addTrees(project, extension.sourceTrees)
      protoSourceInput.addJars(project, extension.sourceJars)
      protoSourceInput.addPaths(project, extension.sourcePaths)
      // TODO(Benoit) Should we add our default source folders everytime? Right now, someone could
      //  not combine a custom protoSource with our default using variants.
      if (protoSourceInput.dependencies.isEmpty()) {
        protoSourceInput.addPaths(project, defaultSourceFolders(source))
      }

      val inputFiles = project.layout.files(protoSourceInput.inputFiles, protoPathInput.inputFiles)

      val projectDependencies =
        (protoSourceInput.dependencies + protoPathInput.dependencies).filterIsInstance<ProjectDependency>()

      val targets = outputs.map { outputs ->
        outputs.map { output ->
          output.toTarget(
            when (val out = output.out) {
              null -> project.relativePath(source.outputDir(project))
              project.libraryProtoOutputPath() -> project.relativePath(out)
              else -> out
            },
          )
        }
      }
      val generatedSourcesDirectories = objects.fileCollection().from(
        targets.map { list ->
          // Emitted `.proto` files have a special treatment. Their root should be a resource, not a
          // source. We exclude the `ProtoTarget` and we'll add its output to the resources below.
          list
            .filterNot { it is ProtoTarget }
            .map { target -> target.outDirectory }
        },
      )

      // Both the JavaCompile and KotlinCompile tasks might already have been configured by now.
      // Even though we add the Wire output directories into the corresponding sourceSets, the
      // compilation tasks won't know about them so we fix that here.
      project.tasks
        .withType(JavaCompile::class.java)
        .matching { it.name == "compileJava" }
        .configureEach {
          if (hasJavaOutput.get()) {
            it.source(generatedSourcesDirectories)
          }
        }

      project.tasks
        .withType(AbstractKotlinCompile::class.java)
        .matching {
          it.name.equals("compileKotlin") || it.name == "compile${source.name.capitalize()}Kotlin"
        }
        .configureEach {
          if (hasJavaOutput.get() || hasKotlinOutput.get()) {
            // Note that [KotlinCompile.source] will process files but will ignore strings.
            SOURCE_FUNCTION.invoke(it, arrayOf(generatedSourcesDirectories))
          }
        }

      // TODO: pair up generatedSourceDirectories with their targets so we can be precise.
      source.javaSourceDirectorySet?.maybeAddSrcDirs(hasJavaOutput, generatedSourcesDirectories)
      source.kotlinSourceDirectorySet?.maybeAddSrcDirs(hasKotlinOutput, generatedSourcesDirectories)

      val taskName = "generate${source.name.capitalize()}Protos"
      val task = project.tasks.register(taskName, WireTask::class.java) { task: WireTask ->
        task.group = GROUP
        task.description = "Generate protobuf implementation for ${source.name}"
        task.source(protoSourceInput.configuration)

        if (task.logger.isDebugEnabled) {
          protoSourceInput.debug(task.logger)
          protoPathInput.debug(task.logger)
        }
        val outputDirectories = targets.map { list ->
          list
            // Emitted `.proto` files have a special treatment. Their root should be a resource, not
            // a source. We exclude the `ProtoTarget` and we'll add its output to the resources
            // below.
            .filterNot { it is ProtoTarget }
            .map(Target::outDirectory)
        }

        task.outputDirectories.setFrom(outputDirectories)
        if (extension.protoLibrary) {
          task.protoLibraryOutput.set(File(project.libraryProtoOutputPath()))
        }
        task.sourceInput.set(protoSourceInput.toLocations(project))
        task.protoInput.set(protoPathInput.toLocations(project))
        task.roots.set(extension.roots.toList())
        task.prunes.set(extension.prunes.toList())
        task.moves.set(extension.moves.toList())
        task.sinceVersion.set(extension.sinceVersion)
        task.untilVersion.set(extension.untilVersion)
        task.onlyVersion.set(extension.onlyVersion)
        task.rules.set(extension.rules)
        task.targets.set(targets)
        task.permitPackageCycles.set(extension.permitPackageCycles)
        task.dryRun.set(extension.dryRun)
        task.rejectUnusedRootsOrPrunes.set(extension.rejectUnusedRootsOrPrunes)

        task.inputFiles.setFrom(inputFiles)

        task.projectDirProperty.set(project.layout.projectDirectory)
        task.buildDirProperty.set(project.layout.buildDirectory)

        val factories = extension.eventListenerFactories + extension.eventListenerFactoryClasses().map(::newEventListenerFactory)
        task.eventListenerFactories.set(factories)

        for (projectDependency in projectDependencies) {
          task.dependsOn(projectDependency)
        }
      }

      val taskOutputDirectories = task.map { it.outputDirectories }
      // Note that we have to pass a Provider for Gradle to add the Wire task into the tasks
      // dependency graph. It fails silently otherwise.
      source.kotlinSourceDirectorySet?.srcDir(taskOutputDirectories)
      source.javaSourceDirectorySet?.srcDir(taskOutputDirectories)
      source.registerGeneratedDirectory?.invoke(taskOutputDirectories)

      val protoOutputDirectory = task.map { it.protoLibraryOutput }
      if (extension.protoLibrary) {
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        // Note that there are no source sets for some platforms such as native.
        if (sourceSets.isNotEmpty()) {
          sourceSets.getByName("main") { main: SourceSet ->
            main.resources.srcDir(protoOutputDirectory)
          }
        }
      }

      project.tasks.named(ROOT_TASK).configure {
        it.dependsOn(task)
      }

      source.registerTaskDependency?.invoke(task)
    }
  }

  private fun Source.outputDir(project: Project): File {
    return if (sources.size > 1) {
      File(project.targetDefaultOutputPath(), name)
    } else {
      File(project.targetDefaultOutputPath())
    }
  }

  private fun WireSourceDirectorySet.maybeAddSrcDirs(hasOutput: Provider<Boolean>, files: FileCollection) {
    srcDir(
      project.provider {
        val result = objects.fileCollection()

        if (hasOutput.get()) {
          result.from(
            files.map { it.toRelativeString(project.projectDir) },
          )
        }

        result
      },
    )
  }

  private fun Project.addWireRuntimeDependency(
    hasJavaOrKotlinOutput: Provider<Boolean>,
  ) {
    // Indicates when the plugin is applied inside the Wire repo to Wire's own modules.
    val isInternalBuild = project.properties["com.squareup.wire.internal"] == "true"
    val isMultiplatform = project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
    val isJsOnly =
      if (isMultiplatform) false else project.plugins.hasPlugin("org.jetbrains.kotlin.js")
    val runtimeDependency = wireRuntimeDependency(isInternalBuild)
    val runtimeDependencyProviderIfRequired = hasJavaOrKotlinOutput.map { requires ->
      if (requires) runtimeDependency else null
    }

    when {
      isMultiplatform -> {
        val sourceSets =
          project.extensions.getByType(KotlinMultiplatformExtension::class.java).sourceSets
        val sourceSet = (sourceSets.getByName("commonMain") as DefaultKotlinSourceSet)
        project.dependencies.addProvider(sourceSet.apiConfigurationName, runtimeDependencyProviderIfRequired)
      }

      isJsOnly -> {
        val sourceSets =
          project.extensions.getByType(KotlinJsProjectExtension::class.java).sourceSets
        val sourceSet = (sourceSets.getByName("main") as DefaultKotlinSourceSet)
        project.dependencies.addProvider(sourceSet.apiConfigurationName, runtimeDependencyProviderIfRequired)
      }

      else -> {
        try {
          project.dependencies.addProvider("api", runtimeDependencyProviderIfRequired)
        } catch (_: UnknownConfigurationException) {
          // No `api` configuration on Java applications.
          project.dependencies.addProvider("implementation", runtimeDependencyProviderIfRequired)
        }
      }
    }
  }

  private fun wireRuntimeDependency(isInternalBuild: Boolean): Dependency {
    return if (isInternalBuild) {
      project.dependencies.create(project.project(":wire-runtime"))
    } else {
      project.dependencies.create("com.squareup.wire:wire-runtime:$VERSION")
    }
  }

  private fun defaultSourceFolders(source: Source): Set<String> {
    val parser = FileOrUriNotationConverter.parser()
    return source.sourceSets.map { "src/$it/proto" }.filter { path ->
      val converted = parser.parseNotation(path) as File
      val file =
        if (!converted.isAbsolute) File(project.projectDir, converted.path) else converted
      return@filter file.exists()
    }.toSet()
  }

  internal companion object {
    const val ROOT_TASK = "generateProtos"
    const val GROUP = "wire"

    // The signature of this function changed in Kotlin 1.7, so we invoke it reflectively to support
    // both.
    // 1.6.x: `fun source(vararg sources: Any): SourceTask`
    // 1.7.x: `fun source(vararg sources: Any)`
    private val SOURCE_FUNCTION = KotlinCompile::class.java.getMethod(
      "source",
      JavaArray.newInstance(Any::class.java, 0).javaClass,
    )
  }
}

private fun String.capitalize(): String {
  return replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
}
