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
import com.squareup.wire.PartitionedSchema.Partition
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.PruningRules
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Service
import com.squareup.wire.schema.Target.SchemaHandler
import com.squareup.wire.schema.Type
import com.squareup.wire.schema.internal.DagChecker
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.nio.file.Path
import java.util.ArrayDeque

data class Manifest(
  val modules: Map<String, Module>
) {
  init {
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
  }

  /** Modules which have no dependencies. */
  val roots: Set<String> = modules.filterValues { it.dependencies.isEmpty() }.keys

  /** Module ordering such that every module will see its dependencies appear before it. */
  val order: List<String> = run {
    val seen = LinkedHashSet<String>() // Insertion order is important to produce the final list!
    val queue = ArrayDeque<String>().apply { addAll(roots) }
    while (queue.isNotEmpty()) {
      val currentName = queue.removeFirst()
      val currentDependencies = modules.getValue(currentName).dependencies
      if (seen.containsAll(currentDependencies)) {
        seen += currentName

        val downstreamModules = modules.filterValues { currentName in it.dependencies }.keys
        queue += downstreamModules
      } else {
        queue += currentName
      }
    }
    seen.toList()
  }

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
      return Manifest(Yaml.default.parse(serializer, string))
    }
  }
}

/** Module name to partition info. The iteration order of this map is the generation order. */
class PartitionedSchema(
  modules: Map<String, Partition>,
  val warnings: List<String>,
  val errors: List<String>
) : Map<String, Partition> by modules {
  class Partition(
    val schema: Schema,
    val types: Set<ProtoType>,
    val upstreamTypes: Map<ProtoType, String>
  )
}

private const val debug = false

