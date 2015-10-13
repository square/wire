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

import java.util.NavigableSet;
import java.util.TreeSet;

final class MarkSet {
  final IdentifierSet identifierSet;
  final NavigableSet<String> marks;

  public MarkSet(IdentifierSet identifierSet) {
    this.identifierSet = identifierSet;
    this.marks = new TreeSet<>();

    for (String include : identifierSet.includes) {
      marks.add(include);
      int hash = include.indexOf('#');
      if (hash != -1) marks.add(include.substring(0, hash));
    }
  }

  /**
   * Marks a type as transitively reachable by the includes set. Returns true if the mark is new,
   * the type will be retained, and its own dependencies should be traversed.
   */
  boolean mark(ProtoType type) {
    if (type == null) throw new NullPointerException("type == null");
    if (identifierSet.excludes(type)) return false;
    return marks.add(type.toString());
  }

  boolean mark(ProtoMember protoMember) {
    return mark(protoMember.type(), protoMember.member());
  }

  /**
   * Marks a member as transitively reachable by the includes set. Returns true if the mark is new,
   * the member will be retained, and its own dependencies should be traversed.
   */
  boolean mark(ProtoType type, String member) {
    if (identifierSet.excludes(type, member)) return false;
    if (!contains(type)) throw new IllegalStateException();
    if (contains(type, member)) return false;
    return marks.add(type + "#" + member);
  }

  /** Returns true if {@code member} is marked and should be retained. */
  boolean contains(ProtoType type, String member) {
    if (type == null) throw new NullPointerException("type == null");
    if (member == null) throw new NullPointerException("member == null");
    if (identifierSet.excludes(type, member)) return false;
    if (identifierSet.includesEverything()) return true;

    return containsAllMembers(type) || marks.contains(type + "#" + member);
  }

  /** Returns true if {@code type} is marked and should be retained. */
  boolean contains(ProtoType type) {
    if (type == null) throw new NullPointerException("type == null");
    if (identifierSet.excludes(type)) return false;
    if (identifierSet.includesEverything()) return true;

    return marks.contains(type.toString());
  }

  /** Returns true if all members of {@code type} are marked and should be retained. */
  boolean containsAllMembers(ProtoType type) {
    return contains(type) && !containsAnyMember(type);
  }

  private boolean containsAnyMember(ProtoType typeName) {
    // If there's a member field, it will sort immediately after <Name># in the marks set.
    String prefix = typeName + "#";
    String ceiling = marks.ceiling(prefix);
    return ceiling != null && ceiling.startsWith(prefix);
  }

  public boolean contains(ProtoMember protoMember) {
    return contains(protoMember.type(), protoMember.member());
  }
}
