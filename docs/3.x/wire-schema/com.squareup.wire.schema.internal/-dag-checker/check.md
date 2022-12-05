//[wire-schema](../../../index.md)/[com.squareup.wire.schema.internal](../index.md)/[DagChecker](index.md)/[check](check.md)

# check

[common]\
fun [check](check.md)(): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[N](index.md)&gt;&gt;

Returns a set of strongly connected components. Each strongly connected component is a list of nodes that are mutually reachable to each other.

If the graph contains nodes that have self edges but are not strongly connected to any other nodes, those nodes will be single-element lists in the result.

If the result is empty the graph is acyclic.
