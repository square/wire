//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Location](index.md)

# Location

[common]\
data class [Location](index.md)(base: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), line: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), column: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html))

Locates a .proto file, or a position within a .proto file, on the file system. This includes a base directory or a .jar file, and a path relative to that base.

## Constructors

| | |
|---|---|
| [Location](-location.md) | [common]<br>fun [Location](-location.md)(base: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), line: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = -1, column: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = -1) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [at](at.md) | [common]<br>fun [at](at.md)(line: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), column: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [Location](index.md) |
| [toString](to-string.md) | [common]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [withoutBase](without-base.md) | [common]<br>fun [withoutBase](without-base.md)(): [Location](index.md)<br>Returns a copy of this location with an empty base. |
| [withPathOnly](with-path-only.md) | [common]<br>fun [withPathOnly](with-path-only.md)(): [Location](index.md)<br>Returns a copy of this location including only its path. |

## Properties

| Name | Summary |
|---|---|
| [base](base.md) | [common]<br>val [base](base.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>The base of this location; typically a directory or .jar file. |
| [column](column.md) | [common]<br>val [column](column.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>The column on the line of this location, or -1 for no specific column. |
| [line](line.md) | [common]<br>val [line](line.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>The line number of this location, or -1 for no specific line number. |
| [path](path.md) | [common]<br>val [path](path.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>The path to this location relative to [base](base.md). |
