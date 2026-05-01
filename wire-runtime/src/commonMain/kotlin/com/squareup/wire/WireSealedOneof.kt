/*
 * Copyright (C) 2026 Square, Inc.
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

/**
 * Annotates a sealed class generated for a oneof with its position in the enclosing message's
 * primary constructor. This is used by [com.squareup.wire.KotlinConstructorBuilder] to reconstruct
 * the message when no explicit `Builder` class is present (i.e. `javaInterop = false`).
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class WireSealedOneof(
  /**
   * This is the order that this oneof was declared in the `.proto` schema, counting all
   * constructor parameters (regular fields and sealed oneofs). Used to reconstruct the correct
   * constructor call order.
   */
  val schemaIndex: Int,
)
