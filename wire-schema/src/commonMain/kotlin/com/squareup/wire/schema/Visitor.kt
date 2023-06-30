package com.squareup.wire.schema

import com.squareup.wire.schema.internal.TypeMover

/**
 * Extend this class to audit Wire, receive metrics, and validate them, including all [schema handlers][SchemaHandler]
 * involved in the Protobuf schema manipulation.
 *
 * The events' order is as follows:
 *  - start
 *  - loadSchemaStart
 *  - loadSchemaEnd
 *  - treeShakeStart
 *  - treeShakeEnd
 *  - moveTypesStart
 *  - moveTypesEnd
 *  - schemaHandlersStart // Looping over all handlers.
 *    - schemaHandlerStart
 *    - schemaHandlerEnd
 *  - schemaHandlersEnd
 *  - validate
 *  - end
 */
abstract class Visitor {
  /**
   * Invoked prior to [end]. Use this method if you want to wait for the completion of all Wire's operations before
   * executing some work.
   */
  open fun validate() {}

  /** Invoked prior to Wire starting. */
  open fun start() {}

  /** Invoked after Wire has executed all operations. This is the last visitor's method called. */
  open fun end() {}

  /**
   * Invoked prior to loading the Protobuf schema. this includes parsing `.proto` files, and resolving all referenced
   * types.
   */
  open fun loadSchemaStart() {}

  /**
   * Invoked after having loaded the Protobuf [schema]. this includes parsing `.proto` files, and resolving all
   * referenced types.
   */
  open fun loadSchemaEnd(
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
    targetName: String,
    emittingRules: EmittingRules,
  ) {}

  /** Invoked after a [schema handler][SchemaHandler] has finished. */
  open fun schemaHandlerEnd(
    targetName: String,
    emittingRules: EmittingRules,
  ) {}

  fun interface Factory {
    /**
     * Creates an instance of the [Visitor] for one Wire execution. The returned [Visitor] instance will be used during
     * the lifecycle of the Wire's task.
     */
    fun create(): Visitor
  }
}
