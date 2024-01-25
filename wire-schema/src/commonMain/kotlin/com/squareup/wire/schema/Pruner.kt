/*
 * Copyright (C) 2015 Square, Inc.
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

import com.squareup.wire.schema.ProtoMember.Companion.get
import com.squareup.wire.schema.internal.mutableQueueOf

/**
 * Creates a new schema that contains only the types selected by the pruning rules, including their
 * transitive dependencies.
 */
class Pruner(
  private val schema: Schema,
  private val pruningRules: PruningRules,
) {
  private val marks = MarkSet(pruningRules)

  /**
   * [types][ProtoType] and [members][ProtoMember] whose immediate dependencies have not
   * yet been visited.
   */
  private val queue = mutableQueueOf<Any>()

  fun prune(): Schema {
    markRoots()
    markReachable()

    val retained = retainImports(retainAll(schema, marks))

    return Schema(retained)
  }

  private fun retainAll(
    schema: Schema,
    marks: MarkSet,
  ): List<ProtoFile> {
    return schema.protoFiles.map { protoFile ->
      protoFile.retainAll(schema, marks)
    }
  }

  private fun retainImports(protoFiles: List<ProtoFile>): List<ProtoFile> {
    val schema = Schema(protoFiles)
    return protoFiles.map { protoFile ->
      protoFile.retainImports(schema)
    }
  }

  private fun markRoots() {
    for (protoFile in schema.protoFiles) {
      markRoots(protoFile)
    }
  }

  private fun markRoots(protoFile: ProtoFile) {
    for (type in protoFile.types) {
      markRootsIncludingNested(type)
    }
    for (service in protoFile.services) {
      markRoots(service.type)
    }
  }

  private fun markRootsIncludingNested(type: Type) {
    markRoots(type.type)

    for (nested in type.nestedTypes) {
      markRootsIncludingNested(nested)
    }
  }

  private fun markRoots(protoType: ProtoType) {
    if (pruningRules.isRoot(protoType)) {
      marks.root(protoType)
      queue.add(protoType)
      return
    }

    // The top-level type isn't a root, search for root members inside.
    for (reachable in reachableObjects(protoType)) {
      if (reachable !is ProtoMember) continue
      if (!isRetainedVersion(reachable)) continue
      if (pruningRules.isRoot(reachable)) {
        marks.root(reachable)
        marks.mark(reachable.type) // Consider this type as visited.
        queue.add(reachable)
      }
    }
  }

  /** Returns true if this member survives `since` and `until` pruning. */
  private fun isRetainedVersion(protoMember: ProtoMember): Boolean {
    val member = protoMember.member
    return when (val type = schema.getType(protoMember.type)) {
      is MessageType -> {
        val field = type.field(member) ?: type.extensionField(member)
        if (field != null) {
          pruningRules.isFieldRetainedVersion(field.options)
        } else {
          pruningRules.isFieldRetainedVersion(type.oneOf(member)!!.options)
        }
      }
      is EnumType -> {
        val enumConstant = type.constant(member)!!
        pruningRules.isEnumConstantRetainedVersion(enumConstant.options)
      }
      else -> true
    }
  }

  /**
   * Mark everything transitively reachable from the queue, adding to the queue whenever a reachable
   * object brings along more reachable objects.
   */
  private fun markReachable() {
    while (true) {
      val root: Any = queue.poll() ?: break
      val reachableMembers: List<Any?> = reachableObjects(root)

      for (reachable in reachableMembers) {
        when (reachable) {
          is ProtoType -> {
            if (root is ProtoMember) {
              if (marks.mark(reachable, root)) {
                queue.add(reachable)
              }
            } else {
              if (marks.mark(reachable)) {
                queue.add(reachable)
              }
            }
          }

          is ProtoMember -> {
            if (isRetainedVersion(reachable) && marks.mark(reachable)) {
              queue.add(reachable)
            }
          }

          null -> {
            // Skip nulls.
            // TODO(jwilson): create a dedicated UNLINKED type as a placeholder.
          }

          else -> {
            throw IllegalStateException("unexpected object: $reachable")
          }
        }
      }
    }
  }

  /**
   * Returns everything reachable from `root` when traversing the graph. The returned list
   * contains instances of type [ProtoMember] and [ProtoType].
   *
   * @param root either a [ProtoMember] or [ProtoType].
   */
  private fun reachableObjects(root: Any): List<Any?> {
    val result = mutableListOf<Any?>()
    val options: Options
    var fileOptions: Options? = null

    when (root) {
      is ProtoMember -> {
        val member = root.member
        val type = schema.getType(root.type)
        val service = schema.getService(root.type)

        if (type is MessageType) {
          val field = type.field(member) ?: type.extensionField(member)
          if (field != null) {
            result.add(field.type)
            options = field.options
          } else {
            val oneOf = checkNotNull(type.oneOf(member)) { "unexpected member: $member" }
            options = oneOf.options
          }
        } else if (type is EnumType) {
          val constant = type.constant(member)
            ?: throw IllegalStateException("unexpected member: $member")
          options = constant.options
        } else if (service != null) {
          val rpc = service.rpc(member) ?: throw IllegalStateException("unexpected rpc: $member")
          result.add(rpc.requestType)
          result.add(rpc.responseType)
          options = rpc.options
        } else {
          throw IllegalStateException("unexpected member: $member")
        }
      }

      is ProtoType -> {
        if (root.isMap) {
          result.add(root.keyType)
          result.add(root.valueType)
          return result
        }

        if (root.isScalar) {
          return result // Skip scalar types.
        }

        val type = schema.getType(root)
        val service = schema.getService(root)
        fileOptions = schema.protoFile(root)!!.options

        if (type is MessageType) {
          options = type.options
          for (field in type.declaredFields) {
            result.add(get(root, field.name))
          }
          for (field in type.extensionFields) {
            result.add(get(root, field.qualifiedName))
          }
          for (oneOf in type.oneOfs) {
            result.add(get(root, oneOf.name))
            for (field in oneOf.fields) {
              result.add(get(root, field.name))
            }
          }
        } else if (type is EnumType) {
          options = type.options
          for (constant in type.constants) {
            result.add(get(type.type, constant.name))
          }
        } else if (type is EnclosingType) {
          options = type.options
        } else if (service != null) {
          options = service.options
          for (rpc in service.rpcs) {
            result.add(get(service.type, rpc.name))
          }
        } else {
          throw IllegalStateException("unexpected type: $root")
        }
      }
      else -> {
        throw IllegalStateException("unexpected root: $root")
      }
    }

    addOptions(options.fields(pruningRules).values(), result)
    if (fileOptions != null) {
      addOptions(fileOptions.fields(pruningRules).values(), result)
    }

    return result
  }

  private fun addOptions(options: Collection<ProtoMember>, result: MutableList<Any?>) {
    for (member in options) {
      // If it's an extension, don't consider the entire enclosing type to be reachable.
      if (!schema.isExtensionField(member)) {
        result.add(member.type)
      }
      result.add(member)
    }
  }
}
