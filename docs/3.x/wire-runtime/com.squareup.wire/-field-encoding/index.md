//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[FieldEncoding](index.md)

# FieldEncoding

[common]\
enum [FieldEncoding](index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[FieldEncoding](index.md)&gt;

## Entries

| | |
|---|---|
| [FIXED32](-f-i-x-e-d32/index.md) | [common]<br>[FIXED32](-f-i-x-e-d32/index.md)(5) |
| [LENGTH_DELIMITED](-l-e-n-g-t-h_-d-e-l-i-m-i-t-e-d/index.md) | [common]<br>[LENGTH_DELIMITED](-l-e-n-g-t-h_-d-e-l-i-m-i-t-e-d/index.md)(2) |
| [FIXED64](-f-i-x-e-d64/index.md) | [common]<br>[FIXED64](-f-i-x-e-d64/index.md)(1) |
| [VARINT](-v-a-r-i-n-t/index.md) | [common]<br>[VARINT](-v-a-r-i-n-t/index.md)(0) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [rawProtoAdapter](raw-proto-adapter.md) | [common]<br>fun [rawProtoAdapter](raw-proto-adapter.md)(): [ProtoAdapter](../-proto-adapter/index.md)&lt;*&gt;<br>Returns a Wire adapter that reads this field encoding without interpretation. For example, messages are returned as byte strings and enums are returned as integers. |

## Properties

| Name | Summary |
|---|---|
| [name](../-wire-field/-label/-r-e-q-u-i-r-e-d/index.md#-372974862%2FProperties%2F-1082500773) | [common]<br>val [name](../-wire-field/-label/-r-e-q-u-i-r-e-d/index.md#-372974862%2FProperties%2F-1082500773): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [ordinal](../-wire-field/-label/-r-e-q-u-i-r-e-d/index.md#-739389684%2FProperties%2F-1082500773) | [common]<br>val [ordinal](../-wire-field/-label/-r-e-q-u-i-r-e-d/index.md#-739389684%2FProperties%2F-1082500773): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
