/*
 * Copyright (C) 2020 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.gradle.kotlin

import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_DSL_NAME
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_JS_DSL_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

internal val AndroidSourceSet.kotlinSourceDirectorySet: SourceDirectorySet?
  get() = kotlinSourceSet

internal val SourceSet.kotlin: SourceDirectorySet?
  get() = kotlinSourceSet

private val Any.kotlinSourceSet: SourceDirectorySet?
  get() {
    val convention =
      (getConvention(KOTLIN_DSL_NAME) ?: getConvention(KOTLIN_JS_DSL_NAME)) ?: return null
    val kotlinSourceSetIface =
      convention.javaClass.interfaces.find { it.name == KotlinSourceSet::class.qualifiedName }
    val getKotlin = kotlinSourceSetIface?.methods?.find { it.name == "getKotlin" } ?: return null
    return getKotlin(convention) as? SourceDirectorySet
  }

private fun Any.getConvention(name: String): Any? =
  (this as HasConvention).convention.plugins[name]
