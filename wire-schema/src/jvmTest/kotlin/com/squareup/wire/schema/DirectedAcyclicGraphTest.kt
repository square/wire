/*
 * Copyright (C) 2025 Square, Inc.
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
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.test.fail

class DirectedAcyclicGraphTest {

  @Test
  fun singleNode() {
    val nodes = listOf("A")
    val graph = DirectedAcyclicGraph(nodes) { emptyList() }
    val order = graph.topologicalOrder()
    assertThat(order).containsExactly("A")
  }

  @Test
  fun simpleLinearDag() {
    // A -> B -> C
    val nodes = listOf("A", "B", "C")
    val edges = mapOf(
      "A" to listOf("B"),
      "B" to listOf("C"),
      "C" to emptyList(),
    )
    val graph = DirectedAcyclicGraph(nodes) { edges[it].orEmpty() }
    val order = graph.topologicalOrder()
    assertThat(order).isEqualTo(listOf("A", "B", "C"))
  }

  @Test
  fun simpleNonLinearDag() {
    // A
    // C -> B
    val nodes = listOf("A", "B", "C")
    val edges = mapOf(
      "A" to emptyList(),
      "B" to emptyList(),
      "C" to listOf("B"),
    )
    val graph = DirectedAcyclicGraph(nodes) { edges[it].orEmpty() }
    val order = graph.topologicalOrder()
    assertThat(order).isEqualTo(listOf("A", "C", "B"))
  }

  @Test
  fun branchingDag() {
    //   A
    //  / \
    // B   C
    //  \ /
    //   D
    val nodes = listOf("A", "B", "C", "D")
    val edges = mapOf(
      "A" to listOf("B", "C"),
      "B" to listOf("D"),
      "C" to listOf("D"),
      "D" to emptyList(),
    )
    val graph = DirectedAcyclicGraph(nodes) { edges[it].orEmpty() }
    val order = graph.topologicalOrder()
    assertThat(order).isEqualTo(listOf("A", "B", "C", "D"))
  }

  @Test
  fun multipleRoots() {
    // A -> C
    // B -> C
    val nodes = listOf("A", "B", "C")
    val edges = mapOf(
      "A" to listOf("C"),
      "B" to listOf("C"),
      "C" to emptyList(),
    )
    val graph = DirectedAcyclicGraph(nodes) { edges[it].orEmpty() }
    val order = graph.topologicalOrder()
    assertThat(order).isEqualTo(listOf("A", "B", "C"))
  }

  @Test
  fun cycleThrowsError() {
    // A -> B -> C -> A
    val nodes = listOf("A", "B", "C")
    val edges = mapOf(
      "A" to listOf("B"),
      "B" to listOf("C"),
      "C" to listOf("A"),
    )
    val graph = DirectedAcyclicGraph(nodes) { edges[it].orEmpty() }

    try {
      graph.topologicalOrder()
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage("Graph contains a cycle, topological sort not possible!")
    }
  }
}
