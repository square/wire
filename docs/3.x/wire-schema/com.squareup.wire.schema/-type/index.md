//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Type](index.md)

# Type

[common]\
sealed class [Type](index.md)

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [linkMembers](link-members.md) | [common]<br>abstract fun [linkMembers](link-members.md)(linker: [Linker](../-linker/index.md)) |
| [linkOptions](link-options.md) | [common]<br>abstract fun [linkOptions](link-options.md)(linker: [Linker](../-linker/index.md), syntaxRules: [SyntaxRules](../-syntax-rules/index.md), validate: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |
| [retainAll](retain-all.md) | [common]<br>abstract fun [retainAll](retain-all.md)(schema: [Schema](../-schema/index.md), markSet: [MarkSet](../-mark-set/index.md)): [Type](index.md)? |
| [retainLinked](retain-linked.md) | [common]<br>abstract fun [retainLinked](retain-linked.md)(linkedTypes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[ProtoType](../-proto-type/index.md)&gt;, linkedFields: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[Field](../-field/index.md)&gt;): [Type](index.md)?<br>Returns a copy of this containing only the types in [linkedTypes](retain-linked.md) and extensions in [linkedFields](retain-linked.md), or null if that set is empty. This will return an [EnclosingType](../-enclosing-type/index.md) if it is itself not linked, but its nested types are linked. |
| [typesAndNestedTypes](types-and-nested-types.md) | [common]<br>fun [typesAndNestedTypes](types-and-nested-types.md)(): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Type](index.md)&gt;<br>Returns all types and subtypes which are linked to the type. |
| [validate](validate.md) | [common]<br>abstract fun [validate](validate.md)(linker: [Linker](../-linker/index.md), syntaxRules: [SyntaxRules](../-syntax-rules/index.md)) |

## Properties

| Name | Summary |
|---|---|
| [documentation](documentation.md) | [common]<br>abstract val [documentation](documentation.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [location](location.md) | [common]<br>abstract val [location](location.md): [Location](../-location/index.md) |
| [name](name.md) | [common]<br>abstract val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [nestedExtendList](nested-extend-list.md) | [common]<br>abstract val [nestedExtendList](nested-extend-list.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Extend](../-extend/index.md)&gt; |
| [nestedTypes](nested-types.md) | [common]<br>abstract val [nestedTypes](nested-types.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Type](index.md)&gt; |
| [options](options.md) | [common]<br>abstract val [options](options.md): [Options](../-options/index.md) |
| [syntax](syntax.md) | [common]<br>abstract val [syntax](syntax.md): Syntax |
| [type](type.md) | [common]<br>abstract val [type](type.md): [ProtoType](../-proto-type/index.md) |

## Inheritors

| Name |
|---|
| [EnclosingType](../-enclosing-type/index.md) |
| [EnumType](../-enum-type/index.md) |
| [MessageType](../-message-type/index.md) |
