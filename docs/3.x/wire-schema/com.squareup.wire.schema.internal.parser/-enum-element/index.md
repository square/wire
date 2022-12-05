//[wire-schema](../../../index.md)/[com.squareup.wire.schema.internal.parser](../index.md)/[EnumElement](index.md)

# EnumElement

[common]\
data class [EnumElement](index.md)(location: [Location](../../com.squareup.wire.schema/-location/index.md), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), options: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](../-option-element/index.md)&gt;, constants: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[EnumConstantElement](../-enum-constant-element/index.md)&gt;, reserveds: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ReservedElement](../-reserved-element/index.md)&gt;) : [TypeElement](../-type-element/index.md)

## Functions

| Name | Summary |
|---|---|
| [toSchema](to-schema.md) | [common]<br>open override fun [toSchema](to-schema.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Properties

| Name | Summary |
|---|---|
| [constants](constants.md) | [common]<br>val [constants](constants.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[EnumConstantElement](../-enum-constant-element/index.md)&gt; |
| [documentation](documentation.md) | [common]<br>open override val [documentation](documentation.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [location](location.md) | [common]<br>open override val [location](location.md): [Location](../../com.squareup.wire.schema/-location/index.md) |
| [name](name.md) | [common]<br>open override val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [nestedTypes](nested-types.md) | [common]<br>open override val [nestedTypes](nested-types.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeElement](../-type-element/index.md)&gt; |
| [options](options.md) | [common]<br>open override val [options](options.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](../-option-element/index.md)&gt; |
| [reserveds](reserveds.md) | [common]<br>val [reserveds](reserveds.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ReservedElement](../-reserved-element/index.md)&gt; |
