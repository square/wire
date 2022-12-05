//[wire-schema](../../../index.md)/[com.squareup.wire.schema.internal.parser](../index.md)/[SyntaxReader](index.md)

# SyntaxReader

[common]\
class [SyntaxReader](index.md)(data: [CharArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char-array/index.html), location: [Location](../../com.squareup.wire.schema/-location/index.md))

A general purpose reader for formats like .proto.

## Constructors

| | |
|---|---|
| [SyntaxReader](-syntax-reader.md) | [common]<br>fun [SyntaxReader](-syntax-reader.md)(data: [CharArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char-array/index.html), location: [Location](../../com.squareup.wire.schema/-location/index.md)) |

## Functions

| Name | Summary |
|---|---|
| [exhausted](exhausted.md) | [common]<br>fun [exhausted](exhausted.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [expect](expect.md) | [common]<br>inline fun [expect](expect.md)(condition: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), location: [Location](../../com.squareup.wire.schema/-location/index.md) = location(), message: () -&gt; [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [location](location.md) | [common]<br>fun [location](location.md)(): [Location](../../com.squareup.wire.schema/-location/index.md) |
| [peekChar](peek-char.md) | [common]<br>fun [peekChar](peek-char.md)(): [Char](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char/index.html)<br>Peeks a non-whitespace character and returns it. The only difference between this and [readChar](read-char.md) is that this doesn't consume the char.<br>[common]<br>fun [peekChar](peek-char.md)(c: [Char](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [pushBack](push-back.md) | [common]<br>fun [pushBack](push-back.md)(c: [Char](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char/index.html))<br>Push back the most recently read character. |
| [readChar](read-char.md) | [common]<br>fun [readChar](read-char.md)(): [Char](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char/index.html)<br>Reads a non-whitespace character and returns it. |
| [readDataType](read-data-type.md) | [common]<br>fun [readDataType](read-data-type.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Reads a scalar, map, or type name.<br>[common]<br>fun [readDataType](read-data-type.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Reads a scalar, map, or type name with name as a prefix word. |
| [readDocumentation](read-documentation.md) | [common]<br>fun [readDocumentation](read-documentation.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Like skipWhitespace, but this returns a string containing all comment text. By convention, comments before a declaration document that declaration. |
| [readInt](read-int.md) | [common]<br>fun [readInt](read-int.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Reads an integer and returns it. |
| [readName](read-name.md) | [common]<br>fun [readName](read-name.md)(allowLeadingDigit: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, retainWrap: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Reads a (paren-wrapped), square-wrapped or naked symbol name. If {@code retainWrap} is true and the symbol was wrapped in parens or square brackets, the returned string retains the wrapping punctuation. Otherwise, just the symbol is returned. |
| [readQuotedString](read-quoted-string.md) | [common]<br>fun [readQuotedString](read-quoted-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [readString](read-string.md) | [common]<br>fun [readString](read-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Reads a quoted or unquoted string and returns it. |
| [readWord](read-word.md) | [common]<br>fun [readWord](read-word.md)(allowLeadingDigit: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Reads a non-empty word and returns it. |
| [require](require.md) | [common]<br>fun [require](require.md)(c: [Char](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char/index.html))<br>Reads a non-whitespace character 'c', or throws an exception. |
| [tryAppendTrailingDocumentation](try-append-trailing-documentation.md) | [common]<br>fun [tryAppendTrailingDocumentation](try-append-trailing-documentation.md)(documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [unexpected](unexpected.md) | [common]<br>fun [unexpected](unexpected.md)(message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), location: [Location](../../com.squareup.wire.schema/-location/index.md)? = location()): [RuntimeException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-runtime-exception/index.html) |
