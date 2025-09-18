/*
 * Copyright (C) 2023 Square, Inc.
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
package com.squareup.wire.gradle.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

internal fun Project.targetDefaultOutputPath(): String {
  return "$buildDir/generated/source/wire"
}

internal fun Project.libraryProtoOutputPath(): String {
  return "$buildDir/wire/proto-sources"
}

internal fun protoProjectDependenciesJvmConfiguration(classLoader: ClassLoader): (Configuration) -> Unit {
  try {
    // We check if the Kotlin Gradle plugin is on the class path.
    Class.forName(
      "org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType",
      false,
      classLoader,
    ) as Class<*>
  } catch (_: ClassNotFoundException) {
    // Kotlin is not available.
    return {
      it.isCanBeResolved = true
      it.isCanBeConsumed = false
    }
  }

  return {
    it.isCanBeResolved = true
    it.isCanBeConsumed = false
    it.attributes { attributesContainer ->
      // TODO(Benoit) If another project, on which this one depends, exposes multiple variants,
      //  Wire won't be able to pick one. We force the resolution to JVM. On the other hand, this
      //  breaks inter-module dependencies for non-jvm modules. We need to fix it.
      attributesContainer.attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
    }
  }
}