fun Schema.partition(manifest: Manifest): PartitionedSchema {
  val modules = manifest.modules

  if (debug) {
    println("Modules: ${modules.keys}")
    println("Roots: ${manifest.roots}")
    println("Order: ${manifest.order}")
  }

  val disjointGraphs = manifest.roots
      .map { root ->
        val reachableNames = mutableSetOf<String>()
        val visitQueue = ArrayDeque<String>().apply { add(root) }
        while (visitQueue.isNotEmpty()) {
          val visitName = visitQueue.removeFirst()
          reachableNames += visitName

          val upstreamModules = modules.getValue(visitName).dependencies
          val downstreamModules = modules.filterValues { visitName in it.dependencies }.keys
          for (dependency in upstreamModules + downstreamModules) {
            if (dependency !in reachableNames && dependency !in visitQueue) {
              visitQueue += dependency
            }
          }
        }
        // Sort the reachable names by their module order.
        manifest.order.filter { it in reachableNames }
      }
      .toSet()
  if (debug) {
    println("Disjoint Graphs:")
    for (disjointGraph in disjointGraphs) {
      println(" - $disjointGraph")
    }
  }

  val errors = mutableListOf<String>()
  val partitions = mutableMapOf<String, Partition>()
  for (moduleName in manifest.order) {
    val module = modules.getValue(moduleName)

    val transitiveDependencySet = mutableSetOf<String>().apply {
      val queue = ArrayDeque<String>().apply { add(moduleName) }
      while (queue.isNotEmpty()) {
        val dependency = modules.getValue(queue.removeFirst())
        addAll(dependency.dependencies)
        queue.addAll(dependency.dependencies)
      }
    } as Set<String> // TODO use buildSet

    val pruningRules = PruningRules.Builder()
        .apply {
          for (dependencyName in transitiveDependencySet) {
            val dependency = modules.getValue(dependencyName)
            addRoot(dependency.roots)
            prune(dependency.prunes)
          }
          addRoot(module.roots)
          // TODO(jw): what do we do about prunes that apply to types in an upstream module? fail?
          prune(module.prunes)
        }
        .build()

    val upstreamTypes = mutableMapOf<ProtoType, String>().apply {
      val duplicateTypes = mutableMapOf<ProtoType, MutableSet<String>>()
      for (dependencyName in transitiveDependencySet) {
        for (type in partitions.getValue(dependencyName).types) {
          val replaced = put(type, dependencyName)
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
    } as Map<ProtoType, String> // TODO use buildMap

    val moduleSchema = prune(pruningRules)

    if (debug) {
      fun Schema.stats() = buildString {
        append(protoFiles.size)
        append(" files, ")
        append(protoFiles.sumBy { it.typesAndNestedTypes().size })
        append(" types, ")
        append(protoFiles.sumBy { it.services.size })
        append(" services")
      }
      println()
      println(moduleName)
      println("  Dependencies: ${module.dependencies}")
      println("  Rules:")
      println("   - roots: ${module.roots}")
      println("   - prunes: ${module.prunes}")
      println("  Schema:")
      println("   - original: ${stats()}")
      println("   - pruned: ${moduleSchema.stats()}")
      println("  Existing: ${upstreamTypes.size} proto types")
      println("  Content:")
    }
    val generatedTypes = mutableSetOf<ProtoType>()
    for (protoFile in moduleSchema.protoFiles) {
      for (type in protoFile.typesAndNestedTypes()) {
        val protoType = type.type
        if (protoType !in upstreamTypes) {
          if (debug) {
            println("   - type: $protoType")
          }
          generatedTypes += protoType
        }
      }
      for (service in protoFile.services) {
        val protoType = service.type()
        if (protoType !in upstreamTypes) {
          if (debug) {
            println("   - service: $protoType")
          }
          generatedTypes += protoType
        }
      }

      partitions[moduleName] = Partition(moduleSchema, generatedTypes, upstreamTypes)
    }
  }

  val warnings = mutableListOf<String>()
  for (moduleGraph in disjointGraphs) {
    for (currentName in moduleGraph) {
      for (otherName in moduleGraph) {
        if (otherName == currentName) {
          break
        }
        val currentTypes = partitions.getValue(currentName).types
        val otherTypes = partitions.getValue(otherName).types
        val duplicates = currentTypes.intersect(otherTypes)
        if (duplicates.isNotEmpty()) {
          val currentModule = modules.getValue(currentName)
          val otherModule = modules.getValue(otherName)
          for (duplicate in duplicates) {
            val duplicateName = duplicate.toString()
            if (duplicateName !in currentModule.roots || duplicateName !in otherModule.roots) {
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

fun PartitionedSchema.buildSchemaHandler(
  factory: (name: String, partition: Partition) -> SchemaHandler
): SchemaHandler {
  // TODO this means you cannot generate the same type in two unrelated modules! fix!
  val handlers = this
      .map { (name, partition) ->
        // Create the handler for each partition.
        partition to factory(name, partition)
      }
      .flatMap { partitionHandlerPair ->
        // Associate each partition type with the partition/handler pair.
        partitionHandlerPair.first.types.map { it to partitionHandlerPair }
      }
      .toMap()

  return object : SchemaHandler {
    override fun handle(type: Type): Path? {
      val protoType = type.type
      return handlers[protoType]?.let { (partition, delegate) ->
        // The schema used to invoke this function is the global one which has not been pruned so we
        // swap its type for the corresponding pruned one.
        // TODO(jw): Split WireRun into two phases so we can insert our pruned schema before step 4.
        val prunedType = partition.schema.getType(protoType)!!
        delegate.handle(prunedType)
      }
    }
    override fun handle(service: Service): List<Path> {
      val protoType = service.type()
      return handlers[protoType]?.let { (partition, delegate) ->
        // The schema used to invoke this function is the global one which has not been pruned so we
        // swap its type for the corresponding pruned one.
        // TODO(jw): Split WireRun into two phases so we can insert our pruned schema before step 4.
        val prunedType = partition.schema.getService(protoType)!!
        delegate.handle(prunedType)
      } ?: emptyList()
    }
  }
}
