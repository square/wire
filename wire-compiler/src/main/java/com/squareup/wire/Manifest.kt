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
package com.squareup.wire

import com.charleskorn.kaml.Yaml
import com.squareup.wire.schema.PruningRules
import com.squareup.wire.schema.WireRun
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

private val defaultRoots = setOf("*")
private val defaultPrunes = emptySet<String>()

@Serializable
private data class ManifestModule(
  val dependencies: Set<String> = emptySet(),
  val roots: Set<String> = defaultRoots,
  val prunes: Set<String> = defaultPrunes,
)

private val serializer = MapSerializer(String.serializer(), ManifestModule.serializer())

internal fun parseManifestModules(yaml: String): Map<String, WireRun.Module> {
  val modules = Yaml.default.decodeFromString(serializer, yaml)
  return modules.mapValues { (_, module) ->
    WireRun.Module(
      dependencies = module.dependencies,
      pruningRules = if (module.roots != defaultRoots || module.prunes != defaultPrunes) {
        PruningRules.Builder()
          .addRoot(module.roots)
          .prune(module.prunes)
          .build()
      } else {
        null
      },
    )
  }
}
