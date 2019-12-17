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
package com.squareup.wire.schema

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PruningRulesTest {
  @Test
  fun enclosing() {
    assertThat(PruningRules.enclosing("a.b.Outer#member")).isEqualTo("a.b.Outer")
    assertThat(PruningRules.enclosing("a.b.Outer")).isEqualTo("a.b.*")
    assertThat(PruningRules.enclosing("a.b.*")).isEqualTo("a.*")
    assertThat(PruningRules.enclosing("a.*")).isEqualTo("*")
    assertThat(PruningRules.enclosing("*")).isNull()
  }

  @Test
  fun enclosingOnNestedClass() {
    assertThat(PruningRules.enclosing("a.b.Outer.Inner#member")).isEqualTo("a.b.Outer.Inner")
    assertThat(PruningRules.enclosing("a.b.Outer.Inner")).isEqualTo("a.b.Outer.*")
    assertThat(PruningRules.enclosing("a.b.Outer.*")).isEqualTo("a.b.*")
  }

  @Test
  fun empty() {
    val set = PruningRules.Builder().build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.INCLUDED)
  }

  /** Note that including a type includes nested members, but not nested types.  */
  @Test
  fun includeType() {
    val set = PruningRules.Builder()
        .include("a.b.Message")
        .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message.Nested")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.UNSPECIFIED)
  }

  @Test
  fun includeMember() {
    val set = PruningRules.Builder()
        .include("a.b.Message#member")
        .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Message.Nested")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#other")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.UNSPECIFIED)
  }

  @Test
  fun includePackage() {
    val set = PruningRules.Builder()
        .include("a.b.*")
        .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.c.Message")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.c.Message#member")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.c.Another")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.c.Another#member")).isEqualTo(Policy.UNSPECIFIED)
  }

  @Test
  fun includeAll() {
    val set = PruningRules.Builder()
        .include("*")
        .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.INCLUDED)
  }

  @Test
  fun excludeType() {
    val set = PruningRules.Builder()
        .exclude("a.b.Message")
        .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.INCLUDED)
  }

  @Test
  fun excludeMember() {
    val set = PruningRules.Builder()
        .exclude("a.b.Message#member")
        .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#other")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.INCLUDED)
  }

  @Test
  fun excludePackage() {
    val set = PruningRules.Builder()
        .exclude("a.b.*")
        .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.c.Message")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.c.Message#member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.c.Another")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.c.Another#member")).isEqualTo(Policy.INCLUDED)
  }

  @Test
  fun excludePackageTakesPrecedenceOverIncludeType() {
    val set = PruningRules.Builder()
        .exclude("a.b.*")
        .include("a.b.Message")
        .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.c.YetAnother")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.c.YetAnother#member")).isEqualTo(Policy.UNSPECIFIED)
  }

  @Test
  fun excludeTypeTakesPrecedenceOverIncludeMember() {
    val set = PruningRules.Builder()
        .exclude("a.b.Message")
        .include("a.b.Message#member")
        .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#other")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.UNSPECIFIED)
  }

  @Test
  fun excludeMemberTakesPrecedenceOverIncludeType() {
    val set = PruningRules.Builder()
        .exclude("a.b.Message#member")
        .include("a.b.Message")
        .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#other")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.UNSPECIFIED)
  }

  @Test
  fun trackingUnusedIncludes() {
    val set = PruningRules.Builder()
        .include("a.*")
        .include("b.IncludedType")
        .include("c.IncludedMember#member")
        .build()
    assertThat(set.unusedIncludes()).containsExactly("a.*", "b.IncludedType",
        "c.IncludedMember#member")

    set.includes(ProtoType.get("a.*"))
    assertThat(set.unusedIncludes()).containsExactly("b.IncludedType", "c.IncludedMember#member")

    set.includes(ProtoType.get("b.IncludedType"))
    assertThat(set.unusedIncludes()).containsExactly("c.IncludedMember#member")

    set.includes(ProtoMember.get("c.IncludedMember#member"))
    assertThat(set.unusedIncludes()).isEmpty()
  }

  @Test
  fun trackingUnusedExcludes() {
    val set = PruningRules.Builder()
        .exclude("a.*")
        .exclude("b.ExcludedType")
        .exclude("c.ExcludedMember#member")
        .build()
    assertThat(set.unusedExcludes()).containsExactly("a.*", "b.ExcludedType",
        "c.ExcludedMember#member")

    set.includes(ProtoType.get("a.*"))
    assertThat(set.unusedExcludes()).containsExactly("b.ExcludedType", "c.ExcludedMember#member")

    set.includes(ProtoType.get("b.ExcludedType"))
    assertThat(set.unusedExcludes()).containsExactly("c.ExcludedMember#member")

    set.includes(ProtoMember.get("c.ExcludedMember#member"))
    assertThat(set.unusedExcludes()).isEmpty()
  }

  @Test
  fun trackingUnusedIncludesPrecedence() {
    val set = PruningRules.Builder()
        .include("a.*")
        .include("a.IncludedType")
        .build()
    set.includes(ProtoMember.get("a.IncludedType#member"))
    assertThat(set.unusedIncludes()).containsExactly("a.IncludedType")
  }

  @Test
  fun trackingUnusedExcludesPrecedence() {
    val set = PruningRules.Builder()
        .exclude("a.*")
        .exclude("a.IncludedType")
        .build()
    set.includes(ProtoMember.get("a.IncludedType#member"))
    assertThat(set.unusedExcludes()).containsExactly("a.IncludedType")
  }

  @Test
  fun trackingUnusedPrecedence() {
    val set = PruningRules.Builder()
        .include("a.*")
        .exclude("a.*")
        .build()
    set.includes(ProtoType.get("a.Message"))
    assertThat(set.unusedExcludes()).isEmpty()
    assertThat(set.unusedIncludes()).containsExactly("a.*")
  }

  private fun policy(set: PruningRules, identifier: String): Policy {
    return if (identifier.contains("#")) {
      val protoMember = ProtoMember.get(identifier)
      if (set.includes(protoMember)) return Policy.INCLUDED
      if (set.excludes(protoMember)) Policy.EXCLUDED else Policy.UNSPECIFIED
    } else {
      val protoType = ProtoType.get(identifier)
      if (set.includes(protoType)) return Policy.INCLUDED
      if (set.excludes(protoType)) Policy.EXCLUDED else Policy.UNSPECIFIED
    }
  }

  internal enum class Policy {
    INCLUDED,
    UNSPECIFIED,
    EXCLUDED
  }
}
