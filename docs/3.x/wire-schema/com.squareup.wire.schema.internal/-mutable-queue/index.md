//[wire-schema](../../../index.md)/[com.squareup.wire.schema.internal](../index.md)/[MutableQueue](index.md)

# MutableQueue

[common, js]\
interface [MutableQueue](index.md)&lt;[T](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; : [MutableCollection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-collection/index.html)&lt;[T](index.md)&gt;

[jvm]\
typealias [MutableQueue](index.md) = [Queue](https://docs.oracle.com/javase/8/docs/api/java/util/Queue.html)&lt;[T](index.md)&gt;

## Functions

| Name | Summary |
|---|---|
| add | [common, js]<br>[common]<br>abstract fun [add](index.md#-1287944101%2FFunctions%2F-876600652)(element: [T](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>[js]<br>abstract fun [add](index.md#-1287944101%2FFunctions%2F-652715946)(element: [T](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| addAll | [common, js]<br>[common]<br>abstract fun [addAll](index.md#1634521508%2FFunctions%2F-876600652)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[T](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>[js]<br>abstract fun [addAll](index.md#1634521508%2FFunctions%2F-652715946)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[T](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| clear | [common, js]<br>[common]<br>abstract fun [clear](index.md#1405312578%2FFunctions%2F-876600652)()<br>[js]<br>abstract fun [clear](index.md#1405312578%2FFunctions%2F-652715946)() |
| contains | [common, js]<br>[common]<br>abstract operator fun [contains](index.md#-1356748575%2FFunctions%2F-876600652)(element: [T](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>[js]<br>abstract operator fun [contains](index.md#-1356748575%2FFunctions%2F-652715946)(element: [T](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| containsAll | [common, js]<br>[common]<br>abstract fun [containsAll](index.md#-784379734%2FFunctions%2F-876600652)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[T](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>[js]<br>abstract fun [containsAll](index.md#-784379734%2FFunctions%2F-652715946)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[T](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| isEmpty | [common, js]<br>[common]<br>abstract fun [isEmpty](index.md#-719293276%2FFunctions%2F-876600652)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>[js]<br>abstract fun [isEmpty](index.md#-719293276%2FFunctions%2F-652715946)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| iterator | [common, js]<br>[common]<br>abstract operator override fun [iterator](index.md#1177836957%2FFunctions%2F-876600652)(): [MutableIterator](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-iterator/index.html)&lt;[T](index.md)&gt;<br>[js]<br>abstract operator override fun [iterator](index.md#1177836957%2FFunctions%2F-652715946)(): [MutableIterator](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-iterator/index.html)&lt;[T](index.md)&gt; |
| [poll](poll.md) | [common, js]<br>[common, js]<br>abstract fun [poll](poll.md)(): [T](index.md)? |
| remove | [common, js]<br>[common]<br>abstract fun [remove](index.md#-58122892%2FFunctions%2F-876600652)(element: [T](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>[js]<br>abstract fun [remove](index.md#-58122892%2FFunctions%2F-652715946)(element: [T](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| removeAll | [common, js]<br>[common]<br>abstract fun [removeAll](index.md#1245700221%2FFunctions%2F-876600652)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[T](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>[js]<br>abstract fun [removeAll](index.md#1245700221%2FFunctions%2F-652715946)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[T](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| retainAll | [common, js]<br>[common]<br>abstract fun [retainAll](index.md#-2061348068%2FFunctions%2F-876600652)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[T](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>[js]<br>abstract fun [retainAll](index.md#-2061348068%2FFunctions%2F-652715946)(elements: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[T](index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |

## Properties

| Name | Summary |
|---|---|
| [size](index.md#-113084078%2FProperties%2F-876600652) | [common]<br>abstract val [size](index.md#-113084078%2FProperties%2F-876600652): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [size](index.md#-113084078%2FProperties%2F-652715946) | [js]<br>abstract val [size](index.md#-113084078%2FProperties%2F-652715946): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
