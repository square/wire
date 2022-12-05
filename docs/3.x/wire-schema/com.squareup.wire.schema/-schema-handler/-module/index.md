//[wire-schema](../../../../index.md)/[com.squareup.wire.schema](../../index.md)/[SchemaHandler](../index.md)/[Module](index.md)

# Module

[common]\
data class [Module](index.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), types: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[ProtoType](../../-proto-type/index.md)&gt;, upstreamTypes: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[ProtoType](../../-proto-type/index.md), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;)

A [Module](index.md) dictates how the loaded types are to be partitioned and handled.

## Constructors

| | |
|---|---|
| [Module](-module.md) | [common]<br>fun [Module](-module.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), types: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[ProtoType](../../-proto-type/index.md)&gt;, upstreamTypes: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[ProtoType](../../-proto-type/index.md), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = mapOf()) |

## Properties

| Name | Summary |
|---|---|
| [name](name.md) | [common]<br>val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>The name of the [Module](index.md). |
| [types](types.md) | [common]<br>val [types](types.md): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[ProtoType](../../-proto-type/index.md)&gt;<br>The types that this module is to handle. |
| [upstreamTypes](upstream-types.md) | [common]<br>val [upstreamTypes](upstream-types.md): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[ProtoType](../../-proto-type/index.md), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>These are the types depended upon by [types](types.md) associated with their module name. |
