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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class IdentifierSetTest {
  @Test public void enclosing() throws Exception {
    assertThat(IdentifierSet.enclosing("a.b.Outer#member")).isEqualTo("a.b.Outer");
    assertThat(IdentifierSet.enclosing("a.b.Outer")).isEqualTo("a.b.*");
    assertThat(IdentifierSet.enclosing("a.b.*")).isEqualTo("a.*");
    assertThat(IdentifierSet.enclosing("a.*")).isNull();
  }

  @Test public void enclosingOnNestedClass() throws Exception {
    assertThat(IdentifierSet.enclosing("a.b.Outer.Inner#member")).isEqualTo("a.b.Outer.Inner");
    assertThat(IdentifierSet.enclosing("a.b.Outer.Inner")).isEqualTo("a.b.Outer.*");
    assertThat(IdentifierSet.enclosing("a.b.Outer.*")).isEqualTo("a.b.*");
  }

  @Test public void empty() throws Exception {
    IdentifierSet set = new IdentifierSet.Builder()
        .build();
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED);
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.INCLUDED);
  }

  /** Note that including a type includes nested members, but not nested types. */
  @Test public void includeType() throws Exception {
    IdentifierSet set = new IdentifierSet.Builder()
        .include("a.b.Message")
        .build();
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED);
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.INCLUDED);
    assertThat(policy(set, "a.b.Message.Nested")).isEqualTo(Policy.UNSPECIFIED);
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.UNSPECIFIED);
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.UNSPECIFIED);
  }

  @Test public void includeMember() throws Exception {
    IdentifierSet set = new IdentifierSet.Builder()
        .include("a.b.Message#member")
        .build();
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.UNSPECIFIED);
    assertThat(policy(set, "a.b.Message.Nested")).isEqualTo(Policy.UNSPECIFIED);
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.INCLUDED);
    assertThat(policy(set, "a.b.Message#other")).isEqualTo(Policy.UNSPECIFIED);
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.UNSPECIFIED);
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.UNSPECIFIED);
  }

  @Test public void includePackage() throws Exception {
    IdentifierSet set = new IdentifierSet.Builder()
        .include("a.b.*")
        .build();
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED);
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.INCLUDED);
    assertThat(policy(set, "a.b.c.Message")).isEqualTo(Policy.INCLUDED);
    assertThat(policy(set, "a.b.c.Message#member")).isEqualTo(Policy.INCLUDED);
    assertThat(policy(set, "a.c.Another")).isEqualTo(Policy.UNSPECIFIED);
    assertThat(policy(set, "a.c.Another#member")).isEqualTo(Policy.UNSPECIFIED);
  }

  @Test public void excludeType() throws Exception {
    IdentifierSet set = new IdentifierSet.Builder()
        .exclude("a.b.Message")
        .build();
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.EXCLUDED);
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.EXCLUDED);
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.INCLUDED);
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.INCLUDED);
  }

  @Test public void excludeMember() throws Exception {
    IdentifierSet set = new IdentifierSet.Builder()
        .exclude("a.b.Message#member")
        .build();
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED);
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.EXCLUDED);
    assertThat(policy(set, "a.b.Message#other")).isEqualTo(Policy.INCLUDED);
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.INCLUDED);
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.INCLUDED);
  }

  @Test public void excludePackage() throws Exception {
    IdentifierSet set = new IdentifierSet.Builder()
        .exclude("a.b.*")
        .build();
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.EXCLUDED);
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.EXCLUDED);
    assertThat(policy(set, "a.b.c.Message")).isEqualTo(Policy.EXCLUDED);
    assertThat(policy(set, "a.b.c.Message#member")).isEqualTo(Policy.EXCLUDED);
    assertThat(policy(set, "a.c.Another")).isEqualTo(Policy.INCLUDED);
    assertThat(policy(set, "a.c.Another#member")).isEqualTo(Policy.INCLUDED);
  }

  @Test public void excludePackageTakesPrecedenceOverIncludeType() throws Exception {
    IdentifierSet set = new IdentifierSet.Builder()
        .exclude("a.b.*")
        .include("a.b.Message")
        .build();
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.EXCLUDED);
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.EXCLUDED);
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.EXCLUDED);
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.EXCLUDED);
    assertThat(policy(set, "a.c.YetAnother")).isEqualTo(Policy.UNSPECIFIED);
    assertThat(policy(set, "a.c.YetAnother#member")).isEqualTo(Policy.UNSPECIFIED);
  }

  @Test public void excludeTypeTakesPrecedenceOverIncludeMember() throws Exception {
    IdentifierSet set = new IdentifierSet.Builder()
        .exclude("a.b.Message")
        .include("a.b.Message#member")
        .build();
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.EXCLUDED);
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.EXCLUDED);
    assertThat(policy(set, "a.b.Message#other")).isEqualTo(Policy.EXCLUDED);
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.UNSPECIFIED);
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.UNSPECIFIED);
  }

  @Test public void excludeMemberTakesPrecedenceOverIncludeType() throws Exception {
    IdentifierSet set = new IdentifierSet.Builder()
        .exclude("a.b.Message#member")
        .include("a.b.Message")
        .build();
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED);
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.EXCLUDED);
    assertThat(policy(set, "a.b.Message#other")).isEqualTo(Policy.INCLUDED);
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.UNSPECIFIED);
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.UNSPECIFIED);
  }

  @Test public void trackingUnusedIncludes() throws Exception {
    IdentifierSet set = new IdentifierSet.Builder()
        .include("a.*")
        .include("b.IncludedType")
        .include("c.IncludedMember#member")
        .build();
    assertThat(set.unusedIncludes()).containsExactly(
        "a.*", "b.IncludedType", "c.IncludedMember#member");

    set.includes(ProtoType.get("a.*"));
    assertThat(set.unusedIncludes()).containsExactly(
        "b.IncludedType", "c.IncludedMember#member");

    set.includes(ProtoType.get("b.IncludedType"));
    assertThat(set.unusedIncludes()).containsExactly(
        "c.IncludedMember#member");

    set.includes(ProtoMember.get("c.IncludedMember#member"));
    assertThat(set.unusedIncludes()).isEmpty();
  }

  @Test public void trackingUnusedExcludes() throws Exception {
    IdentifierSet set = new IdentifierSet.Builder()
        .exclude("a.*")
        .exclude("b.ExcludedType")
        .exclude("c.ExcludedMember#member")
        .build();
    assertThat(set.unusedExcludes()).containsExactly(
        "a.*", "b.ExcludedType", "c.ExcludedMember#member");

    set.includes(ProtoType.get("a.*"));
    assertThat(set.unusedExcludes()).containsExactly(
        "b.ExcludedType", "c.ExcludedMember#member");

    set.includes(ProtoType.get("b.ExcludedType"));
    assertThat(set.unusedExcludes()).containsExactly(
        "c.ExcludedMember#member");

    set.includes(ProtoMember.get("c.ExcludedMember#member"));
    assertThat(set.unusedExcludes()).isEmpty();
  }

  @Test public void trackingUnusedIncludesPrecedence() throws Exception {
    IdentifierSet set = new IdentifierSet.Builder()
        .include("a.*")
        .include("a.IncludedType")
        .build();
    set.includes(ProtoMember.get("a.IncludedType#member"));
    assertThat(set.unusedIncludes()).containsExactly("a.IncludedType");
  }

  @Test public void trackingUnusedExcludesPrecedence() throws Exception {
    IdentifierSet set = new IdentifierSet.Builder()
        .exclude("a.*")
        .exclude("a.IncludedType")
        .build();
    set.includes(ProtoMember.get("a.IncludedType#member"));
    assertThat(set.unusedExcludes()).containsExactly("a.IncludedType");
  }

  @Test public void trackingUnusedPrecedence() throws Exception {
    IdentifierSet set = new IdentifierSet.Builder()
        .include("a.*")
        .exclude("a.*")
        .build();
    set.includes(ProtoType.get("a.Message"));
    assertThat(set.unusedExcludes()).isEmpty();
    assertThat(set.unusedIncludes()).containsExactly("a.*");
  }

  private Policy policy(IdentifierSet set, String identifier) {
    if (identifier.contains("#")) {
      ProtoMember protoMember = ProtoMember.get(identifier);
      if (set.includes(protoMember)) return Policy.INCLUDED;
      if (set.excludes(protoMember)) return Policy.EXCLUDED;
      return Policy.UNSPECIFIED;
    } else {
      ProtoType protoType = ProtoType.get(identifier);
      if (set.includes(protoType)) return Policy.INCLUDED;
      if (set.excludes(protoType)) return Policy.EXCLUDED;
      return Policy.UNSPECIFIED;
    }
  }

  enum Policy {
    INCLUDED, UNSPECIFIED, EXCLUDED
  }
}
