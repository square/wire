/*
 * Copyright (C) 2020 Square, Inc.
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
package com.squareup.wire.gradle.kotlin

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.squareup.wire.gradle.JavaOutput
import com.squareup.wire.gradle.KotlinOutput
import com.squareup.wire.gradle.WireExtension
import com.squareup.wire.gradle.WirePlugin
import com.squareup.wire.gradle.WireTask
import com.squareup.wire.gradle.inputLocations
import com.squareup.wire.gradle.internal.targetDefaultOutputPath
import com.squareup.wire.schema.ProtoTarget
import com.squareup.wire.schema.newEventListenerFactory
import java.io.File
import java.lang.reflect.Array as JavaArray
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun interface WireTaskFactory {
  fun createWireTasks(project: Project, extension: WireExtension)
}

internal fun getWireTaskFactory(
  project: Project,
  hasKotlin: Boolean,
  hasJava: Boolean,
  hasAndroid: Boolean,
): WireTaskFactory = when {
  hasAndroid -> {
    val extension = project.extensions.getByType(AndroidComponentsExtension::class.java)
    AndroidWireTaskFactory(extension, hasKotlin)
  }
  hasKotlin && project.extensions.findByType(KotlinMultiplatformExtension::class.java) != null -> {
    KotlinMultiplatformWireTaskFactory(project.extensions.getByType(KotlinMultiplatformExtension::class.java))
  }
  hasKotlin -> {
    val kotlinSourceSets = project.extensions.findByType(KotlinProjectExtension::class.java)?.sourceSets
    val javaSourceSets = project.extensions.findByType(SourceSetContainer::class.java)
    JvmWireTaskFactory(kotlinSourceSets, javaSourceSets, true)
  }
  hasJava -> {
    val javaSourceSets = project.extensions.findByType(SourceSetContainer::class.java)
    JvmWireTaskFactory(null, javaSourceSets, false)
  }
  else -> {
    throw IllegalStateException("Wire Gradle plugin requires Android, Kotlin, or Java to be configured on project '${project.name}'.")
  }
}

private class KotlinMultiplatformWireTaskFactory(
  private val kotlinMultiplatformExtension: KotlinMultiplatformExtension,
) : WireTaskFactory {
  override fun createWireTasks(project: Project, extension: WireExtension) {
    val sources = kotlinMultiplatformExtension.sourceRoots()
    val hasMultipleSources = sources.size > 1
    sources.forEach { source ->
      setupWireTask(project, extension, source, hasMultipleSources, true)
    }
  }
}

private class AndroidWireTaskFactory(
  private val androidComponents: AndroidComponentsExtension<*, *, *>,
  private val kotlin: Boolean,
) : WireTaskFactory {
  override fun createWireTasks(project: Project, extension: WireExtension) {
    androidComponents.onVariants { variant ->
      val sourceSetNames = mutableListOf("main")
      variant.buildType?.let { sourceSetNames.add(it) }
      sourceSetNames.addAll(variant.productFlavors.map { it.second })
      sourceSetNames.add(variant.name)

      val source = AndroidSource(
        name = variant.name,
        sourceSets = sourceSetNames.distinct(),
        variant = variant,
        kotlin = kotlin,
      )
      setupWireTask(project, extension, source, hasMultipleSources = true, hasKotlin = kotlin)
    }
  }
}

private class JvmWireTaskFactory(
  private val kotlinSourceSets: NamedDomainObjectContainer<KotlinSourceSet>?,
  private val javaSourceSets: SourceSetContainer?,
  private val hasKotlin: Boolean,
) : WireTaskFactory {
  override fun createWireTasks(project: Project, extension: WireExtension) {
    val kotlinSourceDirectorySet = kotlinSourceSets?.findByName("main")?.kotlin
    val javaSourceDirectorySet = javaSourceSets?.findByName("main")?.java
    val source = JvmSource(
      name = "main",
      sourceSets = listOf("main"),
      kotlinSourceDirectorySet = kotlinSourceDirectorySet,
      javaSourceDirectorySet = javaSourceDirectorySet,
    )
    setupWireTask(
      project,
      extension,
      source,
      hasMultipleSources = false,
      hasKotlin = hasKotlin,
    )
  }
}

private fun setupWireTask(
  project: Project,
  extension: WireExtension,
  source: Source,
  hasMultipleSources: Boolean,
  hasKotlin: Boolean,
) {
  val outputs = extension.outputs
  val hasJavaOutput = outputs.any { it is JavaOutput }
  val hasKotlinOutput = outputs.any { it is KotlinOutput }

  val protoSourceProtoRootSets = extension.protoSourceProtoRootSets.toMutableList()
  val protoPathProtoRootSets = extension.protoPathProtoRootSets.toMutableList()

  if (protoSourceProtoRootSets.all { it.isEmpty }) {
    val sourceSetProtoRootSet = WireExtension.ProtoRootSet(
      project = project,
      name = "${source.name}ProtoSource",
    )
    protoSourceProtoRootSets += sourceSetProtoRootSet
    for (sourceFolder in source.defaultSourceFolders(project)) {
      sourceSetProtoRootSet.srcDir(sourceFolder)
    }
  }

  val targets = outputs.map { output ->
    output.toTarget(project.relativePath(output.out ?: source.outputDir(project, hasMultipleSources)))
  }
  val generatedSourcesDirectories: Set<File> =
    targets
      // Emitted `.proto` files have a special treatment. Their root should be a resource, not a
      // source. We exclude the `ProtoTarget` and we'll add its output to the resources below.
      .filterNot { it is ProtoTarget }
      .map { target -> project.file(target.outDirectory) }
      .toSet()

  val protoTarget = targets.filterIsInstance<ProtoTarget>().firstOrNull()

  if (hasJavaOutput) {
    project.tasks
      .withType(JavaCompile::class.java)
      .matching { it.name == "compileJava" }
      .configureEach {
        it.source(generatedSourcesDirectories)
      }
  }

  if ((hasJavaOutput || hasKotlinOutput) && hasKotlin) {
    val kotlinCompileClass = Class.forName(
      "org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile",
      false,
      WirePlugin::class.java.classLoader,
    ) as Class<out org.gradle.api.Task>
    project.tasks
      .withType(kotlinCompileClass)
      .matching {
        it.name.equals("compileKotlin") || it.name == "compile${source.name.replaceFirstChar { it.uppercase() }}Kotlin"
      }.configureEach {
        // Note that [KotlinCompile.source] will process files but will ignore strings.
        SOURCE_FUNCTION.invoke(it, arrayOf(generatedSourcesDirectories))
      }
  }

  val taskName = "generate${source.name.replaceFirstChar { it.uppercase() }}Protos"
  val task = project.tasks.register(taskName, WireTask::class.java) { task: WireTask ->
    task.group = WirePlugin.GROUP
    task.description = "Generate protobuf implementation for ${source.name}"

    var addedSourcesDependencies = false
    // Flatten all the input files here. Changes to any of them will cause the task to re-run.
    for (rootSet in protoSourceProtoRootSets) {
      task.source(rootSet.configuration)
      if (!rootSet.isEmpty) {
        // Use the isEmpty flag to avoid resolving the configuration eagerly
        addedSourcesDependencies = true
      }
    }
    // We only want to add ProtoPath sources if we have other sources already. The WireTask
    // would otherwise run even through we have no sources.
    if (addedSourcesDependencies) {
      for (rootSet in protoPathProtoRootSets) {
        task.source(rootSet.configuration)
      }
    }

    targets
      // Emitted `.proto` files have a special treatment. Their root should be a resource, not
      // a source. We exclude the `ProtoTarget` and we'll add its output to the resources
      // below.
      .filterNot { it is ProtoTarget }.forEach { target ->
        val dir = project.objects.directoryProperty()
        dir.set(
          project.tasks.named(taskName).map {
            project.layout.projectDirectory.dir(target.outDirectory)
          },
        )
        task.outputDirectories.add(dir)
      }
    task.protoSourceConfiguration.setFrom(project.configurations.getByName("protoSource"))
    task.protoPathConfiguration.setFrom(project.configurations.getByName("protoPath"))
    task.projectDependenciesJvmConfiguration.setFrom(project.configurations.getByName("protoProjectDependenciesJvm"))
    if (protoTarget != null) {
      task.protoLibraryOutput.set(project.file(protoTarget.outDirectory))
    }
    task.sourceInput.set(project.provider { protoSourceProtoRootSets.inputLocations })
    task.protoInput.set(project.provider { protoPathProtoRootSets.inputLocations })
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
    task.loadExhaustively.set(extension.loadExhaustively)
    task.dryRun.set(extension.dryRun)
    task.rejectUnusedRootsOrPrunes.set(extension.rejectUnusedRootsOrPrunes)

    task.projectDirProperty.set(project.layout.projectDirectory)
    task.buildDirProperty.set(project.layout.buildDirectory)

    val factories = extension.eventListenerFactories + extension.eventListenerFactoryClasses().map(::newEventListenerFactory)
    task.eventListenerFactories.set(factories)
  }

  source.registerGeneratedSources(project, task, generatedSourcesDirectories)

  val protoOutputDirectory = task.map { it.protoLibraryOutput }
  if (protoTarget != null) {
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

  project.tasks.named(WirePlugin.ROOT_TASK).configure {
    it.dependsOn(task)
  }
}

private fun KotlinMultiplatformExtension.sourceRoots(): List<Source> {
  // Wire only supports commonMain as in other cases, we'd be expected to generate both
  // `expect` and `actual` classes which doesn't make much sense for what Wire does.
  return listOf(
    JvmSource(
      name = "commonMain",
      kotlinSourceDirectorySet = sourceSets.getByName("commonMain").kotlin,
      javaSourceDirectorySet = null,
      sourceSets = listOf("commonMain"),
    ),
  )
}

private sealed class Source(
  val name: String,
  val sourceSets: List<String>,
) {
  fun outputDir(project: Project, hasMultipleSources: Boolean): File {
    return if (hasMultipleSources) {
      File(project.targetDefaultOutputPath(), name)
    } else {
      File(project.targetDefaultOutputPath())
    }
  }

  fun defaultSourceFolders(project: Project): Set<String> {
    return sourceSets.map { "src/$it/proto" }
      .filter { path -> File(project.projectDir, path).exists() }
      .toSet()
  }

  abstract fun registerGeneratedSources(
    project: Project,
    wireTask: TaskProvider<WireTask>,
    generatedSourcesDirectories: Set<File>,
  )
}

private class JvmSource(
  name: String,
  sourceSets: List<String>,
  private val kotlinSourceDirectorySet: SourceDirectorySet?,
  private val javaSourceDirectorySet: SourceDirectorySet?,
) : Source(name, sourceSets) {
  override fun registerGeneratedSources(
    project: Project,
    wireTask: TaskProvider<WireTask>,
    generatedSourcesDirectories: Set<File>,
  ) {
    val taskOutputDirectories = wireTask.map { project.files(it.outputDirectories) }
    if (javaSourceDirectorySet != null) {
      javaSourceDirectorySet.srcDir(taskOutputDirectories)
    } else {
      kotlinSourceDirectorySet?.srcDir(taskOutputDirectories)
    }
  }
}

private class AndroidSource(
  name: String,
  sourceSets: List<String>,
  private val variant: Variant,
  private val kotlin: Boolean,
) : Source(name, sourceSets) {
  override fun registerGeneratedSources(
    project: Project,
    wireTask: TaskProvider<WireTask>,
    generatedSourcesDirectories: Set<File>,
  ) {
    generatedSourcesDirectories.toList().forEachIndexed { i, _ ->
      variant.sources.java?.addGeneratedSourceDirectory(wireTask) { it.outputDirectories[i] }
      if (kotlin) {
        variant.sources.kotlin?.addGeneratedSourceDirectory(wireTask) { it.outputDirectories[i] }
      }
    }
  }
}

private val SOURCE_FUNCTION by lazy(LazyThreadSafetyMode.NONE) {
  KotlinCompile::class.java.getMethod(
    "source",
    JavaArray.newInstance(Any::class.java, 0).javaClass,
  )
}
