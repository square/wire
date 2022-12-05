//[wire-schema](../../../index.md)/[com.squareup.wire.schema.internal.parser](../index.md)/[OptionReader](index.md)

# OptionReader

[common]\
class [OptionReader](index.md)(reader: [SyntaxReader](../-syntax-reader/index.md))

## Functions

| Name | Summary |
|---|---|
| [readOption](read-option.md) | [common]<br>fun [readOption](read-option.md)(keyValueSeparator: [Char](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char/index.html)): [OptionElement](../-option-element/index.md)<br>Reads a option containing a name, an '=' or ':', and a value. |
| [readOptions](read-options.md) | [common]<br>fun [readOptions](read-options.md)(): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](../-option-element/index.md)&gt;<br>Reads options enclosed in '' and '' if they are present and returns them. Returns an empty list if no options are present. |
