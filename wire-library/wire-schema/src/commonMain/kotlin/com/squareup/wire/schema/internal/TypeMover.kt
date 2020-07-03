/*
 * Copyright (C) 2020 Square, Inc.
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
package com.squareup.wire.schema.internal

import com.squareup.wire.schema.Field
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Type

/**
 * Refactor a schema by moving a proto type declaration.
 *
 * This class attempts to avoid making unnecessary changes to the target schema. For example, it
 * won't remove unused imports if they are unrelated to the types being moved.
 */
internal class TypeMover(
  private val oldSchema: Schema,
  private val moves: List<Move>
) {
  /** The working copy of proto files. This is mutated as we perform the moves. */
  private val pathToFile = oldSchema.protoFiles.associateBy { it.location.path }.toMutableMap()

  /** Paths that have had types added or removed. */
  private val sourceAndTargetPaths = mutableSetOf<String>()

  /** Indexes for import updates. */
  private val typeToPath = mutableMapOf<ProtoType, String>()
  private val pathToTypes = mutableMapOf<String, Set<ProtoType>>()

  /** Errors accumulated by this move. */
  private val errors = mutableListOf<String>()

  fun move(): Schema {
    // Move the types.
    for (move in moves) {
      val targetPath = move.targetPath

      val oldSourceProtoFile: ProtoFile? = oldSchema.protoFile(move.type)
      if (oldSourceProtoFile == null) {
        errors += "cannot move ${move.type}, it isn't in this schema"
        continue
      }

      val sourceTypes = oldSourceProtoFile.types.toMutableList()
      val typeIndex = sourceTypes.indexOfFirst { it.type == move.type }
      val movedType = sourceTypes.removeAt(typeIndex)

      val newSourceProtoFile = oldSourceProtoFile.copy(types = sourceTypes)
      pathToFile[newSourceProtoFile.location.path] = newSourceProtoFile

      val targetProtoFile = oldSchema.protoFile(targetPath)
          ?: oldSourceProtoFile.emptyCopy(targetPath)
      val newTargetProtoFile = targetProtoFile.copy(types = targetProtoFile.types + movedType)
      pathToFile[newTargetProtoFile.location.path] = newTargetProtoFile

      sourceAndTargetPaths += newSourceProtoFile.location.path
      sourceAndTargetPaths += newTargetProtoFile.location.path
    }

    // Build an index of types and paths so we know what's where when fixing imports.
    for ((path, protoFile) in pathToFile) {
      val declaredTypes = mutableSetOf<ProtoType>()
      protoFile.collectDeclaredTypes(declaredTypes)
      pathToTypes[path] = declaredTypes
      for (protoType in declaredTypes) {
        typeToPath[protoType] = path
      }
    }

    // Fix imports.
    val updatedProtoFiles = pathToFile.values.map { it.fixImports() }

    checkForErrors()

    return Schema(updatedProtoFiles)
  }

  private fun ProtoFile.fixImports(): ProtoFile {
    if (location.path !in sourceAndTargetPaths &&
        sourceAndTargetPaths.none { it in imports || it in publicImports }) {
      return this // This file isn't impacted. Skip it.
    }

    val referencedTypes = mutableSetOf<ProtoType>()
    collectReferencedTypes(referencedTypes)

    val definitelyNeed = mutableSetOf<ProtoType>()
    val possiblyDrop = mutableSetOf<ProtoType>()

    for (move in moves) {
      if (move.type in referencedTypes) {
        definitelyNeed += move.type
      } else {
        possiblyDrop += move.type
      }

      // If this file is where the type moved from, we might not need imports for the type's use.
      if (oldSchema.protoFile(move.type)!!.location.path == location.path) {
        getType(move).collectReferencedTypes(possiblyDrop)
      }

      // If this file is where the type moved to, we'll need imports for the type's use.
      if (location.path == move.targetPath) {
        getType(move).collectReferencedTypes(definitelyNeed)
      }
    }

    // Promote the possible drop list into a definite drop list.
    val obsoleteImports = mutableSetOf<String>()
    for (type in possiblyDrop) {
      val path = typeToPath[type] ?: continue // Probably a built-in type like string.
      val otherTypesInFile = pathToTypes[path]!!
      if (otherTypesInFile.any { it in referencedTypes }) continue // Still needed.
      obsoleteImports.add(path)
    }

    // Rewrite the imports.
    val newImports = imports.toMutableList()
    val newPublicImports = publicImports.toMutableList()
    for (requiredType in definitelyNeed) {
      val path = typeToPath[requiredType] ?: continue // Built-in type like string or int32.
      if (path == location.path) continue // Don't import self!
      if (path in newImports || path in publicImports) continue // Already imported.
      newImports += path
    }
    newImports.removeAll(obsoleteImports)
    newPublicImports.removeAll(obsoleteImports)

    return copy(
        imports = newImports,
        publicImports = newPublicImports
    )
  }

  /** Returns the type that moved. */
  private fun getType(move: Move): Type {
    return pathToFile[move.targetPath]!!.types.first { it.type == move.type }
  }

  private fun ProtoFile.collectReferencedTypes(sink: MutableSet<ProtoType>) {
    for (type in types) {
      type.collectReferencedTypes(sink)
    }
  }

  private fun Type.collectReferencedTypes(sink: MutableSet<ProtoType>) {
    for (type in nestedTypes) {
      type.collectReferencedTypes(sink)
    }
    if (this is MessageType) {
      for (field in fieldsAndOneOfFields) {
        field.collectReferencedTypes(sink)
      }
    }
  }

  private fun Field.collectReferencedTypes(sink: MutableSet<ProtoType>) {
    sink.add(type!!)
  }

  private fun ProtoFile.collectDeclaredTypes(sink: MutableSet<ProtoType>) {
    for (type in types) {
      type.collectDeclaredTypes(sink)
    }
  }

  private fun Type.collectDeclaredTypes(sink: MutableSet<ProtoType>) {
    sink.add(type)
    for (type in nestedTypes) {
      type.collectDeclaredTypes(sink)
    }
  }

  private fun ProtoFile.emptyCopy(path: String): ProtoFile {
    return copy(
        location = location.copy(path = path),
        imports = listOf(),
        publicImports = listOf(),
        types = listOf(),
        services = listOf(),
        extendList = listOf()
    )
  }

  private fun checkForErrors() {
    require(errors.isEmpty()) { errors.joinToString(separator = "\n") }
  }
}

internal class Move(
  val type: ProtoType,
  val targetPath: String
)
