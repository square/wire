//[wire-schema-tests](../../../index.md)/[com.squareup.wire](../index.md)/[WireTestLogger](index.md)

# WireTestLogger

[jvm]\
class [WireTestLogger](index.md) : WireLogger

## Functions

| Name | Summary |
|---|---|
| [artifactHandled](artifact-handled.md) | [jvm]<br>open override fun [artifactHandled](artifact-handled.md)(outputPath: Path, qualifiedName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), targetName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [artifactSkipped](artifact-skipped.md) | [jvm]<br>open override fun [artifactSkipped](artifact-skipped.md)(type: ProtoType, targetName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [unusedExcludesInTarget](unused-excludes-in-target.md) | [jvm]<br>open override fun [unusedExcludesInTarget](unused-excludes-in-target.md)(unusedExcludes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;) |
| [unusedIncludesInTarget](unused-includes-in-target.md) | [jvm]<br>open override fun [unusedIncludesInTarget](unused-includes-in-target.md)(unusedIncludes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;) |
| [unusedPrunes](unused-prunes.md) | [jvm]<br>open override fun [unusedPrunes](unused-prunes.md)(unusedPrunes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;) |
| [unusedRoots](unused-roots.md) | [jvm]<br>open override fun [unusedRoots](unused-roots.md)(unusedRoots: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;) |

## Properties

| Name | Summary |
|---|---|
| [artifactHandled](artifact-handled.md) | [jvm]<br>val [artifactHandled](artifact-handled.md): [ArrayDeque](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-array-deque/index.html)&lt;[Triple](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-triple/index.html)&lt;Path, [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;&gt; |
| [artifactSkipped](artifact-skipped.md) | [jvm]<br>val [artifactSkipped](artifact-skipped.md): [ArrayDeque](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-array-deque/index.html)&lt;[Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;ProtoType, [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;&gt; |
| [unusedExcludesInTarget](unused-excludes-in-target.md) | [jvm]<br>val [unusedExcludesInTarget](unused-excludes-in-target.md): [ArrayDeque](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-array-deque/index.html)&lt;[Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;&gt; |
| [unusedIncludesInTarget](unused-includes-in-target.md) | [jvm]<br>val [unusedIncludesInTarget](unused-includes-in-target.md): [ArrayDeque](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-array-deque/index.html)&lt;[Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;&gt; |
| [unusedPrunes](unused-prunes.md) | [jvm]<br>val [unusedPrunes](unused-prunes.md): [ArrayDeque](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-array-deque/index.html)&lt;[Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;&gt; |
| [unusedRoots](unused-roots.md) | [jvm]<br>val [unusedRoots](unused-roots.md): [ArrayDeque](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-array-deque/index.html)&lt;[Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;&gt; |
