//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Service](index.md)

# Service

[common]\
data class [Service](index.md)(type: [ProtoType](../-proto-type/index.md), location: [Location](../-location/index.md), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), rpcs: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Rpc](../-rpc/index.md)&gt;, options: [Options](../-options/index.md))

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [link](link.md) | [common]<br>fun [link](link.md)(linker: [Linker](../-linker/index.md)) |
| [linkOptions](link-options.md) | [common]<br>fun [linkOptions](link-options.md)(linker: [Linker](../-linker/index.md), validate: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |
| [retainAll](retain-all.md) | [common]<br>fun [retainAll](retain-all.md)(schema: [Schema](../-schema/index.md), markSet: [MarkSet](../-mark-set/index.md)): [Service](index.md)? |
| [rpc](rpc.md) | [common]<br>fun [rpc](rpc.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Rpc](../-rpc/index.md)?<br>Returns the RPC named name, or null if this service has no such method. |
| [validate](validate.md) | [common]<br>fun [validate](validate.md)(linker: [Linker](../-linker/index.md)) |

## Properties

| Name | Summary |
|---|---|
| [documentation](documentation.md) | [common]<br>@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = "documentation")<br>val [documentation](documentation.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [location](location.md) | [common]<br>@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = "location")<br>val [location](location.md): [Location](../-location/index.md) |
| [name](name.md) | [common]<br>@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = "name")<br>val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [options](options.md) | [common]<br>@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = "options")<br>val [options](options.md): [Options](../-options/index.md) |
| [rpcs](rpcs.md) | [common]<br>@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = "rpcs")<br>val [rpcs](rpcs.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Rpc](../-rpc/index.md)&gt; |
| [type](type.md) | [common]<br>@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = "type")<br>val [type](type.md): [ProtoType](../-proto-type/index.md) |
