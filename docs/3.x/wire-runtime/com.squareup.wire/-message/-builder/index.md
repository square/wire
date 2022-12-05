//[wire-runtime](../../../../index.md)/[com.squareup.wire](../../index.md)/[Message](../index.md)/[Builder](index.md)

# Builder

[common, js, jvm, native]\
abstract class [Builder](index.md)&lt;[M](index.md) : [Message](../index.md)&lt;[M](index.md), [B](index.md)&gt;, [B](index.md) : [Message.Builder](index.md)&lt;[M](index.md), [B](index.md)&gt;&gt;

Superclass for protocol buffer message builders.

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [js]<br>fun [Builder](-builder.md)() |
| [Builder](-builder.md) | [native]<br>fun [Builder](-builder.md)() |

## Functions

| Name | Summary |
|---|---|
| [addUnknownField](add-unknown-field.md) | [jvm]<br>fun [addUnknownField](add-unknown-field.md)(tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), fieldEncoding: FieldEncoding, value: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?): [Message.Builder](index.md)&lt;[M](index.md), [B](index.md)&gt; |
| [addUnknownFields](add-unknown-fields.md) | [jvm]<br>fun [addUnknownFields](add-unknown-fields.md)(unknownFields: ByteString): [Message.Builder](index.md)&lt;[M](index.md), [B](index.md)&gt; |
| [build](build.md) | [jvm]<br>abstract fun [build](build.md)(): [M](index.md)<br>Returns an immutable [Message](../index.md) based on the fields that set in this builder. |
| [buildUnknownFields](build-unknown-fields.md) | [jvm]<br>fun [buildUnknownFields](build-unknown-fields.md)(): ByteString<br>Returns a byte string with this message's unknown fields. Returns an empty byte string if this message has no unknown fields. |
| [clearUnknownFields](clear-unknown-fields.md) | [jvm]<br>fun [clearUnknownFields](clear-unknown-fields.md)(): [Message.Builder](index.md)&lt;[M](index.md), [B](index.md)&gt; |
