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

import com.android.build.api.dsl.AndroidSourceDirectorySet
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.squareup.wire.gradle.WirePlugin
import com.squareup.wire.gradle.WireTask
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/**
 * @return A list of source roots and their dependencies.
 *
 * Examples:
 *   Multiplatform Environment. Ios target labeled "ios".
 *     -> iosMain deps [commonMain]
 *
 *   Android environment. internal, production, release, debug variants.
 *     -> internalDebug deps [internal, debug, main]
 *     -> internalRelease deps [internal, release, main]
 *     -> productionDebug deps [production, debug, main]
 *     -> productionRelease deps [production, release, main]
 *
 *    Multiplatform environment with android target (oh boy)
 */
internal fun WirePlugin.sourceRoots(kotlin: Boolean, java: Boolean): List<Source> {
  // Multiplatform project.
  project.extensions.findByType(KotlinMultiplatformExtension::class.java)?.let {
    return it.sourceRoots()
  }

  // Java project.
  if (!kotlin && java) {
    val sourceSets = project.property("sourceSets") as SourceSetContainer
    return listOf(
      Source(
        type = KotlinPlatformType.jvm,
        kotlinSourceDirectorySet = null,
        javaSourceDirectorySet = WireSourceDirectorySet.of(sourceSets.getByName("main").java),
        name = "main",
        sourceSets = listOf("main"),
      ),
    )
  }

  // Android project.
  project.extensions.findByName("android")?.let {
    return (it as BaseExtension).sourceRoots(project, kotlin)
  }

  // Kotlin project.
  val sourceSets = (project.extensions.getByName("kotlin") as KotlinProjectExtension).sourceSets
  val sourceDirectorySet =
    WireSourceDirectorySet.of(sourceDirectorySet = sourceSets.getByName("main").kotlin)
  return listOf(
    Source(
      type = KotlinPlatformType.jvm,
      kotlinSourceDirectorySet = sourceDirectorySet,
      javaSourceDirectorySet = null,
      name = "main",
      sourceSets = listOf("main"),
    ),
  )
}

private fun KotlinMultiplatformExtension.sourceRoots(): List<Source> {
  // Wire only supports commonMain as in other cases, we'd be expected to generate both
  // `expect` and `actual` classes which doesn't make much sense for what Wire does.
  return listOf(
    Source(
      type = KotlinPlatformType.common,
      name = "commonMain",
      variantName = null,
      kotlinSourceDirectorySet = WireSourceDirectorySet.of(sourceSets.getByName("commonMain").kotlin),
      javaSourceDirectorySet = null,
      sourceSets = listOf("commonMain"),
    ),
  )
}

private fun BaseExtension.sourceRoots(project: Project, kotlin: Boolean): List<Source> {
  val variants: DomainObjectSet<out BaseVariant> = when (this) {
    is AppExtension -> applicationVariants
    is LibraryExtension -> libraryVariants
    else -> throw IllegalStateException("Unknown Android plugin $this")
  }
  val androidSourceSets: Map<String, AndroidSourceDirectorySet>? =
    if (kotlin) {
      null
    } else {
      sourceSets
        .associate { sourceSet ->
          sourceSet.name to sourceSet.java
        }
    }
  val sourceSets: Map<String, SourceDirectorySet?>? = run {
    if (kotlin) {
      val kotlinSourceSets =
        (project.extensions.getByName("kotlin") as KotlinProjectExtension).sourceSets
      sourceSets
        .associate { sourceSet ->
          sourceSet.name to kotlinSourceSets.getByName(sourceSet.name).kotlin
        }
    } else {
      null
    }
  }

  return variants.map { variant ->
    val kotlinSourceDirectSet = when {
      kotlin -> {
        val sourceDirectorySet = sourceSets!![variant.name]!!
        WireSourceDirectorySet.of(sourceDirectorySet)
      }

      else -> null
    }
    val androidSourceDirectorySet = androidSourceSets?.get(variant.name)
    if (!kotlin) checkNotNull(androidSourceDirectorySet)
    val javaSourceDirectorySet = when {
      androidSourceDirectorySet != null -> WireSourceDirectorySet.of(androidSourceDirectorySet)
      else -> null
    }

    Source(
      type = KotlinPlatformType.androidJvm,
      kotlinSourceDirectorySet = kotlinSourceDirectSet,
      javaSourceDirectorySet = javaSourceDirectorySet,
      name = variant.name,
      variantName = variant.name,
      sourceSets = variant.sourceSets.map { it.name },
      registerGeneratedDirectory = { outputDirectory ->
        variant.addJavaSourceFoldersToModel(outputDirectory.get().files)
      },
      registerTaskDependency = { task ->
        // TODO: Lazy task configuration!!!
        variant.registerJavaGeneratingTask(task.get(), task.get().outputDirectories.files)
        val compileTaskName =
          if (kotlin) {
            """compile${variant.name.capitalize()}Kotlin"""
          } else {
            """compile${variant.name.capitalize()}Sources"""
          }
        project.tasks.named(compileTaskName).dependsOn(task)
      },
    )
  }
}

internal data class Source(
  val type: KotlinPlatformType,
  val kotlinSourceDirectorySet: WireSourceDirectorySet?,
  val javaSourceDirectorySet: WireSourceDirectorySet?,
  val name: String,
  val variantName: String? = null,
  val sourceSets: List<String>,
  val registerGeneratedDirectory: ((Provider<ConfigurableFileCollection>) -> Unit)? = null,
  val registerTaskDependency: ((TaskProvider<WireTask>) -> Unit)? = null,
)

internal class WireSourceDirectorySet private constructor(
  private val sourceDirectorySet: SourceDirectorySet?,
  private val androidSourceDirectorySet: AndroidSourceDirectorySet?,
) {
  init {
    check(
      (sourceDirectorySet == null || androidSourceDirectorySet == null) &&
        (sourceDirectorySet != null || androidSourceDirectorySet != null),
    ) {
      "At least and at most one of sourceDirectorySet, androidSourceDirectorySet should be non-null"
    }
  }

  /** Adds the path to this set. */
  fun srcDir(fileCollectionProvider: Provider<ConfigurableFileCollection>): WireSourceDirectorySet {
    sourceDirectorySet?.srcDir(fileCollectionProvider)
    androidSourceDirectorySet?.setSrcDirs(fileCollectionProvider.get().files)

    return this
  }

  /** Adds the path to this set. */
  fun srcDir(path: String): WireSourceDirectorySet {
    sourceDirectorySet?.srcDir(path)
    androidSourceDirectorySet?.srcDir(path)

    return this
  }

  companion object {
    fun of(sourceDirectorySet: SourceDirectorySet): WireSourceDirectorySet {
      return WireSourceDirectorySet(sourceDirectorySet, null)
    }

    fun of(androidSourceDirectorySet: AndroidSourceDirectorySet?): WireSourceDirectorySet {
      return WireSourceDirectorySet(null, androidSourceDirectorySet)
    }
  }
}
