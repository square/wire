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

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasMessage
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.test.fail

class PruningRulesTest {
  @Test
  fun enclosing() {
    assertThat(PruningRules.enclosing("a.b.Outer#c.d.member")).isEqualTo("a.b.Outer#c.d.*")
    assertThat(PruningRules.enclosing("a.b.Outer#c.d.*")).isEqualTo("a.b.Outer#c.*")
    assertThat(PruningRules.enclosing("a.b.Outer#c.*")).isEqualTo("a.b.Outer#*")
    assertThat(PruningRules.enclosing("a.b.Outer#*")).isEqualTo("a.b.Outer")
    assertThat(PruningRules.enclosing("a.b.Outer")).isEqualTo("a.b.*")
    assertThat(PruningRules.enclosing("a.b.*")).isEqualTo("a.*")
    assertThat(PruningRules.enclosing("a.*")).isEqualTo("*")
    assertThat(PruningRules.enclosing("*")).isNull()
  }

  @Test
  fun enclosingOnNestedClass() {
    assertThat(PruningRules.enclosing("a.b.Outer.Inner#c.member")).isEqualTo("a.b.Outer.Inner#c.*")
    assertThat(PruningRules.enclosing("a.b.Outer.Inner#c.*")).isEqualTo("a.b.Outer.Inner#*")
    assertThat(PruningRules.enclosing("a.b.Outer.Inner#*")).isEqualTo("a.b.Outer.Inner")
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
  fun includeMemberWithQualifiedName() {
    val set = PruningRules.Builder()
      .addRoot("a.b.Message#c.d.member")
      .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Message.Nested")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Message#c.d.member")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#c.d.other")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Message#c.e.member")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Message#other")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.UNSPECIFIED)
  }

