//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[MessageType](index.md)

# MessageType

[common]\
data class [MessageType](index.md)(type: [ProtoType](../-proto-type/index.md), location: [Location](../-location/index.md), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), declaredFields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Field](../-field/index.md)&gt;, extensionFields: [MutableList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)&lt;[Field](../-field/index.md)&gt;, oneOfs: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OneOf](../-one-of/index.md)&gt;, nestedTypes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Type](../-type/index.md)&gt;, nestedExtendList: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Extend](../-extend/index.md)&gt;, extensionsList: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Extensions](../-extensions/index.md)&gt;, reserveds: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Reserved](../-reserved/index.md)&gt;, options: [Options](../-options/index.md), syntax: Syntax) : [Type](../-type/index.md)

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [addExtensionFields](add-extension-fields.md) | [common]<br>fun [addExtensionFields](add-extension-fields.md)(fields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Field](../-field/index.md)&gt;) |
| [extensionField](extension-field.md) | [common]<br>fun [extensionField](extension-field.md)(qualifiedName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Field](../-field/index.md)?<br>Returns the field with the qualified name [qualifiedName](extension-field.md), or null if this type has no such field. |
| [extensionFieldsMap](extension-fields-map.md) | [common]<br>fun [extensionFieldsMap](extension-fields-map.md)(): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Field](../-field/index.md)&gt; |
| [field](field.md) | [common]<br>fun [field](field.md)(tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [Field](../-field/index.md)?<br>Returns the field tagged [tag](field.md), or null if this type has no such field.<br>[common]<br>fun [field](field.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Field](../-field/index.md)?<br>Returns the field named [name](field.md), or null if this type has no such field. |
| [linkMembers](link-members.md) | [common]<br>open override fun [linkMembers](link-members.md)(linker: [Linker](../-linker/index.md)) |
| [linkOptions](link-options.md) | [common]<br>open override fun [linkOptions](link-options.md)(linker: [Linker](../-linker/index.md), syntaxRules: [SyntaxRules](../-syntax-rules/index.md), validate: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |
| [retainAll](retain-all.md) | [common]<br>open override fun [retainAll](retain-all.md)(schema: [Schema](../-schema/index.md), markSet: [MarkSet](../-mark-set/index.md)): [Type](../-type/index.md)? |
| [retainLinked](retain-linked.md) | [common]<br>open override fun [retainLinked](retain-linked.md)(linkedTypes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[ProtoType](../-proto-type/index.md)&gt;, linkedFields: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[Field](../-field/index.md)&gt;): [Type](../-type/index.md)?<br>Returns a copy of this containing only the types in [linkedTypes](retain-linked.md) and extensions in [linkedFields](retain-linked.md), or null if that set is empty. This will return an [EnclosingType](../-enclosing-type/index.md) if it is itself not linked, but its nested types are linked. |
| [toElement](to-element.md) | [common]<br>fun [toElement](to-element.md)(): [MessageElement](../../com.squareup.wire.schema.internal.parser/-message-element/index.md) |
| [typesAndNestedTypes](../-type/types-and-nested-types.md) | [common]<br>fun [typesAndNestedTypes](../-type/types-and-nested-types.md)(): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Type](../-type/index.md)&gt;<br>Returns all types and subtypes which are linked to the type. |
| [validate](validate.md) | [common]<br>open override fun [validate](validate.md)(linker: [Linker](../-linker/index.md), syntaxRules: [SyntaxRules](../-syntax-rules/index.md)) |

## Properties

| Name | Summary |
|---|---|
| [declaredFields](declared-fields.md) | [common]<br>val [declaredFields](declared-fields.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Field](../-field/index.md)&gt; |
| [documentation](documentation.md) | [common]<br>open override val [documentation](documentation.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [extensionFields](extension-fields.md) | [common]<br>val [extensionFields](extension-fields.md): [MutableList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)&lt;[Field](../-field/index.md)&gt; |
| [extensionsList](extensions-list.md) | [common]<br>val [extensionsList](extensions-list.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Extensions](../-extensions/index.md)&gt; |
| [fields](fields.md) | [common]<br>@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = "fields")<br>val [fields](fields.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Field](../-field/index.md)&gt; |
| [fieldsAndOneOfFields](fields-and-one-of-fields.md) | [common]<br>val [fieldsAndOneOfFields](fields-and-one-of-fields.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Field](../-field/index.md)&gt; |
| [isDeprecated](is-deprecated.md) | [common]<br>val [isDeprecated](is-deprecated.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [location](location.md) | [common]<br>open override val [location](location.md): [Location](../-location/index.md) |
| [name](name.md) | [common]<br>open override val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [nestedExtendList](nested-extend-list.md) | [common]<br>open override val [nestedExtendList](nested-extend-list.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Extend](../-extend/index.md)&gt; |
| [nestedTypes](nested-types.md) | [common]<br>open override val [nestedTypes](nested-types.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Type](../-type/index.md)&gt; |
| [oneOfs](one-ofs.md) | [common]<br>val [oneOfs](one-ofs.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OneOf](../-one-of/index.md)&gt; |
| [options](options.md) | [common]<br>open override val [options](options.md): [Options](../-options/index.md) |
| [requiredFields](required-fields.md) | [common]<br>val [requiredFields](required-fields.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Field](../-field/index.md)&gt; |
| [syntax](syntax.md) | [common]<br>open override val [syntax](syntax.md): Syntax |
| [type](type.md) | [common]<br>open override val [type](type.md): [ProtoType](../-proto-type/index.md) |
