//[wire-runtime](../../../../index.md)/[com.squareup.wire](../../index.md)/[WireField](../index.md)/[Label](index.md)

# Label

[common]\
enum [Label](index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[WireField.Label](index.md)&gt; 

A protocol buffer label.

## Entries

| | |
|---|---|
| [OMIT_IDENTITY](-o-m-i-t_-i-d-e-n-t-i-t-y/index.md) | [common]<br>[OMIT_IDENTITY](-o-m-i-t_-i-d-e-n-t-i-t-y/index.md)()<br>Special label to define proto3 fields which should not be emitted if their value is equal to their type's respective identity value. E.g.: a field of type int32 will not get emitted if its value is 0. |
| [PACKED](-p-a-c-k-e-d/index.md) | [common]<br>[PACKED](-p-a-c-k-e-d/index.md)()<br>Implies [REPEATED](-r-e-p-e-a-t-e-d/index.md). |
| [ONE_OF](-o-n-e_-o-f/index.md) | [common]<br>[ONE_OF](-o-n-e_-o-f/index.md)() |
| [REPEATED](-r-e-p-e-a-t-e-d/index.md) | [common]<br>[REPEATED](-r-e-p-e-a-t-e-d/index.md)() |
| [OPTIONAL](-o-p-t-i-o-n-a-l/index.md) | [common]<br>[OPTIONAL](-o-p-t-i-o-n-a-l/index.md)() |
| [REQUIRED](-r-e-q-u-i-r-e-d/index.md) | [common]<br>[REQUIRED](-r-e-q-u-i-r-e-d/index.md)() |

## Properties

| Name | Summary |
|---|---|
| [isOneOf](is-one-of.md) | [common]<br>@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = "isOneOf")<br>val [isOneOf](is-one-of.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isPacked](is-packed.md) | [common]<br>@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = "isPacked")<br>val [isPacked](is-packed.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isRepeated](is-repeated.md) | [common]<br>@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = "isRepeated")<br>val [isRepeated](is-repeated.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [name](-r-e-q-u-i-r-e-d/index.md#-372974862%2FProperties%2F-1082500773) | [common]<br>val [name](-r-e-q-u-i-r-e-d/index.md#-372974862%2FProperties%2F-1082500773): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [ordinal](-r-e-q-u-i-r-e-d/index.md#-739389684%2FProperties%2F-1082500773) | [common]<br>val [ordinal](-r-e-q-u-i-r-e-d/index.md#-739389684%2FProperties%2F-1082500773): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
