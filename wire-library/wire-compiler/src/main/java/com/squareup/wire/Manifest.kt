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
package com.squareup.wire

import com.charleskorn.kaml.Yaml
import com.squareup.wire.schema.internal.DagChecker
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

data class Manifest(
  val modules: Map<String, Module>
) {
  @Serializable
  data class Module(
    val dependencies: Set<String> = emptySet(),
    val roots: Set<String> = setOf("*"),
    val prunes: Set<String> = emptySet()
  )

  companion object {
    @JvmStatic
    val NONE = Manifest(
        modules = mapOf("./" to Module())
    )

    private val serializer = MapSerializer(String.serializer(), Module.serializer())

    fun fromYaml(string: String): Manifest {
      val modules = Yaml.default.parse(serializer, string)

      val dagChecker = DagChecker(modules.keys) { moduleName ->
        modules.getValue(moduleName).dependencies
      }
      val cycles = dagChecker.check()
      require(cycles.isEmpty()) {
        buildString {
          append("ERROR: Manifest modules contain dependency cycles:\n")
          for (cycle in cycles) {
            append(" - ")
            append(cycle)
            append('\n')
          }
        }
      }

      return Manifest(modules)
    }
  }
}
