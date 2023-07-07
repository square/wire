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
package com.squareup.wire.gradle

import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.internal.TypeMover
import java.io.Serializable
import javax.inject.Inject

/**
 * A directive to move a type to a new location and adjust all references to the type in this
 * schema. Typically this is used with `proto` output to refactor a proto project.
 *
 * ```
 * wire {
 *   move {
 *     type = "squareup.geology.Period"
 *     targetPath = "squareup/geology/geology.proto"
 *   }
 *
 *   proto {}
 * }
 * ```
 */
open class Move @Inject constructor() : Serializable {
  var type: String? = null
  var targetPath: String? = null

  fun toTypeMoverMove(): TypeMover.Move {
    check(type != null) { "move requires a type but was null" }
    check(targetPath != null) { "move requires a targetPath but was null" }

    return TypeMover.Move(
      type = ProtoType.get(type),
      targetPath = targetPath!!,
    )
  }
}
