//[wire-schema](../../../index.md)/[com.squareup.wire.schema.internal.parser](../index.md)/[ServiceElement](index.md)

# ServiceElement

[common]\
data class [ServiceElement](index.md)(location: [Location](../../com.squareup.wire.schema/-location/index.md), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), rpcs: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[RpcElement](../-rpc-element/index.md)&gt;, options: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](../-option-element/index.md)&gt;)

## Functions

| Name | Summary |
|---|---|
| [toSchema](to-schema.md) | [common]<br>fun [toSchema](to-schema.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Properties

| Name | Summary |
|---|---|
| [documentation](documentation.md) | [common]<br>val [documentation](documentation.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [location](location.md) | [common]<br>val [location](location.md): [Location](../../com.squareup.wire.schema/-location/index.md) |
| [name](name.md) | [common]<br>val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [options](options.md) | [common]<br>val [options](options.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](../-option-element/index.md)&gt; |
| [rpcs](rpcs.md) | [common]<br>val [rpcs](rpcs.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[RpcElement](../-rpc-element/index.md)&gt; |
