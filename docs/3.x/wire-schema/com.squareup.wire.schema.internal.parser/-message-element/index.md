//[wire-schema](../../../index.md)/[com.squareup.wire.schema.internal.parser](../index.md)/[MessageElement](index.md)

# MessageElement

[common]\
data class [MessageElement](index.md)(location: [Location](../../com.squareup.wire.schema/-location/index.md), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), nestedTypes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeElement](../-type-element/index.md)&gt;, options: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](../-option-element/index.md)&gt;, reserveds: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ReservedElement](../-reserved-element/index.md)&gt;, fields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[FieldElement](../-field-element/index.md)&gt;, oneOfs: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OneOfElement](../-one-of-element/index.md)&gt;, extensions: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ExtensionsElement](../-extensions-element/index.md)&gt;, groups: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[GroupElement](../-group-element/index.md)&gt;, extendDeclarations: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ExtendElement](../-extend-element/index.md)&gt;) : [TypeElement](../-type-element/index.md)

## Functions

| Name | Summary |
|---|---|
| [toSchema](to-schema.md) | [common]<br>open override fun [toSchema](to-schema.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Properties

| Name | Summary |
|---|---|
| [documentation](documentation.md) | [common]<br>open override val [documentation](documentation.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [extendDeclarations](extend-declarations.md) | [common]<br>val [extendDeclarations](extend-declarations.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ExtendElement](../-extend-element/index.md)&gt; |
| [extensions](extensions.md) | [common]<br>val [extensions](extensions.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ExtensionsElement](../-extensions-element/index.md)&gt; |
| [fields](fields.md) | [common]<br>val [fields](fields.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[FieldElement](../-field-element/index.md)&gt; |
| [groups](groups.md) | [common]<br>val [groups](groups.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[GroupElement](../-group-element/index.md)&gt; |
| [location](location.md) | [common]<br>open override val [location](location.md): [Location](../../com.squareup.wire.schema/-location/index.md) |
| [name](name.md) | [common]<br>open override val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [nestedTypes](nested-types.md) | [common]<br>open override val [nestedTypes](nested-types.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeElement](../-type-element/index.md)&gt; |
| [oneOfs](one-ofs.md) | [common]<br>val [oneOfs](one-ofs.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OneOfElement](../-one-of-element/index.md)&gt; |
| [options](options.md) | [common]<br>open override val [options](options.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](../-option-element/index.md)&gt; |
| [reserveds](reserveds.md) | [common]<br>val [reserveds](reserveds.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ReservedElement](../-reserved-element/index.md)&gt; |
