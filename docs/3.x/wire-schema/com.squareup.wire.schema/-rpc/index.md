//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Rpc](index.md)

# Rpc

[common]\
data class [Rpc](index.md)(location: [Location](../-location/index.md), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestTypeElement: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), responseTypeElement: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestStreaming: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), responseStreaming: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), options: [Options](../-options/index.md))

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [link](link.md) | [common]<br>fun [link](link.md)(linker: [Linker](../-linker/index.md)) |
| [linkOptions](link-options.md) | [common]<br>fun [linkOptions](link-options.md)(linker: [Linker](../-linker/index.md), validate: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |
| [retainAll](retain-all.md) | [common]<br>fun [retainAll](retain-all.md)(schema: [Schema](../-schema/index.md), markSet: [MarkSet](../-mark-set/index.md)): [Rpc](index.md)? |
| [validate](validate.md) | [common]<br>fun [validate](validate.md)(linker: [Linker](../-linker/index.md)) |

## Properties

| Name | Summary |
|---|---|
| [documentation](documentation.md) | [common]<br>val [documentation](documentation.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [location](location.md) | [common]<br>val [location](location.md): [Location](../-location/index.md) |
| [name](name.md) | [common]<br>val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [options](options.md) | [common]<br>val [options](options.md): [Options](../-options/index.md) |
| [requestStreaming](request-streaming.md) | [common]<br>val [requestStreaming](request-streaming.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [requestType](request-type.md) | [common]<br>var [requestType](request-type.md): [ProtoType](../-proto-type/index.md)? = null |
| [responseStreaming](response-streaming.md) | [common]<br>val [responseStreaming](response-streaming.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [responseType](response-type.md) | [common]<br>var [responseType](response-type.md): [ProtoType](../-proto-type/index.md)? = null |
