/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.java

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import com.squareup.wire.java.internal.ProfileFileElement
import com.squareup.wire.schema.ProtoType

/**
 * Describes how to map `.proto` to `.java`. A single repository of `.proto` files
 * may have multiple profiles; for example a project may target both Android and Java.
 */
class Profile(
  private val profileFiles: List<ProfileFileElement> = emptyList()
) {
  fun getTarget(type: ProtoType): TypeName? {
    val typeConfig = typeConfig(type)
    return if (typeConfig != null) ClassName.bestGuess(typeConfig.target) else null
  }

  fun getAdapter(type: ProtoType): AdapterConstant? {
    val typeConfig = typeConfig(type)
    return if (typeConfig != null) AdapterConstant(typeConfig.adapter) else null
  }

  /** Returns the config for [type], or null if it is not configured.  */
  private fun typeConfig(type: ProtoType) =
    profileFiles
        .flatMap { it.typeConfigs }
        .find { it.type == type.toString() }
}