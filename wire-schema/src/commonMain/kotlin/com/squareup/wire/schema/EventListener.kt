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
package com.squareup.wire.schema

import com.squareup.wire.internal.Serializable
import com.squareup.wire.schema.internal.TypeMover

/**
 * Listener for metrics events. Extend this class to monitor WireRun and all [schema handlers][SchemaHandler]
 * involved in the Protobuf schema manipulation.
 *
 * The events' order is as follows:
 *  - runStart
 *  - loadSchemaStart
 *  - loadSchemaSuccess
 *  - treeShakeStart
 *  - treeShakeEnd
 *  - moveTypesStart
 *  - moveTypesEnd
 *  - schemaHandlersStart // Looping over all handlers.
 *    - schemaHandlerStart
 *    - schemaHandlerEnd
 *  - schemaHandlersEnd
 *  - runSuccess / runFailed
 */
abstract class EventListener {
  /** Invoked prior to Wire starting. */
  open fun runStart(wireRun: WireRun) {}

  /** Invoked after Wire has executed all operations. */
  open fun runSuccess(wireRun: WireRun) {}

  open fun runFailed(errors: List<String>) {}

  /**
   * Invoked prior to loading the Protobuf schema. this includes parsing `.proto` files, and resolving all referenced
   * types.
   */
  open fun loadSchemaStart() {}

  /**
   * Invoked after having loaded the Protobuf [schema]. this includes parsing `.proto` files, and resolving all
   * referenced types.
   */
  open fun loadSchemaSuccess(
    schema: Schema,
  ) {}

  /** Invoked prior to refactoring the Protobuf [schema] by tree-shaking it using the [pruning rules][PruningRules]. */
  open fun treeShakeStart(
    schema: Schema,
    pruningRules: PruningRules,
  ) {}

  /** Invoked after having refactored the Protobuf schema by tree-shaking it using the [pruning rules][PruningRules]. */
  open fun treeShakeEnd(
    refactoredSchema: Schema,
    pruningRules: PruningRules,
  ) {}

  /** Invoked prior to refactoring the Protobuf [schema] by applying the [moves]. */
  open fun moveTypesStart(
    schema: Schema,
    moves: List<TypeMover.Move>,
  ) {}

  /** Invoked after having refactored the Protobuf schema by applying the [moves]. */
  open fun moveTypesEnd(
    refactoredSchema: Schema,
    moves: List<TypeMover.Move>,
  ) {}

  /** Invoked prior to executing all [schema handler][SchemaHandler]. */
  open fun schemaHandlersStart() {}

  /** Invoked after having executed all [schema handler][SchemaHandler]. */
  open fun schemaHandlersEnd() {}

  /** Invoked prior a [schema handler][SchemaHandler] starting. */
  open fun schemaHandlerStart(
    schemaHandler: SchemaHandler,
    emittingRules: EmittingRules,
  ) {
  }

  /** Invoked after a [schema handler][SchemaHandler] has finished. */
  open fun schemaHandlerEnd(
    schemaHandler: SchemaHandler,
    emittingRules: EmittingRules,
  ) {}

  fun interface Factory : Serializable {
    /**
     * Creates an instance of the [EventListener] for one Wire execution. The returned [EventListener] instance will be used during
     * the lifecycle of the Wire's task.
     */
    fun create(): EventListener
  }
}
