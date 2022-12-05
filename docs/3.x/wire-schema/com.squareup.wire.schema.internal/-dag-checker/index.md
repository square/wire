//[wire-schema](../../../index.md)/[com.squareup.wire.schema.internal](../index.md)/[DagChecker](index.md)

# DagChecker

[common]\
class [DagChecker](index.md)&lt;[N](index.md)&gt;(nodes: [Iterable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html)&lt;[N](index.md)&gt;, edges: ([N](index.md)) -&gt; [Iterable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html)&lt;[N](index.md)&gt;)

Checks whether a graph is a directed acyclic graph using [Tarjan's algorithm](https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm).

Note that all cycles are strongly connected components, but a strongly connected component is not strictly a cycle. In particular it may contain nodes that are mutually reachable from each other through multiple paths.

## Constructors

| | |
|---|---|
| [DagChecker](-dag-checker.md) | [common]<br>fun &lt;[N](index.md)&gt; [DagChecker](-dag-checker.md)(nodes: [Iterable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html)&lt;[N](index.md)&gt;, edges: ([N](index.md)) -&gt; [Iterable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html)&lt;[N](index.md)&gt;) |

## Functions

| Name | Summary |
|---|---|
| [check](check.md) | [common]<br>fun [check](check.md)(): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[N](index.md)&gt;&gt;<br>Returns a set of strongly connected components. Each strongly connected component is a list of nodes that are mutually reachable to each other. |
