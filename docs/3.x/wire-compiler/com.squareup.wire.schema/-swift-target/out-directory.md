//[wire-compiler](../../../index.md)/[com.squareup.wire.schema](../index.md)/[SwiftTarget](index.md)/[outDirectory](out-directory.md)

# outDirectory

[jvm]\
open override val [outDirectory](out-directory.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)

Directory where this target will write its output.

In Gradle, when this class is serialized, this is relative to the project to improve build cacheability. Callers must use [copyTarget](copy-target.md) to resolve it to real path prior to use.