  @Test
  fun includeMemberWithQualifiedNameAndWildstar() {
    val set = PruningRules.Builder()
      .addRoot("a.b.Message#c.d.*")
      .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Message.Nested")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Message#c.d.member")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#c.d.other")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#c.e.member")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Message#other")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.UNSPECIFIED)
  }

  @Test
  fun includeMemberWithWildstar() {
    val set = PruningRules.Builder()
      .addRoot("a.b.Message#*")
      .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Message.Nested")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Message#c.d.member")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#c.d.other")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#c.e.member")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#other")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.UNSPECIFIED)
  }

  @Test
  fun excludeMemberWithQualifiedName() {
    val set = PruningRules.Builder()
      .prune("a.b.Message#c.d.member")
      .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message.Nested")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#c.d.member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#c.d.other")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#c.e.member")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#other")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.INCLUDED)
  }

  @Test
  fun excludeMemberWithQualifiedNameAndWildstar() {
    val set = PruningRules.Builder()
      .prune("a.b.Message#c.d.*")
      .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message.Nested")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#c.d.member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#c.d.other")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#c.e.member")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#other")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.INCLUDED)
  }

  @Test
  fun excludeMemberWithWildstar() {
    val set = PruningRules.Builder()
      .prune("a.b.Message#*")
      .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message.Nested")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#c.d.member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#c.d.other")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#c.e.member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#other")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.INCLUDED)
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
  fun excludeTypeWithQualifiedNameDoesNotTakePrecedenceOverIncludeMember() {
    val set = PruningRules.Builder()
      .prune("a.b.Message")
      .addRoot("a.b.Message#c.member")
      .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#c.member")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#c.other")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#d.other")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another#c.member")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.UNSPECIFIED)
  }

  @Test
  fun excludeTypeWithQualifiedNameWildcardDoesNotTakePrecedenceOverIncludeMember() {
    val set = PruningRules.Builder()
      .prune("a.b.Message")
      .addRoot("a.b.Message#c.*")
      .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#c.member")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#c.other")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#d.other")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another#c.member")).isEqualTo(Policy.UNSPECIFIED)
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
  fun excludeMemberTakesPrecedenceOverIncludeTypeWithQualifiedName() {
    val set = PruningRules.Builder()
      .prune("a.b.Message#c.member")
      .addRoot("a.b.Message")
      .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#c.member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#c.other")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#d.other")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another#c.member")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another#member")).isEqualTo(Policy.UNSPECIFIED)
  }

  @Test
  fun excludeMemberTakesPrecedenceOverIncludeTypeWithQualifiedNameWildcard() {
    val set = PruningRules.Builder()
      .prune("a.b.Message#c.*")
      .addRoot("a.b.Message")
      .build()
    assertThat(policy(set, "a.b.Message")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Message#c.member")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#c.other")).isEqualTo(Policy.EXCLUDED)
    assertThat(policy(set, "a.b.Message#d.other")).isEqualTo(Policy.INCLUDED)
    assertThat(policy(set, "a.b.Another")).isEqualTo(Policy.UNSPECIFIED)
    assertThat(policy(set, "a.b.Another#c.member")).isEqualTo(Policy.UNSPECIFIED)
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
      .addRoot("d.IncludedMember#e.member")
      .addRoot("f.IncludedMember#g.*")
      .build()
    assertThat(set.unusedRoots()).containsExactly(
      "a.*",
      "b.IncludedType",
      "c.IncludedMember#member",
      "d.IncludedMember#e.member",
      "f.IncludedMember#g.*",
    )

    set.isRoot(ProtoType.get("a.*"))
    assertThat(set.unusedRoots())
      .containsExactly("b.IncludedType", "c.IncludedMember#member", "d.IncludedMember#e.member", "f.IncludedMember#g.*")

    set.isRoot(ProtoType.get("b.IncludedType"))
    assertThat(set.unusedRoots())
      .containsExactly("c.IncludedMember#member", "d.IncludedMember#e.member", "f.IncludedMember#g.*")

    set.isRoot(ProtoMember.get("c.IncludedMember#member"))
    assertThat(set.unusedRoots())
      .containsExactly("d.IncludedMember#e.member", "f.IncludedMember#g.*")

    set.isRoot(ProtoMember.get("d.IncludedMember#e.member"))
    assertThat(set.unusedRoots()).containsExactly("f.IncludedMember#g.*")

    set.isRoot(ProtoMember.get("f.IncludedMember#g.*"))
    assertThat(set.unusedRoots()).isEmpty()
  }

  @Test
  fun trackingUnusedExcludes() {
    val set = PruningRules.Builder()
      .prune("a.*")
      .prune("b.ExcludedType")
      .prune("c.ExcludedMember#member")
      .prune("d.ExcludedMember#e.member")
      .prune("f.ExcludedMember#g.*")
      .build()
    assertThat(set.unusedPrunes()).containsExactly(
      "a.*",
      "b.ExcludedType",
      "c.ExcludedMember#member",
      "d.ExcludedMember#e.member",
      "f.ExcludedMember#g.*",
    )

    set.isRoot(ProtoType.get("a.*"))
    assertThat(set.unusedPrunes()).containsExactly("b.ExcludedType", "c.ExcludedMember#member", "d.ExcludedMember#e.member", "f.ExcludedMember#g.*")

    set.isRoot(ProtoType.get("b.ExcludedType"))
    assertThat(set.unusedPrunes()).containsExactly("c.ExcludedMember#member", "d.ExcludedMember#e.member", "f.ExcludedMember#g.*")

    set.isRoot(ProtoMember.get("c.ExcludedMember#member"))
    assertThat(set.unusedPrunes()).containsExactly("d.ExcludedMember#e.member", "f.ExcludedMember#g.*")

    set.isRoot(ProtoMember.get("d.ExcludedMember#e.member"))
    assertThat(set.unusedPrunes()).containsExactly("f.ExcludedMember#g.*")

    set.isRoot(ProtoMember.get("f.ExcludedMember#g.*"))
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

  @Test
  fun onlyCannotBeSetAlongSideSince() {
    try {
      PruningRules.Builder()
        .only("3")
        .since("3")
        .build()
      fail()
    } catch (exception: IllegalStateException) {
      assertThat(exception).hasMessage("only cannot be set along side since and until")
    }
  }

  @Test
  fun onlyCannotBeSetAlongSideUntil() {
    try {
      PruningRules.Builder()
        .only("3")
        .until("3.1")
        .build()
      fail()
    } catch (exception: IllegalStateException) {
      assertThat(exception).hasMessage("only cannot be set along side since and until")
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
    EXCLUDED,
  }
}
