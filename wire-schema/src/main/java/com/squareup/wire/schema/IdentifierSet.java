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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A heterogeneous set of rules to include and exclude types and members. If a member is included in
 * the set, its type is implicitly also included. A type that is included without a specific member
 * implicitly includes all of that type's members, but not its nested types.
 *
 * <p>Rules in this set may be in the following forms:
 * <ul>
 *   <li>Package names, followed by {@code .*}, like {@code squareup.protos.person.*}. This matches
 *       types and services defined in the package and its descendant packages.
 *   <li>Fully qualified type and service names, like {@code squareup.protos.person.Person}.
 *   <li>Fully qualified member names, which are type names followed by a '#', followed by a member
 *       name, like {@code squareup.protos.person.Person#address}. Members may be fields, enum
 *       constants or RPCs.
 * </ul>
 *
 * <p>An identifier set populated with {@code Movie} and {@code Actor#name} contains all members of
 * {@code Movie} (such as {@code Movie#name} and {@code Movie#release_date}). It contains the type
 * {@code Actor} and one member {@code Actor#name}, but not {@code Actor#birth_date} or {@code
 * Actor#oscar_count}.
 *
 * <p>This set has <i>included identifiers</i> and <i>excluded identifiers</i>, with excludes taking
 * precedence over includes. That is, if a type {@code Movie} is in both the includes and the
 * excludes, it is not contained in the set.
 *
 * <p>If the includes set is empty, that implies that all elements should be included. Use this to
 * exclude unwanted types and members without also including everything else.
 *
 * <p>Despite the builder, instances of this class are not safe for concurrent use.
 */
public final class IdentifierSet {
  private final ImmutableSet<String> includes;
  private final ImmutableSet<String> excludes;
  private final Set<String> usedIncludes = new LinkedHashSet<>();
  private final Set<String> usedExcludes = new LinkedHashSet<>();

  private IdentifierSet(Builder builder) {
    this.includes = builder.includes.build();
    this.excludes = builder.excludes.build();
  }

  public boolean isEmpty() {
    return includes.isEmpty() && excludes.isEmpty();
  }

  /** Returns true if {@code type} is a root. */
  public boolean includes(ProtoType type) {
    return includes(type.toString());
  }

  /** Returns true if {@code protoMember} is a root. */
  public boolean includes(ProtoMember protoMember) {
    return includes(protoMember.toString());
  }

  /**
   * Returns true if {@code identifier} or any of its enclosing identifiers is included. If any
   * enclosing identifier is excluded, that takes precedence and this returns false.
   */
  private boolean includes(String identifier) {
    if (includes.isEmpty()) return !exclude(identifier);

    String includeMatch = null;
    String excludeMatch = null;
    for (String rule = identifier; rule != null; rule = enclosing(rule)) {
      if (excludes.contains(rule)) {
        excludeMatch = rule;
      }
      if (includes.contains(rule)) {
        includeMatch = rule;
      }
    }
    if (excludeMatch != null) {
      usedExcludes.add(excludeMatch);
      return false;
    }
    if (includeMatch != null) {
      usedIncludes.add(includeMatch);
      return true;
    }
    return false;
  }

  /**
   * Returns true if {@code type} should be excluded, even if it is a transitive dependency of a
   * root. In that case, the referring member is also excluded.
   */
  public boolean excludes(ProtoType type) {
    return exclude(type.toString());
  }

  /** Returns true if {@code protoMember} should be excluded. */
  public boolean excludes(ProtoMember protoMember) {
    return exclude(protoMember.toString());
  }

  /** Returns true if {@code identifier} or any of its enclosing identifiers is excluded. */
  private boolean exclude(String identifier) {
    String excludeMatch = null;
    for (String rule = identifier; rule != null; rule = enclosing(rule)) {
      if (excludes.contains(rule)) {
        excludeMatch = rule;
      }
    }
    if (excludeMatch != null) {
      usedExcludes.add(excludeMatch);
      return true;
    }
    return false;
  }

  /**
   * Returns the identifier or wildcard that encloses {@code identifier}, or null if it is not
   * enclosed.
   *
   * <ul>
   *   <li>If {@code identifier} is a member this returns the enclosing type.
   *   <li>If it is a type it returns the enclosing package with a wildcard, like {@code
   *       squareup.dinosaurs.*}.
   *   <li>If it is a package with a wildcard, it returns the parent package with a wildcard, like
   *       {@code squareup.*}.
   * </ul>
   */
  static String enclosing(String identifier) {
    int hash = identifier.lastIndexOf('#');
    if (hash != -1) return identifier.substring(0, hash);

    int from = identifier.endsWith(".*") ? identifier.length() - 3 : identifier.length() - 1;
    int dot = identifier.lastIndexOf('.', from);
    if (dot != -1) return identifier.substring(0, dot) + ".*";

    return null;
  }

  public Set<String> unusedIncludes() {
    return Sets.difference(includes, usedIncludes);
  }

  public Set<String> unusedExcludes() {
    return Sets.difference(excludes, usedExcludes);
  }

  public static final class Builder {
    final ImmutableSet.Builder<String> includes = ImmutableSet.builder();
    final ImmutableSet.Builder<String> excludes = ImmutableSet.builder();

    public Builder include(String identifier) {
      if (identifier == null) throw new NullPointerException("identifier == null");
      includes.add(identifier);
      return this;
    }

    public Builder exclude(String identifier) {
      if (identifier == null) throw new NullPointerException("identifier == null");
      excludes.add(identifier);
      return this;
    }

    public IdentifierSet build() {
      return new IdentifierSet(this);
    }
  }
}
