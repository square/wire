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
import kotlin.test.fail

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
        .addRoot("a.b.Message")
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
        .addRoot("a.b.Message#member")
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
        .addRoot("a.b.*")
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
        .addRoot("*")
        .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.INCLUDED)
  }

  @Test
  fun excludeType() {
    val set = PruningRules.Builder()
        .prune("a.b.Message")
        .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.INCLUDED)
  }

  @Test
  fun excludeMember() {
    val set = PruningRules.Builder()
        .prune("a.b.Message#member")
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
        .prune("a.b.*")
        .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.c.Message")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.c.Message#member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.c.Another")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.c.Another#member")).isEqualTo(Policy.INCLUDED)
  }

  @Test
  fun excludePackageDoesNotTakePrecedenceOverIncludeType() {
    val set = PruningRules.Builder()
        .prune("a.b.*")
        .addRoot("a.b.Message")
        .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.c.YetAnother")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.c.YetAnother#member")).isEqualTo(Policy.UNSPECIFIED)
  }

  @Test
  fun excludeTypeDoesNotTakePrecedenceOverIncludeMember() {
    val set = PruningRules.Builder()
        .prune("a.b.Message")
        .addRoot("a.b.Message#member")
        .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#other")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.UNSPECIFIED)
  }

  @Test
  fun excludeMemberTakesPrecedenceOverIncludeType() {
    val set = PruningRules.Builder()
        .prune("a.b.Message#member")
        .addRoot("a.b.Message")
        .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#other")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.UNSPECIFIED)
  }

  @Test
  fun mostPreciseRuleTakesPrecedence() {
    val set = PruningRules.Builder()
        .prune("a.b.Message#member")
        .addRoot("a.b.Message")
        .prune("a.b.*")
        .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#other")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.EXCLUDED)
  }

  @Test
  fun trackingUnusedIncludes() {
    val set = PruningRules.Builder()
        .addRoot("a.*")
        .addRoot("b.IncludedType")
        .addRoot("c.IncludedMember#member")
        .build()
    assertThat(set.unusedRoots()).containsExactly("a.*", "b.IncludedType",
        "c.IncludedMember#member")

    set.isRoot(ProtoType.get("a.*"))
    assertThat(set.unusedRoots()).containsExactly("b.IncludedType", "c.IncludedMember#member")

    set.isRoot(ProtoType.get("b.IncludedType"))
    assertThat(set.unusedRoots()).containsExactly("c.IncludedMember#member")

    set.isRoot(ProtoMember.get("c.IncludedMember#member"))
    assertThat(set.unusedRoots()).isEmpty()
  }

  @Test
  fun trackingUnusedExcludes() {
    val set = PruningRules.Builder()
        .prune("a.*")
        .prune("b.ExcludedType")
        .prune("c.ExcludedMember#member")
        .build()
    assertThat(set.unusedPrunes()).containsExactly("a.*", "b.ExcludedType",
        "c.ExcludedMember#member")

    set.isRoot(ProtoType.get("a.*"))
    assertThat(set.unusedPrunes()).containsExactly("b.ExcludedType", "c.ExcludedMember#member")

    set.isRoot(ProtoType.get("b.ExcludedType"))
    assertThat(set.unusedPrunes()).containsExactly("c.ExcludedMember#member")

    set.isRoot(ProtoMember.get("c.ExcludedMember#member"))
    assertThat(set.unusedPrunes()).isEmpty()
  }

  @Test
  fun trackingUnusedIncludesPrecedence() {
    val set = PruningRules.Builder()
        .addRoot("a.*")
        .addRoot("a.IncludedType")
        .build()
    set.isRoot(ProtoMember.get("a.IncludedType#member"))
    assertThat(set.unusedRoots()).containsExactly("a.*")
  }

  @Test
  fun trackingUnusedExcludesPrecedence() {
    val set = PruningRules.Builder()
        .prune("a.*")
        .prune("a.IncludedType")
        .build()
    set.isRoot(ProtoMember.get("a.IncludedType#member"))
    assertThat(set.unusedPrunes()).containsExactly("a.*")
  }

  @Test
  fun crashForConflictingRules() {
    try {
      PruningRules.Builder()
          .addRoot("a.*")
          .prune("a.*")
          .build()
      fail()
    } catch (exception: IllegalStateException) {
      assertThat(exception).hasMessage("same rule(s) defined in both roots and prunes: a.*")
    }
  }

  private fun policy(set: PruningRules, identifier: String): Policy {
    return if (identifier.contains("#")) {
      val protoMember = ProtoMember.get(identifier)
      if (set.isRoot(protoMember)) return Policy.INCLUDED
      if (set.prunes(protoMember)) Policy.EXCLUDED else Policy.UNSPECIFIED
    } else {
      val protoType = ProtoType.get(identifier)
      if (set.isRoot(protoType)) return Policy.INCLUDED
      if (set.prunes(protoType)) Policy.EXCLUDED else Policy.UNSPECIFIED
    }
  }

  internal enum class Policy {
    INCLUDED,
    UNSPECIFIED,
    EXCLUDED
  }
}
