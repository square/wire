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

/**
 * A mark set is used in three phases:
 *
 *  1. Marking root types and root members. These are the identifiers specifically identified by
 *     the user in the includes set. In this phase it is an error to mark a type that is excluded,
 *     or to mark both a type and one of its members.
 *  2. Marking members transitively reachable by those roots. In this phase if a member is visited,
 *     the member's enclosing type is marked instead, unless it is of a type that has a specific
 *     member already marked.
 *  3. Retaining which members and types have been marked.
 */
class MarkSet(
  val pruningRules: PruningRules,
) {
  /** The types to retain. We may retain a type but not all of its members. */
  val types = mutableSetOf<ProtoType>()

  /** The members to retain. Any member not in here should be pruned! */
  val members = mutableMapOf<ProtoType, MutableSet<ProtoMember>>()

  /** The root members which are never to be pruned, including their referenced type. */
  private val rootMemberTypes = mutableMapOf<ProtoMember, ProtoType>()

  /** The members this MarkSet have seen. The values should be non-null once the marking is done. */
  private val memberTypes = mutableMapOf<ProtoMember, ProtoType>()

  /**
   * Marks `protoMember`, throwing if it is explicitly excluded. This implicitly excludes other
   * members of the same type.
   */
  fun root(protoMember: ProtoMember) {
    check(!pruningRules.prunes(protoMember))
    types.add(protoMember.type)
    rootMemberTypes[protoMember] = UNKNOWN_TYPE
    val memberSet = members.getOrPut(protoMember.type, { mutableSetOf() })
    memberSet += protoMember
  }

  /** Marks `type`, throwing if it is explicitly excluded. */
  fun root(type: ProtoType) {
    check(!pruningRules.prunes(type))
    types.add(type)
  }

  /**
   * Marks a type as transitively reachable by the includes set. Returns true if the mark is new,
   * the type will be retained, and reachable objects should be traversed.
   *
   * If there is an exclude for [type], non-root members referencing it will be pruned. The type
   * itself will also be pruned unless it is referenced by a root member.
   */
  fun mark(type: ProtoType, reference: ProtoMember): Boolean {
    memberTypes[reference] = type

    if (rootMemberTypes.containsKey(reference)) {
      rootMemberTypes[reference] = type
      // We keep.
      return types.add(type)
    }

    return mark(type)
  }

  /**
   * Marks a type as transitively reachable by the includes set. Returns true if the mark is new,
   * the type will be retained, and reachable objects should be traversed.
   */
  fun mark(type: ProtoType): Boolean {
    if (pruningRules.prunes(type)) return false
    return types.add(type)
  }

  /**
   * Marks a member as transitively reachable by the includes set. Returns true if the mark is new,
   * the member will be retained, and reachable objects should be traversed.
   */
  fun mark(protoMember: ProtoMember): Boolean {
    if (pruningRules.prunes(protoMember)) return false
    types.add(protoMember.type)
    val memberSet = members.getOrPut(protoMember.type, { mutableSetOf() })
    return memberSet.add(protoMember)
  }

  /** Returns true if `type` is marked and should be retained. */
  operator fun contains(type: ProtoType): Boolean {
    return types.contains(type)
  }

  /** Returns true if `member` is marked and should be retained. */
  operator fun contains(protoMember: ProtoMember): Boolean {
    val memberType: ProtoType? = memberTypes[protoMember]

    // We do not contain non-root members whose referenced type is excluded.
    if (!rootMemberTypes.containsKey(protoMember) &&
      memberType != null && pruningRules.prunes(memberType)
    ) {
      return false
    }

    // A member cannot be included if its referencing type is excluded unless a root member of this
    // referenced type exists.
    if (pruningRules.prunes(protoMember.type) &&
      rootMemberTypes.containsValue(protoMember.type)
    ) {
      return true
    }

    if (pruningRules.prunes(protoMember)) return false
    val memberSet = members[protoMember.type]
    return memberSet != null && memberSet.contains(protoMember)
  }

  companion object {
    private val UNKNOWN_TYPE = ProtoType.get("(unknown type)")
  }
}
