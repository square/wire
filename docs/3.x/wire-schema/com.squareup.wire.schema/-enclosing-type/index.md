//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[EnclosingType](index.md)

# EnclosingType

[common]\
data class [EnclosingType](index.md)(location: [Location](../-location/index.md), type: [ProtoType](../-proto-type/index.md), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), nestedTypes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Type](../-type/index.md)&gt;, nestedExtendList: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Extend](../-extend/index.md)&gt;, syntax: Syntax) : [Type](../-type/index.md)

An empty type which only holds nested types.

## Constructors

| | |
|---|---|
| [EnclosingType](-enclosing-type.md) | [common]<br>fun [EnclosingType](-enclosing-type.md)(location: [Location](../-location/index.md), type: [ProtoType](../-proto-type/index.md), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), nestedTypes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Type](../-type/index.md)&gt;, nestedExtendList: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Extend](../-extend/index.md)&gt;, syntax: Syntax) |

## Functions

| Name | Summary |
|---|---|
| [linkMembers](link-members.md) | [common]<br>open override fun [linkMembers](link-members.md)(linker: [Linker](../-linker/index.md)) |
| [linkOptions](link-options.md) | [common]<br>open override fun [linkOptions](link-options.md)(linker: [Linker](../-linker/index.md), syntaxRules: [SyntaxRules](../-syntax-rules/index.md), validate: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |
| [retainAll](retain-all.md) | [common]<br>open override fun [retainAll](retain-all.md)(schema: [Schema](../-schema/index.md), markSet: [MarkSet](../-mark-set/index.md)): [Type](../-type/index.md)? |
| [retainLinked](retain-linked.md) | [common]<br>open override fun [retainLinked](retain-linked.md)(linkedTypes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[ProtoType](../-proto-type/index.md)&gt;, linkedFields: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[Field](../-field/index.md)&gt;): [Type](../-type/index.md)?<br>Returns a copy of this containing only the types in [linkedTypes](retain-linked.md) and extensions in [linkedFields](retain-linked.md), or null if that set is empty. This will return an [EnclosingType](index.md) if it is itself not linked, but its nested types are linked. |
| [toElement](to-element.md) | [common]<br>fun [toElement](to-element.md)(): [MessageElement](../../com.squareup.wire.schema.internal.parser/-message-element/index.md) |
| [typesAndNestedTypes](../-type/types-and-nested-types.md) | [common]<br>fun [typesAndNestedTypes](../-type/types-and-nested-types.md)(): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Type](../-type/index.md)&gt;<br>Returns all types and subtypes which are linked to the type. |
| [validate](validate.md) | [common]<br>open override fun [validate](validate.md)(linker: [Linker](../-linker/index.md), syntaxRules: [SyntaxRules](../-syntax-rules/index.md)) |

## Properties

| Name | Summary |
|---|---|
| [documentation](documentation.md) | [common]<br>open override val [documentation](documentation.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [location](location.md) | [common]<br>open override val [location](location.md): [Location](../-location/index.md) |
| [name](name.md) | [common]<br>open override val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [nestedExtendList](nested-extend-list.md) | [common]<br>open override val [nestedExtendList](nested-extend-list.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Extend](../-extend/index.md)&gt; |
| [nestedTypes](nested-types.md) | [common]<br>open override val [nestedTypes](nested-types.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Type](../-type/index.md)&gt; |
| [options](options.md) | [common]<br>open override val [options](options.md): [Options](../-options/index.md) |
| [syntax](syntax.md) | [common]<br>open override val [syntax](syntax.md): Syntax |
| [type](type.md) | [common]<br>open override val [type](type.md): [ProtoType](../-proto-type/index.md) |
