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
import com.squareup.wire.gradle.CustomOutput
import com.squareup.wire.gradle.JavaOutput
import com.squareup.wire.gradle.KotlinOutput
import com.squareup.wire.gradle.WireOutput
import com.squareup.wire.gradle.WireTask
import com.squareup.wire.gradle.internal.targetDefaultOutputPath
import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

internal fun forEachWireSource(
  project: Project,
  hasKotlin: Boolean,
  hasJava: Boolean,
  hasAndroid: Boolean,
  sourceHandler: (WireSource) -> Unit,
) {
  when {
    hasAndroid -> {
      val extension = project.extensions.getByType(AndroidComponentsExtension::class.java)
      extension.onVariants { variant ->
        val sourceSetNames = mutableListOf("main")
        variant.buildType?.let { sourceSetNames.add(it) }
        sourceSetNames.addAll(variant.productFlavors.map { it.second })
        sourceSetNames.add(variant.name)

        val source = AndroidSource(
          name = variant.name,
          sourceSets = sourceSetNames.distinct(),
          variant = variant,
        )
        sourceHandler(source)
      }
    }
    hasKotlin && project.extensions.findByType(KotlinMultiplatformExtension::class.java) != null -> {
      val extension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
      extension.sourceRoots().forEach(sourceHandler)
    }
    hasKotlin -> {
      val kotlinSourceSets = project.extensions.findByType(KotlinProjectExtension::class.java)?.sourceSets
      val javaSourceSets = project.extensions.findByType(SourceSetContainer::class.java)
      val kotlinSourceSet = kotlinSourceSets?.findByName("main")
      val javaSourceDirectorySet = javaSourceSets?.findByName("main")?.java
      val source = JvmOrKmpSource(
        name = "main",
        sourceSets = listOf("main"),
        kotlinSourceSet = kotlinSourceSet,
        javaSourceDirectorySet = javaSourceDirectorySet,
      )
      sourceHandler(source)
    }
    hasJava -> {
      val javaSourceSets = project.extensions.findByType(SourceSetContainer::class.java)
      val javaSourceDirectorySet = javaSourceSets?.findByName("main")?.java
      val source = JvmOrKmpSource(
        name = "main",
        sourceSets = listOf("main"),
        kotlinSourceSet = null,
        javaSourceDirectorySet = javaSourceDirectorySet,
      )
      sourceHandler(source)
    }
    else -> {
      throw IllegalStateException("Wire Gradle plugin requires Android, Kotlin, or Java to be configured on project '${project.name}'.")
    }
  }
}

internal abstract class WireSource(
  val name: String,
  private val sourceSets: List<String>,
) {
  fun defaultSourceFolders(project: Project): Set<String> = sourceSets.map { "src/$it/proto" }
    .filter { path -> File(project.projectDir, path).exists() }
    .toSet()

  abstract fun outputDir(project: Project): File

  abstract fun registerGeneratedSources(
    project: Project,
    wireTask: TaskProvider<WireTask>,
    outputs: List<WireOutput>,
  )
}

private fun KotlinMultiplatformExtension.sourceRoots(): List<WireSource> {
  // Wire only supports commonMain as in other cases, we'd be expected to generate both
  // `expect` and `actual` classes which doesn't make much sense for what Wire does.
  return listOf(
    JvmOrKmpSource(
      name = "commonMain",
      kotlinSourceSet = sourceSets.getByName("commonMain"),
      javaSourceDirectorySet = null,
      sourceSets = listOf("commonMain"),
    ),
  )
}

private class JvmOrKmpSource(
  name: String,
  sourceSets: List<String>,
  private val kotlinSourceSet: KotlinSourceSet?,
  private val javaSourceDirectorySet: SourceDirectorySet?,
) : WireSource(name, sourceSets) {
  override fun outputDir(project: Project): File = File(project.targetDefaultOutputPath())

  override fun registerGeneratedSources(
    project: Project,
    wireTask: TaskProvider<WireTask>,
    outputs: List<WireOutput>,
  ) {
    outputs.forEachIndexed { index, output ->
      val outputDirectory = wireTask.flatMap { it.outputDirectoriesList[index] }
      when (output) {
        is JavaOutput -> {
          javaSourceDirectorySet?.srcDir(outputDirectory)
        }
        is KotlinOutput -> {
          registerKotlinGeneratedSources(kotlinSourceSet, outputDirectory)
        }
        is CustomOutput -> {
          // Custom targets are wildcards, so we add all output directories.
          javaSourceDirectorySet?.srcDir(outputDirectory)
          registerKotlinGeneratedSources(kotlinSourceSet, outputDirectory)
        }
        else -> {
          throw IllegalArgumentException(
            "Wire output ${output::class.simpleName} is not supported in project ${project.path}",
          )
        }
      }
    }
  }
}

private class AndroidSource(
  name: String,
  sourceSets: List<String>,
  private val variant: Variant,
) : WireSource(name, sourceSets) {
  override fun outputDir(project: Project): File = File(project.targetDefaultOutputPath(), name)

  override fun registerGeneratedSources(
    project: Project,
    wireTask: TaskProvider<WireTask>,
    outputs: List<WireOutput>,
  ) {
    outputs.forEachIndexed { index, output ->
      when (output) {
        is JavaOutput -> {
          variant.sources.java?.addGeneratedSourceDirectory(wireTask) { it.outputDirectoriesList[index] }
        }
        is KotlinOutput -> {
          variant.sources.kotlin?.addGeneratedSourceDirectory(wireTask) { it.outputDirectoriesList[index] }
        }
        is CustomOutput -> {
          // Custom targets are wildcards, so we add all output directories.
          variant.sources.java?.addGeneratedSourceDirectory(wireTask) { it.outputDirectoriesList[index] }
          variant.sources.kotlin?.addGeneratedSourceDirectory(wireTask) { it.outputDirectoriesList[index] }
        }
        else -> {
          throw IllegalArgumentException(
            "Wire output ${output::class.simpleName} is not supported in Android project ${project.path}",
          )
        }
      }
    }
  }
}

/**
 * Registers [outputDirectory] as a generated Kotlin source directory on [kotlinSourceSet].
 *
 * On Kotlin 2.3+, uses the [KotlinSourceSet.generatedKotlin] API so that IDEs can distinguish
 * generated sources from handwritten ones. Falls back to [KotlinSourceSet.kotlin] on older
 * versions of the Kotlin Gradle Plugin where the API is not available.
 */
private fun registerKotlinGeneratedSources(
  kotlinSourceSet: KotlinSourceSet?,
  outputDirectory: Provider<Directory>,
) {
  if (kotlinSourceSet == null) return
  // generatedKotlin was introduced experimentally in Kotlin 2.3. Detect it reflectively so that
  // Wire remains compatible with earlier Kotlin Gradle Plugin versions.
  val generatedKotlinMethod = runCatching {
    kotlinSourceSet.javaClass.getMethod("getGeneratedKotlin")
  }.getOrNull()
  if (generatedKotlinMethod != null) {
    val generatedKotlin = generatedKotlinMethod.invoke(kotlinSourceSet) as SourceDirectorySet
    generatedKotlin.srcDir(outputDirectory)
  } else {
    kotlinSourceSet.kotlin.srcDir(outputDirectory)
  }
}
