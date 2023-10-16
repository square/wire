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

import java.lang.reflect.Array as JavaArray
import com.squareup.wire.VERSION
import com.squareup.wire.gradle.internal.libraryProtoOutputPath
import com.squareup.wire.gradle.internal.targetDefaultOutputPath
import com.squareup.wire.gradle.kotlin.Source
import com.squareup.wire.gradle.kotlin.sourceRoots
import com.squareup.wire.schema.ProtoTarget
import com.squareup.wire.schema.Target
import com.squareup.wire.schema.newEventListenerFactory
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.internal.file.FileOrUriNotationConverter
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class WirePlugin : Plugin<Project> {
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
    project.configurations.create("protoProjectDependenciesJvm").also {
      it.isCanBeResolved = true
      it.isCanBeConsumed = false
      it.attributes { attributesContainer ->
        // TODO(Benoit) If another project, on which this one depends, exposes multiple variants,
        //  Wire won't be able to pick one. We force the resolution to JVM. On the other hand, this
        //  breaks inter-module dependencies for non-jvm modules. We need to fix it.
        attributesContainer.attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
      }
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

    project.tasks.register(ROOT_TASK) {
      it.group = GROUP
      it.description = "Aggregation task which runs every generation task for every given source"
    }

    if (extension.protoLibrary) {
      extension.proto { protoOutput ->
        protoOutput.out = File(project.libraryProtoOutputPath()).path
      }
    }

    val projectDependenciesJvmConfiguration = project.configurations.getByName("protoProjectDependenciesJvm")

    val outputs = extension.outputs
    check(outputs.isNotEmpty()) {
      "At least one target must be provided for project '${project.path}\n" + "See our documentation for details: https://square.github.io/wire/wire_compiler/#customizing-output"
    }
    val hasJavaOutput = outputs.any { it is JavaOutput }
    val hasKotlinOutput = outputs.any { it is KotlinOutput }
    check(!hasKotlinOutput || kotlin.get()) {
      "Wire Gradle plugin applied in " + "project '${project.path}' but no supported Kotlin plugin was found"
    }

    addWireRuntimeDependency(hasJavaOutput, hasKotlinOutput)

    val protoPathInput = WireInput(project.configurations.getByName("protoPath"))
    protoPathInput.addTrees(project, extension.protoTrees)
    protoPathInput.addJars(project, extension.protoJars)
    protoPathInput.addPaths(project, extension.protoPaths)

    sources.forEach { source ->
      val protoSourceCopy = project.configurations.getByName("protoSource").copy()
      try {
        protoSourceCopy.isCanBeConsumed = false
      } catch (ignored: GradleException) {
        // There's a bug in Configurations.copy() that marks the copy's usageCanBeMutated to false
        // if the source's usageCanBeMutated is true. Just leave the consumable state as-is when
        // this happens.
      }

      val protoSourceInput = WireInput(configuration = protoSourceCopy)
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
      for (projectDependency in projectDependencies) {
        projectDependenciesJvmConfiguration.dependencies.add(projectDependency)
      }

      val targets = outputs.map { output ->
        output.toTarget(
          when (val out = output.out) {
            null -> project.relativePath(source.outputDir(project))
            project.libraryProtoOutputPath() -> project.relativePath(out)
            else -> out
          },
        )
      }
      val generatedSourcesDirectories: Set<File> =
        targets
          // Emitted `.proto` files have a special treatment. Their root should be a resource, not a
          // source. We exclude the `ProtoTarget` and we'll add its output to the resources below.
          .filterNot { it is ProtoTarget }
          .map { target -> project.file(target.outDirectory) }
          .toSet()

      // Both the JavaCompile and KotlinCompile tasks might already have been configured by now.
      // Even though we add the Wire output directories into the corresponding sourceSets, the
      // compilation tasks won't know about them so we fix that here.
      if (hasJavaOutput) {
        project.tasks
          .withType(JavaCompile::class.java)
          .matching { it.name == "compileJava" }
          .configureEach {
            it.source(generatedSourcesDirectories)
          }
      }
      if (hasJavaOutput || hasKotlinOutput) {
        project.tasks
          .withType(AbstractKotlinCompile::class.java)
          .matching {
            it.name.equals("compileKotlin") || it.name == "compile${source.name.capitalize()}Kotlin"
          }.configureEach {
            // Note that [KotlinCompile.source] will process files but will ignore strings.
            SOURCE_FUNCTION.invoke(it, arrayOf(generatedSourcesDirectories))
          }
      }

      // TODO: pair up generatedSourceDirectories with their targets so we can be precise.
      for (generatedSourcesDirectory in generatedSourcesDirectories) {
        val relativePath = generatedSourcesDirectory.toRelativeString(project.projectDir)
        if (hasJavaOutput) {
          source.javaSourceDirectorySet?.srcDir(relativePath)
        }
        if (hasKotlinOutput) {
          source.kotlinSourceDirectorySet?.srcDir(relativePath)
        }
      }

      val taskName = "generate${source.name.capitalize()}Protos"
      val task = project.tasks.register(taskName, WireTask::class.java) { task: WireTask ->
        task.group = GROUP
        task.description = "Generate protobuf implementation for ${source.name}"
        task.source(protoSourceInput.configuration)

        val outputDirectories: List<String> = buildList {
          addAll(
            targets
              // Emitted `.proto` files have a special treatment. Their root should be a resource, not
              // a source. We exclude the `ProtoTarget` and we'll add its output to the resources
              // below.
              .filterNot { it is ProtoTarget }
              .map(Target::outDirectory),
          )
        }
        task.outputDirectories.setFrom(outputDirectories)
        task.projectDependencies.setFrom(projectDependenciesJvmConfiguration)
        if (extension.protoLibrary) {
          task.protoLibraryOutput.set(File(project.libraryProtoOutputPath()))
        }
        task.sourceInput.set(protoSourceInput.toLocations(project))
        task.protoInput.set(protoPathInput.toLocations(project))
        task.roots.set(extension.roots.toList())
        task.prunes.set(extension.prunes.toList())
        task.moves.set(extension.moves.toList())
        task.opaques.set(extension.opaques.toList())
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
        // TODO(Benoit) Probably should be checking for other names than `main`. As well, source
        //  sets might be created 'afterEvaluate'. Does that mean we should do this work in
        //  `afterEvaluate` as well? See: https://kotlinlang.org/docs/multiplatform-dsl-reference.html#source-sets
        if (sourceSets.findByName("main") != null) {
          sourceSets.getByName("main") { main: SourceSet ->
            main.resources.srcDir(protoOutputDirectory)
          }
        } else {
          project.logger.warn("${project.displayName} doesn't have a 'main' source sets. The .proto files will not automatically be added to the artifact.")
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

  private fun Project.addWireRuntimeDependency(
    hasJavaOutput: Boolean,
    hasKotlinOutput: Boolean,
  ) {
    if (!hasJavaOutput && !hasKotlinOutput) return

    // Indicates when the plugin is applied inside the Wire repo to Wire's own modules.
    val isInternalBuild = project.properties["com.squareup.wire.internal"].toString() == "true"
    val isMultiplatform = project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
    val isJsOnly =
      if (isMultiplatform) false else project.plugins.hasPlugin("org.jetbrains.kotlin.js")
    val runtimeDependency = wireRuntimeDependency(isInternalBuild)

    when {
      isMultiplatform -> {
        val sourceSets =
          project.extensions.getByType(KotlinMultiplatformExtension::class.java).sourceSets
        val sourceSet = (sourceSets.getByName("commonMain") as DefaultKotlinSourceSet)
        project.dependencies.add(sourceSet.apiConfigurationName, runtimeDependency)
      }

      isJsOnly -> {
        val sourceSets =
          project.extensions.getByType(KotlinJsProjectExtension::class.java).sourceSets
        val sourceSet = (sourceSets.getByName("main") as DefaultKotlinSourceSet)
        project.dependencies.add(sourceSet.apiConfigurationName, runtimeDependency)
      }

      else -> {
        try {
          project.dependencies.add("api", runtimeDependency)
        } catch (_: UnknownConfigurationException) {
          // No `api` configuration on Java applications.
          project.dependencies.add("implementation", runtimeDependency)
        }
      }
    }
  }

  private fun wireRuntimeDependency(isInternalBuild: Boolean): Any {
    return if (isInternalBuild) {
      project.project(":wire-runtime")
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
