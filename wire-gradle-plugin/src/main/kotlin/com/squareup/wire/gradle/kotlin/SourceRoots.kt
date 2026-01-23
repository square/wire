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
import com.squareup.wire.gradle.WireTask
import com.squareup.wire.gradle.internal.targetDefaultOutputPath
import com.squareup.wire.schema.CustomTarget
import com.squareup.wire.schema.JavaTarget
import com.squareup.wire.schema.KotlinTarget
import com.squareup.wire.schema.ProtoTarget
import com.squareup.wire.schema.Target
import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

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
      val kotlinSourceDirectorySet = kotlinSourceSets?.findByName("main")?.kotlin
      val javaSourceDirectorySet = javaSourceSets?.findByName("main")?.java
      val source = JvmOrKmpSource(
        name = "main",
        sourceSets = listOf("main"),
        kotlinSourceDirectorySet = kotlinSourceDirectorySet,
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
        kotlinSourceDirectorySet = null,
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
  fun defaultSourceFolders(project: Project): Set<String> {
    return sourceSets.map { "src/$it/proto" }
      .filter { path -> File(project.projectDir, path).exists() }
      .toSet()
  }

  abstract fun outputDir(project: Project): File

  abstract fun registerGeneratedSources(
    project: Project,
    wireTask: TaskProvider<WireTask>,
    targets: List<Target>,
  )
}

private fun KotlinMultiplatformExtension.sourceRoots(): List<WireSource> {
  // Wire only supports commonMain as in other cases, we'd be expected to generate both
  // `expect` and `actual` classes which doesn't make much sense for what Wire does.
  return listOf(
    JvmOrKmpSource(
      name = "commonMain",
      kotlinSourceDirectorySet = sourceSets.getByName("commonMain").kotlin,
      javaSourceDirectorySet = null,
      sourceSets = listOf("commonMain"),
    ),
  )
}

private class JvmOrKmpSource(
  name: String,
  sourceSets: List<String>,
  private val kotlinSourceDirectorySet: SourceDirectorySet?,
  private val javaSourceDirectorySet: SourceDirectorySet?,
) : WireSource(name, sourceSets) {
  override fun outputDir(project: Project): File {
    return File(project.targetDefaultOutputPath())
  }

  override fun registerGeneratedSources(
    project: Project,
    wireTask: TaskProvider<WireTask>,
    targets: List<Target>,
  ) {
    targets.forEachIndexed { index, target ->
      val outputDirectory = wireTask.flatMap { it.outputDirectoriesList[index] }
      when (target) {
        is JavaTarget -> {
          javaSourceDirectorySet?.srcDir(outputDirectory)
        }
        is KotlinTarget -> {
          kotlinSourceDirectorySet?.srcDir(outputDirectory)
        }
        is CustomTarget -> {
          // Custom targets are wildcards, so we add all output directories.
          javaSourceDirectorySet?.srcDir(outputDirectory)
          kotlinSourceDirectorySet?.srcDir(outputDirectory)
        }
        is ProtoTarget -> {
          // Do nothing
        }
        else -> {
          throw IllegalArgumentException(
            "Wire target ${target::class.simpleName} is not supported in project ${project.path}",
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
  override fun outputDir(project: Project): File {
    return File(project.targetDefaultOutputPath(), name)
  }

  override fun registerGeneratedSources(
    project: Project,
    wireTask: TaskProvider<WireTask>,
    targets: List<Target>,
  ) {
    targets.forEachIndexed { index, target ->
      when (target) {
        is JavaTarget -> {
          variant.sources.java?.addGeneratedSourceDirectory(wireTask) { it.outputDirectoriesList[index] }
        }
        is KotlinTarget -> {
          variant.sources.kotlin?.addGeneratedSourceDirectory(wireTask) { it.outputDirectoriesList[index] }
          // Remove line below when AGP is upgraded to 9.0+ as it will contain fix for https://issuetracker.google.com/446220448
          variant.sources.java?.addGeneratedSourceDirectory(wireTask) { it.outputDirectoriesList[index] }
        }
        is CustomTarget -> {
          // Custom targets are wildcards, so we add all output directories.
          variant.sources.java?.addGeneratedSourceDirectory(wireTask) { it.outputDirectoriesList[index] }
          variant.sources.kotlin?.addGeneratedSourceDirectory(wireTask) { it.outputDirectoriesList[index] }
        }
        is ProtoTarget -> {
          // Do nothing
        }
        else -> {
          throw IllegalArgumentException(
            "Wire target ${target::class.simpleName} is not supported in Android project ${project.path}",
          )
        }
      }
    }
  }
}
