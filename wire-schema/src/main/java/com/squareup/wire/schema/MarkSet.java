/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.wire.schema;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A mark set is used in three phases:
 *
 * <ol>
 *   <li>Marking root types and root members. These are the identifiers specifically identified by
 *       the user in the includes set. In this phase it is an error to mark a type that is excluded,
 *       or to both a type and one of its members.
 *   <li>Marking members transitively reachable by those roots. In this phase if a member is
 *       visited, the member's enclosing type is marked instead, unless it is of a type that has a
 *       specific member already marked.
 *   <li>Retaining which members and types have been marked.
 * </ol>
 */
final class MarkSet {
  final IdentifierSet identifierSet;

  /** The types to retain. We may retain a type but not all of its members. */
  final Set<ProtoType> types = new LinkedHashSet<>();

  /** The members to retain. Any member not in here should be pruned! */
  final Multimap<ProtoType, ProtoMember> members = LinkedHashMultimap.create();

  MarkSet(IdentifierSet identifierSet) {
    this.identifierSet = identifierSet;
  }

  /**
   * Marks {@code protoMember}, throwing if it is explicitly excluded. This implicitly excludes
   * other members of the same type.
   */
  void root(ProtoMember protoMember) {
    if (protoMember == null) throw new NullPointerException("protoMember == null");
    checkArgument(!identifierSet.excludes(protoMember));
    types.add(protoMember.type());
    members.put(protoMember.type(), protoMember);
  }

  /** Marks {@code type}, throwing if it is explicitly excluded. */
  void root(ProtoType type) {
    if (type == null) throw new NullPointerException("type == null");
    checkArgument(!identifierSet.excludes(type));
    types.add(type);
  }

  /**
   * Marks a type as transitively reachable by the includes set. Returns true if the mark is new,
   * the type will be retained, and reachable objects should be traversed.
   */
  boolean mark(ProtoType type) {
    if (type == null) throw new NullPointerException("type == null");
    if (identifierSet.excludes(type)) return false;
    return types.add(type);
  }

  /**
   * Marks a member as transitively reachable by the includes set. Returns true if the mark is new,
   * the member will be retained, and reachable objects should be traversed.
   */
  boolean mark(ProtoMember protoMember) {
    if (protoMember == null) throw new NullPointerException("type == null");
    if (identifierSet.excludes(protoMember)) return false;
    types.add(protoMember.type());
    return members.put(protoMember.type(), protoMember);
  }

  /** Returns true if {@code type} is marked and should be retained. */
  boolean contains(ProtoType type) {
    if (type == null) throw new NullPointerException("type == null");
    return types.contains(type);
  }

  /** Returns true if {@code member} is marked and should be retained. */
  boolean contains(ProtoMember protoMember) {
    if (protoMember == null) throw new NullPointerException("protoMember == null");
    if (identifierSet.excludes(protoMember)) return false;
    return members.containsEntry(protoMember.type(), protoMember);
  }
}
