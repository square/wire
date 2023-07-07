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
package com.squareup.wire.schema

/**
 * Collects errors to be reported as a batch. Errors include both a detail message plus context of
 * where they occurred within the schema.
 */
class ErrorCollector {
  private val errorsBuilder: MutableList<String>
  val errors: List<String> get() = errorsBuilder.toList()
  private val contextStack: List<Any>

  constructor() {
    this.errorsBuilder = mutableListOf()
    this.contextStack = listOf()
  }

  private constructor(enclosing: ErrorCollector, contextStack: List<Any>) {
    this.errorsBuilder = enclosing.errorsBuilder
    this.contextStack = contextStack
  }

  /**
   * Returns a copy of this error collector that includes [additionalContext] in error messages
   * reported to it. The current and returned instance both contribute errors to the same list.
   */
  fun at(additionalContext: Any) = ErrorCollector(this, contextStack + additionalContext)

  /** Add [message] as an error to this collector. */
  operator fun plusAssign(message: String) {
    val error = StringBuilder()
    error.append(message)

    val contextStack = contextStack.toMutableList()
    if (contextStack.any { it !is ProtoFile }) {
      contextStack.removeAll { it is ProtoFile }
    }
    for (i in contextStack.indices.reversed()) {
      val context = contextStack[i]
      val prefix = if (i == contextStack.size - 1) "\n  for" else "\n  in"

      when (context) {
        is Rpc -> error.append("$prefix rpc ${context.name} (${context.location})")
        is Field -> error.append("$prefix field ${context.name} (${context.location})")
        is MessageType -> error.append("$prefix message ${context.type} (${context.location})")
        is EnumConstant -> error.append("$prefix constant ${context.name} (${context.location})")
        is EnumType -> error.append("$prefix enum ${context.type} (${context.location})")
        is Service -> error.append("$prefix service ${context.type} (${context.location})")
        is Extensions -> error.append("$prefix extensions (${context.location})")
        is ProtoFile -> error.append("$prefix file ${context.location}")
        is Extend -> {
          if (context.type != null) {
            error.append("$prefix extend ${context.type} (${context.location})")
          } else {
            error.append("$prefix extend (${context.location})")
          }
        }
      }
    }
    errorsBuilder += error.toString()
  }

  fun throwIfNonEmpty() {
    if (errorsBuilder.isNotEmpty()) {
      throw SchemaException(errorsBuilder)
    }
  }
}
