/*
 * Copyright 2020 Square Inc.
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

import com.squareup.wire.Message
import com.squareup.wire.Syntax
import com.squareup.wire.internal.FieldBinding.JsonFormatter
import java.lang.reflect.Type

/**
 * Integrates a JSON library like Moshi or Gson into proto. This rigid interface attempts to make it
 * easy all JSON libraries to encode and decode JSON in the exact same way.
 */
abstract class JsonIntegration<F, A> {
  /** Returns [framework]'s built-in adapter for [type]. */
  abstract fun frameworkAdapter(framework: F, type: Type): A

  /** Returns an adapter iterates a list of the target adapter. */
  abstract fun listAdapter(elementAdapter: A): A

  /** Returns an adapter iterates keys and values of the target adapter. */
  // TODO(jwilson): fix frameworks so this takes key+value adapters instead of their types.
  abstract fun mapAdapter(
    framework: F,
    keyType: Type,
    valueType: Type
  ): A

  /**
   * Returns an adapter that handles trees of Maps, Lists, and other JSON types. Should always
   * serialize nulls, including when they are values in maps.
   */
  abstract fun structAdapter(framework: F): A

  /** Returns an adapter that applies [jsonStringAdapter] to each value. */
  abstract fun formatterAdapter(jsonStringAdapter: JsonFormatter<*>): A

  /** Returns a JSON adapter for [field]. */
  fun <M : Message<M, B>, B : Message.Builder<M, B>> jsonAdapter(
    framework: F,
    syntax: Syntax,
    field: FieldBinding<M, B>
  ): A {
    if (field.singleAdapter().isStruct) {
      return structAdapter(framework)
    }

    val jsonStringAdapter = field.jsonStringAdapter(syntax)
    val singleAdapter = when {
      jsonStringAdapter != null -> formatterAdapter(jsonStringAdapter)
      else -> frameworkAdapter(framework, field.singleAdapter().type?.javaObjectType as Type)
    }

    return when {
      field.label.isRepeated -> listAdapter(singleAdapter)
      field.isMap -> {
        val keyType = field.keyAdapter().type?.javaObjectType!!
        val valueType = field.singleAdapter().type?.javaObjectType!!
        mapAdapter(
            framework = framework,
            keyType = keyType,
            valueType = valueType
        )
      }
      else -> singleAdapter
    }
  }
}
