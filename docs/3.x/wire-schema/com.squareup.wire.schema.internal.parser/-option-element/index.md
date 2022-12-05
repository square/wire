//[wire-schema](../../../index.md)/[com.squareup.wire.schema.internal.parser](../index.md)/[OptionElement](index.md)

# OptionElement

[common]\
data class [OptionElement](index.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), kind: [OptionElement.Kind](-kind/index.md), value: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), isParenthesized: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html))

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |
| [Kind](-kind/index.md) | [common]<br>enum [Kind](-kind/index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[OptionElement.Kind](-kind/index.md)&gt; |
| [OptionPrimitive](-option-primitive/index.md) | [common]<br>data class [OptionPrimitive](-option-primitive/index.md)(kind: [OptionElement.Kind](-kind/index.md), value: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html))<br>An internal representation of the Option primitive types. |

## Functions

| Name | Summary |
|---|---|
| [toSchema](to-schema.md) | [common]<br>fun [toSchema](to-schema.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [toSchemaDeclaration](to-schema-declaration.md) | [common]<br>fun [toSchemaDeclaration](to-schema-declaration.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Properties

| Name | Summary |
|---|---|
| [isParenthesized](is-parenthesized.md) | [common]<br>val [isParenthesized](is-parenthesized.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>If true, this [OptionElement](index.md) is a custom option. |
| [kind](kind.md) | [common]<br>val [kind](kind.md): [OptionElement.Kind](-kind/index.md) |
| [name](name.md) | [common]<br>val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [value](value.md) | [common]<br>val [value](value.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
