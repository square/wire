//[wire-schema](../../../index.md)/[com.squareup.wire.schema.internal.parser](../index.md)/[TypeElement](index.md)

# TypeElement

[common]\
interface [TypeElement](index.md)

A message type or enum type declaration.

## Functions

| Name | Summary |
|---|---|
| [toSchema](to-schema.md) | [common]<br>abstract fun [toSchema](to-schema.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Properties

| Name | Summary |
|---|---|
| [documentation](documentation.md) | [common]<br>abstract val [documentation](documentation.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [location](location.md) | [common]<br>abstract val [location](location.md): [Location](../../com.squareup.wire.schema/-location/index.md) |
| [name](name.md) | [common]<br>abstract val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [nestedTypes](nested-types.md) | [common]<br>abstract val [nestedTypes](nested-types.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeElement](index.md)&gt; |
| [options](options.md) | [common]<br>abstract val [options](options.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](../-option-element/index.md)&gt; |

## Inheritors

| Name |
|---|
| [EnumElement](../-enum-element/index.md) |
| [MessageElement](../-message-element/index.md) |
