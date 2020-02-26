/*
 * Copyright 2019 Square Inc.
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
@file:JvmName("GrpcUtils")

package com.squareup.wire.internal

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

/**
 * Returns the raw type of this type. If this is a raw type already it is returned. For example,
 * given `? extends String`, this returns `String`.
 */
internal fun Type.rawType(): Class<*> {
  return when (this) {
    is Class<*> -> this
    is WildcardType -> {
      if (lowerBounds.isNotEmpty()) lowerBounds[0].rawType()
      else upperBounds[0].rawType()
    }
    is ParameterizedType -> rawType.rawType()
    else -> throw IllegalArgumentException("no raw type: $this")
  }
}
