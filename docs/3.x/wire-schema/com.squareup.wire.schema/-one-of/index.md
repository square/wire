//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[OneOf](index.md)

# OneOf

[common]\
data class [OneOf](index.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), fields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Field](../-field/index.md)&gt;)

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [link](link.md) | [common]<br>fun [link](link.md)(linker: [Linker](../-linker/index.md)) |
| [linkOptions](link-options.md) | [common]<br>fun [linkOptions](link-options.md)(linker: [Linker](../-linker/index.md), syntaxRules: [SyntaxRules](../-syntax-rules/index.md), validate: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |
| [retainAll](retain-all.md) | [common]<br>fun [retainAll](retain-all.md)(schema: [Schema](../-schema/index.md), markSet: [MarkSet](../-mark-set/index.md), enclosingType: [ProtoType](../-proto-type/index.md)): [OneOf](index.md)? |
| [retainLinked](retain-linked.md) | [common]<br>fun [retainLinked](retain-linked.md)(): [OneOf](index.md)? |

## Properties

| Name | Summary |
|---|---|
| [documentation](documentation.md) | [common]<br>val [documentation](documentation.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [fields](fields.md) | [common]<br>val [fields](fields.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Field](../-field/index.md)&gt; |
| [name](name.md) | [common]<br>val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
