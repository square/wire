//[wire-schema](../../../index.md)/[com.squareup.wire.schema.internal.parser](../index.md)/[SyntaxReader](index.md)/[readName](read-name.md)

# readName

[common]\
fun [readName](read-name.md)(allowLeadingDigit: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, retainWrap: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)

Reads a (paren-wrapped), square-wrapped or naked symbol name. If {@code retainWrap} is true and the symbol was wrapped in parens or square brackets, the returned string retains the wrapping punctuation. Otherwise, just the symbol is returned.
