//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[ErrorCollector](index.md)

# ErrorCollector

[common]\
class [ErrorCollector](index.md)

Collects errors to be reported as a batch. Errors include both a detail message plus context of where they occurred within the schema.

## Constructors

| | |
|---|---|
| [ErrorCollector](-error-collector.md) | [common]<br>fun [ErrorCollector](-error-collector.md)() |

## Functions

| Name | Summary |
|---|---|
| [at](at.md) | [common]<br>fun [at](at.md)(additionalContext: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)): [ErrorCollector](index.md)<br>Returns a copy of this error collector that includes [additionalContext](at.md) in error messages reported to it. The current and returned instance both contribute errors to the same list. |
| [plusAssign](plus-assign.md) | [common]<br>operator fun [plusAssign](plus-assign.md)(message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Add [message](plus-assign.md) as an error to this collector. |
| [throwIfNonEmpty](throw-if-non-empty.md) | [common]<br>fun [throwIfNonEmpty](throw-if-non-empty.md)() |
