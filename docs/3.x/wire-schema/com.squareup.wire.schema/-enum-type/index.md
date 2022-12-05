//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[EnumType](index.md)

# EnumType

[common]\
data class [EnumType](index.md)(type: [ProtoType](../-proto-type/index.md), location: [Location](../-location/index.md), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), constants: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[EnumConstant](../-enum-constant/index.md)&gt;, reserveds: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Reserved](../-reserved/index.md)&gt;, options: [Options](../-options/index.md), syntax: Syntax) : [Type](../-type/index.md)

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [allowAlias](allow-alias.md) | [common]<br>fun [allowAlias](allow-alias.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [constant](constant.md) | [common]<br>fun [constant](constant.md)(tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [EnumConstant](../-enum-constant/index.md)?<br>Returns the constant tagged tag, or null if this enum has no such constant.<br>[common]<br>fun [constant](constant.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [EnumConstant](../-enum-constant/index.md)?<br>Returns the constant named name, or null if this enum has no such constant. |
| [linkMembers](link-members.md) | [common]<br>open override fun [linkMembers](link-members.md)(linker: [Linker](../-linker/index.md)) |
| [linkOptions](link-options.md) | [common]<br>open override fun [linkOptions](link-options.md)(linker: [Linker](../-linker/index.md), syntaxRules: [SyntaxRules](../-syntax-rules/index.md), validate: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |
| [retainAll](retain-all.md) | [common]<br>open override fun [retainAll](retain-all.md)(schema: [Schema](../-schema/index.md), markSet: [MarkSet](../-mark-set/index.md)): [Type](../-type/index.md)? |
| [retainLinked](retain-linked.md) | [common]<br>open override fun [retainLinked](retain-linked.md)(linkedTypes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[ProtoType](../-proto-type/index.md)&gt;, linkedFields: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[Field](../-field/index.md)&gt;): [Type](../-type/index.md)?<br>Returns a copy of this containing only the types in [linkedTypes](retain-linked.md) and extensions in [linkedFields](retain-linked.md), or null if that set is empty. This will return an [EnclosingType](../-enclosing-type/index.md) if it is itself not linked, but its nested types are linked. |
| [toElement](to-element.md) | [common]<br>fun [toElement](to-element.md)(): [EnumElement](../../com.squareup.wire.schema.internal.parser/-enum-element/index.md) |
| [typesAndNestedTypes](../-type/types-and-nested-types.md) | [common]<br>fun [typesAndNestedTypes](../-type/types-and-nested-types.md)(): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Type](../-type/index.md)&gt;<br>Returns all types and subtypes which are linked to the type. |
| [validate](validate.md) | [common]<br>open override fun [validate](validate.md)(linker: [Linker](../-linker/index.md), syntaxRules: [SyntaxRules](../-syntax-rules/index.md)) |

## Properties

| Name | Summary |
|---|---|
| [constants](constants.md) | [common]<br>val [constants](constants.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[EnumConstant](../-enum-constant/index.md)&gt; |
| [documentation](documentation.md) | [common]<br>open override val [documentation](documentation.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [isDeprecated](is-deprecated.md) | [common]<br>val [isDeprecated](is-deprecated.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [location](location.md) | [common]<br>open override val [location](location.md): [Location](../-location/index.md) |
| [name](name.md) | [common]<br>open override val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [nestedExtendList](nested-extend-list.md) | [common]<br>open override val [nestedExtendList](nested-extend-list.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Extend](../-extend/index.md)&gt; |
| [nestedTypes](nested-types.md) | [common]<br>open override val [nestedTypes](nested-types.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Type](../-type/index.md)&gt; |
| [options](options.md) | [common]<br>open override val [options](options.md): [Options](../-options/index.md) |
| [syntax](syntax.md) | [common]<br>open override val [syntax](syntax.md): Syntax |
| [type](type.md) | [common]<br>open override val [type](type.md): [ProtoType](../-proto-type/index.md) |
