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
package com.squareup.wire.schema.internal

/**
 * Checks whether a graph is a directed acyclic graph using [Tarjan's algorithm][tarjan].
 *
 * Note that all cycles are strongly connected components, but a strongly connected component is not
 * strictly a cycle. In particular it may contain nodes that are mutually reachable from each other
 * through multiple paths.
 *
 * [tarjan]: https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm
 */
class DagChecker<N>(
  private val nodes: Iterable<N>,
  private val edges: (N) -> Iterable<N>,
) {
  private var nextDiscoveryId = 0
  private val tags = nodes.associateWith { Tag(it) }
  private val stack = mutableListOf<Tag<N>>()
  private val result = mutableSetOf<List<N>>()

  private val N.tag: Tag<N>?
    get() = tags[this]

  /**
   * Returns a set of strongly connected components. Each strongly connected component is a list of
   * nodes that are mutually reachable to each other.
   *
   * If the graph contains nodes that have self edges but are not strongly connected to any other
   * nodes, those nodes will be single-element lists in the result.
   *
   * If the result is empty the graph is acyclic.
   */
  fun check(): Set<List<N>> {
    check(nextDiscoveryId == 0)

    for (node in nodes) {
      val tag = node.tag!!
      if (tag.discoveryId == -1) {
        tag.discoverDepthFirst()
      }
    }

    return result
  }

  /**
   * Traverse this node and all of the nodes it can reach. This returns the lowest discovery ID of
   * the set of nodes strongly connected to this node.
   */
  private fun Tag<N>.discoverDepthFirst(): Int {
    discoveryId = nextDiscoveryId
    lowestConnectedDiscoveryId = nextDiscoveryId
    nextDiscoveryId++

    val stackIndex = stack.size
    stack += this
    onStack = true

    for (target in edges(node)) {
      val t = target.tag ?: continue

      if (t.discoveryId == -1) {
        // Traverse a new node. If in the process it received a lower discovery ID, it must be
        // strongly connected to this node! Take that lower discovery ID.
        lowestConnectedDiscoveryId = minOf(lowestConnectedDiscoveryId, t.discoverDepthFirst())
      } else if (t.onStack) {
        // Node a new node, but one we're in a cycle with. Take its discover ID if it's lower.
        if (t == this) selfEdge = true
        lowestConnectedDiscoveryId = minOf(lowestConnectedDiscoveryId, t.discoveryId)
      }
    }

    // We've traversed all the edges. If our discovery ID is the lowest then we're the root of our
    // strongly connected component. Include it in the result.
    if (discoveryId == lowestConnectedDiscoveryId) {
      val slice = stack.subList(stackIndex, stack.size)
      val component = slice.toList()
      slice.clear()

      for (tag in component) {
        tag.onStack = false
      }

      if (component.size > 1 || component.single().selfEdge) {
        result += component.map { it.node }
      }
    }

    return lowestConnectedDiscoveryId
  }

  private class Tag<N>(
    var node: N,
  ) {
    var discoveryId = -1
    var lowestConnectedDiscoveryId = -1
    var onStack = false
    var selfEdge = false
  }
}
