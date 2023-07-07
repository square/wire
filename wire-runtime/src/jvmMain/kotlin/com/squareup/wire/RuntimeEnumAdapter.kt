/*
 * Copyright (C) 2013 Square, Inc.
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
package com.squareup.wire

import com.squareup.wire.internal.identityOrNull
import java.lang.reflect.Method

/**
 * Converts values of an enum to and from integers using reflection.
 */
class RuntimeEnumAdapter<E : WireEnum> internal constructor(
  private val javaType: Class<E>,
  syntax: Syntax,
) : EnumAdapter<E>(javaType.kotlin, syntax, javaType.identityOrNull) {
  // Obsolete; for Java classes generated before syntax were added.
  constructor(javaType: Class<E>) : this(javaType, Syntax.PROTO_2)

  private var fromValueMethod: Method? = null // Lazy to avoid reflection during class loading.

  private fun getFromValueMethod(): Method {
    return fromValueMethod ?: javaType.getMethod("fromValue", Int::class.javaPrimitiveType).also {
      fromValueMethod = it
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun fromValue(value: Int): E = getFromValueMethod().invoke(null, value) as E

  override fun equals(other: Any?) = other is RuntimeEnumAdapter<*> && other.type == type

  override fun hashCode() = type.hashCode()

  companion object {
    @JvmStatic fun <E : WireEnum> create(
      enumType: Class<E>,
    ): RuntimeEnumAdapter<E> {
      val defaultAdapter = get(enumType as Class<*>)
      return RuntimeEnumAdapter(enumType, defaultAdapter.syntax)
    }
  }
}
