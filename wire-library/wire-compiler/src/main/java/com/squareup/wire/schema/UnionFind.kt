package com.squareup.wire.schema

internal class UnionFind<T>(val elements: Iterable<T>) {

  private val parentMap = mutableMapOf<T, T>()
  private val rankMap = mutableMapOf<T, Int>()

  init {
    for (e in elements) {
      parentMap[e] = e
      rankMap[e] = 0
    }
  }

  val parents: Map<T, T>
    get() = parentMap.toMap()

  fun find(element: T): T {
    var current = element
    while (true) {
      val parent = parentMap.getValue(current)
      if (parent == current) {
        break
      }
      current = parent
    }

    val root = current
    current = element
    while (current != root) {
      val parent = parentMap.getValue(current)
      parentMap[current] = root
      current = parent
    }
    return root
  }

  fun union(element1: T, element2: T) {
    val parent1 = find(element1)
    val parent2 = find(element2)

    if (parent1 == parent2) {
      return
    }

    val rank1 = rankMap.getValue(parent1)
    val rank2 = rankMap.getValue(parent2)
    when {
      rank1 > rank2 -> parentMap[parent2] = parent1
      rank1 < rank2 -> parentMap[parent1] = parent2
      else -> {
        parentMap[parent2] = parent1
        rankMap[parent1] = rank1 + 1
      }
    }
  }
}