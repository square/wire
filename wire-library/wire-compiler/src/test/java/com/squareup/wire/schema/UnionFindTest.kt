package com.squareup.wire.schema

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class UnionFindTest {

  private fun UnionFind<String>.union(graph: Map<String, Set<String>>) {
    for (v in graph.keys) {
      for (adj in graph.getValue(v)) {
        union(v, adj)
      }
    }
  }

  @Test
  fun unionFindNoEdges() {
    val graph = mapOf<String, Set<String>>(
        "a" to emptySet(),
        "b" to emptySet(),
        "c" to emptySet()
    )
    val uf = UnionFind(graph.keys)

    uf.union(graph)

    assertThat(uf.find("a")).isEqualTo("a")
    assertThat(uf.find("b")).isEqualTo("b")
    assertThat(uf.find("c")).isEqualTo("c")
  }

  @Test
  fun unionFindWithEdges() {
    val graph = mapOf(
        "a" to setOf("b"),
        "b" to setOf("c"),
        "c" to setOf("a"),
        "d" to setOf("e"),
        "e" to emptySet()
    )
    val uf = UnionFind(graph.keys)

    uf.union(graph)

    assertThat(uf.find("b")).isEqualTo("a")
    assertThat(uf.find("c")).isEqualTo("a")
    assertThat(uf.find("d")).isEqualTo("d")
    assertThat(uf.find("e")).isEqualTo("d")
  }
}