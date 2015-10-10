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

/**
 * A heterogeneous set of type names and type members. If a member is added to the set, its type is
 * implicitly also added. A type that is added without a specific member implicitly contains all
 * of that type's members.
 *
 * <p>For example, an identifiers set populated with {@code Movie} and {@code Actor#name} contains
 * all members of {@code Movie} (such as {@code Movie#name} and {@code Movie#release_date}). It
 * contains the type {@code Actor} and one member {@code Actor#name}, but not {@code
 * Actor#birth_date} or {@code Actor#oscar_count}.
 */
public final class IdentifierSet {
  final NavigableSet<String> set = new TreeSet<>();

  public boolean isEmpty() {
    return set.isEmpty();
  }

  /** Adds a type name or member name. Returns true if the set was modified. */
  public boolean add(String identifier) {
    if (identifier == null) throw new NullPointerException("identifier == null");

    int hash = identifier.indexOf('#');
    if (hash != -1) set.add(identifier.substring(0, hash));
    return set.add(identifier);
  }

  /** Adds a type. Returns true if the set was modified. */
  public boolean add(ProtoType type) {
    if (type == null) throw new NullPointerException("type == null");
    return set.add(type.toString());
  }

  /**
   * Adds a member, without constraining {@code type} to a specific subset of members unless is is
   * already. Prefer this over {@link #add} when a member is reachable implicitly, since this method
   * won't have the side effect of causing sibling members to be excluded.
   */
  public boolean addIfAbsent(ProtoType type, String member) {
    if (!contains(type)) throw new IllegalStateException();
    if (contains(type, member)) return false;
    return set.add(type + "#" + member);
  }

  public boolean contains(ProtoType type, String member) {
    if (type == null) throw new NullPointerException("type == null");
    if (member == null) throw new NullPointerException("member == null");

    return containsAllMembers(type) || set.contains(type + "#" + member);
  }

  public boolean contains(ProtoType type) {
    if (type == null) throw new NullPointerException("type == null");

    return set.contains(type.toString());
  }

  public boolean containsAllMembers(ProtoType type) {
    return contains(type) && !containsAnyMember(type);
  }

  private boolean containsAnyMember(ProtoType typeName) {
    // If there's a member field, it will sort immediately after <Name># in the marks set.
    String prefix = typeName + "#";
    String ceiling = set.ceiling(prefix);
    return ceiling != null && ceiling.startsWith(prefix);
  }
}
