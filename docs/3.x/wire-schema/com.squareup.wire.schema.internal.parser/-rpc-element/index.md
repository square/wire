//[wire-schema](../../../index.md)/[com.squareup.wire.schema.internal.parser](../index.md)/[RpcElement](index.md)

# RpcElement

[common]\
data class [RpcElement](index.md)(location: [Location](../../com.squareup.wire.schema/-location/index.md), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), responseType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestStreaming: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), responseStreaming: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), options: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](../-option-element/index.md)&gt;)

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
| [requestStreaming](request-streaming.md) | [common]<br>val [requestStreaming](request-streaming.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false |
| [requestType](request-type.md) | [common]<br>val [requestType](request-type.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [responseStreaming](response-streaming.md) | [common]<br>val [responseStreaming](response-streaming.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false |
| [responseType](response-type.md) | [common]<br>val [responseType](response-type.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
