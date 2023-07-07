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

import com.squareup.wire.schema.PartitionedSchema.Partition
import com.squareup.wire.schema.WireRun.Module
import com.squareup.wire.schema.internal.withStubs

internal class PartitionedSchema(
  /** Module name to partition info. The iteration order of this map is the generation order. */
  val partitions: Map<String, Partition>,
  val warnings: List<String>,
  val errors: List<String>,
) {
  class Partition(
    val schema: Schema,
    /** The types that this partition will generate. */
    val types: Set<ProtoType> = schema.types,
    /** These are the types depended upon by [types] associated with their module name. */
    val transitiveUpstreamTypes: Map<ProtoType, String> = emptyMap(),
  )
}

internal fun Schema.partition(modules: Map<String, Module>): PartitionedSchema {
  val moduleGraph = DirectedAcyclicGraph(modules.keys) { modules.getValue(it).dependencies }

  val errors = mutableListOf<String>()
  val partitions = mutableMapOf<String, Partition>()
  for (moduleName in moduleGraph.topologicalOrder()) {
    val module = modules.getValue(moduleName)

    val upstreamTypes = buildMap {
      val duplicateTypes = mutableMapOf<ProtoType, MutableSet<String>>()
      for (dependencyName in moduleGraph.transitiveNodes(moduleName)) {
        for (type in partitions.getValue(dependencyName).types) {
          val replaced: String? = put(type, dependencyName)
          if (replaced != null) {
            duplicateTypes.getOrPut(type) { mutableSetOf(replaced) }.add(dependencyName)
          }
        }
      }
      for ((duplicate, sourceModules) in duplicateTypes) {
        errors += """
          |$moduleName sees $duplicate in ${sourceModules.joinToString()}.
          |  In order to avoid confusion and incompatibility, either make one of these modules
          |  depend on the other or move this type up into a common dependency.
        """.trimMargin()
      }
    }

    // Replace types which have already been generated with stub types that have no external
    // references. This ensures our types can still link. More critically, it ensures that
    // transitive types which were pruned upstream will only be generated in this module if they
    // are reachable from this module's types.
    val stubbedSchema = withStubs(upstreamTypes.keys)

    val prunedSchema = if (module.pruningRules != null) {
      stubbedSchema.prune(module.pruningRules)
    } else {
      stubbedSchema
    }

    val ownedTypes = prunedSchema.protoFiles
      .flatMap { protoFile ->
        val messageTypes = protoFile.typesAndNestedTypes().map { it.type }
        val serviceTypes = protoFile.services.map { it.type }
        messageTypes + serviceTypes
      }
      .filter { it !in upstreamTypes }
      .toSet()

    partitions[moduleName] = Partition(prunedSchema, ownedTypes, upstreamTypes)
  }

  val warnings = mutableListOf<String>()
  for (subgraph in moduleGraph.disjointGraphs()) {
    val subgraphList = subgraph.toList()
    for ((index, currentName) in subgraphList.withIndex()) {
      for (otherName in subgraphList.drop(index + 1)) {
        val currentTypes = partitions.getValue(currentName).types
        val otherTypes = partitions.getValue(otherName).types
        val duplicates = currentTypes.intersect(otherTypes)
        if (duplicates.isNotEmpty()) {
          val currentModule = modules.getValue(currentName)
          val otherModule = modules.getValue(otherName)
          for (duplicate in duplicates) {
            val duplicateName = duplicate.toString()
            val currentModuleRoots = currentModule.pruningRules?.roots ?: emptySet()
            val otherModuleRoots = otherModule.pruningRules?.roots ?: emptySet()
            if (duplicateName !in currentModuleRoots || duplicateName !in otherModuleRoots) {
              warnings += """
                |$duplicate is generated twice in peer modules $currentName and $otherName.
                |  Consider moving this type into a common dependency of both modules.
                |  To suppress this warning, explicitly add the type to the roots of both modules.
              """.trimMargin()
            }
          }
        }
      }
    }
  }

  return PartitionedSchema(partitions, warnings, errors)
}
