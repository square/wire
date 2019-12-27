/*
 * Copyright (C) 2013 Square, Inc.
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
package com.squareup.wire.schema.internal.parser

import com.squareup.wire.schema.Location
import com.squareup.wire.schema.internal.appendDocumentation
import com.squareup.wire.schema.internal.appendIndented

data class ServiceElement(
  val location: Location,
  val name: String,
  val documentation: String = "",
  val rpcs: List<RpcElement> = emptyList(),
  val options: List<OptionElement> = emptyList()
) {
  fun toSchema() = buildString{
    appendDocumentation(documentation)
    append("service $name {")
    if (options.isNotEmpty()) {
      append('\n')
      for (option in options) {
        appendIndented(option.toSchemaDeclaration())
      }
    }
    if (rpcs.isNotEmpty()) {
      append('\n')
      for (rpc in rpcs) {
        appendIndented(rpc.toSchema())
      }
    }
    append("}\n")
  }
}
