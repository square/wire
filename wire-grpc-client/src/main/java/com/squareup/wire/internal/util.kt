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
package com.squareup.wire.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Invokes `block` and returns its result to `continuation`. Useful in reflection code when you
 * can't implement a `suspend fun` directly.
 */
internal fun <T> CoroutineContext.invokeSuspending(
  continuation: Continuation<T>,
  block: suspend () -> T
): Any {
  val deferred = CoroutineScope(this).async {
    block()
  }

  deferred.invokeOnCompletion { cause: Throwable? ->
    if (cause != null) {
      continuation.resumeWithException(cause)
    } else {
      continuation.resume(deferred.getCompleted())
    }
  }

  return kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
}

/**
 * Returns this type's first type parameter. For example, given `List<String>`, this returns
 * `String`. For Kotlin the returned type parameter is often a wildcard like `? extends Number`.
 */
internal fun Type.genericParameterType(index: Int = 0): Type {
  when (this) {
    is ParameterizedType -> return actualTypeArguments[index]
    else -> throw IllegalArgumentException("no generic parameter type: $this")
  }
}

/**
 * Returns the raw type of this type. If this is a raw type already it is returned. For example,
 * given `? extends String`, this returns `String`.
 */
internal fun Type.rawType(): Class<*> {
  return when (this) {
    is Class<*> -> this
    is WildcardType -> lowerBounds[0].rawType()
    is ParameterizedType -> rawType.rawType()
    else -> throw IllegalArgumentException("no raw type: $this")
  }
}
