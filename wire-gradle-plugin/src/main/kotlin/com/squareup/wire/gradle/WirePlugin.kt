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

import com.squareup.wire.gradle.internal.libraryProtoOutputPath
import com.squareup.wire.gradle.internal.protoProjectDependenciesJvmConfiguration
import com.squareup.wire.gradle.kotlin.getWireTaskFactory
import com.squareup.wire.wireVersion
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.UnknownConfigurationException
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet

class WirePlugin : Plugin<Project> {
  private val android = AtomicBoolean(false)
  private val java = AtomicBoolean(false)
  private val kotlin = AtomicBoolean(false)

  private lateinit var extension: WireExtension
  internal lateinit var project: Project

  override fun apply(project: Project) {
    this.extension = project.extensions.create("wire", WireExtension::class.java, project)
    this.project = project

    // Add default 'protoSource' and 'protoPath' configurations, which can be used in the
    // dependencies {} block.
    extension.addProtoPathProtoRootSet()
    extension.addProtoSourceProtoRootSet()

    project.configurations.create("protoProjectDependenciesJvm")
      .also(protoProjectDependenciesJvmConfiguration(WirePlugin::class.java.classLoader))

    val androidPluginHandler = { _: Plugin<*> ->
      android.set(true)
      // When `android.builtInKotlin` property is enabled, AGP provides Kotlin support for all projects without
      // requiring users to apply the `org.jetbrains.kotlin.android` plugin.
      project.extensions.findByName("kotlin")?.let { kotlin.set(true) }
      applyWirePlugin()
    }
    project.plugins.withId("com.android.application", androidPluginHandler)
    project.plugins.withId("com.android.library", androidPluginHandler)
    project.plugins.withId("com.android.instantapp", androidPluginHandler)
    project.plugins.withId("com.android.feature", androidPluginHandler)
    project.plugins.withId("com.android.dynamic-feature", androidPluginHandler)

    val kotlinPluginHandler = { _: Plugin<*> -> kotlin.set(true) }
    project.plugins.withId("org.jetbrains.kotlin.multiplatform", kotlinPluginHandler)
    project.plugins.withId("org.jetbrains.kotlin.jvm", kotlinPluginHandler)
    project.plugins.withId("org.jetbrains.kotlin.js", kotlinPluginHandler)
    project.plugins.withId("kotlin2js", kotlinPluginHandler)
    // When `android.builtInKotlin`` property is disabled, the users  will need to apply either the
    // `com.android.experimental.built-in-kotlin` plugin or the `org.jetbrains.kotlin.android` plugin to have Kotlin.
    project.plugins.withId("com.android.experimental.built-in-kotlin", kotlinPluginHandler)
    project.plugins.withId("org.jetbrains.kotlin.android", kotlinPluginHandler)

    val javaPluginHandler = { _: Plugin<*> -> java.set(true) }
    project.plugins.withId("java", javaPluginHandler)
    project.plugins.withId("java-library", javaPluginHandler)

    project.afterEvaluate {
      if (extension.protoLibrary) {
        extension.proto { protoOutput ->
          protoOutput.out = File(project.libraryProtoOutputPath()).path
        }
      }

      if (!android.get()) {
        applyWirePlugin()
      }

      val outputs = extension.outputs
      check(outputs.isNotEmpty()) {
        "At least one target must be provided for project '${project.path}\n" + "See our documentation for details: https://square.github.io/wire/wire_compiler/#customizing-output"
      }
      val hasJavaOutput = outputs.any { it is JavaOutput }
      val hasKotlinOutput = outputs.any { it is KotlinOutput }
      check(!hasKotlinOutput || kotlin.get()) {
        "Wire Gradle plugin applied in " + "project '${project.path}' to generate Kotlin types but no supported Kotlin plugin was found"
      }

      project.addWireRuntimeDependency(hasJavaOutput, hasKotlinOutput)
    }
  }

  private fun applyWirePlugin() {
    check(android.get() || java.get() || kotlin.get()) {
      "Wire Gradle plugin applied in " + "project '${project.path}' but unable to find either the Java, Kotlin, or Android plugin"
    }

    project.tasks.register(ROOT_TASK) {
      it.group = GROUP
      it.description = "Aggregation task which runs every generation task for every given source"
    }

    val factory = getWireTaskFactory(project, kotlin.get(), java.get(), android.get())
    factory.createWireTasks(project, extension)
  }

  private fun Project.addWireRuntimeDependency(
    hasJavaOutput: Boolean,
    hasKotlinOutput: Boolean,
  ) {
    if (!hasJavaOutput && !hasKotlinOutput) return

    // Indicates when the plugin is applied inside the Wire repo to Wire's own modules.
    val isInternalBuild = project.providers.gradleProperty("com.squareup.wire.internal").getOrElse("false").toBoolean()
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
      project.dependencies.create("com.squareup.wire:wire-runtime:$wireVersion")
    }
  }

  internal companion object {
    const val ROOT_TASK = "generateProtos"
    const val GROUP = "wire"
  }
}
