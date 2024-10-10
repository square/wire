/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.wire.schema

import com.palantir.javapoet.ClassName
import com.palantir.javapoet.TypeName
import com.squareup.kotlinpoet.ClassName as KClassName
import com.squareup.kotlinpoet.TypeName as KTypeName
import com.squareup.wire.schema.internal.ProfileFileElement

/**
 * Describes how to map `.proto` to `.java`. A single repository of `.proto` files
 * may have multiple profiles; for example a project may target both Android and Java.
 */
actual class Profile actual constructor(
  private val profileFiles: List<ProfileFileElement>,
) {
  fun javaTarget(type: ProtoType): TypeName? {
    val typeConfig = typeConfig(type)
    return if (typeConfig != null) ClassName.bestGuess(typeConfig.target) else null
  }

  fun kotlinTarget(type: ProtoType): KTypeName? {
    val typeConfig = typeConfig(type)
    return if (typeConfig != null) KClassName.bestGuess(typeConfig.target!!) else null
  }

  fun getAdapter(type: ProtoType): AdapterConstant? {
    val typeConfig = typeConfig(type)
    return if (typeConfig != null) AdapterConstant(typeConfig.adapter!!) else null
  }

  /** Returns the config for [type], or null if it is not configured.  */
  private fun typeConfig(type: ProtoType) =
    profileFiles
      .flatMap { it.typeConfigs }
      .find { it.type == type.toString() }
}
