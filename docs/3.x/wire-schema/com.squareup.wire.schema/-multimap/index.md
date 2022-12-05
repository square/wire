//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Multimap](index.md)

# Multimap

[common, js]\
interface [Multimap](index.md)&lt;[K](index.md), [V](index.md)&gt;

[jvm]\
typealias [Multimap](index.md) = Multimap&lt;[K](index.md), [V](index.md)&gt;

## Functions

| Name | Summary |
|---|---|
| [asMap](as-map.md) | [common, js]<br>[common, js]<br>abstract fun [asMap](as-map.md)(): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[K](index.md), [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[V](index.md)&gt;&gt; |
| [containsKey](contains-key.md) | [common, js]<br>[common, js]<br>abstract fun [containsKey](contains-key.md)(key: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [containsValue](contains-value.md) | [common, js]<br>[common, js]<br>abstract fun [containsValue](contains-value.md)(value: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [get](get.md) | [common, js]<br>[common, js]<br>abstract operator fun [get](get.md)(key: [K](index.md)?): [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[V](index.md)&gt; |
| [isEmpty](is-empty.md) | [common, js]<br>[common, js]<br>abstract fun [isEmpty](is-empty.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [size](size.md) | [common, js]<br>[common, js]<br>abstract fun [size](size.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [values](values.md) | [common, js]<br>[common, js]<br>abstract fun [values](values.md)(): [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)&lt;[V](index.md)&gt; |
