/*
 * Copyright (C) 2020 Square, Inc.
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
package com.squareup.wire.schema.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DagCheckerTest {
  @Test fun `empty graph has no cycles`() {
    val nodes = listOf<String>()
    val edges = listOf<String>()
    val cycleFinder = DagChecker(nodes) { it.targets(edges) }
    assertThat(cycleFinder.check()).isEmpty()
  }

  @Test fun `single node has no cycles`() {
    val nodes = listOf("a")
    val edges = listOf<String>()
    val cycleFinder = DagChecker(nodes) { it.targets(edges) }
    assertThat(cycleFinder.check()).isEmpty()
  }

  @Test fun `single node pointing to itself has a cycle`() {
    val nodes = listOf("a")
    val edges = listOf("aa")
    val cycleFinder = DagChecker(nodes) { it.targets(edges) }
    assertThat(cycleFinder.check()).containsExactly(listOf("a"))
  }

  @Test fun `two node dag has no cycle`() {
    val nodes = listOf("a", "b")
    val edges = listOf("ab")
    val cycleFinder = DagChecker(nodes) { it.targets(edges) }
    assertThat(cycleFinder.check()).isEmpty()
  }

  @Test fun `two node loop has a cycle`() {
    val nodes = listOf("a", "b")
    val edges = listOf("ab", "ba")
    val cycleFinder = DagChecker(nodes) { it.targets(edges) }
    assertThat(cycleFinder.check()).containsExactly(listOf("a", "b"))
  }

  @Test fun `three node dag has no cycle`() {
    val nodes = listOf("a", "b", "c")
    val edges = listOf("ab", "bc", "ac")
    val cycleFinder = DagChecker(nodes) { it.targets(edges) }
    assertThat(cycleFinder.check()).isEmpty()
  }

  @Test fun `three node cycle`() {
    val nodes = listOf("a", "b", "c")
    val edges = listOf("ab", "bc", "ca")
    val cycleFinder = DagChecker(nodes) { it.targets(edges) }
    assertThat(cycleFinder.check()).containsExactly(listOf("a", "b", "c"))
  }

  @Test fun `two node cycle with not strongly connected extra node`() {
    val nodes = listOf("a", "b", "c")
    val edges = listOf("ab", "ac", "ca")
    val cycleFinder = DagChecker(nodes) { it.targets(edges) }
    assertThat(cycleFinder.check()).containsExactly(listOf("a", "c"))
  }

  @Test fun `three node cycle with self edges`() {
    val nodes = listOf("a", "b", "c")
    val edges = listOf("ab", "bc", "ca", "bb", "cc")
    val cycleFinder = DagChecker(nodes) { it.targets(edges) }
    assertThat(cycleFinder.check()).containsExactly(listOf("a", "b", "c"))
  }

  @Test fun `two independent strongly connected components`() {
    val nodes = listOf("a", "b", "c")
    val edges = listOf("ab", "ba", "cc")
    val cycleFinder = DagChecker(nodes) { it.targets(edges) }
    assertThat(cycleFinder.check()).containsExactly(listOf("a", "b"), listOf("c"))
  }

  /**
   * This is the example graph from the Wikipedia page on the Tarjan algorithm:
   *
   *    A ← B ← C ↔ D
   *    ↓ ↗ ↑   ↑   ↑
   *    E ← F ↔ G ← H ↫
   *
   * These are the four strongly connected components:
   *
   *    A ← B     C ↔ D
   *    ↓ ↗
   *    E     F ↔ G     H ↫
   */
  @Test fun `wikipedia example`() {
    val nodes = listOf("a", "b", "c", "d", "e", "f", "g", "h")
    val edges = listOf(
      "ae", "ba", "cb", "cd", "dc", "eb",
      "fb", "fe", "fg", "gc", "gf", "hd", "hg", "hh"
    )
    val cycleFinder = DagChecker(nodes) { it.targets(edges) }
    assertThat(cycleFinder.check()).containsExactly(
      listOf("a", "e", "b"),
      listOf("c", "d"),
      listOf("f", "g"),
      listOf("h")
    )
  }

  /**
   * Node 'E' participates in two cycles, creating a strongly connected component of size 7. Note
   * that the returned list of nodes is not a cycle; the last element does not have an edge to the
   * first element.
   *
   *    A ← B ← C
   *    ↓   ↑
   *    D → E → F
   *    ↓   ↑   ↓
   *    G   H ← I
   */
  @Test fun `cojoined cycles`() {
    val nodes = listOf("a", "b", "c", "d", "e", "f", "g", "h", "i")
    val edges = listOf(
      "ad", "ba", "cb",
      "de", "dg", "eb", "ef", "fi",
      "he", "ih"
    )
    val cycleFinder = DagChecker(nodes) { it.targets(edges) }
    assertThat(cycleFinder.check()).containsExactly(listOf("a", "d", "e", "b", "f", "i", "h"))
  }

  private fun String.targets(allEdges: List<String>): List<String> {
    return allEdges.filter { it.startsWith(this) }
      .map { it.substring(1) }
  }
}
