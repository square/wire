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

/**
 * A heterogeneous set of type names and type members. If a member is included in the set, its type
 * is implicitly also included. A type that is included without a specific member implicitly
 * includes all of that type's members.
 *
 * <p>Identifiers in this set may be in the following forms:
 * <ul>
 *   <li>Fully qualified type names, like {@code squareup.protos.person.Person}.
 *   <li>Fully qualified type names, followed by a '#', followed by a member name, like
 *       {@code squareup.protos.person.Person#address}.
 *   <li>Fully qualified service names, like {@code com.squareup.services.ExampleService}.
 *   <li>Fully qualified service names, followed by a '#', followed by an RPC name, like
 *       {@code com.squareup.services.ExampleService#SendSomeData}.
 * </ul>
 *
 * <p>An identifiers set populated with {@code Movie} and {@code Actor#name} contains all members of
 * {@code Movie} (such as {@code Movie#name} and {@code Movie#release_date}). It contains the type
 * {@code Actor} and one member {@code Actor#name}, but not {@code Actor#birth_date} or {@code
 * Actor#oscar_count}.
 *
 * <p>This set is initialized with <i>included identifiers</i> and <i>excluded identifiers</i>, with
 * excludes taking precedence over includes. That is, if a type {@code Movie} is in both the
 * includes and the excludes, it is not contained in the set. Any attempt to mark an excluded
 * identifier will return false, preventing the identifier's transitive dependencies from also being
 * marked.
 *
 * <p>If the includes set is empty, that implies that all elements should be included. Use this to
 * exclude unwanted types and members without also including everything else.
 */
public final class IdentifierSet {
  private final ImmutableSet<String> includes;
  private final ImmutableSet<String> excludes;

  private IdentifierSet(Builder builder) {
    this.includes = builder.includes.build();
    this.excludes = builder.excludes.build();
  }

  public boolean isEmpty() {
    return includes.isEmpty() && excludes.isEmpty();
  }

  public boolean excludes(ProtoType type) {
    return excludes.contains(type.toString());
  }

  public boolean excludes(ProtoMember protoMember) {
    return excludes.contains(protoMember.toString());
  }

  public boolean include(ProtoType type) {
    if (excludes.contains(type.toString())) return false;
    return includes.isEmpty() || includes.contains(type.toString());
  }

  public boolean include(ProtoMember protoMember) {
    if (excludes.contains(protoMember.toString())) return false;
    return includes.isEmpty() || includes.contains(protoMember.toString());
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
