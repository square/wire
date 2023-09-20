/*
 * Copyright (C) 2020 Square, Inc.
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
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.test.fail

class EmittingRulesTest {
  @Test
  fun enclosing() {
    assertThat(EmittingRules.enclosing("a.b.Outer")).isEqualTo("a.b.*")
    assertThat(EmittingRules.enclosing("a.b.*")).isEqualTo("a.*")
    assertThat(EmittingRules.enclosing("a.*")).isEqualTo("*")
    assertThat(EmittingRules.enclosing("*")).isNull()
  }

  @Test
  fun enclosingOnNestedClass() {
    assertThat(EmittingRules.enclosing("a.b.Outer.Inner")).isEqualTo("a.b.Outer.*")
    assertThat(EmittingRules.enclosing("a.b.Outer.*")).isEqualTo("a.b.*")
  }

  @Test
  fun empty() {
    val rules = EmittingRules.Builder().build()
    assertThat(rules.includes(ProtoType.get("a.b.Message"))).isTrue()
  }

  /** Note that including a type includes nested members, but not nested types. */
  @Test
  fun includeType() {
    val rules = EmittingRules.Builder()
      .include("a.b.Message")
      .build()
    assertThat(rules.includes(ProtoType.get("a.b.Message"))).isTrue()
    assertThat(rules.includes(ProtoType.get("a.b.Message.Nested"))).isFalse()
    assertThat(rules.includes(ProtoType.get("a.b.Another"))).isFalse()
  }

  @Test
  fun includePackage() {
    val rules = EmittingRules.Builder()
      .include("a.b.*")
      .build()
    assertThat(rules.includes(ProtoType.get("a.b.Message"))).isTrue()
    assertThat(rules.includes(ProtoType.get("a.b.c.Message"))).isTrue()
    assertThat(rules.includes(ProtoType.get("a.c.Another"))).isFalse()
  }

  @Test
  fun includeAll() {
    val rules = EmittingRules.Builder()
      .include("*")
      .build()
    assertThat(rules.includes(ProtoType.get("a.b.Message"))).isTrue()
  }

  @Test
  fun excludeType() {
    val rules = EmittingRules.Builder()
      .exclude("a.b.Message")
      .build()
    assertThat(rules.includes(ProtoType.get("a.b.Message"))).isFalse()
    assertThat(rules.includes(ProtoType.get("a.b.Another"))).isTrue()
  }

  @Test
  fun excludePackage() {
    val rules = EmittingRules.Builder()
      .exclude("a.b.*")
      .build()
    assertThat(rules.includes(ProtoType.get("a.b.Message"))).isFalse()
    assertThat(rules.includes(ProtoType.get("a.b.c.Message"))).isFalse()
    assertThat(rules.includes(ProtoType.get("a.c.Another"))).isTrue()
  }

  @Test
  fun mostPreciseIncludeTakesPrecedenceOverExclude() {
    val rules = EmittingRules.Builder()
      .exclude("a.b.*")
      .include("a.b.Message")
      .build()
    assertThat(rules.includes(ProtoType.get("a.b.Message"))).isTrue()
    assertThat(rules.includes(ProtoType.get("a.b.Another"))).isFalse()
    assertThat(rules.includes(ProtoType.get("a.c.YetAnother"))).isFalse()
  }

  @Test
  fun mostPreciseExcludeTakesPrecedenceOverInclude() {
    val rules = EmittingRules.Builder()
      .exclude("a.b.Message")
      .include("a.b.*")
      .build()
    assertThat(rules.includes(ProtoType.get("a.b.Message"))).isFalse()
    assertThat(rules.includes(ProtoType.get("a.b.Another"))).isTrue()
    assertThat(rules.includes(ProtoType.get("a.c.YetAnother"))).isFalse()
  }

  @Test
  fun trackingUnusedIncludes() {
    val rules = EmittingRules.Builder()
      .include("a.*")
      .include("b.IncludedType")
      .build()
    assertThat(rules.unusedIncludes()).containsExactly("a.*", "b.IncludedType")

    rules.includes(ProtoType.get("a.*"))
    assertThat(rules.unusedIncludes()).containsExactly("b.IncludedType")

    rules.includes(ProtoType.get("b.IncludedType"))
    assertThat(rules.unusedIncludes()).isEmpty()
  }

  @Test
  fun trackingUnusedExcludes() {
    val rules = EmittingRules.Builder()
      .exclude("a.*")
      .exclude("b.ExcludedType")
      .build()
    assertThat(rules.unusedExcludes()).containsExactly("a.*", "b.ExcludedType")

    rules.includes(ProtoType.get("a.*"))
    assertThat(rules.unusedExcludes()).containsExactly("b.ExcludedType")

    rules.includes(ProtoType.get("b.ExcludedType"))
    assertThat(rules.unusedExcludes()).isEmpty()
  }

  @Test
  fun trackingUnusedIncludesPrecedence() {
    val rules = EmittingRules.Builder()
      .include("a.*")
      .include("a.IncludedType")
      .build()
    rules.includes(ProtoType.get("a.IncludedType.NestedType"))
    assertThat(rules.unusedIncludes()).containsExactly("a.IncludedType")
  }

  @Test
  fun trackingUnusedExcludesPrecedence() {
    val rules = EmittingRules.Builder()
      .exclude("a.*")
      .exclude("a.IncludedType")
      .build()
    rules.includes(ProtoType.get("a.IncludedType.NestedType"))
    assertThat(rules.unusedExcludes()).containsExactly("a.IncludedType")
  }

  @Test
  fun crashForConflictingRules() {
    try {
      EmittingRules.Builder()
        .include("a.*")
        .exclude("a.*")
        .build()
      fail()
    } catch (exception: IllegalStateException) {
      assertThat(exception).hasMessage("same rule(s) defined in both includes and excludes: a.*")
    }
  }
}
