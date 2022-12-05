//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[ErrorCollector](index.md)/[at](at.md)

# at

[common]\
fun [at](at.md)(additionalContext: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)): [ErrorCollector](index.md)

Returns a copy of this error collector that includes [additionalContext](at.md) in error messages reported to it. The current and returned instance both contribute errors to the same list.
